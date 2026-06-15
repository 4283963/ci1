<template>
  <div class="revenue-panel">
    <div class="panel-header">
      <div class="card-title">{{ title }}</div>
      <div class="header-actions">
        <el-radio-group v-model="chartMode" size="small" @change="renderChart">
          <el-radio-button value="trend">收入趋势</el-radio-button>
          <el-radio-button value="channel">渠道对比</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="summary-row" v-if="summary">
      <div class="summary-item">
        <span class="label">总收入</span>
        <span class="value gross">{{ formatMoney(summary.totalGrossRevenue) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">平台费用</span>
        <span class="value fee">-{{ formatMoney(summary.totalPlatformFee) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">净利润</span>
        <span class="value net">{{ formatMoney(summary.totalNetProfit) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">净利率</span>
        <span class="value rate">{{ summary.avgNetRate }}%</span>
      </div>
    </div>
    <div class="panel-body">
      <div v-if="loading" class="status-wrap">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span class="status-text">收益数据加载中...</span>
      </div>
      <div v-else-if="hasError" class="status-wrap">
        <el-icon class="error-icon"><Warning /></el-icon>
        <span class="status-text">数据加载失败</span>
        <el-button size="small" type="primary" plain @click="$emit('retry')">重试</el-button>
      </div>
      <div v-else-if="!dailyForecast || dailyForecast.length === 0" class="status-wrap">
        <el-icon class="empty-icon"><DataBoard /></el-icon>
        <span class="status-text">暂无收益数据</span>
      </div>
      <div v-else ref="chartRef" class="chart-container"></div>
    </div>
    <div class="channel-table" v-if="chartMode === 'channel' && channelBreakdown && channelBreakdown.length > 0">
      <el-table :data="channelBreakdown" size="small" stripe :header-cell-style="{background:'#0c1e4a',color:'#a3b5d6',borderColor:'#1a3278'}" :cell-style="{background:'#091535',color:'#e0e6f0',borderColor:'#1a3278'}">
        <el-table-column prop="channelName" label="渠道" width="100" />
        <el-table-column prop="grossRevenue" label="总收入" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.grossRevenue) }}</template>
        </el-table-column>
        <el-table-column prop="platformFee" label="平台费" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.platformFee) }}</template>
        </el-table-column>
        <el-table-column prop="netProfit" label="净利润" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.netProfit > 0 ? 'text-green' : 'text-red'">{{ formatMoney(row.netProfit) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="netRate" label="净利率" width="80" align="right">
          <template #default="{ row }">
            <span :class="row.netRate > 50 ? 'text-green' : 'text-yellow'">{{ row.netRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="利润排名" width="80" align="center">
          <template #default="{ $index }">
            <el-tag :type="$index === 0 ? 'danger' : $index === 1 ? 'warning' : 'info'" size="small" effect="dark">
              {{ $index + 1 }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Loading, Warning, DataBoard } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '动态收益预测' },
  dailyForecast: { type: Array, default: () => [] },
  channelBreakdown: { type: Array, default: () => [] },
  summary: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  hasError: { type: Boolean, default: false }
})

defineEmits(['retry'])

const chartRef = ref(null)
const chartMode = ref('trend')
let chartInstance = null

const formatMoney = (val) => {
  if (val == null) return '¥0'
  const num = parseFloat(val)
  if (isNaN(num)) return '¥0'
  if (num >= 10000) return '¥' + (num / 10000).toFixed(2) + '万'
  return '¥' + num.toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const renderChart = async () => {
  if (!chartRef.value) return
  if (!props.dailyForecast || props.dailyForecast.length === 0) {
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    return
  }

  await nextTick()
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
  }

  try {
    if (chartMode.value === 'trend') {
      renderTrendChart()
    } else {
      renderChannelChart()
    }
  } catch (e) {
    console.error('[RevenuePanel] 渲染失败:', e)
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  }
}

