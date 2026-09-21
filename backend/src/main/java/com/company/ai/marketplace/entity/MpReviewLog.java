package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 审核步骤流水（按 ADR-0003 ReviewStep 设计）。 */
@Data
@TableName("mp_review_log")
public class MpReviewLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    /** MANUAL / AI_TEXT / AI_CODE / AI_BINARY / AI_DATA */
    private String stepName;
    /** SKIPPED / PASSED / REJECTED / PENDING */
    private String stepStatus;
    private Long reviewerId;
    private String resultDetail;
    private LocalDateTime createdAt;
}
