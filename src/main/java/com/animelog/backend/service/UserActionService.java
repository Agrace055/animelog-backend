package com.animelog.backend.service;

import com.animelog.backend.domain.*;
import com.animelog.backend.dto.NotificationVO;
import com.animelog.backend.dto.UpdateProfileRequest;
import com.animelog.backend.dto.UserVO;

import java.util.List;

/**
 * 用户操作业务接口，提供追番记录、收藏、评价、通知、日历、反馈、NSFW 申请等功能。
 */
public interface UserActionService {
    /** 获取用户的媒体记录列表。 */
    List<UserMediaRecord> records(Long userId);

    /** 新增或更新媒体记录。 */
    UserMediaRecord saveRecord(Long userId, UserMediaRecord record);

    /** 切换收藏状态。 */
    void toggleFavorite(Long userId, Long mediaId);

    /** 获取用户的收藏媒体 ID 列表。 */
    List<Long> favorites(Long userId);

    /** 获取某媒体条目的已通过评价列表。 */
    List<Review> reviews(Long mediaId);

    /** 创建评价。 */
    Review createReview(Long mediaId, Review review);

    /** 举报评价。 */
    void reportReview(Long reviewId, ReviewReport report);

    /** 点赞评价（同一用户重复点赞为取消，若已点踩则切换）。 */
    void likeReview(Long reviewId, Long userId);

    /** 点踩评价（同一用户重复点踩为取消，若已点赞则切换）。 */
    void dislikeReview(Long reviewId, Long userId);

    /** 获取用户通知列表（含该用户的已读状态）。 */
    List<NotificationVO> notifications(Long userId);

    /** 标记指定通知为已读。 */
    void markNotificationRead(Long userId, Long notificationId);

    /** 标记所有通知为已读。 */
    void markAllNotificationsRead(Long userId);

    /** 获取追番日历。 */
    List<CalendarItem> calendar();

    /** 提交反馈。 */
    UserFeedback feedback(UserFeedback feedback);

    /** 提交 NSFW 访问申请。 */
    NsfwAccessApplication applyNsfw(NsfwAccessApplication application);

    /** 获取用户个人信息。 */
    UserVO userProfile(Long userId);

    /** 更新用户个人信息（nickname、email、phone、avatarUrl，null 则跳过）。 */
    UserVO updateProfile(Long userId, UpdateProfileRequest request);
}
