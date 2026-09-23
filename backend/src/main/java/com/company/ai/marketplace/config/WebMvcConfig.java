package com.company.ai.marketplace.config;

import com.company.ai.marketplace.config.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：CORS + 拦截器白名单 + Knife4j 资源放行。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录入口逐个放行；不能写成 /auth/**，
                        // 否则 /auth/me 也会被跳过，ThreadLocalContext 里拿不到登录态
                        "/auth/wecom/redirect",
                        "/auth/wecom/login",
                        "/auth/local/login",
                        // 公开接口
                        "/public/**",
                        // Knife4j
                        "/doc.html", "/v3/api-docs/**", "/webjars/**", "/favicon.ico",
                        "/error",
                        // 静态资源
                        "/static/**", "/storage/**"
                );
    }
}
