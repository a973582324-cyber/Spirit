# 02 · 数据库设计

> 第二章交付 · MySQL 8.0 · utf8mb4
> 完整 DDL 落盘于 `backend/src/main/resources/schema.sql`

---

## 一、表结构总览（8 张核心表）

| # | 表名 | 行数规模（demo） | 关联 | 用途 |
| --- | --- | ---: | --- | --- |
| 1 | `sys_user` | 4 | — | 三类岗位账号 |
| 2 | `customer` | 2 | — | 客户主数据 |
| 3 | `material_order` | 4 | → customer, → sys_user(createBy/confirmBy) | 订单主表 |
| 4 | `material_task` | 9 | → material_order, → sys_user(assignee) | 物料任务 |
| 5 | `design_version` | 3 | → material_task | 设计稿版本（append-only） |
| 6 | `modification_feedback` | 2 | → material_task | 客户修改意见 |
| 7 | `order_change_record` | 6 | → material_order | 订单变更强制留痕 |
| 8 | `operation_history` | 11+ | → 通用 business_id | 全流程操作审计 |

### ER 图

```
 sys_user (id)
 ├── customer.create_by ──────► 谁录入的
 ├── material_order.create_by ─► 谁创建的（客户经理）
 ├── material_order.confirm_by─► 谁审核的（业务负责人）
 ├── material_order.cancel_by─► 谁取消的
 ├── material_task.assignee_id─► 谁来做（设计制作人员）
 ├── design_version.upload_by ─► 谁传的
 ├── modification_feedback.record_by──► 谁录的意见
 ├── order_change_record.change_by ──► 谁发起的变更
 └── operation_history.operator_id  ─► 谁操作的

 customer (id)
 └── material_order.customer_id ──► 订单属于哪个客户

 material_order (id)
 ├── material_task.order_id (1:N, CASCADE)
 ├── order_change_record.order_id (1:N, CASCADE)
 └── material_task → design_version / modification_feedback (间接 1:N)

 material_task (id)
 ├── design_version.task_id (1:N, CASCADE)
 └── modification_feedback.task_id (1:N, CASCADE)
```

---

## 二、各表字段详解

### 1. sys_user 用户表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK, AUTO_INCREMENT | |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登录账号 |
| password | VARCHAR(100) | NOT NULL | demo 明文；生产应 BCrypt |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | CS_MANAGER / BIZ_LEADER / DESIGNER |
| phone | VARCHAR(20) | | 联系电话 |
| email | VARCHAR(100) | | |
| enabled | TINYINT(1) | DEFAULT 1 | 1 启用 0 停用 |
| create_time / update_time | DATETIME | NOT NULL | 自动填充 |

**索引**：`uk_username(username)`, `idx_role(role)`

**Demo 数据**：

| username | real_name | role |
| --- | --- | --- |
| chenliang | 陈亮 | CS_MANAGER |
| wangli | 王丽 | BIZ_LEADER |
| zhangxiaoyi | 张小艺 | DESIGNER |
| liusiyuan | 刘思源 | DESIGNER |

---

### 2. customer 客户表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| customer_name | VARCHAR(100) NOT NULL | 如"甜野奶茶望京店" |
| contact_person | VARCHAR(50) NOT NULL | 对接人 |
| contact_phone | VARCHAR(20) NOT NULL | |
| contact_email | VARCHAR(100) | |
| address | VARCHAR(200) | |
| notes | VARCHAR(500) | 客户偏好、历史合作 |
| create_by | BIGINT | 客户经理 sys_user.id |

**索引**：`idx_customer_name(customer_name)`, `idx_create_by(create_by)`

---

### 3. material_order 订单主表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| order_no | VARCHAR(50) NOT NULL, UNIQUE | 业务生成 MO{YYYYMMDD}{seq} |
| customer_id | BIGINT FK → customer.id | |
| customer_contact | VARCHAR(100) | 下单时快照（客户改资料后订单不变） |
| overall_deadline | DATETIME NOT NULL | 整单交付截止 |
| order_status | VARCHAR(30) NOT NULL | 见状态机 |
| notes | VARCHAR(1000) | 整体备注 |
| create_by | BIGINT NOT NULL FK → sys_user.id | 客户经理 |
| confirm_by | BIGINT FK → sys_user.id | 业务负责人 |
| confirm_time | DATETIME | |
| complete_time | DATETIME | 客户经理点"完成交付"的时间 |
| cancel_by | BIGINT FK → sys_user.id | |
| cancel_time | DATETIME | |
| cancel_reason | VARCHAR(500) | |

**索引**：`uk_order_no(order_no)`, `idx_status(order_status)`, `idx_create_by(create_by)`

