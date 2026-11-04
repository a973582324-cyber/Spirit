package com.admaterial.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户修改意见 VO（带处理状态）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackVO {
    private Long feedbackId;
    private Integer targetVersionNo;
    private String feedbackContent;
    private String feedbackSource;
    private String recordByName;       // 谁录的（客户经理姓名）
    private LocalDateTime recordTime;
    private Boolean processed;          // 设计人员是否已处理
    private LocalDateTime processTime;
}
