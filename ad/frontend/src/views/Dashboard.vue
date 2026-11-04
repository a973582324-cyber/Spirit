<template>
  <div class="dashboard">
    <!-- 欢迎语 -->
    <el-card class="welcome">
      <h3>你好，{{ userStore.realName }} 👋</h3>
      <p>角色：{{ userStore.roleLabel }} · 请选择以下快捷入口开始工作</p>
    </el-card>

    <!-- 三角色快捷入口（动态 span，v-if 后均分宽度不留空） -->
    <el-row :gutter="16">
      <el-col v-if="userStore.isCSManager" :span="colSpan">
        <el-card class="quick-card" shadow="hover" @click="$router.push('/orders/create')">
          <el-icon class="icon primary"><Plus /></el-icon>
          <h4>新建订单</h4>
          <p>登记客户需求和物料清单</p>
        </el-card>
      </el-col>
      <el-col v-if="userStore.isLeader" :span="colSpan">
        <el-card class="quick-card" shadow="hover" @click="$router.push('/tasks')">
          <el-icon class="icon success"><Tickets /></el-icon>
          <h4>分派待办</h4>
          <p>审核订单 + 分派物料任务</p>
        </el-card>
      </el-col>
      <el-col v-if="true" :span="colSpan">
        <el-card class="quick-card" shadow="hover" @click="$router.push('/tasks')">
          <el-icon class="icon warning"><EditPen /></el-icon>
          <h4>我的任务</h4>
          <p>查看待处理 / 逾期任务</p>
        </el-card>
      </el-col>
      <el-col v-if="true" :span="colSpan">
        <el-card class="quick-card" shadow="hover" @click="$router.push('/orders')">
          <el-icon class="icon info"><Document /></el-icon>
          <h4>订单概览</h4>
          <p>查看所有订单状态</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日关注 -->
    <el-card class="focus" header="今日关注">
      <el-alert v-if="userStore.isCSManager" title="客户经理关注：PENDING_CLIENT_REVIEW 的任务需要及时录入客户意见" type="warning" :closable="false" />
      <el-alert v-if="userStore.isLeader" title="负责人关注：PENDING_ASSIGN 的任务需要尽快分派给设计人员" type="warning" :closable="false" />
      <el-alert v-if="userStore.isDesigner" title="设计人员关注：NEEDS_REVISION 的任务需要响应客户意见提交新版本" type="warning" :closable="false" />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { Plus, Tickets, EditPen, Document } from '@element-plus/icons-vue'

const userStore = useUserStore()

// 当前角色可见几张卡片，每张均分 24 栅格
// CS_MANAGER: 新建订单 + 我的任务 + 订单概览 = 3 → span=8
// BIZ_LEADER: 分派待办 + 我的任务 + 订单概览 = 3 → span=8
// DESIGNER:   我的任务 + 订单概览 = 2 → span=12
const visibleCount = computed(() => {
  let n = 2  // 我的任务 + 订单概览（始终显示）
  if (userStore.isCSManager) n++   // +1 新建订单
  if (userStore.isLeader)   n++   // +1 分派待办
  return n
})
const colSpan = computed(() => Math.floor(24 / visibleCount.value))
</script>

<style scoped>
.welcome { margin-bottom: 16px; }
.welcome h3 { margin: 0 0 4px; }
.quick-card { cursor: pointer; text-align: center; margin-bottom: 16px; }
.quick-card:hover { transform: translateY(-2px); }
.icon { font-size: 36px; margin-bottom: 8px; }
.icon.primary { color: #409eff; }
.icon.success { color: #67c23a; }
.icon.warning { color: #e6a23c; }
.icon.info { color: #909399; }
.focus { margin-top: 16px; }
</style>
