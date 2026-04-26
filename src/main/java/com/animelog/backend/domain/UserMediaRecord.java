package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户媒体观看/阅读记录实体，映射 user_media_record 表。
 */
@Data
@TableName("user_media_record")
public class UserMediaRecord {
    private Long id;                        // 主键 ID
    private Long userId;                     // 用户 ID
    private Long mediaId;                    // 媒体条目 ID
    private String status;                   // 观看状态：watching（在看）、completed（已看完）、dropped（弃番）等
    private Integer progress;                // 进度（当前看到第几集/第几话）
    private Integer rating;                  // 用户评分（1-10）
    private LocalDateTime createdAt;         // 创建时间
    private LocalDateTime updatedAt;         // 更新时间
    @TableLogic
    private Integer isDeleted;               // 逻辑删除标志（0=正常，1=已删除）
}
