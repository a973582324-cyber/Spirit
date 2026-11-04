# 宣传物料订单管理系统 — 项目交付总结报告

> **选题一 · 广告制作公司宣传物料订单管理系统**
> 课程全流程大作业 · 2026-11-04
> 运行地址：`http://localhost:5173` （前端）· `http://localhost:8080/api` （后端）

---

## 一、项目概述

### 1.1 解决什么问题

广告制作公司日常运营围绕"接单 → 拆成具体物料任务 → 设计制作 → 客户确认 → 交付"展开。实际工作中普遍存在：

- 沟通记录散落在微信/QQ/口头传达中，客户说"做得年轻一点"，设计人员不知道尺寸、数量、用途
- 设计稿改了两次，没人说得清客户最终确认的是哪一版，线上传了文件 ≠ 客户点头了
- 客户经理只能逐个人问进度，没法一眼看清整张订单哪项物料在卡壳
- 已经交付的订单，客户临时加一张展架或改尺寸，变更过程没有记录，说不清谁确认了什么

一句话：**"上传了文件"、"客户确认了文件"、"整笔订单完成"是三件不同的事，但现在被混成了一件。**

### 1.2 核心业务边界（首版范围）

| 做什么 | 不做什么 |
| --- | --- |
| 客户信息登记、物料订单创建、物料清单管理 | 合同/报价/支付/发票 |
| 订单和任务的状态流转、负责人分派 | 在线图片编辑 |
| 设计稿多版本记录（append-only） | 印刷/生产/库存/配送 |
| 客户修改意见录入、客户确认 | 真实邮件/短信通知 |
| 变更强制留痕、操作历史审计 | — |

### 1.3 三个角色 & 对应权限

| 角色 | 账号 | 密码 | 在系统里做什么 |
| --- | --- | --- | --- |
| 客户经理 | chenliang | demo123 | 登记客户、创建订单、记录客户意见、确认客户点头、发起变更、完成交付、取消自己的订单 |
| 业务负责人 | wangli | demo123 | 审核订单物料清单、分派设计人员、取消任意订单 |
| 设计制作人员 | zhangxiaoyi / liusiyuan | demo123 | 接收分派、上传设计稿、响应客户意见提交新版本 |

---

## 二、技术架构

### 2.1 技术栈

| 层次 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus (Spring Boot 3 专用) | 3.5.5 |
| 数据库 | MySQL | 8.0.28 |
| 前端框架 | Vue 3 + Composition API | 3.4.38 |
| UI 组件 | Element Plus | 2.8.4 |
| 状态管理 | Pinia | 2.2.2 |
| 路由 | Vue Router | 4.4.3 |
| HTTP | Axios | 1.7.7 |
| 构建 | Vite | 5.4.2 |
| JDK | — | 17 |

### 2.2 后端目录结构（62 源文件）

```
backend/src/main/java/com/admaterial/
├── AdMaterialApplication.java       启动类 (@MapperScan + ASCII 横幅)
├── common/                           通用基础设施
│   ├── Result.java                   统一响应 {code, message, data}
│   ├── LoginUser.java                当前登录用户 DTO
│   ├── UserContext.java              ThreadLocal 持有登录上下文 + requireLogin()
│   ├── BusinessException.java        业务异常（带 HTTP code + errorKey）
│   ├── GlobalExceptionHandler.java   全局异常处理
│   └── enums/                        7 个状态枚举
│       ├── UserRole.java             CS_MANAGER / BIZ_LEADER / DESIGNER
│       ├── OrderStatus.java          6 种订单状态
│       ├── TaskStatus.java           7 种任务状态（含 isFinalOrIgnored()）
│       ├── MaterialType.java         5 种物料类型
│       ├── ChangeType.java           6 种变更类型
│       ├── BusinessType.java         5 种业务对象类型
│       └── OperationType.java        11 种操作类型
├── entity/                           8 张表的实体类
├── mapper/                           8 个 Mapper 接口（继承 BaseMapper）
├── config/
│   ├── MyBatisPlusConfig.java        分页插件 + 审计字段自动填充
│   ├── WebMvcConfig.java             跨域 CORS
│   └── AuthFilter.java               严格前置鉴权（缺 Header → 401 JSON）
├── service/                          业务逻辑层（7 个 Service）
├── controller/                       REST 接口（5 个 Controller ~25 个接口）
└── dto/ vo/                          入参 DTO + 出参 VO
```

