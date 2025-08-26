<template>
  <div class="wallet-deposit-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">钱包充值</h1>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
        选择合适的充值方式向您的钱包充值
      </p>
    </div>

    <!-- 当前余额 -->
    <div class="mt-6">
      <el-card class="balance-card">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-600 dark:text-gray-400">当前余额</p>
            <p class="text-3xl font-bold text-gray-900 dark:text-white">
              {{ formatCurrency(walletBalance) }}
            </p>
          </div>
          <div class="balance-icon">
            <el-icon :size="40" class="text-blue-500">
              <Wallet />
            </el-icon>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 充值方式选择 -->
    <div class="mt-6">
      <el-card>
        <template #header>
          <span class="text-lg font-medium">选择充值方式</span>
        </template>
        
        <el-radio-group v-model="depositMethod" size="large" class="deposit-methods">
          <el-radio-button 
            v-for="method in depositMethods" 
            :key="method.id"
            :label="method.id"
            :disabled="!method.enabled"
            class="deposit-method-option"
          >
            <div class="flex items-center space-x-3">
              <img :src="method.icon" :alt="method.name" class="w-8 h-8" />
              <div>
                <div class="font-medium">{{ method.name }}</div>
                <div class="text-xs text-gray-500">{{ method.description }}</div>
              </div>
            </div>
          </el-radio-button>
        </el-radio-group>
      </el-card>
    </div>

    <!-- 充值表单 -->
    <div class="mt-6" v-if="depositMethod">
      <el-card>
        <template #header>
          <span class="text-lg font-medium">充值信息</span>
        </template>

        <el-form
          ref="formRef"
          :model="depositForm"
          :rules="formRules"
          label-width="120px"
          size="large"
        >
          <!-- 币种选择 -->
          <el-form-item label="币种" prop="currency">
            <el-select 
              v-model="depositForm.currency" 
              placeholder="请选择币种"
              @change="handleCurrencyChange"
            >
              <el-option 
                v-for="currency in availableCurrencies" 
                :key="currency.code"
                :label="`${currency.name} (${currency.code})`"
                :value="currency.code"
              >
                <div class="flex items-center justify-between">
                  <span>{{ currency.name }} ({{ currency.code }})</span>
                  <span class="text-sm text-gray-500">
                    余额: {{ formatCurrency(currency.balance, currency.code) }}
                  </span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- 充值金额 -->
          <el-form-item label="充值金额" prop="amount">
            <el-input
              v-model="depositForm.amount"
              placeholder="请输入充值金额"
              type="number"
              :min="selectedCurrency?.minDeposit || 0"
              :max="selectedCurrency?.maxDeposit || 999999999"
            >
              <template #append>{{ depositForm.currency }}</template>
            </el-input>
            <div class="mt-2 text-sm text-gray-500">
              <p v-if="selectedCurrency">
                最小充值: {{ formatCurrency(selectedCurrency.minDeposit, depositForm.currency) }}
                | 最大充值: {{ formatCurrency(selectedCurrency.maxDeposit, depositForm.currency) }}
              </p>
              <p v-if="depositForm.amount && exchangeRate">
                约等于: {{ formatCurrency(parseFloat(depositForm.amount) * exchangeRate, 'USD') }}
              </p>
            </div>
          </el-form-item>

          <!-- 网络选择（加密货币） -->
          <el-form-item 
            v-if="depositMethod === 'crypto'" 
            label="网络" 
            prop="network"
          >
            <el-select v-model="depositForm.network" placeholder="请选择网络">
              <el-option 
                v-for="network in availableNetworks" 
                :key="network.id"
                :label="network.name"
                :value="network.id"
              >
                <div class="flex items-center justify-between">
                  <span>{{ network.name }}</span>
                  <span class="text-sm text-gray-500">
                    手续费: {{ network.fee }} {{ depositForm.currency }}
                  </span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- 银行卡选择（银行转账） -->
          <el-form-item 
            v-if="depositMethod === 'bank'" 
            label="银行卡" 
            prop="bankCard"
          >
            <el-select v-model="depositForm.bankCard" placeholder="请选择银行卡">
              <el-option 
                v-for="card in bankCards" 
                :key="card.id"
                :label="`${card.bankName} (*${card.cardNumber.slice(-4)})`"
                :value="card.id"
              />
            </el-select>
            <div class="mt-2">
              <el-link type="primary" @click="showAddBankCard">
                + 添加新银行卡
              </el-link>
            </div>
          </el-form-item>
        </el-form>

        <!-- 费用明细 -->
        <div class="mt-6" v-if="depositForm.amount">
          <el-card class="fee-summary">
            <template #header>
              <span class="font-medium">费用明细</span>
            </template>
            
            <div class="space-y-3">
              <div class="flex justify-between">
                <span>充值金额</span>
                <span class="font-medium">
                  {{ formatCurrency(depositForm.amount, depositForm.currency) }}
                </span>
              </div>
              <div class="flex justify-between">
                <span>手续费</span>
                <span class="font-medium">
                  {{ formatCurrency(calculatedFee, depositForm.currency) }}
                </span>
              </div>
              <el-divider />
              <div class="flex justify-between text-lg font-semibold">
                <span>实际到账</span>
                <span>
                  {{ formatCurrency(actualAmount, depositForm.currency) }}
                </span>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 操作按钮 -->
        <div class="mt-6 flex justify-end space-x-3">
          <el-button @click="resetForm">重置</el-button>
          <el-button 
            type="primary" 
            @click="proceedDeposit"
            :loading="processing"
            :disabled="!canProceed"
          >
            确认充值
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 充值指引弹窗 -->
    <el-dialog
      v-model="showDepositGuide"
      title="充值指引"
      width="600px"
      center
    >
      <!-- 加密货币充值指引 -->
      <div v-if="depositMethod === 'crypto'" class="crypto-guide">
        <div class="text-center mb-6">
          <h3 class="text-lg font-medium mb-2">{{ selectedCurrency?.name }} 充值地址</h3>
          <p class="text-sm text-gray-500">请向以下地址转账</p>
        </div>

        <!-- 充值地址 -->
        <div class="deposit-address-section">
          <el-card class="text-center">
            <!-- 二维码 -->
            <div class="qr-code mb-4">
              <div class="w-48 h-48 mx-auto bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center">
                <span class="text-gray-500">QR Code</span>
              </div>
            </div>
            
            <!-- 地址 -->
            <div class="address-display">
              <p class="text-sm text-gray-600 dark:text-gray-400 mb-2">充值地址</p>
              <div class="flex items-center justify-center space-x-2">
                <el-input
                  :value="depositAddress"
                  readonly
                  class="text-center"
                />
                <el-button 
                  @click="copyAddress"
                  type="primary"
                  :icon="CopyDocument"
                >
                  复制
                </el-button>
              </div>
            </div>

            <!-- 网络信息 -->
            <div class="mt-4 p-4 bg-yellow-50 dark:bg-yellow-900/20 rounded-lg">
              <p class="text-sm text-yellow-700 dark:text-yellow-300">
                <strong>网络:</strong> {{ selectedNetwork?.name }}<br>
                <strong>最小充值:</strong> {{ formatCurrency(selectedCurrency?.minDeposit, depositForm.currency) }}<br>
                <strong>确认数:</strong> {{ selectedNetwork?.confirmations }} 个区块确认
              </p>
            </div>
          </el-card>
        </div>

        <!-- 注意事项 -->
        <el-alert
          type="warning"
          :closable="false"
          class="mt-4"
        >
          <template #title>
            <strong>重要提醒</strong>
          </template>
          <ul class="list-disc list-inside space-y-1 text-sm">
            <li>请确保转账网络与选择的网络一致</li>
            <li>转账金额必须大于最小充值限额</li>
            <li>充值到账需要等待区块确认，请耐心等待</li>
            <li>请勿向此地址充值其他币种，否则资产将无法找回</li>
          </ul>
        </el-alert>
      </div>

      <!-- 银行转账指引 -->
      <div v-if="depositMethod === 'bank'" class="bank-guide">
        <div class="text-center mb-6">
          <h3 class="text-lg font-medium mb-2">银行转账信息</h3>
          <p class="text-sm text-gray-500">请按以下信息进行转账</p>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="收款银行">交通银行</el-descriptions-item>
          <el-descriptions-item label="收款账号">1234567890123456</el-descriptions-item>
          <el-descriptions-item label="收款人">USDT Trading Platform</el-descriptions-item>
          <el-descriptions-item label="转账金额">
            {{ formatCurrency(depositForm.amount, 'CNY') }}
          </el-descriptions-item>
          <el-descriptions-item label="转账备注">
            {{ depositOrderNumber }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          :closable="false"
          class="mt-4"
        >
          <template #title>
            <strong>转账说明</strong>
          </template>
          <ul class="list-disc list-inside space-y-1 text-sm">
            <li>请务必在转账备注中填写订单号，否则无法及时到账</li>
            <li>转账金额必须与订单金额完全一致</li>
            <li>工作日转账通常2小时内到账，节假日可能延迟</li>
            <li>如有疑问请联系客服</li>
          </ul>
        </el-alert>
      </div>

      <template #footer>
        <div class="flex justify-between">
          <el-button @click="showDepositGuide = false">稍后充值</el-button>
          <el-button type="primary" @click="confirmDeposit">
            已完成转账
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 充值记录 -->
    <div class="mt-6">
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-lg font-medium">最近充值记录</span>
            <el-button 
              text 
              type="primary" 
              @click="viewAllDeposits"
            >
              查看全部
            </el-button>
          </div>
        </template>

        <el-table 
          :data="recentDeposits" 
          style="width: 100%"
          v-loading="loadingDeposits"
        >
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="currency" label="币种" width="100" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              {{ formatCurrency(row.amount, row.currency) }}
            </template>
          </el-table-column>
          <el-table-column prop="method" label="方式" width="100">
            <template #default="{ row }">
              {{ getMethodName(row.method) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="txHash" label="交易哈希" min-width="200">
            <template #default="{ row }">
              <el-link 
                v-if="row.txHash"
                type="primary" 
                @click="viewTransaction(row.txHash)"
              >
                {{ row.txHash.slice(0, 20) }}...
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <el-empty 
          v-if="!loadingDeposits && recentDeposits.length === 0"
          description="暂无充值记录"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Wallet,
  CopyDocument
} from '@element-plus/icons-vue'
import { 
  getWalletBalance,
  getDepositMethods,
  getDepositAddress,
  createDepositOrder,
  getDepositHistory
} from '@/api/wallet'

// 响应式状态
const router = useRouter()
const formRef = ref()
const showDepositGuide = ref(false)
const processing = ref(false)
const loadingDeposits = ref(false)

const walletBalance = ref(0)
const depositMethod = ref('')
const depositAddress = ref('')
const depositOrderNumber = ref('')

// 表单数据
const depositForm = reactive({
  currency: 'USDT',
  amount: '',
  network: '',
  bankCard: ''
})

// 充值方式
const depositMethods = ref([
  {
    id: 'crypto',
    name: '加密货币充值',
    description: '使用USDT、BTC等加密货币充值',
    icon: '/images/crypto-icon.png',
    enabled: true
  },
  {
    id: 'bank',
    name: '银行转账',
    description: '通过银行卡转账充值',
    icon: '/images/bank-icon.png',
    enabled: true
  },
  {
    id: 'alipay',
    name: '支付宝',
    description: '使用支付宝快速充值',
    icon: '/images/alipay-icon.png',
    enabled: false
  },
  {
    id: 'wechat',
    name: '微信支付',
    description: '使用微信支付快速充值',
    icon: '/images/wechat-icon.png',
    enabled: false
  }
])

// 可用币种
const availableCurrencies = ref([
  {
    code: 'USDT',
    name: 'Tether',
    balance: 0,
    minDeposit: 10,
    maxDeposit: 100000,
    exchangeRate: 1
  },
  {
    code: 'BTC',
    name: 'Bitcoin',
    balance: 0,
    minDeposit: 0.001,
    maxDeposit: 10,
    exchangeRate: 45000
  },
  {
    code: 'ETH',
    name: 'Ethereum',
    balance: 0,
    minDeposit: 0.01,
    maxDeposit: 100,
    exchangeRate: 3000
  }
])

// 可用网络
const availableNetworks = ref([
  {
    id: 'trc20',
    name: 'TRC20 (Tron)',
    fee: 1,
    confirmations: 1
  },
  {
    id: 'erc20',
    name: 'ERC20 (Ethereum)',
    fee: 5,
    confirmations: 12
  },
  {
    id: 'bep20',
    name: 'BEP20 (BSC)',
    fee: 0.5,
    confirmations: 3
  }
])

// 银行卡列表
const bankCards = ref([
  {
    id: '1',
    bankName: '中国银行',
    cardNumber: '1234567890123456'
  },
  {
    id: '2',
    bankName: '工商银行',
    cardNumber: '2345678901234567'
  }
])

// 充值记录
const recentDeposits = ref([])

// 表单验证规则
const formRules = {
  currency: [
    { required: true, message: '请选择币种', trigger: 'change' }
  ],
  amount: [
    { required: true, message: '请输入充值金额', trigger: 'blur' },
    { pattern: /^\d+(\.\d{1,8})?$/, message: '请输入有效的金额', trigger: 'blur' }
  ],
  network: [
    { required: true, message: '请选择网络', trigger: 'change' }
  ],
  bankCard: [
    { required: true, message: '请选择银行卡', trigger: 'change' }
  ]
}

// 计算属性
const selectedCurrency = computed(() => {
  return availableCurrencies.value.find(c => c.code === depositForm.currency)
})

const selectedNetwork = computed(() => {
  return availableNetworks.value.find(n => n.id === depositForm.network)
})

const exchangeRate = computed(() => {
  return selectedCurrency.value?.exchangeRate || 1
})

const calculatedFee = computed(() => {
  if (!depositForm.amount) return 0
  
  if (depositMethod.value === 'crypto' && selectedNetwork.value) {
    return selectedNetwork.value.fee
  } else if (depositMethod.value === 'bank') {
    return parseFloat(depositForm.amount) * 0.001 // 0.1% 手续费
  }
  return 0
})

const actualAmount = computed(() => {
  if (!depositForm.amount) return 0
  return parseFloat(depositForm.amount) - calculatedFee.value
})

const canProceed = computed(() => {
  if (!depositForm.currency || !depositForm.amount) return false
  
  const amount = parseFloat(depositForm.amount)
  const currency = selectedCurrency.value
  
  if (!currency || amount < currency.minDeposit || amount > currency.maxDeposit) {
    return false
  }

  if (depositMethod.value === 'crypto' && !depositForm.network) {
    return false
  }
  
  if (depositMethod.value === 'bank' && !depositForm.bankCard) {
    return false
  }

  return true
})

// 方法
const fetchWalletBalance = async () => {
  try {
    const response = await getWalletBalance()
    if (response.data.success) {
      walletBalance.value = response.data.data.balance
    }
  } catch (error) {
    console.error('获取钱包余额失败:', error)
  }
}

const fetchDepositHistory = async () => {
  try {
    loadingDeposits.value = true
    const response = await getDepositHistory({ limit: 5 })
    if (response.data.success) {
      recentDeposits.value = response.data.data.records
    }
  } catch (error) {
    console.error('获取充值记录失败:', error)
  } finally {
    loadingDeposits.value = false
  }
}

const handleCurrencyChange = () => {
  depositForm.network = ''
  depositAddress.value = ''
}

const proceedDeposit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    processing.value = true

    // 创建充值订单
    const response = await createDepositOrder({
      currency: depositForm.currency,
      amount: parseFloat(depositForm.amount),
      method: depositMethod.value,
      network: depositForm.network,
      bankCardId: depositForm.bankCard
    })

    if (response.data.success) {
      const order = response.data.data
      depositOrderNumber.value = order.orderNumber
      
      if (depositMethod.value === 'crypto') {
        // 获取充值地址
        const addressResponse = await getDepositAddress({
          currency: depositForm.currency,
          network: depositForm.network
        })
        
        if (addressResponse.data.success) {
          depositAddress.value = addressResponse.data.data.address
        }
      }
      
      showDepositGuide.value = true
    } else {
      ElMessage.error(response.data.message || '创建充值订单失败')
    }
  } catch (error) {
    console.error('创建充值订单失败:', error)
    ElMessage.error('创建充值订单失败，请重试')
  } finally {
    processing.value = false
  }
}

