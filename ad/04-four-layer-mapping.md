# 四层映射矩阵：页面 → 接口 → Java 对象 → 数据库

> 这是第三章最核心的产物。用一张大表把每一个页面元素、它背后调哪个接口、接口内部创建/更新哪些 Java 对象、最终落到哪几张数据库表 —— 一一对齐。

---

## 客户经理页面映射

### 页面：工作台首页

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 今日概览卡片（在做订单数、本周到期物料数等） | `GET /api/workbench/cs/stats` | `WorkbenchService.getCSStats()` → `CSWorkbenchStatsVO` | `material_order`, `material_task` | 只读聚合查询 |
| 待办提醒区 | 同上（一个接口返回全部概览） | 同上 | 同上 | 只读聚合查询 |
| 我创建的订单列表 | `GET /api/orders?createBy=我` | `OrderService.pageQuery(userId)` → `List<OrderListItemVO>` | `material_order`, `customer` JOIN | 只读 JOIN 查询 |
| [+ 新建订单] 按钮 | 路由到新建订单页 | - | - | - |

### 页面：新建订单

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 客户下拉框 | `GET /api/customers?pageSize=100` | `CustomerService.pageQuery()` | `customer` | 只读 |
| 整体截止时间 | 表单字段 | - | - | - |
| 物料清单（动态行：增/删物料） | 前端表单校验（不调接口） | - | - | - |
| [提交创建] | `POST /api/orders` | `OrderService.create(CreateOrderDTO)` → `MaterialOrder` + 多个 `MaterialTask` | `material_order` (INSERT) + `material_task` (BATCH INSERT) + `operation_history` (INSERT) | 事务写入 |

### 页面：订单详情（客户经理视角）

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 主 Tab：订单基础信息 | `GET /api/orders/{id}` | `OrderService.getDetail()` → `OrderDetailVO`（一次返回全部 Tab） | `material_order`, `customer`, `material_task`, `order_change_record`, `operation_history` | 只读多表 JOIN |
| 物料任务列表 Tab | 同上（已包含在 `orderDetailVO.tasks`） | `List<TaskDetailLiteVO>` | `material_task` | 只读 |
| 变更记录 Tab | 同上（已包含在 `orderDetailVO.changes`） | `List<OrderChangeRecord>` | `order_change_record` | 只读 |
| 操作历史 Tab | 同上（已包含在 `orderDetailVO.operations`） | `List<OperationHistory>` | `operation_history` | 只读 |
| 任务卡片上的 [记录客户意见] | `POST /api/tasks/{taskId}/feedback` | `TaskService.submitFeedback(CreateFeedbackDTO)` → `ModificationFeedback` | `modification_feedback` (INSERT) + `material_task` (UPDATE status→NEEDS_REVISION) + `operation_history` (INSERT) | 事务写入 |
| 任务卡片上的 [标记客户确认] | `POST /api/tasks/{taskId}/client-confirm` | `TaskService.clientConfirm(taskId, userId)` | `material_task` (UPDATE status) + `material_order` (可能 UPDATE status→PENDING_DELIVERY) + `operation_history` (INSERT) | 事务写入 |
| [发起变更] 按钮 | `POST /api/orders/{id}/changes` | `OrderService.addChange(CreateChangeDTO)` | `order_change_record` (INSERT) + 主表字段 UPDATE + `operation_history` (INSERT) | 事务写入 |
| [完成交付] 按钮 | `POST /api/orders/{id}/complete` | `OrderService.completeOrder(orderId, userId)` | `material_order` (UPDATE) + `operation_history` (INSERT) | 事务写入 |

### 页面：客户管理

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 客户列表 | `GET /api/customers` | `CustomerService.pageQuery()` → `List<CustomerVO>` | `customer` | 只读 |
| [+ 新建客户] | `POST /api/customers` | `CustomerService.create(CreateCustomerDTO)` | `customer` (INSERT) | 写入 |
| [编辑] | `PUT /api/customers/{id}` | `CustomerService.update(id, UpdateCustomerDTO)` | `customer` (UPDATE) | 写入 |

---

## 负责人页面映射

### 页面：工作台首页

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 待处理卡片 | `GET /api/workbench/leader/stats` | `WorkbenchService.getLeaderStats()` → `LeaderWorkbenchStatsVO` | `material_order`, `material_task` | 只读聚合 |
| 延期/风险卡片 | 同上 | 同上 | 同上 | 只读聚合 |
| 全部订单列表 | `GET /api/orders`（无 createBy 过滤，负责人看全部） | `OrderService.pageQuery(null, leaderRole)` | `material_order`, `customer` | 只读 |

### 页面：审核中心

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 待审核订单列表 | `GET /api/orders?status=DRAFT` | 同上 | `material_order` | 只读 |
| 审核详情（弹窗/页面） | `GET /api/orders/{id}` | `OrderService.getDetail()` | 多表 JOIN | 只读 |
| [✓ 审核通过] | `POST /api/orders/{id}/confirm` | `OrderService.confirm(orderId, leaderId)` | `material_order` (UPDATE: status→CONFIRMED, confirmBy, confirmTime) + `operation_history` (INSERT) | 事务写入 |
| [✗ 打回修改] | `POST /api/orders/{id}/reject` | `OrderService.reject(orderId, leaderId, reason)` | `material_order` (UPDATE: 回到 DRAFT 或 CANCELLED) + `operation_history` (INSERT) | 事务写入 |

