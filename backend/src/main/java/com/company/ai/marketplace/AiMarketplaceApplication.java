package com.company.ai.marketplace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业 AI 应用市场 — 后端启动类
 *
 * <p>职责：
 * <ul>
 *   <li>承载 Spring Boot 3 + JDK 17 启动入口</li>
 *   <li>扫描 mapper 包路径为 com.company.ai.marketplace.mapper</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.company.ai.marketplace.mapper")
public class AiMarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiMarketplaceApplication.class, args);
    }
}
