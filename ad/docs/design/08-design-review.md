# 设计评审与修改记录

> 这是文件明确要求的"设计评审与修改记录"。不是形式上的 checklist，而是把设计过程中推翻重来、反复琢磨的点真实记录下来，说明"为什么这样设计"而不只是"设计成什么样"。

---

## 评审时间线

| # | 时间 | 主题 | 评审人 | 结论 |
| --- | --- | --- | --- | --- |
| R1 | 2026-11-04 下午 | 初稿评审 | 自己（做设计时的第一性思考） | 5 个需要修改的点 |
| R2 | 2026-11-04 晚上 | 对照 project-canonical.md 逐条验收 | 自己 | 补了 2 张表 + 修正了 3 个规则 |
| R3 | 2026-11-05 凌晨 | 状态机和自动联动验证 | 自己 | 修正了 CLIENT_CONFIRMED 后订单状态自动联动的边界条件 |

---

## 修改记录详情

### 修改点 1：物料任务是否应该独立有自己的状态机？

**初稿**：只给订单设计了状态机，物料任务只是订单的"附属明细"，没有独立状态。

**问题**：文件核心规则第 7 条——"**订单内所有物料都确认后，整笔订单才能完成**"——如果物料没有独立的 `CLIENT_CONFIRMED` 状态，这条规则根本没地方表达。订单和物料的状态必须**两层独立**：一个订单是 `IN_PROGRESS` 时，它的某个物料可能已经 `CLIENT_CONFIRMED`，另一个可能还在 `NEEDS_REVISION`。

**修改**：给 `material_task` 独立设计了 7 个状态（PENDING_ASSIGN → ASSIGNED → IN_PROGRESS → PENDING_CLIENT_REVIEW → NEEDS_REVISION → CLIENT_CONFIRMED / CANCELLED），与订单状态机解耦但通过 Service 层的"订单完成检查"形成联动。

---

### 修改点 2：设计稿版本记录是独立表还是嵌入任务表？

**初稿**：在 `material_task` 表上加了 `current_version_url`、`current_version_no` 两个字段，覆盖式更新——每次上传新版本直接替换。

**问题**：文件核心规则第 5 条——"**新版本不能覆盖旧版本，必须能够查看修改过程**"——覆盖式更新直接把这条规则干没了。而且客户经理看客户意见时需要追溯"客户是对哪个版本提的意见"，没有历史版本链就没法关联。

**修改**：拆出独立的 `design_version` 表，append-only，永不 UPDATE 已有行。`material_task` 只留 `version_count` 和 `last_submit_time` 作为冗余快照字段（方便列表展示，不承担事实来源）。同时 `modification_feedback.target_version_no` 外键关联到具体版本号，形成完整追溯链：**客户意见 → 哪个版本 → 那个版本的设计人员说了什么**。

---

### 修改点 3：变更记录怎么触发？

**初稿**：`order_change_record` 是一张可选的"备注表"，客户经理觉得重要时手动写一笔。

**问题**：文件核心规则第 2 条——"**新增物料或改变截止时间时必须留下变更记录**"——"必须"意味着不能靠自觉。而且如果 `DRAFT` 状态直接改物料清单不触发变更、只有确认后的才触发，这条规则的边界在哪？

**修改**：把 `order_change_record` 从"可选备注表"升级为"**强制双写**"机制。在 `OrderService` 和 `TaskService` 的所有 UPDATE 方法里前置拦截：

```java
// 伪代码：任何会改变物料清单或 deadline 的方法
public void updateTaskDeadline(Long taskId, LocalDateTime newDeadline, Long operatorId) {
    MaterialTask task = mapper.selectById(taskId);
    // 前置：DRAFT 状态的订单不强制写变更记录（直接编辑）
    MaterialOrder order = orderMapper.selectById(task.getOrderId());
    if (order.getOrderStatus() != OrderStatus.DRAFT) {
        // 事务内先 INSERT 变更记录
        OrderChangeRecord change = new OrderChangeRecord();
        change.setChangeType(ChangeType.TASK_DEADLINE_CHANGE);
        change.setChangeBefore(task.getTaskDeadline().toString());
        change.setChangeAfter(newDeadline.toString());
        change.setChangeReason("...");
        changeMapper.insert(change);
        // 再 UPDATE 主表
    }
    task.setTaskDeadline(newDeadline);
    mapper.updateById(task);
}
```

