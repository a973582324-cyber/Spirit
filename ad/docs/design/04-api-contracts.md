# 接口清单（RESTful API Contract）

> 基础路径：`/api`
> 统一响应格式：`{ code: 200, message: "ok", data: ... }`
> 统一异常格式：`{ code: 403|404|409|422, message: "...", errorKey: "..." }`

## 通用约定

### 鉴权
- 所有接口（除登录外）需在 Header 携带 `Authorization: Bearer <token>`
- Token 内携带：`userId / role / realName`
- 后端 Controller 层通过 `@PreAuthorize` 或自定义注解 `@RequireRole("CS_MANAGER")` 校验

### 分页
- 查询列表接口统一支持：`page`（从 0 起）、`size`（默认 20）
- 返回：`{ content: [...], totalElements: N, totalPages: M, page, size }`

---

## 一、认证接口（所有人）

| 方法 | 路径 | 权限 | 输入 | 输出 | 说明 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auth/login` | 公开 | `{username, password}` | `{token, userId, role, realName}` | 登录拿 token |
| POST | `/api/auth/logout` | 已登录 | Header token | 200 空 | 服务端可做黑名单；demo 直接返回 |

---

## 二、客户管理（客户经理 CS_MANAGER）

| 方法 | 路径 | 权限 | 输入 | 输出 | 说明 |
| --- | --- | --- | --- | --- | --- |
| GET  | `/api/customers` | CS_MANAGER | Query: `customerName`(模糊), `contactPhone`, `page`, `size` | 分页 `CustomerVO` | 列表查询 |
| GET  | `/api/customers/{id}` | CS_MANAGER | - | `CustomerVO` | 详情 |
| POST | `/api/customers` | CS_MANAGER | `CreateCustomerDTO` | `CustomerVO` | 新建，自动记 `createBy = 当前用户` |
| PUT  | `/api/customers/{id}` | CS_MANAGER | `UpdateCustomerDTO` | `CustomerVO` | 更新（不能改 createBy） |

```java
// CreateCustomerDTO
customerName: String   // 必填
contactPerson: String  // 必填
contactPhone: String   // 必填
contactEmail: String
address: String
notes: String
```

---

## 三、物料订单管理

### 3.1 订单创建（客户经理）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/orders` | CS_MANAGER | `CreateOrderDTO` | `OrderDetailVO` |

```java
// CreateOrderDTO
customerId: Long            // 必填
overallDeadline: DateTime   // 必填
notes: String
tasks: List<CreateTaskDTO>  // 至少 1 项

// CreateTaskDTO（嵌套在订单创建里）
materialType: "POSTER" | "STANDEE" | "MENU" | "BROCHURE" | "ONLINE_IMAGE"
sizeSpec: String
quantity: int               // 默认 1
usage: String
refNotes: String
taskDeadline: DateTime      // 必须 <= overallDeadline
```

**业务规则**：
- 自动生成 `orderNo`（格式 `MO` + 年月日 + 3 位序号，如 `MO20261104001`）
- 自动设置 `createBy = 当前 userId`
- 订单初始状态 `DRAFT`，物料任务初始状态 `PENDING_ASSIGN`
- 校验：`taskDeadline <= overallDeadline`，否则 422

### 3.2 订单查询（所有角色可查，但有数据权限）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| GET  | `/api/orders` | 所有人（数据权限不同） | Query 见下 | 分页 `OrderListItemVO` |
| GET  | `/api/orders/{id}` | 所有人 | - | `OrderDetailVO` |

**数据权限**：
- `CS_MANAGER`：只能看到 `createBy = 自己` 的订单
- `BIZ_LEADER`：能看到全部订单
- `DESIGNER`：能看到分派了自己任务的订单（需 JOIN material_task WHERE assigneeId = 自己）

**Query 参数**：
```
customerName: 模糊搜索客户名
orderStatus: DRAFT / CONFIRMED / IN_PROGRESS / PENDING_DELIVERY / COMPLETED / CANCELLED
overallDeadlineFrom / overallDeadlineTo: 日期范围
page / size
```

```java
// OrderListItemVO
orderId: Long
orderNo: String
customerName: String
overallDeadline: DateTime
orderStatus: String
taskCount: int
confirmedCount: int       // 已 CLIENT_CONFIRMED 的物料数
createBy: String          // 客户经理姓名
confirmBy: String         // 负责人姓名（可能 null）
```

```java
// OrderDetailVO（聚合视图，查一张表就够所有 Tab 用）
order: MaterialOrder
customer: Customer
tasks: List<MaterialTaskVO>        // 带每个任务的状态、版本数
changes: List<OrderChangeRecord>   // 变更记录
operationHistory: List<OperationHistory>
```

### 3.3 订单审核（负责人）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/orders/{id}/confirm` | BIZ_LEADER | `{reviewNotes: String}` | `OrderDetailVO` |
| POST | `/api/orders/{id}/reject` | BIZ_LEADER | `{rejectReason: String}` | 200 |

