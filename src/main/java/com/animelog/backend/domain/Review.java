package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户评价实体，映射 review 表。
 */
@Data
@TableName("review")
public class Review {
    private Long id;                        // 主键 ID
    private Long mediaId;                    // 关联的媒体条目 ID
    private Long userId;                     // 评价用户 ID
    private String content;                  // 评价内容
    private Integer rating;                  // 评分（1-10）
    private String status;                   // 状态：approved（已通过）、pending（审核中）、rejected（已驳回）
    private Integer likeCount;               // 点赞数
    private Integer dislikeCount;            // 点踩数
    private Boolean reported;                // 是否被举报
    private LocalDateTime createdAt;         // 创建时间
    private LocalDateTime updatedAt;         // 更新时间
    @TableLogic
    private Integer isDeleted;               // 逻辑删除标志（0=正常，1=已删除）
}
