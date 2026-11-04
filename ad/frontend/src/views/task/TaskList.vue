<template>
  <el-card>
    <el-form :inline="true" :model="query" size="default" @submit.prevent="load">
      <el-form-item label="物料类型">
        <el-select v-model="query.materialType" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="m in materialTypes" :key="m.code" :label="m.label" :value="m.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态">
        <el-select v-model="query.taskStatus" clearable placeholder="全部" style="width: 160px">
          <el-option v-for="s in taskStatusOptions" :key="s.code" :label="s.label" :value="s.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="仅逾期">
        <el-switch v-model="query.overdue" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" stripe>
      <el-table-column label="#" width="50" prop="taskNo" />
      <el-table-column prop="orderNo" label="所属订单" width="160" />
      <el-table-column label="物料类型" width="120">
        <template #default="{ row }">{{ row.materialTypeLabel }}</template>
      </el-table-column>
      <el-table-column prop="sizeSpec" label="规格" width="160" />
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
        <template #default="{ row }">
          <template v-if="userStore.isLeader && row.taskStatus==='PENDING_ASSIGN'">
            <el-select v-if="row._assigning" v-model="row._assigneeId" placeholder="选择设计人员" size="small" style="width:120px">
              <el-option v-for="d in designerOptions" :key="d.id" :label="d.realName" :value="d.id" />
            </el-select>
            <template v-else>
              <el-button size="small" link type="primary" @click="startAssign(row)">分派</el-button>
            </template>
          </template>
          <span v-else>{{ row.assigneeName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="版本数" width="80" prop="versionCount" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" link @click="$router.push(`/tasks/${row.taskId}`)">详情</el-button>
          <el-button v-if="userStore.isLeader && row._assigning" size="small" link type="success" @click="confirmAssign(row)">确定</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 16px; justify-content: flex-end"
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" layout="prev, pager, next, total"
      @current-change="load" @size-change="load" />
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listTasks, assignTask, listUsersByRole } from '@/api/task'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const designerOptions = ref([])

const query = reactive({
  page: 0, size: 10,
  materialType: '', taskStatus: '', overdue: false
})

const materialTypes = [
  { code: 'POSTER', label: '海报' },
  { code: 'STANDEE', label: '展架' },
  { code: 'MENU', label: '菜单' },
  { code: 'BROCHURE', label: '宣传册' },
  { code: 'ONLINE_IMAGE', label: '线上配图' }
]

const taskStatusOptions = [
  { code: 'PENDING_ASSIGN', label: '待分派' },
  { code: 'ASSIGNED', label: '已分派' },
  { code: 'IN_PROGRESS', label: '制作中' },
  { code: 'PENDING_CLIENT_REVIEW', label: '待客户审核' },
  { code: 'NEEDS_REVISION', label: '需修改' },
  { code: 'CLIENT_CONFIRMED', label: '客户已确认' },
  { code: 'CANCELLED', label: '已取消' }
]

function taskStatusTag(code) {
  const map = { PENDING_ASSIGN: 'info', ASSIGNED: '', IN_PROGRESS: '',
                PENDING_CLIENT_REVIEW: 'warning', NEEDS_REVISION: 'danger',
                CLIENT_CONFIRMED: 'success', CANCELLED: 'info' }
  return map[code] || 'info'
}

function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.overdue) delete params.overdue
    const page = await listTasks(params)
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { page: 0, size: 10, materialType: '', taskStatus: '', overdue: false })
  load()
}

function startAssign(row) {
  designerOptions.value.forEach(d => { /* no-op */ })
  row._assigning = true
}

async function confirmAssign(row) {
  await assignTask(row.taskId, row._assigneeId)
  ElMessage.success('分派成功')
  load()
}

async function loadDesigners() {
  if (userStore.isLeader) {
    designerOptions.value = await listUsersByRole('DESIGNER')
  }
}

onMounted(async () => { await Promise.all([load(), loadDesigners()]) })
</script>
