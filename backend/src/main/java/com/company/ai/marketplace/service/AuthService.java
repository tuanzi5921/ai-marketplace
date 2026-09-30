package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.config.AppProperties;
import com.company.ai.marketplace.dto.CreateUserDTO;
import com.company.ai.marketplace.dto.LoginResultVO;
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
 * <p>正式登录入口为账号密码（{@link #loginByPassword}），企微 SSO 已移除。
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

    /**
     * 账号密码登录（正式入口）。
     */
    public LoginResultVO loginByPassword(String account, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account));
        // 账号不存在与口令错误返回同一个错误码，避免被用来枚举有效账号
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
        // AuthInterceptor 只做登录态解析，角色控制必须在业务层兜底：
        // 否则任何普通 USER 登录后都能调此接口创建 ADMIN 账号，形成越权提权
        LoginUser operator = ThreadLocalContext.get();
        if (operator == null || operator.getRoles() == null
                || !operator.getRoles().contains("ADMIN")) {
            throw new BizException(ErrorCode.NO_PERMISSION.getCode(), "仅超管可创建用户");
        }
        String email = dto.getEmail();
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "邮箱格式不合法");
        }
        // account 默认取邮箱 @ 之前的部分；显式传入则优先使用
        String account = StrUtil.isNotBlank(dto.getAccount()) ? dto.getAccount() : email.substring(0, email.indexOf('@'));
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "账号已存在: " + account);
        }
        SysUser user = new SysUser();
        user.setWecomUserid(account); // 兼容历史 NOT NULL 字段，占位填账号本身
        user.setAccount(account);
        user.setPasswordHash(BCrypt.hashpw(props.getAuth().getDefaultPassword()));
        user.setUsername(account);
        user.setEmail(email);
        user.setRoles(String.join(",", dto.getRoles()));
        user.setPoints(0);
        user.setEnabled(1);
        user.setMustChangePassword(1);
        userMapper.insert(user);
        log.info("管理员创建用户: id={} account={} email={} roles={}",
                user.getId(), account, email, user.getRoles());
        return toUserVO(user);
    }

    /**
     * 当前登录用户。由 AuthInterceptor 解析 JWT 后写入 ThreadLocalContext，
     * 此处回查数据库以补齐 points / avatar / enabled 等 JWT 里不携带的字段。
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

    /** 校验启用状态后签发 JWT，并一并返回前端需要的用户信息 */
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
        // 角色为空会导致 JwtService.sign 里 String.join 得到空串、解析后出现空角色，
        // 兜底给 USER 保证任何已启用用户至少有基础权限
        if (result.isEmpty()) result.add("USER");
        return result;
    }
}
