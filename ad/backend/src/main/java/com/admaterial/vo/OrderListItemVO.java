package com.admaterial.vo;

import com.admaterial.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemVO {
    private Long orderId;
    private String orderNo;
    private String customerName;
    private LocalDateTime overallDeadline;
    private OrderStatus orderStatus;
    private String orderStatusLabel;
    private int taskCount;
    private int confirmedCount;
    private String createByName;
    private String confirmByName;
    private Boolean overdue;     // 整体 deadline 已过且订单未完成
}
