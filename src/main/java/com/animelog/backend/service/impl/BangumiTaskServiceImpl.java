package com.animelog.backend.service.impl;

import com.animelog.backend.config.AnimeLogProperties;
import com.animelog.backend.domain.*;
import com.animelog.backend.mapper.*;
import com.animelog.backend.service.BangumiTaskService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Bangumi 任务业务实现，提供存档同步和业务导入功能。<br>
 * 包含定时任务执行器，解析管理员上传的 Bangumi 归档数据并导入。
 */
@Service
public class BangumiTaskServiceImpl implements BangumiTaskService {
    private static final String LOCK_KEY = "bangumi:task:runner";
    private static final String ARCHIVE_EXTENSION = ".zip";
    private static final String CHUNK_UPLOAD_DIR = ".uploads";
    private static final long MAX_CHUNK_BYTES = 5L * 1024L * 1024L;
    private final BangumiTaskMapper taskMapper;
    private final BangumiTaskStepMapper stepMapper;
    private final BangumiTaskItemMapper itemMapper;
    private final BangumiSourceSubjectMapper sourceMapper;
    private final BangumiArchiveFileMapper archiveFileMapper;
    private final MediaMapper mediaMapper;
    private final MediaTagMapper mediaTagMapper;
    private final MediaTagRelationMapper mediaTagRelationMapper;
    private final StringRedisTemplate redisTemplate;
    private final AnimeLogProperties properties;
    private final ObjectMapper objectMapper;

