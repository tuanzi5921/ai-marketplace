package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.LocalLoginDTO;
import com.company.ai.marketplace.dto.LoginResultVO;
import com.company.ai.marketplace.dto.UserVO;
import com.company.ai.marketplace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口。
 * <p>路径与两个前端 {@code src/api/auth.ts} 中的调用严格对应，改动需同步前端。
 * <p>白名单说明：{@code /auth/wecom/redirect}、{@code /auth/wecom/login}、
 * {@code /auth/local/login} 三个免鉴权路径在 WebMvcConfig 中逐个放行，
 * 而 {@code /auth/me} 必须携带有效 JWT——不要再退回放行整个 {@code /auth/**}，
 * 否则 /auth/me 拿不到 ThreadLocalContext 里的登录态。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 取企微 OAuth2 授权跳转地址。
     * <p>GET /api/auth/wecom/redirect → {@code {"redirectUrl": "https://open.weixin.qq.com/..."}}
     * <p>返回 JSON 而非 302，是因为两个前端都用 XHR 取 redirectUrl 后自行跳转，
     * 这样失败时能拿到业务错误码并提示，而不是被浏览器导航到一个错误页。
     */
    @GetMapping("/wecom/redirect")
    public Result<Map<String, String>> wecomRedirect() {
        return Result.ok(Map.of("redirectUrl", authService.buildWecomAuthUrl()));
    }

    /**
     * 企微授权回调后，前端用 code 换 JWT。
     * <p>POST /api/auth/wecom/login?code=xxx
     */
    @PostMapping("/wecom/login")
    public Result<LoginResultVO> wecomLogin(@RequestParam String code) {
        return Result.ok(authService.loginByWecomCode(code));
    }

    /**
     * 账号密码登录（兜底），仅在 {@code app.auth.local-login-enabled=true} 时可用。
     * <p>POST /api/auth/local/login  body: {@code {"account":"...","password":"..."}}
     */
    @PostMapping("/local/login")
    public Result<LoginResultVO> localLogin(@Valid @RequestBody LocalLoginDTO dto) {
        return Result.ok(authService.loginByPassword(dto.getAccount(), dto.getPassword()));
    }

    /**
     * 当前登录用户信息。
     * <p>GET /api/auth/me，需携带 Authorization: Bearer &lt;token&gt;
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser());
    }
}
