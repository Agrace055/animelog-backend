package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bangumi 任务步骤实体，映射 bangumi_task_step 表。<br>
 * 记录任务中各步骤的执行状态。
 */
@Data
@TableName("bangumi_task_step")
public class BangumiTaskStep {
    private Long id;                        // 主键 ID
    private Long taskId;                    // 所属任务 ID
    private Integer stepOrder;              // 步骤序号
    private String stepName;                // 步骤名称：load_uploaded_archive、parse_and_upsert 等
    private String status;                  // 步骤状态：running、success、failed
    private String errorMessage;            // 错误信息
    private LocalDateTime startedAt;        // 步骤开始时间
    private LocalDateTime finishedAt;       // 步骤结束时间
}
