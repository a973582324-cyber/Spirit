package com.admaterial.service;

import com.admaterial.common.BusinessException;
import com.admaterial.common.LoginUser;
import com.admaterial.common.UserContext;
import com.admaterial.common.enums.BusinessType;
import com.admaterial.common.enums.ChangeType;
import com.admaterial.common.enums.OrderStatus;
import com.admaterial.common.enums.OperationType;
import com.admaterial.common.enums.TaskStatus;
import com.admaterial.common.enums.UserRole;
import com.admaterial.dto.CreateChangeDTO;
import com.admaterial.dto.CreateOrderDTO;
import com.admaterial.dto.CreateTaskDTO;
import com.admaterial.dto.ReviewOrderDTO;
import com.admaterial.entity.Customer;
import com.admaterial.entity.MaterialOrder;
import com.admaterial.entity.MaterialTask;
import com.admaterial.entity.OperationHistory;
import com.admaterial.entity.OrderChangeRecord;
import com.admaterial.entity.SysUser;
import com.admaterial.mapper.MaterialOrderMapper;
import com.admaterial.mapper.MaterialTaskMapper;
import com.admaterial.mapper.OrderChangeRecordMapper;
import com.admaterial.vo.CustomerVO;
import com.admaterial.vo.OrderDetailVO;
import com.admaterial.vo.OrderListItemVO;
import com.admaterial.vo.TaskLiteVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单 Service —— 创建 / 审核通过 / 详情查询 / 列表查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MaterialOrderMapper orderMapper;
    private final MaterialTaskMapper taskMapper;
    private final OrderChangeRecordMapper changeRecordMapper;
    private final CustomerService customerService;
    private final OperationLogService logService;
    private final SysUserService userService;

    // ========================================================================
    // 1. 创建订单（客户经理）
    // ========================================================================

    /**
     * 客户经理创建订单 + 物料清单
     */
    @Transactional
    public OrderDetailVO create(CreateOrderDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.CS_MANAGER) {
            throw BusinessException.forbidden("只有客户经理可以创建订单");
        }

        Customer customer = customerService.getById(dto.getCustomerId());

        // 物料 deadline 不能晚于订单整体 deadline
        for (CreateTaskDTO t : dto.getTasks()) {
            if (t.getTaskDeadline().isAfter(dto.getOverallDeadline())) {
                throw BusinessException.unprocessable(
                        "物料 [" + t.getMaterialType().getLabel() + "] 的截止时间不能晚于订单整体截止时间");
            }
        }

        // 订单主记录
        MaterialOrder order = new MaterialOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(dto.getCustomerId());
        order.setCustomerContact(buildCustomerContactSnapshot(customer));
        order.setOverallDeadline(dto.getOverallDeadline());
        order.setOrderStatus(OrderStatus.DRAFT);
        order.setNotes(dto.getNotes());
        order.setCreateBy(current.getUserId());
        orderMapper.insert(order);

        // 批量创建物料任务
        List<MaterialTask> tasks = new ArrayList<>();
        int taskNo = 1;
        for (CreateTaskDTO t : dto.getTasks()) {
            MaterialTask task = new MaterialTask();
            task.setOrderId(order.getId());
            task.setTaskNo(taskNo++);
            task.setMaterialType(t.getMaterialType());
            task.setSizeSpec(t.getSizeSpec());
            task.setQuantity(t.getQuantity() != null ? t.getQuantity() : 1);
            task.setUsage(t.getUsage());
            task.setRefNotes(t.getRefNotes());
            task.setTaskDeadline(t.getTaskDeadline());
            task.setAssigneeId(null);
            task.setTaskStatus(TaskStatus.PENDING_ASSIGN);
            task.setVersionCount(0);
            tasks.add(task);
        }
        for (MaterialTask t : tasks) {
            taskMapper.insert(t);
        }

        logService.log(
                BusinessType.ORDER, order.getId(),
                OperationType.CREATE,
                "创建订单 " + order.getOrderNo() + "（" + customer.getCustomerName() + "），含 " + tasks.size() + " 项物料",
                current.getUserId(), current.getRealName()
        );

        log.info("[Order] 创建订单成功: orderNo={}, taskCount={}", order.getOrderNo(), tasks.size());
        return assembleDetail(order.getId());
    }

    // ========================================================================
    // 2. 负责人审核通过
    // ========================================================================

    /**
     * 业务负责人审核通过订单
     * —— 状态机约束：只有 DRAFT 才能 CONFIRMED
     */
    @Transactional
    public OrderDetailVO confirm(Long orderId, ReviewOrderDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.BIZ_LEADER) {
            throw BusinessException.forbidden("只有业务负责人可以审核订单");
        }

        MaterialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw BusinessException.conflict(
                    "订单当前状态 [" + order.getOrderStatus().getLabel() + "] 不允许审核通过");
        }

        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setConfirmBy(current.getUserId());
        order.setConfirmTime(LocalDateTime.now());
        orderMapper.updateById(order);

        logService.log(
                BusinessType.ORDER, order.getId(),
                OperationType.CONFIRM,
                "审核通过物料清单和截止时间" + (dto != null && dto.getReviewNotes() != null
                        ? "，备注: " + dto.getReviewNotes() : ""),
                current.getUserId(), current.getRealName()
        );

        log.info("[Order] 负责人审核通过: orderId={}, confirmBy={}", orderId, current.getRealName());
        return assembleDetail(orderId);
    }

    // ========================================================================
    // 3. 订单详情（聚合视图）
    // ========================================================================

    public OrderDetailVO getDetail(Long orderId) {
        MaterialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在，id=" + orderId);
        }

        LoginUser current = UserContext.get();
        if (current.getRole() == UserRole.CS_MANAGER
                && !order.getCreateBy().equals(current.getUserId())) {
            throw BusinessException.forbidden("您无权查看该订单");
        }

        return assembleDetail(orderId);
    }

    private OrderDetailVO assembleDetail(Long orderId) {
        MaterialOrder order = orderMapper.selectById(orderId);
        Customer customer = order.getCustomerId() != null
                ? customerService.getById(order.getCustomerId()) : null;

        List<MaterialTask> rawTasks = taskMapper.selectList(
                new LambdaQueryWrapper<MaterialTask>()
                        .eq(MaterialTask::getOrderId, orderId)
                        .orderByAsc(MaterialTask::getTaskNo)
        );

        // 预查所有 assignee 用户姓名（批量查，避免 N+1）
        List<Long> assigneeIds = rawTasks.stream()
                .map(MaterialTask::getAssigneeId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> assigneeNameMap = new HashMap<>();
        if (!assigneeIds.isEmpty()) {
            for (Long id : assigneeIds) {
                SysUser u = userService.getById(id);
                if (u != null) assigneeNameMap.put(id, u.getRealName());
            }
        }

        List<TaskLiteVO> tasks = rawTasks.stream()
                .map(t -> toTaskLiteVO(t, assigneeNameMap))
                .collect(Collectors.toList());

        return new OrderDetailVO(order, CustomerVO.from(customer), tasks, null, null);
    }

    private TaskLiteVO toTaskLiteVO(MaterialTask t, Map<Long, String> assigneeNameMap) {
        boolean overdue = t.getTaskDeadline() != null
                && t.getTaskDeadline().isBefore(LocalDateTime.now())
                && !t.getTaskStatus().isFinalOrIgnored();

        return new TaskLiteVO(
                t.getId(), t.getTaskNo(), t.getMaterialType(), t.getMaterialType().getLabel(),
                t.getSizeSpec(), t.getQuantity(), t.getUsage(), t.getTaskDeadline(),
                t.getTaskStatus(), t.getTaskStatus().getLabel(),
                t.getAssigneeId(), assigneeNameMap.get(t.getAssigneeId()),
                t.getVersionCount(), overdue, null
        );
    }

    // ========================================================================
    // 4. 订单列表查询
    // ========================================================================

    /**
     * 分页查询订单（完整查询能力）
     * @param customerName 模糊搜客户名（内部先查 customer 表拿 id 列表）
     * @param status       订单状态筛选
     * @param deadlineFrom 整体 deadline 起始
     * @param deadlineTo   整体 deadline 结束
     * @param overdue      true=只看已逾期未完成的
     */
    public Page<OrderListItemVO> pageQuery(String customerName, OrderStatus status,
                                           LocalDateTime deadlineFrom, LocalDateTime deadlineTo,
                                           Boolean overdue, int page, int size) {
        LoginUser current = UserContext.get();

        LambdaQueryWrapper<MaterialOrder> wrapper = new LambdaQueryWrapper<>();

        // 数据权限
        if (current.getRole() == UserRole.CS_MANAGER) {
            wrapper.eq(MaterialOrder::getCreateBy, current.getUserId());
        }

        // customerName 模糊搜索 → 先查 customer 表拿 id 列表
        if (customerName != null && !customerName.isBlank()) {
            List<Customer> matchedCustomers = customerService.pageQuery(customerName.trim(), 0, 9999).getRecords();
            List<Long> customerIds = matchedCustomers.stream().map(Customer::getId).toList();
            if (customerIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            wrapper.in(MaterialOrder::getCustomerId, customerIds);
        }

        if (status != null) {
            wrapper.eq(MaterialOrder::getOrderStatus, status);
        }
        if (deadlineFrom != null) {
            wrapper.ge(MaterialOrder::getOverallDeadline, deadlineFrom);
        }
        if (deadlineTo != null) {
            wrapper.le(MaterialOrder::getOverallDeadline, deadlineTo);
        }

        wrapper.orderByDesc(MaterialOrder::getCreateTime);

        Page<MaterialOrder> orderPage = orderMapper.selectPage(Page.of(page, size), wrapper);

        // 预查所有订单关联的客户（批量）
        List<Long> customerIds = orderPage.getRecords().stream()
                .map(MaterialOrder::getCustomerId).distinct().collect(Collectors.toList());
        Map<Long, String> customerNameMap = new HashMap<>();
        for (Long id : customerIds) {
            Customer c = customerService.getById(id);
            if (c != null) customerNameMap.put(id, c.getCustomerName());
        }

        // 预查创建人/审核人姓名
        List<Long> userIds = orderPage.getRecords().stream()
                .flatMap(o -> java.util.stream.Stream.of(o.getCreateBy(), o.getConfirmBy()))
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        for (Long id : userIds) {
            SysUser u = userService.getById(id);
            if (u != null) userNameMap.put(id, u.getRealName());
        }

        Page<OrderListItemVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(orderPage.getRecords().stream()
                .map(o -> toListItemVO(o, customerNameMap, userNameMap))
                .filter(item -> !Boolean.TRUE.equals(overdue) || Boolean.TRUE.equals(item.getOverdue()))
                .collect(Collectors.toList()));
        return result;
    }

    private OrderListItemVO toListItemVO(MaterialOrder o,
                                         Map<Long, String> customerNameMap,
                                         Map<Long, String> userNameMap) {
        String customerName = customerNameMap.getOrDefault(o.getCustomerId(), "未知客户");
        int taskCount = (int) taskMapper.countByOrderId(o.getId());
        int confirmedCount = (int) taskMapper.countConfirmedByOrderId(o.getId());

        boolean overdue = o.getOverallDeadline() != null
                && o.getOverallDeadline().isBefore(LocalDateTime.now())
                && o.getOrderStatus() != OrderStatus.COMPLETED
                && o.getOrderStatus() != OrderStatus.CANCELLED;

        return new OrderListItemVO(
                o.getId(), o.getOrderNo(), customerName, o.getOverallDeadline(),
                o.getOrderStatus(), o.getOrderStatus().getLabel(),
                taskCount, confirmedCount,
                userNameMap.get(o.getCreateBy()),
                userNameMap.get(o.getConfirmBy()),
                overdue
        );
    }

    // ========================================================================
    // 工具方法
    // ========================================================================

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String generateOrderNo() {
        String datePrefix = LocalDateTime.now().format(DATE_FMT);
        String maxOrderNo = orderMapper.findMaxOrderNoOfDay(datePrefix);

        int nextSeq = 1;
        if (maxOrderNo != null && maxOrderNo.length() >= 13) {
            try {
                int currentSeq = Integer.parseInt(maxOrderNo.substring(10));
                nextSeq = currentSeq + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return "MO" + datePrefix + String.format("%03d", nextSeq);
    }

    // ========================================================================
    // 5. 内部方法 —— 给 TaskService 调用（客户确认后自动联动）
    // ========================================================================

    /**
     * 按 id 取订单（公开的内部方法，不做数据权限校验 —— 调用方自己负责）
     */
    public MaterialOrder getOrderById(Long id) {
        MaterialOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw BusinessException.notFound("订单不存在, id=" + id);
        }
        return o;
    }

    /**
     * 订单状态自动推进到 PENDING_DELIVERY
     * —— 由 TaskService.checkAndPromoteOrderStatus() 在客户确认后自动调用
     * —— 无 @Transactional：调用方已在事务里
     */
    public void markPendingDelivery(Long orderId) {
        MaterialOrder order = orderMapper.selectById(orderId);
        if (order == null) return;
        if (order.getOrderStatus() != OrderStatus.COMPLETED
                && order.getOrderStatus() != OrderStatus.CANCELLED) {
            order.setOrderStatus(OrderStatus.PENDING_DELIVERY);
            orderMapper.updateById(order);
        }
    }

    /**
     * 查某客户经理创建的所有订单 id（给 TaskService 做数据权限过滤用）
     */
    public List<Long> listOrderIdsByCreator(Long createBy) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<MaterialOrder>()
                        .eq(MaterialOrder::getCreateBy, createBy)
                        .select(MaterialOrder::getId)
        ).stream().map(MaterialOrder::getId).toList();
    }

    // ========================================================================
    // 6. 发起订单变更（客户经理）—— 强制双写机制
    // ========================================================================

    /**
     * 客户经理对已确认订单发起变更
     * —— 文件规则："物料清单和截止时间确认后不能直接覆盖原要求；
     *               新增物料或改变截止时间必须留下变更记录"
     * —— DRAFT 订单不走这个流程（草稿可以直接编辑）
     * —— COMPLETED / CANCELLED 终态不允许变更
     */
    @Transactional
    public OrderChangeRecord addChange(Long orderId, CreateChangeDTO dto) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.CS_MANAGER) {
            throw BusinessException.forbidden("只有客户经理可以发起订单变更");
        }

        MaterialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        // 归属校验
        if (!order.getCreateBy().equals(current.getUserId())) {
            throw BusinessException.forbidden("您无权变更非自己创建的订单");
        }

        // 状态机校验
        OrderStatus status = order.getOrderStatus();
        if (status == OrderStatus.DRAFT) {
            throw BusinessException.unprocessable(
                    "草稿订单直接编辑物料清单即可，无需走变更流程");
        }
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw BusinessException.conflict("终态订单（已完成/已取消）不允许变更");
        }

        // 1. 先写变更记录（事务内保证落库）
        OrderChangeRecord record = new OrderChangeRecord();
        record.setOrderId(orderId);
        record.setChangeType(dto.getChangeType());
        record.setChangeTarget(dto.getChangeTarget());
        record.setChangeBefore(dto.getChangeBefore());
        record.setChangeAfter(dto.getChangeAfter());
        record.setChangeReason(dto.getChangeReason());
        record.setChangeBy(current.getUserId());
        changeRecordMapper.insert(record);

        // 2. 根据变更类型做对应业务操作
        applyChange(order, dto);

        // 3. 操作历史
        String changeLabel = dto.getChangeType() != null ? dto.getChangeType().getLabel() : "变更";
        logService.log(BusinessType.CHANGE, orderId, OperationType.CHANGE,
                changeLabel + "：" + (dto.getChangeTarget() != null ? dto.getChangeTarget() : "")
                        + "，原因：" + dto.getChangeReason(),
                current.getUserId(), current.getRealName());

        log.info("[Order] 发起变更: orderId={}, type={}", orderId, dto.getChangeType());
        return record;
    }

    /**
     * 根据变更类型执行实际更新操作
     * —— 外部订单整体 deadline 变更直接 UPDATE 主表
     * —— 物料新增/修改/移除由 TaskService 处理
     */
    private void applyChange(MaterialOrder order, CreateChangeDTO dto) {
        ChangeType type = dto.getChangeType();
        if (type == null) return;

        switch (type) {
            case ORDER_DEADLINE_CHANGE -> {
                try {
                    LocalDateTime newDeadline = LocalDateTime.parse(dto.getChangeAfter());
                    order.setOverallDeadline(newDeadline);
                    orderMapper.updateById(order);
                } catch (Exception e) {
                    throw BusinessException.unprocessable("整体截止时间变更格式错误");
                }
            }
            case TASK_ADD -> {
                if (dto.getNewTask() == null) {
                    throw BusinessException.unprocessable("新增物料时必须携带 newTask 嵌套数据");
                }
                // 序号 = 当前最大 taskNo + 1
                Integer maxNo = taskMapper.selectList(
                        new LambdaQueryWrapper<MaterialTask>()
                                .eq(MaterialTask::getOrderId, order.getId())
                                .orderByDesc(MaterialTask::getTaskNo)
                                .last("LIMIT 1")
                ).stream().findFirst().map(MaterialTask::getTaskNo).orElse(0);

                MaterialTask newTask = new MaterialTask();
                newTask.setOrderId(order.getId());
                newTask.setTaskNo(maxNo + 1);
                newTask.setMaterialType(dto.getNewTask().getMaterialType());
                newTask.setSizeSpec(dto.getNewTask().getSizeSpec());
                newTask.setQuantity(dto.getNewTask().getQuantity() != null ? dto.getNewTask().getQuantity() : 1);
                newTask.setUsage(dto.getNewTask().getUsage());
                newTask.setRefNotes(dto.getNewTask().getRefNotes());
                newTask.setTaskDeadline(dto.getNewTask().getTaskDeadline());
                newTask.setAssigneeId(null);
                newTask.setTaskStatus(TaskStatus.PENDING_ASSIGN);
                newTask.setVersionCount(0);
                taskMapper.insert(newTask);
            }
            default -> {
                // TASK_REMOVE / TASK_MODIFY / TASK_DEADLINE_CHANGE / OTHER
                // 只写变更记录，主表/任务表的字段更新由调用方（如 Controller 或后续 Service）处理
                // 课程 demo 阶段：变更记录本身就是主要交付物，主表同步可按需扩展
                log.info("[Order] 变更类型 {} 仅记录，主表同步由调用方处理", type);
            }
        }
    }

    /**
     * 查订单下所有变更记录
     */
    public List<OrderChangeRecord> listChangesByOrderId(Long orderId) {
        return changeRecordMapper.selectList(
                new LambdaQueryWrapper<OrderChangeRecord>()
                        .eq(OrderChangeRecord::getOrderId, orderId)
                        .orderByAsc(OrderChangeRecord::getChangeTime)
        );
    }

    // ========================================================================
    // 7. 订单完成交付（客户经理）
    // ========================================================================

    /**
     * 客户经理标记订单完成交付
     * —— 前置：所有物料都 CLIENT_CONFIRMED / CANCELLED；订单状态 PENDING_DELIVERY
     */
    @Transactional
    public MaterialOrder completeOrder(Long orderId) {
        LoginUser current = UserContext.get();
        if (current.getRole() != UserRole.CS_MANAGER) {
            throw BusinessException.forbidden("只有客户经理可以完成订单交付");
        }

        MaterialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        // 归属校验
        if (!order.getCreateBy().equals(current.getUserId())) {
            throw BusinessException.forbidden("只能完成自己创建的订单");
        }

        // 状态机校验：PENDING_DELIVERY → COMPLETED
        if (order.getOrderStatus() != OrderStatus.PENDING_DELIVERY) {
            throw BusinessException.conflict(
                    "订单当前状态 [" + order.getOrderStatus().getLabel()
                            + "] 不允许完成交付（必须先完成所有物料的客户确认）");
        }

        // 关键业务规则二次校验：所有物料都已处理完
        long remaining = taskMapper.countUnfinishedByOrderId(orderId);
        if (remaining > 0) {
            throw new BusinessException(409,
                    "还有 " + remaining + " 项物料未客户确认，无法完成订单",
                    "unfinishedTasks");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        orderMapper.updateById(order);

        logService.log(BusinessType.ORDER, orderId, OperationType.COMPLETE,
                "订单完成交付",
                current.getUserId(), current.getRealName());

        log.info("[Order] 订单完成: orderId={}", orderId);
        return order;
    }

    // ========================================================================
    // 8. 订单取消（客户经理 / 负责人）
    // ========================================================================

    /**
     * 取消订单
     * —— 客户经理可以取消自己创建的；负责人可以取消任意订单
     * —— 状态机：除终态外的所有状态都可以取消
     */
    @Transactional
    public MaterialOrder cancelOrder(Long orderId, String cancelReason) {
        LoginUser current = UserContext.get();
        boolean isLeader = current.getRole() == UserRole.BIZ_LEADER;
        boolean isOwner = current.getRole() == UserRole.CS_MANAGER;

        MaterialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        // 归属校验
        if (!isLeader && !(isOwner && order.getCreateBy().equals(current.getUserId()))) {
            throw BusinessException.forbidden("您无权取消该订单");
        }

        // 状态机校验：终态（COMPLETED / CANCELLED）不允许再取消
        if (order.getOrderStatus() == OrderStatus.COMPLETED
                || order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw BusinessException.conflict(
                    "终态订单 [" + order.getOrderStatus().getLabel() + "] 不允许取消");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelBy(current.getUserId());
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(cancelReason != null ? cancelReason : "");
        orderMapper.updateById(order);

        // 订单取消后，所有非终态物料也标记为 CANCELLED
        List<MaterialTask> unfinishedTasks = taskMapper.selectList(
                new LambdaQueryWrapper<MaterialTask>()
                        .eq(MaterialTask::getOrderId, orderId)
                        .notIn(MaterialTask::getTaskStatus,
                                TaskStatus.CLIENT_CONFIRMED, TaskStatus.CANCELLED)
        );
        for (MaterialTask t : unfinishedTasks) {
            t.setTaskStatus(TaskStatus.CANCELLED);
            taskMapper.updateById(t);
        }

        logService.log(BusinessType.ORDER, orderId, OperationType.CANCEL,
                "取消订单，原因：" + cancelReason + "（同时取消了 " + unfinishedTasks.size() + " 项未完成物料）",
                current.getUserId(), current.getRealName());

        log.info("[Order] 订单取消: orderId={}, unfinishedTasksCancelled={}", orderId, unfinishedTasks.size());
        return order;
    }

    private String buildCustomerContactSnapshot(Customer c) {
        StringBuilder sb = new StringBuilder();
        if (c.getContactPerson() != null) sb.append(c.getContactPerson());
        if (c.getContactPhone() != null) sb.append(" ").append(c.getContactPhone());
        return sb.toString().trim();
    }
}
