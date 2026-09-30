package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.CreateUserDTO;
import com.company.ai.marketplace.dto.UserVO;
import com.company.ai.marketplace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理接口。
 * <p>v1 仅提供「新建用户」端点；列表 / 角色 / 启停等端点在前端 user.ts 已约定，
 * 后续按需在本控制器或独立控制器补齐。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    /**
     * 创建用户账号（管理员）。
     * <p>POST /api/admin/users  body: {@link CreateUserDTO}
     * <p>初始口令固定为 {@code app.auth.default-password}，用户首次登录后强制改密。
     */
    @PostMapping("/users")
    public Result<UserVO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        return Result.ok(authService.createUser(dto));
    }
}
