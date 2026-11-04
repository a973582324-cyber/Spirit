package com.admaterial.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户经理记录客户确认 DTO
 */
@Data
public class ClientConfirmDTO {
    @NotNull(message = "必须确认客户认可的是哪个版本")
    private Integer confirmedVersionNo;
}
