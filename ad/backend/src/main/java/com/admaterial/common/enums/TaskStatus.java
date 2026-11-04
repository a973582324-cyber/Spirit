package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 物料任务状态
 */
@Getter
public enum TaskStatus {
    PENDING_ASSIGN("PENDING_ASSIGN", "待分派"),
    ASSIGNED("ASSIGNED", "已分派"),
    IN_PROGRESS("IN_PROGRESS", "制作中"),
    PENDING_CLIENT_REVIEW("PENDING_CLIENT_REVIEW", "待客户审核"),
    NEEDS_REVISION("NEEDS_REVISION", "需修改"),
    CLIENT_CONFIRMED("CLIENT_CONFIRMED", "客户已确认"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String label;

    TaskStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }

    /**
     * 这个任务状态是否代表"已完成/可忽略"
     * —— 用于判断订单是否可以进入 PENDING_DELIVERY
     */
    public boolean isFinalOrIgnored() {
        return this == CLIENT_CONFIRMED || this == CANCELLED;
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus s : values()) if (s.code.equalsIgnoreCase(code)) return s;
        throw new IllegalArgumentException("未知任务状态: " + code);
    }
}
