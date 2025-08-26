<template>
  <div class="orders-view">
    <!-- 訂單篩選 -->
    <div class="filter-section">
      <div class="filter-card">
        <div class="filter-header">
          <h3>訂單記錄</h3>
          <div class="filter-stats">
            <span class="stats-item">
              總訂單: <strong>{{ totalOrders }}</strong>
            </span>
            <span class="stats-item">
              本月成交: <strong>{{ monthlyOrders }}</strong>
            </span>
          </div>
        </div>
        
        <div class="filter-controls">
          <div class="filter-tabs">
            <button 
              v-for="tab in filterTabs"
              :key="tab.value"
              :class="['filter-tab', { active: activeFilter.type === tab.value }]"
              @click="setActiveFilter('type', tab.value)"
            >
              {{ tab.label }}
              <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
            </button>
          </div>
          
          <div class="filter-options">
            <select v-model="activeFilter.status" @change="loadOrders">
              <option value="">所有狀態</option>
              <option value="PENDING">待處理</option>
              <option value="PROCESSING">處理中</option>
              <option value="COMPLETED">已完成</option>
              <option value="CANCELLED">已取消</option>
              <option value="FAILED">失敗</option>
            </select>
            
            <input 
              v-model="activeFilter.startDate" 
              type="date" 
              @change="loadOrders"
            >
            
            <input 
              v-model="activeFilter.endDate" 
              type="date" 
              @change="loadOrders"
            >
            
            <button class="refresh-btn" @click="refreshOrders" :disabled="loading">
              <i class="icon-refresh" :class="{ spinning: loading }"></i>
              刷新
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 訂單列表 -->
    <div class="orders-section">
      <div v-if="loading && orders.length === 0" class="loading-placeholder">
        <div class="loading-spinner"></div>
        <p>載入中...</p>
      </div>
      
      <div v-else-if="orders.length > 0" class="orders-list">
        <div 
          v-for="order in orders" 
          :key="order.id"
          class="order-card"
          @click="viewOrderDetail(order.id)"
        >
          <div class="order-header">
            <div class="order-basic">
              <div class="order-type" :class="order.orderType.toLowerCase()">
                {{ order.orderType === 'BUY' ? '買入' : '賣出' }}
              </div>
              <div class="order-number">{{ order.orderNumber }}</div>
              <div class="order-time">{{ formatDateTime(order.createdAt) }}</div>
            </div>
            
            <div class="order-status" :class="order.status.toLowerCase()">
              <div class="status-dot"></div>
              <span>{{ getStatusText(order.status) }}</span>
            </div>
          </div>
          
          <div class="order-content">
            <div class="order-amounts">
              <div class="amount-item">
                <span class="label">
                  {{ order.orderType === 'BUY' ? '支付金額' : '賣出數量' }}
                </span>
                <span class="value">
                  {{ order.orderType === 'BUY' ? 
                      formatPrice(order.amount) : 
                      formatCrypto(order.usdtAmount) + ' USDT' }}
                </span>
              </div>
              
              <div class="amount-item">
                <span class="label">
                  {{ order.orderType === 'BUY' ? '獲得USDT' : '獲得金額' }}
                </span>
                <span class="value">
                  {{ order.orderType === 'BUY' ? 
                      formatCrypto(order.usdtAmount) + ' USDT' : 
                      formatPrice(order.amount) }}
                </span>
              </div>
              
              <div class="amount-item">
                <span class="label">成交價格</span>
                <span class="value">{{ formatPrice(order.price) }}</span>
              </div>
            </div>
            
            <div v-if="order.paymentMethod" class="order-payment">
              <span class="label">支付方式:</span>
              <span class="value">{{ getPaymentMethodText(order.paymentMethod) }}</span>
            </div>
            
            <div v-if="order.receivingBank" class="order-bank">
              <span class="label">收款銀行:</span>
              <span class="value">{{ order.receivingBank }}</span>
            </div>
          </div>
          
          <div class="order-actions">
            <button class="detail-btn" @click.stop="viewOrderDetail(order.id)">
              查看詳情
            </button>
            
            <button 
              v-if="canCancelOrder(order)"
              class="cancel-btn"
              @click.stop="showCancelModal(order)"
            >
              取消訂單
            </button>
            
            <button 
              v-if="canConfirmPayment(order)"
              class="confirm-btn"
              @click.stop="showPaymentModal(order)"
            >
              確認支付
            </button>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-orders">
        <div class="empty-icon">
          <i class="icon-inbox"></i>
        </div>
        <h3>暫無訂單記錄</h3>
        <p>您還沒有任何交易訂單</p>
        <router-link to="/trading" class="start-trading-btn">
          開始交易
        </router-link>
      </div>

      <!-- 分頁 -->
      <div v-if="orders.length > 0 && pagination.total > pagination.pageSize" class="pagination-section">
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

    <!-- 取消訂單彈窗 -->
    <div v-if="showCancelOrderModal" class="modal-overlay" @click="showCancelOrderModal = false">
      <div class="modal cancel-modal" @click.stop>
        <div class="modal-header">
          <h3>取消訂單</h3>
          <button class="close-btn" @click="showCancelOrderModal = false">×</button>
        </div>
        
        <div class="modal-body">
          <div class="order-info">
            <p>您確定要取消以下訂單嗎？</p>
            <div class="order-summary">
              <div class="summary-item">
                <span>訂單號:</span>
                <span>{{ selectedOrder?.orderNumber }}</span>
              </div>
              <div class="summary-item">
                <span>類型:</span>
                <span>{{ selectedOrder?.orderType === 'BUY' ? '買入' : '賣出' }}</span>
              </div>
              <div class="summary-item">
                <span>金額:</span>
                <span>
                  {{ selectedOrder?.orderType === 'BUY' ? 
                      formatPrice(selectedOrder.amount) : 
                      formatCrypto(selectedOrder.usdtAmount) + ' USDT' }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label>取消原因</label>
            <select v-model="cancelReason">
              <option value="">請選擇取消原因</option>
              <option value="price_change">價格變動</option>
              <option value="payment_issue">支付問題</option>
              <option value="change_mind">改變主意</option>
              <option value="other">其他原因</option>
            </select>
          </div>
          
          <div v-if="cancelReason === 'other'" class="form-group">
            <label>詳細說明</label>
            <textarea 
              v-model="cancelReasonDetail" 
              placeholder="請詳細說明取消原因"
            ></textarea>
          </div>
          
          <div class="modal-actions">
            <button class="cancel-btn" @click="showCancelOrderModal = false">
              返回
            </button>
            <button 
              class="confirm-btn" 
              @click="confirmCancelOrder"
              :disabled="!cancelReason || cancelling"
            >
              {{ cancelling ? '取消中...' : '確認取消' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 確認支付彈窗 -->
    <div v-if="showPaymentConfirmModal" class="modal-overlay" @click="showPaymentConfirmModal = false">
      <div class="modal payment-modal" @click.stop>
        <div class="modal-header">
          <h3>確認支付</h3>
          <button class="close-btn" @click="showPaymentConfirmModal = false">×</button>
        </div>
        
        <div class="modal-body">
          <div class="payment-info">
            <h4>支付信息</h4>
            <div class="payment-details">
              <div class="detail-item">
                <span>訂單號:</span>
                <span>{{ selectedOrder?.orderNumber }}</span>
              </div>
              <div class="detail-item">
                <span>應付金額:</span>
                <span class="amount">{{ formatPrice(selectedOrder?.amount) }}</span>
              </div>
              <div class="detail-item">
                <span>支付方式:</span>
                <span>{{ getPaymentMethodText(selectedOrder?.paymentMethod) }}</span>
              </div>
            </div>
          </div>
          
          <div class="payment-proof">
            <h4>上傳支付憑證</h4>
            <div class="upload-area" @click="triggerPaymentProofUpload">
              <div v-if="!paymentProof" class="upload-placeholder">
                <i class="icon-upload"></i>
                <p>點擊上傳支付截圖或收據</p>
                <span class="upload-hint">支持 JPG、PNG，大小不超過5MB</span>
              </div>
              <div v-else class="upload-preview">
                <img :src="paymentProof.preview" alt="支付憑證">
                <button @click.stop="removePaymentProof" class="remove-btn">×</button>
              </div>
            </div>
            <input 
              ref="paymentProofInput"
              type="file" 
              accept="image/*" 
              @change="handlePaymentProofUpload"
              hidden
            >
          </div>
          
          <div class="payment-notes">
            <h4>重要提醒</h4>
            <ul>
              <li>請確保已完成實際支付</li>
              <li>上傳清晰的支付憑證截圖</li>
              <li>支付憑證將用於人工審核</li>
              <li>虛假支付憑證可能導致賬戶限制</li>
            </ul>
          </div>
          
          <div class="modal-actions">
            <button class="cancel-btn" @click="showPaymentConfirmModal = false">
              取消
            </button>
            <button 
              class="confirm-btn" 
              @click="confirmPayment"
              :disabled="!paymentProof || confirming"
            >
              {{ confirming ? '確認中...' : '確認支付' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/utils/http'

export default {
  name: 'OrdersView',
  setup() {
    const router = useRouter()
    
    // 響應式數據
    const loading = ref(false)
    const orders = ref([])
    const totalOrders = ref(0)
    const monthlyOrders = ref(0)
    const showCancelOrderModal = ref(false)
    const showPaymentConfirmModal = ref(false)
    const selectedOrder = ref(null)
    const cancelling = ref(false)
    const confirming = ref(false)
    const cancelReason = ref('')
    const cancelReasonDetail = ref('')
    const paymentProof = ref(null)
    
    const activeFilter = reactive({
      type: 'ALL',
      status: '',
      startDate: '',
      endDate: ''
    })
    
    const pagination = reactive({
      current: 1,
      pageSize: 20,
      total: 0,
      pages: 0
    })
    
    // 計算屬性
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
      { label: '全部', value: 'ALL', count: 0 },
      { label: '買入', value: 'BUY', count: 0 },
      { label: '賣出', value: 'SELL', count: 0 }
    ]
    
    // 方法
    const formatPrice = (price) => {
      return new Intl.NumberFormat('zh-TW', {
        style: 'currency',
        currency: 'TWD',
        minimumFractionDigits: 0
      }).format(price || 0)
    }
    
    const formatCrypto = (amount) => {
      return parseFloat(amount || 0).toFixed(6)
    }
    
    const formatDateTime = (dateTime) => {
      return new Date(dateTime).toLocaleString('zh-TW', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'PENDING': '待處理',
        'PROCESSING': '處理中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'FAILED': '失敗'
      }
      return statusMap[status] || status
    }
    
    const getPaymentMethodText = (method) => {
      const methodMap = {
        'bank_transfer': '銀行轉帳',
        'convenience_store': '超商付款',
        'online_payment': '線上支付'
      }
      return methodMap[method] || method
    }
    
    const canCancelOrder = (order) => {
      return ['PENDING', 'PROCESSING'].includes(order.status)
    }
    
    const canConfirmPayment = (order) => {
      return order.status === 'PENDING' && order.orderType === 'BUY'
    }
    
    const setActiveFilter = (key, value) => {
      activeFilter[key] = value
      pagination.current = 1
      loadOrders()
    }
    
    const changePage = (page) => {
      pagination.current = page
      loadOrders()
    }
    
    const viewOrderDetail = (orderId) => {
      router.push(`/orders/${orderId}`)
    }
    
    const showCancelModal = (order) => {
      selectedOrder.value = order
      cancelReason.value = ''
      cancelReasonDetail.value = ''
      showCancelOrderModal.value = true
    }
    
    const showPaymentModal = (order) => {
      selectedOrder.value = order
      paymentProof.value = null
      showPaymentConfirmModal.value = true
    }
    
    // 支付憑證處理
    const triggerPaymentProofUpload = () => {
      const input = document.querySelector('input[ref="paymentProofInput"]')
      if (input) input.click()
    }
    
    const handlePaymentProofUpload = (event) => {
      const file = event.target.files[0]
      if (!file) return
      
      if (!validateFile(file)) return
      
      const reader = new FileReader()
      reader.onload = (e) => {
        paymentProof.value = {
          file: file,
          preview: e.target.result
        }
      }
      reader.readAsDataURL(file)
    }
    
    const removePaymentProof = () => {
      paymentProof.value = null
      const input = document.querySelector('input[ref="paymentProofInput"]')
      if (input) input.value = ''
    }
    
    const validateFile = (file) => {
      if (!file.type.startsWith('image/')) {
        alert('請選擇圖片文件')
        return false
      }
      
      if (file.size > 5 * 1024 * 1024) {
        alert('文件大小不能超過5MB')
        return false
      }
      
      return true
    }
    
    // API 調用
    const loadOrders = async () => {
      loading.value = true
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        }
        
        if (activeFilter.type !== 'ALL') {
          params.orderType = activeFilter.type
        }
        
        if (activeFilter.status) {
          params.status = activeFilter.status
        }
        
        if (activeFilter.startDate) {
          params.startDate = activeFilter.startDate
        }
        
        if (activeFilter.endDate) {
          params.endDate = activeFilter.endDate
        }
        
        const response = await api.get('/orders/my', { params })
        
        if (response.data.success) {
          const data = response.data.data
          orders.value = data.records
          pagination.total = data.total
          pagination.pages = data.pages
        }
      } catch (error) {
        console.error('載入訂單失敗:', error)
      } finally {
        loading.value = false
      }
    }
    
    const refreshOrders = () => {
      loadOrders()
    }
    
    const confirmCancelOrder = async () => {
      if (!cancelReason.value) return
      
      cancelling.value = true
      try {
        const reason = cancelReason.value === 'other' ? 
          cancelReasonDetail.value : 
          getCancelReasonText(cancelReason.value)
        
        const response = await api.post(`/trading/${selectedOrder.value.id}/cancel`, {
          reason: reason
        })
        
        if (response.data.success) {
          showCancelOrderModal.value = false
          loadOrders() // 重新載入訂單列表
          alert('訂單已取消')
        } else {
          alert('取消失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('取消訂單失敗:', error)
        alert('取消失敗，請稍後重試')
      } finally {
        cancelling.value = false
      }
    }
    
    const confirmPayment = async () => {
      if (!paymentProof.value) return
      
      confirming.value = true
      try {
        const paymentProofData = {
          screenshot: paymentProof.value.file.name,
          uploadTime: new Date().toISOString()
        }
        
        const response = await api.post(`/trading/${selectedOrder.value.id}/confirm-payment`, {
          paymentProof: paymentProofData
        })
        
        if (response.data.success) {
          showPaymentConfirmModal.value = false
          loadOrders() // 重新載入訂單列表
          alert('支付確認成功，請等待審核')
        } else {
          alert('確認失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('確認支付失敗:', error)
        alert('確認失敗，請稍後重試')
      } finally {
        confirming.value = false
      }
    }
    
    const getCancelReasonText = (reason) => {
      const reasonMap = {
        'price_change': '價格變動',
        'payment_issue': '支付問題', 
        'change_mind': '改變主意',
        'other': '其他原因'
      }
      return reasonMap[reason] || reason
    }
    
    const loadStatistics = async () => {
      try {
        // 載入統計數據
        totalOrders.value = orders.value.length
        monthlyOrders.value = orders.value.filter(order => {
          const orderDate = new Date(order.createdAt)
          const now = new Date()
          return orderDate.getMonth() === now.getMonth() && 
                 orderDate.getFullYear() === now.getFullYear()
        }).length
        
        // 更新篩選標籤計數
        filterTabs[0].count = orders.value.length
        filterTabs[1].count = orders.value.filter(o => o.orderType === 'BUY').length
        filterTabs[2].count = orders.value.filter(o => o.orderType === 'SELL').length
        
      } catch (error) {
        console.error('載入統計數據失敗:', error)
      }
    }
    
    // 生命週期
    onMounted(() => {
      loadOrders().then(() => {
        loadStatistics()
      })
    })
    
    return {
      loading,
      orders,
      totalOrders,
      monthlyOrders,
      showCancelOrderModal,
      showPaymentConfirmModal,
      selectedOrder,
      cancelling,
      confirming,
      cancelReason,
      cancelReasonDetail,
      paymentProof,
      activeFilter,
      pagination,
      visiblePages,
      filterTabs,
      formatPrice,
      formatCrypto,
      formatDateTime,
      getStatusText,
      getPaymentMethodText,
      canCancelOrder,
      canConfirmPayment,
      setActiveFilter,
      changePage,
      viewOrderDetail,
      showCancelModal,
      showPaymentModal,
      triggerPaymentProofUpload,
      handlePaymentProofUpload,
      removePaymentProof,
      refreshOrders,
      confirmCancelOrder,
      confirmPayment
    }
  }
}
</script>

<style scoped>
.orders-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.filter-section {
  margin-bottom: 24px;
}

.filter-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-header h3 {
  margin: 0;
  color: #1a1a1a;
}

.filter-stats {
  display: flex;
  gap: 24px;
}

.stats-item {
  font-size: 14px;
  color: #6b7280;
}

.stats-item strong {
  color: #1a1a1a;
}

.filter-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
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

.tab-count {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.filter-options {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-options select,
.filter-options input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: #e5e7eb;
}

.refresh-btn:disabled {
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

.orders-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading-placeholder {
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

.orders-list {
  display: flex;
  flex-direction: column;
}

.order-card {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:hover {
  background: #f9fafb;
}

.order-card:last-child {
  border-bottom: none;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.order-basic {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-type {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.order-type.buy {
  background: #d1fae5;
  color: #065f46;
}

.order-type.sell {
  background: #fee2e2;
  color: #991b1b;
}

.order-number {
  font-family: monospace;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.order-time {
  font-size: 14px;
  color: #9ca3af;
}

.order-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.order-status.pending {
  background: #fef3c7;
  color: #92400e;
}

.order-status.processing {
  background: #dbeafe;
  color: #1e40af;
}

.order-status.completed {
  background: #d1fae5;
  color: #065f46;
}

.order-status.cancelled {
  background: #f3f4f6;
  color: #6b7280;
}

.order-status.failed {
  background: #fee2e2;
  color: #991b1b;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.order-content {
  margin-bottom: 16px;
}

.order-amounts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 12px;
}

.amount-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.amount-item .label {
  font-size: 12px;
  color: #6b7280;
}

.amount-item .value {
  font-weight: 600;
  color: #1a1a1a;
}

.order-payment,
.order-bank {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 8px;
}

.order-payment .label,
.order-bank .label {
  color: #6b7280;
}

.order-payment .value,
.order-bank .value {
  color: #4b5563;
}

.order-actions {
  display: flex;
  gap: 12px;
}

.order-actions button {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-btn {
  background: white;
  color: #374151;
}

.detail-btn:hover {
  background: #f9fafb;
}

.cancel-btn {
  background: white;
  color: #dc2626;
  border-color: #fecaca;
}

.cancel-btn:hover {
  background: #fef2f2;
}

.confirm-btn {
  background: #10b981;
  color: white;
  border-color: #10b981;
}

.confirm-btn:hover {
  background: #059669;
}

.empty-orders {
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

.empty-orders h3 {
  margin: 0 0 8px 0;
  color: #374151;
}

.empty-orders p {
  margin: 0 0 24px 0;
  color: #9ca3af;
}

.start-trading-btn {
  padding: 12px 24px;
  background: #2563eb;
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s;
}

.start-trading-btn:hover {
  background: #1d4ed8;
}

.pagination-section {
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
  max-width: 500px;
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

.order-info p {
  margin: 0 0 16px 0;
  color: #374151;
}

.order-summary {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 16px;
}

.form-group textarea {
  min-height: 80px;
  resize: vertical;
}

.payment-info {
  margin-bottom: 24px;
}

.payment-info h4 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
}

.payment-details {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item .amount {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.payment-proof {
  margin-bottom: 24px;
}

.payment-proof h4 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.upload-area:hover {
  border-color: #2563eb;
  background: #f8fafc;
}

.upload-placeholder i {
  font-size: 32px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.upload-placeholder p {
  margin: 0 0 4px 0;
  color: #4b5563;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #9ca3af;
}

.upload-preview {
  position: relative;
}

.upload-preview img {
  width: 100%;
  max-width: 300px;
  height: auto;
  border-radius: 6px;
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}

.payment-notes {
  background: #fef3c7;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #f59e0b;
  margin-bottom: 24px;
}

.payment-notes h4 {
  margin: 0 0 12px 0;
  color: #92400e;
}

.payment-notes ul {
  margin: 0;
  padding-left: 20px;
  color: #92400e;
}

.payment-notes li {
  margin-bottom: 4px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.modal-actions .cancel-btn {
  background: #f3f4f6;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.modal-actions .confirm-btn {
  background: #2563eb;
  color: white;
  border: 1px solid #2563eb;
}

.modal-actions button {
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .orders-view {
    padding: 16px;
  }
  
  .filter-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .filter-options {
    flex-wrap: wrap;
  }
  
  .order-amounts {
    grid-template-columns: 1fr;
  }
  
  .pagination-section {
    flex-direction: column;
    gap: 16px;
  }
  
  .modal {
    width: 95%;
  }
}
</style>