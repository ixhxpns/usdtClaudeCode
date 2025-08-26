<template>
  <div class="trading-orders-view">
    <el-card>
      <template #header>
        <div class="card-header flex justify-between items-center">
          <h3 class="text-lg font-medium">当前订单</h3>
          <div class="flex items-center space-x-2">
            <el-button @click="refreshData" :loading="loading" size="small">
              刷新
            </el-button>
            <el-button @click="cancelAllOrders" type="danger" size="small">
              取消全部
            </el-button>
          </div>
        </div>
      </template>

      <!-- 订单筛选 -->
      <div class="filters mb-4">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-select v-model="filters.type" placeholder="订单类型" clearable size="small">
              <el-option label="全部" value="" />
              <el-option label="限价单" value="limit" />
              <el-option label="市价单" value="market" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-select v-model="filters.side" placeholder="买卖方向" clearable size="small">
              <el-option label="全部" value="" />
              <el-option label="买入" value="buy" />
              <el-option label="卖出" value="sell" />
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

      <!-- 订单表格 -->
      <el-table :data="tableData" :loading="loading" stripe>
        <el-table-column prop="time" label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="pair" label="交易对" width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">
              {{ row.type === 'limit' ? '限价' : '市价' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="side" label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.side === 'buy' ? 'success' : 'danger'" size="small">
              {{ row.side === 'buy' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120" align="right">
          <template #default="{ row }">
            {{ row.type === 'market' ? '市价' : formatPrice(row.price) }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="数量" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="filled" label="已成交" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.filled) }}
          </template>
        </el-table-column>
        <el-table-column prop="remaining" label="剩余" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.remaining) }}
          </template>
        </el-table-column>
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="getProgress(row)" 
              :stroke-width="6"
              :show-text="false"
            />
            <div class="text-xs text-gray-500 mt-1">
              {{ getProgress(row) }}%
            </div>
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
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <div class="flex space-x-2">
              <el-button 
                link 
                type="primary" 
                @click="viewDetails(row)"
                size="small"
              >
                详情
              </el-button>
              <el-button 
                link 
                type="danger" 
                @click="cancelOrder(row)"
                size="small"
                :disabled="!canCancel(row.status)"
              >
                取消
              </el-button>
            </div>
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

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
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
            <el-tag>
              {{ selectedOrder.type === 'limit' ? '限价单' : '市价单' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="方向">
            <el-tag :type="selectedOrder.side === 'buy' ? 'success' : 'danger'">
              {{ selectedOrder.side === 'buy' ? '买入' : '卖出' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="委托价格">
            {{ selectedOrder.type === 'market' ? '市价' : formatPrice(selectedOrder.price) }}
          </el-descriptions-item>
          <el-descriptions-item label="委托数量">
            {{ formatAmount(selectedOrder.amount) }}
          </el-descriptions-item>
          <el-descriptions-item label="已成交">
            {{ formatAmount(selectedOrder.filled) }}
          </el-descriptions-item>
          <el-descriptions-item label="剩余数量">
            {{ formatAmount(selectedOrder.remaining) }}
          </el-descriptions-item>
          <el-descriptions-item label="成交进度">
            {{ getProgress(selectedOrder) }}%
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
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedOrder = ref(null)

const filters = reactive({
  type: '',
  side: '',
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

const getProgress = (row: any) => {
  if (!row.amount || row.amount === 0) return 0
  return Math.round((row.filled / row.amount) * 100)
}

const getStatusType = (status: string) => {
  const statusMap = {
    open: 'warning',
    partial: 'info',
    filled: 'success',
    cancelled: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap = {
    open: '等待成交',
    partial: '部分成交',
    filled: '完全成交',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

const canCancel = (status: string) => {
  return ['open', 'partial'].includes(status)
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

const cancelOrder = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单 ${row.id} 吗？`,
      '确认取消',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API取消订单
    ElMessage.success('订单取消成功')
    loadData()
  } catch (error) {
    console.error('取消订单失败:', error)
  }
}

const cancelAllOrders = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消所有当前订单吗？此操作不可逆！',
      '确认取消全部',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 调用API取消全部订单
    ElMessage.success('所有订单已取消')
    loadData()
  } catch (error) {
    console.error('取消全部订单失败:', error)
  }
}

const refreshData = () => {
  loadData()
}

const loadData = async () => {
  try {
    loading.value = true
    // TODO: 调用API获取当前订单
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    // 模拟数据
    tableData.value = [
      {
        id: 'ORD001',
        time: '2024-01-20 10:30:00',
        pair: 'USDT/USDC',
        type: 'limit',
        side: 'buy',
        price: 1.0001,
        amount: 1000,
        filled: 500,
        remaining: 500,
        status: 'partial'
      }
    ]
    pagination.total = 1
  } catch (error) {
    console.error('加载订单失败:', error)
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
.trading-orders-view {
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