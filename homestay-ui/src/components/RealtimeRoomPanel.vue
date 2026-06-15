<template>
  <div class="realtime-panel">
    <div class="panel-header">
      <div class="card-title">实时房态监控</div>
      <div class="header-right">
        <el-tag :type="wsConnected ? 'success' : 'info'" effect="dark" size="small">
          <el-icon class="mr-1"><Connection /></el-icon>
          {{ wsConnected ? '实时推送中' : '轮询模式' }}
        </el-tag>
        <span class="update-time">{{ lastUpdateTime }}</span>
      </div>
    </div>
    <div class="panel-body">
      <el-table
        :data="tableData"
        stripe
        size="small"
        height="100%"
        :row-class-name="rowClassName"
        style="width: 100%"
      >
        <el-table-column prop="propertyName" label="物业" width="130" />
        <el-table-column prop="typeName" label="房型" min-width="120" />
        <el-table-column label="总房量" width="90" align="center">
          <template #default="{ row }">
            <span class="total-num">{{ row.totalRooms || row.physicalRooms || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已预订" width="100" align="center">
          <template #default="{ row }">
            <span class="booked-num">{{ row.bookedRooms || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="锁定中" width="90" align="center">
          <template #default="{ row }">
            <span class="locked-num">{{ row.lockedRooms || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可用房" width="100" align="center">
          <template #default="{ row }">
            <span :class="['available-num', (row.availableRooms || 0) <= 2 ? 'warn' : '']">
              {{ row.availableRooms || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="基准价" width="100" align="center">
          <template #default="{ row }">
            <span class="price">¥{{ Number(row.basePrice || 0).toFixed(0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="出租率" min-width="160">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="Number(row.bookingRate || 0).toFixed(0)"
                :stroke-width="14"
                :color="progressColor(row.bookingRate)"
                :show-text="true"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row)" size="small" effect="dark">
              {{ statusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tableData: { type: Array, default: () => [] },
  lastUpdateTime: { type: String, default: '' },
  wsConnected: { type: Boolean, default: false }
})

const progressColor = (rate) => {
  const r = Number(rate || 0)
  if (r >= 90) return '#ff4d4f'
  if (r >= 70) return '#ffb547'
  if (r >= 50) return '#00d4ff'
  return '#00ffa3'
}

const statusType = (row) => {
  const avail = Number(row.availableRooms || 0)
  const total = Number(row.totalRooms || row.physicalRooms || 0)
  if (avail === 0 && total > 0) return 'danger'
  if (avail <= 2) return 'warning'
  if (Number(row.bookingRate || 0) >= 70) return 'success'
  return 'info'
}

const statusText = (row) => {
  const avail = Number(row.availableRooms || 0)
  const total = Number(row.totalRooms || row.physicalRooms || 0)
  if (avail === 0 && total > 0) return '已满房'
  if (avail <= 2) return '紧张'
  return '正常'
}

const rowClassName = ({ row }) => {
  const avail = Number(row.availableRooms || 0)
  if (avail === 0) return 'row-full'
  if (avail <= 2) return 'row-warn'
  return ''
}
</script>

<style lang="scss" scoped>
.realtime-panel {
  height: 100%;
  @include card-style;
  display: flex;
  flex-direction: column;

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid $border-color;
    @include flex-between;

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .update-time {
        font-size: 12px;
        color: $text-secondary;
      }
    }
  }

  .panel-body {
    flex: 1;
    padding: 8px;
    overflow: hidden;
  }

  :deep(.el-table) {
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: rgba(0, 102, 255, 0.1);
    --el-table-border-color: rgba(0, 212, 255, 0.1);
    --el-table-text-color: $text-primary;
    --el-table-header-text-color: $text-primary;
    font-size: 13px;

    th {
      font-weight: 600 !important;
    }
  }

  :deep(.row-full) {
    background: rgba(255, 77, 79, 0.08) !important;
  }
  :deep(.row-warn) {
    background: rgba(255, 181, 71, 0.08) !important;
  }

  .total-num { font-weight: 600; }
  .booked-num { color: $secondary-color; font-weight: 600; }
  .locked-num { color: $warning-color; font-weight: 600; }
  .available-num {
    color: $success-color;
    font-weight: 700;
    &.warn { color: $danger-color; animation: blink 1.2s infinite; }
  }
  .price { color: $warning-color; font-weight: 600; }
  .rate-cell { padding: 0 8px; }

  @keyframes blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
}
</style>
