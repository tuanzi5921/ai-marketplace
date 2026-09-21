package com.company.ai.marketplace.dto;

import lombok.Data;

/** 评论创建 DTO */
@Data
public class CommentCreateDTO {
    private Long submissionId;
    /** 回复某条评论时填写 parentId，顶级评论为 null */
    private Long parentId;
    private String content;
}
