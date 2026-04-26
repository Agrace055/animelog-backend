package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知状态实体，映射 user_notification_state 表。<br>
 * 记录每个用户对每一条通知的已读状态。
 */
@Data
@TableName("user_notification_state")
public class UserNotificationState {
    private Long id;                        // 主键 ID
    private Long userId;                    // 用户 ID
    private Long notificationId;            // 通知 ID
    private Boolean read;                   // 是否已读
    private LocalDateTime readAt;           // 阅读时间
}
