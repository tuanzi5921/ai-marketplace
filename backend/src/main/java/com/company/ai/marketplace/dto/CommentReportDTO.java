package com.company.ai.marketplace.dto;

import lombok.Data;

/** 评论举报 DTO */
@Data
public class CommentReportDTO {
    private Long commentId;
    private String reason;
}
