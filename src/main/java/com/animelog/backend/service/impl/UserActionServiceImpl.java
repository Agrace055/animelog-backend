package com.animelog.backend.service.impl;

import com.animelog.backend.domain.*;
import com.animelog.backend.dto.NotificationVO;
import com.animelog.backend.dto.UpdateProfileRequest;
import com.animelog.backend.dto.UserVO;
import com.animelog.backend.mapper.*;
import com.animelog.backend.service.UserActionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户操作业务实现。
 */
@Service
public class UserActionServiceImpl implements UserActionService {
    private final UserMediaRecordMapper recordMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewReportMapper reportMapper;
    private final ReviewReactionMapper reactionMapper;
    private final NotificationMapper notificationMapper;
    private final UserNotificationStateMapper notificationStateMapper;
    private final CalendarItemMapper calendarMapper;
    private final UserFeedbackMapper feedbackMapper;
    private final NsfwAccessApplicationMapper nsfwMapper;
    private final UserAccountMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserActionServiceImpl(
        UserMediaRecordMapper recordMapper,
        UserFavoriteMapper favoriteMapper,
        ReviewMapper reviewMapper,
        ReviewReportMapper reportMapper,
        ReviewReactionMapper reactionMapper,
        NotificationMapper notificationMapper,
        UserNotificationStateMapper notificationStateMapper,
        CalendarItemMapper calendarMapper,
        UserFeedbackMapper feedbackMapper,
        NsfwAccessApplicationMapper nsfwMapper,
        UserAccountMapper userMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.recordMapper = recordMapper;
        this.favoriteMapper = favoriteMapper;
        this.reviewMapper = reviewMapper;
        this.reportMapper = reportMapper;
        this.reactionMapper = reactionMapper;
        this.notificationMapper = notificationMapper;
        this.notificationStateMapper = notificationStateMapper;
        this.calendarMapper = calendarMapper;
        this.feedbackMapper = feedbackMapper;
        this.nsfwMapper = nsfwMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserMediaRecord> records(Long userId) {
        return recordMapper.selectList(new QueryWrapper<UserMediaRecord>()
            .eq("user_id", userId).orderByDesc("updated_at"));
    }

    @Override
    public UserMediaRecord saveRecord(Long userId, UserMediaRecord record) {
        record.setUserId(userId);
        if (record.getId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }
        return record;
    }

    @Override
    public void toggleFavorite(Long userId, Long mediaId) {
        UserFavorite existing = favoriteMapper.selectOne(new QueryWrapper<UserFavorite>()
            .eq("user_id", userId).eq("media_id", mediaId).last("LIMIT 1"));
        if (existing == null) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setMediaId(mediaId);
            favoriteMapper.insert(favorite);
        } else {
            favoriteMapper.deleteById(existing.getId());
        }
    }

    @Override
    public List<Long> favorites(Long userId) {
        return favoriteMapper.selectList(new QueryWrapper<UserFavorite>()
                .eq("user_id", userId).select("media_id"))
            .stream().map(UserFavorite::getMediaId).collect(Collectors.toList());
    }

    @Override
    public List<Review> reviews(Long mediaId) {
        return reviewMapper.selectList(new QueryWrapper<Review>()
            .eq("media_id", mediaId).eq("status", "approved").orderByDesc("created_at"));
    }

    @Override
    public Review createReview(Long mediaId, Review review) {
        review.setMediaId(mediaId);
        review.setStatus("approved");
        review.setLikeCount(0);
        review.setDislikeCount(0);
        review.setReported(false);
        reviewMapper.insert(review);
        return review;
    }

