package com.admaterial.vo;

import com.admaterial.entity.MaterialOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单详情聚合 VO
 * —— 一次返回订单详情页所有 Tab 需要的数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO {
    private MaterialOrder order;
    private CustomerVO customer;
    private List<TaskLiteVO> tasks;
    private List<com.admaterial.entity.OrderChangeRecord> changes;
    private List<com.admaterial.entity.OperationHistory> operations;
}
