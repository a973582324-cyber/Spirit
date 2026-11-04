package com.admaterial.entity;

import com.admaterial.common.enums.MaterialType;
import com.admaterial.common.enums.TaskStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物料任务 · 对应 material_task
 */
@Data
@TableName("material_task")
public class MaterialTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Integer taskNo;

    private MaterialType materialType;

    private String sizeSpec;

    private Integer quantity;

    @TableField("`usage`")
    private String usage;

    private String refNotes;

    private LocalDateTime taskDeadline;

    private Long assigneeId;

    private TaskStatus taskStatus;

    private Integer versionCount;

    private LocalDateTime lastSubmitTime;

    private LocalDateTime lastFeedbackTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
