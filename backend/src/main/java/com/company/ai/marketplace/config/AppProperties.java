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
    private WeCom wecom = new WeCom();
    private Storage storage = new Storage();
    private Review review = new Review();

    @Data
    public static class Jwt {
        private String secret;
        private int expireHours = 72;
    }

    @Data
    public static class WeCom {
        private String corpId;
        private String agentId;
        private String appSecret;
        private String authCallback;
        private String groupWebhook;
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
