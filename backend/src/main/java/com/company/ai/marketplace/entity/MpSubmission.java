package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品（Submission）主表。
 * <p>状态机：DRAFT → PENDING → APPROVED/REJECTED → PUBLISHED → UNLISTED/FROZEN
 */
@Data
@TableName("mp_submission")
public class MpSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long authorId;
    private String shortDesc;
    private String detailDesc;
    /** SOURCE / EXECUTABLE / SAAS / DOCUMENT */
    private String type;
    private String businessDomain;
    private String usageScenario;
    private String techStack;
    private String coverUrl;
    private String tags;
    private String version;
    private Long latestArtifactId;
    private Integer downloadCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    /** DRAFT/PENDING/APPROVED/REJECTED/PUBLISHED/UNLISTED/FROZEN */
    private String status;
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
