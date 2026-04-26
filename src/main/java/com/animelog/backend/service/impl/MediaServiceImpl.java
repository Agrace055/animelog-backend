package com.animelog.backend.service.impl;

import com.animelog.backend.domain.Media;
import com.animelog.backend.dto.MediaQueryRequest;
import com.animelog.backend.dto.MediaSearchRequest;
import com.animelog.backend.dto.MediaVO;
import com.animelog.backend.dto.PageResult;
import com.animelog.backend.mapper.MediaMapper;
import com.animelog.backend.service.MediaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public PageResult<MediaVO> list(MediaQueryRequest request) {
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
        if (!request.isNsfw()) {
            query.eq("nsfw", false);
        }
        if ("rating".equals(request.getSort())) {
            query.orderByDesc("score");
        } else {
            query.orderByDesc("updated_at");
        }
        Page<Media> p = mediaMapper.selectPage(Page.of(request.getPage(), request.getSize()), query);
        List<MediaVO> records = p.getRecords().stream().map(MediaVO::from).collect(Collectors.toList());
        return PageResult.of((int) p.getCurrent(), (int) p.getSize(), p.getTotal(), records);
    }

    @Override
    public PageResult<MediaVO> search(MediaSearchRequest request) {
        QueryWrapper<Media> query = new QueryWrapper<Media>()
            .eq("is_deleted", 0)
            .and(w -> w.like("title", request.getKeyword()).or().like("original_title", request.getKeyword()).or().like("summary", request.getKeyword()));
        if (request.getType() != null && !request.getType().isBlank() && !"all".equals(request.getType())) {
            query.eq("type", request.getType());
        }
        if (request.getYear() != null) {
            query.eq("year", request.getYear());
        }
        if (!request.isNsfw()) {
            query.eq("nsfw", false);
        }
        query.orderByDesc("score");
        Page<Media> p = mediaMapper.selectPage(Page.of(request.getPage(), request.getSize()), query);
        List<MediaVO> records = p.getRecords().stream().map(MediaVO::from).collect(Collectors.toList());
        return PageResult.of((int) p.getCurrent(), (int) p.getSize(), p.getTotal(), records);
    }

    @Override
    public MediaVO detail(Long id) {
        return MediaVO.from(mediaMapper.selectById(id));
    }
}
