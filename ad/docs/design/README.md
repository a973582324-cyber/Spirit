# design/ · 设计层

> 回答 **"怎么做 · 怎么实现"** —— 从数据 → 页面 → 接口 → 代码 的完整技术设计

---

## 文件索引（按设计顺序排列）

| 编号 | 文件 | 章节 | 内容 | 读者 |
| --- | --- | --- | --- | --- |
| **02** | [02-database-design.md](./02-database-design.md) | 第二章 | 8 表字段详解 + ER 图 + 订单/任务双状态机 + 业务规则怎么落地到数据库 | 后端 / 全栈 |
| **03** | [03-pages-and-workbench.md](./03-pages-and-workbench.md) | 第三章 | 7 个页面线框图 + 路由表 + 按钮 v-if 状态机对齐 + 4 条核心时序图 | 前端 / 产品 |
| **04** | [04-api-contracts.md](./04-api-contracts.md) | 第三章 | 25 个 REST 接口 + 权限矩阵 + 输入输出 + 业务规则 + 错误码 | 前后端 / 测试 |
| **05** | [05-java-objects.md](./05-java-objects.md) | 第三章 | 8 Entity + DTO/VO + Service 伪代码 + 状态机校验 | 后端 |
| **06** | [06-four-layer-mapping.md](./06-four-layer-mapping.md) | 第三章 | 页面 → 接口 → 对象 → 表 的完整映射矩阵 | 所有人（对齐用） |
| **07** | [07-status-matrix.md](./07-status-matrix.md) | 第三章 | 9 场景 × 4 种情况（正常/空数据/异常/无权限） | 测试 / 后端 |
| **08** | [08-design-review.md](./08-design-review.md) | 第三章 | 设计时修改过什么 + 遗留什么问题 | 所有人 |

---

## 设计链路

```
需求（requirements/）
    │
    ▼
02 database-design ──── 数据层先行
    │                    （先想清楚存什么）
    ▼
03 pages-and-workbench ── 页面交互
    │                    （用户看到什么）
    ▼
04 api-contracts ──────── 接口契约
    │                    （页面调什么）
    ▼
05 java-objects ───────── Java 对象模型
    │                    （接口背后的结构）
    │
    ├──► 06 four-layer-mapping ── 四层对齐检查
    ├──► 07 status-matrix ────── 边界全覆盖
    └──► 08 design-review ────── 设计复盘
```

---

## 设计 → 运行时代码 快速对照

| 想找什么 | 看设计文件 | 看代码 |
| --- | --- | --- |
| 订单状态机怎么定义 | `02-database-design.md` § 三 | `entity/MaterialOrder.java` + `enums/OrderStatus.java` |
| 前端按钮什么时候显示 | `03-pages-and-workbench.md` § 三 | `views/order/OrderDetail.vue` 的 4 个 `computed canXxx` |
| 后端接口长什么样 | `04-api-contracts.md` | `controller/*.java` |
| Service 层怎么校验状态机 | `05-java-objects.md` § 状态机 | `service/OrderService.java` / `service/TaskService.java` |
| 某个页面元素调哪个接口 | `06-four-layer-mapping.md` | — |
| 空数据/异常怎么处理 | `07-status-matrix.md` | 全局异常处理 + Service 层校验 |
| 设计时有哪些已知问题 | `08-design-review.md` | — |
