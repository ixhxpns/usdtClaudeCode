<template>
  <div class="wallet-transactions-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">交易记录</h1>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
        查看您的所有钱包交易记录
      </p>
    </div>

    <!-- 筛选条件 -->
    <div class="mt-6">
      <el-card>
        <el-form 
          :model="filterForm" 
          :inline="true" 
          @submit.prevent="fetchTransactions"
        >
          <el-form-item label="交易类型">
            <el-select 
              v-model="filterForm.type" 
              placeholder="全部类型"
              clearable
              @change="fetchTransactions"
            >
              <el-option label="全部" value="" />
              <el-option label="充值" value="deposit" />
              <el-option label="提现" value="withdraw" />
              <el-option label="买入" value="buy" />
              <el-option label="卖出" value="sell" />
              <el-option label="转账" value="transfer" />
              <el-option label="手续费" value="fee" />
            </el-select>
          </el-form-item>

          <el-form-item label="币种">
            <el-select 
              v-model="filterForm.currency" 
              placeholder="全部币种"
              clearable
              @change="fetchTransactions"
            >
              <el-option label="全部" value="" />
              <el-option label="USDT" value="USDT" />
              <el-option label="BTC" value="BTC" />
              <el-option label="ETH" value="ETH" />
              <el-option label="CNY" value="CNY" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态">
            <el-select 
              v-model="filterForm.status" 
              placeholder="全部状态"
              clearable
              @change="fetchTransactions"
            >
              <el-option label="全部" value="" />
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已完成" value="completed" />
              <el-option label="失败" value="failed" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </el-form-item>

          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="fetchTransactions"
            />
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              @click="fetchTransactions"
              :loading="loading"
            >
              搜索
            </el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 统计卡片 -->
    <div class="mt-6 grid grid-cols-1 md:grid-cols-4 gap-6">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-green-100 text-green-600">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div>
            <p class="stat-label">总收入</p>
            <p class="stat-value text-green-600">
              {{ formatCurrency(statistics.totalIncome) }}
            </p>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-red-100 text-red-600">
            <el-icon :size="24"><Bottom /></el-icon>
          </div>
          <div>
            <p class="stat-label">总支出</p>
            <p class="stat-value text-red-600">
              {{ formatCurrency(statistics.totalExpense) }}
            </p>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-blue-100 text-blue-600">
            <el-icon :size="24"><Money /></el-icon>
          </div>
          <div>
            <p class="stat-label">手续费</p>
            <p class="stat-value text-blue-600">
              {{ formatCurrency(statistics.totalFees) }}
            </p>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon bg-purple-100 text-purple-600">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div>
            <p class="stat-label">交易笔数</p>
            <p class="stat-value text-purple-600">
              {{ statistics.totalCount }}
            </p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 交易记录表格 -->
    <div class="mt-6">
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-lg font-medium">交易记录</span>
            <div class="space-x-2">
              <el-button 
                @click="exportTransactions"
                :loading="exporting"
              >
                导出记录
              </el-button>
              <el-button 
                @click="fetchTransactions"
                :loading="loading"
              >
                刷新
              </el-button>
            </div>
          </div>
        </template>

        <el-table 
          :data="transactions" 
          style="width: 100%"
          v-loading="loading"
          row-key="id"
        >
          <el-table-column prop="createdAt" label="时间" width="180" sortable>
            <template #default="{ row }">
              <div>
                <div class="text-sm font-medium">
                  {{ formatDate(row.createdAt) }}
                </div>
                <div class="text-xs text-gray-500">
                  {{ formatTime(row.createdAt) }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag 
                :type="getTypeTagType(row.type)"
                effect="light"
                size="small"
              >
                {{ getTypeName(row.type) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="description" label="描述" min-width="200">
            <template #default="{ row }">
              <div>
                <div class="font-medium">{{ row.description }}</div>
                <div v-if="row.orderNumber" class="text-xs text-gray-500">
                  订单号: {{ row.orderNumber }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="currency" label="币种" width="80" />

          <el-table-column prop="amount" label="金额" width="150">
            <template #default="{ row }">
              <div class="text-right">
                <div 
                  class="font-medium"
                  :class="{
                    'text-green-600': row.direction === 'in',
                    'text-red-600': row.direction === 'out'
                  }"
                >
                  {{ row.direction === 'in' ? '+' : '-' }}{{ formatAmount(row.amount) }}
                </div>
                <div class="text-xs text-gray-500">
                  {{ row.currency }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="fee" label="手续费" width="120">
            <template #default="{ row }">
              <div class="text-right">
                <span v-if="row.fee > 0" class="text-red-600">
                  -{{ formatAmount(row.fee) }}
                </span>
                <span v-else class="text-gray-400">-</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="balance" label="余额" width="150">
            <template #default="{ row }">
              <div class="text-right font-medium">
                {{ formatAmount(row.balanceAfter) }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag 
                :type="getStatusType(row.status)"
                effect="light"
                size="small"
              >
                {{ getStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-dropdown trigger="click">
                <el-button text type="primary">
                  更多 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="viewDetails(row)">
                      查看详情
                    </el-dropdown-item>
                    <el-dropdown-item 
                      v-if="row.txHash"
                      @click="viewOnBlockchain(row.txHash)"
                    >
                      区块链浏览器
                    </el-dropdown-item>
                    <el-dropdown-item 
                      v-if="canCancel(row)"
                      @click="cancelTransaction(row)"
                      divided
                    >
                      取消交易
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="mt-6 flex justify-center">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchTransactions"
            @current-change="fetchTransactions"
          />
        </div>

        <!-- 空状态 -->
        <el-empty 
          v-if="!loading && transactions.length === 0"
          description="暂无交易记录"
          :image-size="200"
        />
      </el-card>
    </div>

    <!-- 交易详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      title="交易详情"
      width="600px"
      center
    >
      <div v-if="selectedTransaction" class="transaction-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="交易ID" span="2">
            <span class="font-mono">{{ selectedTransaction.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="交易类型">
            <el-tag :type="getTypeTagType(selectedTransaction.type)">
              {{ getTypeName(selectedTransaction.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedTransaction.status)">
              {{ getStatusName(selectedTransaction.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="币种">
            {{ selectedTransaction.currency }}
          </el-descriptions-item>
          <el-descriptions-item label="金额">
            <span :class="{
              'text-green-600': selectedTransaction.direction === 'in',
              'text-red-600': selectedTransaction.direction === 'out'
            }">
              {{ selectedTransaction.direction === 'in' ? '+' : '-' }}{{ formatAmount(selectedTransaction.amount) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="手续费">
            <span v-if="selectedTransaction.fee > 0" class="text-red-600">
              {{ formatAmount(selectedTransaction.fee) }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="交易后余额">
            {{ formatAmount(selectedTransaction.balanceAfter) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(selectedTransaction.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间">
            {{ selectedTransaction.completedAt ? formatDateTime(selectedTransaction.completedAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="订单号" v-if="selectedTransaction.orderNumber">
            <span class="font-mono">{{ selectedTransaction.orderNumber }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="交易哈希" v-if="selectedTransaction.txHash">
            <el-link 
              type="primary" 
              @click="viewOnBlockchain(selectedTransaction.txHash)"
              class="font-mono"
            >
              {{ selectedTransaction.txHash }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="地址信息" v-if="selectedTransaction.address" span="2">
            <span class="font-mono break-all">{{ selectedTransaction.address }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" v-if="selectedTransaction.remark" span="2">
            {{ selectedTransaction.remark }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  TrendCharts,
  Bottom,
  Money,
  Document,
  ArrowDown
} from '@element-plus/icons-vue'
import { 
  getWalletTransactions,
  getTransactionStatistics,
  exportTransactionHistory,
  cancelTransaction as cancelTransactionApi
} from '@/api/wallet'

// 响应式状态
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const exporting = ref(false)
const showDetailDialog = ref(false)
const selectedTransaction = ref(null)

// 筛选表单
const filterForm = reactive({
  type: '',
  currency: '',
  status: '',
  startDate: '',
  endDate: ''
})

const dateRange = ref<[string, string] | null>(null)

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 数据
const transactions = ref([])
const statistics = ref({
  totalIncome: 0,
  totalExpense: 0,
  totalFees: 0,
  totalCount: 0
})

// 监听URL参数
watch(() => route.query, (query) => {
  if (query.type) {
    filterForm.type = query.type as string
  }
  fetchTransactions()
}, { immediate: true })

// 监听日期范围变化
watch(dateRange, (range) => {
  if (range) {
    filterForm.startDate = range[0]
    filterForm.endDate = range[1]
  } else {
    filterForm.startDate = ''
    filterForm.endDate = ''
  }
})

// 方法
const fetchTransactions = async () => {
  try {
    loading.value = true
    const params = {
      ...filterForm,
      page: pagination.page,
      size: pagination.size
    }

    const response = await getWalletTransactions(params)
    
    if (response.data.success) {
      transactions.value = response.data.data.records
      pagination.total = response.data.data.total
    } else {
      ElMessage.error(response.data.message || '获取交易记录失败')
    }
  } catch (error) {
    console.error('获取交易记录失败:', error)
    ElMessage.error('获取交易记录失败，请重试')
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const params = {
      type: filterForm.type,
      currency: filterForm.currency,
      startDate: filterForm.startDate,
      endDate: filterForm.endDate
    }

    const response = await getTransactionStatistics(params)
    
    if (response.data.success) {
      statistics.value = response.data.data
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const resetFilters = () => {
  filterForm.type = ''
  filterForm.currency = ''
  filterForm.status = ''
  filterForm.startDate = ''
  filterForm.endDate = ''
  dateRange.value = null
  pagination.page = 1
  fetchTransactions()
}

const exportTransactions = async () => {
  try {
    exporting.value = true
    const params = {
      ...filterForm,
      format: 'xlsx'
    }

    const response = await exportTransactionHistory(params)
    
    if (response.data.success) {
      // 创建下载链接
      const url = URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.download = `transactions_${new Date().getTime()}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      
      ElMessage.success('导出成功')
    } else {
      ElMessage.error('导出失败')
    }
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

const viewDetails = (transaction: any) => {
  selectedTransaction.value = transaction
  showDetailDialog.value = true
}

const viewOnBlockchain = (txHash: string) => {
  // 根据不同网络打开对应的区块链浏览器
  const url = `https://tronscan.org/#/transaction/${txHash}`
  window.open(url, '_blank')
}

const canCancel = (transaction: any) => {
  return transaction.status === 'pending' && 
         ['withdraw', 'transfer'].includes(transaction.type)
}

const cancelTransaction = async (transaction: any) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      '确定要取消这笔交易吗？',
      '取消交易',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (!confirmed) return

    const response = await cancelTransactionApi(transaction.id)
    
    if (response.data.success) {
      ElMessage.success('交易已取消')
      fetchTransactions()
    } else {
      ElMessage.error(response.data.message || '取消失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消交易失败:', error)
      ElMessage.error('取消失败，请重试')
    }
  }
}

const formatCurrency = (amount: number, currency = 'USDT') => {
  return `${amount.toLocaleString()} ${currency}`
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString(undefined, { maximumFractionDigits: 8 })
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatTime = (date: string) => {
  return new Date(date).toLocaleTimeString('zh-CN', { 
    hour12: false,
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDateTime = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const getTypeName = (type: string) => {
  const map = {
    deposit: '充值',
    withdraw: '提现',
    buy: '买入',
    sell: '卖出',
    transfer: '转账',
    fee: '手续费',
    reward: '奖励',
    refund: '退款'
  }
  return map[type as keyof typeof map] || type
}

const getTypeTagType = (type: string) => {
  const map = {
    deposit: 'success',
    withdraw: 'warning',
    buy: 'primary',
    sell: 'danger',
    transfer: 'info',
    fee: '',
    reward: 'success',
    refund: 'warning'
  }
  return map[type as keyof typeof map] || 'info'
}

const getStatusName = (status: string) => {
  const map = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return map[status as keyof typeof map] || status
}

const getStatusType = (status: string) => {
  const map = {
    pending: 'warning',
    processing: 'info',
    completed: 'success',
    failed: 'danger',
    cancelled: 'info'
  }
  return map[status as keyof typeof map] || 'info'
}

// 生命周期
onMounted(() => {
  fetchTransactions()
  fetchStatistics()
})

// 监听筛选条件变化
watch(filterForm, () => {
  fetchStatistics()
})
</script>

<style scoped>
.wallet-transactions-view {
  @apply max-w-7xl mx-auto p-6;
}

.page-header {
  @apply mb-6;
}

.stat-card {
  @apply transition-shadow duration-200 hover:shadow-md;
}

.stat-content {
  @apply flex items-center space-x-4;
}

.stat-icon {
  @apply w-12 h-12 rounded-lg flex items-center justify-center;
}

.stat-label {
  @apply text-sm text-gray-600 dark:text-gray-400 mb-1;
}

.stat-value {
  @apply text-xl font-bold;
}

.transaction-detail {
  @apply space-y-4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .wallet-transactions-view {
    @apply p-4;
  }
  
  .grid {
    @apply grid-cols-2;
  }
  
  .stat-content {
    @apply flex-col space-x-0 space-y-2 text-center;
  }
  
  :deep(.el-table) {
    font-size: 12px;
  }
  
  :deep(.el-table .cell) {
    padding: 8px 4px;
  }
}

/* 暗黑模式支持 */
.dark .stat-card {
  @apply bg-gray-800 border-gray-700;
}
</style>