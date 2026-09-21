package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.integration.wecom.WecomClient;
import com.company.ai.marketplace.integration.wecom.dto.WecomApiDto;
import com.company.ai.marketplace.mapper.SysUserMapper;
import com.company.ai.marketplace.security.JwtService;
import com.company.ai.marketplace.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 认证服务：企微 SSO 闭环。
 * <p>流程：前端拿企微 OAuth code → 调 login(code) → 返回 JWT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WecomClient wecomClient;
    private final SysUserMapper userMapper;
    private final JwtService jwtService;

    /**
     * 企微 OAuth 登录：code → wecomUserId → 查/建用户 → JWT。
     */
    public String login(String code) {
        // 1. code → wecomUserId
        String wecomUserId = wecomClient.getUserIdByCode(code);
        if (StrUtil.isBlank(wecomUserId)) {
            throw new RuntimeException("企微 OAuth 未返回 userid");
        }

        // 2. 查库 / 新建用户
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWecomUserid, wecomUserId));
        if (user == null) {
            // 首次登录：从通讯录拉详情，建用户
            WecomApiDto.UserDetailResp info = wecomClient.getUserDetail(wecomUserId);
            user = new SysUser();
            user.setWecomUserid(wecomUserId);
            user.setUsername(info != null ? info.getName() : wecomUserId);
            user.setEmail(info != null ? info.getEmail() : null);
            user.setMobile(info != null ? info.getMobile() : null);
            user.setAvatarUrl(info != null ? info.getAvatar() : null);
            user.setRoles("USER");
            user.setPoints(0);
            user.setEnabled(1);
            userMapper.insert(user);
            log.info("首次登录建用户: {} ({})", user.getUsername(), wecomUserId);
        }

        // 3. 构造 LoginUser + 签 JWT
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setWecomUserid(user.getWecomUserid());
        loginUser.setUsername(user.getUsername());
        loginUser.setDepartment(user.getDepartment());
        Set<String> roles = new HashSet<>();
        if (StrUtil.isNotBlank(user.getRoles())) {
            for (String r : user.getRoles().split(",")) roles.add(r.trim());
        }
        loginUser.setRoles(roles);

        return jwtService.sign(loginUser);
    }
}
