package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 账号密码登录请求。
 * <p>仅在 {@code app.auth.local-login-enabled=true} 时可用，用于企微 SSO 尚未接通
 * （可信域名未验证、凭据未下发）阶段让运营与测试人员能进入系统。
 */
@Data
public class LocalLoginDTO {

    /** 对应 sys_user.account */
    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号长度不能超过 64")
    private String account;

    @NotBlank(message = "口令不能为空")
    @Size(max = 128, message = "口令长度不能超过 128")
    private String password;
}
