package com.admaterial.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量分派任务 DTO
 */
@Data
public class BatchAssignTaskDTO {
    @NotEmpty(message = "至少分派 1 项任务")
    private List<AssignTaskDTO> items;
}
