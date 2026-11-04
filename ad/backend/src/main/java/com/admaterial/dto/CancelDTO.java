package com.admaterial.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取消 DTO（订单或任务取消共用）
 */
@Data
public class CancelDTO {
    @NotBlank(message = "取消原因必须填写")
    private String reason;
}
