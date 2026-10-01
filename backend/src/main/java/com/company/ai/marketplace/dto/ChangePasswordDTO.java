package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
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
    private String newPassword;
}
