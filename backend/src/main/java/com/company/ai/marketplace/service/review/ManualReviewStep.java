package com.company.ai.marketplace.service.review;

import com.company.ai.marketplace.entity.MpSubmission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 人工终审步骤（v1 唯一启用的 ReviewStep）。
 * <p>实际审核决策由运营在 Controller 层发起，本步骤仅记录"已走人工审核"流水；
 * 具体通过/驳回由 ReviewService.review() 直接操作。
 */
@Slf4j
@Component
public class ManualReviewStep implements ReviewStep {

    @Override
    public String stepName() { return "MANUAL"; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public String execute(MpSubmission submission) {
        log.debug("ManualReviewStep: submission={}", submission.getId());
        return "PASSED"; // 占位：实际决策由运营操作驱动
    }
}
