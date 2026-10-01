package com.company.ai.marketplace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.dto.PageResult;
import com.company.ai.marketplace.entity.*;
import com.company.ai.marketplace.mapper.*;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审核管线服务：人工终审（v1） + AI 评审预留（按 ADR-0003）。
 * <p>SLA：3 工作日（AppProperties.review.manual-sla-days）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final MpSubmissionMapper submissionMapper;
    private final MpReviewLogMapper reviewLogMapper;
    private final SysUserMapper userMapper;
    private final AuditService auditService;

    /**
     * 人工审核：通过。
     */
    @Transactional
    public void approve(Long submissionId, String reason) {
        doReview(submissionId, true, reason);
    }

    /**
     * 人工审核：驳回。
     */
    @Transactional
    public void reject(Long submissionId, String reason) {
        doReview(submissionId, false, reason);
    }

    /**
     * 待审队列分页（运营后台用）。
     */
    public PageResult<MpSubmission> pendingQueue(int page, int size) {
        Page<MpSubmission> p = submissionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpSubmission>()
                        .eq(MpSubmission::getStatus, "PENDING")
                        .orderByAsc(MpSubmission::getCreatedAt));
        return PageResult.from(p);
    }

    // ====== 内部 ======

    private void doReview(Long submissionId, boolean approved, String reason) {
        LoginUser current = ThreadLocalContext.get();
        MpSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"PENDING".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.STATUS_MISMATCH.getCode(),
                    "当前状态不允许审核: " + sub.getStatus());
        }

        LambdaUpdateWrapper<MpSubmission> uw = new LambdaUpdateWrapper<>();
        uw.eq(MpSubmission::getId, sub.getId());
        if (approved) {
            uw.set(MpSubmission::getStatus, "PUBLISHED");
            uw.set(MpSubmission::getPublishedAt, LocalDateTime.now());
        } else {
            uw.set(MpSubmission::getStatus, "REJECTED");
            uw.set(MpSubmission::getRejectReason, reason);
        }
        uw.set(MpSubmission::getReviewedBy, current.getId());
        uw.set(MpSubmission::getReviewedAt, LocalDateTime.now());
        submissionMapper.update(null, uw);

        MpReviewLog reviewLog = new MpReviewLog();
        reviewLog.setSubmissionId(sub.getId());
        reviewLog.setStepName("MANUAL");
        reviewLog.setStepStatus(approved ? "PASSED" : "REJECTED");
        reviewLog.setReviewerId(current.getId());
        reviewLog.setResultDetail(approved ? "人工审核通过" : "驳回: " + reason);
        reviewLogMapper.insert(reviewLog);

        auditService.log("REVIEW", approved ? "APPROVE" : "REJECT",
                "SUBMISSION", sub.getId(),
                "reviewer=" + current.getId() + " reason=" + reason);

        log.info("审核完成: submission={} decision={} reviewer={}",
                sub.getId(), approved ? "APPROVED" : "REJECTED", current.getId());
    }
}
