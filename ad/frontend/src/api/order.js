import request from './request'

// 订单列表（带完整筛选）
export function listOrders(params) {
  return request.get('/orders', { params })
}

// 订单详情
export function getOrder(id) {
  return request.get(`/orders/${id}`)
}

// 创建订单
export function createOrder(payload) {
  return request.post('/orders', payload)
}

// 负责人审核通过
export function confirmOrder(id, notes) {
  return request.post(`/orders/${id}/confirm`, notes ? { reviewNotes: notes } : {})
}

// 发起变更
export function addOrderChange(id, payload) {
  return request.post(`/orders/${id}/changes`, payload)
}

// 查变更记录
export function listOrderChanges(id) {
  return request.get(`/orders/${id}/changes`)
}

// 完成交付
export function completeOrder(id) {
  return request.post(`/orders/${id}/complete`)
}

// 取消订单
export function cancelOrder(id, reason) {
  return request.post(`/orders/${id}/cancel`, { reason })
}

// 客户创建
export function createCustomer(payload) {
  return request.post('/customers', payload)
}
