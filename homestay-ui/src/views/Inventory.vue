<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>库存管理</h2>
      <div class="header-actions">
        <el-input-number v-model="roomTypeId" :min="1" label="房型ID:" />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" @click="loadInventory">查询</el-button>
        <el-button @click="showGenerate = true">批量生成</el-button>
      </div>
    </div>
    <div class="page-body">
      <el-table :data="inventory" stripe border height="calc(100vh - 220px)">
        <el-table-column prop="stayDate" label="日期" width="130" fixed />
        <el-table-column prop="typeName" label="房型" width="140" />
        <el-table-column label="总房量" width="110" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.totalRooms" :min="0" size="small" controls-position="right" @change="adjust(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="bookedRooms" label="已预订" width="100" align="center" />
        <el-table-column prop="lockedRooms" label="锁定中" width="100" align="center" />
        <el-table-column prop="availableRooms" label="可用" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.availableRooms <= 2 ? '#ff4d4f' : '#00ffa3', fontWeight: 600 }">
              {{ row.availableRooms }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="基准价" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.basePrice"
              :min="0"
              :precision="2"
              size="small"
              controls-position="right"
              @change="adjustPrice(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="lockRow(row)">锁定</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showGenerate" title="批量生成库存" width="480px">
      <el-form label-width="90px">
        <el-form-item label="房型ID">
          <el-input-number v-model="genData.roomTypeId" :min="1" />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="genData.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="默认房量">
          <el-input-number v-model="genData.rooms" :min="0" />
        </el-form-item>
        <el-form-item label="默认价格">
          <el-input-number v-model="genData.price" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerate = false">取消</el-button>
        <el-button type="primary" @click="doGenerate">确认生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { queryInventory, adjustInventory, adjustPrice, generateInventory, lockInventory } from '@/api/inventory'
import { ElMessage, ElMessageBox } from 'element-plus'

const roomTypeId = ref(1)
const dateRange = ref([dayjs().format('YYYY-MM-DD'), dayjs().add(30, 'day').format('YYYY-MM-DD')])
const inventory = ref([])
const showGenerate = ref(false)
const genData = reactive({
  roomTypeId: 1,
  dateRange: [dayjs().format('YYYY-MM-DD'), dayjs().add(60, 'day').format('YYYY-MM-DD')],
  rooms: 10,
  price: 299
})

const loadInventory = async () => {
  const res = await queryInventory(roomTypeId.value, dateRange.value[0], dateRange.value[1])
  if (res.code === 200) inventory.value = res.data || []
}

const adjust = async (row) => {
  const res = await adjustInventory(row.roomTypeId, row.stayDate, row.totalRooms)
  if (res.code === 200) { ElMessage.success('已更新'); loadInventory() }
}
const adjustPrice = async (row) => {
  const res = await adjustPrice(row.roomTypeId, row.stayDate, row.basePrice)
  if (res.code === 200) ElMessage.success('价格已更新')
}
const lockRow = async (row) => {
  await ElMessageBox.confirm(`锁定 ${row.stayDate} 1间房?`, '提示')
  const res = await lockInventory({
    roomTypeId: row.roomTypeId,
    startDate: row.stayDate,
    endDate: row.stayDate,
    lockCount: 1,
    lockType: 3,
    sourceType: 'MANUAL_BLOCK',
    sourceId: 'MANUAL_' + Date.now(),
    expireMinutes: 1440 * 365
  })
  if (res.code === 200) { ElMessage.success('已锁定'); loadInventory() }
}
const doGenerate = async () => {
  const res = await generateInventory({
    roomTypeId: genData.roomTypeId,
    startDate: genData.dateRange[0],
    endDate: genData.dateRange[1],
    rooms: genData.rooms,
    price: genData.price
  })
  if (res.code === 200) { ElMessage.success('生成成功'); showGenerate.value = false; loadInventory() }
}

onMounted(loadInventory)
</script>

<style lang="scss" scoped>
.page-wrap { padding: 24px; }
.page-header {
  @include flex-between;
  margin-bottom: 20px;
  h2 { color: $text-primary; }
  .header-actions { display: flex; gap: 12px; flex-wrap: wrap; }
}
:deep(.el-table) { --el-table-bg-color: $bg-card; }
</style>
