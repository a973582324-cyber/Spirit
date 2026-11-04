package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 物料类型
 */
@Getter
public enum MaterialType {
    POSTER("POSTER", "海报"),
    STANDEE("STANDEE", "展架"),
    MENU("MENU", "菜单"),
    BROCHURE("BROCHURE", "宣传册"),
    ONLINE_IMAGE("ONLINE_IMAGE", "线上配图");

    private final String code;
    private final String label;

    MaterialType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }

    public static MaterialType fromCode(String code) {
        for (MaterialType m : values()) if (m.code.equalsIgnoreCase(code)) return m;
        throw new IllegalArgumentException("未知物料类型: " + code);
    }
}
