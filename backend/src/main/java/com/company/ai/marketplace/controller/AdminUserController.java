package com.company.ai.marketplace.controller;

import com.company.ai.marketplace.common.Result;
import com.company.ai.marketplace.dto.CreateUserDTO;
import com.company.ai.marketplace.dto.PageResult;
import com.company.ai.marketplace.dto.UserVO;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理接口。
 * <p>路径与前端 {@code frontend-admin/src/api/user.ts} 严格对应：
 * <ul>
 *   <li>POST   /admin/users — 创建用户</li>
 *   <li>GET    /admin/users — 用户列表</li>
 *   <li>POST   /admin/users/{id}/roles?role= — 授予角色</li>
 *   <li>DELETE /admin/users/{id}/roles?role= — 撤销角色</li>
 *   <li>POST   /admin/users/{id}/enable — 启用</li>
 *   <li>POST   /admin/users/{id}/disable — 停用</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    /** 创建用户账号 */
    @PostMapping("/users")
    public Result<UserVO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        return Result.ok(authService.createUser(dto));
    }

    /** 用户列表 */
    @GetMapping("/users")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(authService.listUsers(page, size));
    }

    /** 授予角色 */
    @PostMapping("/users/{id}/roles")
    public Result<Void> grantRole(@PathVariable Long id, @RequestParam String role) {
        authService.grantRole(id, role);
        return Result.ok();
    }

    /** 撤销角色 */
    @DeleteMapping("/users/{id}/roles")
    public Result<Void> revokeRole(@PathVariable Long id, @RequestParam String role) {
        authService.revokeRole(id, role);
        return Result.ok();
    }

    /** 启用用户 */
    @PostMapping("/users/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        authService.setEnabled(id, true);
        return Result.ok();
    }

    /** 停用用户 */
    @PostMapping("/users/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        authService.setEnabled(id, false);
        return Result.ok();
    }
}
