package com.admaterial.service;

import com.admaterial.common.BusinessException;
import com.admaterial.common.LoginUser;
import com.admaterial.common.UserContext;
import com.admaterial.common.enums.BusinessType;
import com.admaterial.common.enums.MaterialType;
import com.admaterial.common.enums.OrderStatus;
import com.admaterial.common.enums.OperationType;
import com.admaterial.common.enums.TaskStatus;
import com.admaterial.common.enums.UserRole;
import com.admaterial.dto.AssignTaskDTO;
import com.admaterial.dto.BatchAssignTaskDTO;
import com.admaterial.dto.ClientConfirmDTO;
import com.admaterial.dto.CreateFeedbackDTO;
import com.admaterial.entity.DesignVersion;
import com.admaterial.entity.MaterialOrder;
import com.admaterial.entity.MaterialTask;
import com.admaterial.entity.ModificationFeedback;
import com.admaterial.entity.SysUser;
import com.admaterial.mapper.DesignVersionMapper;
import com.admaterial.mapper.MaterialTaskMapper;
import com.admaterial.mapper.ModificationFeedbackMapper;
import com.admaterial.vo.FeedbackVO;
import com.admaterial.vo.TaskDetailVO;
import com.admaterial.vo.TaskLiteVO;
import com.admaterial.util.FileUploadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物料任务 Service
 * —— 分派 / 批量分派 / 上传版本 / 录客户意见 / 客户确认 / 详情查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final MaterialTaskMapper taskMapper;
    private final DesignVersionMapper versionMapper;
    private final ModificationFeedbackMapper feedbackMapper;
    private final SysUserService userService;
    private final OperationLogService logService;
    private final OrderService orderService;
    private final FileUploadUtil fileUploadUtil;

    // ========================================================================
    // 1. 单个任务分派（负责人）
    // ========================================================================

    @Transactional
    public MaterialTask assign(AssignTaskDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.BIZ_LEADER) {
            throw BusinessException.forbidden("只有业务负责人可以分派任务");
        }
        MaterialTask task = requireTask(dto.getTaskId());
        if (task.getTaskStatus() != TaskStatus.PENDING_ASSIGN) {
            throw BusinessException.conflict(
                    "任务当前状态 [" + task.getTaskStatus().getLabel() + "] 不允许分派");
        }
        SysUser assignee = requireDesigner(dto.getAssigneeId());
        task.setAssigneeId(assignee.getId());
        task.setTaskStatus(TaskStatus.ASSIGNED);
        taskMapper.updateById(task);
        logService.log(BusinessType.TASK, task.getId(), OperationType.ASSIGN,
                task.getMaterialType().getLabel() + " 任务 #" + task.getTaskNo() + " 分派给 " + assignee.getRealName(),
                current.getUserId(), current.getRealName());
        return task;
    }

    // ========================================================================
    // 2. 批量分派
    // ========================================================================

    @Transactional
    public int batchAssign(BatchAssignTaskDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.BIZ_LEADER) {
            throw BusinessException.forbidden("只有业务负责人可以批量分派任务");
        }
        // 第一轮全量校验
        for (AssignTaskDTO item : dto.getItems()) {
            MaterialTask task = requireTask(item.getTaskId());
            if (task.getTaskStatus() != TaskStatus.PENDING_ASSIGN) {
                throw BusinessException.conflict(
                        "任务 #" + task.getTaskNo() + " 当前状态 [" + task.getTaskStatus().getLabel() + "] 不允许分派");
            }
            requireDesigner(item.getAssigneeId());
        }
        // 第二轮执行
        int count = 0;
        for (AssignTaskDTO item : dto.getItems()) {
            MaterialTask task = taskMapper.selectById(item.getTaskId());
            SysUser assignee = userService.getById(item.getAssigneeId());
            task.setAssigneeId(assignee.getId());
            task.setTaskStatus(TaskStatus.ASSIGNED);
            taskMapper.updateById(task);
            logService.log(BusinessType.TASK, task.getId(), OperationType.BATCH_ASSIGN,
                    "批量分派：" + task.getMaterialType().getLabel() + " #" + task.getTaskNo() + " → " + assignee.getRealName(),
                    current.getUserId(), current.getRealName());
            count++;
        }
        return count;
    }

    // ========================================================================
    // 3. 上传设计稿（设计人员）—— append-only，永不覆盖旧版本
    // ========================================================================

    /**
     * 设计人员上传初稿或新版本
     * —— 状态机：ASSIGNED / IN_PROGRESS / NEEDS_REVISION → PENDING_CLIENT_REVIEW
     * —— versionNo = 当前 MAX + 1
     * —— 自动把上一轮未处理的客户意见置为已处理（processed=1）
     */
    @Transactional
    public DesignVersion uploadVersion(Long taskId, MultipartFile file, String versionNotes) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.DESIGNER) {
            throw BusinessException.forbidden("只有设计制作人员可以上传设计稿");
        }

        MaterialTask task = requireTask(taskId);

        // 数据权限：只能上传分派给自己的任务
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(current.getUserId())) {
            throw BusinessException.forbidden("该任务未分派给您，无法上传");
        }

        // 状态机校验：以下状态允许上传
        //   ASSIGNED / IN_PROGRESS / NEEDS_REVISION → 都可以上传新稿
        TaskStatus status = task.getTaskStatus();
        if (status == TaskStatus.PENDING_ASSIGN) {
            throw BusinessException.conflict("任务还没分派，无法上传设计稿");
        }
        if (status == TaskStatus.CLIENT_CONFIRMED) {
            throw BusinessException.conflict("客户已确认的任务不能再上传新版本");
        }
        if (status == TaskStatus.CANCELLED) {
            throw BusinessException.conflict("已取消的任务不能上传");
        }

        // 1. 文件保存
        FileUploadUtil.UploadResult result = fileUploadUtil.save(file, "versions");

        // 2. 计算版本号（MAX + 1）
        int currentMax = versionMapper.findMaxVersionNo(taskId);
        int newVersionNo = currentMax + 1;

        // 3. INSERT 新版本（append-only）
        DesignVersion dv = new DesignVersion();
        dv.setTaskId(taskId);
        dv.setVersionNo(newVersionNo);
        dv.setFileUrl(result.fileUrl());
        dv.setFileName(result.originalFilename());
        dv.setFileSizeBytes(result.fileSizeBytes());
        dv.setUploadBy(current.getUserId());
        dv.setVersionNotes(versionNotes != null ? versionNotes : "");
        versionMapper.insert(dv);

        // 4. 更新任务主记录
        task.setVersionCount(task.getVersionCount() + 1);
        task.setLastSubmitTime(LocalDateTime.now());
        task.setTaskStatus(TaskStatus.PENDING_CLIENT_REVIEW);  // 等待客户审核
        taskMapper.updateById(task);

        // 5. 自动把上一轮未处理的客户意见置为已处理
        List<ModificationFeedback> pendingFeedbacks = feedbackMapper.selectList(
                new LambdaQueryWrapper<ModificationFeedback>()
                        .eq(ModificationFeedback::getTaskId, taskId)
                        .eq(ModificationFeedback::getProcessed, false)
        );
        for (ModificationFeedback fb : pendingFeedbacks) {
            fb.setProcessed(true);
            fb.setProcessTime(LocalDateTime.now());
            feedbackMapper.updateById(fb);
        }

        // 6. 操作历史
        OperationType logType = newVersionNo == 1 ? OperationType.UPLOAD : OperationType.SUBMIT_NEW_VERSION;
        String logDesc = newVersionNo == 1
                ? "上传 " + task.getMaterialType().getLabel() + " 初稿 v" + newVersionNo
                : "提交 " + task.getMaterialType().getLabel() + " 新版本 v" + newVersionNo
                        + (pendingFeedbacks.isEmpty() ? "" : "（已响应 " + pendingFeedbacks.size() + " 条意见）");
        logService.log(BusinessType.VERSION, taskId, logType, logDesc,
                current.getUserId(), current.getRealName());

        log.info("[Task] 上传版本成功: taskId={}, versionNo={}, fileSize={}",
                taskId, newVersionNo, result.fileSizeBytes());
        return dv;
    }

    // ========================================================================
    // 4. 客户经理录入客户修改意见
    // ========================================================================

    /**
     * 客户经理记录客户对某个版本的反馈
     * —— 状态机：PENDING_CLIENT_REVIEW → NEEDS_REVISION
     */
    @Transactional
    public ModificationFeedback submitFeedback(Long taskId, CreateFeedbackDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.CS_MANAGER) {
            throw BusinessException.forbidden("只有客户经理可以录入客户修改意见");
        }

        MaterialTask task = requireTask(taskId);

        // 数据权限：客户经理只能操作自己参与的订单（简化：非 BIZ_LEADER 时只能操作自己创建的订单）
        MaterialOrder order = orderService.getOrderById(task.getOrderId());
        if (current.getRole() != UserRole.BIZ_LEADER
                && !order.getCreateBy().equals(current.getUserId())) {
            throw BusinessException.forbidden("您无权操作该订单下的任务");
        }

        // 前置：该版本必须存在
        DesignVersion target = versionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getTaskId, taskId)
                        .eq(DesignVersion::getVersionNo, dto.getTargetVersionNo())
        );
        if (target == null) {
            throw BusinessException.notFound("目标版本不存在：taskId=" + taskId + ", versionNo=" + dto.getTargetVersionNo());
        }

        // 状态机校验
        if (task.getTaskStatus() == TaskStatus.CLIENT_CONFIRMED) {
            throw BusinessException.conflict("客户已确认的任务不能再录入意见");
        }
        if (task.getTaskStatus() == TaskStatus.CANCELLED) {
            throw BusinessException.conflict("已取消的任务不能录入意见");
        }

        // INSERT 意见
        ModificationFeedback fb = new ModificationFeedback();
        fb.setTaskId(taskId);
        fb.setTargetVersionNo(dto.getTargetVersionNo());
        fb.setFeedbackContent(dto.getFeedbackContent());
        fb.setFeedbackSource(dto.getFeedbackSource());
        fb.setRecordBy(current.getUserId());
        fb.setProcessed(false);
        feedbackMapper.insert(fb);

        // 更新任务状态：进入 NEEDS_REVISION
        task.setTaskStatus(TaskStatus.NEEDS_REVISION);
        task.setLastFeedbackTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 操作历史
        logService.log(BusinessType.FEEDBACK, taskId, OperationType.FEEDBACK,
                "客户对 v" + dto.getTargetVersionNo() + " 提出修改意见",
                current.getUserId(), current.getRealName());

        log.info("[Task] 录入客户意见: taskId={}, versionNo={}", taskId, dto.getTargetVersionNo());
        return fb;
    }

    // ========================================================================
    // 5. 客户经理记录客户确认
    // ========================================================================

    /**
     * 客户经理标记客户对某个版本表示认可
     * —— 状态机：PENDING_CLIENT_REVIEW → CLIENT_CONFIRMED
     * —— 自动联动：若订单下所有物料都 CLIENT_CONFIRMED / CANCELLED，订单 → PENDING_DELIVERY
     */
    @Transactional
    public MaterialTask clientConfirm(Long taskId, ClientConfirmDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.CS_MANAGER) {
            throw BusinessException.forbidden("只有客户经理可以记录客户确认");
        }

        MaterialTask task = requireTask(taskId);

        // 状态机校验：不能重复确认
        if (task.getTaskStatus() == TaskStatus.CLIENT_CONFIRMED) {
            throw BusinessException.conflict("该任务已经客户确认过");
        }
        if (task.getTaskStatus() == TaskStatus.CANCELLED) {
            throw BusinessException.conflict("已取消的任务不能确认");
        }
        // 前置：被确认的版本必须存在
        DesignVersion target = versionMapper.selectOne(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getTaskId, taskId)
                        .eq(DesignVersion::getVersionNo, dto.getConfirmedVersionNo())
        );
        if (target == null) {
            throw BusinessException.notFound("被确认的版本不存在：v" + dto.getConfirmedVersionNo());
        }

        // 执行确认
        task.setTaskStatus(TaskStatus.CLIENT_CONFIRMED);
        taskMapper.updateById(task);

        logService.log(BusinessType.TASK, taskId, OperationType.CLIENT_CONFIRM,
                "客户确认 " + task.getMaterialType().getLabel() + " #" + task.getTaskNo() + " 的 v" + dto.getConfirmedVersionNo(),
                current.getUserId(), current.getRealName());

        // 自动联动：检查订单是否所有物料都已 CLIENT_CONFIRMED / CANCELLED
        checkAndPromoteOrderStatus(task.getOrderId(), current);

        log.info("[Task] 客户确认: taskId={}, versionNo={}", taskId, dto.getConfirmedVersionNo());
        return task;
    }

    // ========================================================================
    // 6. 任务详情（聚合视图）
    // ========================================================================

    /**
     * 组装任务详情 VO：task + orderNo + assigneeName + 所有历史版本 + 所有客户意见
     */
    public TaskDetailVO getDetail(Long taskId) {
        MaterialTask task = requireTask(taskId);

        LoginUser current = UserContext.get();
        // 数据权限：设计人员只能看自己的任务
        if (current.getRole() == UserRole.DESIGNER
                && !task.getAssigneeId().equals(current.getUserId())) {
            throw BusinessException.forbidden("该任务未分派给您");
        }

        // 订单信息
        MaterialOrder order = orderService.getOrderById(task.getOrderId());

        // 负责人姓名
        String assigneeName = null;
        if (task.getAssigneeId() != null) {
            SysUser u = userService.getById(task.getAssigneeId());
            if (u != null) assigneeName = u.getRealName();
        }

        // 所有历史版本
        List<DesignVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<DesignVersion>()
                        .eq(DesignVersion::getTaskId, taskId)
                        .orderByAsc(DesignVersion::getVersionNo)
        );

        // 所有客户意见（按 targetVersionNo 排序，再按时间）
        List<ModificationFeedback> rawFeedbacks = feedbackMapper.selectList(
                new LambdaQueryWrapper<ModificationFeedback>()
                        .eq(ModificationFeedback::getTaskId, taskId)
                        .orderByAsc(ModificationFeedback::getTargetVersionNo)
                        .orderByAsc(ModificationFeedback::getRecordTime)
        );

        // 预查所有录入人姓名
        List<Long> recordByIds = rawFeedbacks.stream()
                .map(ModificationFeedback::getRecordBy).distinct().collect(Collectors.toList());
        Map<Long, String> recordByNameMap = new java.util.HashMap<>();
        for (Long id : recordByIds) {
            SysUser u = userService.getById(id);
            if (u != null) recordByNameMap.put(id, u.getRealName());
        }

        List<FeedbackVO> feedbacks = rawFeedbacks.stream()
                .map(fb -> new FeedbackVO(
                        fb.getId(), fb.getTargetVersionNo(), fb.getFeedbackContent(),
                        fb.getFeedbackSource(), recordByNameMap.get(fb.getRecordBy()),
                        fb.getRecordTime(), fb.getProcessed(), fb.getProcessTime()))
                .collect(Collectors.toList());

        return new TaskDetailVO(
                task, order.getOrderNo(), order.getOrderStatus().getLabel(),
                assigneeName, versions, feedbacks);
    }

    // ========================================================================
    // 7. 内部工具
    // ========================================================================

    /**
     * 按 id 必取任务，不存在直接抛 404
     */
    public MaterialTask requireTask(Long id) {
        MaterialTask t = taskMapper.selectById(id);
        if (t == null) throw BusinessException.notFound("物料任务不存在, id=" + id);
        return t;
    }

    /**
     * 按 id 必取 DESIGNER 用户，不存在/角色不对/停用均抛异常
     */
    private SysUser requireDesigner(Long id) {
        SysUser u = userService.getById(id);
        if (u == null) throw BusinessException.notFound("设计人员不存在, id=" + id);
        if (u.getRole() != UserRole.DESIGNER) {
            throw BusinessException.unprocessable(
                    "目标用户角色是 [" + u.getRole().getLabel() + "]，不是设计制作人员");
        }
        if (!Boolean.TRUE.equals(u.getEnabled())) {
            throw BusinessException.unprocessable("设计人员账号已停用");
        }
        return u;
    }

    /**
     * 检查订单是否所有物料都 CLIENT_CONFIRMED / CANCELLED
     * 如果是 → 自动把订单状态改为 PENDING_DELIVERY
     * —— 这是文件核心业务规则 7 的实现："订单内所有物料都确认后，整笔订单才能完成"
     */
    private void checkAndPromoteOrderStatus(Long orderId, LoginUser operator) {
        MaterialOrder order = orderService.getOrderById(orderId);

        // 订单已经是终态就别动
        if (order.getOrderStatus() == OrderStatus.COMPLETED
                || order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // 还有没到终态/忽略态的任务吗？
        long unfinished = taskMapper.countUnfinishedByOrderId(orderId);
        if (unfinished > 0) {
            log.info("[Task] 订单 {} 还有 {} 项物料未确认，维持当前状态", orderId, unfinished);
            return;
        }

        // 全部完成 → PENDING_DELIVERY
        orderService.markPendingDelivery(orderId);

        logService.log(BusinessType.ORDER, orderId, OperationType.CLIENT_CONFIRM,
                "订单下所有物料都已客户确认，订单状态自动变为【待交付】",
                operator.getUserId(), operator.getRealName());

        log.info("[Task] 订单 {} 自动推进到 PENDING_DELIVERY", orderId);
    }

    /**
     * 按订单 id 查所有任务（订单详情页用）
     */
    public List<MaterialTask> listByOrderId(Long orderId) {
        return taskMapper.selectList(
                new LambdaQueryWrapper<MaterialTask>()
                        .eq(MaterialTask::getOrderId, orderId)
                        .orderByAsc(MaterialTask::getTaskNo)
        );
    }

    // ========================================================================
    // 8. 任务列表分页查询（工作台 / 分派中心 / 延期监控）
    // ========================================================================

    /**
     * 分页查询物料任务（完整查询能力 + 数据权限过滤）
     *
     * @param orderId        按订单筛选
     * @param materialType   按物料类型筛选
     * @param taskStatus     按任务状态筛选
     * @param assigneeId     负责人可指定；设计人员端固定为自己
     * @param deadlineFrom   截止时间起始
     * @param deadlineTo     截止时间结束
     * @param overdue        true=只看已逾期未完成的
     */
    public Page<TaskLiteVO> pageQuery(Long orderId, MaterialType materialType, TaskStatus taskStatus,
                                      Long assigneeId, LocalDateTime deadlineFrom, LocalDateTime deadlineTo,
                                      Boolean overdue, int page, int size) {
        LoginUser current = UserContext.get();

        LambdaQueryWrapper<MaterialTask> wrapper = new LambdaQueryWrapper<>();

        // 数据权限 —— 这是最核心的拦截，按"从窄到宽"处理
        if (current.getRole() == UserRole.DESIGNER) {
            // 设计人员：只能看分派给自己的（含 assignee_id IS NULL 的"待分派"——给负责人分派时看）
            // 但一般来说设计人员不应该看到待分派的，所以只看自己的
            wrapper.eq(MaterialTask::getAssigneeId, current.getUserId());
        } else if (current.getRole() == UserRole.CS_MANAGER) {
            // 客户经理：只能看自己创建的订单下的任务
            // 先拿到该客户经理创建的所有订单 id
            List<Long> orderIdsOfMine = orderService.listOrderIdsByCreator(current.getUserId());
            if (orderIdsOfMine.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            wrapper.in(MaterialTask::getOrderId, orderIdsOfMine);
        }
        // BIZ_LEADER：不做额外过滤，可看全部

        // 业务筛选条件
        if (orderId != null) wrapper.eq(MaterialTask::getOrderId, orderId);
        if (materialType != null) wrapper.eq(MaterialTask::getMaterialType, materialType);
        if (taskStatus != null) wrapper.eq(MaterialTask::getTaskStatus, taskStatus);

        // assigneeId 只能由负责人手动指定（其他角色上面已经做了硬过滤）
        if (assigneeId != null && current.getRole() == UserRole.BIZ_LEADER) {
            wrapper.eq(MaterialTask::getAssigneeId, assigneeId);
        }

        if (deadlineFrom != null) wrapper.ge(MaterialTask::getTaskDeadline, deadlineFrom);
        if (deadlineTo != null) wrapper.le(MaterialTask::getTaskDeadline, deadlineTo);

        wrapper.orderByAsc(MaterialTask::getTaskDeadline)  // 先看谁快到期了
               .orderByAsc(MaterialTask::getOrderId)
               .orderByAsc(MaterialTask::getTaskNo);

        Page<MaterialTask> taskPage = taskMapper.selectPage(Page.of(page, size), wrapper);

        // 预查：orderNo 映射 + assigneeName 映射
        List<Long> orderIds = taskPage.getRecords().stream().map(MaterialTask::getOrderId).distinct().toList();
        List<Long> assigneeIds = taskPage.getRecords().stream()
                .map(MaterialTask::getAssigneeId).filter(id -> id != null).distinct().toList();

        java.util.Map<Long, String> orderNoMap = new java.util.HashMap<>();
        for (Long oid : orderIds) {
            MaterialOrder o = orderService.getOrderById(oid);
            if (o != null) orderNoMap.put(oid, o.getOrderNo());
        }
        java.util.Map<Long, String> assigneeNameMap = new java.util.HashMap<>();
        for (Long aid : assigneeIds) {
            SysUser u = userService.getById(aid);
            if (u != null) assigneeNameMap.put(aid, u.getRealName());
        }

        Page<TaskLiteVO> result = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        List<TaskLiteVO> records = taskPage.getRecords().stream()
                .map(t -> toTaskLiteVO(t, orderNoMap, assigneeNameMap))
                .filter(item -> !Boolean.TRUE.equals(overdue) || Boolean.TRUE.equals(item.getOverdue()))
                .collect(java.util.stream.Collectors.toList());
        result.setRecords(records);
        return result;
    }

    private TaskLiteVO toTaskLiteVO(MaterialTask t,
                                    java.util.Map<Long, String> orderNoMap,
                                    java.util.Map<Long, String> assigneeNameMap) {
        boolean overdue = t.getTaskDeadline() != null
                && t.getTaskDeadline().isBefore(LocalDateTime.now())
                && !t.getTaskStatus().isFinalOrIgnored();

        return new TaskLiteVO(
                t.getId(), t.getTaskNo(), t.getMaterialType(), t.getMaterialType().getLabel(),
                t.getSizeSpec(), t.getQuantity(), t.getUsage(), t.getTaskDeadline(),
                t.getTaskStatus(), t.getTaskStatus().getLabel(),
                t.getAssigneeId(), assigneeNameMap.get(t.getAssigneeId()),
                t.getVersionCount(), overdue,
                orderNoMap.get(t.getOrderId())   // 额外字段（TaskLiteVO 原设计里没有 orderNo，但工作台展示需要）
        );
    }
}
