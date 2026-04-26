package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价点赞/踩记录实体，映射 review_reaction 表。
 */
@Data
@TableName("review_reaction")
public class ReviewReaction {
    private Long id;
    private Long reviewId;
    private Long userId;
    /** 反应类型：like（点赞）或 dislike（点踩）。 */
    private String reaction;
    private LocalDateTime createdAt;
}
