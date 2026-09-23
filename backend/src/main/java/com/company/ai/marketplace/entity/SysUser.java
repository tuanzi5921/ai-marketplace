package com.company.ai.marketplace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String wecomUserid;
    /**
     * 本地登录账号。与 wecomUserid 分开存放，这样把 wecomUserid 从占位值
     * 换成真实企微 userId 后，账号密码登录依然可用。仅本地登录兜底启用时才有值。
     */
    private String account;
    /** BCrypt 哈希，永不返回给前端 */
    @JsonIgnore
    private String passwordHash;
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
