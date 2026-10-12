package com.company.ai.marketplace.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 排行榜单项：作品视图 + 名次与分数。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RankingItemVO extends SubmissionVO {

    /** 名次，从 1 开始，按榜单顺序连续编号 */
    private Integer rank;

    /** 榜单排序用的分数，等于 ratingAvg；未评分时为 null */
    private BigDecimal score;
}
