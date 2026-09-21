package com.company.ai.marketplace.integration.wecom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微 OpenAPI 响应/请求 DTO 集合。
 * <p>所有响应都含 errcode/errmsg；errcode=0 表示成功。
 */
public final class WecomApiDto {

    private WecomApiDto() {}

    /** 通用响应头 */
    @Data
    public static class BaseResp {
        @JsonProperty("errcode")
        private Integer errcode;
        @JsonProperty("errmsg")
        private String errmsg;
    }

    /** access_token 响应 */
    @Data
    public static class TokenResp extends BaseResp {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
    }

    /** OAuth2 code 换 userid 响应 */
    @Data
    public static class UserInfoResp extends BaseResp {
        /** 企业成员 userid */
        @JsonProperty("UserId")
        private String userId;
        /** 非企业成员的 OpenId */
        @JsonProperty("OpenId")
        private String openId;
        @JsonProperty("DeviceId")
        private String deviceId;
    }

    /** 通讯录 — 用户详情 */
    @Data
    public static class UserDetailResp extends BaseResp {
        @JsonProperty("userid")
        private String userId;
        @JsonProperty("name")
        private String name;
        @JsonProperty("department")
        private Integer[] department;
        /** 部门名数组（API 返回字符串，前端按 order 解析） */
        @JsonProperty("order")
        private Integer[] order;
        @JsonProperty("email")
        private String email;
        @JsonProperty("mobile")
        private String mobile;
        @JsonProperty("avatar")
        private String avatar;
        @JsonProperty("status")
        private Integer status;
    }

    /** 文本卡片消息请求体（个人应用消息） */
    @Data
    public static class TextCardMessage {
        @JsonProperty("touser")
        private String toUser;
        @JsonProperty("msgtype")
        private final String msgtype = "textcard";
        @JsonProperty("agentid")
        private Integer agentId;
        @JsonProperty("textcard")
        private CardBody textcard;
    }

    @Data
    public static class CardBody {
        @JsonProperty("title")
        private String title;
        @JsonProperty("description")
        private String description;
        @JsonProperty("url")
        private String url;
        @JsonProperty("btntxt")
        private String btntxt;
    }

    /** 简单文本消息请求体 */
    @Data
    public static class TextMessage {
        @JsonProperty("touser")
        private String toUser;
        @JsonProperty("msgtype")
        private final String msgtype = "text";
        @JsonProperty("agentid")
        private Integer agentId;
        @JsonProperty("text")
        private TextBody text;
    }

    @Data
    public static class TextBody {
        @JsonProperty("content")
        private String content;
    }
}
