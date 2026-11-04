<template>
  <el-card>
    <!-- 筛选条 -->
    <el-form :inline="true" :model="query" size="default" @submit.prevent="load">
      <el-form-item label="客户名">
        <el-input v-model="query.customerName" clearable placeholder="模糊搜索" style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 160px">
          <el-option v-for="s in orderStatusOptions" :key="s.code" :label="s.label" :value="s.code" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button v-if="userStore.isCSManager" type="success" @click="$router.push('/orders/create')">+ 新建订单</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table :data="records" v-loading="loading" stripe style="margin-top: 10px">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="customerName" label="客户" width="160" />
      <el-table-column prop="overallDeadline" label="整体截止" width="180" :formatter="fmtDate" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="orderStatusTag(row.orderStatus)">{{ row.orderStatusLabel }}</el-tag>
          <el-tag v-if="row.overdue" type="danger" size="small" style="margin-left:4px">已逾期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物料进度" width="140">
        <template #default="{ row }">
          {{ row.confirmedCount }}/{{ row.taskCount }}
        </template>
      </el-table-column>
      <el-table-column prop="createByName" label="创建人" width="120" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" link @click="$router.push(`/orders/${row.orderId}`)">详情</el-button>
          <el-button v-if="row.orderStatus==='DRAFT' && userStore.isLeader" size="small" link type="success" @click="handleConfirm(row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total" layout="prev, pager, next, total"
      @current-change="load" @size-change="load" />
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, confirmOrder } from '@/api/order'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({ page: 0, size: 10, customerName: '', status: '' })

// 订单状态选项（和后端 enum 对齐）
const orderStatusOptions = [
  { code: 'DRAFT', label: '草稿' },
  { code: 'CONFIRMED', label: '已确认' },
  { code: 'IN_PROGRESS', label: '制作中' },
  { code: 'PENDING_DELIVERY', label: '待交付' },
  { code: 'COMPLETED', label: '已完成' },
  { code: 'CANCELLED', label: '已取消' }
]

function orderStatusTag(code) {
  const map = { DRAFT: 'info', CONFIRMED: '', IN_PROGRESS: 'warning',
                PENDING_DELIVERY: 'primary', COMPLETED: 'success', CANCELLED: 'danger' }
  return map[code] || 'info'
}

function fmtDate(_, __, v) {
  return v ? v.replace('T', ' ').slice(0, 16) : '-'
}

async function load() {
  loading.value = true
  try {
    const page = await listOrders({ ...query })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { page: 0, size: 10, customerName: '', status: '' })
  load()
}

async function handleConfirm(row) {
  await ElMessageBox.confirm(`确认审核通过订单 ${row.orderNo}？`, '审核订单', { type: 'warning' })
  await confirmOrder(row.orderId)
  ElMessage.success('审核通过')
  load()
}

onMounted(load)
</script>
