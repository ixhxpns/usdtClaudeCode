<template>
  <div class="withdrawal-pending-view">
    <!-- 統計卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.pendingCount }}</div>
            <div class="stat-label">待處理</div>
          </div>
          <div class="stat-icon pending">
            <el-icon><Clock /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">${{ formatNumber(statistics.pendingAmount) }}</div>
            <div class="stat-label">待處理金額</div>
          </div>
          <div class="stat-icon warning">
            <el-icon><Money /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.todayProcessed }}</div>
            <div class="stat-label">今日已處理</div>
          </div>
          <div class="stat-icon success">
            <el-icon><CircleCheckFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.avgProcessTime }}分鐘</div>
            <div class="stat-label">平均處理時間</div>
          </div>
          <div class="stat-icon info">
            <el-icon><Timer /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 篩選條件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filterForm" inline label-width="100px">
        <el-form-item label="提現ID">
          <el-input 
            v-model="filterForm.withdrawalId" 
            placeholder="請輸入提現ID"
            clearable
          />
        </el-form-item>

        <el-form-item label="用戶信息">
          <el-input 
            v-model="filterForm.userInfo" 
            placeholder="用戶名/郵箱/手機"
            clearable
          />
        </el-form-item>

        <el-form-item label="提現金額">
          <el-input-number
            v-model="filterForm.minAmount"
            placeholder="最小金額"
            :min="0"
            :precision="2"
            style="width: 120px;"
          />
          <span style="margin: 0 8px;">-</span>
          <el-input-number
            v-model="filterForm.maxAmount"
            placeholder="最大金額"
            :min="0"
            :precision="2"
            style="width: 120px;"
          />
        </el-form-item>

        <el-form-item label="優先級">
          <el-select 
            v-model="filterForm.priority" 
            placeholder="選擇優先級"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>

        <el-form-item label="提交時間">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="開始時間"
            end-placeholder="結束時間"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="loadWithdrawals">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
          <el-button type="success" @click="autoAssign">
            <el-icon><Select /></el-icon>
            智能分配
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提現列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <h3>待處理提現列表</h3>
          <div class="table-actions">
            <el-button-group>
              <el-button 
                :type="viewMode === 'list' ? 'primary' : ''"
                @click="viewMode = 'list'"
              >
                <el-icon><List /></el-icon>
                列表視圖
              </el-button>
              <el-button 
                :type="viewMode === 'card' ? 'primary' : ''"
                @click="viewMode = 'card'"
              >
                <el-icon><Grid /></el-icon>
                卡片視圖
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <!-- 列表視圖 -->
      <div v-if="viewMode === 'list'">
        <el-table
          :data="withdrawalList"
          v-loading="loading"
          row-key="id"
          @selection-change="handleSelectionChange"
          @sort-change="handleSortChange"
        >
          <el-table-column type="selection" width="50" />
          
          <el-table-column 
            prop="id" 
            label="提現ID" 
            width="100"
            sortable="custom"
          />

          <el-table-column label="用戶信息" min-width="200">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :size="32" :src="row.user.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="user-details">
                  <div class="username">{{ row.user.username }}</div>
                  <div class="email">{{ row.user.email }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="提現金額" width="140" sortable="custom">
            <template #default="{ row }">
              <div class="amount-info">
                <div class="amount">${{ formatNumber(row.amount) }}</div>
                <div class="fee">手續費: ${{ formatNumber(row.fee) }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="收款信息" min-width="200">
            <template #default="{ row }">
              <div class="payment-info">
                <div class="method">{{ row.paymentMethod }}</div>
                <div class="account">{{ row.paymentAccount }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="優先級" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getPriorityType(row.priority)">
                {{ getPriorityText(row.priority) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="等待時間" width="120" sortable="custom">
            <template #default="{ row }">
              <div class="wait-time" :class="getWaitTimeClass(row.waitTime)">
                {{ formatWaitTime(row.waitTime) }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="提交時間" width="160">
            <template #default="{ row }">
              {{ dayjs(row.createdAt).format('MM-DD HH:mm') }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small"
                @click="reviewWithdrawal(row)"
              >
                審核
              </el-button>
              <el-button 
                type="success" 
                size="small"
                @click="quickApprove(row)"
              >
                快速通過
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 卡片視圖 -->
      <div v-else class="card-view">
        <div class="withdrawal-cards">
          <div 
            v-for="withdrawal in withdrawalList" 
            :key="withdrawal.id"
            class="withdrawal-card"
            :class="getPriorityClass(withdrawal.priority)"
          >
            <div class="card-header">
              <div class="card-id">提現 #{{ withdrawal.id }}</div>
              <el-tag :type="getPriorityType(withdrawal.priority)" size="small">
                {{ getPriorityText(withdrawal.priority) }}
              </el-tag>
            </div>

            <div class="card-body">
              <div class="user-section">
                <el-avatar :size="40" :src="withdrawal.user.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="user-info">
                  <div class="username">{{ withdrawal.user.username }}</div>
                  <div class="email">{{ withdrawal.user.email }}</div>
                </div>
              </div>

              <div class="amount-section">
                <div class="amount-label">提現金額</div>
                <div class="amount-value">${{ formatNumber(withdrawal.amount) }}</div>
                <div class="fee-info">手續費: ${{ formatNumber(withdrawal.fee) }}</div>
              </div>

              <div class="payment-section">
                <div class="payment-method">{{ withdrawal.paymentMethod }}</div>
                <div class="payment-account">{{ withdrawal.paymentAccount }}</div>
              </div>

              <div class="time-section">
                <div class="submit-time">
                  提交時間: {{ dayjs(withdrawal.createdAt).format('MM-DD HH:mm') }}
                </div>
                <div class="wait-time" :class="getWaitTimeClass(withdrawal.waitTime)">
                  等待: {{ formatWaitTime(withdrawal.waitTime) }}
                </div>
              </div>
            </div>

            <div class="card-footer">
              <el-button 
                type="primary" 
                size="small"
                @click="reviewWithdrawal(withdrawal)"
              >
                詳細審核
              </el-button>
              <el-button 
                type="success" 
                size="small"
                @click="quickApprove(withdrawal)"
              >
                快速通過
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分頁 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadWithdrawals"
          @current-change="loadWithdrawals"
        />
      </div>
    </el-card>

    <!-- 批量操作 -->
    <el-card v-if="selectedWithdrawals.length > 0" class="batch-actions" shadow="never">
      <div class="batch-info">
        已選擇 {{ selectedWithdrawals.length }} 筆提現，總金額 ${{ formatNumber(selectedAmount) }}
      </div>
      <div class="batch-buttons">
        <el-button type="success" @click="batchApprove">
          <el-icon><Select /></el-icon>
          批量通過
        </el-button>
        <el-button type="danger" @click="batchReject">
          <el-icon><CloseBold /></el-icon>
          批量拒絕
        </el-button>
        <el-button @click="assignToReviewer">
          <el-icon><User /></el-icon>
          分配審核員
        </el-button>
      </div>
    </el-card>

    <!-- 審核對話框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="提現審核"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="currentWithdrawal" class="review-content">
        <!-- 提現詳情 -->
        <el-descriptions title="提現詳情" :column="2" border>
          <el-descriptions-item label="提現ID">
            {{ currentWithdrawal.id }}
          </el-descriptions-item>
          <el-descriptions-item label="用戶">
            {{ currentWithdrawal.user.username }} ({{ currentWithdrawal.user.email }})
          </el-descriptions-item>
          <el-descriptions-item label="提現金額">
            ${{ formatNumber(currentWithdrawal.amount) }}
          </el-descriptions-item>
          <el-descriptions-item label="手續費">
            ${{ formatNumber(currentWithdrawal.fee) }}
          </el-descriptions-item>
          <el-descriptions-item label="實際到賬">
            ${{ formatNumber(currentWithdrawal.amount - currentWithdrawal.fee) }}
          </el-descriptions-item>
          <el-descriptions-item label="收款方式">
            {{ currentWithdrawal.paymentMethod }}
          </el-descriptions-item>
          <el-descriptions-item label="收款賬戶" :span="2">
            {{ currentWithdrawal.paymentAccount }}
          </el-descriptions-item>
          <el-descriptions-item label="提交時間">
            {{ dayjs(currentWithdrawal.createdAt).format('YYYY-MM-DD HH:mm:ss') }}
          </el-descriptions-item>
          <el-descriptions-item label="等待時間">
            {{ formatWaitTime(currentWithdrawal.waitTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 風險評估 -->
        <div class="risk-assessment">
          <h4>風險評估</h4>
          <el-alert
            :title="getRiskAssessment(currentWithdrawal).title"
            :type="getRiskAssessment(currentWithdrawal).type"
            :description="getRiskAssessment(currentWithdrawal).description"
            show-icon
            :closable="false"
          />
        </div>

        <!-- 審核表單 -->
        <div class="review-form">
          <h4>審核決定</h4>
          <el-form :model="reviewForm" label-width="100px">
            <el-form-item label="審核結果" required>
              <el-radio-group v-model="reviewForm.decision">
                <el-radio label="APPROVED">通過</el-radio>
                <el-radio label="REJECTED">拒絕</el-radio>
                <el-radio label="HOLD">暫緩</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item 
              label="審核備註" 
              :required="reviewForm.decision === 'REJECTED' || reviewForm.decision === 'HOLD'"
            >
              <el-input 
                v-model="reviewForm.remarks" 
                type="textarea"
                :rows="4"
                placeholder="請填寫審核備註（拒絕或暫緩時必填）"
              />
            </el-form-item>

            <el-form-item v-if="reviewForm.decision === 'APPROVED'" label="處理方式">
              <el-radio-group v-model="reviewForm.processType">
                <el-radio label="AUTO">自動處理</el-radio>
                <el-radio label="MANUAL">手動處理</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitReview"
          :loading="reviewSubmitting"
        >
          提交審核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { AdminApi } from '@/api/admin'
import { dayjs } from 'element-plus'
import {
  Clock,
  Money,
  CircleCheckFilled,
  Timer,
  Search,
  RefreshLeft,
  List,
  Grid,
  User,
  Select,
  CloseBold
} from '@element-plus/icons-vue'

const authStore = useAuthStore()

// 響應式數據
const loading = ref(false)
const viewMode = ref<'list' | 'card'>('list')
const withdrawalList = ref<any[]>([])
const selectedWithdrawals = ref<any[]>([])
const dateRange = ref<[string, string] | null>(null)

// 對話框
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const currentWithdrawal = ref<any>(null)

// 篩選表單
const filterForm = reactive({
  withdrawalId: '',
  userInfo: '',
  minAmount: null as number | null,
  maxAmount: null as number | null,
  priority: ''
})

// 分頁信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 統計信息
const statistics = reactive({
  pendingCount: 0,
  pendingAmount: 0,
  todayProcessed: 0,
  avgProcessTime: 0
})

// 審核表單
const reviewForm = reactive({
  decision: 'APPROVED',
  remarks: '',
  processType: 'AUTO'
})

// 計算屬性
const selectedAmount = computed(() => {
  return selectedWithdrawals.value.reduce((total, item) => total + item.amount, 0)
})

// 方法
const formatNumber = (num: number) => {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num)
}

const getPriorityType = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return 'danger'
    case 'MEDIUM':
      return 'warning'
    case 'LOW':
      return 'success'
    default:
      return 'info'
  }
}

const getPriorityText = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return '高'
    case 'MEDIUM':
      return '中'
    case 'LOW':
      return '低'
    default:
      return '普通'
  }
}

const getPriorityClass = (priority: string) => {
  switch (priority) {
    case 'HIGH':
      return 'high-priority'
    case 'MEDIUM':
      return 'medium-priority'
    case 'LOW':
      return 'low-priority'
    default:
      return 'normal-priority'
  }
}

const getWaitTimeClass = (waitTime: number) => {
  if (waitTime > 60) return 'urgent'
  if (waitTime > 30) return 'warning'
  return 'normal'
}

const formatWaitTime = (minutes: number) => {
  if (minutes < 60) {
    return `${minutes}分鐘`
  } else {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `${hours}小時${mins}分鐘`
  }
}

const getRiskAssessment = (withdrawal: any) => {
  const riskScore = withdrawal.riskScore || 0
  
  if (riskScore >= 80) {
    return {
      title: '高風險提現',
      type: 'error' as const,
      description: '該提現存在高風險因素，建議仔細審核或進行人工處理'
    }
  } else if (riskScore >= 50) {
    return {
      title: '中等風險提現',
      type: 'warning' as const,
      description: '該提現存在一定風險因素，建議關注相關指標'
    }
  } else {
    return {
      title: '低風險提現',
      type: 'success' as const,
      description: '該提現風險較低，可正常處理'
    }
  }
}

const loadWithdrawals = async () => {
  try {
    loading.value = true
    
    const params = {
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      status: 'PENDING',
      ...filterForm,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }

    // const response = await AdminApi.getWithdrawalList(params)
    // withdrawalList.value = response.data
    // pagination.total = response.total

    // 模擬數據
    loadMockWithdrawals()
  } catch (error: any) {
    ElMessage.error(error.message || '載入提現列表失敗')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    // const response = await AdminApi.getWithdrawalStatistics()
    // Object.assign(statistics, response)

    // 模擬數據
    Object.assign(statistics, {
      pendingCount: 23,
      pendingAmount: 45680.50,
      todayProcessed: 67,
      avgProcessTime: 25
    })
  } catch (error: any) {
    console.error('載入統計信息失敗:', error)
  }
}

const loadMockWithdrawals = () => {
  withdrawalList.value = [
    {
      id: 'W001',
      user: {
        username: 'john_doe',
        email: 'john@example.com',
        avatar: ''
      },
      amount: 5000.00,
      fee: 50.00,
      paymentMethod: '銀行轉賬',
      paymentAccount: '***1234',
      priority: 'HIGH',
      waitTime: 75,
      createdAt: '2024-01-20T10:30:00Z',
      riskScore: 85
    },
    {
      id: 'W002',
      user: {
        username: 'jane_smith',
        email: 'jane@example.com',
        avatar: ''
      },
      amount: 1200.00,
      fee: 12.00,
      paymentMethod: 'USDT',
      paymentAccount: 'TN7p...8xK2',
      priority: 'MEDIUM',
      waitTime: 35,
      createdAt: '2024-01-20T12:15:00Z',
      riskScore: 25
    }
  ]
  pagination.total = 2
}

const resetFilter = () => {
  Object.assign(filterForm, {
    withdrawalId: '',
    userInfo: '',
    minAmount: null,
    maxAmount: null,
    priority: ''
  })
  dateRange.value = null
  pagination.currentPage = 1
  loadWithdrawals()
}

const handleSelectionChange = (selection: any[]) => {
  selectedWithdrawals.value = selection
}

const handleSortChange = ({ prop, order }: any) => {
  // 處理排序
  loadWithdrawals()
}

const reviewWithdrawal = (withdrawal: any) => {
  currentWithdrawal.value = withdrawal
  reviewForm.decision = 'APPROVED'
  reviewForm.remarks = ''
  reviewForm.processType = 'AUTO'
  reviewDialogVisible.value = true
}

const quickApprove = async (withdrawal: any) => {
  try {
    await ElMessageBox.confirm(`確定快速通過提現 #${withdrawal.id} 嗎？`, '確認操作')
    
    // await AdminApi.approveWithdrawal(withdrawal.id, {
    //   decision: 'APPROVED',
    //   remarks: '快速通過',
    //   processType: 'AUTO'
    // })

    ElMessage.success('提現審核通過')
    loadWithdrawals()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '審核失敗')
    }
  }
}

const submitReview = async () => {
  if (!currentWithdrawal.value) return

  if ((reviewForm.decision === 'REJECTED' || reviewForm.decision === 'HOLD') && !reviewForm.remarks) {
    ElMessage.warning('拒絕或暫緩提現時必須填寫備註')
    return
  }

  try {
    reviewSubmitting.value = true
    
    // await AdminApi.reviewWithdrawal(currentWithdrawal.value.id, reviewForm)

    ElMessage.success('審核提交成功')
    reviewDialogVisible.value = false
    loadWithdrawals()
  } catch (error: any) {
    ElMessage.error(error.message || '審核提交失敗')
  } finally {
    reviewSubmitting.value = false
  }
}

const autoAssign = async () => {
  try {
    // await AdminApi.autoAssignWithdrawals()
    ElMessage.success('智能分配完成')
    loadWithdrawals()
  } catch (error: any) {
    ElMessage.error(error.message || '智能分配失敗')
  }
}

const batchApprove = async () => {
  try {
    await ElMessageBox.confirm(`確定批量通過選中的 ${selectedWithdrawals.value.length} 筆提現嗎？`, '批量操作')
    
    const ids = selectedWithdrawals.value.map(item => item.id)
    // await AdminApi.batchApproveWithdrawals(ids)
    
    ElMessage.success('批量審核成功')
    loadWithdrawals()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量審核失敗')
    }
  }
}

const batchReject = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      `確定批量拒絕選中的 ${selectedWithdrawals.value.length} 筆提現嗎？請填寫拒絕原因：`,
      '批量拒絕',
      {
        confirmButtonText: '確認拒絕',
        cancelButtonText: '取消'
      }
    )

    const ids = selectedWithdrawals.value.map(item => item.id)
    // await AdminApi.batchRejectWithdrawals(ids, value)
    
    ElMessage.success('批量拒絕成功')
    loadWithdrawals()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量拒絕失敗')
    }
  }
}

