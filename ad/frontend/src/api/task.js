import request from './request'

// 任务列表（带完整筛选 + 数据权限后端自动处理）
export function listTasks(params) {
  return request.get('/tasks', { params })
}

// 任务详情
export function getTask(id) {
  return request.get(`/tasks/${id}`)
}

// 分派单个
export function assignTask(taskId, assigneeId) {
  return request.post(`/tasks/${taskId}/assign`, { taskId, assigneeId })
}

// 批量分派
export function batchAssign(items) {
  return request.post('/tasks/batch-assign', { items })
}

// 上传设计稿（multipart）
export function uploadVersion(taskId, file, versionNotes) {
  const fd = new FormData()
  fd.append('file', file)
  if (versionNotes) fd.append('versionNotes', versionNotes)
  return request.post(`/tasks/${taskId}/versions`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 录入客户修改意见
export function submitFeedback(taskId, payload) {
  return request.post(`/tasks/${taskId}/feedback`, payload)
}

// 客户确认
export function clientConfirm(taskId, versionNo) {
  return request.post(`/tasks/${taskId}/client-confirm`, { confirmedVersionNo: versionNo })
}

// 按角色查用户（下拉框）
export function listUsersByRole(role) {
  return request.get('/users', { params: { role } })
}
