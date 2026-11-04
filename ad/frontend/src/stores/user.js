import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login } from '@/api/auth'

/**
 * 当前登录用户 Store
 * —— 课程 demo 阶段：角色信息存 localStorage，后端接口靠 Header X-User-Id 做鉴权
 */
export const useUserStore = defineStore('user', () => {
  const userId = ref(null)
  const username = ref('')
  const realName = ref('')
  const role = ref('')          // CS_MANAGER / BIZ_LEADER / DESIGNER
  const roleLabel = ref('')

  // 登录
  async function doLogin(payload) {
    const data = await login(payload)
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    role.value = data.role
    roleLabel.value = data.roleLabel
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('userInfo', JSON.stringify(data))
    return data
  }

  // 退出
  function logout() {
    userId.value = null
    username.value = ''
    realName.value = ''
    role.value = ''
    roleLabel.value = ''
    localStorage.removeItem('userId')
    localStorage.removeItem('userInfo')
  }

  // 从 localStorage 恢复（刷新不丢登录态）
  function restore() {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      try {
        const data = JSON.parse(raw)
        userId.value = data.userId
        username.value = data.username
        realName.value = data.realName
        role.value = data.role
        roleLabel.value = data.roleLabel
      } catch { /* ignore */ }
    }
  }

  // 角色快捷判断
  const isCSManager = computed(() => role.value === 'CS_MANAGER')
  const isLeader = computed(() => role.value === 'BIZ_LEADER')
  const isDesigner = computed(() => role.value === 'DESIGNER')
  const isLoggedIn = computed(() => !!userId.value)

  return {
    userId, username, realName, role, roleLabel,
    doLogin, logout, restore,
    isCSManager, isLeader, isDesigner, isLoggedIn
  }
})
