package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bangumi 任务项实体，映射 bangumi_task_item 表。<br>
 * 记录任务中每个条目的处理结果。
 */
@Data
@TableName("bangumi_task_item")
public class BangumiTaskItem {
    private Long id;                        // 主键 ID
    private Long taskId;                    // 所属任务 ID
    private Long bangumiId;                 // Bangumi 条目 ID
    private Long mediaId;                   // 对应 media 表 ID
    private String itemName;                // 条目名称
    private String action;                  // 操作类型：inserted（新增）、updated（更新）、skipped（跳过）
    private String status;                  // 处理状态：success、failed
    private String errorMessage;            // 错误信息
    private LocalDateTime createdAt;        // 创建时间
}
