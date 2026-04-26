package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 媒体-标签关联实体，映射 media_tag_relation 表（复合主键，无自增 id）。
 */
@Data
@TableName("media_tag_relation")
public class MediaTagRelation {
    private Long mediaId;   // 媒体 ID
    private Long tagId;     // 标签 ID
}
