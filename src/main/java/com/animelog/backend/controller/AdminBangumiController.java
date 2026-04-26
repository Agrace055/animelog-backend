package com.animelog.backend.controller;

import com.animelog.backend.dto.AjaxResult;
import com.animelog.backend.dto.BangumiSourceQueryRequest;
import com.animelog.backend.service.BangumiTaskService;
import com.animelog.backend.service.BangumiSourceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Bangumi 数据管理控制器，提供存档同步、数据导入和源条目查询等管理功能。
 */
@RestController
@RequestMapping("/api/v1/admin/bangumi")
public class AdminBangumiController {
    private final BangumiTaskService taskService;
    private final BangumiSourceService sourceService;

    public AdminBangumiController(BangumiTaskService taskService, BangumiSourceService sourceService) {
        this.taskService = taskService;
        this.sourceService = sourceService;
    }

    /**
     * 创建存档同步任务，解析服务器中已上传的数据归档。
     */
    @PostMapping("/tasks/archive-sync")
    public AjaxResult createArchiveSyncTask() {
        return AjaxResult.success(taskService.createArchiveSyncTask());
    }

    /**
     * 上传 Bangumi 存档压缩包，并创建解析任务。
     */
    @PostMapping("/archive/upload")
    public AjaxResult uploadArchive(@RequestParam("file") MultipartFile file) {
        return AjaxResult.success(taskService.uploadArchiveAndCreateSyncTask(file));
    }

    /**
     * 创建业务导入任务，将指定 Bangumi 条目导入到 media 表。
     */
    @PostMapping("/tasks/import")
    public AjaxResult createImportTask(@RequestBody ImportRequest request) {
        return AjaxResult.success(taskService.createBusinessImportTask(request.bangumiIds()));
    }

    /**
     * 获取任务列表，按创建时间倒序排列。
     */
    @GetMapping("/tasks")
    public AjaxResult tasks(@RequestParam(defaultValue = "50") int limit) {
        return AjaxResult.success(taskService.listTasks(limit));
    }

    /**
     * 分页查询 Bangumi 源条目，支持关键词、类型、年份过滤。
     */
    @GetMapping("/sources")
    public AjaxResult sources(BangumiSourceQueryRequest request) {
        return AjaxResult.success(sourceService.sources(request));
    }

    /**
     * 导入请求参数，指定需要导入的 Bangumi 条目 ID 列表。
     */
    public record ImportRequest(List<Long> bangumiIds) {
    }
}
