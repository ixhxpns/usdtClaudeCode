<template>
  <div class="order-list-view">
    <div class="page-header">
      <h1>订单管理</h1>
      <p class="page-description">管理平台所有交易订单</p>
    </div>

    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-group">
          <label>订单状态:</label>
          <select v-model="filters.status" @change="loadData">
            <option value="">全部</option>
            <option value="pending">待处理</option>
            <option value="paid">已支付</option>
            <option value="completed">已完成</option>
            <option value="cancelled">已取消</option>
            <option value="expired">已过期</option>
          </select>
        </div>

        <div class="filter-group">
          <label>订单类型:</label>
          <select v-model="filters.type" @change="loadData">
            <option value="">全部</option>
            <option value="buy">买入</option>
            <option value="sell">卖出</option>
          </select>
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

        <div class="filter-group">
          <label>订单号:</label>
          <input 
            type="text" 
            v-model="filters.orderNo" 
            @keyup.enter="loadData"
            placeholder="输入订单号"
          />
        </div>

        <div class="filter-group">
          <label>创建时间:</label>
          <input 
            type="date" 
            v-model="filters.createDate" 
            @change="loadData"
          />
        </div>

        <button @click="loadData" class="btn btn-primary">搜索</button>
        <button @click="resetFilters" class="btn btn-secondary">重置</button>
      </div>
    </div>

    <div class="table-section">
      <div class="table-header">
        <h3>订单列表</h3>
        <div class="table-actions">
          <button @click="exportData" class="btn btn-outline">导出订单</button>
          <button @click="showStats" class="btn btn-outline">统计分析</button>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户信息</th>
              <th>订单类型</th>
              <th>金额信息</th>
              <th>价格</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>完成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orderList" :key="order.id">
              <td>
                <div class="order-no">{{ order.orderNo }}</div>
                <div class="order-id">ID: {{ order.id }}</div>
              </td>
              <td>
                <div class="user-info">
                  <div class="user-name">{{ order.user?.username }}</div>
                  <div class="user-id">ID: {{ order.userId }}</div>
                  <div class="user-email">{{ order.user?.email }}</div>
                </div>
              </td>
              <td>
                <span class="order-type" :class="`type-${order.type}`">
                  {{ getTypeText(order.type) }}
                </span>
              </td>
              <td>
                <div class="amount-info">
                  <div class="usdt-amount">{{ order.amount }} USDT</div>
                  <div class="total-amount">{{ order.totalAmount }} TWD</div>
                </div>
              </td>
              <td>
                <div class="price">{{ order.price }} TWD/USDT</div>
              </td>
              <td>
                <span class="status-badge" :class="`status-${order.status}`">
                  {{ getStatusText(order.status) }}
                </span>
              </td>
              <td>{{ formatDateTime(order.createdAt) }}</td>
              <td>{{ formatDateTime(order.completedAt) }}</td>
              <td>
                <div class="action-buttons">
                  <button @click="viewOrderDetail(order)" class="btn btn-sm btn-primary">
                    详情
                  </button>
                  <button 
                    v-if="order.status === 'paid'"
                    @click="confirmOrder(order)" 
                    class="btn btn-sm btn-success"
                  >
                    确认完成
                  </button>
                  <button 
                    v-if="canCancel(order)"
                    @click="cancelOrder(order)" 
                    class="btn btn-sm btn-danger"
                  >
                    取消订单
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

        <div v-if="!loading && orderList.length === 0" class="empty-state">
          <p>暂无订单记录</p>
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
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，总计 {{ total }} 条记录
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

    <!-- 订单详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>订单详情</h3>
          <button @click="closeDetailModal" class="btn-close">&times;</button>
        </div>
        
        <div class="modal-body" v-if="selectedOrder">
          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>订单号:</label>
                <span>{{ selectedOrder.orderNo }}</span>
              </div>
              <div class="detail-item">
                <label>订单类型:</label>
                <span class="order-type" :class="`type-${selectedOrder.type}`">
                  {{ getTypeText(selectedOrder.type) }}
                </span>
              </div>
              <div class="detail-item">
                <label>订单状态:</label>
                <span class="status-badge" :class="`status-${selectedOrder.status}`">
                  {{ getStatusText(selectedOrder.status) }}
                </span>
              </div>
              <div class="detail-item">
                <label>创建时间:</label>
                <span>{{ formatDateTime(selectedOrder.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>交易信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>USDT数量:</label>
                <span>{{ selectedOrder.amount }} USDT</span>
              </div>
              <div class="detail-item">
                <label>单价:</label>
                <span>{{ selectedOrder.price }} TWD/USDT</span>
              </div>
              <div class="detail-item">
                <label>总金额:</label>
                <span>{{ selectedOrder.totalAmount }} TWD</span>
              </div>
              <div class="detail-item">
                <label>已成交数量:</label>
                <span>{{ selectedOrder.filledAmount || 0 }} USDT</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>用户信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>用户ID:</label>
                <span>{{ selectedOrder.userId }}</span>
              </div>
              <div class="detail-item">
                <label>用户名:</label>
                <span>{{ selectedOrder.user?.username }}</span>
              </div>
              <div class="detail-item">
                <label>邮箱:</label>
                <span>{{ selectedOrder.user?.email }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section" v-if="selectedOrder.paymentInfo">
            <h4>支付信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>支付方式:</label>
                <span>{{ selectedOrder.paymentMethod }}</span>
              </div>
              <div class="detail-item">
                <label>银行账户:</label>
                <span>{{ selectedOrder.bankAccount }}</span>
              </div>
              <div class="detail-item">
                <label>支付截止时间:</label>
                <span>{{ formatDateTime(selectedOrder.paymentDeadline) }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section" v-if="selectedOrder.adminNote">
            <h4>管理员备注</h4>
            <p>{{ selectedOrder.adminNote }}</p>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="closeDetailModal" class="btn btn-secondary">关闭</button>
          <button 
            v-if="selectedOrder && selectedOrder.status === 'paid'"
            @click="confirmOrder(selectedOrder)" 
            class="btn btn-success"
          >
            确认完成
          </button>
        </div>
      </div>
    </div>

    <!-- 统计分析弹窗 -->
    <div v-if="showStatsModal" class="modal-overlay" @click="closeStatsModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>订单统计分析</h3>
          <button @click="closeStatsModal" class="btn-close">&times;</button>
        </div>
        
        <div class="modal-body">
          <div class="stats-grid">
            <div class="stats-card">
              <h4>订单总数</h4>
              <div class="stats-number">{{ stats.totalOrders }}</div>
            </div>
            <div class="stats-card">
              <h4>交易总额</h4>
              <div class="stats-number">{{ stats.totalAmount }} TWD</div>
            </div>
            <div class="stats-card">
              <h4>今日新增</h4>
              <div class="stats-number">{{ stats.todayOrders }}</div>
            </div>
            <div class="stats-card">
              <h4>完成率</h4>
              <div class="stats-number">{{ stats.completionRate }}%</div>
            </div>
          </div>

          <div class="stats-section">
            <h4>状态分布</h4>
            <div class="status-stats">
              <div v-for="(count, status) in stats.statusDistribution" :key="status" class="status-item">
                <span class="status-label">{{ getStatusText(status) }}:</span>
                <span class="status-count">{{ count }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="closeStatsModal" class="btn btn-secondary">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { AdminApi } from '@/api'

interface Order {
  id: number
  orderNo: string
  userId: number
  type: string
  amount: number
  price: number
  totalAmount: number
  filledAmount?: number
  status: string
  paymentMethod?: string
  bankAccount?: string
  paymentDeadline?: string
  adminNote?: string
  createdAt: string
  completedAt?: string
  user?: {
    id: number
    username: string
    email: string
  }
}

interface OrderStats {
  totalOrders: number
  totalAmount: number
  todayOrders: number
  completionRate: number
  statusDistribution: Record<string, number>
}

const loading = ref(false)
const orderList = ref<Order[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetailModal = ref(false)
const showStatsModal = ref(false)
const selectedOrder = ref<Order | null>(null)
const stats = ref<OrderStats>({
  totalOrders: 0,
  totalAmount: 0,
  todayOrders: 0,
  completionRate: 0,
  statusDistribution: {}
})

const filters = reactive({
  status: '',
  type: '',
  userId: '',
  orderNo: '',
  createDate: ''
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
    
    const response = await AdminApi.getOrders(params)
    if (response.success) {
      orderList.value = response.data.records || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置过滤器
const resetFilters = () => {
  Object.keys(filters).forEach(key => {
    (filters as any)[key] = ''
  })
  currentPage.value = 1
  loadData()
}

// 分页
const changePage = (page: number) => {
  currentPage.value = page
  loadData()
}

// 查看订单详情
const viewOrderDetail = (order: Order) => {
  selectedOrder.value = order
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedOrder.value = null
}

// 确认订单
const confirmOrder = async (order: Order) => {
  try {
    const response = await AdminApi.confirmOrder(order.id)
    if (response.success) {
      alert('订单确认成功')
      loadData()
      if (showDetailModal.value) {
        closeDetailModal()
      }
    }
  } catch (error) {
    console.error('确认订单失败:', error)
    alert('确认订单失败')
  }
}

// 取消订单
const cancelOrder = async (order: Order) => {
  if (!confirm('确定要取消这个订单吗？')) return
  
  try {
    const response = await AdminApi.cancelOrder(order.id)
    if (response.success) {
      alert('订单取消成功')
      loadData()
    }
  } catch (error) {
    console.error('取消订单失败:', error)
    alert('取消订单失败')
  }
}

// 判断是否可以取消
const canCancel = (order: Order) => {
  return ['pending', 'paid'].includes(order.status)
}

// 显示统计
const showStats = async () => {
  try {
    const response = await AdminApi.getOrderStats()
    if (response.success) {
      stats.value = response.data
      showStatsModal.value = true
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 关闭统计弹窗
const closeStatsModal = () => {
  showStatsModal.value = false
}

// 导出数据
const exportData = async () => {
  try {
    const params = { ...filters, export: true }
    const response = await AdminApi.exportOrders(params)
    // 处理文件下载
    const blob = new Blob([response.data], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `orders_${new Date().getTime()}.xlsx`
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

// 获取订单类型文本
const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    buy: '买入',
    sell: '卖出'
  }
  return typeMap[type] || type
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: '待处理',
    paid: '已支付',
    completed: '已完成',
    cancelled: '已取消',
    expired: '已过期'
  }
  return statusMap[status] || status
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.order-list-view {
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
  min-width: 1200px;
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

.order-no {
  font-weight: 500;
  color: $text-primary;
}

.order-id {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
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

.order-type {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.type-buy {
  background: #e6f7ff;
  color: #1890ff;
}

.type-sell {
  background: #fff2e6;
  color: #fa8c16;
}

.amount-info {
  min-width: 120px;
}

.usdt-amount {
  font-weight: 500;
  color: $text-primary;
}

.total-amount {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.price {
  font-weight: 500;
  color: $text-primary;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-paid {
  background: #f6ffed;
  color: #52c41a;
}

.status-completed {
  background: #e6f7ff;
  color: #1890ff;
}

.status-cancelled {
  background: #fff1f0;
  color: #ff4d4f;
}

.status-expired {
  background: #f5f5f5;
  color: #8c8c8c;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 160px;
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stats-card {
  text-align: center;
  padding: 20px;
  border: 1px solid $border-color;
  border-radius: 8px;
}

.stats-card h4 {
  margin: 0 0 12px 0;
  color: $text-secondary;
  font-size: 14px;
}

.stats-number {
  font-size: 24px;
  font-weight: bold;
  color: $admin-primary;
}

.stats-section {
  margin-bottom: 24px;
}

.status-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  color: $text-secondary;
}

.status-count {
  font-weight: 500;
  color: $text-primary;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
  background: color.adjust($admin-primary, $lightness: -10%);
}

.btn-secondary {
  background: $admin-secondary;
  color: white;
}

.btn-secondary:hover {
  background: color.adjust($admin-secondary, $lightness: -10%);
}

.btn-success {
  background: $status-approved;
  color: white;
}

.btn-success:hover {
  background: color.adjust($status-approved, $lightness: -10%);
}

.btn-danger {
  background: $status-rejected;
  color: white;
}

.btn-danger:hover {
  background: color.adjust($status-rejected, $lightness: -10%);
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