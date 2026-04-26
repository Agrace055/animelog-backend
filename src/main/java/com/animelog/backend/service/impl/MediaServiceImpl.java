package com.animelog.backend.service.impl;

import com.animelog.backend.domain.Media;
import com.animelog.backend.dto.MediaQueryRequest;
import com.animelog.backend.dto.MediaSearchRequest;
import com.animelog.backend.dto.PageResult;
import com.animelog.backend.mapper.MediaMapper;
import com.animelog.backend.service.MediaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

/**
 * 媒体条目业务实现。
 */
@Service
public class MediaServiceImpl implements MediaService {
    private final MediaMapper mediaMapper;

    public MediaServiceImpl(MediaMapper mediaMapper) {
        this.mediaMapper = mediaMapper;
    }

    @Override
    public PageResult<Media> list(MediaQueryRequest request) {
        QueryWrapper<Media> query = new QueryWrapper<Media>().eq("is_deleted", 0);
        if (request.getType() != null && !request.getType().isBlank()) {
            query.eq("type", request.getType());
        }
        if (request.getYear() != null) {
            query.eq("year", request.getYear());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            query.eq("status", request.getStatus());
        }
        if (!request.isIncludeNsfw()) {
            query.eq("nsfw", false);
        }
        if ("rating".equals(request.getSort())) {
            query.orderByDesc("score");
        } else {
            query.orderByDesc("updated_at");
        }
        Page<Media> p = mediaMapper.selectPage(Page.of(request.getPage(), request.getSize()), query);
        return PageResult.of((int) p.getCurrent(), (int) p.getSize(), p.getTotal(), p.getRecords());
    }

    @Override
    public PageResult<Media> search(MediaSearchRequest request) {
        QueryWrapper<Media> query = new QueryWrapper<Media>()
            .eq("is_deleted", 0)
            .and(w -> w.like("title", request.getQ()).or().like("original_title", request.getQ()).or().like("summary", request.getQ()));
        if (request.getType() != null && !request.getType().isBlank() && !"all".equals(request.getType())) {
            query.eq("type", request.getType());
        }
        if (!request.isIncludeNsfw()) {
            query.eq("nsfw", false);
        }
        query.orderByDesc("score");
        Page<Media> p = mediaMapper.selectPage(Page.of(request.getPage(), request.getSize()), query);
        return PageResult.of((int) p.getCurrent(), (int) p.getSize(), p.getTotal(), p.getRecords());
    }

    @Override
    public Media detail(Long id) {
        return mediaMapper.selectById(id);
    }
}