这样不管谁写代码改 deadline，只要走 Service 层就一定会触发变更记录，绕不开。

---

### 修改点 4：客户修改意见关联版本还是关联任务？

**初稿**：`modification_feedback` 只有 `task_id`，不关心针对哪个版本提的意见。

**问题**：设计人员上传了 v1，客户提了意见；设计人员上传了 v2，客户又提了新意见。如果 feedback 只关联 task_id，两条意见都挂在同一个任务上，分不清哪条是对哪个版本的。这就没法让设计人员知道"v2 要处理的是**第 2 条意见**"。

**修改**：增加 `target_version_no` 字段，feedback 必须明确指向一个具体版本。同时在设计人员上传新版本时，自动把上一轮所有 `processed = 0` 的 feedback 置 1，表示"这条意见已经被新版本回应了"。

---

### 修改点 5：客户确认任务后，订单状态自动联动的边界条件

**初稿**：客户经理执行 `client-confirm` 动作时，同步检查"该订单下是否所有物料都 CLIENT_CONFIRMED"，如果是就把订单状态改为 `COMPLETED`。

**问题**：文件核心验收场景 5 说——"**海报已确认而展架未完成时，整笔订单仍显示制作中**"，这没问题；但验收场景 1 说"包含海报和展架的订单，负责人分别分派任务"——如果只有一个物料 `CLIENT_CONFIRMED`、另一个 `CANCELLED`，订单也应该 `PENDING_DELIVERY`。初稿只检查了 `CLIENT_CONFIRMED`，没算上 `CANCELLED`。

**修改**：联动逻辑改为：
```sql
-- 只有当订单下所有物料的状态都在 (CLIENT_CONFIRMED, CANCELLED) 两个集合之一时
-- 才算"该订单的所有物料都处理完了"
SELECT COUNT(*) FROM material_task
WHERE order_id = #{orderId}
  AND task_status NOT IN ('CLIENT_CONFIRMED', 'CANCELLED')
-- 结果为 0 才触发订单 → PENDING_DELIVERY
```

这样"确认 3 项、取消 1 项"也算订单可以交付了。

---

### 修改点 6：文件上传存哪里？（schema.sql 里只写了 file_url）

**初稿**：`design_version.file_url` 存绝对路径字符串，假设后端知道往哪存。

**问题**：课程 demo 阶段用本地存储没问题，但设计里没写清楚"文件上传和数据库写入不在同一个事务"的补偿策略——如果 INSERT `design_version` 成功但写入磁盘失败怎么办？会出现"数据库里有版本记录但文件实际不存在"。

**修改**：设计里明确约定"**文件存储与数据库写入解耦**"：
1. Controller 先调用文件服务存文件（本地磁盘或对象存储），拿到返回的 `fileUrl`
2. 拿到 fileUrl 后再 INSERT `design_version`（这步在事务里）
3. 如果第 1 步失败直接返回 500，不进入第 2 步
4. 如果第 2 步失败（数据库异常），第 1 步存的文件成为孤儿文件，由定时任务定期扫描清理（demo 阶段可以不做，只在 README 里说明）

这样避免了分布式事务问题，也符合"课程 demo 用简单方案"的边界。

---

### 修改点 7：订单编号用什么规则生成？

**初稿**：用自增 ID 前面加前缀 `MO-`，如 `MO-1`、`MO-2`。

**问题**：自增 ID 容易被猜到（MO-9 之后是 MO-10），而且不同环境（开发/测试/生产）自增 ID 从 1 开始会重复。生产上订单号一般带日期，方便人工快速定位。

**修改**：格式改为 `MO` + `yyyyMMdd` + 3 位序号，如 `MO20261104001`。序号按当天最大序号 +1 生成。Service 层做事务内 SELECT MAX + INSERT，保证同一天不重复。

---

### 修改点 8：operation_history 的 business_type + business_id 多态关联

