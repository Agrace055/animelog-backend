package com.animelog.backend.service;

import com.animelog.backend.domain.BangumiTask;

import java.util.List;

/**
 * Bangumi 任务业务接口，提供存档同步和导入任务的管理功能。
 */
public interface BangumiTaskService {
    /** 创建存档同步任务。 */
    BangumiTask createArchiveSyncTask();

    /** 创建业务导入任务，将指定 Bangumi 条目导入到 media 表。 */
    BangumiTask createBusinessImportTask(List<Long> bangumiIds);

    /** 获取任务列表（限制最大 100 条）。 */
    List<BangumiTask> listTasks(int limit);
}
