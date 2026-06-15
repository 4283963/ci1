import { defineStore } from 'pinia'
import { defineStore as _defineStore } from 'pinia'
import {
  getRealtimeRoomStatus,
  getChannelOrderDistribution,
  getRoomTypeHeatmap
} from '@/api/dashboard'
import { getRevenueAnalysis } from '@/api/revenue'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    realtimeData: [],
    channelDistribution: [],
    heatmapData: [],
    lastUpdateTime: null,
    wsConnected: false,
    realtimeLoading: false,
    channelLoading: false,
    heatmapLoading: false,
    heatmapError: false,
    revenueForecast: [],
    revenueChannelBreakdown: [],
    revenueSummary: null,
    revenueLoading: false,
    revenueError: false
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
      this.realtimeLoading = true
      try {
        const res = await getRealtimeRoomStatus()
        if (res.code === 200) {
          this.realtimeData = res.data || []
          this.lastUpdateTime = new Date().toLocaleString('zh-CN')
        }
      } catch (e) {
        console.error('获取实时房态失败', e)
        this.realtimeData = []
      } finally {
        this.realtimeLoading = false
      }
    },
    async fetchChannelDistribution(startDate, endDate) {
      this.channelLoading = true
      try {
        const res = await getChannelOrderDistribution(startDate, endDate)
        if (res.code === 200) {
          this.channelDistribution = res.data || []
        }
      } catch (e) {
        console.error('获取渠道分布失败', e)
        this.channelDistribution = []
      } finally {
        this.channelLoading = false
      }
    },
    async fetchHeatmap(startDate, endDate) {
      this.heatmapLoading = true
      this.heatmapError = false
      try {
        const res = await getRoomTypeHeatmap(startDate, endDate)
        if (res.code === 200) {
          this.heatmapData = res.data || []
        } else {
          this.heatmapError = true
          this.heatmapData = []
        }
      } catch (e) {
        console.error('获取热力图数据失败', e)
        this.heatmapError = true
        this.heatmapData = []
      } finally {
        this.heatmapLoading = false
      }
    },
    async fetchRevenueAnalysis(startDate, endDate) {
      this.revenueLoading = true
      this.revenueError = false
      try {
        const res = await getRevenueAnalysis(startDate, endDate)
        if (res.code === 200 && res.data) {
          this.revenueForecast = res.data.dailyForecast || []
          this.revenueChannelBreakdown = res.data.channelBreakdown || []
          this.revenueSummary = {
            totalGrossRevenue: res.data.totalGrossRevenue,
            totalPlatformFee: res.data.totalPlatformFee,
            totalNetProfit: res.data.totalNetProfit,
            avgNetRate: res.data.avgNetRate
          }
        } else {
          this.revenueError = true
          this.revenueForecast = []
          this.revenueChannelBreakdown = []
          this.revenueSummary = null
        }
      } catch (e) {
        console.error('获取收益分析数据失败', e)
        this.revenueError = true
        this.revenueForecast = []
        this.revenueChannelBreakdown = []
        this.revenueSummary = null
      } finally {
        this.revenueLoading = false
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