const assignToReviewer = async () => {
  // 分配審核員功能
  ElMessage.info('審核員分配功能開發中')
}

// 初始化
onMounted(() => {
  loadWithdrawals()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.withdrawal-pending-view {
  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      padding: 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;

      .stat-content {
        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: var(--el-text-color-primary);
          margin-bottom: 8px;
        }

        .stat-label {
          font-size: 14px;
          color: var(--el-text-color-secondary);
        }
      }

      .stat-icon {
        font-size: 48px;
        
        &.pending {
          color: var(--el-color-warning);
        }
        
        &.warning {
          color: var(--el-color-warning);
        }
        
        &.success {
          color: var(--el-color-success);
        }
        
        &.info {
          color: var(--el-color-info);
        }
      }
    }
  }

  .filter-card {
    margin-bottom: 24px;
  }

  .table-card {
    margin-bottom: 24px;

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .user-details {
        .username {
          font-weight: 500;
          margin-bottom: 4px;
        }

        .email {
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }
      }
    }

    .amount-info {
      .amount {
        font-weight: 600;
        font-size: 16px;
        color: var(--el-color-primary);
        margin-bottom: 4px;
      }

      .fee {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .payment-info {
      .method {
        font-weight: 500;
        margin-bottom: 4px;
      }

      .account {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .wait-time {
      &.normal {
        color: var(--el-text-color-primary);
      }

      &.warning {
        color: var(--el-color-warning);
      }

      &.urgent {
        color: var(--el-color-danger);
        font-weight: 600;
      }
    }

    .pagination-wrapper {
      margin-top: 24px;
      text-align: center;
    }
  }

  .card-view {
    .withdrawal-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
      gap: 20px;

      .withdrawal-card {
        border: 1px solid var(--el-border-color);
        border-radius: 8px;
        padding: 20px;
        background: var(--el-bg-color);
        transition: all 0.3s ease;

        &:hover {
          box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
        }

        &.high-priority {
          border-left: 4px solid var(--el-color-danger);
        }

        &.medium-priority {
          border-left: 4px solid var(--el-color-warning);
        }

        &.low-priority {
          border-left: 4px solid var(--el-color-success);
        }

        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;

          .card-id {
            font-weight: 600;
            font-size: 16px;
          }
        }

        .card-body {
          .user-section {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;

            .user-info {
              .username {
                font-weight: 500;
                margin-bottom: 4px;
              }

              .email {
                font-size: 12px;
                color: var(--el-text-color-secondary);
              }
            }
          }

          .amount-section {
            margin-bottom: 16px;

            .amount-label {
              font-size: 14px;
              color: var(--el-text-color-secondary);
              margin-bottom: 4px;
            }

            .amount-value {
              font-size: 24px;
              font-weight: 600;
              color: var(--el-color-primary);
              margin-bottom: 4px;
            }

            .fee-info {
              font-size: 12px;
              color: var(--el-text-color-secondary);
            }
          }

          .payment-section {
            margin-bottom: 16px;

            .payment-method {
              font-weight: 500;
              margin-bottom: 4px;
            }

            .payment-account {
              font-size: 12px;
              color: var(--el-text-color-secondary);
            }
          }

          .time-section {
            .submit-time {
              font-size: 12px;
              color: var(--el-text-color-secondary);
              margin-bottom: 4px;
            }

            .wait-time {
              font-size: 12px;
            }
          }
        }

        .card-footer {
          display: flex;
          gap: 8px;
          margin-top: 16px;
          padding-top: 16px;
          border-top: 1px solid var(--el-border-color-lighter);
        }
      }
    }
  }

  .batch-actions {
    position: sticky;
    bottom: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--el-color-primary-light-9);
    border: 1px solid var(--el-color-primary);

    .batch-info {
      font-weight: 500;
      color: var(--el-color-primary-dark-2);
    }

    .batch-buttons {
      display: flex;
      gap: 12px;
    }
  }

  .review-content {
    .risk-assessment {
      margin: 24px 0;

      h4 {
        margin-bottom: 12px;
      }
    }

    .review-form {
      margin-top: 24px;

      h4 {
        margin-bottom: 16px;
      }
    }
  }
}

@media (max-width: 768px) {
  .withdrawal-pending-view {
    .stats-row {
      .el-col {
        margin-bottom: 16px;
      }
    }

    .card-view {
      .withdrawal-cards {
        grid-template-columns: 1fr;
      }
    }

    .batch-actions {
      flex-direction: column;
      gap: 16px;
      align-items: flex-start !important;
    }
  }
}
</style>