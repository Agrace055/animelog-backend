package com.animelog.backend.dto;

import lombok.Data;

/**
 * 媒体角色视图对象。
 */
@Data
public class MediaCharacterVO {
    private String id;
    private String name;
    private String cvName;
    private String avatarUrl;
}