### 2.3 前端目录结构

```
frontend/src/
├── main.js                           createApp + Pinia + ElementPlus(zh-cn)
├── App.vue                           <router-view />
├── api/
│   ├── request.js                    axios 实例 + 拦截器（自动带 X-User-Id + 统一错误提示 + 401 跳登录）
│   ├── auth.js                       login()
│   ├── order.js                      订单全链路 9 个方法
│   └── task.js                       任务全链路 7 个方法
├── stores/user.js                    Pinia Store（角色快捷判断 + localStorage 持久化 + 刷新恢复）
├── router/
│   ├── index.js                      Hash 路由 + 守卫（登录态 + 角色权限校验）
│   └── menu.js                       菜单配置（按角色过滤显示）
├── layout/AppLayout.vue              侧边栏（深色）+ Header（角色标签+退出）+ Main
└── views/
    ├── Login.vue                     渐变背景登录页 + 演示账号提示
    ├── Dashboard.vue                 工作台（三角色快捷入口 + 今日关注）
    ├── Forbidden.vue                 无权限页
    ├── order/
    │   ├── OrderList.vue             列表（5 种筛选 + 分页 + 逾期标记）
    │   ├── OrderCreate.vue           创建（客户 + deadline + 物料清单增删）
    │   └── OrderDetail.vue           详情（Tabs + 变更对话框 + 按钮 v-if 状态机对齐）
    └── task/
        ├── TaskList.vue              列表（7 种筛选 + 数据权限自动 + 快速分派）
        └── TaskDetail.vue            详情（时间线版本历史 + 客户意见表 + 上传/录意见/确认）
```

---

## 三、数据库设计

### 3.1 8 张表 + 演示数据

| 表名 | 行数 | 用途 |
| --- | ---: | --- |
| `sys_user` | 4 | 客户经理 / 负责人 / 设计师账号 |
| `customer` | 2 | 客户主数据（甜野奶茶 + 川味轩） |
| `material_order` | 4 | 订单主表（含当前 1 条 COMPLETED + 3 条演示/测试） |
| `material_task` | 9 | 订单拆出的物料任务 |
| `design_version` | 3 | 设计稿版本（append-only） |
| `modification_feedback` | 2 | 客户修改意见 |
| `order_change_record` | 6 | 订单变更强制留痕 |
| `operation_history` | 11+ | 全流程操作审计 |

### 3.2 设计亮点

| 亮点 | 实现 |
| --- | --- |
| `usage` 列是 MySQL 保留字 | `schema.sql` 用反引号包裹，Entity 加 `@TableField("\`usage\`")` |
| `task_status` VARCHAR 不够长 | 扩到 30（`PENDING_CLIENT_REVIEW` 22 字符） |
| 版本 append-only | `design_version` 表只 INSERT，永不 UPDATE 旧行 |
| 物料清单变更强制双写 | Service 层同步 INSERT `order_change_record` + 执行主表 UPDATE |
| 取消级联 | 订单 CANCELLED 自动 UPDATE 所有非终态任务 → CANCELLED |
| 审计自动填充 | MyBatis-Plus `MetaObjectHandler` 自动填 createTime / updateTime |
| 唯一约束 | `design_version(task_id, version_no)` 防重 |

### 3.3 订单状态机（6 种）

```
DRAFT ──负责人审核──► CONFIRMED ──首个分派──► IN_PROGRESS
                                                  │
                              所有物料 CLIENT_CONFIRMED ──► PENDING_DELIVERY ──客户经理完成──► COMPLETED
                                                  │
                                      任意非终态时刻可取消 ──► CANCELLED ✅（级联任务）
```

