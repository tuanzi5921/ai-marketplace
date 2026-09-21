package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.dto.ReviewActionDTO;
import com.company.ai.marketplace.entity.*;
import com.company.ai.marketplace.integration.wecom.WecomClient;
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
    private final WecomClient wecomClient;
    private final AuditService auditService;

    /**
     * 人工审核操作：通过 / 驳回。
     */
    @Transactional
    public void review(ReviewActionDTO dto) {
        LoginUser current = ThreadLocalContext.get();
        MpSubmission sub = submissionMapper.selectById(dto.getSubmissionId());
        if (sub == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"PENDING".equals(sub.getStatus())) {
            throw new BizException(ErrorCode.STATUS_MISMATCH.getCode(),
                    "当前状态不允许审核: " + sub.getStatus());
        }

        boolean approved = "APPROVED".equals(dto.getDecision());

        // 1. 更新作品状态
        LambdaUpdateWrapper<MpSubmission> uw = new LambdaUpdateWrapper<>();
        uw.eq(MpSubmission::getId, sub.getId());
        if (approved) {
            uw.set(MpSubmission::getStatus, "PUBLISHED");
            uw.set(MpSubmission::getPublishedAt, LocalDateTime.now());
        } else {
            uw.set(MpSubmission::getStatus, "REJECTED");
            uw.set(MpSubmission::getRejectReason, dto.getRejectReason());
        }
        uw.set(MpSubmission::getReviewedBy, current.getId());
        uw.set(MpSubmission::getReviewedAt, LocalDateTime.now());
        submissionMapper.update(null, uw);

        // 2. 记录审核流水（ReviewStep: MANUAL）
        MpReviewLog reviewLog = new MpReviewLog();
        reviewLog.setSubmissionId(sub.getId());
        reviewLog.setStepName("MANUAL");
        reviewLog.setStepStatus(approved ? "PASSED" : "REJECTED");
        reviewLog.setReviewerId(current.getId());
        reviewLog.setResultDetail(approved ? "人工审核通过" : "驳回: " + dto.getRejectReason());
        reviewLogMapper.insert(reviewLog);

        // 3. 推送企微个人应用消息给作者
        SysUser author = userMapper.selectById(sub.getAuthorId());
        if (author != null && StrUtil.isNotBlank(author.getWecomUserid())) {
            String content = approved
                    ? StrUtil.format("【审核通过】您的作品《{}》已发布上架，恭喜！", sub.getTitle())
                    : StrUtil.format("【审核未通过】您的作品《{}》被驳回，原因：{}。请修改后重新提交。",
                            sub.getTitle(), dto.getRejectReason());
            wecomClient.sendTextMessage(author.getWecomUserid(), content);
        }

        // 4. 审计
        auditService.log("REVIEW", approved ? "APPROVE" : "REJECT",
                "SUBMISSION", sub.getId(),
                "reviewer=" + current.getId() + " reason=" + dto.getRejectReason());

        log.info("审核完成: submission={} decision={} reviewer={}",
                sub.getId(), dto.getDecision(), current.getId());
    }

    /**
     * 待审队列分页（运营后台用）。
     */
    public Page<MpSubmission> pendingQueue(int page, int size) {
        return submissionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MpSubmission>()
                        .eq(MpSubmission::getStatus, "PENDING")
                        .orderByAsc(MpSubmission::getCreatedAt));
    }
}
