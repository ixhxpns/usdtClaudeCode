<template>
  <div class="kyc-management-view">
    <!-- 統計卡片 -->
    <div class="stats-section">
      <div class="stats-grid">
        <div class="stat-card pending">
          <div class="stat-icon">
            <i class="icon-clock"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.pendingReview || 0 }}</div>
            <div class="stat-label">待審核</div>
          </div>
        </div>
        
        <div class="stat-card reviewing">
          <div class="stat-icon">
            <i class="icon-eye"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.underReview || 0 }}</div>
            <div class="stat-label">審核中</div>
          </div>
        </div>
        
        <div class="stat-card approved">
          <div class="stat-icon">
            <i class="icon-check"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.approved || 0 }}</div>
            <div class="stat-label">已通過</div>
          </div>
        </div>
        
        <div class="stat-card rejected">
          <div class="stat-icon">
            <i class="icon-x"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.rejected || 0 }}</div>
            <div class="stat-label">已拒絕</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 篩選和操作 -->
    <div class="filter-section">
      <div class="filter-card">
        <div class="filter-left">
          <div class="filter-tabs">
            <button 
              v-for="tab in filterTabs"
              :key="tab.value"
              :class="['filter-tab', { active: activeFilter.status === tab.value }]"
              @click="setStatusFilter(tab.value)"
            >
              {{ tab.label }}
              <span v-if="tab.count > 0" class="tab-badge">{{ tab.count }}</span>
            </button>
          </div>
          
          <div class="filter-controls">
            <select v-model="activeFilter.riskLevel" @change="loadKycApplications">
              <option value="">所有風險等級</option>
              <option value="1">極低風險</option>
              <option value="2">低風險</option>
              <option value="3">中風險</option>
              <option value="4">高風險</option>
              <option value="5">極高風險</option>
            </select>
            
            <input 
              v-model="activeFilter.startDate" 
              type="date" 
              @change="loadKycApplications"
            >
            
            <input 
              v-model="activeFilter.endDate" 
              type="date" 
              @change="loadKycApplications"
            >
          </div>
        </div>
        
        <div class="filter-right">
          <button class="refresh-btn" @click="refreshData" :disabled="loading">
            <i class="icon-refresh" :class="{ spinning: loading }"></i>
            刷新
          </button>
          
          <button class="batch-btn" @click="showBatchReviewModal = true" :disabled="selectedApplications.length === 0">
            <i class="icon-list"></i>
            批量處理 ({{ selectedApplications.length }})
          </button>
        </div>
      </div>
    </div>

    <!-- KYC申請列表 -->
    <div class="applications-section">
      <div class="applications-card">
        <div v-if="loading && applications.length === 0" class="loading-state">
          <div class="loading-spinner"></div>
          <p>載入中...</p>
        </div>
        
        <div v-else-if="applications.length > 0" class="applications-table">
          <div class="table-header">
            <div class="header-cell">
              <input 
                type="checkbox" 
                :checked="isAllSelected"
                @change="toggleSelectAll"
              >
            </div>
            <div class="header-cell">用戶信息</div>
            <div class="header-cell">提交時間</div>
            <div class="header-cell">風險等級</div>
            <div class="header-cell">狀態</div>
            <div class="header-cell">操作</div>
          </div>
          
          <div class="table-body">
            <div 
              v-for="application in applications"
              :key="application.id"
              class="table-row"
            >
              <div class="body-cell">
                <input 
                  type="checkbox" 
                  :value="application.id"
                  v-model="selectedApplications"
                >
              </div>
              
              <div class="body-cell">
                <div class="user-info">
                  <div class="user-avatar">
                    <span>{{ getUserInitial(application.userName) }}</span>
                  </div>
                  <div class="user-details">
                    <div class="user-name">{{ application.userName || '未填寫' }}</div>
                    <div class="user-id">ID: {{ application.userId }}</div>
                    <div class="user-email">{{ application.userEmail }}</div>
                  </div>
                </div>
              </div>
              
              <div class="body-cell">
                <div class="submit-time">
                  <div class="date">{{ formatDate(application.createdAt) }}</div>
                  <div class="time">{{ formatTime(application.createdAt) }}</div>
                </div>
              </div>
              
              <div class="body-cell">
                <div class="risk-level" :class="getRiskLevelClass(application.riskLevel)">
                  <div class="risk-indicator"></div>
                  <span>{{ getRiskLevelText(application.riskLevel) }}</span>
                </div>
              </div>
              
              <div class="body-cell">
                <div class="status-badge" :class="application.status.toLowerCase()">
                  {{ getStatusText(application.status) }}
                </div>
              </div>
              
              <div class="body-cell">
                <div class="actions">
                  <button 
                    class="action-btn view-btn"
                    @click="viewApplication(application)"
                  >
                    查看
                  </button>
                  
                  <button 
                    v-if="canReview(application.status)"
                    class="action-btn review-btn"
                    @click="reviewApplication(application)"
                  >
                    審核
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-else class="empty-state">
          <div class="empty-icon">
            <i class="icon-folder"></i>
          </div>
          <h3>暫無KYC申請</h3>
          <p>當前沒有符合條件的KYC申請記錄</p>
        </div>

        <!-- 分頁 -->
        <div v-if="applications.length > 0 && pagination.total > pagination.pageSize" class="pagination">
          <div class="pagination-info">
            顯示第 {{ (pagination.current - 1) * pagination.pageSize + 1 }} - 
            {{ Math.min(pagination.current * pagination.pageSize, pagination.total) }} 項，
            共 {{ pagination.total }} 項
          </div>
          
          <div class="pagination-controls">
            <button 
              :disabled="pagination.current <= 1"
              @click="changePage(pagination.current - 1)"
            >
              上一頁
            </button>
            
            <span class="page-numbers">
              <button 
                v-for="page in visiblePages"
                :key="page"
                :class="['page-btn', { active: page === pagination.current }]"
                @click="changePage(page)"
              >
                {{ page }}
              </button>
            </span>
            
            <button 
              :disabled="pagination.current >= pagination.pages"
              @click="changePage(pagination.current + 1)"
            >
              下一頁
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- KYC詳情彈窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal kyc-detail-modal" @click.stop>
        <div class="modal-header">
          <h3>KYC申請詳情</h3>
          <button class="close-btn" @click="closeDetailModal">×</button>
        </div>
        
        <div class="modal-body" v-if="selectedApplication">
          <!-- 用戶基本信息 -->
          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <label>姓名</label>
                <span>{{ selectedApplication.realName || '未填寫' }}</span>
              </div>
              <div class="info-item">
                <label>性別</label>
                <span>{{ getGenderText(selectedApplication.gender) }}</span>
              </div>
              <div class="info-item">
                <label>出生日期</label>
                <span>{{ selectedApplication.birthDate || '未填寫' }}</span>
              </div>
              <div class="info-item">
                <label>身份證號</label>
                <span>{{ maskIdNumber(selectedApplication.idNumber) }}</span>
              </div>
              <div class="info-item">
                <label>手機號碼</label>
                <span>{{ selectedApplication.phone || '未填寫' }}</span>
              </div>
              <div class="info-item full-width">
                <label>居住地址</label>
                <span>{{ selectedApplication.address || '未填寫' }}</span>
              </div>
            </div>
          </div>
          
          <!-- 身份證件 -->
          <div class="detail-section">
            <h4>身份證件</h4>
            <div class="documents-grid">
              <div class="document-item">
                <label>身份證正面</label>
                <div class="document-preview">
                  <img 
                    v-if="selectedApplication.idFrontUrl" 
                    :src="selectedApplication.idFrontUrl" 
                    alt="身份證正面"
                    @click="previewImage(selectedApplication.idFrontUrl)"
                  >
                  <div v-else class="no-document">未上傳</div>
                </div>
              </div>
              
              <div class="document-item">
                <label>身份證背面</label>
                <div class="document-preview">
                  <img 
                    v-if="selectedApplication.idBackUrl" 
                    :src="selectedApplication.idBackUrl" 
                    alt="身份證背面"
                    @click="previewImage(selectedApplication.idBackUrl)"
                  >
                  <div v-else class="no-document">未上傳</div>
                </div>
              </div>
              
              <div class="document-item">
                <label>手持身份證自拍</label>
                <div class="document-preview">
                  <img 
                    v-if="selectedApplication.selfieUrl" 
                    :src="selectedApplication.selfieUrl" 
                    alt="手持身份證自拍"
                    @click="previewImage(selectedApplication.selfieUrl)"
                  >
                  <div v-else class="no-document">未上傳</div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 風險評估 -->
          <div class="detail-section">
            <h4>風險評估</h4>
            <div class="risk-assessment">
              <div class="risk-score">
                <label>風險等級</label>
                <div class="risk-level" :class="getRiskLevelClass(selectedApplication.riskLevel)">
                  <div class="risk-indicator"></div>
                  <span>{{ getRiskLevelText(selectedApplication.riskLevel) }}</span>
                </div>
              </div>
              <div v-if="selectedApplication.riskFactors" class="risk-factors">
                <label>風險因子</label>
                <ul>
                  <li v-for="factor in selectedApplication.riskFactors" :key="factor">
                    {{ factor }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
          
          <!-- 審核歷史 -->
          <div v-if="selectedApplication.reviewHistory && selectedApplication.reviewHistory.length > 0" class="detail-section">
            <h4>審核歷史</h4>
            <div class="review-history">
              <div 
                v-for="review in selectedApplication.reviewHistory" 
                :key="review.id"
                class="review-item"
              >
                <div class="review-header">
                  <span class="reviewer">{{ review.reviewerName }}</span>
                  <span class="review-time">{{ formatDateTime(review.createdAt) }}</span>
                </div>
                <div class="review-result" :class="review.result.toLowerCase()">
                  {{ getReviewResultText(review.result) }}
                </div>
                <div v-if="review.comment" class="review-comment">
                  {{ review.comment }}
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="secondary-btn" @click="closeDetailModal">
            關閉
          </button>
          <button 
            v-if="selectedApplication && canReview(selectedApplication.status)"
            class="primary-btn"
            @click="startReview"
          >
            開始審核
          </button>
        </div>
      </div>
    </div>

    <!-- 審核彈窗 -->
    <div v-if="showReviewModal" class="modal-overlay" @click="closeReviewModal">
      <div class="modal review-modal" @click.stop>
        <div class="modal-header">
          <h3>KYC審核</h3>
          <button class="close-btn" @click="closeReviewModal">×</button>
        </div>
        
        <div class="modal-body">
          <div class="review-form">
            <div class="form-group">
              <label>審核結果 *</label>
              <div class="review-options">
                <label class="review-option">
                  <input type="radio" value="APPROVED" v-model="reviewForm.result">
                  <span class="option-text approved">通過</span>
                </label>
                <label class="review-option">
                  <input type="radio" value="REJECTED" v-model="reviewForm.result">
                  <span class="option-text rejected">拒絕</span>
                </label>
                <label class="review-option">
                  <input type="radio" value="REQUIRES_SUPPLEMENT" v-model="reviewForm.result">
                  <span class="option-text supplement">需要補充材料</span>
                </label>
              </div>
            </div>
            
            <div class="form-group">
              <label>審核意見 *</label>
              <textarea 
                v-model="reviewForm.comment" 
                placeholder="請填寫詳細的審核意見"
                rows="4"
              ></textarea>
            </div>
            
            <div v-if="reviewForm.result === 'REQUIRES_SUPPLEMENT'" class="form-group">
              <label>補充材料要求</label>
              <textarea 
                v-model="reviewForm.supplementRequirement" 
                placeholder="請詳細說明需要補充的材料和要求"
                rows="3"
              ></textarea>
            </div>
            
            <div v-if="reviewForm.result === 'APPROVED'" class="form-group">
              <label>KYC等級</label>
              <select v-model="reviewForm.kycLevel">
                <option value="1">等級 1 - 基礎認證</option>
                <option value="2">等級 2 - 進階認證</option>
                <option value="3">等級 3 - 專業認證</option>
              </select>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="secondary-btn" @click="closeReviewModal">
            取消
          </button>
          <button 
            class="primary-btn"
            @click="submitReview"
            :disabled="!canSubmitReview || submitting"
          >
            {{ submitting ? '提交中...' : '提交審核' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 批量審核彈窗 -->
    <div v-if="showBatchReviewModal" class="modal-overlay" @click="showBatchReviewModal = false">
      <div class="modal batch-review-modal" @click.stop>
        <div class="modal-header">
          <h3>批量審核</h3>
          <button class="close-btn" @click="showBatchReviewModal = false">×</button>
        </div>
        
        <div class="modal-body">
          <div class="batch-info">
            <p>已選擇 <strong>{{ selectedApplications.length }}</strong> 個KYC申請進行批量處理</p>
          </div>
          
          <div class="batch-form">
            <div class="form-group">
              <label>批量操作 *</label>
              <div class="batch-options">
                <label class="batch-option">
                  <input type="radio" value="APPROVED" v-model="batchForm.result">
                  <span class="option-text approved">批量通過</span>
                </label>
                <label class="batch-option">
                  <input type="radio" value="REJECTED" v-model="batchForm.result">
                  <span class="option-text rejected">批量拒絕</span>
                </label>
              </div>
            </div>
            
            <div class="form-group">
              <label>批量審核意見 *</label>
              <textarea 
                v-model="batchForm.comment" 
                placeholder="請填寫批量操作的統一意見"
                rows="3"
              ></textarea>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="secondary-btn" @click="showBatchReviewModal = false">
            取消
          </button>
          <button 
            class="primary-btn"
            @click="submitBatchReview"
            :disabled="!canSubmitBatchReview || batchSubmitting"
          >
            {{ batchSubmitting ? '處理中...' : '確認批量處理' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '@/utils/http'

export default {
  name: 'KycManagementView',
  setup() {
    // 響應式數據
    const loading = ref(false)
    const applications = ref([])
    const selectedApplications = ref([])
    const selectedApplication = ref(null)
    const showDetailModal = ref(false)
    const showReviewModal = ref(false)
    const showBatchReviewModal = ref(false)
    const submitting = ref(false)
    const batchSubmitting = ref(false)
    
    const stats = reactive({
      pendingReview: 0,
      underReview: 0,
      approved: 0,
      rejected: 0
    })
    
    const activeFilter = reactive({
      status: '',
      riskLevel: '',
      startDate: '',
      endDate: ''
    })
    
    const pagination = reactive({
      current: 1,
      pageSize: 20,
      total: 0,
      pages: 0
    })
    
    const reviewForm = reactive({
      result: '',
      comment: '',
      supplementRequirement: '',
      kycLevel: '1'
    })
    
    const batchForm = reactive({
      result: '',
      comment: ''
    })
    
    // 計算屬性
    const isAllSelected = computed(() => {
      return applications.value.length > 0 && 
             selectedApplications.value.length === applications.value.length
    })
    
    const canSubmitReview = computed(() => {
      return reviewForm.result && reviewForm.comment
    })
    
    const canSubmitBatchReview = computed(() => {
      return batchForm.result && batchForm.comment
    })
    
    const visiblePages = computed(() => {
      const start = Math.max(1, pagination.current - 2)
      const end = Math.min(pagination.pages, pagination.current + 2)
      const pages = []
      
      for (let i = start; i <= end; i++) {
        pages.push(i)
      }
      
      return pages
    })
    
    // 靜態數據
    const filterTabs = [
      { label: '全部', value: '', count: 0 },
      { label: '待審核', value: 'PENDING', count: 0 },
      { label: '審核中', value: 'UNDER_REVIEW', count: 0 },
      { label: '已通過', value: 'APPROVED', count: 0 },
      { label: '已拒絕', value: 'REJECTED', count: 0 }
    ]
    
    // 方法
    const formatDate = (dateTime) => {
      return new Date(dateTime).toLocaleDateString('zh-TW')
    }
    
    const formatTime = (dateTime) => {
      return new Date(dateTime).toLocaleTimeString('zh-TW', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
    
    const formatDateTime = (dateTime) => {
      return new Date(dateTime).toLocaleString('zh-TW')
    }
    
    const getUserInitial = (name) => {
      return name ? name.charAt(0).toUpperCase() : 'U'
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'PENDING': '待審核',
        'UNDER_REVIEW': '審核中',
        'APPROVED': '已通過',
        'REJECTED': '已拒絕',
        'REQUIRES_SUPPLEMENT': '需要補充'
      }
      return statusMap[status] || status
    }
    
    const getRiskLevelClass = (level) => {
      if (level <= 2) return 'low'
      if (level <= 3) return 'medium'
      return 'high'
    }
    
    const getRiskLevelText = (level) => {
      const levelMap = {
        1: '極低風險',
        2: '低風險',
        3: '中風險',
        4: '高風險',
        5: '極高風險'
      }
      return levelMap[level] || `等級 ${level}`
    }
    
    const getGenderText = (gender) => {
      const genderMap = {
        'MALE': '男',
        'FEMALE': '女'
      }
      return genderMap[gender] || '未填寫'
    }
    
    const getReviewResultText = (result) => {
      const resultMap = {
        'APPROVED': '通過',
        'REJECTED': '拒絕',
        'REQUIRES_SUPPLEMENT': '需要補充材料'
      }
      return resultMap[result] || result
    }
    
    const maskIdNumber = (idNumber) => {
      if (!idNumber || idNumber.length < 6) return idNumber
      return idNumber.substring(0, 3) + '***' + idNumber.substring(idNumber.length - 3)
    }
    
    const canReview = (status) => {
      return ['PENDING', 'REQUIRES_SUPPLEMENT'].includes(status)
    }
    
    const setStatusFilter = (status) => {
      activeFilter.status = status
      pagination.current = 1
      loadKycApplications()
    }
    
    const changePage = (page) => {
      pagination.current = page
      loadKycApplications()
    }
    
    const toggleSelectAll = () => {
      if (isAllSelected.value) {
        selectedApplications.value = []
      } else {
        selectedApplications.value = applications.value.map(app => app.id)
      }
    }
    
    const viewApplication = (application) => {
      selectedApplication.value = application
      showDetailModal.value = true
    }
    
    const reviewApplication = (application) => {
      selectedApplication.value = application
      // 重置表單
      Object.assign(reviewForm, {
        result: '',
        comment: '',
        supplementRequirement: '',
        kycLevel: '1'
      })
      showReviewModal.value = true
    }
    
    const startReview = () => {
      showDetailModal.value = false
      showReviewModal.value = true
    }
    
    const closeDetailModal = () => {
      showDetailModal.value = false
      selectedApplication.value = null
    }
    
    const closeReviewModal = () => {
      showReviewModal.value = false
      selectedApplication.value = null
    }
    
    const previewImage = (imageUrl) => {
      // 可以實現圖片預覽功能
      window.open(imageUrl, '_blank')
    }
    
    // API 調用
    const loadKycApplications = async () => {
      loading.value = true
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        }
        
        if (activeFilter.status) {
          params.status = activeFilter.status
        }
        
        if (activeFilter.riskLevel) {
          params.riskLevel = activeFilter.riskLevel
        }
        
        if (activeFilter.startDate) {
          params.startDate = activeFilter.startDate
        }
        
        if (activeFilter.endDate) {
          params.endDate = activeFilter.endDate
        }
        
        const response = await api.get('/admin/kyc/applications', { params })
        
        if (response.data.success) {
          const data = response.data.data
          applications.value = data.records
          pagination.total = data.total
          pagination.pages = data.pages
        }
      } catch (error) {
        console.error('載入KYC申請失敗:', error)
      } finally {
        loading.value = false
      }
    }
    
    const loadStats = async () => {
      try {
        const response = await api.get('/admin/kyc/pending-count')
        if (response.data.success) {
          Object.assign(stats, response.data.data)
        }
      } catch (error) {
        console.error('載入統計數據失敗:', error)
      }
    }
    
    const refreshData = () => {
      loadKycApplications()
      loadStats()
    }
    
    const submitReview = async () => {
      if (!canSubmitReview.value) return
      
      submitting.value = true
      try {
        const response = await api.post(`/admin/kyc/${selectedApplication.value.id}/review`, {
          result: reviewForm.result,
          comment: reviewForm.comment,
          supplementRequirement: reviewForm.supplementRequirement,
          kycLevel: reviewForm.kycLevel
        })
        
        if (response.data.success) {
          showReviewModal.value = false
          refreshData()
          alert('審核提交成功')
        } else {
          alert('審核提交失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('提交審核失敗:', error)
        alert('審核提交失敗，請稍後重試')
      } finally {
        submitting.value = false
      }
    }
    
    const submitBatchReview = async () => {
      if (!canSubmitBatchReview.value) return
      
      batchSubmitting.value = true
      try {
        const response = await api.post('/admin/kyc/batch-review', {
          kycIds: selectedApplications.value,
          result: batchForm.result,
          comment: batchForm.comment
        })
        
        if (response.data.success) {
          showBatchReviewModal.value = false
          selectedApplications.value = []
          refreshData()
          alert('批量處理成功')
        } else {
          alert('批量處理失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('批量審核失敗:', error)
        alert('批量處理失敗，請稍後重試')
      } finally {
        batchSubmitting.value = false
      }
    }
    
    // 生命週期
    onMounted(() => {
      loadKycApplications()
      loadStats()
    })
    
    return {
      loading,
      applications,
      selectedApplications,
      selectedApplication,
      showDetailModal,
      showReviewModal,
      showBatchReviewModal,
      submitting,
      batchSubmitting,
      stats,
      activeFilter,
      pagination,
      reviewForm,
      batchForm,
      isAllSelected,
      canSubmitReview,
      canSubmitBatchReview,
      visiblePages,
      filterTabs,
      formatDate,
      formatTime,
      formatDateTime,
      getUserInitial,
      getStatusText,
      getRiskLevelClass,
      getRiskLevelText,
      getGenderText,
      getReviewResultText,
      maskIdNumber,
      canReview,
      setStatusFilter,
      changePage,
      toggleSelectAll,
      viewApplication,
      reviewApplication,
      startReview,
      closeDetailModal,
      closeReviewModal,
      previewImage,
      refreshData,
      submitReview,
      submitBatchReview
    }
  }
}
</script>

<style scoped>
.kyc-management-view {
  padding: 24px;
}

.stats-section {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  border-left: 4px solid;
}

.stat-card.pending {
  border-left-color: #f59e0b;
}

.stat-card.reviewing {
  border-left-color: #2563eb;
}

.stat-card.approved {
  border-left-color: #10b981;
}

.stat-card.rejected {
  border-left-color: #ef4444;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-card.pending .stat-icon {
  background: #fef3c7;
  color: #d97706;
}

.stat-card.reviewing .stat-icon {
  background: #dbeafe;
  color: #2563eb;
}

.stat-card.approved .stat-icon {
  background: #d1fae5;
  color: #059669;
}

.stat-card.rejected .stat-icon {
  background: #fee2e2;
  color: #dc2626;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.filter-section {
  margin-bottom: 24px;
}

.filter-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.filter-tab:hover {
  background: #e5e7eb;
}

.filter-tab.active {
  background: #2563eb;
  color: white;
}

.tab-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.filter-controls {
  display: flex;
  gap: 12px;
}

.filter-controls select,
.filter-controls input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.filter-right {
  display: flex;
  gap: 12px;
}

.refresh-btn,
.batch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.refresh-btn:hover,
.batch-btn:hover {
  background: #f9fafb;
}

.batch-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon-refresh.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.applications-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #6b7280;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f4f6;
  border-top: 3px solid #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.applications-table {
  width: 100%;
}

.table-header {
  display: grid;
  grid-template-columns: 50px 300px 150px 120px 100px 120px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.header-cell {
  padding: 16px 12px;
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.table-body {
  display: flex;
  flex-direction: column;
}

.table-row {
  display: grid;
  grid-template-columns: 50px 300px 150px 120px 100px 120px;
  border-bottom: 1px solid #f3f4f6;
  transition: all 0.2s;
}

.table-row:hover {
  background: #f9fafb;
}

.body-cell {
  padding: 16px 12px;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  font-weight: 600;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 2px;
}

.user-id {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
}

.user-email {
  font-size: 12px;
  color: #9ca3af;
}

.submit-time {
  text-align: center;
}

.date {
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 2px;
  font-size: 14px;
}

.time {
  font-size: 12px;
  color: #6b7280;
}

.risk-level {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
}

.risk-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.risk-level.low .risk-indicator {
  background: #10b981;
}

.risk-level.medium .risk-indicator {
  background: #f59e0b;
}

.risk-level.high .risk-indicator {
  background: #ef4444;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.under_review {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge.approved {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.view-btn {
  color: #2563eb;
  border-color: #bfdbfe;
}

.view-btn:hover {
  background: #eff6ff;
}

.review-btn {
  color: #059669;
  border-color: #a7f3d0;
}

.review-btn:hover {
  background: #ecfdf5;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #6b7280;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #d1d5db;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  color: #374151;
}

.empty-state p {
  margin: 0;
  color: #9ca3af;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-top: 1px solid #e5e7eb;
}

.pagination-info {
  font-size: 14px;
  color: #6b7280;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-controls button {
  padding: 8px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.pagination-controls button:hover:not(:disabled) {
  background: #f9fafb;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn.active {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}

/* 彈窗樣式 */
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

.modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  color: #1a1a1a;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6b7280;
}

.modal-body {
  padding: 24px;
}

.detail-section {
  margin-bottom: 32px;
}

.detail-section h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.info-item span {
  color: #1a1a1a;
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.document-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.document-preview {
  width: 100%;
  height: 150px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.document-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.document-preview img:hover {
  transform: scale(1.05);
}

.no-document {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  background: #f9fafb;
}

.risk-assessment {
  display: flex;
  gap: 32px;
}

.risk-score {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.risk-score label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.risk-factors {
  flex: 1;
}

.risk-factors label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.risk-factors ul {
  margin: 0;
  padding-left: 20px;
  color: #6b7280;
}

.review-history {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #e5e7eb;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.reviewer {
  font-weight: 500;
  color: #1a1a1a;
}

.review-time {
  font-size: 12px;
  color: #6b7280;
}

.review-result {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
  margin-bottom: 8px;
}

.review-result.approved {
  background: #d1fae5;
  color: #065f46;
}

.review-result.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.review-comment {
  color: #4b5563;
  font-size: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e5e7eb;
}

.secondary-btn {
  padding: 8px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn {
  padding: 8px 16px;
  background: #2563eb;
  color: white;
  border: 1px solid #2563eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.primary-btn:hover {
  background: #1d4ed8;
}

.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.review-options,
.batch-options {
  display: flex;
  gap: 16px;
}

.review-option,
.batch-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.option-text {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}

.option-text.approved {
  background: #d1fae5;
  color: #065f46;
}

.option-text.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.option-text.supplement {
  background: #fef3c7;
  color: #92400e;
}

.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
}

.batch-info {
  background: #eff6ff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #2563eb;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .table-header,
  .table-row {
    grid-template-columns: 50px 1fr 120px 100px 100px;
  }
  
  .body-cell:nth-child(3) {
    display: none;
  }
}

@media (max-width: 768px) {
  .kyc-management-view {
    padding: 16px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-card {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-left {
    flex-direction: column;
    gap: 16px;
  }
  
  .modal {
    width: 95%;
  }
  
  .documents-grid {
    grid-template-columns: 1fr;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>