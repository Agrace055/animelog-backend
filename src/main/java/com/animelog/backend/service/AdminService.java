package com.animelog.backend.service;

import com.animelog.backend.domain.*;

import java.util.List;

/**
 * 管理员业务接口，提供媒体管理、评价审核、日历和 NSFW 审核功能。
 */
public interface AdminService {
    /** 管理员更新媒体条目（标记为人工覆盖）。 */
    Media updateMedia(Long id, Media media);

    /** 获取所有被举报的评价。 */
    List<Review> reportedReviews();

    /** 批准举报，清除评价的被举报标记。 */
    void approveReport(Long reviewId);

    /** 逻辑删除评价。 */
    void deleteReview(Long reviewId);

    /** 创建追番日历条目。 */
    CalendarItem createCalendarItem(CalendarItem item);

    /** 更新追番日历条目。 */
    CalendarItem updateCalendarItem(Long id, CalendarItem item);

    /** 逻辑删除追番日历条目。 */
    void deleteCalendarItem(Long id);

    /** 获取所有待审核的 NSFW 访问申请。 */
    List<NsfwAccessApplication> nsfwApplications();

    /** 审核 NSFW 访问申请。 */
    void reviewNsfw(Long id, String action, Long reviewerId);

    /** 获取所有用户反馈，按创建时间倒序。 */
    List<UserFeedback> listFeedbacks();

    /** 更新反馈处理状态（pending / resolved / closed）。 */
    void updateFeedbackStatus(Long id, String status);
}