    @Override
    public void reportReview(Long reviewId, ReviewReport report) {
        report.setReviewId(reviewId);
        report.setStatus("pending");
        reportMapper.insert(report);
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            review.setReported(true);
            reviewMapper.updateById(review);
        }
    }

    @Override
    public void likeReview(Long reviewId, Long userId) {
        ReviewReaction existing = reactionMapper.selectOne(new QueryWrapper<ReviewReaction>()
            .eq("review_id", reviewId).eq("user_id", userId).last("LIMIT 1"));
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return;

        if (existing == null) {
            // 无记录：新增点赞
            ReviewReaction reaction = new ReviewReaction();
            reaction.setReviewId(reviewId);
            reaction.setUserId(userId);
            reaction.setReaction("like");
            reactionMapper.insert(reaction);
            review.setLikeCount(review.getLikeCount() + 1);
        } else if ("dislike".equals(existing.getReaction())) {
            // 已点踩：切换为点赞
            existing.setReaction("like");
            reactionMapper.updateById(existing);
            review.setDislikeCount(Math.max(0, review.getDislikeCount() - 1));
            review.setLikeCount(review.getLikeCount() + 1);
        } else {
            // 已点赞：取消
            reactionMapper.deleteById(existing.getId());
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
        }
        reviewMapper.updateById(review);
    }

    @Override
    public void dislikeReview(Long reviewId, Long userId) {
        ReviewReaction existing = reactionMapper.selectOne(new QueryWrapper<ReviewReaction>()
            .eq("review_id", reviewId).eq("user_id", userId).last("LIMIT 1"));
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return;

        if (existing == null) {
            // 无记录：新增点踩
            ReviewReaction reaction = new ReviewReaction();
            reaction.setReviewId(reviewId);
            reaction.setUserId(userId);
            reaction.setReaction("dislike");
            reactionMapper.insert(reaction);
            review.setDislikeCount(review.getDislikeCount() + 1);
        } else if ("like".equals(existing.getReaction())) {
            // 已点赞：切换为点踩
            existing.setReaction("dislike");
            reactionMapper.updateById(existing);
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
            review.setDislikeCount(review.getDislikeCount() + 1);
        } else {
            // 已点踩：取消
            reactionMapper.deleteById(existing.getId());
            review.setDislikeCount(Math.max(0, review.getDislikeCount() - 1));
        }
        reviewMapper.updateById(review);
    }

    @Override
    public List<NotificationVO> notifications(Long userId) {
        List<Notification> all = notificationMapper.selectList(
            new QueryWrapper<Notification>().orderByDesc("created_at"));

        // 查询该用户的所有已读状态，构成 Map<notificationId, read>
        Map<Long, Boolean> readMap = notificationStateMapper.selectList(
                new QueryWrapper<UserNotificationState>().eq("user_id", userId))
            .stream()
            .collect(Collectors.toMap(UserNotificationState::getNotificationId, UserNotificationState::getRead));

        return all.stream().map(n -> {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setIsRead(readMap.getOrDefault(n.getId(), false));
            vo.setCreatedAt(n.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void markNotificationRead(Long userId, Long notificationId) {
        UserNotificationState state = notificationStateMapper.selectOne(
            new QueryWrapper<UserNotificationState>()
                .eq("user_id", userId).eq("notification_id", notificationId).last("LIMIT 1"));
        if (state == null) {
            state = new UserNotificationState();
            state.setUserId(userId);
            state.setNotificationId(notificationId);
            state.setRead(true);
            state.setReadAt(LocalDateTime.now());
            notificationStateMapper.insert(state);
        } else if (!Boolean.TRUE.equals(state.getRead())) {
            state.setRead(true);
            state.setReadAt(LocalDateTime.now());
            notificationStateMapper.updateById(state);
        }
    }

    @Override
    public void markAllNotificationsRead(Long userId) {
        List<Notification> all = notificationMapper.selectList(null);
        // 查询已有状态记录
        Set<Long> alreadyRead = notificationStateMapper.selectList(
                new QueryWrapper<UserNotificationState>().eq("user_id", userId).eq("read", true))
            .stream().map(UserNotificationState::getNotificationId).collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        for (Notification n : all) {
            if (!alreadyRead.contains(n.getId())) {
                // 检查是否存在但未读
                UserNotificationState existing = notificationStateMapper.selectOne(
                    new QueryWrapper<UserNotificationState>()
                        .eq("user_id", userId).eq("notification_id", n.getId()).last("LIMIT 1"));
                if (existing == null) {
                    UserNotificationState state = new UserNotificationState();
                    state.setUserId(userId);
                    state.setNotificationId(n.getId());
                    state.setRead(true);
                    state.setReadAt(now);
                    notificationStateMapper.insert(state);
                } else {
                    existing.setRead(true);
                    existing.setReadAt(now);
                    notificationStateMapper.updateById(existing);
                }
            }
        }
    }

    @Override
    public List<CalendarItem> calendar() {
        return calendarMapper.selectList(new QueryWrapper<CalendarItem>()
            .orderByAsc("day_of_week").orderByAsc("air_time"));
    }

    @Override
    public UserFeedback feedback(UserFeedback feedback) {
        feedback.setStatus("pending");
        feedbackMapper.insert(feedback);
        return feedback;
    }

    @Override
    public NsfwAccessApplication applyNsfw(NsfwAccessApplication application) {
        application.setStatus("pending");
        nsfwMapper.insert(application);
        return application;
    }

    @Override
    public UserVO userProfile(Long userId) {
        return UserVO.from(userMapper.selectById(userId));
    }

    @Override
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserAccount user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
