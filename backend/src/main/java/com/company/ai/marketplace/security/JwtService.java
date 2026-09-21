package com.company.ai.marketplace.security;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.company.ai.marketplace.config.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JWT 工具：签发 / 解析登录态。
 * 密钥来源：app.jwt.secret。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public String sign(LoginUser u) {
        long now = System.currentTimeMillis();
        long exp = now + (long) props.getJwt().getExpireHours() * 3600_000;
        byte[] key = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        JWTSigner signer = JWTSignerUtil.hs256(key);
        return JWT.create()
                .setPayload("uid", u.getId())
                .setPayload("wid", u.getWecomUserid())
                .setPayload("name", u.getUsername())
                .setPayload("dept", u.getDepartment())
                .setPayload("roles", String.join(",", u.getRoles()))
                .setExpiresAt(new Date(exp))
                .setIssuedAt(new Date(now))
                .sign(signer);
    }

    public LoginUser parse(String token) {
        byte[] key = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (!JWTUtil.verify(token, key)) {
            throw new IllegalArgumentException("invalid token");
        }
        JWT jwt = JWTUtil.parseToken(token);
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new IllegalArgumentException("token expired");
        }
        Map<String, Object> payload = jwt.getPayloads();
        LoginUser u = new LoginUser();
        u.setId(((Number) payload.get("uid")).longValue());
        u.setWecomUserid((String) payload.get("wid"));
        u.setUsername((String) payload.get("name"));
        u.setDepartment((String) payload.getOrDefault("dept", ""));
        String rolesStr = (String) payload.getOrDefault("roles", "USER");
        Set<String> roles = new HashSet<>(Arrays.asList(rolesStr.split(",")));
        u.setRoles(roles);
        return u;
    }
}
