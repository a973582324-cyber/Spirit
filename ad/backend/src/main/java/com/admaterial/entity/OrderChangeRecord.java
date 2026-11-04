package com.admaterial.entity;

import com.admaterial.common.enums.ChangeType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单变更记录 · 对应 order_change_record
 */
@Data
@TableName("order_change_record")
public class OrderChangeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private ChangeType changeType;

    private String changeTarget;

    private String changeBefore;

    private String changeAfter;

    private String changeReason;

    private Long changeBy;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime changeTime;
}
