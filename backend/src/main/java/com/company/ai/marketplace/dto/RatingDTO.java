package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 多维评分 DTO：易用性 / 业务价值 / 技术质量 / 创新性（各 1-5） */
@Data
public class RatingDTO {
    @NotNull(message = "作品 ID 不能为空")
    private Long submissionId;

    @NotNull @Min(1) @Max(5)
    private Integer easeOfUse;

    @NotNull @Min(1) @Max(5)
    private Integer businessValue;

    @NotNull @Min(1) @Max(5)
    private Integer techQuality;

    @NotNull @Min(1) @Max(5)
    private Integer innovation;

    private String commentText;
}
