<template>
  <div class="chart-panel">
    <div class="panel-header">
      <div class="card-title">{{ title }}</div>
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
  title: { type: String, default: '渠道订单分布' },
  distributionData: { type: Array, default: () => [] },
  type: { type: String, default: 'bar' }
})

const chartRef = ref(null)
let chartInstance = null

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
  }

  const channelMap = {}
  props.distributionData.forEach(item => {
    if (!channelMap[item.channelName]) {
      channelMap[item.channelName] = { count: 0, revenue: 0, nights: 0 }
    }
    channelMap[item.channelName].count += item.orderCount || 0
    channelMap[item.channelName].revenue += parseFloat(item.totalRevenue) || 0
    channelMap[item.channelName].nights += item.totalNights || 0
  })

  const channels = Object.keys(channelMap)
  const counts = channels.map(c => channelMap[c].count)
  const revenues = channels.map(c => channelMap[c].revenue)

  const option = props.type === 'pie'
    ? {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} 单 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          textStyle: { color: '#a3b5d6', fontSize: 12 }
        },
        series: [{
          name: '订单量',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['38%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#0b1836',
            borderWidth: 2
          },
          label: { show: false },
          labelLine: { show: false },
          data: channels.map((c, i) => ({
            name: c,
            value: counts[i] || 0,
            itemStyle: {
              color: ['#00d4ff', '#0066ff', '#00ffa3', '#ffb547', '#ff4d4f'][i % 5]
            }
          }))
        }]
      }
    : {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          textStyle: { color: '#fff' },
          backgroundColor: 'rgba(16,38,89,0.95)',
          borderColor: 'rgba(0,212,255,0.3)'
        },
        legend: {
          data: ['订单量', '收入(百元)'],
          textStyle: { color: '#a3b5d6' },
          top: 0
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '18%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: channels,
          axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
          axisLabel: { color: '#a3b5d6', fontSize: 12 }
        },
        yAxis: [
          {
            type: 'value',
            axisLine: { lineStyle: { color: 'rgba(0,212,255,0.3)' } },
            axisLabel: { color: '#a3b5d6' },
            splitLine: { lineStyle: { color: 'rgba(0,212,255,0.08)' } }
          }
        ],
        series: [
          {
            name: '订单量',
            type: 'bar',
            barWidth: '30%',
            data: counts,
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#00d4ff' },
                { offset: 1, color: '#0066ff' }
              ])
            }
          },
          {
            name: '收入(百元)',
            type: 'line',
            smooth: true,
            data: revenues.map(r => Math.round(r / 100)),
            lineStyle: { color: '#ffb547', width: 3 },
            itemStyle: { color: '#ffb547' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(255,181,71,0.3)' },
                { offset: 1, color: 'rgba(255,181,71,0.02)' }
              ])
            }
          }
        ]
      }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance && chartInstance.resize()
}

watch(() => props.distributionData, renderChart, { deep: true })

onMounted(() => {
  setTimeout(renderChart, 100)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style lang="scss" scoped>
.chart-panel {
  height: 100%;
  @include card-style;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid $border-color;
  }

  .panel-body {
    flex: 1;
    padding: 8px;
  }

  .chart-container {
    width: 100%;
    height: 100%;
    min-height: 260px;
  }
}
</style>
