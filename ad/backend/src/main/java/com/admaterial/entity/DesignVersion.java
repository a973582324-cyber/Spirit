package com.admaterial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计稿版本 · 对应 design_version
 * —— append-only，永不 UPDATE 已有行
 */
@Data
@TableName("design_version")
public class DesignVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer versionNo;

    private String fileUrl;

    private String fileName;

    private Long fileSizeBytes;

    private Long uploadBy;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime uploadTime;

    private String versionNotes;
}
