package com.admaterial.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 操作历史 · 操作类型
 */
@Getter
public enum OperationType {
    CREATE("CREATE", "创建"),
    CONFIRM("CONFIRM", "审核通过"),
    REJECT("REJECT", "打回修改"),
    ASSIGN("ASSIGN", "分派任务"),
    BATCH_ASSIGN("BATCH_ASSIGN", "批量分派"),
    UPLOAD("UPLOAD", "上传设计稿"),
    SUBMIT_NEW_VERSION("SUBMIT_NEW_VERSION", "提交新版本"),
    FEEDBACK("FEEDBACK", "录入客户修改意见"),
    CLIENT_CONFIRM("CLIENT_CONFIRM", "客户确认"),
    CANCEL("CANCEL", "取消"),
    COMPLETE("COMPLETE", "完成交付"),
    CHANGE("CHANGE", "发起变更");

    private final String code;
    private final String label;

    OperationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() { return code; }
}