### 3.4 任务状态机（7 种）

```
PENDING_ASSIGN ──负责人分派──► ASSIGNED ──设计人员开始──► IN_PROGRESS
                                                              │
                                         上传初稿 ──► PENDING_CLIENT_REVIEW ──客户经理录意见──► NEEDS_REVISION
                                                              │
                                         提交新版本 ──► PENDING_CLIENT_REVIEW（上一轮意见自动置 processed=true）
                                                              │
                                         客户经理记录客户点头 ──► CLIENT_CONFIRMED（不可再变）
```

---

## 四、完整接口清单（25 个）

### 4.1 认证（1）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 账号密码登录（唯一放行接口） |

### 4.2 客户（2）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/customers` | 创建客户 |
| GET | `/api/customers` | 客户分页（支持 customerName 模糊） |

### 4.3 订单（9）

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/orders` | CS_MANAGER | 创建订单（带物料清单） |
| GET | `/api/orders` | 全部 | 列表（5 种筛选 + 数据权限） |
| GET | `/api/orders/{id}` | 全部 | 详情（聚合视图） |
| POST | `/api/orders/{id}/confirm` | BIZ_LEADER | 审核通过 |
| POST | `/api/orders/{id}/changes` | CS_MANAGER | 发起变更（强制双写） |
| GET | `/api/orders/{id}/changes` | 全部 | 变更记录列表 |
| POST | `/api/orders/{id}/complete` | CS_MANAGER | 完成交付（二次校验所有物料 CLIENT_CONFIRMED） |
| POST | `/api/orders/{id}/cancel` | CS_MANAGER / BIZ_LEADER | 取消（级联未完成任务） |

### 4.4 任务（7）

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/tasks` | 全部 | 列表（7 种筛选 + 三角色数据权限） |
| GET | `/api/tasks/{id}` | 全部 | 详情（主信息 + 版本历史 + 客户意见聚合） |
| POST | `/api/tasks/{id}/assign` | BIZ_LEADER | 单个分派 |
| POST | `/api/tasks/batch-assign` | BIZ_LEADER | 批量分派（事务） |
| POST | `/api/tasks/{id}/versions` | DESIGNER（assignee） | 上传设计稿（multipart + append-only） |
| POST | `/api/tasks/{id}/feedback` | CS_MANAGER（创建人） | 录入客户修改意见 |
| POST | `/api/tasks/{id}/client-confirm` | CS_MANAGER（创建人） | 记录客户确认（自动联动订单） |

### 4.5 用户（1）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/users?role=DESIGNER` | 下拉框用 |

### 4.6 鉴权规则

| 规则 | 后端实现 |
| --- | --- |
| 所有非 `/auth/login` 请求必须带 `X-User-Id` Header | `AuthFilter` 严格前置拦截 |
| 无效 userId 直接返回 401 JSON | `write401(response, msg)` |
| CS_MANAGER 列表只看自己创建的 | Service 层 `createBy = currentUserId` |
| DESIGNER 列表只看分派给自己的 | Service 层 `assigneeId = currentUserId` |
| BIZ_LEADER 可看全部 | 不做额外过滤 |
| DESIGNER 不能上传非自己任务的设计稿 | Service 层 `task.getAssigneeId() != currentUserId` → 403 |

---

## 五、端到端验证记录

### 5.1 后端 curl 15 步主链路（全通）

