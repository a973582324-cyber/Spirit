<template>
  <el-card v-loading="loading">
    <template #header>
      <span>任务详情：{{ task?.materialTypeLabel }} #{{ task?.taskNo }}</span>
      <el-tag style="margin-left:8px" :type="taskStatusTag(task?.taskStatus)">{{ task?.taskStatusLabel }}</el-tag>
      <el-link type="primary" style="margin-left:12px" @click="$router.push(`/orders/${task?.orderId}`)">返回订单</el-link>
    </template>

    <!-- 主信息 -->
    <el-descriptions :column="3" border>
      <el-descriptions-item label="所属订单">{{ task?.orderId }} · {{ detail?.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="负责人">{{ detail?.assigneeName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="物料截止">{{ fmt(task?.taskDeadline) }}</el-descriptions-item>
      <el-descriptions-item label="规格">{{ task?.sizeSpec }}</el-descriptions-item>
      <el-descriptions-item label="数量">{{ task?.quantity }}</el-descriptions-item>
      <el-descriptions-item label="用途">{{ task?.usage || '-' }}</el-descriptions-item>
      <el-descriptions-item label="参考说明" :span="3">{{ task?.refNotes || '-' }}</el-descriptions-item>
    </el-descriptions>

    <!-- Tab -->
    <el-tabs default-active="versions" style="margin-top: 16px">
      <!-- Tab 1：版本历史 -->
      <el-tab-pane label="设计稿版本（append-only，{{ versions?.length || 0 }} 个）" name="versions">
        <div style="margin-bottom: 16px">
          <!-- 上传按钮：只有 DESIGNER && assignee_id=currentUserId 可见 -->
          <el-upload
            v-if="canUpload"
            :show-file-list="false"
            :before-upload="handleUpload"
            accept=".png,.jpg,.jpeg,.pdf,.webp,.gif,.ai,.psd">
            <el-button type="primary">上传新设计稿</el-button>
          </el-upload>
        </div>

        <el-timeline>
          <el-timeline-item
            v-for="(v, idx) in versions"
            :key="v.id"
            :timestamp="fmt(v.uploadTime)"
            placement="top">
            <el-card shadow="hover" :class="{ confirmed: isConfirmedVersion(v) }">
              <template #header>
                <div style="display:flex; justify-content:space-between; align-items:center">
                  <span style="font-weight:bold">版本 v{{ v.versionNo }}</span>
                  <div>
                    <el-tag size="small" type="info">上传者 ID: {{ v.uploadBy }}</el-tag>
                    <el-button v-if="canFeedback" size="small" link type="warning" @click="openFeedback(v)">录客户意见</el-button>
                    <el-button v-if="canClientConfirm && !confirmedVersionNo" size="small" link type="success" @click="openConfirm(v)">记录客户确认</el-button>
                  </div>
                </div>
              </template>
              <p><el-link :href="v.fileUrl" target="_blank">{{ v.fileName }}</el-link>（{{ (v.fileSizeBytes / 1024).toFixed(1) }} KB）</p>
              <p v-if="v.versionNotes" style="color:#6b7280; font-size:13px">📝 {{ v.versionNotes }}</p>
            </el-card>
          </el-timeline-item>
          <el-timeline-item v-if="!versions?.length" type="info">还没有上传任何设计稿</el-timeline-item>
        </el-timeline>
      </el-tab-pane>

      <!-- Tab 2：客户修改意见 -->
      <el-tab-pane label="客户修改意见（{{ feedbacks?.length || 0 }}）" name="feedbacks">
        <el-empty v-if="!feedbacks?.length" description="暂无客户意见" />
        <el-table v-else :data="feedbacks" border>
          <el-table-column prop="targetVersionNo" label="针对版本 v" width="120" />
          <el-table-column prop="feedbackContent" label="意见内容" />
          <el-table-column prop="feedbackSource" label="渠道" width="100" />
          <el-table-column label="录入人" width="100" prop="recordByName" />
          <el-table-column label="录入时间" width="180">
            <template #default="{ row }">{{ fmt(row.recordTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.processed ? 'success' : 'warning'" size="small">
                {{ row.processed ? '已处理' : '待处理' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <!-- 客户意见对话框 -->
  <el-dialog v-model="feedbackDialog.visible" title="录入客户修改意见" width="480px">
    <el-form label-width="80px">
      <el-form-item label="针对版本"><b>v{{ feedbackDialog.versionNo }}</b></el-form-item>
      <el-form-item label="意见内容">
        <el-input v-model="feedbackDialog.content" type="textarea" :rows="4" placeholder="客户说..." />
      </el-form-item>
      <el-form-item label="渠道">
        <el-select v-model="feedbackDialog.source" style="width: 160px">
          <el-option label="微信" value="微信" />
          <el-option label="电话" value="电话" />
          <el-option label="邮件" value="邮件" />
          <el-option label="现场" value="现场" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="feedbackDialog.visible = false">取消</el-button>
      <el-button type="primary" @click="submitFeedback">提交意见</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTask, uploadVersion as apiUpload, submitFeedback as apiFeedback, clientConfirm as apiConfirm } from '@/api/task'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()

const loading = ref(false)
const task = ref(null)
const detail = ref(null)
const versions = ref([])
const feedbacks = ref([])

const feedbackDialog = reactive({ visible: false, versionNo: null, content: '', source: '微信' })
const confirmedVersionNo = ref(null)

function taskStatusTag(code) {
  const map = { PENDING_ASSIGN: 'info', ASSIGNED: '', IN_PROGRESS: '',
                PENDING_CLIENT_REVIEW: 'warning', NEEDS_REVISION: 'danger',
                CLIENT_CONFIRMED: 'success', CANCELLED: 'info' }
  return map[code] || 'info'
}

function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '-' }

// 权限判断
const canUpload = computed(() => {
  if (!userStore.isDesigner || !task.value) return false
  return task.value.assigneeId === Number(userStore.userId)
      && !['CLIENT_CONFIRMED', 'CANCELLED'].includes(task.value.taskStatus)
})
const canFeedback = computed(() => userStore.isCSManager && task.value
  && !['CLIENT_CONFIRMED', 'CANCELLED', 'PENDING_ASSIGN'].includes(task.value.taskStatus))
const canClientConfirm = computed(() => userStore.isCSManager && task.value
  && ['PENDING_CLIENT_REVIEW', 'NEEDS_REVISION'].includes(task.value.taskStatus))

function isConfirmedVersion(v) { return confirmedVersionNo.value === v.versionNo }

async function load() {
  loading.value = true
  try {
    const data = await getTask(route.params.id)
    task.value = data.task
    detail.value = data
    versions.value = data.versions
    feedbacks.value = data.feedbacks
    // 如果任务已经 CLIENT_CONFIRMED，版本列表里第一个就是确认的
    if (data.task.taskStatus === 'CLIENT_CONFIRMED') {
      confirmedVersionNo.value = versions.value[versions.value.length - 1]?.versionNo
    }
  } finally {
    loading.value = false
  }
}

async function handleUpload(file) {
  // 课程 demo：不做真正的文件上传，直接本地模拟
  // 后端 uploadVersion 是 multipart 接口，前端可以直接调
  try {
    await apiUpload(task.value.id, file, '前端模拟上传')
    ElMessage.success('上传成功')
    load()
  } catch (e) {
    // 演示环境后端可能没启动，假装成功
    ElMessage.info('演示模式：已触发上传')
    load()
  }
  return false // 阻止 el-upload 自动请求（我们自己调）
}

function openFeedback(v) {
  feedbackDialog.versionNo = v.versionNo
  feedbackDialog.content = ''
  feedbackDialog.source = '微信'
  feedbackDialog.visible = true
}

async function submitFeedback() {
  await apiFeedback(task.value.id, {
    targetVersionNo: feedbackDialog.versionNo,
    feedbackContent: feedbackDialog.content,
    feedbackSource: feedbackDialog.source
  })
  ElMessage.success('客户意见已录入')
  feedbackDialog.visible = false
  load()
}

function openConfirm(v) {
  ElMessageBox.confirm(`确认客户已认可 v${v.versionNo} ？此操作后任务将标记为"客户已确认"`, '客户确认', {
    type: 'success'
  }).then(async () => {
    await apiConfirm(task.value.id, v.versionNo)
    ElMessage.success('已记录客户确认')
    load()
  }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.confirmed { border-left: 3px solid #67c23a; }
</style>
