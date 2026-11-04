package com.admaterial.dto;

import lombok.Data;

/**
 * 订单审核 DTO
 */
@Data
public class ReviewOrderDTO {
    private String reviewNotes;   // 可选，审核备注
}
