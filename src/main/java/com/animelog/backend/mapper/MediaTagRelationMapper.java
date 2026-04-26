package com.animelog.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 媒体-标签关联 Mapper（复合主键表，不继承 BaseMapper）。
 * 提供关联关系的写入与清除操作。
 */
public interface MediaTagRelationMapper {

    /**
     * 插入媒体与标签的关联关系，若已存在则忽略（ON CONFLICT DO NOTHING）。
     */
    @Insert("INSERT INTO media_tag_relation (media_id, tag_id) VALUES (#{mediaId}, #{tagId}) ON CONFLICT DO NOTHING")
    void upsert(@Param("mediaId") Long mediaId, @Param("tagId") Long tagId);

    /**
     * 删除指定媒体的所有标签关联，用于标签更新时先清除再重建。
     */
    @Delete("DELETE FROM media_tag_relation WHERE media_id = #{mediaId}")
    void deleteByMediaId(@Param("mediaId") Long mediaId);
}
