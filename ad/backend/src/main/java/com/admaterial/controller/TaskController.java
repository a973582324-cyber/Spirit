package com.admaterial.controller;

import com.admaterial.common.Result;
import com.admaterial.common.enums.MaterialType;
import com.admaterial.common.enums.TaskStatus;
import com.admaterial.dto.AssignTaskDTO;
import com.admaterial.dto.BatchAssignTaskDTO;
import com.admaterial.dto.ClientConfirmDTO;
import com.admaterial.dto.CreateFeedbackDTO;
import com.admaterial.entity.DesignVersion;
import com.admaterial.entity.MaterialTask;
import com.admaterial.entity.ModificationFeedback;
import com.admaterial.service.TaskService;
import com.admaterial.vo.TaskDetailVO;
import com.admaterial.vo.TaskLiteVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 物料任务 Controller
 * —— 分派 / 批量分派 / 上传版本 / 录客户意见 / 客户确认 / 详情
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // ========================================================================
    // 1. 负责人分派
    // ========================================================================

    /** 单个任务分派 */
    @PostMapping("/{id}/assign")
    public Result<MaterialTask> assign(@PathVariable Long id,
                                       @Valid @RequestBody AssignTaskDTO dto) {
        if (!id.equals(dto.getTaskId())) dto.setTaskId(id);
        return Result.ok(taskService.assign(dto));
    }

    /** 批量分派 */
    @PostMapping("/batch-assign")
    public Result<Integer> batchAssign(@Valid @RequestBody BatchAssignTaskDTO dto) {
        return Result.ok(taskService.batchAssign(dto));
    }

    // ========================================================================
    // 2. 设计人员上传设计稿（multipart）
    // ========================================================================

    /**
     * 设计人员上传初稿或新版本
     * POST /api/tasks/{id}/versions
     * Content-Type: multipart/form-data
     * form fields: file + versionNotes (可选)
     */
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DesignVersion> uploadVersion(@PathVariable Long id,
                                               @RequestPart("file") MultipartFile file,
                                               @RequestPart(value = "versionNotes", required = false) String versionNotes) {
        return Result.ok(taskService.uploadVersion(id, file, versionNotes));
    }

    // ========================================================================
    // 3. 客户经理录入客户修改意见
    // ========================================================================

    /**
     * 客户经理记录客户对某个版本的反馈
     * POST /api/tasks/{id}/feedback
     */
    @PostMapping("/{id}/feedback")
    public Result<ModificationFeedback> submitFeedback(@PathVariable Long id,
                                                        @Valid @RequestBody CreateFeedbackDTO dto) {
        return Result.ok(taskService.submitFeedback(id, dto));
    }

    // ========================================================================
    // 4. 客户经理记录客户确认
    // ========================================================================

    /**
     * 客户经理标记客户对某个版本表示认可
     * POST /api/tasks/{id}/client-confirm
     * —— 自动联动：若订单所有物料都已确认，订单 → PENDING_DELIVERY
     */
    @PostMapping("/{id}/client-confirm")
    public Result<MaterialTask> clientConfirm(@PathVariable Long id,
                                              @Valid @RequestBody ClientConfirmDTO dto) {
        return Result.ok(taskService.clientConfirm(id, dto));
    }

    // ========================================================================
    // 5. 查询
    // ========================================================================

    /**
     * 任务详情（聚合视图：主信息 + 历史版本 + 客户意见）
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    public Result<TaskDetailVO> getDetail(@PathVariable Long id) {
        return Result.ok(taskService.getDetail(id));
    }

    /**
     * 任务列表（完整查询能力 + 数据权限自动过滤）
     * GET /api/tasks?orderId=&materialType=&taskStatus=&assigneeId=&deadlineFrom=&deadlineTo=&overdue=&page=&size=
     *
     * 数据权限：
     *   - DESIGNER 只能看分派给自己的
     *   - CS_MANAGER 只能看自己创建的订单下的任务
     *   - BIZ_LEADER 可看全部
     */
    @GetMapping
    public Result<Page<TaskLiteVO>> page(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) MaterialType materialType,
            @RequestParam(required = false) TaskStatus taskStatus,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineTo,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(taskService.pageQuery(orderId, materialType, taskStatus, assigneeId,
                deadlineFrom, deadlineTo, overdue, page, size));
    }
}
