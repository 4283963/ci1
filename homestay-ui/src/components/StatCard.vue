<template>
  <div class="stat-card" :style="{ borderTop: `3px solid ${color}` }">
    <div class="stat-icon" :style="{ background: `${color}20`, color }">
      <el-icon :size="28"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">
        <span class="num">{{ displayValue }}</span>
        <span v-if="unit" class="unit">{{ unit }}</span>
      </div>
      <div v-if="trend !== undefined" class="stat-trend" :class="trend >= 0 ? 'up' : 'down'">
        <el-icon><CaretTop v-if="trend >= 0" /><CaretBottom v-else /></el-icon>
        {{ Math.abs(trend) }}%
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  value: [Number, String],
  unit: String,
  icon: String,
  color: { type: String, default: '#00d4ff' },
  trend: Number
})

const displayValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString('zh-CN')
  }
  return props.value || 0
})
</script>

<style lang="scss" scoped>
.stat-card {
  @include card-style;
  padding: 20px;
  display: flex;
  gap: 18px;
  align-items: center;
  transition: transform 0.3s, box-shadow 0.3s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 30px rgba(0, 102, 255, 0.2);
  }

  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    @include flex-center;
    flex-shrink: 0;
  }

  .stat-content {
    flex: 1;
    min-width: 0;

    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: 6px;
    }

    .stat-value {
      margin-bottom: 4px;

      .num {
        font-size: 28px;
        font-weight: 700;
        font-family: 'DIN', sans-serif;
        letter-spacing: -0.5px;
      }

      .unit {
        font-size: 13px;
        color: $text-secondary;
        margin-left: 4px;
      }
    }

    .stat-trend {
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 2px;

      &.up { color: $success-color; }
      &.down { color: $danger-color; }
    }
  }
}
</style>
