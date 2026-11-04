package com.admaterial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建客户 DTO
 */
@Data
public class CreateCustomerDTO {
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100)
    private String customerName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50)
    private String contactPerson;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20)
    private String contactPhone;

    private String contactEmail;
    private String address;
    private String notes;
}
