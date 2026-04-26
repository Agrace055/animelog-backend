package com.animelog.backend.dto;

import lombok.Data;

/**
 * 媒体列表查询参数。
 */
@Data
public class MediaQueryRequest {
    private String type;
    private Integer year;
    private String status;
    private boolean includeNsfw = false;
    private String sort = "latest";
    private int page = 1;
    private int size = 20;
}
