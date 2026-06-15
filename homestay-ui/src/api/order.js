import request from '@/utils/request'

export function createOrder(data) {
  return request.post('/orders', data)
}

export function getOrder(id) {
  return request.get(`/orders/${id}`)
}

export function confirmOrder(id) {
  return request.post(`/orders/${id}/confirm`)
}

export function cancelOrder(id, reason = '') {
  return request.post(`/orders/${id}/cancel`, null, { params: { reason } })
}

export function listOrders(startDate, endDate, status) {
  return request.get('/orders/list', { params: { startDate, endDate, status } })
}
