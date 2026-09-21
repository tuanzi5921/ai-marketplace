package com.company.ai.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 作品提交 / 创建 DTO */
@Data
public class SubmissionCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String shortDesc;
    private String detailDesc;

    /** SOURCE / EXECUTABLE / SAAS / DOCUMENT */
    @NotBlank(message = "作品类型不能为空")
    private String type;

    private String businessDomain;
    private String usageScenario;
    private String techStack;
    private String coverUrl;
    private String tags;

    /** 版本号，默认 1.0.0 */
    private String version;

    /** SAAS 类型作品必填：访问 URL */
    private String saasUrl;
    private String saasCredentials;
}
