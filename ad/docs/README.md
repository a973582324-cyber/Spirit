# docs/ 文档目录

> 宣传物料订单管理系统 · 完整文档链

---

## 目录结构

```
docs/
├── requirements/     ← 需求层 · 回答 "做什么 · 为什么做"
│   ├── 00-project-canonical.md     原始任务书
│   └── 01-direction-card.md        第一章方向卡
│
└── design/            ← 设计层 · 回答 "怎么做 · 怎么实现"
    ├── 02-database-design.md       数据库设计（8 表 + 双状态机）
    ├── 03-pages-and-workbench.md   页面结构 + 工作台 + 交互时序
    ├── 04-api-contracts.md         接口契约（25 个 REST + 权限）
    ├── 05-java-objects.md          Java 对象模型
    ├── 06-four-layer-mapping.md    四层映射矩阵（页面→接口→对象→表）
    ├── 07-status-matrix.md         状态处理矩阵（9 场景 × 4 种情况）
    └── 08-design-review.md         设计评审记录
```

---

## 阅读顺序

### 想了解"做什么" → 从 requirements/ 开始

1. `00-project-canonical.md` — 原始任务书，所有规则的源头
2. `01-direction-card.md` — 方向卡，明确边界和岗位

### 想了解"怎么实现" → 从 design/ 开始

推荐顺序（从下往上，符合实际设计过程）：

1. `02-database-design.md` — 先定数据长什么样
2. `03-pages-and-workbench.md` — 再画页面和交互
3. `04-api-contracts.md` — 页面需要什么接口
4. `05-java-objects.md` — 接口背后的 Java 对象
5. `06-four-layer-mapping.md` — 四层对齐检查（页面→接口→对象→表）
6. `07-status-matrix.md` — 边界场景全覆盖
7. `08-design-review.md` — 设计时改了什么、遗留了什么

---

## 设计 → 代码 对照表

| 设计文档 | 落盘位置 |
| --- | --- |
| `design/02-database-design.md` | `backend/src/main/resources/schema.sql` |
| `design/04-api-contracts.md` | `backend/.../controller/` + `frontend/src/api/` |
| `design/03-pages-and-workbench.md` | `frontend/src/views/` + `frontend/src/layout/` |
| `design/05-java-objects.md` | `backend/.../entity/` + `dto/` + `vo/` + `service/` |

---

## 配套文件（根目录）

| 文件 | 说明 |
| --- | --- |
| `DELIVERY_REPORT.md` | 完整交付总结（含运行方式 + 验证记录 + 修复清单） |
| `schema.sql` | 数据库 DDL（同 backend 里那份的备份） |
| `backend/` | Spring Boot 后端（62 源文件） |
| `frontend/` | Vue 3 前端（完整 UI） |
