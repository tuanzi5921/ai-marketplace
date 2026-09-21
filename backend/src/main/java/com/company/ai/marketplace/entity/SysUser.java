package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String wecomUserid;
    private String username;
    private String email;
    private String mobile;
    private String department;
    private String avatarUrl;
    /** 角色集合，逗号分隔：USER,OPERATOR,ADMIN,JUDGE,DEPT_HEAD */
    private String roles;
    private Integer points;
    private Integer enabled;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
