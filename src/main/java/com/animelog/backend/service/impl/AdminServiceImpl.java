package com.animelog.backend.service.impl;

import com.animelog.backend.domain.*;
import com.animelog.backend.mapper.*;
import com.animelog.backend.service.AdminService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员业务实现。
 */
@Service
public class AdminServiceImpl implements AdminService {
    private final MediaMapper mediaMapper;
    private final ReviewMapper reviewMapper;
    private final CalendarItemMapper calendarMapper;
    private final NsfwAccessApplicationMapper nsfwMapper;
    private final UserAccountMapper userMapper;
    private final UserFeedbackMapper feedbackMapper;

    public AdminServiceImpl(
        MediaMapper mediaMapper,
        ReviewMapper reviewMapper,
        CalendarItemMapper calendarMapper,
        NsfwAccessApplicationMapper nsfwMapper,
        UserAccountMapper userMapper,
        UserFeedbackMapper feedbackMapper
    ) {
        this.mediaMapper = mediaMapper;
        this.reviewMapper = reviewMapper;
        this.calendarMapper = calendarMapper;
        this.nsfwMapper = nsfwMapper;
        this.userMapper = userMapper;
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    public Media updateMedia(Long id, Media media) {
        // 设置 ID 并标记为人工覆盖，避免被后续自动同步覆盖
        media.setId(id);
        media.setManualOverride(true);
        mediaMapper.updateById(media);
        return mediaMapper.selectById(id);
    }

    @Override
    public List<Review> reportedReviews() {
        // 查询所有被举报且未删除的评价，按创建时间倒序（is_deleted=0 由 @TableLogic 自动附加）
        return reviewMapper.selectList(new QueryWrapper<Review>()
            .eq("reported", true).orderByDesc("created_at"));
    }

    @Override
    public void approveReport(Long reviewId) {
        // 清除评价的被举报标记
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            review.setReported(false);
            reviewMapper.updateById(review);
        }
    }

    @Override
    public void deleteReview(Long reviewId) {
        // 逻辑删除评价（MyBatis-Plus 自动执行 UPDATE SET is_deleted=1）
        reviewMapper.deleteById(reviewId);
    }

    @Override
    public CalendarItem createCalendarItem(CalendarItem item) {
        // 初始化日历条目的基本字段（is_deleted 由 DB DEFAULT 0 自动设置）
        calendarMapper.insert(item);
        return item;
    }

    @Override
    public CalendarItem updateCalendarItem(Long id, CalendarItem item) {
        item.setId(id);
        calendarMapper.updateById(item);
        return calendarMapper.selectById(id);
    }

    @Override
    public void deleteCalendarItem(Long id) {
        // 逻辑删除日历条目（MyBatis-Plus 自动执行 UPDATE SET is_deleted=1）
        calendarMapper.deleteById(id);
    }

    @Override
    public List<NsfwAccessApplication> nsfwApplications() {
        // 查询所有待审核的 NSFW 申请，按创建时间升序
        return nsfwMapper.selectList(new QueryWrapper<NsfwAccessApplication>()
            .eq("status", "pending").orderByAsc("created_at"));
    }

    @Override
    public void reviewNsfw(Long id, String action, Long reviewerId) {
        // 查询申请记录
        NsfwAccessApplication app = nsfwMapper.selectById(id);
        if (app == null) {
            return;
        }
        // 根据审核操作更新申请状态
        String status = "approve".equals(action) ? "approved" : "rejected";
        app.setStatus(status);
        app.setReviewedBy(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        nsfwMapper.updateById(app);
        // 如果审核通过，更新用户的 NSFW 访问权限
        if ("approved".equals(status)) {
            UserAccount user = userMapper.selectById(app.getUserId());
            if (user != null) {
                user.setNsfwStatus("approved");
                userMapper.updateById(user);
            }
        }
    }

    @Override
    public List<UserFeedback> listFeedbacks() {
        return feedbackMapper.selectList(
            new QueryWrapper<UserFeedback>().orderByDesc("created_at"));
    }

    @Override
    public void updateFeedbackStatus(Long id, String status) {
        UserFeedback fb = feedbackMapper.selectById(id);
        if (fb != null) {
            fb.setStatus(status);
            feedbackMapper.updateById(fb);
        }
    }
}
