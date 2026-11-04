package com.admaterial.dto;

import com.admaterial.common.enums.MaterialType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建物料任务 DTO（嵌入订单创建里）
 */
@Data
public class CreateTaskDTO {
    @NotNull(message = "物料类型不能为空")
    private MaterialType materialType;

    @NotBlank(message = "尺寸规格不能为空")
    private String sizeSpec;

    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity = 1;

    private String usage;
    private String refNotes;

    @NotNull(message = "物料截止时间不能为空")
    @Future(message = "截止时间必须在未来")
    private LocalDateTime taskDeadline;
}
