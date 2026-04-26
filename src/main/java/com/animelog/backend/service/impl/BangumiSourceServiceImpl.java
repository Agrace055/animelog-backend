package com.animelog.backend.service.impl;

import com.animelog.backend.domain.BangumiSourceSubject;
import com.animelog.backend.dto.BangumiSourceQueryRequest;
import com.animelog.backend.dto.PageResult;
import com.animelog.backend.mapper.BangumiSourceSubjectMapper;
import com.animelog.backend.service.BangumiSourceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

/**
 * Bangumi 源条目业务实现。
 */
@Service
public class BangumiSourceServiceImpl implements BangumiSourceService {
    private final BangumiSourceSubjectMapper sourceMapper;

    public BangumiSourceServiceImpl(BangumiSourceSubjectMapper sourceMapper) {
        this.sourceMapper = sourceMapper;
    }

    @Override
    public PageResult<BangumiSourceSubject> sources(BangumiSourceQueryRequest request) {
        QueryWrapper<BangumiSourceSubject> query = new QueryWrapper<>();
        if (request.getQ() != null && !request.getQ().isBlank()) {
            query.and(w -> w.like("name", request.getQ()).or().like("name_cn", request.getQ()));
        }
        if (request.getMediaType() != null && !request.getMediaType().isBlank()) {
            query.eq("media_type", request.getMediaType());
        }
        if (request.getYear() != null) {
            query.eq("year", request.getYear());
        }
        if (!request.isIncludeNsfw()) {
            query.eq("nsfw", false);
        }
        query.orderByDesc("score");
        Page<BangumiSourceSubject> p = sourceMapper.selectPage(Page.of(request.getPage(), request.getSize()), query);
        return PageResult.of((int) p.getCurrent(), (int) p.getSize(), p.getTotal(), p.getRecords());
    }
}
