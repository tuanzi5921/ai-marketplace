package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 / SSO 登录接口。
 * <p>白名单路径（WebMvcConfig 中 excludePathPatterns）。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 企微 OAuth 登录：前端传入 code，后端返回 JWT。
     * <p>GET /api/auth/login?code=xxx
     */
    @GetMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String code) {
        String token = authService.login(code);
        return Result.ok(Map.of("token", token));
    }

    /**
     * 生成企微 OAuth 授权跳转 URL（前端拿这个 URL 跳企微）。
     * <p>GET /api/auth/oauth-url
     */
    @GetMapping("/oauth-url")
    public Result<String> oauthUrl(@RequestParam String redirectUri) {
        // 实际 URL 需要替换 corp_id / agent_id / state
        // 这里返回模板，前端或运营按企微文档拼装
        String url = "https://open.weixin.qq.com/connect/oauth2/authorize"
                + "?appid=${corpid}&redirect_uri=" + redirectUri
                + "&response_type=code&scope=snsapi_base&agentid=${agentid}#wechat_redirect";
        return Result.ok(url);
    }
}
