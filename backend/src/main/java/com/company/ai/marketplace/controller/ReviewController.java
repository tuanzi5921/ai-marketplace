package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.PageResult;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审核接口（运营后台用）。
 * <p>路径与前端 {@code frontend-admin/src/api/review.ts} 严格对应：
 * <ul>
 *   <li>GET  /reviews/pending — 待审队列</li>
 *   <li>POST /reviews/{id}/approve?reason= — 审核通过</li>
 *   <li>POST /reviews/{id}/reject?reason= — 审核退回</li>
 * </ul>
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 待审队列 */
    @GetMapping("/pending")
    public Result<PageResult<MpSubmission>> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.pendingQueue(page, size));
    }

    /** 审核通过 */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "") String reason) {
        reviewService.approve(id, reason);
        return Result.ok();
    }

    /** 审核退回 */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam(required = false, defaultValue = "") String reason) {
        reviewService.reject(id, reason);
        return Result.ok();
    }
}
