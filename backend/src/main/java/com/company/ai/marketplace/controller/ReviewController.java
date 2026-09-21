package com.company.ai.marketplace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.ReviewActionDTO;
import com.company.ai.marketplace.entity.MpSubmission;
import com.company.ai.marketplace.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审核接口（运营后台用）。
 */
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 待审队列 */
    @GetMapping("/pending")
    public Result<Page<MpSubmission>> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(reviewService.pendingQueue(page, size));
    }

    /** 审核操作：通过 / 驳回 */
    @PostMapping("/action")
    public Result<Void> action(@Valid @RequestBody ReviewActionDTO dto) {
        reviewService.review(dto);
        return Result.ok();
    }
}
