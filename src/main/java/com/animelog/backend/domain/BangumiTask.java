package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bangumi 任务实体，映射 bangumi_task 表。<br>
 * 记录存档同步和数据导入的后台任务信息。
 */
@Data
@TableName("bangumi_task")
public class BangumiTask {
    private Long id;                        // 主键 ID
    private String taskType;                // 任务类型：archive_sync（存档同步）、business_import（业务导入）
    private String status;                  // 任务状态：pending（待执行）、running（执行中）、success（完成）、failed（失败）
    private String currentStep;             // 当前执行步骤
    private String requestPayload;          // 请求参数 JSON
    private Integer totalCount;             // 总处理数
    private Integer successCount;           // 成功数
    private Integer skipCount;              // 跳过数
    private Integer updateCount;            // 更新数
    private Integer failureCount;           // 失败数
    private String errorMessage;            // 错误信息
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime startedAt;        // 开始执行时间
    private LocalDateTime finishedAt;       // 完成时间
    private LocalDateTime updatedAt;        // 更新时间
}
