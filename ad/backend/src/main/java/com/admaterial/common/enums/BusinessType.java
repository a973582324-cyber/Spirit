package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 操作历史 · 业务类型
 */
@Getter
public enum BusinessType {
    ORDER("ORDER", "订单"),
    TASK("TASK", "物料任务"),
    VERSION("VERSION", "设计稿版本"),
    FEEDBACK("FEEDBACK", "客户修改意见"),
    CHANGE("CHANGE", "订单变更");

    private final String code;
    private final String label;

    BusinessType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }
}
