package com.company.ai.marketplace.integration.wecom;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.config.AppProperties;
import com.company.ai.marketplace.integration.wecom.dto.WecomApiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 企微 OpenAPI 客户端（v1）。
 * <p>覆盖能力：
 * <ul>
 *   <li>access_token 获取与 Redis 缓存（提前 5 分钟刷新）</li>
 *   <li>OAuth2 code → userid（SSO 入口）</li>
 *   <li>通讯录 — 用户详情查询</li>
 *   <li>个人应用消息推送（文本卡片 / 纯文本）</li>
 * </ul>
 * <p>失败策略：网络/接口错误抛 BizException(WECOM_MSG_FAIL)，由调用方决定是否吞掉。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomClient {

    private static final String API_BASE = "https://qyapi.weixin.qq.com/cgi-bin";
    private static final String REDIS_KEY_TOKEN = "wecom:access_token";
    private static final Duration TOKEN_TTL = Duration.ofSeconds(7000); // 提前 200s 刷新

    private final AppProperties appProperties;
    private final StringRedisTemplate redisTemplate;

    /** 配置是否就绪（corpId + appSecret 都已注入） */
    public boolean isConfigured() {
        AppProperties.WeCom w = appProperties.getWecom();
        return w != null && StrUtil.isNotBlank(w.getCorpId())
                && StrUtil.isNotBlank(w.getAppSecret());
    }

    /**
     * 获取 access_token（带 Redis 缓存）。
     */
    public String getAccessToken() {
        // 1. 先查缓存
        String cached = redisTemplate.opsForValue().get(REDIS_KEY_TOKEN);
        if (StrUtil.isNotBlank(cached)) return cached;

        if (!isConfigured()) {
            throw new BizException(ErrorCode.WECOM_MSG_FAIL.getCode(),
                    "企微配置缺失：corp-id 或 app-secret 未注入");
        }

        AppProperties.WeCom w = appProperties.getWecom();
        String url = String.format("%s/gettoken?corpid=%s&corpsecret=%s",
                API_BASE, w.getCorpId(), w.getAppSecret());
        String body = HttpUtil.get(url, 5000);
        WecomApiDto.TokenResp resp = JSONUtil.toBean(body, WecomApiDto.TokenResp.class);
        if (resp.getErrcode() != null && resp.getErrcode() != 0) {
            log.error("企微 gettoken 失败: errcode={} errmsg={}", resp.getErrcode(), resp.getErrmsg());
            throw new BizException(ErrorCode.WECOM_MSG_FAIL.getCode(),
                    "access_token 获取失败: " + resp.getErrmsg());
        }
        redisTemplate.opsForValue().set(REDIS_KEY_TOKEN, resp.getAccessToken(), TOKEN_TTL);
        return resp.getAccessToken();
    }

    /**
     * OAuth2 code 换企业成员 userid（SSO 入口）。
     * 返回 null 表示非企业成员（OpenId 路径，v1 不支持）。
     */
    public String getUserIdByCode(String code) {
        String url = String.format("%s/user/getuserinfo?access_token=%s&code=%s",
                API_BASE, getAccessToken(), code);
        String body = HttpUtil.get(url, 5000);
        WecomApiDto.UserInfoResp resp = JSONUtil.toBean(body, WecomApiDto.UserInfoResp.class);
        if (resp.getErrcode() != null && resp.getErrcode() != 0) {
            log.error("企微 getuserinfo 失败: errcode={} errmsg={}", resp.getErrcode(), resp.getErrmsg());
            throw new BizException(ErrorCode.AUTH_WECOM_FAIL.getCode(),
                    "OAuth 换 userid 失败: " + resp.getErrmsg());
        }
        return resp.getUserId();
    }

    /**
     * 通讯录 — 查询用户详情。
     */
    public WecomApiDto.UserDetailResp getUserDetail(String wecomUserId) {
        String url = String.format("%s/user/get?access_token=%s&userid=%s",
                API_BASE, getAccessToken(), wecomUserId);
        String body = HttpUtil.get(url, 5000);
        WecomApiDto.UserDetailResp resp = JSONUtil.toBean(body, WecomApiDto.UserDetailResp.class);
        if (resp.getErrcode() != null && resp.getErrcode() != 0) {
            log.error("企微 user/get 失败: userid={} errcode={} errmsg={}",
                    wecomUserId, resp.getErrcode(), resp.getErrmsg());
        }
        return resp;
    }

    /**
     * 推送文本卡片消息（推荐用于审核结果等结构化通知）。
     * @param toUser 企微 userid
     * @param title 标题
     * @param description 正文（支持 \n 换行）
     * @param url 点击跳转地址
     */
    public void sendTextCard(String toUser, String title, String description, String url) {
        if (StrUtil.isBlank(toUser)) {
            log.warn("推送目标 userid 为空，跳过");
            return;
        }
        AppProperties.WeCom w = appProperties.getWecom();
        WecomApiDto.TextCardMessage msg = new WecomApiDto.TextCardMessage();
        msg.setToUser(toUser);
        msg.setAgentId(parseIntSafe(w.getAgentId()));
        WecomApiDto.CardBody card = new WecomApiDto.CardBody();
        card.setTitle(title);
        card.setDescription(description);
        card.setUrl(StrUtil.isBlank(url) ? "URL" : url);
        card.setBtntxt("查看");
        msg.setTextcard(card);

        send("/message/send", JSONUtil.toJsonStr(msg));
        log.info("企微文本卡片推送: to={} title={}", toUser, title);
    }

    /**
     * 推送纯文本消息（最简形态）。
     */
    public void sendTextMessage(String toUser, String content) {
        if (StrUtil.isBlank(toUser)) return;
        AppProperties.WeCom w = appProperties.getWecom();
        WecomApiDto.TextMessage msg = new WecomApiDto.TextMessage();
        msg.setToUser(toUser);
        msg.setAgentId(parseIntSafe(w.getAgentId()));
        WecomApiDto.TextBody text = new WecomApiDto.TextBody();
        text.setContent(content);
        msg.setText(text);

        send("/message/send", JSONUtil.toJsonStr(msg));
        log.info("企微文本推送: to={} content_len={}", toUser, content.length());
    }

    // ====== 内部 ======

    private void send(String path, String jsonBody) {
        String url = String.format("%s%s?access_token=%s", API_BASE, path, getAccessToken());
        String resp = HttpUtil.post(url, jsonBody, 5000);
        JSONObject obj = JSONUtil.parseObj(resp);
        Integer errcode = obj.getInt("errcode");
        if (errcode != null && errcode != 0) {
            // access_token 过期时清缓存，让下一次调用自动刷新
            if (errcode == 42001 || errcode == 40014) {
                redisTemplate.delete(REDIS_KEY_TOKEN);
            }
            String errmsg = obj.getStr("errmsg");
            log.error("企微 API 调用失败: path={} errcode={} errmsg={}", path, errcode, errmsg);
            throw new BizException(ErrorCode.WECOM_MSG_FAIL.getCode(),
                    "企微消息推送失败: " + errmsg);
        }
    }

    private Integer parseIntSafe(String s) {
        if (StrUtil.isBlank(s)) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    /** 暴露用于 Service 层判断 access_token 是否缓存 */
    @SuppressWarnings("unused")
    public Optional<String> peekCachedToken() {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REDIS_KEY_TOKEN));
    }
}
