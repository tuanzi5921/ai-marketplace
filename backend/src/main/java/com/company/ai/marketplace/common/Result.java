package com.company.ai.marketplace.common;

/**
 * 统一 API 返回结构。
 * 所有 Controller 返回值统一包装为 Result。
 */
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Result<T> {

    /** 业务码：0 成功，非 0 失败 */
    private Integer code;
    private String  message;
    private T       data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(ErrorCode ec) {
        return new Result<>(ec.getCode(), ec.getMessage(), null);
    }

    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
