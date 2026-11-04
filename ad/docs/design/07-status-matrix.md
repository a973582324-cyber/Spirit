# 状态处理矩阵：正常 / 空数据 / 异常 / 无权限

> 这是文件明确要求的"正常、空数据、异常和无权限状态"覆盖。每个核心场景逐一列出：应该怎么处理、返回什么、页面怎么呈现。

---

## 一、订单创建

| 场景 | 条件 | 后端行为 | HTTP code | 返回 |
| --- | --- | --- | --- | --- |
| ✅ 正常创建 | customerId 存在；overallDeadline > now；tasks 非空；taskDeadline 均 <= overallDeadline | INSERT order + batch INSERT tasks；状态 DRAFT / PENDING_ASSIGN；写 operation_history | 201 | `OrderDetailVO` |
| ⚠️ 客户不存在 | customerId 查不到 | 拒绝 | 404 | `{ errorKey: "customerNotFound" }` |
| ⚠️ taskDeadline 晚于 overallDeadline | 创建时物料截止时间比订单整体还晚 | 拒绝 | 422 | `{ errorKey: "taskDeadlineAfterOverall" }` |
| ⚠️ tasks 为空 | 没带任何物料 | 拒绝 | 422 | `{ errorKey: "emptyTaskList" }` |
| ⚠️ 客户已停用 | customer.enabled = false | 拒绝 | 422 | `{ errorKey: "customerDisabled" }` |
| 🚫 无权限 | 非 CS_MANAGER 发起 | 拒绝 | 403 | `{ errorKey: "forbiddenRole" }` |

**页面呈现**：
- 正常 → 跳转订单详情页，顶部 toast "订单创建成功，等待负责人审核"
- 422 → 表单红字提示具体字段
- 403 / 404 → 全局错误弹窗

---

## 二、订单审核

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 审核通过 | status = DRAFT；操作人 = BIZ_LEADER | status → CONFIRMED；写 confirmBy / confirmTime；operation_history | 200 |
| ✅ 打回修改 | 同上 | 回到 DRAFT 或 CANCELLED（可配置）；写 reject reason | 200 |
| ⚠️ 订单不是 DRAFT | 已经 CONFIRMED / IN_PROGRESS 了 | 拒绝 | 409 |
| 🚫 无权限 | 非 BIZ_LEADER | 拒绝 | 403 |

---

## 三、任务分派

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 正常分派 | task status = PENDING_ASSIGN；assigneeId 的用户 role = DESIGNER | UPDATE assignee_id；status → ASSIGNED | 200 |
| ✅ 批量分派 | 同上 | 事务内批量 | 200 |
| ⚠️ 任务已分派 | status != PENDING_ASSIGN | 拒绝 | 409 |
| ⚠️ assignee 不是设计人员 | 选了客户经理或负责人 | 拒绝 | 422 |
| 🚫 无权限 | 非 BIZ_LEADER | 拒绝 | 403 |

---

## 四、设计稿上传

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 正常上传 | assignee_id = 当前用户；任务非 PENDING_ASSIGN；文件不为空 | INSERT design_version(versionNo=max+1)；UPDATE task.version_count++ / last_submit_time / status→PENDING_CLIENT_REVIEW；上一轮未处理的 feedback 自动 processed=1 | 201 |
| ⚠️ 任务还没分派 | assignee_id IS NULL | 拒绝 | 409 |
| ⚠️ 文件为空 | MultipartFile.isEmpty() | 拒绝 | 422 |
| ⚠️ 文件类型不支持 | 不是图片/PDF/常见设计格式 | 拒绝 | 422 |
| ⚠️ 文件超大 | > 20MB（demo 可改） | 拒绝 | 422 |
| 🚫 不是自己的任务 | task.assigneeId != 当前用户 | 拒绝 | 403 |
| 🚫 无角色权限 | 非 DESIGNER | 拒绝 | 403 |

**页面呈现**：
- 正常 → 版本列表自动刷新到最上方，最新版本高亮；任务状态徽章变"待客户审核"
- 403 → 红色提示"该任务未分派给您"
- 422 → 文件上传控件下方红字提示

---

## 五、客户修改意见录入

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 正常录入 | targetVersionNo 存在；任务未 CANCELLED / 未 CLIENT_CONFIRMED | INSERT modification_feedback(processed=0)；UPDATE task status→NEEDS_REVISION / last_feedback_time | 201 |
| ⚠️ 该版本不存在 | targetVersionNo 在 design_version 里查不到 | 拒绝 | 404 |
| ⚠️ 任务已客户确认 | task.status = CLIENT_CONFIRMED | 拒绝 | 409 |
| 🚫 无权限 | 非 CS_MANAGER | 拒绝 | 403 |

**页面呈现**：
- 正常 → 意见卡片追加到任务下方；任务状态徽章变"需修改"
- 409 → "该物料客户已确认，无法再录入意见"

---

## 六、客户确认

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 正常确认 | task.status != CLIENT_CONFIRMED（还没确认过） | status → CLIENT_CONFIRMED；同步检查订单是否所有物料都 CLIENT_CONFIRMED/CANCELLED，是则订单 → PENDING_DELIVERY | 200 |
| ⚠️ 已确认过 | task.status = CLIENT_CONFIRMED | 拒绝 | 409 |
| ⚠️ 任务已取消 | task.status = CANCELLED | 拒绝 | 409 |
| 🚫 无权限 | 非 CS_MANAGER | 拒绝 | 403 |

