package com.animelog.backend.dto;

import lombok.Data;

/**
 * 媒体搜索查询参数。
 */
@Data
public class MediaSearchRequest {
    private String q;
    private String type;
    private boolean includeNsfw = false;
    private int page = 1;
    private int size = 20;
}
