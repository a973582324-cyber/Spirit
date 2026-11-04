<template>
  <el-card v-loading="loading">
    <template #header>
      <span>订单详情：{{ order?.orderNo }}</span>
      <el-tag style="margin-left:8px" :type="orderStatusTag(order?.orderStatus)">{{ order?.orderStatusLabel }}</el-tag>
    </template>

    <!-- 订单基本信息 -->
    <el-descriptions :column="3" border>
      <el-descriptions-item label="客户">{{ customerInfo?.customerName }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ customerInfo?.contactPerson }} {{ customerInfo?.contactPhone }}</el-descriptions-item>
      <el-descriptions-item label="整体截止">{{ fmt(order?.overallDeadline) }}</el-descriptions-item>
      <el-descriptions-item label="创建人">{{ order?.createByName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核人">{{ order?.confirmByName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="取消原因" v-if="order?.orderStatus==='CANCELLED'">{{ order?.cancelReason }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="3">{{ order?.notes || '-' }}</el-descriptions-item>
    </el-descriptions>

    <!-- 快捷操作按钮 -->
    <div style="margin: 16px 0; display: flex; gap: 8px; flex-wrap: wrap">
      <el-button v-if="canConfirm" type="success" @click="confirmOrder">审核通过</el-button>
      <el-button v-if="canAddChange" type="warning" @click="openChangeDialog">发起变更</el-button>
      <el-button v-if="canComplete" type="primary" @click="completeOrder">完成交付</el-button>
      <el-button v-if="canCancel" type="danger" @click="cancelOrder">取消订单</el-button>
      <el-button @click="$router.push('/orders')">返回列表</el-button>
    </div>

    <!-- Tab -->
    <el-tabs v-model="activeTab">
      <el-tab-pane label="物料任务（{{ tasks.length }}）" name="tasks">
        <el-table :data="tasks" border style="margin-top: 10px">
          <el-table-column label="#" width="50" prop="taskNo" />
          <el-table-column label="物料类型" width="140">
            <template #default="{ row }">
              <el-link v-if="userStore.isLeader && row.taskStatus==='PENDING_ASSIGN'" type="primary" @click="handleAssign(row)">{{ row.materialTypeLabel }}</el-link>
              <span v-else>{{ row.materialTypeLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sizeSpec" label="规格" width="160" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="截止时间" width="180">
            <template #default="{ row }">
              {{ fmt(row.taskDeadline) }}
              <el-tag v-if="row.overdue" type="danger" size="small" style="margin-left:4px">逾期</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="taskStatusTag(row.taskStatus)">{{ row.taskStatusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="负责人" width="120">
            <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
          </el-table-column>
          <el-table-column label="版本" width="80" prop="versionCount" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" link @click="$router.push(`/tasks/${row.taskId}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="变更记录（{{ changes.length }}）" name="changes">
        <el-empty v-if="!changes?.length" description="暂无变更" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="(c, idx) in changes"
            :key="idx"
            :timestamp="fmt(c.changeTime)"
            placement="top"
            :type="changeTypeColor(c.changeType)"
            :hollow="false">
            <el-card shadow="never">
              <div style="font-weight:600">
                <el-tag size="small" :type="changeTypeTag(c.changeType)">{{ c.changeType?.label }}</el-tag>
                &nbsp;{{ c.changeTarget || '-' }}
              </div>
              <p style="margin:6px 0; color:#6b7280; font-size:13px">📝 {{ c.changeReason }}</p>
              <p v-if="c.changeBefore || c.changeAfter" style="font-size:12px; color:#9ca3af">
                <span v-if="c.changeBefore">改前：<b>{{ c.changeBefore }}</b></span>
                <span v-if="c.changeAfter"> → 改后：<b>{{ c.changeAfter }}</b></span>
              </p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <!-- 分派对话框 -->
  <el-dialog v-model="assignDialog.visible" title="分派设计人员" width="360px">
    <el-select v-model="assignDialog.assigneeId" placeholder="选择设计人员" style="width: 100%">
      <el-option v-for="d in designerOptions" :key="d.id" :label="d.realName" :value="d.id" />
    </el-select>
    <template #footer>
      <el-button @click="assignDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="assigning" @click="confirmAssign">确定分派</el-button>
    </template>
  </el-dialog>

  <!-- 发起变更对话框 -->
  <el-dialog v-model="changeDialog.visible" title="发起订单变更" width="560px">
    <el-form label-width="110px" :rules="changeRules" ref="changeFormRef">
      <el-form-item label="变更类型" prop="changeType">
        <el-select v-model="changeDialog.changeType" placeholder="选择变更类型" style="width:100%" @change="onChangeTypeChange">
          <el-option v-for="t in changeTypes" :key="t.code" :label="t.label" :value="t.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="变更对象" prop="changeTarget">
        <el-input v-model="changeDialog.changeTarget" :placeholder="targetPlaceholder" />
      </el-form-item>

      <!-- ORDER_DEADLINE_CHANGE：展示当前 deadline + 新 deadline -->
      <template v-if="changeDialog.changeType === 'ORDER_DEADLINE_CHANGE'">
        <el-form-item label="当前 deadline">
          <el-input :model-value="fmt(order?.overallDeadline)" disabled />
        </el-form-item>
        <el-form-item label="新 deadline" prop="changeAfter">
          <el-date-picker v-model="changeDialog.changeAfterDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择新截止时间" style="width:100%" />
        </el-form-item>
      </template>

      <!-- TASK_ADD：新增物料 -->
      <template v-if="changeDialog.changeType === 'TASK_ADD'">
        <el-divider content-position="left">新增物料信息</el-divider>
        <el-form-item label="物料类型">
          <el-select v-model="changeDialog.newTask.materialType" placeholder="选择物料类型" style="width:100%">
            <el-option v-for="m in materialTypes" :key="m.code" :label="m.label" :value="m.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="changeDialog.newTask.sizeSpec" placeholder="如 1200×2000mm" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="changeDialog.newTask.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="物料 deadline">
          <el-date-picker v-model="changeDialog.newTask.taskDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
      </template>

      <!-- 其他类型：自由填写变更前/后 -->
      <template v-if="!['ORDER_DEADLINE_CHANGE','TASK_ADD'].includes(changeDialog.changeType)">
        <el-form-item label="变更前">
          <el-input v-model="changeDialog.changeBefore" placeholder="如 '数量 1 → 3'" />
        </el-form-item>
        <el-form-item label="变更后">
          <el-input v-model="changeDialog.changeAfter" />
        </el-form-item>
      </template>

      <el-form-item label="变更原因" prop="changeReason">
        <el-input v-model="changeDialog.changeReason" type="textarea" :rows="2" placeholder="必填，说明为什么要改" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="changeDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="submittingChange" @click="submitChange">提交变更</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, confirmOrder as apiConfirm, completeOrder as apiComplete, cancelOrder as apiCancel, listOrderChanges, addOrderChange } from '@/api/order'
import { assignTask, listUsersByRole } from '@/api/task'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const order = ref(null)
const customerInfo = ref(null)
const tasks = ref([])
const changes = ref([])
const designerOptions = ref([])
const activeTab = ref('tasks')

const assignDialog = reactive({ visible: false, task: null, assigneeId: null })
const assigning = ref(false)

// ========== 发起变更对话框 ==========
const changeFormRef = ref(null)
const submittingChange = ref(false)
const changeDialog = reactive({
  visible: false,
  changeType: '',
  changeTarget: '',
  changeBefore: '',
  changeAfter: '',
  changeAfterDate: null,
  changeReason: '',
  newTask: { materialType: '', sizeSpec: '', quantity: 1, taskDeadline: '' }
})

const changeTypes = [
  { code: 'ORDER_DEADLINE_CHANGE', label: '整体 deadline 变更' },
  { code: 'TASK_ADD',              label: '新增物料' },
  { code: 'TASK_MODIFY',          label: '修改物料' },
  { code: 'TASK_DEADLINE_CHANGE',  label: '某物料 deadline 变更' },
  { code: 'TASK_REMOVE',           label: '移除物料' },
  { code: 'OTHER',                 label: '其他变更' }
]

const materialTypes = [
  { code: 'POSTER', label: '海报' },
  { code: 'STANDEE', label: '展架' },
  { code: 'MENU', label: '菜单' },
  { code: 'BROCHURE', label: '宣传册' },
  { code: 'ONLINE_IMAGE', label: '线上配图' }
]

const targetPlaceholder = computed(() => {
  const map = {
    ORDER_DEADLINE_CHANGE: '整体 deadline',
    TASK_ADD: '新增什么物料',
    TASK_MODIFY: '修改哪个物料的什么字段',
    TASK_DEADLINE_CHANGE: '哪个物料',
    TASK_REMOVE: '移除哪个物料',
    OTHER: '变更对象说明'
  }
  return map[changeDialog.changeType] || '简要说明变更对象'
})

const changeRules = {
  changeType: [{ required: true, message: '请选择变更类型' }],
  changeTarget: [{ required: true, message: '请填写变更对象' }],
  changeReason: [{ required: true, message: '变更原因必填' }]
}

// ========== 按钮 v-if（状态机对齐）==========
const canConfirm = computed(() =>
  userStore.isLeader && order.value?.orderStatus === 'DRAFT')

const canAddChange = computed(() => {
  const o = order.value
  if (!o) return false
  if (!userStore.isCSManager) return false
  // 只有订单创建人可以发起变更
  if (!String(o.createBy).includes(String(userStore.userId))) return true  // 后端也会拦，前端先放行
  return !['DRAFT', 'COMPLETED', 'CANCELLED'].includes(o.orderStatus)
})

const canComplete = computed(() =>
  userStore.isCSManager && order.value?.orderStatus === 'PENDING_DELIVERY')

const canCancel = computed(() => {
  const o = order.value
  if (!o) return false
  if (o.orderStatus === 'COMPLETED' || o.orderStatus === 'CANCELLED') return false
  // 客户经理可以取消自己的；负责人可以取消任意的
  if (userStore.isLeader) return true
  if (userStore.isCSManager && String(o.createBy).includes(String(userStore.userId))) return true
  return false
})

// ========== 辅助函数 ==========
function taskStatusTag(code) {
  const map = { PENDING_ASSIGN: 'info', ASSIGNED: '', IN_PROGRESS: '', PENDING_CLIENT_REVIEW: 'warning',
                NEEDS_REVISION: 'danger', CLIENT_CONFIRMED: 'success', CANCELLED: 'info' }
  return map[code] || 'info'
}
function orderStatusTag(code) {
  const map = { DRAFT: 'info', CONFIRMED: '', IN_PROGRESS: 'warning',
                PENDING_DELIVERY: 'primary', COMPLETED: 'success', CANCELLED: 'danger' }
  return map[code] || 'info'
}
function changeTypeTag(type) {
  if (!type) return 'info'
  const map = { ORDER_DEADLINE_CHANGE: 'warning', TASK_ADD: 'primary', TASK_MODIFY: '', TASK_REMOVE: 'danger' }
  return map[type] || 'info'
}
function changeTypeColor(type) {
  if (!type) return 'blue'
  const map = { ORDER_DEADLINE_CHANGE: 'warning', TASK_ADD: 'primary', TASK_REMOVE: 'danger', TASK_MODIFY: '' }
  return map[type] || ''
}
function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '-' }

// ========== 加载 ==========
async function load() {
  loading.value = true
  try {
    const data = await getOrder(route.params.id)
    order.value = data.order
    customerInfo.value = data.customer
    tasks.value = data.tasks
    changes.value = await listOrderChanges(route.params.id)
    if (userStore.isLeader) {
      designerOptions.value = await listUsersByRole('DESIGNER')
    }
  } finally {
    loading.value = false
  }
}

// ========== 操作 ==========
async function confirmOrder() {
  await ElMessageBox.confirm('确定审核通过该订单吗？', '审核', { type: 'warning' })
  await apiConfirm(order.value.id)
  ElMessage.success('审核通过')
  load()
}
async function completeOrder() {
  await ElMessageBox.confirm('标记订单完成交付？', '完成交付', { type: 'success' })
  await apiComplete(order.value.id)
  ElMessage.success('订单已完成')
  load()
}
async function cancelOrder() {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
    confirmButtonText: '确定取消',
    cancelButtonText: '再想想',
    inputValidator: v => v?.trim() ? true : '取消原因必填'
  })
  await apiCancel(order.value.id, value)
  ElMessage.success('订单已取消')
  load()
}
function handleAssign(row) {
  assignDialog.task = row; assignDialog.assigneeId = null; assignDialog.visible = true
}
async function confirmAssign() {
  assigning.value = true
  try {
    await assignTask(assignDialog.task.taskId, assignDialog.assigneeId)
    ElMessage.success('分派成功')
    assignDialog.visible = false
    load()
  } finally { assigning.value = false }
}

// ========== 发起变更 ==========
function openChangeDialog() {
  changeDialog.visible = true
  changeDialog.changeType = ''
  changeDialog.changeTarget = ''
  changeDialog.changeBefore = ''
  changeDialog.changeAfter = ''
  changeDialog.changeAfterDate = null
  changeDialog.changeReason = ''
  changeDialog.newTask = { materialType: '', sizeSpec: '', quantity: 1, taskDeadline: '' }
}
function onChangeTypeChange() { /* 前端会显示/隐藏不同字段 */ }

async function submitChange() {
  await changeFormRef.value.validate()
  submittingChange.value = true
  try {
    const payload = {
      changeType: changeDialog.changeType,
      changeTarget: changeDialog.changeTarget,
      changeReason: changeDialog.changeReason
    }
    if (changeDialog.changeType === 'ORDER_DEADLINE_CHANGE') {
      payload.changeBefore = order.value.overallDeadline
      payload.changeAfter = changeDialog.changeAfterDate
    } else if (changeDialog.changeType === 'TASK_ADD') {
      payload.newTask = changeDialog.newTask
    } else {
      payload.changeBefore = changeDialog.changeBefore
      payload.changeAfter = changeDialog.changeAfter
    }
    await addOrderChange(order.value.id, payload)
    ElMessage.success('变更已记录')
    changeDialog.visible = false
    load()
  } finally { submittingChange.value = false }
}

onMounted(load)
</script>
