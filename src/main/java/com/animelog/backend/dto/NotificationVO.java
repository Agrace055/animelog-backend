package com.animelog.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图对象，附带当前用户的已读状态。
 */
@Data
public class NotificationVO {
    private Long id;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
