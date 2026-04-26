package com.animelog.backend.controller;

import com.animelog.backend.dto.AjaxResult;
import com.animelog.backend.dto.MediaQueryRequest;
import com.animelog.backend.dto.MediaSearchRequest;
import com.animelog.backend.service.MediaService;
import org.springframework.web.bind.annotation.*;

/**
 * 媒体条目控制器，提供媒体列表、搜索、详情等公开查询接口。<br>
 * 映射 /api/v1/media 路径。
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * 分页查询媒体条目列表，支持按类型、年份、状态过滤及排序。
     */
    @GetMapping
    public AjaxResult list(MediaQueryRequest request) {
        return AjaxResult.success(mediaService.list(request));
    }

    /**
     * 关键词搜索媒体条目，在标题、原标题和简介中匹配。
     */
    @GetMapping("/search")
    public AjaxResult search(MediaSearchRequest request) {
        return AjaxResult.success(mediaService.search(request));
    }

    /**
     * 根据 ID 获取媒体条目详情。
     *
     * @param id 媒体条目 ID
     * @return 媒体详情
     */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return AjaxResult.success(mediaService.detail(id));
    }
}
