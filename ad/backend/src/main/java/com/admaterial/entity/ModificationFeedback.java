package com.admaterial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户修改意见 · 对应 modification_feedback
 */
@Data
@TableName("modification_feedback")
public class ModificationFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer targetVersionNo;

    private String feedbackContent;

    private String feedbackSource;

    private Long recordBy;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime recordTime;

    private Boolean processed;

    private LocalDateTime processTime;
}