**⚠️ 设计决策**：`customer_contact` 做快照冗余——客户表改联系人后，订单里的联系人保持下单时的值，历史可追溯。

---

### 4. material_task 物料任务

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| order_id | BIGINT FK → material_order.id (CASCADE) | |
| task_no | INT NOT NULL | 订单内序号 1, 2, 3… |
| material_type | VARCHAR(30) NOT NULL | POSTER / STANDEE / MENU / BROCHURE / ONLINE_IMAGE |
| size_spec | VARCHAR(100) | 如"50×70cm"、"A5 双面" |
| quantity | INT NOT NULL DEFAULT 1 | 数量 |
| `usage` | VARCHAR(200) | 用途说明（**保留字**，反引号包裹） |
| reference | VARCHAR(500) | 参考链接或文字 |
| task_deadline | DATETIME NOT NULL | 单项物料 deadline（≤ overall_deadline） |
| task_status | VARCHAR(30) NOT NULL | 见任务状态机 |
| assignee_id | BIGINT FK → sys_user.id | 分派给哪位设计人员（可为 NULL） |
| version_count | INT DEFAULT 0 | 上传过几个版本 |
| client_confirmed_version | INT | 客户点头的版本号 |

**索引**：`idx_order_id(order_id)`, `idx_task_status(task_status)`, `idx_assignee_id(assignee_id)`

**⚠️ 保留字坑**：MySQL 8.0 中 `usage` 是保留字，DDL 用反引号包裹，Java Entity 中加 `@TableField("\`usage\`")`。

---

### 5. design_version 设计稿版本

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| task_id | BIGINT FK → material_task.id (CASCADE) | |
| version_no | INT NOT NULL | 1, 2, 3…（**append-only，永不覆盖**） |
| file_name | VARCHAR(200) | 原始文件名 |
| file_url | VARCHAR(500) | 服务器存储路径 |
| file_size | BIGINT | 文件大小 bytes |
| version_notes | VARCHAR(500) | 设计人员写的改动说明 |
| upload_by | BIGINT FK → sys_user.id | 设计人员 |
| upload_time | DATETIME | |

**唯一约束**：`uk_task_version(task_id, version_no)` —— 同一个任务不会有相同版本号。

**⚠️ append-only 设计**：新版本只 INSERT 新行，永不 UPDATE 旧行。版本号 = `MAX(version_no) + 1`。

---

### 6. modification_feedback 客户修改意见

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| task_id | BIGINT FK → material_task.id (CASCADE) | |
| target_version_no | INT | 这条意见针对的是哪个版本 |
| feedback_content | TEXT NOT NULL | 客户意见原文 |
| feedback_source | VARCHAR(30) | 来源：微信/电话/邮件 |
| record_by | BIGINT FK → sys_user.id | 客户经理 |
| record_time | DATETIME | |
| processed | TINYINT(1) DEFAULT 0 | 设计人员提交新版本后自动置 1 |

---

### 7. order_change_record 订单变更记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| order_id | BIGINT FK → material_order.id (CASCADE) | |
| change_type | VARCHAR(30) NOT NULL | 见 ChangeType 枚举 |
| change_target | VARCHAR(100) | 变更对象描述 |
| change_before | TEXT | 变更前快照 |
| change_after | TEXT | 变更后值 |
| change_reason | VARCHAR(500) NOT NULL | 为什么改（必填） |
| change_by | BIGINT FK → sys_user.id | |
| change_time | DATETIME | |

**6 种变更类型**：

| change_type | 自动处理 |
| --- | --- |
| ORDER_DEADLINE_CHANGE | **Service 层自动**改 `material_order.overall_deadline` |
| TASK_ADD | **Service 层自动** INSERT 新 `material_task`（taskNo = max+1, status=PENDING_ASSIGN） |
| TASK_MODIFY | 只存快照（主表同步可扩展） |
| TASK_DEADLINE_CHANGE | 只存快照 |
| TASK_REMOVE | 只存快照 |
| OTHER | 只存快照 |

**⚠️ 强制双写**：Service 层所有订单变更在一个事务内同时做：
1. INSERT `order_change_record`
2. 根据 change_type 执行主表 UPDATE / INSERT
3. INSERT `operation_history`

**状态机门槛**：
- DRAFT 订单走变更 → 422 "草稿直接编辑"
- COMPLETED/CANCELLED → 409 "终态不允许变更"
- 非创建人 → 403

---

### 8. operation_history 操作历史

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | |
| business_type | VARCHAR(30) NOT NULL | ORDER / TASK / CUSTOMER 等 |
| business_id | BIGINT NOT NULL | 业务对象 id |
| operation_type | VARCHAR(30) NOT NULL | CREATE / CONFIRM / ASSIGN / UPLOAD / FEEDBACK / CLIENT_CONFIRM / CHANGE / CANCEL / COMPLETE 等 |
| operator_id | BIGINT FK → sys_user.id | |
| description | VARCHAR(500) | 处理说明 |
| operate_time | DATETIME | |

