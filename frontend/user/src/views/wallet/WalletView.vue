<template>
  <div class="wallet-view">
    <!-- 錢包餘額卡片 -->
    <div class="balance-section">
      <div class="balance-card">
        <div class="balance-header">
          <h2>錢包餘額</h2>
          <button class="refresh-btn" @click="refreshBalance" :disabled="refreshing">
            <i class="icon-refresh" :class="{ spinning: refreshing }"></i>
            刷新
          </button>
        </div>
        
        <div class="balance-grid">
          <div class="balance-item twd">
            <div class="currency-icon">
              <span>$</span>
            </div>
            <div class="balance-info">
              <div class="currency">台幣 (TWD)</div>
              <div class="amount">{{ formatPrice(balances.twd) }}</div>
              <div class="equivalent">≈ {{ formatPrice(balances.twd) }}</div>
            </div>
          </div>
          
          <div class="balance-item usdt">
            <div class="currency-icon">
              <span>₮</span>
            </div>
            <div class="balance-info">
              <div class="currency">USDT</div>
              <div class="amount">{{ formatCrypto(balances.usdt) }}</div>
              <div class="equivalent">≈ {{ formatPrice(balances.usdt * currentPrice.sellPrice) }}</div>
            </div>
          </div>
        </div>
        
        <div class="total-balance">
          <span class="label">總資產價值</span>
          <span class="total">{{ formatPrice(totalValue) }}</span>
        </div>
      </div>
    </div>

    <!-- 操作按鈕 -->
    <div class="action-section">
      <div class="action-grid">
        <button class="action-btn deposit" @click="showDepositModal = true">
          <i class="icon-deposit"></i>
          <span>充值</span>
        </button>
        
        <button class="action-btn withdraw" @click="showWithdrawModal = true">
          <i class="icon-withdraw"></i>
          <span>提現</span>
        </button>
        
        <button class="action-btn trade" @click="$router.push('/trading')">
          <i class="icon-trade"></i>
          <span>交易</span>
        </button>
        
        <button class="action-btn history" @click="$router.push('/wallet/history')">
          <i class="icon-history"></i>
          <span>歷史</span>
        </button>
      </div>
    </div>

    <!-- 最近交易記錄 -->
    <div class="transactions-section">
      <div class="section-header">
        <h3>最近交易</h3>
        <router-link to="/wallet/history" class="view-all">查看全部</router-link>
      </div>
      
      <div v-if="recentTransactions.length > 0" class="transactions-list">
        <div 
          v-for="transaction in recentTransactions" 
          :key="transaction.id"
          class="transaction-item"
        >
          <div class="transaction-icon" :class="transaction.type.toLowerCase()">
            <i :class="getTransactionIcon(transaction.type)"></i>
          </div>
          
          <div class="transaction-info">
            <div class="transaction-type">{{ getTransactionTypeText(transaction.type) }}</div>
            <div class="transaction-time">{{ formatTime(transaction.createdAt) }}</div>
          </div>
          
          <div class="transaction-amount" :class="getAmountClass(transaction.type)">
            <div class="amount">
              {{ getAmountPrefix(transaction.type) }}{{ formatAmount(transaction) }}
            </div>
            <div class="status" :class="transaction.status.toLowerCase()">
              {{ getStatusText(transaction.status) }}
            </div>
          </div>
        </div>
      </div>
      
      <div v-else class="no-transactions">
        <p>暫無交易記錄</p>
      </div>
    </div>

    <!-- 充值彈窗 -->
    <div v-if="showDepositModal" class="modal-overlay" @click="showDepositModal = false">
      <div class="modal deposit-modal" @click.stop>
        <div class="modal-header">
          <h3>USDT 充值</h3>
          <button class="close-btn" @click="showDepositModal = false">×</button>
        </div>
        
        <div class="modal-body">
          <div class="network-selector">
            <label>選擇網絡</label>
            <div class="network-options">
              <button 
                v-for="network in supportedNetworks"
                :key="network.value"
                :class="['network-btn', { active: selectedNetwork === network.value }]"
                @click="selectedNetwork = network.value"
              >
                <span class="network-name">{{ network.label }}</span>
                <span class="network-fee">手續費: {{ network.fee }}</span>
              </button>
            </div>
          </div>
          
          <div class="address-section">
            <label>充值地址</label>
            <div class="address-display">
              <div class="address-text">{{ depositAddress }}</div>
              <button class="copy-btn" @click="copyAddress">
                <i class="icon-copy"></i>
                複製
              </button>
            </div>
            
            <div class="qr-code">
              <div class="qr-placeholder">
                <p>QR 碼</p>
                <p class="qr-address">{{ depositAddress }}</p>
              </div>
            </div>
          </div>
          
          <div class="deposit-notes">
            <h4>重要提醒</h4>
            <ul>
              <li>僅支持 {{ selectedNetwork }} 網絡的 USDT 充值</li>
              <li>最小充值金額: 1 USDT</li>
              <li>充值需要 {{ getConfirmations(selectedNetwork) }} 個區塊確認</li>
              <li>請勿向此地址充值其他幣種</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 提現彈窗 -->
    <div v-if="showWithdrawModal" class="modal-overlay" @click="showWithdrawModal = false">
      <div class="modal withdraw-modal" @click.stop>
        <div class="modal-header">
          <h3>USDT 提現</h3>
          <button class="close-btn" @click="showWithdrawModal = false">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>提現網絡</label>
            <select v-model="withdrawForm.network">
              <option value="">選擇網絡</option>
              <option 
                v-for="network in supportedNetworks" 
                :key="network.value" 
                :value="network.value"
              >
                {{ network.label }} (手續費: {{ network.withdrawFee }})
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label>提現地址</label>
            <input 
              v-model="withdrawForm.address" 
              type="text" 
              placeholder="請輸入 USDT 錢包地址"
              @blur="validateAddress"
            >
            <div v-if="addressValidation.checked" class="address-validation">
              <span :class="['validation-status', addressValidation.valid ? 'valid' : 'invalid']">
                {{ addressValidation.valid ? '✓ 地址格式正確' : '✗ 地址格式錯誤' }}
              </span>
            </div>
          </div>
          
          <div class="form-group">
            <label>提現數量</label>
            <div class="amount-input">
              <input 
                v-model="withdrawForm.amount" 
                type="number" 
                step="0.000001"
                placeholder="最少 1 USDT"
                @input="calculateWithdrawFee"
              >
              <button class="max-btn" @click="setMaxAmount">全部</button>
            </div>
            <div class="balance-hint">
              可用餘額: {{ formatCrypto(balances.usdt) }} USDT
            </div>
          </div>
          
          <div class="fee-summary">
            <div class="fee-item">
              <span>網絡手續費</span>
              <span>{{ withdrawFeeAmount }} USDT</span>
            </div>
            <div class="fee-item">
              <span>實際到賬</span>
              <span>{{ actualReceiveAmount }} USDT</span>
            </div>
          </div>
          
          <div class="withdraw-limits">
            <div class="limit-item">
              <span>單筆限額</span>
              <span>{{ withdrawLimits.min }} - {{ withdrawLimits.max }} USDT</span>
            </div>
            <div class="limit-item">
              <span>日剩餘額度</span>
              <span>{{ withdrawLimits.dailyRemaining }} USDT</span>
            </div>
          </div>
          
          <button 
            class="submit-btn"
            :disabled="!canSubmitWithdraw"
            @click="submitWithdraw"
          >
            {{ withdrawing ? '提交中...' : '確認提現' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
// import { useRouter } from 'vue-router' // 暂时未使用
import { api } from '@/utils/http'

export default {
  name: 'WalletView',
  setup() {
    // const router = useRouter() // 暂时未使用
    
    // 響應式數據
    const refreshing = ref(false)
    const showDepositModal = ref(false)
    const showWithdrawModal = ref(false)
    const withdrawing = ref(false)
    const selectedNetwork = ref('TRC20')
    const depositAddress = ref('')
    
    const balances = reactive({
      twd: 0,
      usdt: 0
    })
    
    const currentPrice = reactive({
      buyPrice: 0,
      sellPrice: 0
    })
    
    const withdrawForm = reactive({
      network: '',
      address: '',
      amount: ''
    })
    
    const addressValidation = reactive({
      checked: false,
      valid: false
    })
    
    const withdrawLimits = reactive({
      min: 1,
      max: 1000,
      dailyRemaining: 5000
    })
    
    const recentTransactions = ref([])
    
    // 計算屬性
    const totalValue = computed(() => {
      return balances.twd + (balances.usdt * currentPrice.sellPrice)
    })
    
    const withdrawFeeAmount = computed(() => {
      const network = supportedNetworks.find(n => n.value === withdrawForm.network)
      return network ? network.withdrawFee : '0'
    })
    
    const actualReceiveAmount = computed(() => {
      const amount = parseFloat(withdrawForm.amount) || 0
      const fee = parseFloat(withdrawFeeAmount.value) || 0
      return Math.max(0, amount - fee).toFixed(6)
    })
    
    const canSubmitWithdraw = computed(() => {
      return withdrawForm.network &&
             withdrawForm.address &&
             addressValidation.valid &&
             withdrawForm.amount &&
             parseFloat(withdrawForm.amount) >= withdrawLimits.min &&
             parseFloat(withdrawForm.amount) <= balances.usdt &&
             !withdrawing.value
    })
    
    // 靜態數據
    const supportedNetworks = [
      { 
        label: 'TRC20 (TRON)', 
        value: 'TRC20', 
        fee: '免費', 
        withdrawFee: '2',
        confirmations: '1'
      },
      { 
        label: 'ERC20 (Ethereum)', 
        value: 'ERC20', 
        fee: '高', 
        withdrawFee: '15',
        confirmations: '12'
      },
      { 
        label: 'BSC (Binance Smart Chain)', 
        value: 'BSC', 
        fee: '低', 
        withdrawFee: '1',
        confirmations: '3'
      }
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
    
    const formatTime = (timestamp) => {
      return new Date(timestamp).toLocaleDateString('zh-TW')
    }
    
    const formatAmount = (transaction) => {
      if (transaction.currency === 'USDT') {
        return formatCrypto(transaction.amount)
      }
      return formatPrice(transaction.amount)
    }
    
    const getTransactionIcon = (type) => {
      const icons = {
        'DEPOSIT': 'icon-arrow-down',
        'WITHDRAW': 'icon-arrow-up', 
        'BUY': 'icon-plus',
        'SELL': 'icon-minus',
        'TRANSFER_IN': 'icon-arrow-down',
        'TRANSFER_OUT': 'icon-arrow-up'
      }
      return icons[type] || 'icon-transaction'
    }
    
    const getTransactionTypeText = (type) => {
      const texts = {
        'DEPOSIT': '充值',
        'WITHDRAW': '提現',
        'BUY': '買入',
        'SELL': '賣出',
        'TRANSFER_IN': '轉入',
        'TRANSFER_OUT': '轉出'
      }
      return texts[type] || type
    }
    
    const getAmountClass = (type) => {
      return ['DEPOSIT', 'BUY', 'TRANSFER_IN'].includes(type) ? 'positive' : 'negative'
    }
    
    const getAmountPrefix = (type) => {
      return ['DEPOSIT', 'BUY', 'TRANSFER_IN'].includes(type) ? '+' : '-'
    }
    
    const getStatusText = (status) => {
      const texts = {
        'PENDING': '處理中',
        'PROCESSING': '確認中',
        'COMPLETED': '已完成',
        'FAILED': '失敗',
        'CANCELLED': '已取消'
      }
      return texts[status] || status
    }
    
    const getConfirmations = (network) => {
      const networkData = supportedNetworks.find(n => n.value === network)
      return networkData ? networkData.confirmations : '1'
    }
    
    // API 調用
    const refreshBalance = async () => {
      if (refreshing.value) return
      
      refreshing.value = true
      try {
        const response = await api.get('/wallet/balance')
        if (response.data.success) {
          Object.assign(balances, response.data.data)
        }
      } catch (error) {
        console.error('刷新餘額失敗:', error)
      } finally {
        refreshing.value = false
      }
    }
    
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
    
    const loadDepositAddress = async () => {
      try {
        const response = await api.get(`/wallet/address?currency=USDT`)
        if (response.data.success) {
          depositAddress.value = response.data.data.address
        }
      } catch (error) {
        console.error('獲取充值地址失敗:', error)
      }
    }
    
    const loadRecentTransactions = async () => {
      try {
        const response = await api.get('/wallet/transactions?pageSize=10')
        if (response.data.success) {
          recentTransactions.value = response.data.data.records
        }
      } catch (error) {
        console.error('獲取交易記錄失敗:', error)
      }
    }
    
    const loadWithdrawLimits = async () => {
      try {
        const response = await api.get('/wallet/withdraw-info?currency=USDT')
        if (response.data.success) {
          Object.assign(withdrawLimits, response.data.data)
        }
      } catch (error) {
        console.error('獲取提現限額失敗:', error)
      }
    }
    
    const validateAddress = async () => {
      if (!withdrawForm.address || !withdrawForm.network) {
        addressValidation.checked = false
        return
      }
      
      try {
        const response = await api.post('/wallet/validate-address', {
          address: withdrawForm.address,
          network: withdrawForm.network
        })
        
        addressValidation.checked = true
        addressValidation.valid = response.data.success && response.data.data.valid
      } catch (error) {
        addressValidation.checked = true
        addressValidation.valid = false
      }
    }
    
    const calculateWithdrawFee = () => {
      // 可以添加手續費計算邏輯
    }
    
    const setMaxAmount = () => {
      const fee = parseFloat(withdrawFeeAmount.value) || 0
      const maxAmount = Math.max(0, balances.usdt - fee)
      withdrawForm.amount = maxAmount.toFixed(6)
    }
    
    const copyAddress = async () => {
      try {
        await navigator.clipboard.writeText(depositAddress.value)
        alert('地址已複製到剪貼板')
      } catch (error) {
        console.error('複製失敗:', error)
      }
    }
    
    const submitWithdraw = async () => {
      if (!canSubmitWithdraw.value) return
      
      withdrawing.value = true
      try {
        const response = await api.post('/wallet/withdraw', {
          amount: parseFloat(withdrawForm.amount),
          toAddress: withdrawForm.address,
          network: withdrawForm.network
        })
        
        if (response.data.success) {
          showWithdrawModal.value = false
          // 重置表單
          Object.assign(withdrawForm, { network: '', address: '', amount: '' })
          addressValidation.checked = false
          addressValidation.valid = false
          
          // 刷新餘額
          refreshBalance()
          
          alert('提現申請已提交，請等待審核')
        } else {
          alert('提現失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('提現失敗:', error)
        alert('提現失敗，請稍後重試')
      } finally {
        withdrawing.value = false
      }
    }
    
    // 生命週期
    onMounted(() => {
      refreshBalance()
      loadCurrentPrice()
      loadDepositAddress()
      loadRecentTransactions()
      loadWithdrawLimits()
    })
    
    return {
      refreshing,
      showDepositModal,
      showWithdrawModal,
      withdrawing,
      selectedNetwork,
      depositAddress,
      balances,
      currentPrice,
      withdrawForm,
      addressValidation,
      withdrawLimits,
      recentTransactions,
      totalValue,
      withdrawFeeAmount,
      actualReceiveAmount,
      canSubmitWithdraw,
      supportedNetworks,
      formatPrice,
      formatCrypto,
      formatTime,
      formatAmount,
      getTransactionIcon,
      getTransactionTypeText,
      getAmountClass,
      getAmountPrefix,
      getStatusText,
      getConfirmations,
      refreshBalance,
      validateAddress,
      calculateWithdrawFee,
      setMaxAmount,
      copyAddress,
      submitWithdraw
    }
  }
}
</script>

<style scoped>
.wallet-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.balance-section {
  margin-bottom: 24px;
}

.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.balance-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.balance-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.3);
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

.balance-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.balance-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.1);
  padding: 20px;
  border-radius: 12px;
}

.currency-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
}

.balance-info .currency {
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.balance-info .amount {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.balance-info .equivalent {
  font-size: 14px;
  opacity: 0.7;
}

.total-balance {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.total-balance .label {
  font-size: 16px;
  opacity: 0.8;
}

.total-balance .total {
  font-size: 24px;
  font-weight: 600;
}

.action-section {
  margin-bottom: 32px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  color: #374151;
}

.action-btn:hover {
  background: #f9fafb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-btn i {
  font-size: 24px;
  margin-bottom: 4px;
}

.action-btn.deposit i {
  color: #10b981;
}

.action-btn.withdraw i {
  color: #ef4444;
}

.action-btn.trade i {
  color: #2563eb;
}

.action-btn.history i {
  color: #6b7280;
}

.transactions-section {
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

.transactions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.transaction-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.transaction-item:hover {
  background: #f9fafb;
}

.transaction-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.transaction-icon.deposit {
  background: #10b981;
}

.transaction-icon.withdraw {
  background: #ef4444;
}

.transaction-icon.buy {
  background: #2563eb;
}

.transaction-icon.sell {
  background: #f59e0b;
}

.transaction-info {
  flex: 1;
}

.transaction-type {
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.transaction-time {
  font-size: 14px;
  color: #6b7280;
}

.transaction-amount {
  text-align: right;
}

.transaction-amount .amount {
  font-weight: 600;
  margin-bottom: 4px;
}

.transaction-amount.positive .amount {
  color: #10b981;
}

.transaction-amount.negative .amount {
  color: #ef4444;
}

.transaction-amount .status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.status.pending {
  background: #fef3c7;
  color: #92400e;
}

.status.processing {
  background: #dbeafe;
  color: #1e40af;
}

.status.completed {
  background: #d1fae5;
  color: #065f46;
}

.no-transactions {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
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

.network-selector {
  margin-bottom: 24px;
}

.network-selector label {
  display: block;
  margin-bottom: 12px;
  font-weight: 500;
  color: #374151;
}

.network-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.network-btn {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.network-btn:hover {
  background: #f9fafb;
}

.network-btn.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.network-name {
  font-weight: 500;
  color: #1a1a1a;
}

.network-fee {
  font-size: 14px;
  color: #6b7280;
}

.address-section {
  margin-bottom: 24px;
}

.address-section label {
  display: block;
  margin-bottom: 12px;
  font-weight: 500;
  color: #374151;
}

.address-display {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.address-text {
  flex: 1;
  padding: 12px;
  background: #f3f4f6;
  border-radius: 6px;
  font-family: monospace;
  font-size: 14px;
  word-break: break-all;
}

.copy-btn {
  padding: 12px 16px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.qr-code {
  display: flex;
  justify-content: center;
}

.qr-placeholder {
  width: 200px;
  height: 200px;
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

.qr-address {
  margin-top: 8px;
  font-size: 12px;
  word-break: break-all;
  text-align: center;
}

.deposit-notes {
  background: #fef3c7;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #f59e0b;
}

.deposit-notes h4 {
  margin: 0 0 12px 0;
  color: #92400e;
}

.deposit-notes ul {
  margin: 0;
  padding-left: 20px;
  color: #92400e;
}

.deposit-notes li {
  margin-bottom: 4px;
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

.form-group input,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 16px;
}

.amount-input {
  display: flex;
  gap: 8px;
}

.amount-input input {
  flex: 1;
}

.max-btn {
  padding: 12px 16px;
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.balance-hint {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
}

.address-validation {
  margin-top: 8px;
}

.validation-status {
  font-size: 14px;
  font-weight: 500;
}

.validation-status.valid {
  color: #10b981;
}

.validation-status.invalid {
  color: #ef4444;
}

.fee-summary {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.fee-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.fee-item:last-child {
  margin-bottom: 0;
  font-weight: 600;
  color: #1a1a1a;
}

.withdraw-limits {
  background: #eff6ff;
  padding: 16px;
  border-radius: 8px;
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

.submit-btn {
  width: 100%;
  padding: 16px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .wallet-view {
    padding: 16px;
  }
  
  .balance-grid {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .modal {
    width: 95%;
  }
}
</style>