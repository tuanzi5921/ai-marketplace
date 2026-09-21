package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事配置表（双模架构：一个平台仅一个 ACTIVE 赛事）。
 * <p>phase: SUBMIT/REVIEW/AWARD（全期宽松 SOP）
 */
@Data
@TableName("mp_competition")
public class MpCompetition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** DRAFT/ACTIVE/JUDGING/CLOSED */
    private String status;
    private LocalDateTime submitStartAt;
    private LocalDateTime submitEndAt;
    private LocalDateTime reviewAt;
    private Integer shortlistThreshold;
    private Integer topNPerTrack;
    /** JSON：奖项配置 */
    private String awardsConfig;
    /** JSON：赛道配置数组 */
    private String tracksConfig;
    /** SUBMIT/REVIEW/AWARD */
    private String phase;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
