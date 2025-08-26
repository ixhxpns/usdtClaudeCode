<template>
  <div class="kyc-reviewed-view">
    <div class="page-header">
      <h1>KYC审核记录</h1>
      <p class="page-description">查看已完成的KYC审核记录</p>
    </div>

    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-group">
          <label>审核状态:</label>
          <select v-model="filters.status" @change="loadData">
            <option value="">全部</option>
            <option value="approved">已通过</option>
            <option value="rejected">已拒绝</option>
          </select>
        </div>

        <div class="filter-group">
          <label>审核时间:</label>
          <input 
            type="date" 
            v-model="filters.reviewDate" 
            @change="loadData"
          />
        </div>

        <div class="filter-group">
          <label>用户ID:</label>
          <input 
            type="text" 
            v-model="filters.userId" 
            @keyup.enter="loadData"
            placeholder="输入用户ID"
          />
        </div>

        <button @click="loadData" class="btn btn-primary">搜索</button>
        <button @click="resetFilters" class="btn btn-secondary">重置</button>
      </div>
    </div>

    <div class="table-section">
      <div class="table-header">
        <h3>审核记录列表</h3>
        <div class="table-actions">
          <button @click="exportData" class="btn btn-outline">导出</button>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>KYC ID</th>
              <th>用户信息</th>
              <th>提交时间</th>
              <th>审核时间</th>
              <th>审核结果</th>
              <th>审核人员</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in reviewedList" :key="record.id">
              <td>{{ record.id }}</td>
              <td>
                <div class="user-info">
                  <div class="user-name">{{ record.user?.username }}</div>
                  <div class="user-id">ID: {{ record.userId }}</div>
                  <div class="user-email">{{ record.user?.email }}</div>
                </div>
              </td>
              <td>{{ formatDateTime(record.createdAt) }}</td>
              <td>{{ formatDateTime(record.reviewedAt) }}</td>
              <td>
                <span class="status-badge" :class="`status-${record.status}`">
                  {{ getStatusText(record.status) }}
                </span>
              </td>
              <td>{{ record.reviewer?.username || '系统' }}</td>
              <td>
                <div class="review-note" v-if="record.reviewNote">
                  {{ record.reviewNote }}
                </div>
                <div class="rejection-reason" v-if="record.rejectionReason">
                  <strong>拒绝原因:</strong> {{ record.rejectionReason }}
                </div>
              </td>
              <td>
                <div class="action-buttons">
                  <button @click="viewDetails(record)" class="btn btn-sm btn-primary">
                    查看详情
                  </button>
                  <button 
                    v-if="record.status === 'rejected'"
                    @click="viewResubmission(record)" 
                    class="btn btn-sm btn-outline"
                  >
                    重新审核
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-if="!loading && reviewedList.length === 0" class="empty-state">
          <p>暂无审核记录</p>
        </div>
      </div>

      <div class="pagination" v-if="total > pageSize">
        <button 
          @click="changePage(currentPage - 1)" 
          :disabled="currentPage <= 1"
          class="btn btn-outline btn-sm"
        >
          上一页
        </button>
        
        <span class="pagination-info">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页
        </span>
        
        <button 
          @click="changePage(currentPage + 1)" 
          :disabled="currentPage >= totalPages"
          class="btn btn-outline btn-sm"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>KYC审核详情</h3>
          <button @click="closeDetailModal" class="btn-close">&times;</button>
        </div>
        
        <div class="modal-body" v-if="selectedRecord">
          <div class="detail-section">
            <h4>用户信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>用户ID:</label>
                <span>{{ selectedRecord.userId }}</span>
              </div>
              <div class="detail-item">
                <label>真实姓名:</label>
                <span>{{ selectedRecord.realName }}</span>
              </div>
              <div class="detail-item">
                <label>证件号码:</label>
                <span>{{ maskIdNumber(selectedRecord.idNumber) }}</span>
              </div>
              <div class="detail-item">
                <label>国籍:</label>
                <span>{{ selectedRecord.nationality }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>审核信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>提交时间:</label>
                <span>{{ formatDateTime(selectedRecord.createdAt) }}</span>
              </div>
              <div class="detail-item">
                <label>审核时间:</label>
                <span>{{ formatDateTime(selectedRecord.reviewedAt) }}</span>
              </div>
              <div class="detail-item">
                <label>审核结果:</label>
                <span class="status-badge" :class="`status-${selectedRecord.status}`">
                  {{ getStatusText(selectedRecord.status) }}
                </span>
              </div>
              <div class="detail-item">
                <label>审核人员:</label>
                <span>{{ selectedRecord.reviewer?.username || '系统' }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section" v-if="selectedRecord.reviewNote">
            <h4>审核备注</h4>
            <p>{{ selectedRecord.reviewNote }}</p>
          </div>

          <div class="detail-section" v-if="selectedRecord.rejectionReason">
            <h4>拒绝原因</h4>
            <p>{{ selectedRecord.rejectionReason }}</p>
          </div>

          <div class="detail-section" v-if="selectedRecord.riskAssessment">
            <h4>风险评估</h4>
            <div class="risk-info">
              <div class="risk-score">
                风险分数: {{ selectedRecord.riskAssessment.riskScore }}
              </div>
              <div class="risk-level">
                风险等级: {{ selectedRecord.riskAssessment.riskLevel }}
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="closeDetailModal" class="btn btn-secondary">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { AdminApi } from '@/api'

interface KycRecord {
  id: number
  userId: number
  realName: string
  idNumber: string
  nationality: string
  status: string
  reviewNote?: string
  rejectionReason?: string
  createdAt: string
  reviewedAt?: string
  user?: {
    id: number
    username: string
    email: string
  }
  reviewer?: {
    id: number
    username: string
  }
  riskAssessment?: {
    riskScore: number
    riskLevel: string
  }
}

const router = useRouter()
const loading = ref(false)
const reviewedList = ref<KycRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetailModal = ref(false)
const selectedRecord = ref<KycRecord | null>(null)

const filters = reactive({
  status: '',
  reviewDate: '',
  userId: ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...filters
    }
    
    const response = await AdminApi.getReviewedKyc(params)
    if (response.success) {
      reviewedList.value = response.data.records || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载KYC审核记录失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置过滤器
const resetFilters = () => {
  filters.status = ''
  filters.reviewDate = ''
  filters.userId = ''
  currentPage.value = 1
  loadData()
}

// 分页
const changePage = (page: number) => {
  currentPage.value = page
  loadData()
}

// 查看详情
const viewDetails = (record: KycRecord) => {
  selectedRecord.value = record
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedRecord.value = null
}

// 查看重新提交
const viewResubmission = (record: KycRecord) => {
  router.push(`/kyc/review/${record.id}`)
}

// 导出数据
const exportData = async () => {
  try {
    const params = { ...filters, export: true }
    const response = await AdminApi.exportReviewedKyc(params)
    // 处理文件下载
    const blob = new Blob([response.data], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `kyc_reviewed_${new Date().getTime()}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('导出失败:', error)
  }
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    approved: '已通过',
    rejected: '已拒绝',
    completed: '已完成'
  }
  return statusMap[status] || status
}

// 脱敏身份证号
const maskIdNumber = (idNumber: string) => {
  if (!idNumber) return '-'
  return idNumber.replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.kyc-reviewed-view {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: $text-primary;
}

.page-description {
  color: $text-secondary;
  margin: 0;
}

.filter-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: end;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  min-width: 120px;
}

.filter-group label {
  margin-bottom: 4px;
  font-weight: 500;
  color: $text-primary;
}

.filter-group input,
.filter-group select {
  padding: 8px 12px;
  border: 1px solid $border-color;
  border-radius: 4px;
  font-size: 14px;
}

.table-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid $border-color;
}

.table-header h3 {
  margin: 0;
  color: $text-primary;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid $border-light;
}

.data-table th {
  background: $background-light;
  font-weight: 500;
  color: $text-primary;
}

.user-info {
  min-width: 160px;
}

.user-name {
  font-weight: 500;
  color: $text-primary;
}

.user-id,
.user-email {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-approved {
  background: $status-success-bg;
  color: $status-success;
}

.status-rejected {
  background: $status-error-bg;
  color: $status-error;
}

.review-note,
.rejection-reason {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: $text-secondary;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid $border-light;
  border-top: 2px solid $admin-primary;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid $border-color;
}

.pagination-info {
  color: $text-secondary;
  font-size: 14px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid $border-color;
}

.modal-header h3 {
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: $text-secondary;
}

.modal-body {
  padding: 20px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin: 0 0 16px 0;
  color: $text-primary;
  font-size: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-item label {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.detail-item span {
  color: $text-primary;
}

.risk-info {
  display: flex;
  gap: 24px;
  align-items: center;
}

.risk-score,
.risk-level {
  padding: 8px 12px;
  border-radius: 4px;
  background: $background-light;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
  border-top: 1px solid $border-color;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: $admin-primary;
  color: white;
}

.btn-primary:hover {
  background: darken($admin-primary, 10%);
}

.btn-secondary {
  background: $admin-secondary;
  color: white;
}

.btn-secondary:hover {
  background: darken($admin-secondary, 10%);
}

.btn-outline {
  background: white;
  color: $admin-primary;
  border: 1px solid $admin-primary;
}

.btn-outline:hover {
  background: $admin-primary;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}
</style>