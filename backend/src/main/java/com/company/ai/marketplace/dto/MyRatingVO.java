package com.company.ai.marketplace.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 「我对某作品的评分」+ 该作品的评分汇总。
 * <p>{@code rated=false} 时四个维度均为 null，前端据此决定是展示已评分还是引导去评分。
 */
@Data
public class MyRatingVO {

    private Long submissionId;

    /** 当前用户是否已评过分 */
    private boolean rated;

    private Integer easeOfUse;
    private Integer businessValue;
    private Integer techQuality;
    private Integer innovation;
    private String commentText;

    /** USER / JUDGE，评分时按角色计入不同权重 */
    private String ratedByRole;

    private LocalDateTime ratedAt;

    /** 作品综合评分（评委 3 + 员工 7 加权），未有人评分时为 null */
    private BigDecimal avgScore;

    /** 作品评分人数 */
    private Integer raterCount;
}
