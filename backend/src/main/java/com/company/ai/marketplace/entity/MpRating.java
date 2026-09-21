package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多维评分记录：每人每作品限一条（uk_user_sub）。
 * <p>维度：易用性 / 业务价值 / 技术质量 / 创新性（各 1-5）。
 */
@Data
@TableName("mp_rating")
public class MpRating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long userId;
    /** USER / JUDGE */
    private String ratedByRole;
    private Integer easeOfUse;
    private Integer businessValue;
    private Integer techQuality;
    private Integer innovation;
    private String commentText;
    private LocalDateTime ratedAt;
}