**自动联动（后端业务规则 7）**：
```
taskStatus = CLIENT_CONFIRMED
    →
SELECT COUNT(*) FROM material_task
WHERE order_id = #{orderId} AND task_status NOT IN ('CLIENT_CONFIRMED','CANCELLED')
    →
0 条 → UPDATE material_order SET order_status = 'PENDING_DELIVERY'
>0 条 → 订单维持 IN_PROGRESS
```

---

## 七、订单完成（整体交付）

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 正常完成 | orderStatus = PENDING_DELIVERY；所有物料 CLIENT_CONFIRMED/CANCELLED | status → COMPLETED；写 completeTime | 200 |
| ⚠️ 还有未完成物料 | 至少一个 task 状态不在 CLIENT_CONFIRMED / CANCELLED | 拒绝 | 409 |
| ⚠️ 订单状态不对 | DRAFT / CONFIRMED / IN_PROGRESS | 拒绝 | 409 |
| ⚠️ 订单已取消 / 已完成 | 终态 | 拒绝 | 409 |
| 🚫 无权限 | 非订单创建人 AND 非 BIZ_LEADER | 拒绝 | 403 |

**409 详细返回**（让前端能展示具体哪个卡壳了）：
```json
{
  "code": 409,
  "message": "还有 2 项物料未客户确认：task#2 展架、task#4 菜单",
  "errorKey": "unfinishedTasks",
  "data": {
    "unfinishedTaskNos": [2, 4]
  }
}
```

---

## 八、订单变更

| 场景 | 条件 | 后端行为 | HTTP code |
| --- | --- | --- | --- |
| ✅ 正常变更 | 订单状态为 CONFIRMED / IN_PROGRESS / PENDING_DELIVERY；changeReason 必填 | 事务内：① INSERT order_change_record ② UPDATE 主表 ③ INSERT operation_history | 200 |
| ⚠️ DRAFT 订单走变更流程 | 还没审核过 | 拒绝，提示"草稿订单直接编辑物料清单即可" | 409 |
| ⚠️ CANCELLED / COMPLETED 订单 | 终态不能改 | 拒绝 | 409 |
| ⚠️ changeReason 为空 | 必填没填 | 拒绝 | 422 |
| 🚫 无权限 | 非 CS_MANAGER | 拒绝 | 403 |

---

## 九、列表查询（空数据场景）

| 场景 | 后端行为 | 页面呈现 |
| --- | --- | --- |
| 我的工作台没订单 | 正常返回空列表 + totalElements=0 | 空状态占位图 + "暂无订单" + [+ 新建订单] 按钮 |
| 负责人的待审核中心为空 | 正常返回空列表 | 🎉 空状态 "当前没有待审核订单" |
| 设计人员的任务列表为空 | 正常返回空列表 | 空状态 "您暂无分派的任务" + 告知联系人 |
| 筛选条件无匹配 | 正常返回空列表 | 空状态 + "没有符合筛选条件的订单" + 提示调整筛选条件 |
| 跨页翻到超出最大页 | 返回空列表（不要抛错） | 正常表格空 |

---

## 十、无权限访问汇总（全局拦截）

| 场景 | 拦截点 | HTTP code | 处理 |
| --- | --- | --- | --- |
| 未登录 token 过期 | Auth 拦截器 | 401 | 前端跳登录页 |
| 角色不对（非设计人员上传版本） | Controller @PreAuthorize | 403 | 返回 errorKey |
| 数据归属不对（设计人员看别人的任务） | Service 层手动检查 | 403 | 返回 errorKey |
| 越权修改（非创建人 complete 订单） | Service 层检查 createBy | 403 | 返回 errorKey |
| 非 BIZ_LEADER 审核订单 | @PreAuthorize | 403 | 返回 errorKey |

**数据权限硬拦截（Service 层不能只靠 @PreAuthorize）**：
```java
// TaskService.getDetail 示例
public TaskDetailVO getDetail(Long taskId, Long currentUserId, UserRole role) {
    MaterialTask task = mapper.selectById(taskId);
    // 设计人员：只能看分派给自己的
    if (role == DESIGNER && !task.getAssigneeId().equals(currentUserId)) {
        throw new ForbiddenException("该任务未分派给您");
    }
    // CS_MANAGER：只能看自己创建的订单下的任务
    if (role == CS_MANAGER) {
        MaterialOrder order = orderMapper.selectById(task.getOrderId());
        if (!order.getCreateBy().equals(currentUserId)) {
            throw new ForbiddenException("您无权查看该订单下的任务");
        }
    }
    // BIZ_LEADER 放行
    return assembleVO(task);
}
```

---

## 十一、前端状态呈现速查

为了避免页面上到处出现"undefined"或"null"，统一一个状态徽章映射表给前端：

```javascript
const ORDER_STATUS_MAP = {
    DRAFT:            { label: '草稿',     color: 'gray'   },
    CONFIRMED:        { label: '已确认',   color: 'blue'   },
    IN_PROGRESS:      { label: '制作中',   color: 'orange' },
    PENDING_DELIVERY: { label: '待交付',   color: 'purple' },
    COMPLETED:        { label: '已完成',   color: 'green'  },
    CANCELLED:        { label: '已取消',   color: 'red'    }
};

const TASK_STATUS_MAP = {
    PENDING_ASSIGN:        { label: '待分派',     color: 'gray'   },
    ASSIGNED:              { label: '已分派',     color: 'blue'   },
    IN_PROGRESS:           { label: '制作中',     color: 'orange' },
    PENDING_CLIENT_REVIEW: { label: '待客户审核', color: 'purple' },
    NEEDS_REVISION:        { label: '需修改',     color: 'red'    },
    CLIENT_CONFIRMED:      { label: '客户已确认', color: 'green'  },
    CANCELLED:             { label: '已取消',     color: 'gray'   }
};
```
