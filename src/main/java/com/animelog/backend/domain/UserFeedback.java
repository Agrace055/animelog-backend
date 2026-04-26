package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈实体，映射 user_feedback 表。
 */
@Data
@TableName("user_feedback")
public class UserFeedback {
    private Long id;                        // 主键 ID
    private Long userId;                    // 用户 ID
    private String type;                    // 反馈类型：suggestion（建议）、bug（问题反馈）、complaint（投诉）
    private String content;                 // 反馈内容
    private String status;                  // 处理状态：pending（待处理）、resolved（已解决）、closed（已关闭）
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
}