const renderTrendChart = () => {
  const dates = props.dailyForecast.map(d => d.date)
  const grossData = props.dailyForecast.map(d => parseFloat(d.grossRevenue) || 0)
  const feeData = props.dailyForecast.map(d => parseFloat(d.platformFee) || 0)
  const netData = props.dailyForecast.map(d => parseFloat(d.netProfit) || 0)

  const today = new Date().toISOString().slice(0, 10)
  const markLineXIndex = dates.indexOf(today)

  const option = {
    tooltip: {
      trigger: 'axis',
      textStyle: { color: '#fff', fontSize: 12 },
      backgroundColor: 'rgba(16,38,89,0.95)',
      borderColor: 'rgba(0,212,255,0.3)',
      formatter: (params) => {
        let html = `<div style="padding:4px 8px"><div style="color:#a3b5d6;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach(p => {
          html += `<div style="display:flex;justify-content:space-between;gap:16px"><span>${p.marker} ${p.seriesName}</span><span style="font-weight:600">¥${p.value.toFixed(0)}</span></div>`
        })
        html += '</div>'
        return html
      }
    },
    legend: {
      data: ['总收入', '平台费用', '净利润'],
      textStyle: { color: '#a3b5d6', fontSize: 12 },
      top: 5,
      right: 10
    },
    grid: { left: '8%', right: '3%', bottom: '12%', top: '18%' },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#a3b5d6', fontSize: 10, rotate: 45 },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#a3b5d6', fontSize: 10, formatter: v => v >= 10000 ? (v / 10000).toFixed(0) + '万' : v },
      splitLine: { lineStyle: { color: 'rgba(0,212,255,0.08)' } }
    },
    series: [
      {
        name: '总收入',
        type: 'line',
        data: grossData,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2, color: '#00d4ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,212,255,0.3)' },
            { offset: 1, color: 'rgba(0,212,255,0.02)' }
          ])
        }
      },
      {
        name: '平台费用',
        type: 'line',
        data: feeData,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2, color: '#ff4d4f', type: 'dashed' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,77,79,0.15)' },
            { offset: 1, color: 'rgba(255,77,79,0.02)' }
          ])
        }
      },
      {
        name: '净利润',
        type: 'line',
        data: netData,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3, color: '#00ffa3' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,255,163,0.25)' },
            { offset: 1, color: 'rgba(0,255,163,0.02)' }
          ])
        },
        markLine: markLineXIndex >= 0 ? {
          silent: true,
          symbol: 'none',
          lineStyle: { color: '#ffb547', type: 'solid', width: 1 },
          data: [{ xAxis: markLineXIndex, label: { show: true, formatter: '今日', color: '#ffb547', fontSize: 10 } }]
        } : undefined
      }
    ]
  }

  chartInstance.setOption(option, true)
}

const renderChannelChart = () => {
  if (!props.channelBreakdown || props.channelBreakdown.length === 0) return

  const channels = props.channelBreakdown.map(c => c.channelName)
  const grossData = props.channelBreakdown.map(c => parseFloat(c.grossRevenue) || 0)
  const feeData = props.channelBreakdown.map(c => parseFloat(c.platformFee) || 0)
  const netData = props.channelBreakdown.map(c => parseFloat(c.netProfit) || 0)
  const rateData = props.channelBreakdown.map(c => parseFloat(c.netRate) || 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      textStyle: { color: '#fff', fontSize: 12 },
      backgroundColor: 'rgba(16,38,89,0.95)',
      borderColor: 'rgba(0,212,255,0.3)',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['总收入', '净利润', '平台费', '净利率'],
      textStyle: { color: '#a3b5d6', fontSize: 12 },
      top: 5,
      right: 10
    },
    grid: [
      { left: '8%', right: '12%', bottom: '12%', top: '18%' }
    ],
    xAxis: {
      type: 'category',
      data: channels,
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#a3b5d6', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
        axisLabel: { color: '#a3b5d6', fontSize: 10, formatter: v => v >= 10000 ? (v / 10000).toFixed(0) + '万' : v },
        splitLine: { lineStyle: { color: 'rgba(0,212,255,0.08)' } }
      },
      {
        type: 'value',
        axisLine: { lineStyle: { color: 'rgba(255,181,71,0.3)' } },
        axisLabel: { color: '#ffb547', fontSize: 10, formatter: v => v + '%' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '总收入',
        type: 'bar',
        data: grossData,
        barWidth: '20%',
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#00d4ff' }, { offset: 1, color: '#0066ff' }]), borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '净利润',
        type: 'bar',
        data: netData,
        barWidth: '20%',
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#00ffa3' }, { offset: 1, color: '#00cc82' }]), borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '平台费',
        type: 'bar',
        data: feeData,
        barWidth: '20%',
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#ff4d4f' }, { offset: 1, color: '#cc3d40' }]), borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '净利率',
        type: 'line',
        yAxisIndex: 1,
        data: rateData,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 2, color: '#ffb547' },
        itemStyle: { color: '#ffb547' },
        label: { show: true, formatter: '{c}%', color: '#ffb547', fontSize: 11, fontWeight: 600 }
      }
    ]
  }

  chartInstance.setOption(option, true)
}

const handleResize = () => chartInstance && chartInstance.resize()

watch(() => props.dailyForecast, () => {
  if (!props.loading && !props.hasError) renderChart()
}, { deep: true })

watch(() => props.channelBreakdown, () => {
  if (chartMode.value === 'channel' && !props.loading) renderChart()
}, { deep: true })

watch(() => props.loading, (newVal) => {
  if (!newVal && !props.hasError) nextTick(renderChart)
})

onMounted(() => {
  if (!props.loading && !props.hasError && props.dailyForecast && props.dailyForecast.length > 0) {
    setTimeout(renderChart, 100)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
})
</script>

<style lang="scss" scoped>
.revenue-panel {
  height: 100%;
  @include card-style;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid $border-color;
    @include flex-between;
  }

  .summary-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    padding: 12px 20px;
    border-bottom: 1px solid $border-color;

    .summary-item {
      text-align: center;
      .label {
        display: block;
        font-size: 11px;
        color: #a3b5d6;
        margin-bottom: 4px;
      }
      .value {
        font-family: 'DIN', sans-serif;
        font-size: 18px;
        font-weight: 700;
        &.gross { color: #00d4ff; }
        &.fee { color: #ff4d4f; }
        &.net { color: #00ffa3; }
        &.rate { color: #ffb547; }
      }
    }
  }

  .panel-body {
    flex: 1;
    padding: 8px;
    position: relative;
  }

  .chart-container {
    width: 100%;
    height: 100%;
    min-height: 280px;
  }

  .channel-table {
    padding: 0 12px 12px;
    max-height: 220px;
    overflow-y: auto;
  }

  .status-wrap {
    width: 100%;
    height: 100%;
    min-height: 280px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: #a3b5d6;
    font-size: 14px;

    .loading-icon { font-size: 40px; color: #00d4ff; animation: spin 1s linear infinite; }
    .error-icon { font-size: 40px; color: #ff4d4f; }
    .empty-icon { font-size: 40px; color: #4d6b9e; }
    .status-text { font-size: 13px; }
  }

  .text-green { color: #00ffa3; font-weight: 600; }
  .text-red { color: #ff4d4f; font-weight: 600; }
  .text-yellow { color: #ffb547; font-weight: 600; }

  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
}
</style>