    public BangumiTaskServiceImpl(
        BangumiTaskMapper taskMapper,
        BangumiTaskStepMapper stepMapper,
        BangumiTaskItemMapper itemMapper,
        BangumiSourceSubjectMapper sourceMapper,
        BangumiArchiveFileMapper archiveFileMapper,
        MediaMapper mediaMapper,
        MediaTagMapper mediaTagMapper,
        MediaTagRelationMapper mediaTagRelationMapper,
        StringRedisTemplate redisTemplate,
        AnimeLogProperties properties
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.itemMapper = itemMapper;
        this.sourceMapper = sourceMapper;
        this.archiveFileMapper = archiveFileMapper;
        this.mediaMapper = mediaMapper;
        this.mediaTagMapper = mediaTagMapper;
        this.mediaTagRelationMapper = mediaTagRelationMapper;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public BangumiTask createArchiveSyncTask() {
        return createTask("archive_sync", null);
    }

    @Override
    public synchronized ArchiveChunkUploadResult uploadArchiveChunkAndCreateSyncTask(
        MultipartFile file,
        String uploadId,
        String filename,
        int chunkIndex,
        int totalChunks
    ) {
        validateChunkUpload(file, uploadId, chunkIndex, totalChunks);
        String safeFilename = sanitizeArchiveFilename(filename);
        Path dir = Paths.get(properties.bangumi().workDir());
        Path uploadDir = dir.resolve(CHUNK_UPLOAD_DIR).resolve(uploadId).normalize();
        if (!uploadDir.startsWith(dir.resolve(CHUNK_UPLOAD_DIR).normalize())) {
            throw new IllegalArgumentException("Invalid upload id");
        }
        try {
            Files.createDirectories(uploadDir);
            Path chunk = uploadDir.resolve(chunkFilename(chunkIndex)).normalize();
            if (!chunk.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid chunk index");
            }
            Path temp = Files.createTempFile(uploadDir, "chunk-", ".upload");
            try {
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(temp, chunk, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }

            int receivedChunks = countReceivedChunks(uploadDir);
            if (receivedChunks < totalChunks) {
                return new ArchiveChunkUploadResult(false, receivedChunks, totalChunks, null);
            }

            Path archive = assembleUploadedArchive(uploadDir, dir, safeFilename, totalChunks);
            deleteDirectory(uploadDir);
            BangumiTask task = createTask("archive_sync", archive.getFileName().toString());
            return new ArchiveChunkUploadResult(true, totalChunks, totalChunks, task);
        } catch (IOException e) {
            throw new IllegalStateException("保存 Bangumi 数据源压缩包分片失败", e);
        }
    }

    @Override
    public BangumiTask createBusinessImportTask(List<Long> bangumiIds) {
        try {
            return createTask("business_import", objectMapper.writeValueAsString(bangumiIds));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid import payload", e);
        }
    }

    @Override
    public List<BangumiTask> listTasks(int limit) {
        return taskMapper.selectList(new QueryWrapper<BangumiTask>().orderByDesc("created_at").last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    /**
     * 定时任务执行器，每 5 秒检查一次是否有待执行的任务。<br>
     * 使用 Redis 分布式锁确保多实例环境下只有一个执行者。
     */
    @Scheduled(fixedDelay = 5000)
    public void runNextScheduled() {
        // 若未启用任务执行器则直接返回
        if (!properties.bangumi().taskRunnerEnabled()) {
            return;
        }
        // 尝试获取分布式锁，防止多实例并发执行
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
            LOCK_KEY,
            UUID.randomUUID().toString(),
            properties.bangumi().taskRunnerLockSeconds(),
            TimeUnit.SECONDS
        );
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            runNext();
        } finally {
            // 释放锁
            redisTemplate.delete(LOCK_KEY);
        }
    }

    /**
     * 创建任务记录并持久化。
     */
    @Transactional
    public BangumiTask createTask(String type, String payload) {
        BangumiTask task = new BangumiTask();
        task.setTaskType(type);
        task.setStatus("pending");
        task.setRequestPayload(payload);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setSkipCount(0);
        task.setUpdateCount(0);
        task.setFailureCount(0);
        taskMapper.insert(task);
        return task;
    }

    /**
     * 取出下一个待执行的任务并执行。
     */
    public void runNext() {
        // 查找最早创建的待执行任务
        BangumiTask task = taskMapper.selectOne(new QueryWrapper<BangumiTask>()
            .eq("status", "pending")
            .orderByAsc("created_at")
            .last("LIMIT 1"));
        if (task == null) {
            return;
        }
        try {
            // 标记任务为运行中
            markTaskRunning(task, "start");
            // 根据任务类型分发执行
            if ("archive_sync".equals(task.getTaskType())) {
                runArchiveSync(task);
            } else if ("business_import".equals(task.getTaskType())) {
                runBusinessImport(task);
            } else {
                throw new IllegalStateException("Unsupported task type: " + task.getTaskType());
            }
            // 标记任务成功
            markTaskFinished(task.getId(), "success", null);
        } catch (Exception e) {
            // 标记任务失败
            markTaskFinished(task.getId(), "failed", e.getMessage());
        }
    }

    /**
     * 执行存档同步任务：读取管理员上传的 ZIP 文件、解析并入库。
     */
    private void runArchiveSync(BangumiTask task) throws Exception {
        // 步骤 1：定位已上传的归档文件
        Path zip = step(task.getId(), 1, "load_uploaded_archive", () -> resolveUploadedArchive(task.getRequestPayload()));
        // 计算文件哈希并记录归档记录
        String archiveHash = sha256(zip);
        upsertArchiveFile(zip.getFileName().toString(), Files.size(zip), archiveHash, zip);

        // 步骤 2：解析 ZIP 并导入数据
        Counters counters = step(task.getId(), 2, "parse_and_upsert", () -> parseZipAndUpsert(task, zip));
        // 更新任务统计信息
        task.setTotalCount(counters.total);
        task.setSuccessCount(counters.inserted);
        task.setSkipCount(counters.skipped);
        task.setUpdateCount(counters.updated);
        task.setFailureCount(counters.failed);
        taskMapper.updateById(task);
    }

    /**
     * 执行业务导入任务：将 Bangumi 源条目导入到 media 表。
     */
    private void runBusinessImport(BangumiTask task) throws Exception {
        // 解析请求中的 Bangumi ID 列表
        List<Long> ids = Arrays.asList(objectMapper.readValue(task.getRequestPayload(), Long[].class));
        Counters counters = new Counters();
        for (Long bangumiId : ids) {
            counters.total++;
            // 查找源条目
            BangumiSourceSubject source = sourceMapper.selectOne(new QueryWrapper<BangumiSourceSubject>().eq("bangumi_id", bangumiId).last("LIMIT 1"));
            if (source == null) {
                counters.failed++;
                taskItem(task.getId(), bangumiId, null, "missing_source", "failed", "Bangumi source not found");
                continue;
            }
            // 检查是否已导入过
            Media existing = mediaMapper.selectOne(new QueryWrapper<Media>()
                .eq("source_type", "bangumi")
                .eq("source_id", String.valueOf(source.getBangumiId()))
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
            if (existing == null) {
                // 未导入：创建新 media 条目
                Media media = mediaFromSource(source);
                mediaMapper.insert(media);
                source.setImportedMediaId(media.getId());
                sourceMapper.updateById(source);
                upsertTagsForMedia(media.getId(), source.getTagsJson());
                counters.inserted++;
                taskItem(task.getId(), source.getBangumiId(), media.getId(), "inserted", "success", null);
            } else if (Objects.equals(existing.getSourceHash(), source.getSourceHash()) || Boolean.TRUE.equals(existing.getManualOverride())) {
                // 数据未变化或已被人工覆盖：跳过
                counters.skipped++;
                taskItem(task.getId(), source.getBangumiId(), existing.getId(), "skipped", "success", null);
            } else {
                // 数据有更新：更新现有 media 条目
                existing.setTitle(bestTitle(source));
                existing.setOriginalTitle(source.getName());
                existing.setSummary(source.getSummary());
                existing.setYear(source.getYear());
                existing.setEpisodeCount(source.getEpisodeCount());
                existing.setScore(source.getScore());
                existing.setNsfw(source.getNsfw());
                existing.setSourceHash(source.getSourceHash());
                mediaMapper.updateById(existing);
                upsertTagsForMedia(existing.getId(), source.getTagsJson());
                counters.updated++;
                taskItem(task.getId(), source.getBangumiId(), existing.getId(), "updated", "success", null);
            }
        }
        // 更新任务统计
        task.setTotalCount(counters.total);
        task.setSuccessCount(counters.inserted);
        task.setSkipCount(counters.skipped);
        task.setUpdateCount(counters.updated);
        task.setFailureCount(counters.failed);
        taskMapper.updateById(task);
    }

    private String sanitizeArchiveFilename(String originalFilename) {
        String filename = Optional.ofNullable(originalFilename)
            .map(name -> Paths.get(name).getFileName().toString())
            .map(name -> name.replace('\\', '_'))
            .map(name -> name.replaceAll("[^A-Za-z0-9._-]", "_"))
            .filter(name -> !name.isBlank())
            .orElse("bangumi-archive.zip");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(ARCHIVE_EXTENSION)) {
            throw new IllegalArgumentException("Bangumi 数据源压缩包必须是 ZIP 文件");
        }
        return filename;
    }

    private void validateChunkUpload(MultipartFile file, String uploadId, int chunkIndex, int totalChunks) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 Bangumi 数据源压缩包分片");
        }
        if (file.getSize() > MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException("单个分片不能超过 5MB");
        }
        if (uploadId == null || !uploadId.matches("[A-Za-z0-9_-]{8,64}")) {
            throw new IllegalArgumentException("Invalid upload id");
        }
        if (totalChunks < 1 || totalChunks > 10000) {
            throw new IllegalArgumentException("Invalid chunk count");
        }
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("Invalid chunk index");
        }
    }

    private String chunkFilename(int chunkIndex) {
        return String.format(Locale.ROOT, "chunk-%05d.part", chunkIndex);
    }

    private int countReceivedChunks(Path uploadDir) throws IOException {
        try (Stream<Path> stream = Files.list(uploadDir)) {
            return (int) stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().matches("chunk-\\d{5}\\.part"))
                .count();
        }
    }

    private Path assembleUploadedArchive(Path uploadDir, Path dir, String filename, int totalChunks) throws IOException {
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "bangumi-archive-", ".upload");
        try {
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunk = uploadDir.resolve(chunkFilename(i));
                    if (!Files.isRegularFile(chunk)) {
                        throw new IllegalArgumentException("Missing archive chunk: " + i);
                    }
                    Files.copy(chunk, out);
                }
            }
            return moveCompletedArchive(temp, dir, filename);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Path moveCompletedArchive(Path temp, Path dir, String filename) throws IOException {
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir.normalize())) {
            throw new IllegalArgumentException("Invalid archive filename");
        }
        deleteExistingArchives(dir);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            Iterator<Path> iterator = stream.sorted(Comparator.reverseOrder()).iterator();
            while (iterator.hasNext()) {
                Files.deleteIfExists(iterator.next());
            }
        }
    }

    /** 删除工作目录中旧的 ZIP 文件。 */
    private void deleteExistingArchives(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            Iterator<Path> iterator = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ARCHIVE_EXTENSION))
                .iterator();
            while (iterator.hasNext()) {
                Files.deleteIfExists(iterator.next());
            }
        }
    }

    /** 根据任务 payload 或工作目录定位已上传的 ZIP 文件。 */
    private Path resolveUploadedArchive(String filename) throws IOException {
        Path dir = Paths.get(properties.bangumi().workDir());
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("No uploaded Bangumi archive found");
        }
        if (filename != null && !filename.isBlank()) {
            Path archive = dir.resolve(filename).normalize();
            if (archive.startsWith(dir.normalize()) && Files.isRegularFile(archive)) {
                return archive;
            }
            throw new IllegalStateException("Uploaded Bangumi archive not found: " + filename);
        }
        try (Stream<Path> stream = Files.list(dir)) {
            Iterator<Path> iterator = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ARCHIVE_EXTENSION))
                .iterator();
            if (!iterator.hasNext()) {
                throw new IllegalStateException("No uploaded Bangumi archive found");
            }
            Path archive = iterator.next();
            if (iterator.hasNext()) {
                throw new IllegalStateException("Multiple Bangumi archives found in work directory");
            }
            return archive;
        }
    }

    /** 解析 ZIP 归档，找到 subject JSONL 文件并进行导入。 */
    private Counters parseZipAndUpsert(BangumiTask task, Path zip) throws Exception {
        Counters counters = new Counters();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase(Locale.ROOT);
                // 查找 subject 相关的 jsonl/jsonlines 文件
                if (!entry.isDirectory() && name.contains("subject") && (name.endsWith(".jsonlines") || name.endsWith(".jsonl"))) {
                    parseSubjectLines(task, zis, counters);
                    return counters;
                }
            }
        }
        throw new IllegalStateException("No subject jsonl file found in archive");
    }

    /** 逐行解析 subject JSONL 数据，逐条插入或更新到源表。 */
    private void parseSubjectLines(BangumiTask task, InputStream stream, Counters counters) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                counters.total++;
                JsonNode node = objectMapper.readTree(line);
                // 映射媒体类型，不支持的原始类型直接跳过
                Integer rawType = node.path("type").isInt() ? node.path("type").asInt() : null;
                String mediaType = mapMediaType(rawType);
                if (mediaType == null) {
                    counters.skipped++;
                    continue;
                }
                // 计算数据哈希，用于后续去重判断
                String hash = hashNode(node);
                Long bangumiId = node.path("id").asLong();
                // 检查源表中是否已存在
                BangumiSourceSubject existing = sourceMapper.selectOne(new QueryWrapper<BangumiSourceSubject>().eq("bangumi_id", bangumiId).last("LIMIT 1"));
                if (existing != null && Objects.equals(existing.getSourceHash(), hash)) {
                    // 数据未变化，跳过
                    counters.skipped++;
                    taskItem(task.getId(), bangumiId, existing.getImportedMediaId(), "skipped", "success", null);
                    continue;
                }
                // 构建源条目对象
                BangumiSourceSubject source = sourceFromNode(node, mediaType, rawType, hash);
                if (existing == null) {
                    // 新增
                    sourceMapper.insert(source);
                    counters.inserted++;
                    taskItem(task.getId(), bangumiId, null, "inserted", "success", null);
                } else {
                    // 更新（保留已导入的 mediaId 关联）
                    source.setId(existing.getId());
                    source.setImportedMediaId(existing.getImportedMediaId());
                    sourceMapper.updateById(source);
                    counters.updated++;
                    taskItem(task.getId(), bangumiId, existing.getImportedMediaId(), "updated", "success", null);
                }
            }
        }
    }

    /** 从 JSON 节点构建 Bangumi 源条目对象。 */
    private BangumiSourceSubject sourceFromNode(JsonNode node, String mediaType, Integer rawType, String hash) throws Exception {
        BangumiSourceSubject source = new BangumiSourceSubject();
        source.setBangumiId(node.path("id").asLong());
        source.setRawType(rawType);
        source.setMediaType(mediaType);
        source.setName(textOrNull(node, "name"));
        source.setNameCn(textOrNull(node, "name_cn"));
        source.setSummary(textOrNull(node, "summary"));
        source.setInfobox(textOrNull(node, "infobox"));
        source.setAirDate(parseDate(textOrNull(node, "date")));
        if (source.getAirDate() != null) {
            source.setYear(source.getAirDate().getYear());
            source.setMonth(source.getAirDate().getMonthValue());
        }
        source.setEpisodeCount(parseEpisodeCount(node.path("infobox").asText("")));
        source.setTagsJson(node.has("tags") ? objectMapper.writeValueAsString(node.get("tags")) : null);
        source.setScore(node.path("score").isNumber() ? BigDecimal.valueOf(node.path("score").asDouble()) : null);
        source.setRank(node.path("rank").isInt() && node.path("rank").asInt() > 0 ? node.path("rank").asInt() : null);
        source.setNsfw(node.path("nsfw").asBoolean(false));
        source.setRawJson(objectMapper.writeValueAsString(node));
        source.setSourceHash(hash);
        return source;
    }

    /** 从 Bangumi 源条目构建 media 对象（用于首次导入）。 */
    private Media mediaFromSource(BangumiSourceSubject source) {
        Media media = new Media();
        media.setType(source.getMediaType());
        media.setTitle(bestTitle(source));
        media.setOriginalTitle(source.getName());
        media.setSummary(source.getSummary());
        media.setYear(source.getYear());
        media.setEpisodeCount(source.getEpisodeCount());
        media.setStatus("upcoming");
        media.setScore(source.getScore());
        media.setScoreCount(0);
        media.setNsfw(Boolean.TRUE.equals(source.getNsfw()));
        media.setManualOverride(false);
        media.setSourceType("bangumi");
        media.setSourceId(String.valueOf(source.getBangumiId()));
        media.setSourceHash(source.getSourceHash());
        media.setIsDeleted(0);
        return media;
    }

    /** 获取最佳显示标题：优先中文名，其次原名。 */
    private String bestTitle(BangumiSourceSubject source) {
        return source.getNameCn() != null && !source.getNameCn().isBlank() ? source.getNameCn() : source.getName();
    }

    /** 对 JSON 节点进行规范化哈希，用于数据去重。 */
    private String hashNode(JsonNode node) throws Exception {
        JsonNode canonical = canonicalize(node);
        return sha256(objectMapper.writeValueAsBytes(canonical));
    }

    /** 对 JSON 节点按 Key 排序进行规范化，确保相同内容产生相同哈希。 */
    private JsonNode canonicalize(JsonNode node) {
        if (!node.isObject()) {
            return node;
        }
        ObjectNode sorted = objectMapper.createObjectNode();
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        Collections.sort(names);
        for (String name : names) {
            sorted.set(name, canonicalize(node.get(name)));
        }
        return sorted;
    }

    /** 计算文件的 SHA-256 哈希。 */
    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new DigestInputStream(Files.newInputStream(path), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return hex(digest.digest());
    }

    /** 计算字节数组的 SHA-256 哈希。 */
    private String sha256(byte[] data) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    /** 将字节数组转换为十六进制字符串。 */
    private String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }

    /** 新增归档文件记录（基于文件哈希去重，已存在则跳过）。 */
    private void upsertArchiveFile(String filename, long size, String hash, Path path) {
        BangumiArchiveFile existing = archiveFileMapper.selectOne(new QueryWrapper<BangumiArchiveFile>().eq("file_hash", hash).last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        BangumiArchiveFile file = new BangumiArchiveFile();
        file.setFilename(filename);
        file.setFileSize(size);
        file.setDumpDate(parseDumpDate(filename));
        file.setFileHash(hash);
        file.setStoragePath(path.toString());
        file.setStatus("downloaded");
        archiveFileMapper.insert(file);
    }

    /** 从文件名中解析数据导出日期（格式：dump-YYYY-MM-dd）。 */
    private LocalDate parseDumpDate(String filename) {
        try {
            int start = filename.indexOf("20");
            return start >= 0 ? LocalDate.parse(filename.substring(start, start + 10)) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 解析日期字符串，处理异常值和空值。 */
    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("0000") || raw.endsWith("-00")) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 从 infobox 中解析集数/话数。匹配模式如 "话数 26" 或 "集数 12"。 */
    private Integer parseEpisodeCount(String infobox) {
        if (infobox == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(话数|集数)[^0-9]{0,20}(\\d+)").matcher(infobox);
        return matcher.find() ? Integer.parseInt(matcher.group(2)) : null;
    }

    /** 安全获取 JSON 文本字段，null 或空字符串时返回 null。 */
    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    /** 将 Bangumi 原始类型映射为内部媒体类型：1→novel, 2→anime, 4→game。 */
    private String mapMediaType(Integer rawType) {
        if (rawType == null) {
            return null;
        }
        return switch (rawType) {
            case 1 -> "novel";
            case 2 -> "anime";
            case 4 -> "game";
            default -> null;
        };
    }

    /** 标记任务为运行中，设置开始时间和当前步骤。 */
    private void markTaskRunning(BangumiTask task, String step) {
        task.setStatus("running");
        task.setCurrentStep(step);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /** 标记任务为已完成状态（成功或失败），记录错误信息。 */
    private void markTaskFinished(Long id, String status, String error) {
        BangumiTask task = taskMapper.selectById(id);
        task.setStatus(status);
        task.setErrorMessage(error);
        task.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /**
     * 执行任务步骤并记录执行状态（带返回值版本）。
     *
     * @param taskId   任务 ID
     * @param order    步骤序号
     * @param name     步骤名称
     * @param supplier 步骤执行逻辑
     * @param <T>      返回值类型
     * @return 步骤执行结果
     */
    private <T> T step(Long taskId, int order, String name, CheckedSupplier<T> supplier) throws Exception {
        BangumiTaskStep step = new BangumiTaskStep();
        step.setTaskId(taskId);
        step.setStepOrder(order);
        step.setStepName(name);
        step.setStatus("running");
        step.setStartedAt(LocalDateTime.now());
        stepMapper.insert(step);
        try {
            T result = supplier.get();
            step.setStatus("success");
            step.setFinishedAt(LocalDateTime.now());
            stepMapper.updateById(step);
            return result;
        } catch (Exception e) {
            step.setStatus("failed");
            step.setErrorMessage(e.getMessage());
            step.setFinishedAt(LocalDateTime.now());
            stepMapper.updateById(step);
            throw e;
        }
    }

    /** 执行任务步骤并记录执行状态（无返回值版本）。 */
    private void step(Long taskId, int order, String name, CheckedRunnable runnable) throws Exception {
        step(taskId, order, name, () -> {
            runnable.run();
            return null;
        });
    }

    /** 记录任务项处理结果。 */
    private void taskItem(Long taskId, Long bangumiId, Long mediaId, String action, String status, String error) {
        BangumiTaskItem item = new BangumiTaskItem();
        item.setTaskId(taskId);
        item.setBangumiId(bangumiId);
        item.setMediaId(mediaId);
        item.setAction(action);
        item.setStatus(status);
        item.setErrorMessage(error);
        itemMapper.insert(item);
    }

    /** 可抛出异常的 Supplier 函数式接口。 */
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /** 可抛出异常的 Runnable 函数式接口。 */
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * 为指定媒体写入标签：先清除旧关联，再按阈值过滤后重建。<br>
     * 规则：
     * <ul>
     *   <li>只导入 count &ge; tagMinCount 的标签（过滤低质量/噪音标签）</li>
     *   <li>标签名做 trim + 去空 + 长度限制（&le;50 字符）</li>
     *   <li>list 去重后再写入，防止同名标签重复关联</li>
     *   <li>tag_name 列存在唯一约束，使用 INSERT … ON CONFLICT DO NOTHING 保证全局唯一</li>
     * </ul>
     *
     * @param mediaId  目标媒体 ID
     * @param tagsJson Bangumi tags 原始 JSON，格式为 [{"name":"...", "count":N}, ...]
     */
    private void upsertTagsForMedia(Long mediaId, String tagsJson) {
        if (mediaId == null || tagsJson == null || tagsJson.isBlank()) {
            return;
        }
        int minCount = properties.bangumi().tagMinCount();
        List<String> tagNames = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(tagsJson);
            if (!arr.isArray()) return;
            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode tagNode : arr) {
                int count = tagNode.path("count").asInt(0);
                if (count < minCount) continue;
                String name = tagNode.path("name").asText("").trim();
                if (!name.isBlank() && name.length() <= 50) {
                    seen.add(name);
                }
            }
            tagNames.addAll(seen);
        } catch (Exception e) {
            // tagsJson 格式异常时静默跳过，不影响主体数据导入
            return;
        }

        // 先清除旧关联（标签本体不删除，仅解除关联）
        mediaTagRelationMapper.deleteByMediaId(mediaId);

        // 逐个 upsert 标签并建立关联
        for (String name : tagNames) {
            // INSERT … ON CONFLICT DO NOTHING，保证全局唯一
            mediaTagMapper.insertIgnore(name);
            // 查出 id（有可能是已存在的记录）
            MediaTag tag = mediaTagMapper.findByName(name);
            if (tag != null) {
                mediaTagRelationMapper.upsert(mediaId, tag.getId());
            }
        }
    }

    /** 处理计数器，统计各类操作的数量。 */
    private static class Counters {
        int total;      // 总数
        int inserted;   // 新增数
        int skipped;    // 跳过数
        int updated;    // 更新数
        int failed;     // 失败数
    }
}
