package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 评论举报记录表。 */
@Data
@TableName("mp_comment_report")
public class MpCommentReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long reporterId;
    private String reason;
    private Integer handled;
    private Long handledBy;
    private LocalDateTime createdAt;
}
