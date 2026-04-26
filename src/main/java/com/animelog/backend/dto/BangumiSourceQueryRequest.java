package com.animelog.backend.dto;

import lombok.Data;

/**
 * Bangumi 源条目查询参数。
 */
@Data
public class BangumiSourceQueryRequest {
    private String keyword;
    private String mediaType;
    private Integer year;
    private boolean nsfw = false;
    private int page = 1;
    private int size = 20;
}
