package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏实体，映射 user_favorite 表。<br>
 * 记录用户对媒体条目的收藏关系。
 */
@Data
@TableName("user_favorite")
public class UserFavorite {
    private Long id;                        // 主键 ID
    private Long userId;                     // 用户 ID
    private Long mediaId;                    // 媒体条目 ID
    private LocalDateTime createdAt;         // 创建时间
    @TableLogic
    private Integer isDeleted;               // 逻辑删除标志（0=已收藏，1=已取消收藏）
}
