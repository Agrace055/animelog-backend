package com.animelog.backend.mapper;

import com.animelog.backend.domain.MediaTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 媒体标签 Mapper，提供 media_tag 表的 CRUD 及关联查询操作。
 */
public interface MediaTagMapper extends BaseMapper<MediaTag> {

    /**
     * 按名称查找标签，仅返回未删除记录。
     */
    @Select("SELECT id, tag_name, is_deleted FROM media_tag WHERE tag_name = #{tagName} AND is_deleted = 0 LIMIT 1")
    MediaTag findByName(@Param("tagName") String tagName);

    /**
     * 忽略冲突插入标签（tag_name 唯一约束），用于并发安全的 upsert。
     * 若同名标签已存在则不报错。
     */
    @Insert("INSERT INTO media_tag (tag_name, is_deleted) VALUES (#{tagName}, 0) ON CONFLICT (tag_name) DO NOTHING")
    int insertIgnore(@Param("tagName") String tagName);

    /**
     * 查询指定媒体的所有标签名称列表。
     */
    @Select("SELECT mt.tag_name FROM media_tag mt " +
            "JOIN media_tag_relation mtr ON mt.id = mtr.tag_id " +
            "WHERE mtr.media_id = #{mediaId} AND mt.is_deleted = 0 " +
            "ORDER BY mt.tag_name")
    List<String> findTagNamesByMediaId(@Param("mediaId") Long mediaId);
}
