package com.company.ai.marketplace.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把业务异常与校验异常统一转为 Result。
 * <p>鉴权类异常（1001/1004/1005/1006）返回对应的 HTTP 状态码，
 * 让前端 axios 的 error.response.status 分支能正确触发。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Object>> handleBiz(BizException e) {
        log.warn("biz: {} code={}", e.getMessage(), e.getCode());
        HttpStatus status = mapToHttpStatus(e.getCode());
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Object>> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().getFieldError() != null) {
            msg = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
                .body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), "参数校验失败：" + e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Object>> handleHttpMsg(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(Result.fail(ErrorCode.PARAM_INVALID.getCode(), "请求体格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleOther(Exception e) {
        log.error("unexpected", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SERVER_ERROR));
    }

    private HttpStatus mapToHttpStatus(int code) {
        return switch (code) {
            case 1001, 1004, 1005, 1006, 1007 -> HttpStatus.UNAUTHORIZED;
            case 1002, 3001 -> HttpStatus.FORBIDDEN;
            case 2001 -> HttpStatus.BAD_REQUEST;
            case 2002 -> HttpStatus.NOT_FOUND;
            case 2003, 4003 -> HttpStatus.CONFLICT;
            case 3002, 4001, 4002 -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK;
        };
    }
}