const copyAddress = async () => {
  try {
    await navigator.clipboard.writeText(depositAddress.value)
    ElMessage.success('地址已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const confirmDeposit = () => {
  showDepositGuide.value = false
  ElMessage.success('充值订单已提交，请等待到账')
  resetForm()
  fetchDepositHistory()
}

const resetForm = () => {
  depositForm.currency = 'USDT'
  depositForm.amount = ''
  depositForm.network = ''
  depositForm.bankCard = ''
  depositAddress.value = ''
  depositOrderNumber.value = ''
}

const showAddBankCard = () => {
  ElMessage.info('添加银行卡功能开发中')
}

const viewAllDeposits = () => {
  router.push('/wallet/transactions?type=deposit')
}

const viewTransaction = (txHash: string) => {
  ElMessage.info(`查看交易: ${txHash}`)
}

const formatCurrency = (amount: number | string, currency = 'USDT') => {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '0'
  
  if (currency === 'CNY') {
    return `¥${num.toLocaleString()}`
  } else if (currency === 'USD') {
    return `$${num.toLocaleString()}`
  } else {
    return `${num.toLocaleString()} ${currency}`
  }
}

const formatDateTime = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const getMethodName = (method: string) => {
  const map = {
    crypto: '加密货币',
    bank: '银行转账',
    alipay: '支付宝',
    wechat: '微信支付'
  }
  return map[method as keyof typeof map] || method
}

const getStatusName = (status: string) => {
  const map = {
    pending: '处理中',
    confirmed: '已确认',
    completed: '已完成',
    failed: '失败'
  }
  return map[status as keyof typeof map] || status
}

const getStatusType = (status: string) => {
  const map = {
    pending: 'warning',
    confirmed: 'info',
    completed: 'success',
    failed: 'danger'
  }
  return map[status as keyof typeof map] || 'info'
}

// 生命周期
onMounted(() => {
  fetchWalletBalance()
  fetchDepositHistory()
})
</script>

<style scoped>
.wallet-deposit-view {
  @apply max-w-6xl mx-auto p-6;
}

.page-header {
  @apply mb-6;
}

.balance-card {
  @apply bg-gradient-to-r from-blue-500 to-purple-600 text-white;
}

.balance-card :deep(.el-card__body) {
  @apply bg-transparent;
}

.balance-icon {
  @apply w-16 h-16 bg-white bg-opacity-20 rounded-full flex items-center justify-center;
}

.deposit-methods {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.deposit-methods :deep(.el-radio-button__inner) {
  @apply w-full p-4 border-2 border-gray-200 rounded-lg hover:border-blue-500;
}

.deposit-methods :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  @apply border-blue-500 bg-blue-50 text-blue-600;
}

.fee-summary {
  @apply bg-gray-50 dark:bg-gray-800;
}

.deposit-address-section {
  @apply mb-6;
}

.address-display input {
  @apply text-center font-mono;
}

.crypto-guide, .bank-guide {
  @apply space-y-4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .wallet-deposit-view {
    @apply p-4;
  }
  
  .deposit-methods {
    @apply grid-cols-1;
  }
  
  .balance-card .flex {
    @apply flex-col space-y-4;
  }
}
</style>