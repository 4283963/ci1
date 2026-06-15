import request from '@/utils/request'

export function getRevenueAnalysis(startDate, endDate) {
  return request.get('/revenue/analysis', {
    params: { startDate, endDate }
  })
}
