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

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Bangumi 任务业务实现，提供存档同步和业务导入功能。<br>
 * 包含定时任务执行器，定期从 Bangumi 下载归档数据并解析导入。
 */
@Service
public class BangumiTaskServiceImpl implements BangumiTaskService {
    private static final String LOCK_KEY = "bangumi:task:runner";
    private final BangumiTaskMapper taskMapper;
    private final BangumiTaskStepMapper stepMapper;
    private final BangumiTaskItemMapper itemMapper;
    private final BangumiSourceSubjectMapper sourceMapper;
    private final BangumiArchiveFileMapper archiveFileMapper;
    private final MediaMapper mediaMapper;
    private final StringRedisTemplate redisTemplate;
    private final AnimeLogProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public BangumiTaskServiceImpl(
        BangumiTaskMapper taskMapper,
        BangumiTaskStepMapper stepMapper,
        BangumiTaskItemMapper itemMapper,
        BangumiSourceSubjectMapper sourceMapper,
        BangumiArchiveFileMapper archiveFileMapper,
        MediaMapper mediaMapper,
        StringRedisTemplate redisTemplate,
        AnimeLogProperties properties
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.itemMapper = itemMapper;
        this.sourceMapper = sourceMapper;
        this.archiveFileMapper = archiveFileMapper;
        this.mediaMapper = mediaMapper;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public BangumiTask createArchiveSyncTask() {
        return createTask("archive_sync", null);
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
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
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
     * 执行存档同步任务：获取最新归档元数据、下载文件、解析并入库。
     */
    private void runArchiveSync(BangumiTask task) throws Exception {
        // 步骤 1：预留（可在此处执行前置校验）
        step(task.getId(), 1, "fetch_latest", () -> {
        });
        // 获取最新归档的元数据信息
        JsonNode latest = fetchLatestJson();
        String filename = latest.path("file").asText();
        String url = latest.path("url").asText();
        long size = latest.path("size").asLong(0);
        if (filename.isBlank() || url.isBlank()) {
            throw new IllegalStateException("Bangumi latest metadata missing file/url");
        }

        // 步骤 2：下载归档文件
        Path zip = step(task.getId(), 2, "download_archive", () -> downloadArchive(filename, url));
        // 计算文件哈希并记录归档记录
        String archiveHash = sha256(zip);
        upsertArchiveFile(filename, size, archiveHash, zip);

        // 步骤 3：解析 ZIP 并导入数据
        Counters counters = step(task.getId(), 3, "parse_and_upsert", () -> parseZipAndUpsert(task, zip));
        // 更新任务统计信息
        task.setTotalCount(counters.total);
        task.setSuccessCount(counters.inserted);
        task.setSkipCount(counters.skipped);
        task.setUpdateCount(counters.updated);
        task.setFailureCount(counters.failed);
        task.setUpdatedAt(LocalDateTime.now());
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
                existing.setUpdatedAt(LocalDateTime.now());
                mediaMapper.updateById(existing);
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
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /** 从 Bangumi 获取最新归档的 JSON 元数据。 */
    private JsonNode fetchLatestJson() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.bangumi().latestJsonUrl())).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("Bangumi latest metadata request failed: " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    /** 下载归档文件到本地工作目录。若文件已存在则直接返回。 */
    private Path downloadArchive(String filename, String url) throws Exception {
        Path dir = Paths.get(properties.bangumi().workDir());
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        if (Files.exists(target)) {
            return target;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IOException("Bangumi archive download failed: " + response.statusCode());
        }
        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
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
        LocalDateTime now = LocalDateTime.now();
        source.setCreatedAt(now);
        source.setUpdatedAt(now);
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
        LocalDateTime now = LocalDateTime.now();
        media.setCreatedAt(now);
        media.setUpdatedAt(now);
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
        file.setCreatedAt(LocalDateTime.now());
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
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /** 标记任务为已完成状态（成功或失败），记录错误信息。 */
    private void markTaskFinished(Long id, String status, String error) {
        BangumiTask task = taskMapper.selectById(id);
        task.setStatus(status);
        task.setErrorMessage(error);
        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
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
        item.setCreatedAt(LocalDateTime.now());
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

    /** 处理计数器，统计各类操作的数量。 */
    private static class Counters {
        int total;      // 总数
        int inserted;   // 新增数
        int skipped;    // 跳过数
        int updated;    // 更新数
        int failed;     // 失败数
    }
}
