package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户角色
 */
@Getter
public enum UserRole {
    CS_MANAGER("CS_MANAGER", "客户经理"),
    BIZ_LEADER("BIZ_LEADER", "业务负责人"),
    DESIGNER("DESIGNER", "设计制作人员");

    private final String code;
    private final String label;

    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }

    public static UserRole fromCode(String code) {
        for (UserRole r : values()) if (r.code.equalsIgnoreCase(code)) return r;
        throw new IllegalArgumentException("未知角色: " + code);
    }
}
