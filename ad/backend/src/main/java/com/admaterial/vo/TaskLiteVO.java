package com.admaterial.vo;

import com.admaterial.common.enums.MaterialType;
import com.admaterial.common.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务列表项 VO（工作台 / 分派中心用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskLiteVO {
    private Long taskId;
    private Integer taskNo;
    private MaterialType materialType;
    private String materialTypeLabel;
    private String sizeSpec;
    private Integer quantity;
    private String usage;
    private LocalDateTime taskDeadline;
    private TaskStatus taskStatus;
    private String taskStatusLabel;
    private Long assigneeId;
    private String assigneeName;
    private Integer versionCount;
    private Boolean overdue;
    private String orderNo;       // 所属订单号（工作台展示需要）
}
