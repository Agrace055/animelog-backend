package com.animelog.backend.service;

import com.animelog.backend.domain.BangumiTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Bangumi 任务业务接口，提供存档同步和导入任务的管理功能。
 */
public interface BangumiTaskService {
    /** 创建存档同步任务。 */
    BangumiTask createArchiveSyncTask();

    /** 上传 Bangumi 存档压缩包分片；全部分片到齐后创建解析任务。 */
    ArchiveChunkUploadResult uploadArchiveChunkAndCreateSyncTask(
        MultipartFile file,
        String uploadId,
        String filename,
        int chunkIndex,
        int totalChunks
    );

    /** 创建业务导入任务，将指定 Bangumi 条目导入到 media 表。 */
    BangumiTask createBusinessImportTask(List<Long> bangumiIds);

    /** 获取任务列表（限制最大 100 条）。 */
    List<BangumiTask> listTasks(int limit);

    record ArchiveChunkUploadResult(boolean completed, int receivedChunks, int totalChunks, BangumiTask task) {
    }
}
