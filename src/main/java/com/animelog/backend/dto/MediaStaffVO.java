package com.animelog.backend.dto;

import lombok.Data;

/**
 * 媒体制作人员视图对象。
 */
@Data
public class MediaStaffVO {
    private String id;
    private String name;
    private String role;
    private String avatarUrl;
}