**初稿**：`operation_history` 不加外键，就是纯审计表，查询时按 `(business_type, business_id)` 找对应业务对象。

**评审**：这是刻意设计的——因为订单完成后可能被归档，任务被 CASCADE DELETE 时历史还要保留。加外键 `FOREIGN KEY business_id REFERENCES material_order(id)` 会导致"订单删了历史也没了"，违背"**关键操作留下可追溯记录**"这条文件要求。

**结论**：不加外键，应用层负责 `business_type` 和 `business_id` 的正确性。这是审计表的常见设计模式。

---

## 遗留问题（明确不做但要记下来）

| # | 问题 | 为什么不做 | 后续如果做的话 |
| --- | --- | --- | --- |
| Q1 | operation_history 要不要加业务快照？比如订单 COMPLETE 时存一份当时的 order JSON | demo 阶段不需要存快照，直接查当时的 material_order + material_task 就行 | 如果后续要做"订单完整生命周期回溯"，在 operation_history 加 `snapshot JSON` 字段 |
| Q2 | 客户修改意见要不要支持多条并发？比如一个客户同时对两个版本提意见 | 可以支持，当前设计没限制，但 demo 阶段的业务场景是一版本一轮意见 | 无修改，当前设计已经天然支持 |
| Q3 | 催办/通知机制？比如任务快到 deadline 时自动发消息给设计人员 | 文件明确说"首版不做"通知，demo 阶段只在工作台卡片上显示"剩余 N 天"的提醒 | 如果后续加，用定时任务 + Redis 通知表；前端轮询 |
| Q4 | 负责人的"分派时看到设计人员当前负载"——要不要在数据库里存负载字段？ | 可以实时 `SELECT COUNT(*) FROM material_task WHERE assignee_id = ? AND task_status NOT IN (CLIENT_CONFIRMED, CANCELLED)` 算出来，不需要存 | 高并发时可以考虑加一个 counter 字段做缓存 |
| Q5 | 订单状态 DRAFT 时物料清单可以直接改不强制变更记录——边界清晰吗？ | DRAFT 是"还没给负责人审核"，客户经理在草稿阶段反复改物料清单很正常，此时强制写变更记录是噪音。CONFIRMED 之后任何改动都必须留痕——边界清晰 | 无修改 |

---

## 对照 project-canonical.md 核心验收场景验证

文件列出的 7 个核心验收场景，逐一验证设计是否覆盖：

| 验收场景 | 设计覆盖位置 | 验证 |
| --- | --- | --- |
| 1. 客户经理创建包含海报和展架的订单后，负责人能够分别分派任务 | `POST /api/orders` + `POST /api/tasks/batch-assign` | ✅ 覆盖，见 02-api-contracts.md |
| 2. 设计人员只能处理分派给自己的物料任务 | `TaskService` Service 层拦截 `assigneeId != currentUserId` | ✅ 覆盖，见 05-status-matrix.md 第十节"无权限访问汇总" |
| 3. 初稿上传后进入待客户确认，不能直接标记完成 | `design_version` INSERT 只把 task.status 改为 PENDING_CLIENT_REVIEW；CLIENT_CONFIRMED 动作由 CS_MANAGER 单独触发 | ✅ 覆盖 |
| 4. 客户提出修改意见后，设计人员能够提交新版本，旧版本仍可查看 | `design_version` append-only；新版本 versionNo = MAX + 1 | ✅ 覆盖 |
| 5. 海报已确认而展架未完成时，整笔订单仍显示制作中 | Service 层 CLIENT_CONFIRMED 后检查所有物料是否都在 CLIENT_CONFIRMED/CANCELLED | ✅ 覆盖 |
| 6. 已确认订单新增物料或更改日期时留下变更记录 | 强制双写机制，CONFIRMED 后任何主表变更都 INSERT order_change_record | ✅ 覆盖 |
| 7. 无权限用户修改订单或他人任务时被拒绝 | @PreAuthorize + Service 层数据归属双重校验 | ✅ 覆盖 |

**结论**：7 个验收场景全部在当前设计中有明确落点，没有遗漏。
