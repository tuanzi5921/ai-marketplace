package com.company.ai.marketplace.service.review;

import com.company.ai.marketplace.entity.MpSubmission;

/**
 * 审核步骤接口（按 ADR-0003 可插拔隔离设计）。
 * <p>v1 仅 ManualReviewStep 启用；AI 步骤（AI_TEXT/AI_CODE/AI_BINARY/AI_DATA）默认关闭，架构预留。
 */
public interface ReviewStep {

    /** 步骤名：MANUAL / AI_TEXT / AI_CODE / AI_BINARY / AI_DATA */
    String stepName();

    /** 是否启用（配置开关） */
    boolean isEnabled();

    /**
     * 执行审核。
     * @param submission 待审作品
     * @return PASSED / REJECTED / SKIPPED
     */
    String execute(MpSubmission submission);
}
