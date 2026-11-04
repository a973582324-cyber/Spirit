package com.admaterial.entity;

import com.admaterial.common.enums.OrderStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宣传物料订单 · 对应 material_order
 */
@Data
@TableName("material_order")
public class MaterialOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerContact;

    private LocalDateTime overallDeadline;

    private OrderStatus orderStatus;

    private String notes;

    private Long createBy;

    private Long confirmBy;

    private LocalDateTime confirmTime;

    private LocalDateTime completeTime;

    private Long cancelBy;

    private LocalDateTime cancelTime;

    private String cancelReason;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
