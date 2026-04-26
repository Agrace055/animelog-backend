package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日历条目实体，映射 calendar_item 表。<br>
 * 用于追番日历，记录每周各动画的播出时间和集数。
 */
@Data
@TableName("calendar_item")
public class CalendarItem {
    private Long id;                        // 主键 ID
    private Long mediaId;                   // 媒体条目 ID
    private Integer dayOfWeek;              // 星期几（1=周一, 7=周日）
    private String airTime;                 // 播出时间（HH:mm 格式）
    private Integer episode;                // 当前集数
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
    @TableLogic
    private Integer isDeleted;              // 逻辑删除标志（0=正常，1=已删除）
}
