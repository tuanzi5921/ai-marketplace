package com.company.ai.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功响应。
 * <p>必须同时返回 token 与 user：两个前端的 LoginResult 都会读 {@code user}，
 * 缺了会让 {@code auth.setUser(undefined)}，运营后台的角色守卫随即判定无权限。
 */
@Data
@AllArgsConstructor
public class LoginResultVO {

    private String token;

    private UserVO user;
}
