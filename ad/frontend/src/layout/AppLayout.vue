<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">🎨 物料订单系统</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1e293b"
        text-color="#cbd5e1"
        active-text-color="#fbbf24">
        <el-menu-item
          v-for="item in currentMenu"
          :key="item.path"
          :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧：Header + Main -->
    <el-container>
      <el-header class="header">
        <div class="title">{{ currentTitle }}</div>
        <div class="user-area">
          <el-tag :type="roleTagType" size="small">{{ userStore.roleLabel }}</el-tag>
          <span class="name">{{ userStore.realName }}</span>
          <el-button size="small" text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { menuByRole } from '@/router/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentMenu = computed(() => menuByRole[userStore.role] || [])

const currentTitle = computed(() => route.meta?.title || '工作台')

const roleTagType = computed(() => ({
  CS_MANAGER: 'primary',
  BIZ_LEADER: 'success',
  DESIGNER: 'warning'
}[userStore.role] || 'info'))

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout { height: 100vh; }
.sidebar { background: #1e293b; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  color: #fff; font-size: 16px; font-weight: bold; letter-spacing: 2px;
  border-bottom: 1px solid #334155;
}
.header {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e5e7eb; background: #fff; padding: 0 20px;
}
.title { font-size: 16px; font-weight: 600; color: #111827; }
.user-area { display: flex; align-items: center; gap: 10px; }
.name { font-size: 13px; color: #6b7280; }
.main { background: #f3f4f6; padding: 20px; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
