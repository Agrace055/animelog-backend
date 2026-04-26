package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bangumi 源条目实体，映射 bangumi_source_subject 表。<br>
 * 存储从 Bangumi 归档解析出的原始条目数据，作为导入 media 表的数据源。
 */
@Data
@TableName("bangumi_source_subject")
public class BangumiSourceSubject {
    private Long id;                        // 主键 ID
    private Long bangumiId;                 // Bangumi 系统中的条目 ID
    private Integer rawType;                // Bangumi 原始类型：1（小说）、2（动画）、4（游戏）
    private String mediaType;               // 映射后的媒体类型：novel、anime、game
    private String name;                    // 原名（日文/英文）
    private String nameCn;                  // 中文名
    private String summary;                 // 简介
    private String infobox;                 // 信息框原始 JSON
    private LocalDate airDate;              // 播出/发售日期
    private Integer year;                   // 年份
    private Integer month;                  // 月份
    private Integer episodeCount;           // 集数/卷数
    private String tagsJson;                // 标签 JSON
    private BigDecimal score;               // Bangumi 评分
    private Integer rank;                   // Bangumi 排名
    private Boolean nsfw;                   // 是否 NSFW
    private String rawJson;                 // 原始 JSON 数据
    private String sourceHash;              // 数据哈希，用于去重
    private Long importedMediaId;           // 已导入的 media 表 ID（为空表示未导入）
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
}
