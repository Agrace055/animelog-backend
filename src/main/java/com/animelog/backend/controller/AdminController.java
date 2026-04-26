package com.animelog.backend.controller;

import com.animelog.backend.domain.*;
import com.animelog.backend.dto.AjaxResult;
import com.animelog.backend.service.AdminService;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员后台控制器，提供媒体管理、评价审核、日历管理和 NSFW 审核等功能。<br>
 * 所有接口需要 admin 角色权限。
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 管理员手动更新媒体条目信息（会标记为人工覆盖）。
     */
    @PutMapping("/media/{id}")
    public AjaxResult updateMedia(@PathVariable Long id, @RequestBody Media media) {
        return AjaxResult.success(adminService.updateMedia(id, media));
    }

    /**
     * 获取所有被举报的评价列表。
     */
    @GetMapping("/reviews/reported")
    public AjaxResult reportedReviews() {
        return AjaxResult.success(adminService.reportedReviews());
    }

    /**
     * 批准举报，清除评价的被举报标记。
     */
    @PostMapping("/reviews/{reviewId}/approve-report")
    public AjaxResult approveReport(@PathVariable Long reviewId) {
        adminService.approveReport(reviewId);
        return AjaxResult.success();
    }

    /**
     * 删除评价（逻辑删除）。
     */
    @DeleteMapping("/reviews/{reviewId}")
    public AjaxResult deleteReview(@PathVariable Long reviewId) {
        adminService.deleteReview(reviewId);
        return AjaxResult.success();
    }

    /**
     * 创建追番日历条目。
     */
    @PostMapping("/calendar-items")
    public AjaxResult createCalendarItem(@RequestBody CalendarItem item) {
        return AjaxResult.success(adminService.createCalendarItem(item));
    }

    /**
     * 更新追番日历条目。
     */
    @PutMapping("/calendar-items/{id}")
    public AjaxResult updateCalendarItem(@PathVariable Long id, @RequestBody CalendarItem item) {
        return AjaxResult.success(adminService.updateCalendarItem(id, item));
    }

    /**
     * 删除追番日历条目（逻辑删除）。
     */
    @DeleteMapping("/calendar-items/{id}")
    public AjaxResult deleteCalendarItem(@PathVariable Long id) {
        adminService.deleteCalendarItem(id);
        return AjaxResult.success();
    }

    /**
     * 获取所有待审核的 NSFW 访问申请。
     */
    @GetMapping("/nsfw/applications")
    public AjaxResult nsfwApplications() {
        return AjaxResult.success(adminService.nsfwApplications());
    }

    /**
     * 审核 NSFW 访问申请（批准或拒绝）。
     *
     * @param id         申请 ID
     * @param action     审核操作：approve（批准）/ reject（拒绝）
     * @param reviewerId 审核人用户 ID
     */
    @PostMapping("/nsfw/applications/{id}/{action}")
    public AjaxResult reviewNsfw(@PathVariable Long id, @PathVariable String action, @RequestParam Long reviewerId) {
        adminService.reviewNsfw(id, action, reviewerId);
        return AjaxResult.success();
    }
}
