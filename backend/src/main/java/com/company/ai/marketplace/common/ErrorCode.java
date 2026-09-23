package com.company.ai.marketplace.common;

/**
 * 业务错误码枚举。
 * <p>范围：
 * <ul>
 *   <li>1xxx 鉴权 / 认证</li>
 *   <li>2xxx 参数 / 资源</li>
 *   <li>3xxx 业务规则（无权限 / 状态机不允许）</li>
 *   <li>4xxx 作品 / 审核管线</li>
 *   <li>5xxx 服务端通用</li>
 * </ul>
 */
@lombok.Getter
@lombok.AllArgsConstructor
public enum ErrorCode {

    OK(0, "ok"),

    // 1xxx
    AUTH_MISSING(1001, "未登录或 Token 已失效"),
    AUTH_FORBIDDEN(1002, "当前角色无权限执行该操作"),
    AUTH_WECOM_FAIL(1003, "企微 OAuth 鉴权失败"),
    /** 账号不存在与口令不匹配合并为同一个码，避免被用于枚举有效账号 */
    AUTH_BAD_CREDENTIALS(1004, "账号或口令错误"),
    AUTH_USER_DISABLED(1005, "账号已停用，请联系运营"),
    AUTH_LOCAL_LOGIN_DISABLED(1006, "账号密码登录未开启，请使用企业微信登录"),
    AUTH_WECOM_NOT_CONFIGURED(1007, "企微登录未配置，请联系管理员"),

    // 2xxx
    PARAM_INVALID(2001, "参数非法"),
    RESOURCE_NOT_FOUND(2002, "资源不存在"),
    DUPLICATE_SUBMIT(2003, "重复提交"),

    // 3xxx
    NO_PERMISSION(3001, "无权限"),
    STATUS_MISMATCH(3002, "当前状态不允许该操作"),

    // 4xxx
    SUBMISSION_NOT_PUBLISHED(4001, "作品尚未发布"),
    MUST_DOWNLOAD_FIRST(4002, "必须先下载作品才能评分"),
    ALREADY_RATED(4003, "您已对该作品评过分，可前往修改评分"),

    // 5xxx
    STORAGE_ERROR(5001, "存储服务异常"),
    WECOM_MSG_FAIL(5002, "企微消息推送失败"),

    SERVER_ERROR(5000, "服务端异常");

    private final int code;
    private final String message;
}