**业务规则**：
- 只有 `orderStatus = DRAFT` 时才能 confirm
- confirm 时自动：`orderStatus → CONFIRMED`，记录 `confirmBy / confirmTime`
- reject 时：订单回到 `DRAFT`（客户经理修改后可重新提交；或者 CANCELLED，由产品决定）

### 3.4 订单变更（客户经理）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/orders/{id}/changes` | CS_MANAGER | `CreateChangeDTO` | `OrderChangeRecord` |

```java
// CreateChangeDTO
changeType: "TASK_ADD" | "TASK_REMOVE" | "TASK_MODIFY" | "TASK_DEADLINE_CHANGE" | "ORDER_DEADLINE_CHANGE" | "OTHER"
changeTarget: String          // 如 "task#1 海报数量" / "订单整体 deadline"
changeAfter: String           // 变更后的值（JSON 或文本）
changeReason: String          // 必填

// 如果 changeType = TASK_ADD，还需携带嵌套的 CreateTaskDTO
// 如果 changeType = TASK_MODIFY / TASK_DEADLINE_CHANGE，还需携带 taskId
```

**业务规则**：
- 订单必须是 `CONFIRMED / IN_PROGRESS / PENDING_DELIVERY` 状态
- `DRAFT` 状态的订单不允许走变更流程（直接在订单详情里改物料清单即可）
- 应用层先 INSERT `order_change_record`，再 UPDATE 主表字段，最后 INSERT `operation_history` —— 三者在同一事务里

### 3.5 订单完成 / 取消

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/orders/{id}/complete` | CS_MANAGER | - | `OrderDetailVO` |
| POST | `/api/orders/{id}/cancel` | CS_MANAGER 或 BIZ_LEADER | `{cancelReason: String}` | 200 |

**完成的前置检查**：
```java
SELECT COUNT(*) FROM material_task
WHERE order_id = ? AND task_status NOT IN ('CLIENT_CONFIRMED', 'CANCELLED')
// 结果必须为 0 才能 complete
// 否则返回 409 Conflict + 未完成任务的 taskNo 列表
```

---

## 四、物料任务管理

### 4.1 任务查询

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| GET  | `/api/tasks` | 所有人（数据权限） | Query: 见下 | 分页 `TaskListItemVO` |
| GET  | `/api/tasks/{id}` | 所有人 | - | `TaskDetailVO` |

**数据权限**：
- CS_MANAGER：能看自己创建的订单下的所有任务
- BIZ_LEADER：全部任务
- DESIGNER：**只能看 `assigneeId = 自己` 的任务**（强约束）

**Query 参数**：
```
orderId: Long
materialType: 枚举
taskStatus: 枚举
assigneeId: Long（BIZ_LEADER 可填；DESIGNER 固定为自己）
taskDeadlineFrom / taskDeadlineTo: 日期范围
overdue: true/false  —— 只显示已过截止日期但状态非 CLIENT_CONFIRMED / CANCELLED 的
page / size
```

```java
// TaskListItemVO
taskId: Long
orderNo: String
materialType: String        // "海报" 中文显示值
sizeSpec: String
quantity: int
taskDeadline: DateTime
taskStatus: String          // 中文显示值
assigneeName: String
versionCount: int
overdue: boolean            // 应用层计算
```

```java
// TaskDetailVO
task: MaterialTask
order: MaterialOrder
assigneeName: String
versions: List<DesignVersion>        // 所有历史版本，按 versionNo 升序
feedbacks: List<FeedbackVO>          // 客户修改意见，每条带 targetVersionNo
```

### 4.2 任务分派（负责人）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/tasks/{id}/assign` | BIZ_LEADER | `{assigneeId: Long}` | `MaterialTask` |
| POST | `/api/tasks/batch-assign` | BIZ_LEADER | `List<{taskId, assigneeId}>` | 200 |

**业务规则**：
- 只能分派 `taskStatus = PENDING_ASSIGN` 的任务
- assigneeId 必须是 `role = DESIGNER` 的用户
- 分派后 `taskStatus → ASSIGNED`
- 同一事务 INSERT operation_history

### 4.3 设计稿上传（设计人员）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/tasks/{id}/versions` | DESIGNER 且 assigneeId = 自己 | multipart/form-data: `file` + `versionNotes` | `DesignVersion` |

**业务规则**：
- 前置检查：`task.assigneeId = 当前用户`（否则 403）
- 前置检查：任务必须已分派（非 `PENDING_ASSIGN`）
- 自动计算 `versionNo = 当前任务最大 versionNo + 1`
- 同时：
  - UPDATE `material_task.version_count = version_count + 1`
  - UPDATE `material_task.last_submit_time = NOW()`
  - UPDATE `material_task.task_status → PENDING_CLIENT_REVIEW`
