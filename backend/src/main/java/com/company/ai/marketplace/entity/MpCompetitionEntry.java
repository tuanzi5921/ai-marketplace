package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 作品参赛报名表。 */
@Data
@TableName("mp_competition_entry")
public class MpCompetitionEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long submissionId;
    private Long authorId;
    private String track;
    private Integer points;
    private Integer shortlisted;
    private Integer judgeRank;
    /** FIRST/SECOND/THIRD/HONOR */
    private String award;
}
