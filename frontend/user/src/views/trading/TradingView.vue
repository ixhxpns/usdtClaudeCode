<template>
  <div class="trading-view">
    <!-- 價格顯示區域 -->
    <div class="price-section">
      <div class="price-card">
        <div class="price-header">
          <h2>USDT 實時價格</h2>
          <div class="price-status" :class="priceStatus">
            <span class="status-dot"></span>
            {{ priceStatus === 'up' ? '上漲' : priceStatus === 'down' ? '下跌' : '平穩' }}
          </div>
        </div>
        
        <div class="price-display">
          <div class="buy-price">
            <span class="label">買入價</span>
            <span class="price">{{ formatPrice(currentPrice.buyPrice) }}</span>
          </div>
          <div class="sell-price">
            <span class="label">賣出價</span>
            <span class="price">{{ formatPrice(currentPrice.sellPrice) }}</span>
          </div>
        </div>
        
        <div class="price-info">
          <div class="info-item">
            <span>24h 漲跌</span>
            <span :class="priceChange24h >= 0 ? 'positive' : 'negative'">
              {{ priceChange24h >= 0 ? '+' : '' }}{{ priceChange24h.toFixed(2) }}%
            </span>
          </div>
          <div class="info-item">
            <span>24h 成交量</span>
            <span>{{ formatVolume(volume24h) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 交易操作區域 -->
    <div class="trading-section">
      <div class="trading-tabs">
        <button 
          v-for="tab in tradingTabs" 
          :key="tab.value"
          :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 買入界面 -->
      <div v-if="activeTab === 'buy'" class="trade-form buy-form">
        <h3>買入 USDT</h3>
        
        <div class="form-group">
          <label>買入金額 (TWD)</label>
          <div class="input-wrapper">
            <input 
              v-model="buyForm.amount" 
              type="number" 
              placeholder="最少 10 TWD"
              @input="calculateBuyAmount"
            >
            <span class="currency">TWD</span>
          </div>
          <div class="conversion-info">
            預計獲得: {{ calculateUSDTAmount(buyForm.amount) }} USDT
          </div>
        </div>

        <div class="form-group">
          <label>支付方式</label>
          <select v-model="buyForm.paymentMethod">
            <option value="">選擇支付方式</option>
            <option v-for="method in paymentMethods" :key="method.value" :value="method.value">
              {{ method.label }}
            </option>
          </select>
        </div>

        <div class="trading-limits">
          <div class="limit-item">
            <span>單筆限額</span>
            <span>{{ formatPrice(tradingLimits.minBuy) }} - {{ formatPrice(tradingLimits.maxBuy) }} TWD</span>
          </div>
          <div class="limit-item">
            <span>當日剩餘</span>
            <span>{{ formatPrice(tradingLimits.dailyRemaining) }} TWD</span>
          </div>
        </div>

        <button 
          class="trade-btn buy-btn" 
          :disabled="!canSubmitBuy"
          @click="submitBuyOrder"
        >
          {{ buyOrderLoading ? '處理中...' : '確認買入' }}
        </button>
      </div>

      <!-- 賣出界面 -->
      <div v-if="activeTab === 'sell'" class="trade-form sell-form">
        <h3>賣出 USDT</h3>
        
        <div class="form-group">
          <label>賣出數量 (USDT)</label>
          <div class="input-wrapper">
            <input 
              v-model="sellForm.usdtAmount" 
              type="number" 
              placeholder="最少 1 USDT"
              @input="calculateSellAmount"
            >
            <span class="currency">USDT</span>
          </div>
          <div class="conversion-info">
            預計獲得: {{ calculateTWDAmount(sellForm.usdtAmount) }} TWD
          </div>
          <div class="balance-info">
            可用餘額: {{ formatCrypto(userBalance.usdt) }} USDT
          </div>
        </div>

        <div class="form-group">
          <label>收款銀行</label>
          <select v-model="sellForm.receivingBank">
            <option value="">選擇收款銀行</option>
            <option v-for="bank in supportedBanks" :key="bank.value" :value="bank.value">
              {{ bank.label }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>收款帳號</label>
          <input 
            v-model="sellForm.receivingAccount" 
            type="text" 
            placeholder="請輸入銀行帳號"
          >
        </div>

        <button 
          class="trade-btn sell-btn" 
          :disabled="!canSubmitSell"
          @click="submitSellOrder"
        >
          {{ sellOrderLoading ? '處理中...' : '確認賣出' }}
        </button>
      </div>
    </div>

    <!-- 最近訂單 -->
    <div class="recent-orders">
      <div class="section-header">
        <h3>最近訂單</h3>
        <router-link to="/orders" class="view-all">查看全部</router-link>
      </div>
      
      <div v-if="recentOrders.length > 0" class="orders-list">
        <div 
          v-for="order in recentOrders" 
          :key="order.id"
          class="order-item"
          @click="$router.push(`/orders/${order.id}`)"
        >
          <div class="order-info">
            <div class="order-type" :class="order.orderType.toLowerCase()">
              {{ order.orderType === 'BUY' ? '買入' : '賣出' }}
            </div>
            <div class="order-details">
              <div class="amount">
                {{ order.orderType === 'BUY' ? formatPrice(order.amount) + ' TWD' : formatCrypto(order.usdtAmount) + ' USDT' }}
              </div>
              <div class="time">{{ formatTime(order.createdAt) }}</div>
            </div>
          </div>
          <div class="order-status" :class="order.status.toLowerCase()">
            {{ getStatusText(order.status) }}
          </div>
        </div>
      </div>
      
      <div v-else class="no-orders">
        <p>暫無交易記錄</p>
        <p class="suggestion">開始您的第一筆交易吧！</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// import { useAuthStore } from '@/stores/auth' // 暂时未使用
import { api } from '@/utils/http'

export default {
  name: 'TradingView',
  setup() {
    const router = useRouter()
    // const authStore = useAuthStore() // 暂时未使用
    
    // 響應式數據
    const activeTab = ref('buy')
    const buyOrderLoading = ref(false)
    const sellOrderLoading = ref(false)
    
    const currentPrice = reactive({
      buyPrice: 0,
      sellPrice: 0,
      lastUpdate: null
    })
    
    const buyForm = reactive({
      amount: '',
      paymentMethod: ''
    })
    
    const sellForm = reactive({
      usdtAmount: '',
      receivingBank: '',
      receivingAccount: ''
    })
    
    const tradingLimits = reactive({
      minBuy: 10,
      maxBuy: 100000,
      minSell: 1,
      maxSell: 1000,
      dailyRemaining: 50000
    })
    
    const userBalance = reactive({
      twd: 0,
      usdt: 0
    })
    
    const recentOrders = ref([])
    const priceChange24h = ref(0)
    const volume24h = ref(0)
    
    // 計算屬性
    const priceStatus = computed(() => {
      if (priceChange24h.value > 0) return 'up'
      if (priceChange24h.value < 0) return 'down'
      return 'stable'
    })
    
    const canSubmitBuy = computed(() => {
      return buyForm.amount && 
             parseFloat(buyForm.amount) >= tradingLimits.minBuy && 
             buyForm.paymentMethod &&
             !buyOrderLoading.value
    })
    
    const canSubmitSell = computed(() => {
      return sellForm.usdtAmount && 
             parseFloat(sellForm.usdtAmount) >= tradingLimits.minSell && 
             parseFloat(sellForm.usdtAmount) <= userBalance.usdt &&
             sellForm.receivingBank &&
             sellForm.receivingAccount &&
             !sellOrderLoading.value
    })
    
    // 靜態數據
    const tradingTabs = [
      { label: '買入', value: 'buy' },
      { label: '賣出', value: 'sell' }
    ]
    
    const paymentMethods = [
      { label: '銀行轉帳', value: 'bank_transfer' },
      { label: '超商付款', value: 'convenience_store' },
      { label: '線上支付', value: 'online_payment' }
    ]
    
    const supportedBanks = [
      { label: '台灣銀行', value: 'bot' },
      { label: '中國信託', value: 'ctbc' },
      { label: '國泰世華', value: 'cathay' },
      { label: '富邦銀行', value: 'fubon' },
      { label: '玉山銀行', value: 'esun' }
    ]
    
    // 方法
    const formatPrice = (price) => {
      return new Intl.NumberFormat('zh-TW', {
        style: 'currency',
        currency: 'TWD',
        minimumFractionDigits: 2
      }).format(price || 0)
    }
    
    const formatCrypto = (amount) => {
      return parseFloat(amount || 0).toFixed(6)
    }
    
    const formatVolume = (volume) => {
      if (volume >= 1000000) {
        return (volume / 1000000).toFixed(1) + 'M'
      }
      if (volume >= 1000) {
        return (volume / 1000).toFixed(1) + 'K'
      }
      return volume.toString()
    }
    
    const formatTime = (timestamp) => {
      return new Date(timestamp).toLocaleString('zh-TW')
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
    
    const calculateUSDTAmount = (twdAmount) => {
      if (!twdAmount || !currentPrice.buyPrice) return '0.000000'
      const usdtAmount = parseFloat(twdAmount) / currentPrice.buyPrice
      return usdtAmount.toFixed(6)
    }
    
    const calculateTWDAmount = (usdtAmount) => {
      if (!usdtAmount || !currentPrice.sellPrice) return '0.00'
      const twdAmount = parseFloat(usdtAmount) * currentPrice.sellPrice
      return twdAmount.toFixed(2)
    }
    
    const calculateBuyAmount = () => {
      // 可以添加額外的計算邏輯
    }
    
    const calculateSellAmount = () => {
      // 可以添加額外的計算邏輯
    }
    
    // API 調用
    const loadCurrentPrice = async () => {
      try {
        const response = await api.get('/price/current')
        if (response.data.success) {
          Object.assign(currentPrice, response.data.data)
        }
      } catch (error) {
        console.error('獲取價格失敗:', error)
      }
    }
    
    const loadUserBalance = async () => {
      try {
        const response = await api.get('/wallet/balance')
        if (response.data.success) {
          Object.assign(userBalance, response.data.data)
        }
      } catch (error) {
        console.error('獲取餘額失敗:', error)
      }
    }
    
    const loadTradingLimits = async () => {
      try {
        const response = await api.get('/trading/limits')
        if (response.data.success) {
          Object.assign(tradingLimits, response.data.data)
        }
      } catch (error) {
        console.error('獲取交易限額失敗:', error)
      }
    }
    
    const loadRecentOrders = async () => {
      try {
        const response = await api.get('/orders/my?pageSize=5')
        if (response.data.success) {
          recentOrders.value = response.data.data.records
        }
      } catch (error) {
        console.error('獲取最近訂單失敗:', error)
      }
    }
    
    const submitBuyOrder = async () => {
      if (!canSubmitBuy.value) return
      
      buyOrderLoading.value = true
      try {
        const response = await api.post('/trading/buy', {
          amount: parseFloat(buyForm.amount),
          paymentMethod: buyForm.paymentMethod
        })
        
        if (response.data.success) {
          // 跳轉到訂單詳情頁
          router.push(`/orders/${response.data.data.orderId}`)
        } else {
          alert('下單失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('買入下單失敗:', error)
        alert('下單失敗，請稍後重試')
      } finally {
        buyOrderLoading.value = false
      }
    }
    
    const submitSellOrder = async () => {
      if (!canSubmitSell.value) return
      
      sellOrderLoading.value = true
      try {
        const response = await api.post('/trading/sell', {
          usdtAmount: parseFloat(sellForm.usdtAmount),
          receivingBank: sellForm.receivingBank,
          receivingAccount: sellForm.receivingAccount
        })
        
        if (response.data.success) {
          // 跳轉到訂單詳情頁
          router.push(`/orders/${response.data.data.orderId}`)
        } else {
          alert('下單失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('賣出下單失敗:', error)
        alert('下單失敗，請稍後重試')
      } finally {
        sellOrderLoading.value = false
      }
    }
    
    // 生命週期
    onMounted(() => {
      loadCurrentPrice()
      loadUserBalance()
      loadTradingLimits()
      loadRecentOrders()
      
      // 設置價格更新定時器
      const priceTimer = setInterval(loadCurrentPrice, 30000) // 30秒更新一次
      
      // 組件銷毀時清除定時器
      return () => {
        clearInterval(priceTimer)
      }
    })
    
    return {
      activeTab,
      buyOrderLoading,
      sellOrderLoading,
      currentPrice,
      buyForm,
      sellForm,
      tradingLimits,
      userBalance,
      recentOrders,
      priceChange24h,
      volume24h,
      priceStatus,
      canSubmitBuy,
      canSubmitSell,
      tradingTabs,
      paymentMethods,
      supportedBanks,
      formatPrice,
      formatCrypto,
      formatVolume,
      formatTime,
      getStatusText,
      calculateUSDTAmount,
      calculateTWDAmount,
      calculateBuyAmount,
      calculateSellAmount,
      submitBuyOrder,
      submitSellOrder
    }
  }
}
</script>

<style scoped>
.trading-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.price-section {
  grid-column: 1 / -1;
}

.price-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.price-header {
  display: flex;
  justify-content: between;
  align-items: center;
  margin-bottom: 20px;
}

.price-header h2 {
  margin: 0;
  color: #1a1a1a;
}

.price-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #6b7280;
}

.price-status.up .status-dot {
  background: #10b981;
}

.price-status.down .status-dot {
  background: #ef4444;
}

.price-display {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 20px;
}

.buy-price, .sell-price {
  text-align: center;
  padding: 20px;
  border-radius: 8px;
  background: #f9fafb;
}

.buy-price {
  border-left: 4px solid #10b981;
}

.sell-price {
  border-left: 4px solid #ef4444;
}

.price-display .label {
  display: block;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.price-display .price {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.price-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.positive {
  color: #10b981;
}

.negative {
  color: #ef4444;
}

.trading-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.trading-tabs {
  display: flex;
  margin-bottom: 24px;
  border-radius: 8px;
  background: #f3f4f6;
  padding: 4px;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: white;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.trade-form h3 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
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

.input-wrapper {
  position: relative;
}

.input-wrapper input {
  width: 100%;
  padding: 12px 50px 12px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 16px;
}

.input-wrapper .currency {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #6b7280;
  font-weight: 500;
}

.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 16px;
}

.conversion-info {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
}

.balance-info {
  margin-top: 4px;
  font-size: 14px;
  color: #2563eb;
}

.trading-limits {
  background: #f9fafb;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.limit-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.limit-item:last-child {
  margin-bottom: 0;
}

.trade-btn {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.buy-btn {
  background: #10b981;
  color: white;
}

.buy-btn:hover:not(:disabled) {
  background: #059669;
}

.sell-btn {
  background: #ef4444;
  color: white;
}

.sell-btn:hover:not(:disabled) {
  background: #dc2626;
}

.trade-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.recent-orders {
  grid-column: 1 / -1;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  color: #1a1a1a;
}

.view-all {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.order-item:hover {
  background: #f9fafb;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-type {
  padding: 4px 8px;
  border-radius: 4px;
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

.order-details .amount {
  font-weight: 600;
  color: #1a1a1a;
}

.order-details .time {
  font-size: 14px;
  color: #6b7280;
}

.order-status {
  padding: 4px 12px;
  border-radius: 12px;
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

.no-orders {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.no-orders .suggestion {
  margin-top: 8px;
  font-size: 14px;
  color: #9ca3af;
}

@media (max-width: 768px) {
  .trading-view {
    grid-template-columns: 1fr;
    padding: 16px;
  }
  
  .price-display {
    grid-template-columns: 1fr;
  }
  
  .price-info {
    grid-template-columns: 1fr;
  }
}
</style>