package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bangumi 存档文件实体，映射 bangumi_archive_file 表。<br>
 * 记录从 Bangumi 下载的数据归档文件信息，用于去重和溯源。
 */
@Data
@TableName("bangumi_archive_file")
public class BangumiArchiveFile {
    private Long id;                        // 主键 ID
    private String filename;                 // 文件名
    private Long fileSize;                   // 文件大小（字节）
    private LocalDate dumpDate;              // 数据导出日期
    private String fileHash;                 // 文件 SHA-256 哈希
    private String storagePath;              // 本地存储路径
    private String status;                   // 状态：downloaded（已下载）、imported（已导入）
    private LocalDateTime createdAt;         // 创建时间
}
