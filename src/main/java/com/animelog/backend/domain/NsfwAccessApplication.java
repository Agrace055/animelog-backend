package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * NSFW 内容访问申请实体，映射 nsfw_access_application 表。<br>
 * 用户申请解锁 NSFW 内容时提交的审核申请。
 */
@Data
@TableName("nsfw_access_application")
public class NsfwAccessApplication {
    private Long id;                        // 主键 ID
    private Long userId;                     // 申请用户 ID
    private String reason;                   // 申请理由
    private String status;                   // 审核状态：pending（待审核）、approved（已通过）、rejected（已驳回）
    private Long reviewedBy;                 // 审核人（管理员用户 ID）
    private LocalDateTime reviewedAt;        // 审核时间
    private LocalDateTime createdAt;         // 创建时间
}
