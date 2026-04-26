package com.animelog.backend.service;

import com.animelog.backend.domain.BangumiSourceSubject;
import com.animelog.backend.dto.BangumiSourceQueryRequest;
import com.animelog.backend.dto.PageResult;

/**
 * Bangumi 源条目业务接口，提供源数据的查询功能。
 */
public interface BangumiSourceService {
    /**
     * 分页查询 Bangumi 源条目。
     *
     * @param request 查询参数（关键词、媒体类型、年份、NSFW 过滤、分页）
     * @return 分页结果
     */
    PageResult<BangumiSourceSubject> sources(BangumiSourceQueryRequest request);
}
