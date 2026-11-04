package com.admaterial.dto;

import com.admaterial.common.enums.ChangeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单变更 DTO
 */
@Data
public class CreateChangeDTO {
    @NotNull(message = "变更类型不能为空")
    private ChangeType changeType;

    private String changeTarget;       // 如 "订单整体 deadline" 或 "task#2 海报数量"
    private String changeBefore;       // 变更前的值（快照）
    private String changeAfter;        // 变更后的值

    @NotBlank(message = "变更原因必须填写")
    private String changeReason;

    // 可选嵌套
    private Long taskId;                       // TASK_MODIFY / TASK_DEADLINE_CHANGE 时用
    @Valid
    private CreateTaskDTO newTask;              // TASK_ADD 时用
}
