package com.admaterial.common;

import lombok.Getter;

/**
 * 业务异常
 * —— 带 HTTP 状态码 + errorKey，方便前端做精细处理
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int httpCode;
    private final String errorKey;

    public BusinessException(int httpCode, String message, String errorKey) {
        super(message);
        this.httpCode = httpCode;
        this.errorKey = errorKey;
    }

    public BusinessException(int httpCode, String message) {
        this(httpCode, message, null);
    }

    // ---------- 便捷工厂 ----------

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message, "notFound");
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message, "badRequest");
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message, "forbidden");
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message, "conflict");
    }

    public static BusinessException unprocessable(String message) {
        return new BusinessException(422, message, "unprocessable");
    }
}
