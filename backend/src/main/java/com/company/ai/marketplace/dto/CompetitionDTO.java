package com.company.ai.marketplace.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 大赛配置 DTO（运营后台编辑） */
@Data
public class CompetitionDTO {
    private Long id;
    private String name;
    private String status;
    private LocalDateTime submitStartAt;
    private LocalDateTime submitEndAt;
    private LocalDateTime reviewAt;
    private Integer shortlistThreshold;
    private Integer topNPerTrack;
    private String awardsConfig;
    private String tracksConfig;
    /** SUBMIT/REVIEW/AWARD */
    private String phase;
}
