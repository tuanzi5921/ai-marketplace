package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品附件 / 版本迭代表（同版本多迭代）。
 */
@Data
@TableName("mp_submission_artifact")
public class MpSubmissionArtifact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private String version;
    private String fileName;
    private String storedPath;
    private Long fileSizeBytes;
    private String contentType;
    private String fileHash;
    private String changelog;
    private Integer isLatest;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
