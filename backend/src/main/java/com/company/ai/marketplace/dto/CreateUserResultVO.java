package com.company.ai.marketplace.dto;

import lombok.Data;

/**
 * 管理员创建用户的返回体。
 * <p>{@code initialPassword} 是 {@code app.auth.default-password} 的明文，
 * <b>只在本接口返回这一次</b>：不写日志、不进用户列表、不进登录返回体。
 * 管理员据此把口令告知用户，用户首次登录会被强制改密。
 * <p>此前前端把初始口令写死成 {@code Welcome@2026}，与该配置项的实际值不一致，
 * 导致新建账号一律登不进去，故改为由后端回传真实值。
 */
@Data
public class CreateUserResultVO {

    private UserVO user;

    /** 初始口令明文，仅创建时返回 */
    private String initialPassword;
}
