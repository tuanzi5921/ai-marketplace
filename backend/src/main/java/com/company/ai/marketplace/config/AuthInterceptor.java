package com.company.ai.marketplace.config;

import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.JwtService;
import com.company.ai.marketplace.security.ThreadLocalContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录态拦截器：从 Header 取 Bearer token → 解析 JWT → 写入 ThreadLocal。
 * 白名单接口（登录/Oauth）在 WebMvcConfig 中跳过。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new com.company.ai.marketplace.common.BizException(ErrorCode.AUTH_MISSING);
        }
        String token = header.substring(7);
        LoginUser user;
        try {
            user = jwtService.parse(token);
        } catch (Exception ex) {
            throw new com.company.ai.marketplace.common.BizException(ErrorCode.AUTH_MISSING);
        }
        ThreadLocalContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ThreadLocalContext.clear();
    }
}
