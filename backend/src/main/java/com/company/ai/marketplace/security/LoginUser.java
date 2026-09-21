package com.company.ai.marketplace.security;

import lombok.Data;

import java.util.Set;

/**
 * 已登录用户的 JWT 载荷（业务最小化）。
 */
@Data
public class LoginUser {
    private Long id;
    private String wecomUserid;
    private String username;
    private String department;
    /** 角色集合：USER / OPERATOR / ADMIN / JUDGE / DEPT_HEAD */
    private Set<String> roles;
}
