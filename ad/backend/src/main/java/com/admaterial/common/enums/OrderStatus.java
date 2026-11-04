package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 订单状态
 */
@Getter
public enum OrderStatus {
    DRAFT("DRAFT", "草稿"),
    CONFIRMED("CONFIRMED", "已确认"),
    IN_PROGRESS("IN_PROGRESS", "制作中"),
    PENDING_DELIVERY("PENDING_DELIVERY", "待交付"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String label;

    OrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus s : values()) if (s.code.equalsIgnoreCase(code)) return s;
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}
