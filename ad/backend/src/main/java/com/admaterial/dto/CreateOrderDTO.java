package com.admaterial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建订单 DTO（客户经理）
 */
@Data
public class CreateOrderDTO {
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "整体截止时间不能为空")
    @Future(message = "整体截止时间必须在未来")
    private LocalDateTime overallDeadline;

    private String notes;

    @NotEmpty(message = "至少要有 1 项物料")
    @Valid
    private List<CreateTaskDTO> tasks;
}
