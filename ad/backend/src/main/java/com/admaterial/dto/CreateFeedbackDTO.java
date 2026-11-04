package com.admaterial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户经理录入客户修改意见 DTO
 */
@Data
public class CreateFeedbackDTO {
    @NotNull(message = "必须指定针对哪个版本")
    private Integer targetVersionNo;

    @NotBlank(message = "客户修改意见不能为空")
    @Size(max = 2000, message = "意见内容过长（最多 2000 字）")
    private String feedbackContent;

    /** 反馈渠道：微信 / 电话 / 邮件 / 现场 */
    private String feedbackSource;
}
