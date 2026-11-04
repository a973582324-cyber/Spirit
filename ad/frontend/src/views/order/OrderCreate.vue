<template>
  <el-card>
    <template #header>
      <span>创建订单</span>
    </template>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" size="default">
      <!-- 客户 -->
      <el-form-item label="客户" prop="customerId">
        <el-select v-model="form.customerId" filterable placeholder="搜索并选择客户" style="width: 320px">
          <el-option v-for="c in customerOptions" :key="c.id" :label="c.customerName" :value="c.id" />
        </el-select>
        <el-button link type="primary" @click="showCustomerDialog = true">+ 新建客户</el-button>
      </el-form-item>

      <!-- 整体 deadline -->
      <el-form-item label="整体截止" prop="overallDeadline">
        <el-date-picker v-model="form.overallDeadline" type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        placeholder="选择整体截止时间" />
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="订单备注">
        <el-input v-model="form.notes" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>

    <!-- 物料清单 Tab -->
    <el-card style="margin: 16px 0">
      <template #header>
        物料清单（{{ form.tasks.length }} 项）
        <el-button size="small" link type="primary" @click="addTask">+ 新增物料</el-button>
      </template>

      <el-table :data="form.tasks" border>
        <el-table-column label="#" width="50" type="index" />
        <el-table-column label="物料类型">
          <template #default="{ row }">
            <el-select v-model="row.materialType" placeholder="选择物料类型">
              <el-option v-for="m in materialTypes" :key="m.code" :label="m.label" :value="m.code" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="尺寸规格" width="180">
          <template #default="{ row }">
            <el-input v-model="row.sizeSpec" placeholder="如 1200×2000mm" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" />
          </template>
        </el-table-column>
        <el-table-column label="用途" width="140">
          <template #default="{ row }">
            <el-input v-model="row.usage" placeholder="选填" />
          </template>
        </el-table-column>
        <el-table-column label="物料截止" width="200">
          <template #default="{ row }">
            <el-date-picker v-model="row.taskDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button size="small" link type="danger" @click="removeTask($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-button type="primary" :loading="submitting" @click="handleSubmit">提交创建</el-button>
    <el-button @click="$router.back()">取消</el-button>
  </el-card>

  <!-- 新建客户对话框 -->
  <el-dialog v-model="showCustomerDialog" title="新建客户" width="480px">
    <el-form :model="customerForm" label-width="80px" ref="customerFormRef">
      <el-form-item label="客户名"><el-input v-model="customerForm.customerName" /></el-form-item>
      <el-form-item label="联系人"><el-input v-model="customerForm.contactPerson" /></el-form-item>
      <el-form-item label="电话"><el-input v-model="customerForm.contactPhone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="customerForm.contactEmail" /></el-form-item>
      <el-form-item label="地址"><el-input v-model="customerForm.address" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCustomerDialog = false">取消</el-button>
      <el-button type="primary" :loading="creatingCustomer" @click="handleCreateCustomer">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder, createCustomer, listOrders } from '@/api/order'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const showCustomerDialog = ref(false)
const creatingCustomer = ref(false)
const customerFormRef = ref(null)
const customerOptions = ref([])

const materialTypes = [
  { code: 'POSTER', label: '海报' },
  { code: 'STANDEE', label: '展架' },
  { code: 'MENU', label: '菜单' },
  { code: 'BROCHURE', label: '宣传册' },
  { code: 'ONLINE_IMAGE', label: '线上配图' }
]

const form = reactive({
  customerId: null,
  overallDeadline: '',
  notes: '',
  tasks: []
})

const customerForm = reactive({
  customerName: '', contactPerson: '', contactPhone: '', contactEmail: '', address: ''
})

const rules = {
  customerId: [{ required: true, message: '请选择客户' }],
  overallDeadline: [{ required: true, message: '请选择整体截止时间' }]
}

function addTask() {
  form.tasks.push({
    materialType: '', sizeSpec: '', quantity: 1,
    usage: '', refNotes: '', taskDeadline: ''
  })
}

function removeTask(i) { form.tasks.splice(i, 1) }

async function handleSubmit() {
  await formRef.value.validate()
  if (form.tasks.length === 0) {
    ElMessage.warning('至少添加 1 项物料')
    return
  }
  submitting.value = true
  try {
    const result = await createOrder(form)
    ElMessage.success('订单创建成功：' + result.order.orderNo)
    router.push(`/orders/${result.order.id}`)
  } finally {
    submitting.value = false
  }
}

async function loadCustomers() {
  // 用 orders 接口间接查客户（前端 demo 简化）
  // 实际后端应提供 GET /customers 列表
  customerOptions.value = [
    { id: 1, customerName: '奶茶先生连锁' },
    { id: 2, customerName: 'XX健身俱乐部' }
  ]
}

async function handleCreateCustomer() {
  creatingCustomer.value = true
  try {
    const c = await createCustomer(customerForm)
    customerOptions.value.push(c)
    form.customerId = c.id
    showCustomerDialog.value = false
    ElMessage.success('客户创建成功')
    Object.assign(customerForm, { customerName: '', contactPerson: '', contactPhone: '', contactEmail: '', address: '' })
  } finally {
    creatingCustomer.value = false
  }
}

onMounted(loadCustomers)
</script>
