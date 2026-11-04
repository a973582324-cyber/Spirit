import request from './request'

// 登录
export function login(payload) {
  return request.post('/auth/login', payload)
}
