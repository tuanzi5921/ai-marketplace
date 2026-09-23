package com.company.ai.marketplace.dto;

import lombok.Data;

import java.util.List;

/**
 * 登录用户信息视图对象。
 * <p>同时服务两个前端：运营后台读取 {@code displayName} / {@code username} / {@code roles}，
 * 工作台读取 {@code displayName} / {@code username} / {@code department} / {@code points}。
 * <p>不直接返回 SysUser 实体，避免把 passwordHash 等字段带出到前端。
 */
@Data
public class UserVO {

    private Long id;

    /** 员工姓名 */
    private String username;

    /** 与 username 同值；运营后台的 AuthUser 接口按此字段名展示 */
    private String displayName;

    private String department;

    private String avatar;

    private Integer points;

    /** 角色集合：USER / OPERATOR / ADMIN / JUDGE / DEPT_HEAD */
    private List<String> roles;

    private Boolean enabled;
}
