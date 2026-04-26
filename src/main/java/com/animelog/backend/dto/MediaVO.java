package com.animelog.backend.dto;

import com.animelog.backend.domain.Media;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 媒体条目视图对象，暴露前端所需字段，屏蔽 coverObjectKey、sourceHash 等内部字段。
 */
@Data
public class MediaVO {
    private Long id;
    private String type;
    private String title;
    private String originalTitle;
    private String summary;
    private String coverUrl;
    private String coverSourceUrl;
    private Integer year;
    private Integer episodeCount;
    private Integer volumeCount;
    private Integer chapterCount;
    private String status;
    private BigDecimal score;
    private Integer scoreCount;
    private Boolean nsfw;
    /** 标签列表（当前暂无专用表，返回空列表）。 */
    private List<String> tags = new ArrayList<>();
    /** 角色列表（当前暂无专用表，返回空列表）。 */
    private List<MediaCharacterVO> characters = new ArrayList<>();
    /** 制作人员列表（当前暂无专用表，返回空列表）。 */
    private List<MediaStaffVO> staff = new ArrayList<>();

    /** 从 Media 实体转换。 */
    public static MediaVO from(Media media) {
        if (media == null) return null;
        MediaVO vo = new MediaVO();
        vo.setId(media.getId());
        vo.setType(media.getType());
        vo.setTitle(media.getTitle());
        vo.setOriginalTitle(media.getOriginalTitle());
        vo.setSummary(media.getSummary());
        vo.setCoverUrl(media.getCoverUrl());
        vo.setCoverSourceUrl(media.getCoverSourceUrl());
        vo.setYear(media.getYear());
        vo.setEpisodeCount(media.getEpisodeCount());
        vo.setVolumeCount(media.getVolumeCount());
        vo.setChapterCount(media.getChapterCount());
        vo.setStatus(media.getStatus());
        vo.setScore(media.getScore());
        vo.setScoreCount(media.getScoreCount());
        vo.setNsfw(media.getNsfw());
        return vo;
    }
}
