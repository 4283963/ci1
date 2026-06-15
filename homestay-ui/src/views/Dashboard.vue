<template>
  <div class="dashboard-page">
    <header class="page-header">
      <div class="header-left">
        <h1 class="app-title">
          <el-icon :size="28"><Hotel /></el-icon>
          <span>多渠道房态管理大屏</span>
        </h1>
        <div class="header-subtitle">Homestay Channel Management Dashboard</div>
      </div>
      <div class="header-center">
        <el-radio-group v-model="dateRange" size="large" @change="onDateChange">
          <el-radio-button value="today">今日</el-radio-button>
          <el-radio-button value="7d">近7天</el-radio-button>
          <el-radio-button value="30d" class="active">近30天</el-radio-button>
          <el-radio-button value="90d">近90天</el-radio-button>
        </el-radio-group>
      </div>
      <div class="header-right">
        <div class="datetime">
          <el-icon><Timer /></el-icon>
          <span>{{ currentTime }}</span>
        </div>
        <el-button type="primary" :icon="Refresh" circle @click="fetchAll" />
      </div>
    </header>

    <section class="stats-section">
      <StatCard
        label="总房量"
        :value="store.totalRooms"
        unit="间"
        icon="House"
        color="#00d4ff"
      />
      <StatCard
        label="已预订"
        :value="store.bookedRooms"
        unit="间"
        icon="Calendar"
        color="#0066ff"
      />
      <StatCard
        label="可用房"
        :value="store.availableRooms"
        unit="间"
        icon="SuccessFilled"
        color="#00ffa3"
      />
      <StatCard
        label="平均出租率"
        :value="store.avgBookingRate"
        unit="%"
        icon="DataLine"
        color="#ffb547"
      />
      <StatCard
        label="累计收入"
        :value="totalRevenueDisplay"
        unit="元"
        icon="Wallet"
        color="#ff4d4f"
      />
    </section>

    <section class="charts-section">
      <div class="left-col">
        <div class="row-1">
          <RealtimeRoomPanel
            :table-data="store.realtimeData"
            :last-update-time="store.lastUpdateTime"
            :ws-connected="store.wsConnected"
          />
        </div>
        <div class="row-2">
          <HeatmapChart
            title="房型热度热力图"
            :heatmap-data="store.heatmapData"
            :loading="store.heatmapLoading"
            :has-error="store.heatmapError"
            @retry="retryHeatmap"
          />
        </div>
      </div>
      <div class="center-col">
        <RevenuePanel
          title="动态收益预测"
          :daily-forecast="store.revenueForecast"
          :channel-breakdown="store.revenueChannelBreakdown"
          :summary="store.revenueSummary"
          :loading="store.revenueLoading"
          :has-error="store.revenueError"
          @retry="retryRevenue"
        />
      </div>
      <div class="right-col">
        <div class="row-1">
          <ChannelChart
            title="渠道销售对比"
            :distribution-data="store.channelDistribution"
            type="bar"
          />
        </div>
        <div class="row-2">
          <ChannelChart
            title="渠道订单占比"
            :distribution-data="store.channelDistribution"
            type="pie"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, onActivated } from 'vue'
import { useDashboardStore } from '@/store/dashboard'
import { connectWebSocket, disconnectWebSocket } from '@/utils/websocket'
import dayjs from 'dayjs'
import StatCard from '@/components/StatCard.vue'
import RealtimeRoomPanel from '@/components/RealtimeRoomPanel.vue'
import ChannelChart from '@/components/ChannelChart.vue'
import HeatmapChart from '@/components/HeatmapChart.vue'
import RevenuePanel from '@/components/RevenuePanel.vue'
import { Refresh } from '@element-plus/icons-vue'

const store = useDashboardStore()
const dateRange = ref('30d')
const currentTime = ref('')

let timeTimer = null
let pollTimer = null

const totalRevenueDisplay = computed(() => {
  const v = store.totalRevenue
  if (v >= 10000) return (v / 10000).toFixed(2) + '万'
  return Math.round(v).toLocaleString()
})

const onDateChange = () => {
  fetchAll()
}

const retryHeatmap = () => {
  let start, end
  const today = dayjs()
  switch (dateRange.value) {
    case 'today': start = today.format('YYYY-MM-DD'); end = today.format('YYYY-MM-DD'); break
    case '7d': start = today.subtract(7, 'day').format('YYYY-MM-DD'); end = today.format('YYYY-MM-DD'); break
    case '90d': start = today.subtract(90, 'day').format('YYYY-MM-DD'); end = today.add(30, 'day').format('YYYY-MM-DD'); break
    default: start = today.subtract(30, 'day').format('YYYY-MM-DD'); end = today.add(30, 'day').format('YYYY-MM-DD')
  }
  store.fetchHeatmap(start, end)
}

const retryRevenue = () => {
  const today = dayjs()
  const start = today.format('YYYY-MM-DD')
  const end = today.add(30, 'day').format('YYYY-MM-DD')
  store.fetchRevenueAnalysis(start, end)
}

const fetchAll = async () => {
  let start, end
  const today = dayjs()
  switch (dateRange.value) {
    case 'today': start = today.format('YYYY-MM-DD'); end = today.format('YYYY-MM-DD'); break
    case '7d': start = today.subtract(7, 'day').format('YYYY-MM-DD'); end = today.format('YYYY-MM-DD'); break
    case '90d': start = today.subtract(90, 'day').format('YYYY-MM-DD'); end = today.add(30, 'day').format('YYYY-MM-DD'); break
    default: start = today.subtract(30, 'day').format('YYYY-MM-DD'); end = today.add(30, 'day').format('YYYY-MM-DD')
  }
  const revenueStart = today.format('YYYY-MM-DD')
  const revenueEnd = today.add(30, 'day').format('YYYY-MM-DD')
  await Promise.all([
    store.fetchRealtimeData(),
    store.fetchChannelDistribution(start, end),
    store.fetchHeatmap(start, end),
    store.fetchRevenueAnalysis(revenueStart, revenueEnd)
  ])
}

const updateTime = () => {
  currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  fetchAll()
  pollTimer = setInterval(() => store.fetchRealtimeData(), 60000)
  setTimeout(connectWebSocket, 1500)
})

onBeforeUnmount(() => {
  clearInterval(timeTimer)
  clearInterval(pollTimer)
  disconnectWebSocket()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px 24px;
  gap: 16px;
  overflow: hidden;
}

.page-header {
  @include flex-between;
  padding: 0 8px;

  .header-left {
    .app-title {
      font-size: 26px;
      font-weight: 700;
      letter-spacing: 2px;
      display: flex;
      align-items: center;
      gap: 10px;
      background: linear-gradient(90deg, #00d4ff 0%, #0066ff 50%, #00ffa3 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .header-subtitle {
      font-size: 12px;
      color: $text-secondary;
      letter-spacing: 3px;
      margin-top: 4px;
      opacity: 0.7;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .datetime {
      font-family: 'DIN', sans-serif;
      font-size: 16px;
      font-weight: 600;
      color: $primary-color;
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.charts-section {
  flex: 1;
  display: grid;
  grid-template-columns: 1.2fr 1.4fr 0.9fr;
  gap: 16px;
  min-height: 0;

  .left-col, .center-col, .right-col {
    display: flex;
    flex-direction: column;
    gap: 16px;
    min-height: 0;
  }

  .left-col {
    .row-1 { flex: 1.1; min-height: 0; }
    .row-2 { flex: 1; min-height: 0; }
  }

  .center-col {
    min-height: 0;
  }

  .right-col {
    .row-1, .row-2 { flex: 1; min-height: 0; }
  }
}
</style>
