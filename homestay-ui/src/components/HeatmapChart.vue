<template>
  <div class="heatmap-panel">
    <div class="panel-header">
      <div class="card-title">{{ title }}</div>
      <div class="tab-switch">
        <el-radio-group v-model="viewMode" size="small" @change="renderChart">
          <el-radio-button value="date">按日期</el-radio-button>
          <el-radio-button value="hour">按时段</el-radio-button>
          <el-radio-button value="weekday">按星期</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="panel-body">
      <div ref="chartRef" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: { type: String, default: '房型热度热力图' },
  heatmapData: { type: Array, default: () => [] }
})

const chartRef = ref(null)
const viewMode = ref('date')
let chartInstance = null

const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const hourNames = Array.from({ length: 24 }, (_, i) => `${i.toString().padStart(2, '0')}:00`)

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
  }

  const roomTypes = [...new Set(props.heatmapData.map(d => d.typeName))]
  let xCategories = []
  let dataMap = {}
  let maxValue = 0

  if (viewMode.value === 'date') {
    xCategories = [...new Set(props.heatmapData.map(d => d.stayDate))].sort()
  } else if (viewMode.value === 'hour') {
    xCategories = hourNames
  } else {
    xCategories = weekdayNames
  }

  props.heatmapData.forEach(d => {
    let xKey
    if (viewMode.value === 'date') xKey = d.stayDate
    else if (viewMode.value === 'hour') xKey = hourNames[d.bookHour || 12]
    else xKey = weekdayNames[d.dayOfWeek || 0]

    const key = `${d.typeName}__${xKey}`
    if (!dataMap[key]) {
      dataMap[key] = 0
    }
    dataMap[key] += d.orderCount || 0
    if (dataMap[key] > maxValue) maxValue = dataMap[key]
  })

  const seriesData = []
  roomTypes.forEach((rt, yIdx) => {
    xCategories.forEach((xc, xIdx) => {
      const val = dataMap[`${rt}__${xc}`] || 0
      seriesData.push([xIdx, yIdx, val])
    })
  })

  const option = {
    tooltip: {
      position: 'top',
      formatter: (p) => {
        const [x, y, v] = p.data
        return `<div style="padding:4px 8px;">
          <div style="color:#a3b5d6;font-size:12px;">${roomTypes[y]} / ${xCategories[x]}</div>
          <div style="color:#00d4ff;font-weight:600;font-size:16px;">${v} 单</div>
        </div>`
      },
      textStyle: { color: '#fff' },
      backgroundColor: 'rgba(16,38,89,0.95)',
      borderColor: 'rgba(0,212,255,0.3)'
    },
    grid: {
      left: '12%',
      right: '5%',
      bottom: '15%',
      top: '5%'
    },
    xAxis: {
      type: 'category',
      data: xCategories,
      splitArea: { show: true, areaStyle: { color: ['rgba(0,0,0,0.02)', 'rgba(0,212,255,0.03)'] } },
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#a3b5d6', fontSize: 11, rotate: viewMode.value === 'date' ? 45 : 0 }
    },
    yAxis: {
      type: 'category',
      data: roomTypes,
      splitArea: { show: true },
      axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
      axisLabel: { color: '#a3b5d6', fontSize: 11 }
    },
    visualMap: {
      min: 0,
      max: Math.max(1, maxValue),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      textStyle: { color: '#a3b5d6', fontSize: 11 },
      inRange: {
        color: ['#0a1229', '#102659', '#0066ff', '#00d4ff', '#ffb547', '#ff4d4f']
      }
    },
    series: [{
      name: '订单数',
      type: 'heatmap',
      data: seriesData,
      label: {
        show: true,
        color: '#fff',
        fontSize: 10,
        formatter: (p) => p.data[2] > 0 ? p.data[2] : ''
      },
      itemStyle: {
        borderRadius: 4,
        borderWidth: 1,
        borderColor: 'rgba(11,24,54,0.6)'
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0,212,255,0.5)'
        }
      }
    }]
  }

  chartInstance.setOption(option, true)
}

const handleResize = () => chartInstance && chartInstance.resize()

watch(() => props.heatmapData, renderChart, { deep: true })

onMounted(() => setTimeout(renderChart, 100))

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
})
</script>

<style lang="scss" scoped>
.heatmap-panel {
  height: 100%;
  @include card-style;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid $border-color;
    @include flex-between;
  }

  .panel-body {
    flex: 1;
    padding: 8px;
  }

  .chart-container {
    width: 100%;
    height: 100%;
    min-height: 320px;
  }
}
</style>
