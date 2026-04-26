package com.animelog.backend.controller;

import com.animelog.backend.domain.*;
import com.animelog.backend.dto.AjaxResult;
import com.animelog.backend.dto.UpdateProfileRequest;
import com.animelog.backend.service.UserActionService;
import org.springframework.web.bind.annotation.*;


/**
 * 用户操作控制器，提供追番记录、收藏、评价、通知、日历、反馈、NSFW 申请等用户侧功能接口。
 */
@RestController
@RequestMapping("/api/v1")
public class UserActionController {
    private final UserActionService userActionService;

    public UserActionController(UserActionService userActionService) {
        this.userActionService = userActionService;
    }

    // ── 用户信息 ────────────────────────────────────────────────────────────────

    /**
     * 获取用户个人信息。
     */
    @GetMapping("/users/{userId}/profile")
    public AjaxResult profile(@PathVariable Long userId) {
        return AjaxResult.success(userActionService.userProfile(userId));
    }

    /**
     * 更新用户个人信息（nickname、email、phone、avatarUrl）。
     */
    @PutMapping("/users/{userId}/profile")
    public AjaxResult updateProfile(@PathVariable Long userId, @RequestBody UpdateProfileRequest request) {
        return AjaxResult.success(userActionService.updateProfile(userId, request));
    }

    /**
     * 修改密码（已登录状态），需提供旧密码验证身份。
     */
    @PutMapping("/users/{userId}/password")
    public AjaxResult changePassword(@PathVariable Long userId, @RequestBody ChangePasswordRequest request) {
        userActionService.changePassword(userId, request.oldPassword(), request.newPassword());
        return AjaxResult.success();
    }

    // ── 观看/阅读记录 ────────────────────────────────────────────────────────────

    /**
     * 获取用户的媒体观看/阅读记录列表。
     */
    @GetMapping("/users/{userId}/records")
    public AjaxResult records(@PathVariable Long userId) {
        return AjaxResult.success(userActionService.records(userId));
    }

    /**
     * 新增或更新用户的媒体观看/阅读记录。有 ID 则更新，无 ID 则新增。
     */
    @PostMapping("/users/{userId}/records")
    public AjaxResult saveRecord(@PathVariable Long userId, @RequestBody UserMediaRecord record) {
        return AjaxResult.success(userActionService.saveRecord(userId, record));
    }

    // ── 收藏 ────────────────────────────────────────────────────────────────────

    /**
     * 切换收藏状态：未收藏则添加，已收藏则取消。
     */
    @PostMapping("/users/{userId}/favorites/{mediaId}/toggle")
    public AjaxResult toggleFavorite(@PathVariable Long userId, @PathVariable Long mediaId) {
        userActionService.toggleFavorite(userId, mediaId);
        return AjaxResult.success();
    }

    /**
     * 获取用户收藏的媒体 ID 列表。
     */
    @GetMapping("/users/{userId}/favorites")
    public AjaxResult favorites(@PathVariable Long userId) {
        return AjaxResult.success(userActionService.favorites(userId));
    }

    // ── 评价 ────────────────────────────────────────────────────────────────────

    /**
     * 获取某媒体条目的已通过评价列表。
     */
    @GetMapping("/media/{mediaId}/reviews")
    public AjaxResult reviews(@PathVariable Long mediaId) {
        return AjaxResult.success(userActionService.reviews(mediaId));
    }

    /**
     * 创建对某媒体条目的评价。
     */
    @PostMapping("/media/{mediaId}/reviews")
    public AjaxResult createReview(@PathVariable Long mediaId, @RequestBody Review review) {
        return AjaxResult.success(userActionService.createReview(mediaId, review));
    }

    /**
     * 举报某条评价。
     */
    @PostMapping("/reviews/{reviewId}/report")
    public AjaxResult reportReview(@PathVariable Long reviewId, @RequestBody ReviewReport report) {
        userActionService.reportReview(reviewId, report);
        return AjaxResult.success();
    }

    /**
     * 点赞评价（重复点赞为取消；若已点踩则切换）。
     *
     * @param reviewId 评价 ID
     * @param userId   操作用户 ID
     */
    @PostMapping("/reviews/{reviewId}/like")
    public AjaxResult likeReview(@PathVariable Long reviewId, @RequestParam Long userId) {
        userActionService.likeReview(reviewId, userId);
        return AjaxResult.success();
    }

    /**
     * 点踩评价（重复点踩为取消；若已点赞则切换）。
     *
     * @param reviewId 评价 ID
     * @param userId   操作用户 ID
     */
    @PostMapping("/reviews/{reviewId}/dislike")
    public AjaxResult dislikeReview(@PathVariable Long reviewId, @RequestParam Long userId) {
        userActionService.dislikeReview(reviewId, userId);
        return AjaxResult.success();
    }

    // ── 通知 ────────────────────────────────────────────────────────────────────

    /**
     * 获取用户通知列表（含该用户的已读状态）。
     */
    @GetMapping("/users/{userId}/notifications")
    public AjaxResult notifications(@PathVariable Long userId) {
        return AjaxResult.success(userActionService.notifications(userId));
    }

    /**
     * 标记指定通知为已读。
     */
    @PatchMapping("/users/{userId}/notifications/{notificationId}/read")
    public AjaxResult markRead(@PathVariable Long userId, @PathVariable Long notificationId) {
        userActionService.markNotificationRead(userId, notificationId);
        return AjaxResult.success();
    }

    /**
     * 标记该用户的所有通知为已读。
     */
    @PatchMapping("/users/{userId}/notifications/read-all")
    public AjaxResult markAllRead(@PathVariable Long userId) {
        userActionService.markAllNotificationsRead(userId);
        return AjaxResult.success();
    }

    // ── 日历、反馈、NSFW ────────────────────────────────────────────────────────

    /**
     * 获取追番日历（本周播出安排）。
     */
    @GetMapping("/calendar")
    public AjaxResult calendar() {
        return AjaxResult.success(userActionService.calendar());
    }

    /**
     * 提交用户反馈。
     */
    @PostMapping("/feedback")
    public AjaxResult feedback(@RequestBody UserFeedback feedback) {
        return AjaxResult.success(userActionService.feedback(feedback));
    }

    /**
     * 提交 NSFW 内容访问申请。
     */
    @PostMapping("/nsfw/applications")
    public AjaxResult applyNsfw(@RequestBody NsfwAccessApplication application) {
        return AjaxResult.success(userActionService.applyNsfw(application));
    }

    /** 修改密码请求参数 */
    public record ChangePasswordRequest(String oldPassword, String newPassword) {
    }
}