| # | 步骤 | 结果 |
| --- | --- | --- |
| 1 | 登录 chenliang | ✅ userId=1, role=CS_MANAGER |
| 2 | 创建订单（2 项物料） | ✅ orderId=4, DRAFT |
| 3 | 订单详情 | ✅ 聚合返回完整 |
| 4 | 负责人审核通过 | ✅ → CONFIRMED |
| 5 | 查设计人员下拉 | ✅ 2 个 DESIGNER |
| 6 | 批量分派 task#8/#9 | ✅ data:2 |
| 7 | 上传初稿 v1 | ✅ versionNo=1 |
| 8 | 录客户意见 | ✅ feedbackId, processed=false |
| 9 | 提交新版本 v2 | ✅ versionNo=2, 上一条 processed→true |
| 10 | 客户经理确认 v2 | ✅ CLIENT_CONFIRMED |
| 11 | 海报确认 + 展架未处理 | ✅ 订单保持 IN_PROGRESS |
| 12 | 刘思源上传展架初稿 | ✅ versionNo=1 |
| 13 | 客户经理确认展架 | ✅ CLIENT_CONFIRMED |
| 14 | 所有物料确认 | ✅ 订单自动 → **PENDING_DELIVERY** |
| 15 | 客户经理完成交付 | ✅ 订单 → **COMPLETED** |

### 5.2 第五章变更 + 取消链路验证（全通）

| 验证项 | 结果 |
| --- | --- |
| ORDER_DEADLINE_CHANGE 自动改主表 | ✅ 11-25 → 11-28 |
| TASK_ADD 自动 INSERT + taskNo=max+1 | ✅ taskNo=3, status=PENDING_ASSIGN |
| 变更记录查询 | ✅ 2 条都在 |
| COMPLETED 终态发起变更 → 409 | ✅ |
| 非创建人越权 → 403 | ✅ |
| DRAFT 走变更 → 422 | ✅ |
| 取消级联所有未完成物料 | ✅ ASSIGNED×2 + PENDING_ASSIGN×1 全变 CANCELLED |

### 5.3 鉴权 401 拦截验证（全通）

| 场景 | HTTP Code | 结果 |
| --- | --- | --- |
| 不带 Header 直接请求业务接口 | **401** | ✅ `{"code":401,"message":"未登录","errorKey":"unauthorized"}` |
| 带 `X-User-Id: 1` | **200** | ✅ 正常返回 |
| 登录接口（应放行） | **200** | ✅ AuthFilter 不拦截 |

### 5.4 浏览器 UI 拦截验证（全通）

| 场景 | 结果 |
| --- | --- |
| chenliang 登录 → 订单列表 | ✅ |
| DRAFT 订单详情页"发起变更"按钮**不可见** | ✅ `canAddChange` 拦截 |
| wangli 登录 → 审核通过 | ✅ 状态变已确认 |
| CONFIRMED 订单详情页"发起变更"按钮**可见** | ✅ |
| 变更对话框能打开 + 6 种 ChangeType 选项 | ✅ |
| 取消订单确认框 + 输入原因 | ✅ |

---

## 六、按钮 v-if 状态机对齐（前端）

```javascript
canConfirm     // DRAFT + BIZ_LEADER
canAddChange   // 非 DRAFT/COMPLETED/CANCELLED + CS_MANAGER + 创建人
canComplete    // PENDING_DELIVERY + CS_MANAGER
canCancel      // 非 COMPLETED/CANCELLED + (CS_MANAGER(自己的) / BIZ_LEADER(任意))
```

**这 4 个 computed 确保前端看到的按钮和后端 Service 层的状态机拦截 100% 对齐**——不会出现"按钮亮着但点了报错"的假可用性。

---

## 七、开发过程中修复的坑