- 同一事务 INSERT operation_history(UPLOAD)
- **绝不覆盖旧版本**——纯 append

### 4.4 客户修改意见（客户经理）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/tasks/{id}/feedback` | CS_MANAGER | `CreateFeedbackDTO` | `ModificationFeedback` |

```java
// CreateFeedbackDTO
targetVersionNo: int         // 必填，针对哪个版本
feedbackContent: String      // 必填
feedbackSource: String       // "微信" / "电话" / "邮件" / "现场"
```

**业务规则**：
- 前置检查：该版本号必须存在
- 自动设置 `recordBy = 当前用户`
- 同时 UPDATE `material_task.task_status → NEEDS_REVISION`
- UPDATE `material_task.last_feedback_time = NOW()`

### 4.5 客户确认（客户经理）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/tasks/{id}/client-confirm` | CS_MANAGER | `{confirmedVersionNo: int}` | `MaterialTask` |

**业务规则**：
- 前置检查：任务当前状态不能是 `CLIENT_CONFIRMED`（已确认就不能重复确认）
- 自动设置 `taskStatus → CLIENT_CONFIRMED`
- 异步/同步检查：同一订单下是否所有物料都 `CLIENT_CONFIRMED / CANCELLED`
  - 若是 → 自动 UPDATE 订单状态 `→ PENDING_DELIVERY`
- INSERT operation_history(CLIENT_CONFIRM)

### 4.6 任务取消

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| POST | `/api/tasks/{id}/cancel` | BIZ_LEADER 或 CS_MANAGER | `{reason: String}` | 200 |

**业务规则**：
- `taskStatus → CANCELLED`
- 如果同一订单所有剩余任务都 CANCELLED / CLIENT_CONFIRMED，同样触发订单状态检查

---

## 五、客户修改意见管理

（已在 4.4 列出创建接口；设计人员通过任务详情看到意见，无需独立查询接口）

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| GET  | `/api/tasks/{taskId}/feedbacks` | 有该任务查看权限的人 | - | `List<FeedbackVO>` |
| PATCH | `/api/feedbacks/{id}/mark-processed` | DESIGNER 且 assignee = 自己 | - | 200 |

**mark-processed** 的自动触发：当设计人员上传新版本时，上一轮所有 `processed = 0` 的 feedback 自动置 1，所以这个 PATCH 是手动兜底的。

---

## 六、变更记录与操作历史

只读，用于审计和追溯。

| 方法 | 路径 | 权限 | 输入 | 输出 |
| --- | --- | --- | --- | --- |
| GET  | `/api/orders/{id}/changes` | 所有人 | - | `List<OrderChangeRecord>` |
| GET  | `/api/orders/{id}/operations` | 所有人 | Query: `operationType`, `from`, `to`, `page`, `size` | 分页 `OperationHistory` |
| GET  | `/api/tasks/{id}/operations` | 所有人 | 同上 | 分页 `OperationHistory` |
| GET  | `/api/users/me/operations` | 所有人 | 同上 | 分页 `OperationHistory` —— 我执行过的 |

---

## 七、核心业务规则在接口层的汇总

| 规则 | 在哪个接口拦截 | 拒绝条件 |
| --- | --- | --- |
| DRAFT 订单不能改 deadline 走变更流程，直接编辑 | `/orders/{id}/changes` | `orderStatus == DRAFT` |
| 非订单创建人（CS_MANAGER）不能 cancel 自己没创建的订单 | `/orders/{id}/cancel` | `currentUserId != order.createBy` 且 role != BIZ_LEADER |
| 设计人员只能上传分派给自己任务的版本 | `/tasks/{id}/versions` | `task.assigneeId != currentUserId` |
| 非 CS_MANAGER 不能录入客户意见 | `/tasks/{id}/feedback` | `currentUser.role != CS_MANAGER` |
| 所有物料未 CLIENT_CONFIRMED 时不能 complete 订单 | `/orders/{id}/complete` | 还有未 CLIENT_CONFIRMED 且未 CANCELLED 的任务 |
| ASSIGNED 之前不能上传设计稿 | `/tasks/{id}/versions` | `task.assigneeId == null` |
| 不能对已 CLIENT_CONFIRMED 的任务再录入意见 | `/tasks/{id}/feedback` | `task.taskStatus == CLIENT_CONFIRMED` |

---

## 八、错误码约定

| code | 场景 | 说明 |
| --- | --- | --- |
| 200 | 成功 | - |
| 400 | 参数校验失败 | errorKey 指向具体字段 |
| 401 | 未登录 / token 过期 | - |
| 403 | 无权限（角色对 / 数据归属不对） | 操作人和业务对象不匹配 |
| 404 | 资源不存在 | id 查不到 |
| 409 | 业务冲突 | 如"订单还有未完成的任务，不能 complete" |
| 422 | 业务规则不满足 | 如"taskDeadline 晚于 overallDeadline" |
| 500 | 系统内部错误 | - |
