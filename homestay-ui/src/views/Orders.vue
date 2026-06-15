<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="['00:00:00', '23:59:59']"
          value-format="YYYY-MM-DD"
        />
        <el-select v-model="statusFilter" placeholder="订单状态" clearable style="width: 140px">
          <el-option label="待确认" :value="1" />
          <el-option label="已确认" :value="2" />
          <el-option label="已入住" :value="3" />
          <el-option label="已退房" :value="4" />
          <el-option label="已取消" :value="5" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </div>
    <div class="page-body">
      <el-table :data="orders" stripe border>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="channelOrderNo" label="渠道订单号" width="160" />
        <el-table-column prop="guestName" label="客人" width="100" />
        <el-table-column prop="guestPhone" label="电话" width="130" />
        <el-table-column prop="checkinDate" label="入住" width="120" />
        <el-table-column prop="checkoutDate" label="离店" width="120" />
        <el-table-column prop="nightCount" label="晚数" width="80" align="center" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.orderStatus)">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.orderStatus === 1" size="small" type="success" @click="confirm(row)">确认</el-button>
            <el-button v-if="row.orderStatus < 5" size="small" type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { listOrders, confirmOrder, cancelOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const dateRange = ref([dayjs().subtract(30, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const statusFilter = ref()
const orders = ref([])

const loadData = async () => {
  const res = await listOrders(dateRange.value[0], dateRange.value[1], statusFilter.value)
  if (res.code === 200) orders.value = res.data || []
}

const statusTag = (s) => ({1:'warning', 2:'primary', 3:'success', 4:'info', 5:'danger', 6:'info'}[s] || 'info')
const statusText = (s) => ({1:'待确认', 2:'已确认', 3:'已入住', 4:'已退房', 5:'已取消', 6:'未到'}[s] || s)

const confirm = async (row) => {
  await ElMessageBox.confirm(`确认订单 ${row.orderNo} ?`, '提示')
  const res = await confirmOrder(row.id)
  if (res.code === 200) { ElMessage.success('确认成功'); loadData() }
}
const cancel = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', { inputValue: '客人取消' })
  const res = await cancelOrder(row.id, value)
  if (res.code === 200) { ElMessage.success('取消成功'); loadData() }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page-wrap { padding: 24px; height: 100vh; overflow: auto; }
.page-header {
  @include flex-between;
  margin-bottom: 20px;
  h2 { color: $text-primary; }
  .header-actions { display: flex; gap: 12px; }
}
:deep(.el-table) { --el-table-bg-color: $bg-card; }
</style>
