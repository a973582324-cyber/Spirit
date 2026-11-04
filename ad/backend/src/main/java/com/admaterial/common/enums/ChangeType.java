package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 订单变更类型
 */
@Getter
public enum ChangeType {
    TASK_ADD("TASK_ADD", "新增物料"),
    TASK_REMOVE("TASK_REMOVE", "移除物料"),
    TASK_MODIFY("TASK_MODIFY", "修改物料规格"),
    TASK_DEADLINE_CHANGE("TASK_DEADLINE_CHANGE", "物料截止时间变更"),
    ORDER_DEADLINE_CHANGE("ORDER_DEADLINE_CHANGE", "订单整体截止时间变更"),
    OTHER("OTHER", "其他变更");

    private final String code;
    private final String label;

    ChangeType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }
}
