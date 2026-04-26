package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 媒体条目实体，映射 media 表。<br>
 * 涵盖动漫、小说、音乐等所有媒体类型的统一模型。
 */
@Data
@TableName("media")
public class Media {
    private Long id;                              // 主键 ID
    private String type;                          // 媒体类型：anime（动画）、novel（小说）、music（音乐）等
    private String title;                         // 标题
    private String originalTitle;                 // 原始标题（日文/英文原名）
    private String summary;                       // 简介/概要
    private String coverObjectKey;                // 封面在 MinIO 中的对象键
    private String coverSourceUrl;                // 封面来源 URL（Bangumi 等）
    private String coverUrl;                      // 封面最终访问 URL
    private Integer year;                         // 发布年份
    private Integer episodeCount;                 // 集数（动画）
    private Integer volumeCount;                  // 卷数（小说/漫画）
    private Integer chapterCount;                 // 话数（漫画）
    private String status;                        // 播出/出版状态：aired（已完结）、airing（连载中）等
    private BigDecimal score;                     // 综合评分
    private Integer scoreCount;                   // 评分人数
    private Boolean nsfw;                         // 是否包含 NSFW 内容
    private Boolean manualOverride;               // 是否已人工覆盖修正
    private String sourceType;                    // 数据来源类型：bangumi、manual 等
    private String sourceId;                      // 来源系统中的唯一标识
    private String sourceHash;                    // 来源数据的哈希值，用于去重
    private LocalDateTime createdAt;              // 创建时间
    private LocalDateTime updatedAt;              // 更新时间
    @TableLogic
    private Integer isDeleted;                    // 逻辑删除标志（0=正常，1=已删除）
}
