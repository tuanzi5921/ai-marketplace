package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 作品评论表（先发后审，举报制）。 */
@Data
@TableName("mp_comment")
public class MpComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer reportCount;
    private Integer hidden;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
