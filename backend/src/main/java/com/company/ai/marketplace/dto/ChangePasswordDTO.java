package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改口令请求。首次登录强制改密 / 自助改密共用。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "旧口令不能为空")
    @Size(max = 128, message = "口令长度不能超过 128")
    private String oldPassword;

    @NotBlank(message = "新口令不能为空")
    @Size(min = 8, max = 128, message = "新口令长度需在 8-128 之间")
    // 与 PR #2 设计说明对齐：新口令需同时包含字母与数字（. 不匹配换行，口令不含换行）
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,128}$", message = "新口令需至少 8 位且同时包含字母和数字")
    private String newPassword;
}
