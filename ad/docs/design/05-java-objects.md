# Java 后端对象模型

> 技术栈：Spring Boot 3.x + MyBatis-Plus + Java 17
> 分层：Controller → Service → Mapper → MySQL
> 对象分三类：Entity（表映射）、DTO（入参）、VO（出参）

---

## 一、Entity 层（与 schema.sql 一一对应）

### 1.1 SysUser（对应 sys_user 表）

```java
@TableName("sys_user")
public class SysUser {
    private Long id;
    private String username;
    private String password;       // demo 用明文，生产 BCrypt
    private String realName;
    private UserRole role;        // 枚举: CS_MANAGER, BIZ_LEADER, DESIGNER
    private String phone;
    private String email;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

public enum UserRole {
    CS_MANAGER,   // 客户经理
    BIZ_LEADER,   // 业务负责人
    DESIGNER      // 设计制作人员
}
```

### 1.2 Customer（对应 customer 表）

```java
@TableName("customer")
public class Customer {
    private Long id;
    private String customerName;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String notes;
    private Long createBy;        // 冗余 sys_user.id
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 1.3 MaterialOrder（对应 material_order 表）

```java
@TableName("material_order")
public class MaterialOrder {
    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerContact;     // 快照字段
    private LocalDateTime overallDeadline;
    private OrderStatus orderStatus;
    private String notes;
    private Long createBy;
    private Long confirmBy;
    private LocalDateTime confirmTime;
    private LocalDateTime completeTime;
    private Long cancelBy;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    PENDING_DELIVERY,
    COMPLETED,
    CANCELLED
}
```

### 1.4 MaterialTask（对应 material_task 表）

```java
@TableName("material_task")
public class MaterialTask {
    private Long id;
    private Long orderId;
    private Integer taskNo;
    private MaterialType materialType;
    private String sizeSpec;
    private Integer quantity;
    private String usage;
    private String refNotes;
    private LocalDateTime taskDeadline;
    private Long assigneeId;       // null 表示待分派
    private TaskStatus taskStatus;
    private Integer versionCount;
    private LocalDateTime lastSubmitTime;
    private LocalDateTime lastFeedbackTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

public enum MaterialType {
    POSTER,       // 海报
    STANDEE,      // 展架
    MENU,         // 菜单
    BROCHURE,     // 宣传册
    ONLINE_IMAGE  // 线上配图
}

public enum TaskStatus {
    PENDING_ASSIGN,           // 待分派
    ASSIGNED,                 // 已分派
    IN_PROGRESS,              // 制作中
    PENDING_CLIENT_REVIEW,    // 待客户审核
    NEEDS_REVISION,           // 需修改
    CLIENT_CONFIRMED,         // 客户已确认
    CANCELLED
}
```

### 1.5 DesignVersion（对应 design_version 表）

```java
@TableName("design_version")
public class DesignVersion {
    private Long id;
    private Long taskId;
    private Integer versionNo;
    private String fileUrl;
    private String fileName;
    private Long fileSizeBytes;
    private Long uploadBy;
    private LocalDateTime uploadTime;
    private String versionNotes;
}
```

### 1.6 ModificationFeedback（对应 modification_feedback 表）

```java
@TableName("modification_feedback")
public class ModificationFeedback {
    private Long id;
    private Long taskId;
    private Integer targetVersionNo;
    private String feedbackContent;
    private String feedbackSource;
    private Long recordBy;
    private LocalDateTime recordTime;
    private Boolean processed;
    private LocalDateTime processTime;
}
```

### 1.7 OrderChangeRecord（对应 order_change_record 表）

```java
@TableName("order_change_record")
public class OrderChangeRecord {
    private Long id;
    private Long orderId;
    private ChangeType changeType;
    private String changeTarget;
    private String changeBefore;
    private String changeAfter;
    private String changeReason;
    private Long changeBy;
    private LocalDateTime changeTime;
}

public enum ChangeType {
    TASK_ADD,
    TASK_REMOVE,
    TASK_MODIFY,
    TASK_DEADLINE_CHANGE,
    ORDER_DEADLINE_CHANGE,
    OTHER
}
```

### 1.8 OperationHistory（对应 operation_history 表）

```java
@TableName("operation_history")
public class OperationHistory {
    private Long id;
    private BusinessType businessType;
    private Long businessId;
    private OperationType operationType;
    private String operationDesc;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
}

public enum BusinessType { ORDER, TASK, VERSION, FEEDBACK, CHANGE }

public enum OperationType {
    CREATE, CONFIRM, REJECT,
    ASSIGN, BATCH_ASSIGN,
    UPLOAD, SUBMIT_NEW_VERSION,
    FEEDBACK,
    CLIENT_CONFIRM,
    CANCEL, COMPLETE,
    CHANGE,
    MARK_PROCESSED
}
```

---

## 二、DTO 层（入参，只接收前端提交的数据）

> DTO 不直接映射数据库，只表达"用户这次提交了什么"。Service 层做 DTO → Entity 的转换。

### 2.1 订单创建 DTO

```java
// 客户经理创建订单时提交
@Valid
public record CreateOrderDTO(
    @NotNull Long customerId,
    @NotNull @Future LocalDateTime overallDeadline,
    @Size(max = 1000) String notes,
    @NotEmpty List<CreateTaskDTO> tasks   // 至少 1 项
) {}

@Valid
public record CreateTaskDTO(
    @NotNull MaterialType materialType,
    @NotBlank String sizeSpec,
    @Min(1) Integer quantity,    // 默认 1
    String usage,
    String refNotes,
    @NotNull @Future LocalDateTime taskDeadline   // 必须 <= overallDeadline（Service 层校验）
) {}
```

### 2.2 订单变更 DTO

```java
// 客户经理发起变更
public record CreateChangeDTO(
    @NotNull ChangeType changeType,
    String changeTarget,
    String changeAfter,
    @NotBlank String changeReason,         // 必填
    // 可选嵌套
    Long taskId,                           // 如果是 TASK_MODIFY / TASK_DEADLINE_CHANGE
    CreateTaskDTO newTask                  // 如果是 TASK_ADD
) {}
```

### 2.3 任务分派 DTO

```java
// 负责人分派
public record AssignTaskDTO(@NotNull Long assigneeId) {}
public record BatchAssignTaskDTO(List<AssignItemDTO> items) {}
public record AssignItemDTO(Long taskId, Long assigneeId) {}
```

### 2.4 设计稿上传 DTO

```java
// 设计人员上传（multipart）
public record UploadVersionDTO(
    @NotNull MultipartFile file,
    @NotBlank String versionNotes
) {}
```

### 2.5 客户意见 DTO

```java
// 客户经理录入
public record CreateFeedbackDTO(
    @NotNull Integer targetVersionNo,
    @NotBlank @Size(max = 2000) String feedbackContent,
    String feedbackSource
) {}
```

### 2.6 客户确认 DTO

```java
public record ClientConfirmDTO(@NotNull Integer confirmedVersionNo) {}
```

### 2.7 订单审核 DTO

```java
public record ReviewOrderDTO(String reviewNotes) {}
public record RejectOrderDTO(@NotBlank String rejectReason) {}
public record CancelDTO(@NotBlank String reason) {}
```

### 2.8 查询 DTO（支持分页）

```java
public record OrderQueryDTO(
    String customerName,
    OrderStatus orderStatus,
    LocalDateTime overallDeadlineFrom,
    LocalDateTime overallDeadlineTo,
    Long createBy,
    int page, int size
) {}

public record TaskQueryDTO(
    Long orderId,
    MaterialType materialType,
    TaskStatus taskStatus,
    Long assigneeId,
    LocalDateTime taskDeadlineFrom,
    LocalDateTime taskDeadlineTo,
    Boolean overdue,          // true 只看已逾期
    int page, int size
) {}
```

---

## 三、VO 层（出参，返回给前端的聚合视图）

> VO 不直接映射数据库表，而是按"页面需要什么"组装。可以跨表聚合。

### 3.1 列表视图（工作台/列表页用）

```java
// 订单列表（客户经理/负责人工作台）
public record OrderListItemVO(
    Long orderId,
    String orderNo,
    String customerName,
    LocalDateTime overallDeadline,
    String orderStatusLabel,       // 中文状态 "制作中"
    int taskCount,
    int confirmedCount,
    String createByName,
    String confirmByName,
    boolean overdue                // 应用层计算：overallDeadline < now 且非 COMPLETED
) {}

// 任务列表（设计人员工作台 / 负责人分派中心）
public record TaskListItemVO(
    Long taskId,
    String orderNo,
    MaterialType materialType,
    String materialTypeLabel,      // 中文 "海报"
    String sizeSpec,
    int quantity,
    LocalDateTime taskDeadline,
    TaskStatus taskStatus,
    String taskStatusLabel,        // 中文
    String assigneeName,
    int versionCount,
    boolean overdue
) {}

// 客户列表（CS_MANAGER 用）
public record CustomerVO(
    Long id,
    String customerName,
    String contactPerson,
    String contactPhone,
    String contactEmail,
    String address,
    String notes
) {}
```

### 3.2 详情视图（订单详情页 / 任务详情页用）

```java
// 订单详情（聚合整个订单上下文，一次返回所有 Tab 需要的数据）
public record OrderDetailVO(
    MaterialOrder order,                    // 主订单
    CustomerVO customer,                    // 客户信息
    List<TaskDetailLiteVO> tasks,            // 下挂所有物料任务（含版本数、确认状态）
    List<OrderChangeRecord> changes,        // 所有变更记录
    List<OperationHistory> operations       // 所有操作历史
) {}

// 任务详情（设计人员/客户经理看任务时用）
public record TaskDetailVO(
    MaterialTask task,
    String orderNo,
    String orderStatusLabel,
    String assigneeName,
    List<DesignVersion> versions,           // 所有历史版本
    List<FeedbackVO> feedbacks              // 所有客户意见
) {}

// 客户意见 VO（带目标版本号 + 状态）
public record FeedbackVO(
    Long feedbackId,
    int targetVersionNo,
    String feedbackContent,
    String feedbackSource,
    String recordByName,
    LocalDateTime recordTime,
    boolean processed,
    LocalDateTime processTime
) {}
```

### 3.3 工作台概览（首页卡片用）

```java
// 客户经理工作台概览
public record CSWorkbenchStatsVO(
    int ordersInProgress,
    int tasksDueThisWeek,
    int versionsPendingClientReview,
    int newCustomersThisMonth
) {}

// 负责人工作台概览
public record LeaderWorkbenchStatsVO(
    int ordersDraft,          // 待审核
    int tasksPendingAssign,   // 待分派
    int tasksOverdue,         // 已逾期
    int ordersRisk            // 3 天内到期且未完成的订单数
) {}

// 设计人员工作台概览
public record DesignerWorkbenchStatsVO(
    int tasksInProgress,
    int tasksWaitingClient,
    int tasksNeedRevision,
    int tasksConfirmed,
    List<TaskListItemVO> urgentTasks    // 剩余 <3 天的任务
) {}
```

---

## 四、Service 层职责拆分

| Service | 职责 | 关键方法 |
| --- | --- | --- |
| `AuthService` | 登录、token 发放 | `login(username, password)` |
| `CustomerService` | 客户 CRUD | `create / update / delete / get / pageQuery` |
| `OrderService` | 订单完整生命周期 | `create / getDetail / confirm / reject / addChange / complete / cancel / pageQuery` |
| `TaskService` | 任务完整生命周期 | `getDetail / assign / batchAssign / uploadVersion / submitFeedback / clientConfirm / cancel / pageQuery` |
| `WorkbenchService` | 三个工作台概览数据 | `getCSStats / getLeaderStats / getDesignerStats` |
| `OperationLogService` | 操作历史（内部用） | `log(type, id, op, desc, userId)` —— 统一入口，被其他 Service 调用 |

### OrderService.complete() 核心逻辑伪代码

```java
@Transactional
public MaterialOrder completeOrder(Long orderId, Long operatorId) {
    MaterialOrder order = mapper.selectById(orderId);
    // 1. 权限
    if (!order.getCreateBy().equals(operatorId)) {
        throw new BusinessException(403, "只有订单创建人可完成订单");
    }
    // 2. 状态前置
    if (order.getOrderStatus() != PENDING_DELIVERY) {
        throw new BusinessException(409, "订单当前状态不允许完成");
    }
    // 3. 关键业务规则：所有物料都 CLIENT_CONFIRMED / CANCELLED
    long remainingTasks = taskMapper.countUnfinished(orderId);
    if (remainingTasks > 0) {
        throw new BusinessException(409, "还有 " + remainingTasks + " 项物料未客户确认");
    }
    // 4. 执行
    order.setOrderStatus(COMPLETED);
    order.setCompleteTime(LocalDateTime.now());
    mapper.updateById(order);
    // 5. 留痕
    operationLogService.log(BusinessType.ORDER, orderId, OperationType.COMPLETE,
        "客户经理完成订单交付", operatorId);
    return order;
}
```

---

## 五、状态流转集中管理

### OrderStatusMachine（订单状态机）

```java
public class OrderStatusMachine {
    // Map<当前状态, 允许进入的状态集合>
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        DRAFT,            Set.of(CONFIRMED, CANCELLED),
        CONFIRMED,        Set.of(IN_PROGRESS, CANCELLED),
        IN_PROGRESS,      Set.of(PENDING_DELIVERY, CANCELLED, COMPLETED),
        PENDING_DELIVERY, Set.of(COMPLETED, CANCELLED),
        COMPLETED,        Set.of(),          // 终态
        CANCELLED,        Set.of()           // 终态
    );

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
```

### TaskStatusMachine（任务状态机）

```java
public class TaskStatusMachine {
    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
        PENDING_ASSIGN,        Set.of(ASSIGNED, CANCELLED),
        ASSIGNED,              Set.of(IN_PROGRESS, PENDING_CLIENT_REVIEW, CANCELLED),
        IN_PROGRESS,           Set.of(PENDING_CLIENT_REVIEW, NEEDS_REVISION, CANCELLED),
        PENDING_CLIENT_REVIEW, Set.of(NEEDS_REVISION, CLIENT_CONFIRMED, CANCELLED),
        NEEDS_REVISION,        Set.of(PENDING_CLIENT_REVIEW, CANCELLED),
        CLIENT_CONFIRMED,      Set.of(),
        CANCELLED,             Set.of()
    );

    public static boolean canTransition(TaskStatus from, TaskStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
```

**为什么要集中管理？** 因为文件明确要求"清楚的状态变化和不允许发生的状态跳转"——任何 `UPDATE` 状态的代码必须先经过状态机校验，非法跳转直接抛异常，保证业务规则不被绕开。
