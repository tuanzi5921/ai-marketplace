package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 审核操作 DTO：通过 / 驳回 */
@Data
public class ReviewActionDTO {
    @NotNull(message = "作品 ID 不能为空")
    private Long submissionId;

    /** APPROVED / REJECTED */
    @NotBlank(message = "审核结果不能为空")
    private String decision;

    /** 驳回时必填 */
    private String rejectReason;
}
