package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 媒体标签实体，映射 media_tag 表。<br>
 * 存储去重后的标签名称，通过 media_tag_relation 与媒体条目关联。
 */
@Data
@TableName("media_tag")
public class MediaTag {
    private Long id;                // 主键 ID
    private String tagName;         // 标签名称（唯一）
    private Integer isDeleted;      // 逻辑删除标志（0=正常，1=已删除）
}
