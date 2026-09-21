package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 下载记录（v1 必须下载后才能评分的判据来源）。 */
@Data
@TableName("mp_download_log")
public class MpDownloadLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long artifactId;
    private Long userId;
    private LocalDateTime createdAt;
}
