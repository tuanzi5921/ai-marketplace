package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 评委信息表（部门推荐制，任期 1 年）。 */
@Data
@TableName("sys_judge")
public class SysJudge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String department;
    private String recommendBy;
    private LocalDateTime onboardAt;
    private LocalDateTime offboardAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
