package com.admaterial.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分派任务 DTO（负责人）
 */
@Data
public class AssignTaskDTO {
    @NotNull(message = "任务 ID 不能为空")
    private Long taskId;

    @NotNull(message = "设计人员 ID 不能为空")
    private Long assigneeId;
}
