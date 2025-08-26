<template>
  <div class="trading-history-view">
    <el-card>
      <template #header>
        <div class="card-header flex justify-between items-center">
          <h3 class="text-lg font-medium">交易历史</h3>
          <div class="flex items-center space-x-2">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
              @change="handleDateChange"
            />
            <el-button @click="refreshData" :loading="loading" size="small">
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filters mb-4">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-select v-model="filters.type" placeholder="交易类型" clearable size="small">
              <el-option label="全部" value="" />
              <el-option label="买入" value="buy" />
              <el-option label="卖出" value="sell" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-select v-model="filters.status" placeholder="状态" clearable size="small">
              <el-option label="全部" value="" />
              <el-option label="已完成" value="completed" />
              <el-option label="部分成交" value="partial" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-input 
              v-model="filters.pair" 
              placeholder="交易对" 
              clearable 
              size="small"
            />
          </el-col>
          <el-col :span="6">
            <el-button type="primary" @click="applyFilters" size="small">
              筛选
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 交易历史表格 -->
      <el-table :data="tableData" :loading="loading" stripe>
        <el-table-column prop="time" label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="pair" label="交易对" width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'buy' ? 'success' : 'danger'" size="small">
              {{ row.type === 'buy' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120" align="right">
          <template #default="{ row }">
            {{ formatPrice(row.price) }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="数量" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="total" label="总额" width="140" align="right">
          <template #default="{ row }">
            {{ formatTotal(row.total) }}
          </template>
        </el-table-column>
        <el-table-column prop="fee" label="手续费" width="100" align="right">
          <template #default="{ row }">
            {{ formatFee(row.fee) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusType(row.status)" 
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button 
              link 
              type="primary" 
              @click="viewDetails(row)"
              size="small"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper mt-4 flex justify-center">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="交易详情"
      width="600px"
    >
      <div v-if="selectedOrder" class="order-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">
            {{ selectedOrder.id }}
          </el-descriptions-item>
          <el-descriptions-item label="交易对">
            {{ selectedOrder.pair }}
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="selectedOrder.type === 'buy' ? 'success' : 'danger'">
              {{ selectedOrder.type === 'buy' ? '买入' : '卖出' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="价格">
            {{ formatPrice(selectedOrder.price) }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">
            {{ formatAmount(selectedOrder.amount) }}
          </el-descriptions-item>
          <el-descriptions-item label="总额">
            {{ formatTotal(selectedOrder.total) }}
          </el-descriptions-item>
          <el-descriptions-item label="手续费">
            {{ formatFee(selectedOrder.fee) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedOrder.status)">
              {{ getStatusText(selectedOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatTime(selectedOrder.time) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedOrder = ref(null)
const dateRange = ref([])

const filters = reactive({
  type: '',
  status: '',
  pair: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const tableData = ref([])

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatPrice = (price: number) => {
  return price?.toFixed(8) || '0.00000000'
}

const formatAmount = (amount: number) => {
  return amount?.toFixed(6) || '0.000000'
}

const formatTotal = (total: number) => {
  return total?.toFixed(2) || '0.00'
}

const formatFee = (fee: number) => {
  return fee?.toFixed(6) || '0.000000'
}

const getStatusType = (status: string) => {
  const statusMap = {
    completed: 'success',
    partial: 'warning',
    cancelled: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap = {
    completed: '已完成',
    partial: '部分成交',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

const handleDateChange = () => {
  loadData()
}

const applyFilters = () => {
  pagination.current = 1
  loadData()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.current = page
  loadData()
}

const viewDetails = (row: any) => {
  selectedOrder.value = row
  detailDialogVisible.value = true
}

const refreshData = () => {
  loadData()
}

const loadData = async () => {
  try {
    loading.value = true
    // TODO: 调用API获取交易历史
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    // 模拟数据
    tableData.value = [
      {
        id: 'TXN001',
        time: '2024-01-20 10:30:00',
        pair: 'USDT/USDC',
        type: 'buy',
        price: 1.0001,
        amount: 1000,
        total: 1000.10,
        fee: 1.00,
        status: 'completed'
      }
    ]
    pagination.total = 1
  } catch (error) {
    console.error('加载交易历史失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.trading-history-view {
  max-width: 1200px;
}

.filters .el-select,
.filters .el-input {
  width: 100%;
}

.order-details {
  padding: 1rem 0;
}
</style>