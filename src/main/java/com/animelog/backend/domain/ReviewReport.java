package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价举报实体，映射 review_report 表。
 */
@Data
@TableName("review_report")
public class ReviewReport {
    private Long id;                        // 主键 ID
    private Long reviewId;                  // 被举报的评价 ID
    private Long reporterUserId;            // 举报人用户 ID
    private String reason;                   // 举报原因
    private String status;                   // 处理状态：pending（待处理）、approved（已采纳）、rejected（已驳回）
    private Long handledBy;                  // 处理人（管理员用户 ID）
    private LocalDateTime handledAt;         // 处理时间
    private LocalDateTime createdAt;         // 创建时间
}
