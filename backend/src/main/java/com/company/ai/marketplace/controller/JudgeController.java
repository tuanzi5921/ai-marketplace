package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.PageResult;
import com.company.ai.marketplace.entity.MpJudgeRecommendation;
import com.company.ai.marketplace.mapper.MpJudgeRecommendationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import com.company.ai.marketplace.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 评委推荐接口（运营后台用）。
 * <p>路径与前端 {@code frontend-admin/src/api/judge.ts} 严格对应：
 * <ul>
 *   <li>GET  /judges/recommendations — 推荐列表</li>
 *   <li>POST /judges/recommendations/{id}/approve — 备案入库</li>
 *   <li>POST /judges/recommendations/{id}/reject — 拒绝</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/judges")
@RequiredArgsConstructor
public class JudgeController {

    private final MpJudgeRecommendationMapper recommendationMapper;
    private final AuditService auditService;

    /** 评委推荐列表 */
    @GetMapping("/recommendations")
    public Result<PageResult<MpJudgeRecommendation>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireOperator();
        Page<MpJudgeRecommendation> p = recommendationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpJudgeRecommendation>()
                        .orderByDesc(MpJudgeRecommendation::getCreatedAt));
        return Result.ok(PageResult.from(p));
    }

    /** 备案入库（通过） */
    @PostMapping("/recommendations/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        requireOperator();
        recommendationMapper.update(null, new LambdaUpdateWrapper<MpJudgeRecommendation>()
                .eq(MpJudgeRecommendation::getId, id)
                .eq(MpJudgeRecommendation::getStatus, "PENDING")
                .set(MpJudgeRecommendation::getStatus, "APPROVED")
                .set(MpJudgeRecommendation::getHandledBy, ThreadLocalContext.get().getId())
                .set(MpJudgeRecommendation::getUpdatedAt, LocalDateTime.now()));
        auditService.log("PERMISSION", "APPROVE_JUDGE", "JUDGE_REC", id, null);
        log.info("评委推荐通过: id={}", id);
        return Result.ok();
    }

    /** 拒绝 */
    @PostMapping("/recommendations/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        requireOperator();
        recommendationMapper.update(null, new LambdaUpdateWrapper<MpJudgeRecommendation>()
                .eq(MpJudgeRecommendation::getId, id)
                .eq(MpJudgeRecommendation::getStatus, "PENDING")
                .set(MpJudgeRecommendation::getStatus, "REJECTED")
                .set(MpJudgeRecommendation::getHandledBy, ThreadLocalContext.get().getId())
                .set(MpJudgeRecommendation::getUpdatedAt, LocalDateTime.now()));
        auditService.log("PERMISSION", "REJECT_JUDGE", "JUDGE_REC", id, null);
        log.info("评委推荐拒绝: id={}", id);
        return Result.ok();
    }

    private void requireOperator() {
        LoginUser u = ThreadLocalContext.get();
        if (u == null || u.getRoles() == null
                || (!u.getRoles().contains("OPERATOR") && !u.getRoles().contains("ADMIN"))) {
            throw new BizException(ErrorCode.NO_PERMISSION.getCode(), "仅运营 / 超管可操作");
        }
    }
}
