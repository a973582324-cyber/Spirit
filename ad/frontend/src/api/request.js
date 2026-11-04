import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 统一 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动加 X-User-Id Header
request.interceptors.request.use(config => {
  const userId = localStorage.getItem('userId')
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
})

// 响应拦截器：统一处理 {code, message, data} 结构
request.interceptors.response.use(
  resp => {
    const body = resp.data
    // 成功
    if (body && body.code === 200) {
      return body.data
    }
    // 业务错误（400/401/403/404/409/422/500 等）
    const httpCode = resp.status
    const msg = body?.message || '请求失败'

    if (httpCode === 401) {
      ElMessage.error('登录已失效，请重新登录')
      localStorage.removeItem('userId')
      localStorage.removeItem('userInfo')
      router.push('/login')
    } else if (httpCode === 403) {
      ElMessage.error('无权限操作：' + msg)
    } else if (httpCode === 404) {
      ElMessage.error(msg)
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(body || { message: msg })
  },
  err => {
    // 网络级错误（后端未启动/跨域等）
    const msg = err.response?.data?.message || err.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
