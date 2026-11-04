package com.admaterial.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 * —— 把各种异常统一转成 Result 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（主动抛出的）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        log.warn("[业务异常] code={}, key={}, msg={}", ex.getHttpCode(), ex.getErrorKey(), ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getHttpCode());
        body.put("message", ex.getMessage());
        body.put("errorKey", ex.getErrorKey());

        // Body 已经包含 status 语义，HttpStatus 直接用同码
        return ResponseEntity.status(ex.getHttpCode()).body(body);
    }

    /**
     * 参数校验失败（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("[参数校验失败] {}", fieldErrors);

        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("message", "参数校验失败: " + fieldErrors);
        body.put("errorKey", "validationFailed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 兜底：所有其他运行时异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleFallback(Exception ex) {
        log.error("[系统异常]", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("code", 500);
        body.put("message", "系统内部错误，请联系管理员");
        body.put("errorKey", "internalError");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
