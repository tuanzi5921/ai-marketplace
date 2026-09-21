package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.RatingDTO;
import com.company.ai.marketplace.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评分接口。
 * <p>前置条件：必须先下载作品才能评分（防刷硬约束）。
 */
@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /** 提交 / 更新评分（一人一评，覆盖更新） */
    @PostMapping
    public Result<Void> rate(@Valid @RequestBody RatingDTO dto) {
        ratingService.rate(dto);
        return Result.ok();
    }
}
