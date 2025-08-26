<template>
  <div class="wallet-withdraw-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">钱包提现</h1>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
        将您的资产提现到外部钱包或银行账户
      </p>
    </div>

    <!-- KYC 状态检查 -->
    <div class="mt-6" v-if="!kycVerified">
      <el-alert
        title="需要完成KYC验证"
        description="为了确保资金安全，提现功能需要完成身份验证"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <div class="mt-3">
            <el-button type="primary" @click="goToKyc">
              立即认证
            </el-button>
          </div>
        </template>
      </el-alert>
    </div>

    <div v-else>
      <!-- 可用余额 -->
      <div class="mt-6">
        <el-card class="balance-card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-600 dark:text-gray-400">可提现余额</p>
              <p class="text-3xl font-bold text-gray-900 dark:text-white">
                {{ formatCurrency(availableBalance) }}
              </p>
              <p class="text-sm text-gray-500 mt-1">
                冻结资金: {{ formatCurrency(frozenBalance) }}
              </p>
            </div>
            <div class="balance-icon">
              <el-icon :size="40" class="text-green-500">
                <Money />
              </el-icon>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 提现表单 -->
      <div class="mt-6">
        <el-card>
          <template #header>
            <span class="text-lg font-medium">提现信息</span>
          </template>

          <el-form
            ref="formRef"
            :model="withdrawForm"
            :rules="formRules"
            label-width="120px"
            size="large"
          >
            <!-- 提现方式选择 -->
            <el-form-item label="提现方式" prop="method">
              <el-radio-group v-model="withdrawForm.method" size="large">
                <el-radio-button 
                  v-for="method in withdrawMethods" 
                  :key="method.id"
                  :label="method.id"
                  :disabled="!method.enabled"
                >
                  <div class="flex items-center space-x-2">
                    <img :src="method.icon" :alt="method.name" class="w-6 h-6" />
                    <span>{{ method.name }}</span>
                  </div>
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <!-- 币种选择 -->
            <el-form-item label="币种" prop="currency">
              <el-select 
                v-model="withdrawForm.currency" 
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

            <!-- 网络选择（加密货币） -->
            <el-form-item 
              v-if="withdrawForm.method === 'crypto'" 
              label="网络" 
              prop="network"
            >
              <el-select v-model="withdrawForm.network" placeholder="请选择网络">
                <el-option 
                  v-for="network in availableNetworks" 
                  :key="network.id"
                  :label="network.name"
                  :value="network.id"
                >
                  <div class="flex items-center justify-between">
                    <span>{{ network.name }}</span>
                    <span class="text-sm text-gray-500">
                      手续费: {{ network.fee }} {{ withdrawForm.currency }}
                    </span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 提现地址（加密货币） -->
            <el-form-item 
              v-if="withdrawForm.method === 'crypto'" 
              label="提现地址" 
              prop="address"
            >
              <el-input
                v-model="withdrawForm.address"
                placeholder="请输入钱包地址"
                clearable
              >
                <template #append>
                  <el-button @click="showAddressBook">
                    地址簿
                  </el-button>
                </template>
              </el-input>
              <div class="mt-2">
                <el-checkbox v-model="saveToAddressBook">
                  保存到地址簿
                </el-checkbox>
              </div>
            </el-form-item>

            <!-- 银行卡选择（银行提现） -->
            <el-form-item 
              v-if="withdrawForm.method === 'bank'" 
              label="银行卡" 
              prop="bankCard"
            >
              <el-select v-model="withdrawForm.bankCard" placeholder="请选择银行卡">
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

            <!-- 提现金额 -->
            <el-form-item label="提现金额" prop="amount">
              <el-input
                v-model="withdrawForm.amount"
                placeholder="请输入提现金额"
                type="number"
                :min="selectedCurrency?.minWithdraw || 0"
                :max="Math.min(selectedCurrency?.maxWithdraw || 999999999, availableBalance)"
              >
                <template #append>{{ withdrawForm.currency }}</template>
              </el-input>
              <div class="mt-2 flex items-center justify-between">
                <div class="text-sm text-gray-500">
                  <p v-if="selectedCurrency">
                    最小提现: {{ formatCurrency(selectedCurrency.minWithdraw, withdrawForm.currency) }}
                    | 最大提现: {{ formatCurrency(Math.min(selectedCurrency.maxWithdraw, availableBalance), withdrawForm.currency) }}
                  </p>
                </div>
                <div class="space-x-2">
                  <el-button 
                    size="small" 
                    @click="setAmount(0.25)"
                    text
                  >
                    25%
                  </el-button>
                  <el-button 
                    size="small" 
                    @click="setAmount(0.5)"
                    text
                  >
                    50%
                  </el-button>
                  <el-button 
                    size="small" 
                    @click="setAmount(0.75)"
                    text
                  >
                    75%
                  </el-button>
                  <el-button 
                    size="small" 
                    @click="setAmount(1)"
                    text
                  >
                    全部
                  </el-button>
                </div>
              </div>
            </el-form-item>

            <!-- 地址标签（加密货币） -->
            <el-form-item 
              v-if="withdrawForm.method === 'crypto' && saveToAddressBook" 
              label="地址标签"
            >
              <el-input
                v-model="withdrawForm.addressLabel"
                placeholder="为此地址添加标签（可选）"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>

            <!-- 资金密码 -->
            <el-form-item label="资金密码" prop="fundPassword">
              <el-input
                v-model="withdrawForm.fundPassword"
                type="password"
                placeholder="请输入资金密码"
                show-password
                maxlength="20"
              />
              <div class="mt-2">
                <el-link type="primary" @click="showForgotPassword">
                  忘记资金密码？
                </el-link>
              </div>
            </el-form-item>

            <!-- 验证码 -->
            <el-form-item label="验证码" prop="verificationCode">
              <div class="flex items-center space-x-3">
                <el-input
                  v-model="withdrawForm.verificationCode"
                  placeholder="请输入验证码"
                  maxlength="6"
                  class="flex-1"
                />
                <el-button 
                  @click="sendVerificationCode"
                  :loading="sendingCode"
                  :disabled="countdown > 0"
                >
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>

          <!-- 费用明细 -->
          <div class="mt-6" v-if="withdrawForm.amount">
            <el-card class="fee-summary">
              <template #header>
                <span class="font-medium">费用明细</span>
              </template>
              
              <div class="space-y-3">
                <div class="flex justify-between">
                  <span>提现金额</span>
                  <span class="font-medium">
                    {{ formatCurrency(withdrawForm.amount, withdrawForm.currency) }}
                  </span>
                </div>
                <div class="flex justify-between">
                  <span>网络手续费</span>
                  <span class="font-medium">
                    {{ formatCurrency(calculatedFee, withdrawForm.currency) }}
                  </span>
                </div>
                <div class="flex justify-between">
                  <span>平台手续费</span>
                  <span class="font-medium">
                    {{ formatCurrency(platformFee, withdrawForm.currency) }}
                  </span>
                </div>
                <el-divider />
                <div class="flex justify-between text-lg font-semibold">
                  <span>实际到账</span>
                  <span>
                    {{ formatCurrency(actualAmount, withdrawForm.currency) }}
                  </span>
                </div>
                <div class="text-sm text-gray-500">
                  预计到账时间: {{ estimatedTime }}
                </div>
              </div>
            </el-card>
          </div>

          <!-- 安全提醒 -->
          <el-alert
            type="warning"
            :closable="false"
            class="mt-6"
          >
            <template #title>
              <strong>安全提醒</strong>
            </template>
            <ul class="list-disc list-inside space-y-1 text-sm">
              <li>请仔细核对提现地址，转账后无法撤销</li>
              <li>提现前请确认网络类型正确，避免资产丢失</li>
              <li>大额提现可能需要人工审核，请耐心等待</li>
              <li>如有疑问请及时联系客服</li>
            </ul>
          </el-alert>

          <!-- 操作按钮 -->
          <div class="mt-6 flex justify-end space-x-3">
            <el-button @click="resetForm">重置</el-button>
            <el-button 
              type="primary" 
              @click="submitWithdraw"
              :loading="submitting"
              :disabled="!canSubmit"
              size="large"
            >
              确认提现
            </el-button>
          </div>
        </el-card>
      </div>

      <!-- 提现记录 -->
      <div class="mt-6">
        <el-card>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-lg font-medium">最近提现记录</span>
              <el-button 
                text 
                type="primary" 
                @click="viewAllWithdrawals"
              >
                查看全部
              </el-button>
            </div>
          </template>

          <el-table 
            :data="recentWithdrawals" 
            style="width: 100%"
            v-loading="loadingWithdrawals"
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
            <el-table-column prop="fee" label="手续费" width="100">
              <template #default="{ row }">
                {{ formatCurrency(row.fee, row.currency) }}
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
            <el-table-column prop="address" label="地址/账号" min-width="200">
              <template #default="{ row }">
                <span v-if="row.address">
                  {{ row.address.slice(0, 10) }}...{{ row.address.slice(-10) }}
                </span>
                <span v-else-if="row.bankAccount">
                  {{ row.bankAccount }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button 
                  text 
                  type="primary" 
                  @click="viewWithdrawalDetail(row.id)"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty 
            v-if="!loadingWithdrawals && recentWithdrawals.length === 0"
            description="暂无提现记录"
          />
        </el-card>
      </div>
    </div>

    <!-- 地址簿弹窗 -->
    <el-dialog
      v-model="showAddressBookDialog"
      title="地址簿"
      width="600px"
      center
    >
      <el-table :data="addressBook" style="width: 100%">
        <el-table-column prop="label" label="标签" width="120" />
        <el-table-column prop="address" label="地址" min-width="300">
          <template #default="{ row }">
            <span class="font-mono text-sm">{{ row.address }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="network" label="网络" width="100" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button 
              text 
              type="primary" 
              @click="selectAddress(row)"
            >
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty 
        v-if="addressBook.length === 0"
        description="暂无保存的地址"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Money } from '@element-plus/icons-vue'
import { 
  getWalletBalance,
  getWithdrawMethods,
  createWithdrawOrder,
  getWithdrawHistory,
  sendWithdrawVerificationCode,
  getAddressBook
} from '@/api/wallet'
import { getKycStatus } from '@/api/kyc'

// 响应式状态
const router = useRouter()
const formRef = ref()
const showAddressBookDialog = ref(false)
const submitting = ref(false)
const sendingCode = ref(false)
const loadingWithdrawals = ref(false)
const countdown = ref(0)
const saveToAddressBook = ref(false)

const kycVerified = ref(true) // 默认已验证，实际应从API获取
const availableBalance = ref(0)
const frozenBalance = ref(0)

// 表单数据
const withdrawForm = reactive({
  method: 'crypto',
  currency: 'USDT',
  network: '',
  address: '',
  addressLabel: '',
  bankCard: '',
  amount: '',
  fundPassword: '',
  verificationCode: ''
})

// 提现方式
const withdrawMethods = ref([
  {
    id: 'crypto',
    name: '加密货币',
    icon: '/images/crypto-icon.png',
    enabled: true
  },
  {
    id: 'bank',
    name: '银行转账',
    icon: '/images/bank-icon.png',
    enabled: true
  }
])

// 可用币种
const availableCurrencies = ref([
  {
    code: 'USDT',
    name: 'Tether',
    balance: 0,
    minWithdraw: 10,
    maxWithdraw: 100000
  },
  {
    code: 'BTC',
    name: 'Bitcoin',
    balance: 0,
    minWithdraw: 0.001,
    maxWithdraw: 10
  },
  {
    code: 'ETH',
    name: 'Ethereum',
    balance: 0,
    minWithdraw: 0.01,
    maxWithdraw: 100
  }
])

// 可用网络
const availableNetworks = ref([
  {
    id: 'trc20',
    name: 'TRC20 (Tron)',
    fee: 1
  },
  {
    id: 'erc20',
    name: 'ERC20 (Ethereum)',
    fee: 15
  },
  {
    id: 'bep20',
    name: 'BEP20 (BSC)',
    fee: 0.8
  }
])

// 银行卡列表
const bankCards = ref([
  {
    id: '1',
    bankName: '中国银行',
    cardNumber: '1234567890123456'
  }
])

// 地址簿
const addressBook = ref([])

// 提现记录
const recentWithdrawals = ref([])

// 表单验证规则
const formRules = {
  method: [
    { required: true, message: '请选择提现方式', trigger: 'change' }
  ],
  currency: [
    { required: true, message: '请选择币种', trigger: 'change' }
  ],
  network: [
    { required: true, message: '请选择网络', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入提现地址', trigger: 'blur' },
    { min: 20, message: '请输入有效的地址', trigger: 'blur' }
  ],
  bankCard: [
    { required: true, message: '请选择银行卡', trigger: 'change' }
  ],
  amount: [
    { required: true, message: '请输入提现金额', trigger: 'blur' },
    { pattern: /^\d+(\.\d{1,8})?$/, message: '请输入有效的金额', trigger: 'blur' }
  ],
  fundPassword: [
    { required: true, message: '请输入资金密码', trigger: 'blur' },
    { min: 6, max: 20, message: '资金密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位数字验证码', trigger: 'blur' }
  ]
}

// 计算属性
const selectedCurrency = computed(() => {
  return availableCurrencies.value.find(c => c.code === withdrawForm.currency)
})

const selectedNetwork = computed(() => {
  return availableNetworks.value.find(n => n.id === withdrawForm.network)
})

const calculatedFee = computed(() => {
  if (!withdrawForm.amount) return 0
  
  if (withdrawForm.method === 'crypto' && selectedNetwork.value) {
    return selectedNetwork.value.fee
  } else if (withdrawForm.method === 'bank') {
    return parseFloat(withdrawForm.amount) * 0.001 // 0.1% 手续费
  }
  return 0
})

const platformFee = computed(() => {
  if (!withdrawForm.amount) return 0
  return parseFloat(withdrawForm.amount) * 0.002 // 0.2% 平台手续费
})

const actualAmount = computed(() => {
  if (!withdrawForm.amount) return 0
  return parseFloat(withdrawForm.amount) - calculatedFee.value - platformFee.value
})

const estimatedTime = computed(() => {
  if (withdrawForm.method === 'crypto') {
    return '10-30分钟'
  } else {
    return '1-3个工作日'
  }
})

const canSubmit = computed(() => {
  if (!withdrawForm.currency || !withdrawForm.amount || !withdrawForm.fundPassword || !withdrawForm.verificationCode) {
    return false
  }
  
  const amount = parseFloat(withdrawForm.amount)
  const currency = selectedCurrency.value
  
  if (!currency || amount < currency.minWithdraw || amount > Math.min(currency.maxWithdraw, availableBalance.value)) {
    return false
  }

  if (withdrawForm.method === 'crypto' && (!withdrawForm.network || !withdrawForm.address)) {
    return false
  }
  
  if (withdrawForm.method === 'bank' && !withdrawForm.bankCard) {
    return false
  }

  return true
})

// 方法
const fetchWalletBalance = async () => {
  try {
    const response = await getWalletBalance()
    if (response.data.success) {
      availableBalance.value = response.data.data.balance
      frozenBalance.value = response.data.data.frozen
    }
  } catch (error) {
    console.error('获取钱包余额失败:', error)
  }
}

const fetchWithdrawHistory = async () => {
  try {
    loadingWithdrawals.value = true
    const response = await getWithdrawHistory({ limit: 5 })
    if (response.data.success) {
      recentWithdrawals.value = response.data.data.records
    }
  } catch (error) {
    console.error('获取提现记录失败:', error)
  } finally {
    loadingWithdrawals.value = false
  }
}

const fetchAddressBook = async () => {
  try {
    const response = await getAddressBook()
    if (response.data.success) {
      addressBook.value = response.data.data
    }
  } catch (error) {
    console.error('获取地址簿失败:', error)
  }
}

const handleCurrencyChange = () => {
  withdrawForm.network = ''
  withdrawForm.address = ''
}

const setAmount = (percentage: number) => {
  const maxAmount = Math.min(
    selectedCurrency.value?.maxWithdraw || 999999999,
    availableBalance.value
  )
  withdrawForm.amount = (maxAmount * percentage).toFixed(8)
}

const sendVerificationCode = async () => {
  try {
    sendingCode.value = true
    const response = await sendWithdrawVerificationCode()
    
    if (response.data.success) {
      ElMessage.success('验证码已发送')
      startCountdown()
    } else {
      ElMessage.error(response.data.message || '发送验证码失败')
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    ElMessage.error('发送验证码失败，请重试')
  } finally {
    sendingCode.value = false
  }
}

const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

const submitWithdraw = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    const confirmed = await ElMessageBox.confirm(
      `确认提现 ${withdrawForm.amount} ${withdrawForm.currency} 吗？`,
      '确认提现',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (!confirmed) return

    submitting.value = true
    const response = await createWithdrawOrder({
      ...withdrawForm,
      amount: parseFloat(withdrawForm.amount),
      saveToAddressBook: saveToAddressBook.value
    })

    if (response.data.success) {
      ElMessage.success('提现申请已提交')
      resetForm()
      fetchWithdrawHistory()
      fetchWalletBalance()
    } else {
      ElMessage.error(response.data.message || '提现申请失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提现申请失败:', error)
      ElMessage.error('提现申请失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  Object.assign(withdrawForm, {
    method: 'crypto',
    currency: 'USDT',
    network: '',
    address: '',
    addressLabel: '',
    bankCard: '',
    amount: '',
    fundPassword: '',
    verificationCode: ''
  })
  saveToAddressBook.value = false
}

const showAddressBook = () => {
  fetchAddressBook()
  showAddressBookDialog.value = true
}

const selectAddress = (address: any) => {
  withdrawForm.address = address.address
  withdrawForm.network = address.network
  showAddressBookDialog.value = false
}

const showAddBankCard = () => {
  ElMessage.info('添加银行卡功能开发中')
}

const showForgotPassword = () => {
  ElMessage.info('忘记密码功能开发中')
}

const goToKyc = () => {
  router.push('/kyc/status')
}

const viewAllWithdrawals = () => {
  router.push('/wallet/transactions?type=withdraw')
}

const viewWithdrawalDetail = (id: string) => {
  ElMessage.info(`查看提现详情: ${id}`)
}

const formatCurrency = (amount: number | string, currency = 'USDT') => {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '0'
  return `${num.toLocaleString()} ${currency}`
}

const formatDateTime = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const getMethodName = (method: string) => {
  const map = {
    crypto: '加密货币',
    bank: '银行转账'
  }
  return map[method as keyof typeof map] || method
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
onMounted(async () => {
  await fetchWalletBalance()
  await fetchWithdrawHistory()
  
  // 检查KYC状态
  try {
    const response = await getKycStatus()
    if (response.data.success) {
      kycVerified.value = response.data.data?.status === 'approved'
    }
  } catch (error) {
    console.error('获取KYC状态失败:', error)
  }
})
</script>

<style scoped>
.wallet-withdraw-view {
  @apply max-w-6xl mx-auto p-6;
}

.page-header {
  @apply mb-6;
}

.balance-card {
  @apply bg-gradient-to-r from-green-500 to-blue-600 text-white;
}

.balance-card :deep(.el-card__body) {
  @apply bg-transparent;
}

.balance-icon {
  @apply w-16 h-16 bg-white bg-opacity-20 rounded-full flex items-center justify-center;
}

.fee-summary {
  @apply bg-gray-50 dark:bg-gray-800;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .wallet-withdraw-view {
    @apply p-4;
  }
  
  .balance-card .flex {
    @apply flex-col space-y-4;
  }
}
</style>