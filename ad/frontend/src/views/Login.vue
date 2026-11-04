<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="brand">🎨 宣传物料订单系统</h2>
      <p class="subtitle">广告制作公司 · 前后端分离 Demo</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
      </el-form>
      <el-divider>课程演示账号</el-divider>
      <div class="demo-accounts">
        <el-tag type="primary">客户经理 chenliang / demo123</el-tag>
        <el-tag type="success">负责人 wangli / demo123</el-tag>
        <el-tag type="warning">设计师 zhangxiaoyi / demo123</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: 'chenliang', password: 'demo123' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.doLogin(form)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card { width: 400px; padding: 20px; }
.brand { text-align: center; margin-bottom: 4px; }
.subtitle { text-align: center; color: #9ca3af; font-size: 13px; margin-bottom: 20px; }
.login-btn { width: 100%; }
.demo-accounts { display: flex; flex-direction: column; gap: 8px; }
</style>