| # | 坑 | 根因 | 修复 |
| --- | --- | --- | --- |
| 1 | `usage` 是 MySQL 保留字 | schema.sql 没加反引号 + Entity 没加注解 | SQL 用反引号，Entity 加 `@TableField("\`usage\`")` |
| 2 | `characterEncoding=utf8mb4` 不被 Java 识别 | JDBC URL 写法错 | 改成 `UTF-8` |
| 3 | `task_status VARCHAR(20)` 不够长 | `PENDING_CLIENT_REVIEW` 22 字符 | 扩到 VARCHAR(30) |
| 4 | `./uploads` 路径变 Tomcat 临时目录 | 相对路径在嵌入式 Tomcat 下被解析到临时目录 | 改成绝对路径 `D:/swapplatform/ad/uploads` |
| 5 | Spring Boot `sql.init.mode: always` 每次重启清库 | 开发/演示阶段初始化后没关 | 改成 `never`（手动执行一次后关闭） |
| 6 | AxiosError 401 前后端不对齐 | AuthFilter 不返回 401 只放行，Service 层 NPE → 500 | AuthFilter 严格前置拦截，缺 Header 直接返回 401 JSON |
| 7 | `listUsersByRole` 导入路径错 | OrderDetail.vue 从 `@/api/order` 导入但实际在 `task.js` | 从 `@/api/task` 导入 |
| 8 | Windows 下 `PID` 是 PowerShell 内置只读变量 | Stop-Process 用 `$pid` 时冲突 | 改名 `$procId` |
| 9 | MySQL 密码不是 root | 本机 MySQL 密码是 `1802787400` | 更新 application.yml |

---

## 八、运行方式

### 8.1 前置条件

- JDK 17
- Maven
- Node.js 24+
- MySQL 8.0（已在本机运行）

### 8.2 启动顺序

```bash
# 1. MySQL（已在运行，密码 1802787400）

# 2. 后端
cd D:\swapplatform\ad\backend
# 首次：执行 schema.sql 初始化
mysql -u root -p1802787400 ad_material < src/main/resources/schema.sql
# 启动
mvn spring-boot:run
# 运行在 http://localhost:8080/api

# 3. 前端
cd D:\swapplatform\ad\frontend
npm install          # 首次
npm run dev
# 运行在 http://localhost:5173
# Vite 自动把 /api 请求反代到后端 8080
```

### 8.3 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 客户经理 | chenliang | demo123 |
| 业务负责人 | wangli | demo123 |
| 设计师 | zhangxiaoyi | demo123 |
| 设计师 | liusiyuan | demo123 |

---

## 九、最终交付文件清单

```
D:\swapplatform\ad\
├── project-canonical.md              原始任务书
├── schema.sql                        数据库 DDL（含修复后：反引号 usage / VARCHAR(30)）
├── backend/
│   ├── pom.xml                       Spring Boot 3.2.5 + MyBatis-Plus + JWT
│   └── src/main/
│       ├── java/com/admaterial/      62 个 Java 源文件（Entity/Mapper/Service/Controller/Config/Common）
│       └── resources/
│           ├── application.yml       端口 8080 + context-path /api + MySQL 连接
│           └── schema.sql            复制的建表脚本
└── frontend/
    ├── package.json                  Vue 3.4 + Element Plus 2.8 + Pinia + Axios
    ├── vite.config.js                端口 5173 + /api 反代
    └── src/                          完整前端（api/stores/router/layout/views）
```

---

## 十、课程收获

1. **企业业务系统的底线认识**：上传文件 ≠ 客户确认 ≠ 订单完成——三个"看似一样"的动作在数据库里是三个独立的状态变迁点，每个点都要有明确的业务规则拦截。
2. **状态机驱动的设计方法**：先画清楚每个对象有什么状态、状态之间允许怎么跳、谁能触发、什么条件，再写数据库和代码——避免"想到啥写啥"。
3. **前后端契约对齐**：前端的按钮 v-if 和后端的 Service 层拦截必须 100% 对齐，否则就是按钮亮着点了报错的假可用性。
4. **保留字陷阱**：MySQL 保留字 `usage`、`order`、`group` 在写 SQL 和 Entity 时都要小心。
5. **安全前置 vs 事后兜底**：AuthFilter 严格前置拦截（缺 Header 直接 401）比"放行后 Service 层自己空指针 NPE"好得多。

---

*完*

> 本报告由 Agent 自动生成，基于本次交付的完整代码、数据库、接口验证记录。运行环境：Windows 10/11 + MySQL 8.0.28 + JDK 17.0.5 + Spring Boot 3.2.5 + Vue 3.4.38。所有服务已在 localhost 运行，可直接访问体验。
