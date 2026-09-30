package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.ChangePasswordDTO;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 * <p>路径与两个前端 {@code src/api/auth.ts} 中的调用严格对应，改动需同步前端。
 * <p>白名单说明：仅 {@code /auth/local/login} 为免鉴权路径，在 WebMvcConfig 中放行；
 * {@code /auth/me} 与 {@code /auth/change-password} 必须携带有效 JWT——
 * 否则 ThreadLocalContext 里拿不到登录态。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 账号密码登录（正式入口）。
     * <p>POST /api/auth/local/login  body: {@code {"account":"...","password":"..."}}
     */
    @PostMapping("/local/login")
    public Result<LoginResultVO> localLogin(@Valid @RequestBody LocalLoginDTO dto) {
        return Result.ok(authService.loginByPassword(dto.getAccount(), dto.getPassword()));
    }

    /**
     * 修改密码（首次登录强制改密 / 自助改密）。
     * <p>POST /api/auth/change-password  body: {@code {"oldPassword":"...","newPassword":"..."}}
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto.getOldPassword(), dto.getNewPassword());
        return Result.ok();
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
