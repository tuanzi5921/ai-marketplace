package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员创建用户请求。
 * <p>邮箱作为账号标识；角色由管理员分配，初始口令固定为 {@code Welcome@2026}，
 * 用户首次登录后强制改密。
 */
@Data
public class CreateUserDTO {

    /** 邮箱（作为账号身份） */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    private String email;

    /** 显式账号名，可省略；省略时取邮箱 @ 之前部分 */
    @Size(max = 64, message = "账号长度不能超过 64")
    private String account;

    /** 角色列表：USER / OPERATOR / ADMIN / JUDGE / DEPT_HEAD */
    @NotEmpty(message = "至少分配一个角色")
    private List<String> roles;
}
