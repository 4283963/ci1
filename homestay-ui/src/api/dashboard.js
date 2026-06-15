import request from '@/utils/request'

export function getRealtimeRoomStatus() {
  return request.get('/dashboard/realtime')
}

export function getChannelOrderDistribution(startDate, endDate) {
  return request.get('/dashboard/channel-distribution', {
    params: { startDate, endDate }
  })
}

export function getRoomTypeHeatmap(startDate, endDate) {
  return request.get('/dashboard/heatmap', {
    params: { startDate, endDate }
  })
}
