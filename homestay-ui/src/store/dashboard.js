import { defineStore } from 'pinia'
import { defineStore as _defineStore } from 'pinia'
import {
  getRealtimeRoomStatus,
  getChannelOrderDistribution,
  getRoomTypeHeatmap
} from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    realtimeData: [],
    channelDistribution: [],
    heatmapData: [],
    lastUpdateTime: null,
    wsConnected: false
  }),
  getters: {
    totalRooms: (state) => {
      return state.realtimeData.reduce((sum, r) => sum + (r.totalRooms || 0), 0)
    },
    bookedRooms: (state) => {
      return state.realtimeData.reduce((sum, r) => sum + (r.bookedRooms || 0), 0)
    },
    availableRooms: (state) => {
      return state.realtimeData.reduce((sum, r) => sum + (r.availableRooms || 0), 0)
    },
    avgBookingRate: (state) => {
      if (!state.realtimeData.length) return 0
      const total = state.realtimeData.reduce((s, r) => s + (r.totalRooms || 0), 0)
      const booked = state.realtimeData.reduce((s, r) => s + (r.bookedRooms || 0), 0)
      return total > 0 ? ((booked / total) * 100).toFixed(1) : 0
    },
    totalRevenue: (state) => {
      return state.channelDistribution.reduce((s, r) => s + (parseFloat(r.totalRevenue) || 0), 0)
    }
  },
  actions: {
    async fetchRealtimeData() {
      try {
        const res = await getRealtimeRoomStatus()
        if (res.code === 200) {
          this.realtimeData = res.data || []
          this.lastUpdateTime = new Date().toLocaleString('zh-CN')
        }
      } catch (e) {
        console.error('获取实时房态失败', e)
      }
    },
    async fetchChannelDistribution(startDate, endDate) {
      try {
        const res = await getChannelOrderDistribution(startDate, endDate)
        if (res.code === 200) {
          this.channelDistribution = res.data || []
        }
      } catch (e) {
        console.error('获取渠道分布失败', e)
      }
    },
    async fetchHeatmap(startDate, endDate) {
      try {
        const res = await getRoomTypeHeatmap(startDate, endDate)
        if (res.code === 200) {
          this.heatmapData = res.data || []
        }
      } catch (e) {
        console.error('获取热力图数据失败', e)
      }
    },
    setRealtimeData(data) {
      this.realtimeData = data || []
      this.lastUpdateTime = new Date().toLocaleString('zh-CN')
    },
    setWsConnected(connected) {
      this.wsConnected = connected
    }
  }
})
