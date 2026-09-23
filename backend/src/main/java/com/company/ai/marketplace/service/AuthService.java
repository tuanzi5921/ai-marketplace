package com.company.ai.marketplace.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.config.AppProperties;
import com.company.ai.marketplace.dto.LoginResultVO;
import com.company.ai.marketplace.dto.UserVO;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.integration.wecom.WecomClient;
import com.company.ai.marketplace.integration.wecom.dto.WecomApiDto;
import com.company.ai.marketplace.mapper.SysUserMapper;
import com.company.ai.marketplace.security.JwtService;
import com.company.ai.marketplace.security.LoginUser;
import com.company.ai.marketplace.security.ThreadLocalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 认证服务。
 * <p>两条登录路径，签发的 JWT 完全一致，后续鉴权无差别：
 * <ul>
 *   <li>企微 OAuth2 SSO（正式路径）：前端跳授权页 → 回调带 code → {@link #loginByWecomCode}</li>
 *   <li>账号密码（兜底路径）：{@link #loginByPassword}，由 {@code app.auth.local-login-enabled} 控制，
 *       仅在企微可信域名尚未验证通过的过渡期开启</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String OAUTH_AUTHORIZE_URL =
            "https://open.weixin.qq.com/connect/oauth2/authorize";

    private final WecomClient wecomClient;
    private final SysUserMapper userMapper;
    private final JwtService jwtService;
    private final AppProperties props;

    /**
     * 构造企微 OAuth2 授权跳转地址，供前端 {@code window.location.href} 直接跳转。
     * <p>用 snsapi_base 静默授权：只换 userid，不弹授权确认页。
     */
    public String buildWecomAuthUrl() {
        AppProperties.WeCom w = props.getWecom();
        if (w == null || StrUtil.isBlank(w.getCorpId()) || StrUtil.isBlank(w.getAgentId())) {
            throw new BizException(ErrorCode.AUTH_WECOM_NOT_CONFIGURED.getCode(),
                    "企微 corp-id 或 agent-id 未配置");
        }
        if (StrUtil.isBlank(w.getAuthCallback())) {
            throw new BizException(ErrorCode.AUTH_WECOM_NOT_CONFIGURED.getCode(),
                    "企微回调地址 app.wecom.auth-callback 未配置");
        }
        // redirect_uri 必须整体 URL 编码，否则企微返回「redirect_uri 参数错误」
        String redirectUri = URLEncoder.encode(w.getAuthCallback(), StandardCharsets.UTF_8);
        return OAUTH_AUTHORIZE_URL
                + "?appid=" + w.getCorpId()
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=snsapi_base"
                + "&agentid=" + w.getAgentId()
                + "#wechat_redirect";
    }

    /**
     * 企微 OAuth 登录：code → userid → 查/建用户 → JWT。
     */
    public LoginResultVO loginByWecomCode(String code) {
        if (StrUtil.isBlank(code)) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "缺少企微授权 code");
        }
        String wecomUserId = wecomClient.getUserIdByCode(code);
        if (StrUtil.isBlank(wecomUserId)) {
            // 返回空 userid 说明是非企业成员（OpenId 路径），v1 不支持
            throw new BizException(ErrorCode.AUTH_WECOM_FAIL.getCode(),
                    "企微未返回企业成员 userid，请确认在企业微信内打开");
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWecomUserid, wecomUserId));
        if (user == null) {
            user = createUserFromWecom(wecomUserId);
        }
        return issueToken(user);
    }

    /**
     * 账号密码登录（兜底）。默认关闭，需显式设置 {@code app.auth.local-login-enabled=true}。
     */
    public LoginResultVO loginByPassword(String account, String password) {
        if (!props.getAuth().isLocalLoginEnabled()) {
            throw new BizException(ErrorCode.AUTH_LOCAL_LOGIN_DISABLED);
        }
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

    /** 企微 SSO 首次登录：拉通讯录详情并建用户，默认只给 USER 角色 */
    private SysUser createUserFromWecom(String wecomUserId) {
        WecomApiDto.UserDetailResp info = wecomClient.getUserDetail(wecomUserId);
        SysUser user = new SysUser();
        user.setWecomUserid(wecomUserId);
        user.setUsername(info != null && StrUtil.isNotBlank(info.getName())
                ? info.getName() : wecomUserId);
        user.setEmail(info != null ? info.getEmail() : null);
        user.setMobile(info != null ? info.getMobile() : null);
        user.setAvatarUrl(info != null ? info.getAvatar() : null);
        user.setRoles("USER");
        user.setPoints(0);
        user.setEnabled(1);
        userMapper.insert(user);
        log.info("企微首次登录建用户: {} ({})", user.getUsername(), wecomUserId);
        return user;
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
