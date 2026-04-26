package com.animelog.backend.service;

import com.animelog.backend.dto.MediaQueryRequest;
import com.animelog.backend.dto.MediaSearchRequest;
import com.animelog.backend.dto.MediaVO;
import com.animelog.backend.dto.PageResult;

/**
 * 媒体条目业务接口。
 */
public interface MediaService {
    /**
     * 分页查询媒体列表，支持多种过滤条件和排序方式。
     *
     * @param request 查询参数（类型、年份、状态、NSFW 过滤、排序、分页）
     * @return 分页结果
     */
    PageResult<MediaVO> list(MediaQueryRequest request);

    /**
     * 关键词搜索媒体条目。
     *
     * @param request 搜索参数（关键词、类型、NSFW 过滤、分页）
     * @return 分页结果
     */
    PageResult<MediaVO> search(MediaSearchRequest request);

    /**
     * 根据 ID 获取媒体详情。
     */
    MediaVO detail(Long id);
}
