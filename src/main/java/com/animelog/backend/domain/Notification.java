package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体，映射 notification 表。
 */
@Data
@TableName("notification")
public class Notification {
    private Long id;                        // 主键 ID
    private String title;                   // 通知标题
    private String content;                 // 通知内容
    private String targetType;              // 目标类型：all（全部用户）、user（指定用户）
    private Long targetUserId;              // 目标用户 ID（targetType=user 时有效）
    private LocalDateTime createdAt;        // 创建时间
}
