package com.animelog.backend.dto;

import lombok.Data;

/**
 * 媒体搜索查询参数。
 */
@Data
public class MediaSearchRequest {
    private String keyword;
    private String type;
    private Integer year;
    private boolean nsfw = false;
    private int page = 1;
    private int size = 20;
}
