import request from '@/utils/request'

export function queryInventory(roomTypeId, startDate, endDate) {
  return request.get('/inventory/query', { params: { roomTypeId, startDate, endDate } })
}

export function lockInventory(data) {
  return request.post('/inventory/lock', data)
}

export function releaseLock(lockId) {
  return request.post(`/inventory/lock/${lockId}/release`)
}

export function adjustInventory(roomTypeId, date, newTotal) {
  return request.post('/inventory/adjust', null, { params: { roomTypeId, date, newTotal } })
}

export function adjustPrice(roomTypeId, date, price) {
  return request.post('/inventory/price', null, { params: { roomTypeId, date, price } })
}

export function generateInventory(params) {
  return request.post('/inventory/generate', null, { params })
}
