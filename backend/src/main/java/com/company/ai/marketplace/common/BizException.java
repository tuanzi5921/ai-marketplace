package com.company.ai.marketplace.common;

/**
 * 业务异常。统一由 GlobalExceptionHandler 捕获并转为 Result。
 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(ErrorCode ec) {
        super(ec.getMessage());
        this.code = ec.getCode();
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() { return code; }
}