**用途**：
- 客户经理/负责人查"这张订单到底发生了什么"
- 审计追溯
- 列表页"最新操作"展示

---

## 三、状态机设计

### 订单状态机（6 种）

```
    创建                负责人审核           物料分派后首版上传      全部物料 CLIENT_CONFIRMED
DRAFT ───────► CONFIRMED ───────► IN_PROGRESS ──────► PENDING_DELIVERY ──────► COMPLETED
                                                                             │
                                 任意非终态可取消 ──────────────────────────► CANCELLED
```

**状态流转表**：

| 从 | 允许跳到 | 触发接口 | 谁能操作 |
| --- | --- | --- | --- |
| DRAFT | CONFIRMED | `POST /orders/{id}/confirm` | BIZ_LEADER |
| DRAFT | CANCELLED | `POST /orders/{id}/cancel` | CS_MANAGER |
| CONFIRMED | IN_PROGRESS | 自动（首个物料分派） | — |
| CONFIRMED | CANCELLED | cancel | CS_MANAGER / BIZ_LEADER |
| IN_PROGRESS | PENDING_DELIVERY | 自动（全部物料 CLIENT_CONFIRMED） | — |
| IN_PROGRESS | CANCELLED | cancel | CS_MANAGER / BIZ_LEADER |
| PENDING_DELIVERY | COMPLETED | `POST /orders/{id}/complete` | CS_MANAGER |
| PENDING_DELIVERY | CANCELLED | cancel | CS_MANAGER / BIZ_LEADER |

**拒绝的跳转**：

| 场景 | 返回 |
| --- | --- |
| COMPLETED / CANCELLED 终态再取消 | 409 Conflict |
| PENDING_DELIVERY 之外的状态点 complete | 409 + 提示未确认物料数 |
| DRAFT 走变更流程 | 422 |
| 终态走变更 | 409 |

---

### 任务状态机（7 种）

```
PENDING_ASSIGN ──负责人分派──► ASSIGNED ──设计人员开始──► IN_PROGRESS
                                                              │
                                         上传初稿 ──► PENDING_CLIENT_REVIEW
                                                              │
                                         客户经理录客户意见 ──► NEEDS_REVISION
                                                              │
                                         设计人员提交新版本 ──► PENDING_CLIENT_REVIEW
                                         （上一轮 feedback.processed 自动置 true）
                                                              │
                                         客户经理记录客户点头 ──► CLIENT_CONFIRMED
```

---

## 四、业务规则 → 数据库层可保障

| 业务规则 | 数据库层 | 应用层 |
| --- | --- | --- |
| 新版本不覆盖旧版本 | `design_version` append-only | 禁止 UPDATE 旧行；`versionNo = MAX + 1` |
| 所有物料 CLIENT_CONFIRMED 后订单自动 PENDING_DELIVERY | — | Service 层 `checkAndPromoteOrderStatus()` |
| 订单变更强制双写 | `order_change_record` 独立表 | 拦截 UPDATE 非 DRAFT 订单时强制生成 |
| 设计人员只能处理自己的任务 | `material_task.assignee_id` FK | 拦截 `currentUserId != assigneeId` → 403 |
| 只有客户经理能记录客户确认 | 角色字段 | Service 层 `role != CS_MANAGER` → 403 |
| 订单取消级联所有未完成物料 | — | Service 层事务内批量 UPDATE |
| 客户经理只能看自己的订单 | — | Service 层 `createBy = currentUserId` 过滤 |
| 设计人员只能看自己的任务 | — | Service 层 `assigneeId = currentUserId` 过滤 |
| 客户信息快照 | `material_order.customer_contact` | 创建订单时 INSERT 快照 |

---

## 五、Demo 数据

`schema.sql` 末尾预置：

| 数据 | 数量 | 说明 |
| --- | ---: | --- |
| sys_user | 4 | 2 CS_MANAGER（1+1 demo 复用）+ 1 BIZ_LEADER + 2 DESIGNER |
| customer | 2 | 甜野奶茶望京店 + 川味轩餐饮连锁 |
| material_order | 4 | 1 COMPLETED（完整链路）+ 1 CANCELLED（变更后取消）+ 1 CANCELLED（浏览器测试）+ 1 CONFIRMED（刚建） |
| material_task | 9 | 对应 4 张订单 |
| design_version | 3 | 完整的 v1→v2 流程 |
| modification_feedback | 2 | 1 条已处理 + 1 条未处理 |
| order_change_record | 6 | deadline 变更 + TASK_ADD |
| operation_history | 11+ | 全流程操作 |
