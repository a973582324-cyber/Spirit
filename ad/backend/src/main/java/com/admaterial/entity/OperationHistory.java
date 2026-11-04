package com.admaterial.entity;

import com.admaterial.common.enums.BusinessType;
import com.admaterial.common.enums.OperationType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作历史 · 对应 operation_history
 * —— 多态关联：business_type + business_id，不加外键
 */
@Data
@TableName("operation_history")
public class OperationHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private BusinessType businessType;

    private Long businessId;

    private OperationType operationType;

    private String operationDesc;

    private Long operatorId;

    private String operatorName;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime operateTime;
}
