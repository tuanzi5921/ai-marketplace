package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.config.AppProperties;
import com.company.ai.marketplace.dto.CreateUserDTO;
import com.company.ai.marketplace.dto.LoginResultVO;
import com.company.ai.marketplace.dto.PageResult;
import com.company.ai.marketplace.dto.UserVO;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.SysUserMapper;
import com.company.ai.marketplace.security.JwtService;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 认证服务。
 * <p>正式登录入口为账号密码，企微 SSO 已移除。
 * 管理员可通过 {@link #createUser} 创建用户账号，初始密码为
 * {@code app.auth.default-password}，首次登录强制改密。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtService jwtService;
    private final AppProperties props;
    private final AuditService auditService;

    /**
     * 账号密码登录（正式入口）。
     */
    public LoginResultVO loginByPassword(String account, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account));
        if (user == null || StrUtil.isBlank(user.getPasswordHash())
                || !BCrypt.checkpw(password, user.getPasswordHash())) {
            log.warn("账号密码登录失败: account={}", account);
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        return issueToken(user);
    }

    /**
     * 修改密码：校验旧口令 → 写入新口令哈希 → 清除改密标志。
     */
    public void changePassword(String oldPassword, String newPassword) {
        LoginUser current = ThreadLocalContext.get();
        if (current == null || current.getId() == null) {
            throw new BizException(ErrorCode.AUTH_MISSING);
        }
        SysUser user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_MISSING.getCode(), "用户已不存在");
        }
        if (StrUtil.isBlank(user.getPasswordHash())
                || !BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        user.setPasswordHash(BCrypt.hashpw(newPassword));
        user.setMustChangePassword(0);
        userMapper.updateById(user);
        log.info("用户改密成功: id={} account={}", user.getId(), user.getAccount());
    }

    /**
     * 管理员创建用户：邮箱作为账号 + 固定默认口令 + 角色分配，首次登录强制改密。
     */
    public UserVO createUser(CreateUserDTO dto) {
        // 权限校验：仅 ADMIN 可创建用户
        requireAdmin();
        String email = dto.getEmail();
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "邮箱格式不合法");
        }
        String account = StrUtil.isNotBlank(dto.getAccount()) ? dto.getAccount() : email.substring(0, email.indexOf('@'));
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "账号已存在: " + account);
        }
        SysUser user = new SysUser();
        user.setWecomUserid(null);
        user.setAccount(account);
        user.setPasswordHash(BCrypt.hashpw(props.getAuth().getDefaultPassword()));
        user.setUsername(dto.getUsername() != null ? dto.getUsername() : account);
        user.setEmail(email);
        // 确保 USER 角色始终存在
        Set<String> roles = new LinkedHashSet<>(dto.getRoles());
        roles.add("USER");
        user.setRoles(String.join(",", roles));
        user.setPoints(0);
        user.setEnabled(1);
        user.setMustChangePassword(1);
        userMapper.insert(user);
        log.info("管理员创建用户: id={} account={} email={} roles={}",
                user.getId(), account, email, user.getRoles());
        auditService.log("PERMISSION", "CREATE_USER", "USER", user.getId(),
                "account=" + account + " roles=" + user.getRoles());
        return toUserVO(user);
    }

    /**
     * 用户列表（管理端，仅 ADMIN）。
     */
    public PageResult<UserVO> listUsers(int page, int size) {
        requireAdmin();
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .orderByDesc(SysUser::getCreatedAt));
        return PageResult.from(p, this::toUserVO);
    }

    /**
     * 授予角色：在用户 roles 字段追加新角色（去重），仅 ADMIN。
     */
    public void grantRole(Long userId, String role) {
        requireAdmin();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        Set<String> roles = parseRoles(user.getRoles());
        roles.add(role);
        user.setRoles(String.join(",", roles));
        userMapper.updateById(user);
        auditService.log("PERMISSION", "GRANT_ROLE", "USER", userId, "role=" + role);
        log.info("角色授予: user={} role={}", userId, role);
    }

    /**
     * 撤销角色：从用户 roles 字段移除指定角色，仅 ADMIN。
     */
    public void revokeRole(Long userId, String role) {
        requireAdmin();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        Set<String> roles = parseRoles(user.getRoles());
        roles.remove(role);
        if (roles.isEmpty()) roles.add("USER");
        user.setRoles(String.join(",", roles));
        userMapper.updateById(user);
        auditService.log("PERMISSION", "REVOKE_ROLE", "USER", userId, "role=" + role);
        log.info("角色撤销: user={} role={}", userId, role);
    }

    /**
     * 启用 / 停用用户，仅 ADMIN。
     */
    public void setEnabled(Long userId, boolean enabled) {
        requireAdmin();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        user.setEnabled(enabled ? 1 : 0);
        userMapper.updateById(user);
        auditService.log("PERMISSION", enabled ? "ENABLE" : "DISABLE", "USER", userId, null);
        log.info("用户{}: id={}", enabled ? "启用" : "停用", userId);
    }

    /**
     * 当前登录用户。
     */
    public UserVO currentUser() {
        LoginUser current = ThreadLocalContext.get();
        if (current == null || current.getId() == null) {
            throw new BizException(ErrorCode.AUTH_MISSING);
        }
        SysUser user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_MISSING.getCode(), "用户已不存在");
        }
        return toUserVO(user);
    }

    // ====== 内部 ======

    private LoginResultVO issueToken(SysUser user) {
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BizException(ErrorCode.AUTH_USER_DISABLED);
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setWecomUserid(user.getWecomUserid());
        loginUser.setUsername(user.getUsername());
        loginUser.setDepartment(user.getDepartment());
        loginUser.setRoles(parseRoles(user.getRoles()));
        return new LoginResultVO(jwtService.sign(loginUser), toUserVO(user));
    }

    private UserVO toUserVO(SysUser u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setDisplayName(u.getUsername());
        vo.setDepartment(u.getDepartment());
        vo.setAvatar(u.getAvatarUrl());
        vo.setPoints(u.getPoints());
        vo.setRoles(new ArrayList<>(parseRoles(u.getRoles())));
        vo.setEnabled(u.getEnabled() != null && u.getEnabled() == 1);
        vo.setMustChangePassword(u.getMustChangePassword() != null && u.getMustChangePassword() == 1);
        return vo;
    }

    private Set<String> parseRoles(String roles) {
        Set<String> result = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(roles)) {
            for (String r : roles.split(",")) {
                if (StrUtil.isNotBlank(r)) result.add(r.trim());
            }
        }
        if (result.isEmpty()) result.add("USER");
        return result;
    }

    /** 要求当前登录用户具有 ADMIN 角色 */
    public void requireAdmin() {
        LoginUser current = ThreadLocalContext.get();
        if (current == null || current.getRoles() == null || !current.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN);
        }
    }

    /** 要求当前登录用户具有指定角色中的任意一个 */
    public void requireAnyRole(String... roles) {
        LoginUser current = ThreadLocalContext.get();
        if (current == null || current.getRoles() == null) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN);
        }
        for (String role : roles) {
            if (current.getRoles().contains(role)) return;
        }
        throw new BizException(ErrorCode.AUTH_FORBIDDEN);
    }
}
