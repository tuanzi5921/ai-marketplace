package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 部门负责人推荐评委记录。 */
@Data
@TableName("mp_judge_recommendation")
public class MpJudgeRecommendation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long referrerId;
    private Long candidateId;
    private String department;
    private String reason;
    /** PENDING/APPROVED/REJECTED */
    private String status;
    private Long handledBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