### 页面：任务分派

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 待分派任务列表 | `GET /api/tasks?status=PENDING_ASSIGN` | `TaskService.pageQuery()` | `material_task`, `material_order` (JOIN 显示 orderNo) | 只读 |
| 设计人员下拉框 | `GET /api/users?role=DESIGNER` | `SysUserService.getByRole()` | `sys_user` | 只读 |
| [批量确认分派] | `POST /api/tasks/batch-assign` | `TaskService.batchAssign(BatchAssignTaskDTO)` | `material_task` (BATCH UPDATE: assignee_id, status→ASSIGNED) + `operation_history` (BATCH INSERT) | 事务批量写入 |

### 页面：延期监控

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 延期/风险任务列表 | `GET /api/tasks?overdue=true` | `TaskService.pageQuery(overdue=true)` | `material_task` WHERE task_deadline < NOW() AND task_status NOT IN (CLIENT_CONFIRMED, CANCELLED) | 只读聚合 |
| 单个任务分派（紧急） | `POST /api/tasks/{id}/assign` | `TaskService.assign(taskId, assigneeId, leaderId)` | `material_task` (UPDATE) + `operation_history` (INSERT) | 事务写入 |

---

## 设计人员页面映射

### 页面：工作台首页

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 我的任务概览 | `GET /api/workbench/designer/stats` | `WorkbenchService.getDesignerStats(userId)` → `DesignerWorkbenchStatsVO` | `material_task` WHERE assignee_id = ? | 只读聚合 |
| 截止提醒 | 同上（已包含 urgentTasks 列表） | 同上 | 同上 | 只读 |
| 分派给我的全部任务 | `GET /api/tasks?assigneeId=我` | `TaskService.pageQuery(assigneeId=me)` | `material_task` WHERE assignee_id = ? | 只读 |

### 页面：任务详情 + 上传区

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 任务详情 | `GET /api/tasks/{id}` | `TaskService.getDetail(taskId, userId)` → `TaskDetailVO` | `material_task`, `design_version`, `modification_feedback`, `material_order` JOIN | 只读多表 JOIN |
| 版本历史 Tab（所有历史版本） | 同上（已包含在 `taskDetailVO.versions`） | `List<DesignVersion>` | `design_version` | 只读 |
| 客户意见区 | 同上（已包含在 `taskDetailVO.feedbacks`） | `List<FeedbackVO>` | `modification_feedback` | 只读 |
| [上传新版本] | `POST /api/tasks/{id}/versions` (multipart) | `TaskService.uploadVersion(taskId, userId, file, notes)` | `design_version` (INSERT) + `material_task` (UPDATE: version_count++, last_submit_time, status) + `modification_feedback` (UPDATE processed=1 针对上一轮) + `operation_history` (INSERT) + 文件存储服务 | 事务写入（文件存储在事务外补偿） |

### 页面：我的上传历史

| 页面元素 | 调用接口 | Java 对象 | 数据库表 | 操作类型 |
| --- | --- | --- | --- | --- |
| 我上传过的所有版本 | `GET /api/versions?uploadBy=我` | `DesignVersionService.pageQuery(uploaderId)` | `design_version` | 只读 |

---

## 后台定时任务映射

| 定时任务 | 频率 | Java 对象 | 数据库表 | 目的 |
| --- | --- | --- | --- | --- |
| 检查即将到期的任务 | 每天 09:00 | `ScheduledTaskService.checkUpcomingDeadlines()` | `material_task` WHERE task_deadline BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 3 DAY) AND status NOT IN (CLIENT_CONFIRMED, CANCELLED) | 写入 Redis 或通知表，工作台"截止提醒"卡片前端轮询 |
| 检查已逾期但未完成的任务 | 每天 09:30 + 18:00 | `ScheduledTaskService.checkOverdueTasks()` | `material_task` WHERE task_deadline < NOW() AND status NOT IN (CLIENT_CONFIRMED, CANCELLED) | 工作台"延期监控"卡片 + 给负责人/设计人员发通知 |
| 清理 operation_history（demo 可选） | 每月 1 号 | `ScheduledTaskService.archiveHistory()` | `operation_history` | demo 阶段不需要真删 |

---

## 数据库表读写频率预估（接近真实业务量）

假设一个广告公司 5 个客户经理、3 个负责人、10 个设计人员，**每月产生约 80 笔订单**，平均每单 3 个物料：

| 表 | 初始演示数据 | 月增量 | 月累计 | 读写频率 |
| --- | ---: | ---: | ---: | --- |
| `sys_user` | 4 | 0（稳定） | 4 | 极低（登录时读一次） |
| `customer` | 2 | ~15 | 17 | 低 |
| `material_order` | 1 | ~80 | 81 | 中（列表页 + 详情页高频） |
| `material_task` | 3 | ~240 | 243 | 高（三个工作台都查） |
| `design_version` | 3 | ~480（每任务平均 2 版） | 483 | 高 |
| `modification_feedback` | 1 | ~240 | 241 | 中 |
| `order_change_record` | 1 | ~40（约一半订单有变更） | 41 | 低（变更记录只读审计） |
| `operation_history` | 12 | ~1500（每笔订单平均 18 次操作） | 1512 | 中（审计 + 历史查询） |

**索引命中验证**：
- `material_task` 最常按 `assignee_id` + `task_status` 查 → 已有 `idx_assignee_id` 和 `idx_task_status`，WHERE 子句通常能命中至少一个
- `material_order` 按 `order_status` 筛选 → `idx_order_status`
- `design_version` 按 `task_id` + `version_no` 唯一约束 → `uk_task_version`
- 全表扫描风险最低的表：`sys_user`（数据量极小）、`order_change_record`（月增量只有 40）
