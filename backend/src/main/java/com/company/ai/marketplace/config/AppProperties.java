package com.company.ai.marketplace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用级自定义配置（对应 application.yml 中 app.*）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Review review = new Review();
    private Auth auth = new Auth();

    @Data
    public static class Jwt {
        private String secret;
        private int expireHours = 72;
    }

    @Data
    public static class Auth {
        /**
         * 账号密码登录为正式入口，默认启用。
         */
        private boolean localLoginEnabled = true;

        /**
         * 管理员创建用户时使用的默认口令，用户首次登录后强制改密。
         */
        private String defaultPassword = "Welcome@2026";
    }

    @Data
    public static class Storage {
        /** 生产建议改为企业存储路径（NAS / 对象存储挂载） */
        private String basePath = "./storage/files";
    }

    @Data
    public static class Review {
        /** AI 评审开关（v1 默认 false，按 ADR-0003 可插拔隔离） */
        private boolean aiEnabled = false;
        private String aiEndpoint;
        private String aiApiKey;
        /** 人工审核 SLA 工作日数，默认 3 */
        private int manualSlaDays = 3;
    }
}
