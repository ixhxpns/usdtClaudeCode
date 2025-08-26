<template>
  <div class="spot-trading-view">
    <!-- 交易对选择和价格信息 -->
    <div class="trading-header">
      <el-card class="mb-4">
        <div class="flex items-center justify-between">
          <!-- 交易对选择 -->
          <div class="flex items-center space-x-4">
            <el-select 
              v-model="selectedPair" 
              placeholder="选择交易对"
              @change="handlePairChange"
              size="large"
              class="w-48"
            >
              <el-option 
                v-for="pair in tradingPairs" 
                :key="pair.symbol"
                :label="pair.symbol"
                :value="pair.symbol"
              >
                <div class="flex items-center justify-between w-full">
                  <span class="font-medium">{{ pair.symbol }}</span>
                  <span class="text-sm text-gray-500">{{ pair.name }}</span>
                </div>
              </el-option>
            </el-select>

            <!-- 价格信息 -->
            <div v-if="currentPair" class="flex items-center space-x-6">
              <div>
                <p class="text-sm text-gray-500">最新价格</p>
                <p class="text-2xl font-bold" :class="priceChangeClass">
                  {{ formatPrice(currentPrice) }}
                </p>
              </div>
              <div>
                <p class="text-sm text-gray-500">24h涨跌</p>
                <p class="text-lg font-medium" :class="priceChangeClass">
                  {{ formatPriceChange(priceChange) }}
                  ({{ formatPercentage(priceChangePercent) }})
                </p>
              </div>
              <div>
                <p class="text-sm text-gray-500">24h量</p>
                <p class="text-lg font-medium">
                  {{ formatVolume(volume24h) }}
                </p>
              </div>
            </div>
          </div>

          <!-- 工具按钮 -->
          <div class="flex items-center space-x-2">
            <el-button @click="toggleFullscreen" size="small">
              <el-icon><FullScreen /></el-icon>
            </el-button>
            <el-button @click="refreshData" size="small" :loading="refreshing">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 主要交易界面 -->
    <div class="trading-main">
      <div class="grid grid-cols-12 gap-4 h-screen">
        <!-- 左侧：K线图和深度图 -->
        <div class="col-span-8">
          <!-- K线图 -->
          <el-card class="h-2/3 mb-4">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-medium">K线图</span>
                <div class="flex items-center space-x-2">
                  <el-radio-group v-model="timeframe" size="small">
                    <el-radio-button 
                      v-for="tf in timeframes" 
                      :key="tf.value"
                      :label="tf.value"
                    >
                      {{ tf.label }}
                    </el-radio-button>
                  </el-radio-group>
                </div>
              </div>
            </template>
            
            <!-- K线图容器 -->
            <div ref="chartContainer" class="w-full h-full min-h-[400px]">
              <!-- 这里应该集成TradingView或其他图表库 -->
              <div class="w-full h-full bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center">
                <div class="text-center">
                  <el-icon :size="48" class="text-gray-400 mb-2"><TrendCharts /></el-icon>
                  <p class="text-gray-500">K线图加载中...</p>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 深度图和最近成交 -->
          <el-card class="h-1/3">
            <el-tabs v-model="bottomTabActive" class="h-full">
              <el-tab-pane label="市场深度" name="depth" class="h-full">
                <div class="depth-chart h-full">
                  <!-- 深度图容器 -->
                  <div class="w-full h-full bg-gray-50 dark:bg-gray-800 rounded flex items-center justify-center">
                    <p class="text-gray-500">深度图</p>
                  </div>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="最近成交" name="trades" class="h-full">
                <div class="recent-trades h-full overflow-y-auto">
                  <div class="trade-header grid grid-cols-3 gap-4 text-xs text-gray-500 font-medium py-2 border-b">
                    <div>价格</div>
                    <div class="text-right">数量</div>
                    <div class="text-right">时间</div>
                  </div>
                  <div class="trade-list">
                    <div 
                      v-for="trade in recentTrades" 
                      :key="trade.id"
                      class="trade-item grid grid-cols-3 gap-4 text-sm py-1 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                      <div :class="trade.side === 'buy' ? 'text-green-600' : 'text-red-600'">
                        {{ formatPrice(trade.price) }}
                      </div>
                      <div class="text-right">{{ formatAmount(trade.amount) }}</div>
                      <div class="text-right text-gray-500">{{ formatTradeTime(trade.time) }}</div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </div>

        <!-- 右侧：订单簿和交易面板 -->
        <div class="col-span-4">
          <!-- 订单簿 -->
          <el-card class="h-1/2 mb-4">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-medium">订单簿</span>
                <el-select v-model="orderbookPrecision" size="small" class="w-20">
                  <el-option 
                    v-for="precision in precisionOptions" 
                    :key="precision"
                    :label="precision"
                    :value="precision"
                  />
                </el-select>
              </div>
            </template>

            <div class="orderbook h-full">
              <!-- 卖单 -->
              <div class="sells mb-2">
                <div class="order-header grid grid-cols-3 gap-2 text-xs text-gray-500 font-medium mb-1">
                  <div>价格</div>
                  <div class="text-right">数量</div>
                  <div class="text-right">累计</div>
                </div>
                <div class="sell-orders space-y-0.5">
                  <div 
                    v-for="order in sellOrders.slice(0, 10)" 
                    :key="`sell-${order.price}`"
                    class="order-row grid grid-cols-3 gap-2 text-xs py-0.5 cursor-pointer hover:bg-red-50 dark:hover:bg-red-900/20"
                    @click="setPrice(order.price)"
                  >
                    <div class="text-red-600 font-medium">{{ formatPrice(order.price) }}</div>
                    <div class="text-right">{{ formatAmount(order.amount) }}</div>
                    <div class="text-right text-gray-500">{{ formatAmount(order.total) }}</div>
                  </div>
                </div>
              </div>

              <!-- 价差 -->
              <div class="spread text-center py-2 border-y border-gray-200 dark:border-gray-600">
                <div class="text-lg font-bold" :class="priceChangeClass">
                  {{ formatPrice(currentPrice) }}
                </div>
                <div class="text-xs text-gray-500">
                  价差: {{ formatPrice(spread) }}
                </div>
              </div>

              <!-- 买单 -->
              <div class="buys mt-2">
                <div class="buy-orders space-y-0.5">
                  <div 
                    v-for="order in buyOrders.slice(0, 10)" 
                    :key="`buy-${order.price}`"
                    class="order-row grid grid-cols-3 gap-2 text-xs py-0.5 cursor-pointer hover:bg-green-50 dark:hover:bg-green-900/20"
                    @click="setPrice(order.price)"
                  >
                    <div class="text-green-600 font-medium">{{ formatPrice(order.price) }}</div>
                    <div class="text-right">{{ formatAmount(order.amount) }}</div>
                    <div class="text-right text-gray-500">{{ formatAmount(order.total) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 交易面板 -->
          <el-card class="h-1/2">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-medium">交易</span>
                <div class="flex items-center space-x-2">
                  <span class="text-sm text-gray-500">可用余额:</span>
                  <span class="text-sm font-medium">
                    {{ formatBalance() }}
                  </span>
                </div>
              </div>
            </template>

            <div class="trading-panel h-full">
              <el-tabs v-model="tradingType" class="h-full">
                <!-- 限价交易 -->
                <el-tab-pane label="限价" name="limit" class="h-full">
                  <div class="limit-trading space-y-4">
                    <!-- 买卖方向 -->
                    <el-radio-group v-model="orderSide" size="large" class="w-full">
                      <el-radio-button label="buy" class="w-1/2 text-center">
                        <span class="text-green-600 font-medium">买入</span>
                      </el-radio-button>
                      <el-radio-button label="sell" class="w-1/2 text-center">
                        <span class="text-red-600 font-medium">卖出</span>
                      </el-radio-button>
                    </el-radio-group>

                    <!-- 价格输入 -->
                    <div>
                      <label class="block text-sm text-gray-700 dark:text-gray-300 mb-1">
                        价格 ({{ quoteCurrency }})
                      </label>
                      <el-input
                        v-model="orderForm.price"
                        placeholder="0.00"
                        type="number"
                        :min="0"
                        step="0.01"
                      >
                        <template #append>{{ quoteCurrency }}</template>
                      </el-input>
                    </div>

                    <!-- 数量输入 -->
                    <div>
                      <label class="block text-sm text-gray-700 dark:text-gray-300 mb-1">
                        数量 ({{ baseCurrency }})
                      </label>
                      <el-input
                        v-model="orderForm.amount"
                        placeholder="0.00"
                        type="number"
                        :min="0"
                        step="0.001"
                      >
                        <template #append>{{ baseCurrency }}</template>
                      </el-input>
                      <!-- 百分比快捷按钮 -->
                      <div class="flex justify-between mt-2">
                        <el-button 
                          v-for="percent in [25, 50, 75, 100]" 
                          :key="percent"
                          size="small" 
                          text
                          @click="setAmountByPercent(percent)"
                        >
                          {{ percent }}%
                        </el-button>
                      </div>
                    </div>

                    <!-- 总金额 -->
                    <div>
                      <label class="block text-sm text-gray-700 dark:text-gray-300 mb-1">
                        总金额 ({{ quoteCurrency }})
                      </label>
                      <el-input
                        :value="totalAmount"
                        placeholder="0.00"
                        readonly
                      >
                        <template #append>{{ quoteCurrency }}</template>
                      </el-input>
                    </div>

                    <!-- 手续费信息 -->
                    <div class="fee-info text-sm text-gray-500">
                      <div class="flex justify-between">
                        <span>手续费 ({{ feeRate }}%):</span>
                        <span>{{ formatAmount(estimatedFee) }} {{ quoteCurrency }}</span>
                      </div>
                    </div>

                    <!-- 提交按钮 -->
                    <el-button 
                      :type="orderSide === 'buy' ? 'success' : 'danger'"
                      size="large"
                      class="w-full"
                      @click="submitOrder"
                      :loading="submittingOrder"
                      :disabled="!canSubmitOrder"
                    >
                      {{ orderSide === 'buy' ? '买入' : '卖出' }} {{ baseCurrency }}
                    </el-button>
                  </div>
                </el-tab-pane>

                <!-- 市价交易 -->
                <el-tab-pane label="市价" name="market" class="h-full">
                  <div class="market-trading space-y-4">
                    <!-- 买卖方向 -->
                    <el-radio-group v-model="orderSide" size="large" class="w-full">
                      <el-radio-button label="buy" class="w-1/2 text-center">
                        <span class="text-green-600 font-medium">买入</span>
                      </el-radio-button>
                      <el-radio-button label="sell" class="w-1/2 text-center">
                        <span class="text-red-600 font-medium">卖出</span>
                      </el-radio-button>
                    </el-radio-group>

                    <!-- 市价买入：输入总金额 -->
                    <div v-if="orderSide === 'buy'">
                      <label class="block text-sm text-gray-700 dark:text-gray-300 mb-1">
                        买入金额 ({{ quoteCurrency }})
                      </label>
                      <el-input
                        v-model="orderForm.quoteAmount"
                        placeholder="0.00"
                        type="number"
                        :min="0"
                      >
                        <template #append>{{ quoteCurrency }}</template>
                      </el-input>
                    </div>

                    <!-- 市价卖出：输入数量 -->
                    <div v-else>
                      <label class="block text-sm text-gray-700 dark:text-gray-300 mb-1">
                        卖出数量 ({{ baseCurrency }})
                      </label>
                      <el-input
                        v-model="orderForm.amount"
                        placeholder="0.00"
                        type="number"
                        :min="0"
                      >
                        <template #append>{{ baseCurrency }}</template>
                      </el-input>
                    </div>

                    <!-- 百分比快捷按钮 -->
                    <div class="flex justify-between">
                      <el-button 
                        v-for="percent in [25, 50, 75, 100]" 
                        :key="percent"
                        size="small" 
                        text
                        @click="setAmountByPercent(percent)"
                      >
                        {{ percent }}%
                      </el-button>
                    </div>

                    <!-- 预计成交信息 -->
                    <div class="estimated-info text-sm space-y-1">
                      <div class="flex justify-between text-gray-600 dark:text-gray-400">
                        <span>预计价格:</span>
                        <span>{{ formatPrice(currentPrice) }}</span>
                      </div>
                      <div class="flex justify-between text-gray-600 dark:text-gray-400">
                        <span>预计手续费:</span>
                        <span>{{ formatAmount(estimatedFee) }} {{ quoteCurrency }}</span>
                      </div>
                    </div>

                    <!-- 提交按钮 -->
                    <el-button 
                      :type="orderSide === 'buy' ? 'success' : 'danger'"
                      size="large"
                      class="w-full"
                      @click="submitMarketOrder"
                      :loading="submittingOrder"
                      :disabled="!canSubmitMarketOrder"
                    >
                      市价{{ orderSide === 'buy' ? '买入' : '卖出' }}
                    </el-button>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 底部：当前订单和历史记录 -->
    <div class="trading-bottom mt-4">
      <el-card>
        <el-tabs v-model="bottomActiveTab">
          <el-tab-pane label="当前委托" name="orders">
            <el-table :data="openOrders" style="width: 100%" v-loading="loadingOrders">
              <el-table-column prop="symbol" label="交易对" width="120" />
              <el-table-column prop="side" label="方向" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.side === 'buy' ? 'success' : 'danger'" size="small">
                    {{ row.side === 'buy' ? '买入' : '卖出' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="80">
                <template #default="{ row }">
                  {{ row.type === 'limit' ? '限价' : '市价' }}
                </template>
              </el-table-column>
              <el-table-column prop="price" label="价格" width="120">
                <template #default="{ row }">
                  {{ row.type === 'limit' ? formatPrice(row.price) : '市价' }}
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="数量" width="120">
                <template #default="{ row }">
                  {{ formatAmount(row.amount) }}
                </template>
              </el-table-column>
              <el-table-column prop="filled" label="已成交" width="120">
                <template #default="{ row }">
                  {{ formatAmount(row.filledAmount) }}
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="时间" width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button 
                    text 
                    type="danger" 
                    size="small"
                    @click="cancelOrder(row.id)"
                  >
                    撤销
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="历史记录" name="history">
            <el-table :data="orderHistory" style="width: 100%" v-loading="loadingHistory">
              <el-table-column prop="symbol" label="交易对" width="120" />
              <el-table-column prop="side" label="方向" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.side === 'buy' ? 'success' : 'danger'" size="small">
                    {{ row.side === 'buy' ? '买入' : '卖出' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="price" label="成交价格" width="120">
                <template #default="{ row }">
                  {{ formatPrice(row.avgPrice) }}
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="成交数量" width="120">
                <template #default="{ row }">
                  {{ formatAmount(row.filledAmount) }}
                </template>
              </el-table-column>
              <el-table-column prop="total" label="成交金额" width="120">
                <template #default="{ row }">
                  {{ formatAmount(row.filledAmount * row.avgPrice) }}
                </template>
              </el-table-column>
              <el-table-column prop="fee" label="手续费" width="100">
                <template #default="{ row }">
                  {{ formatAmount(row.fee) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getOrderStatusType(row.status)" size="small">
                    {{ getOrderStatusName(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="时间" width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  FullScreen,
  Refresh,
  TrendCharts
} from '@element-plus/icons-vue'
import { 
  getTradingPairs,
  getTicker,
  getOrderBook,
  getRecentTrades,
  getWalletBalance,
  createOrder,
  cancelOrder as cancelOrderApi,
  getOpenOrders,
  getOrderHistory
} from '@/api/trading'

// 响应式状态
const refreshing = ref(false)
const submittingOrder = ref(false)
const loadingOrders = ref(false)
const loadingHistory = ref(false)

// 交易对相关
const selectedPair = ref('USDT/CNY')
const tradingPairs = ref([
  { symbol: 'USDT/CNY', name: 'USDT/人民币', baseCurrency: 'USDT', quoteCurrency: 'CNY' },
  { symbol: 'BTC/USDT', name: 'Bitcoin/USDT', baseCurrency: 'BTC', quoteCurrency: 'USDT' },
  { symbol: 'ETH/USDT', name: 'Ethereum/USDT', baseCurrency: 'ETH', quoteCurrency: 'USDT' }
])

// 市场数据
const currentPrice = ref(7.25)
const priceChange = ref(0.05)
const priceChangePercent = ref(0.69)
const volume24h = ref(15623456)
const buyOrders = ref([])
const sellOrders = ref([])
const recentTrades = ref([])

// 界面状态
const timeframe = ref('1m')
const bottomTabActive = ref('depth')
const orderbookPrecision = ref('0.01')
const tradingType = ref('limit')
const orderSide = ref('buy')
const bottomActiveTab = ref('orders')

// 表单数据
const orderForm = reactive({
  price: '',
  amount: '',
  quoteAmount: ''
})

// 账户余额
const balances = ref({
  USDT: 1000,
  CNY: 10000,
  BTC: 0,
  ETH: 0
})

// 订单数据
const openOrders = ref([])
const orderHistory = ref([])

// 配置数据
const timeframes = [
  { label: '1分', value: '1m' },
  { label: '5分', value: '5m' },
  { label: '15分', value: '15m' },
  { label: '1小时', value: '1h' },
  { label: '4小时', value: '4h' },
  { label: '1天', value: '1d' }
]

const precisionOptions = ['0.01', '0.1', '1']
const feeRate = 0.1 // 0.1%

// 计算属性
const currentPair = computed(() => {
  return tradingPairs.value.find(pair => pair.symbol === selectedPair.value)
})

const baseCurrency = computed(() => {
  return currentPair.value?.baseCurrency || 'USDT'
})

const quoteCurrency = computed(() => {
  return currentPair.value?.quoteCurrency || 'CNY'
})

const priceChangeClass = computed(() => {
  return priceChange.value >= 0 ? 'text-green-600' : 'text-red-600'
})

const totalAmount = computed(() => {
  const price = parseFloat(orderForm.price) || 0
  const amount = parseFloat(orderForm.amount) || 0
  return (price * amount).toFixed(2)
})

const estimatedFee = computed(() => {
  const total = parseFloat(totalAmount.value) || 0
  return (total * feeRate / 100).toFixed(2)
})

const spread = computed(() => {
  if (buyOrders.value.length > 0 && sellOrders.value.length > 0) {
    return (sellOrders.value[0].price - buyOrders.value[0].price).toFixed(2)
  }
  return '0.00'
})

const canSubmitOrder = computed(() => {
  return orderForm.price && orderForm.amount && 
         parseFloat(orderForm.price) > 0 && parseFloat(orderForm.amount) > 0
})

const canSubmitMarketOrder = computed(() => {
  if (orderSide.value === 'buy') {
    return orderForm.quoteAmount && parseFloat(orderForm.quoteAmount) > 0
  } else {
    return orderForm.amount && parseFloat(orderForm.amount) > 0
  }
})

// 方法
const handlePairChange = (pair: string) => {
  selectedPair.value = pair
  fetchMarketData()
}

const fetchMarketData = async () => {
  try {
    // 获取市场数据
    const [tickerRes, orderbookRes, tradesRes] = await Promise.all([
      getTicker(selectedPair.value),
      getOrderBook(selectedPair.value),
      getRecentTrades(selectedPair.value)
    ])

    if (tickerRes.data.success) {
      const ticker = tickerRes.data.data
      currentPrice.value = ticker.price
      priceChange.value = ticker.change
      priceChangePercent.value = ticker.changePercent
      volume24h.value = ticker.volume
    }

    if (orderbookRes.data.success) {
      const orderbook = orderbookRes.data.data
      buyOrders.value = orderbook.bids
      sellOrders.value = orderbook.asks
    }

    if (tradesRes.data.success) {
      recentTrades.value = tradesRes.data.data
    }
  } catch (error) {
    console.error('获取市场数据失败:', error)
  }
}

const refreshData = async () => {
  refreshing.value = true
  await fetchMarketData()
  refreshing.value = false
}

const setPrice = (price: number) => {
  orderForm.price = price.toString()
}

const setAmountByPercent = (percent: number) => {
  const availableBalance = orderSide.value === 'buy' 
    ? balances.value[quoteCurrency.value] 
    : balances.value[baseCurrency.value]

  if (orderSide.value === 'buy') {
    if (tradingType.value === 'limit') {
      const price = parseFloat(orderForm.price) || currentPrice.value
      const maxAmount = (availableBalance * percent / 100) / price
      orderForm.amount = maxAmount.toFixed(6)
    } else {
      orderForm.quoteAmount = (availableBalance * percent / 100).toFixed(2)
    }
  } else {
    const maxAmount = availableBalance * percent / 100
    orderForm.amount = maxAmount.toFixed(6)
  }
}

const submitOrder = async () => {
  try {
    submittingOrder.value = true
    
    const orderData = {
      symbol: selectedPair.value,
      side: orderSide.value,
      type: 'limit',
      price: parseFloat(orderForm.price),
      amount: parseFloat(orderForm.amount)
    }

    const response = await createOrder(orderData)
    
    if (response.data.success) {
      ElMessage.success('订单提交成功')
      resetOrderForm()
      fetchOpenOrders()
      fetchBalances()
    } else {
      ElMessage.error(response.data.message || '订单提交失败')
    }
  } catch (error) {
    console.error('提交订单失败:', error)
    ElMessage.error('订单提交失败，请重试')
  } finally {
    submittingOrder.value = false
  }
}

const submitMarketOrder = async () => {
  try {
    submittingOrder.value = true
    
    const orderData = {
      symbol: selectedPair.value,
      side: orderSide.value,
      type: 'market',
      amount: orderSide.value === 'buy' 
        ? parseFloat(orderForm.quoteAmount) / currentPrice.value
        : parseFloat(orderForm.amount)
    }

    const response = await createOrder(orderData)
    
    if (response.data.success) {
      ElMessage.success('市价订单提交成功')
      resetOrderForm()
      fetchOpenOrders()
      fetchBalances()
    } else {
      ElMessage.error(response.data.message || '订单提交失败')
    }
  } catch (error) {
    console.error('提交市价订单失败:', error)
    ElMessage.error('订单提交失败，请重试')
  } finally {
    submittingOrder.value = false
  }
}

const cancelOrder = async (orderId: string) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      '确定要撤销此订单吗？',
      '撤销订单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (!confirmed) return

    const response = await cancelOrderApi(orderId)
    
    if (response.data.success) {
      ElMessage.success('订单已撤销')
      fetchOpenOrders()
    } else {
      ElMessage.error(response.data.message || '撤销失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤销订单失败:', error)
      ElMessage.error('撤销失败，请重试')
    }
  }
}

const fetchOpenOrders = async () => {
  try {
    loadingOrders.value = true
    const response = await getOpenOrders()
    
    if (response.data.success) {
      openOrders.value = response.data.data
    }
  } catch (error) {
    console.error('获取当前订单失败:', error)
  } finally {
    loadingOrders.value = false
  }
}

const fetchOrderHistory = async () => {
  try {
    loadingHistory.value = true
    const response = await getOrderHistory()
    
    if (response.data.success) {
      orderHistory.value = response.data.data
    }
  } catch (error) {
    console.error('获取历史订单失败:', error)
  } finally {
    loadingHistory.value = false
  }
}

const fetchBalances = async () => {
  try {
    const response = await getWalletBalance()
    
    if (response.data.success) {
      // 更新余额数据
      const newBalances = response.data.data
      Object.assign(balances.value, newBalances)
    }
  } catch (error) {
    console.error('获取余额失败:', error)
  }
}

const resetOrderForm = () => {
  orderForm.price = ''
  orderForm.amount = ''
  orderForm.quoteAmount = ''
}

const toggleFullscreen = () => {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    document.documentElement.requestFullscreen()
  }
}

const formatPrice = (price: number) => {
  return price.toFixed(2)
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString(undefined, { maximumFractionDigits: 6 })
}

const formatVolume = (volume: number) => {
  if (volume >= 1000000) {
    return (volume / 1000000).toFixed(2) + 'M'
  } else if (volume >= 1000) {
    return (volume / 1000).toFixed(2) + 'K'
  }
  return volume.toFixed(2)
}

const formatPriceChange = (change: number) => {
  return (change >= 0 ? '+' : '') + change.toFixed(2)
}

const formatPercentage = (percent: number) => {
  return (percent >= 0 ? '+' : '') + percent.toFixed(2) + '%'
}

const formatBalance = () => {
  const currency = orderSide.value === 'buy' ? quoteCurrency.value : baseCurrency.value
  const balance = balances.value[currency] || 0
  return `${formatAmount(balance)} ${currency}`
}

const formatDateTime = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatTradeTime = (time: string) => {
  return new Date(time).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const getOrderStatusName = (status: string) => {
  const map = {
    filled: '已成交',
    cancelled: '已撤销',
    partial: '部分成交',
    pending: '待成交'
  }
  return map[status as keyof typeof map] || status
}

const getOrderStatusType = (status: string) => {
  const map = {
    filled: 'success',
    cancelled: 'info',
    partial: 'warning',
    pending: 'primary'
  }
  return map[status as keyof typeof map] || 'info'
}

// WebSocket连接
let wsConnection: WebSocket | null = null

const connectWebSocket = () => {
  // 实际项目中应该连接到真实的WebSocket服务
  // wsConnection = new WebSocket('wss://api.example.com/ws')
  
  // 模拟实时数据更新
  const interval = setInterval(() => {
    // 模拟价格变化
    const change = (Math.random() - 0.5) * 0.1
    currentPrice.value = Math.max(0.01, currentPrice.value + change)
    priceChange.value += change
    priceChangePercent.value = (priceChange.value / (currentPrice.value - priceChange.value)) * 100
    
    // 模拟订单簿更新
    if (Math.random() > 0.7) {
      fetchMarketData()
    }
  }, 3000)

  return () => clearInterval(interval)
}

// 生命周期
let cleanupInterval: (() => void) | null = null

onMounted(async () => {
  await fetchMarketData()
  await fetchOpenOrders()
  await fetchOrderHistory()
  await fetchBalances()
  cleanupInterval = connectWebSocket()
})

onUnmounted(() => {
  if (wsConnection) {
    wsConnection.close()
  }
  if (cleanupInterval) {
    cleanupInterval()
  }
})

// 监听交易对变化
watch(selectedPair, () => {
  resetOrderForm()
})
</script>

<style scoped>
.spot-trading-view {
  @apply min-h-screen bg-gray-50 dark:bg-gray-900;
}

.trading-header {
  @apply sticky top-0 z-10 bg-gray-50 dark:bg-gray-900 pb-4;
}

.trading-main {
  @apply px-4;
}

.trading-bottom {
  @apply px-4;
}

.orderbook {
  @apply text-xs;
}

.order-row {
  @apply relative;
}

.order-row::before {
  content: '';
  @apply absolute left-0 top-0 bottom-0 bg-current opacity-10;
  width: var(--depth-width, 0%);
}

.trading-panel {
  @apply overflow-y-auto;
}

/* 隐藏滚动条 */
.trading-panel::-webkit-scrollbar {
  display: none;
}

.trading-panel {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .trading-main .grid {
    @apply grid-cols-1 gap-4;
  }
  
  .trading-main .col-span-8,
  .trading-main .col-span-4 {
    @apply col-span-1;
  }
}

@media (max-width: 768px) {
  .spot-trading-view {
    @apply px-2;
  }
  
  .trading-header .flex {
    @apply flex-col space-y-4;
  }
  
  .trading-main {
    @apply px-2;
  }
  
  .trading-bottom {
    @apply px-2;
  }
}

/* 暗黑模式样式 */
.dark .trading-header {
  @apply bg-gray-900;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-tabs__content) {
  @apply p-0;
}

:deep(.el-tab-pane) {
  @apply h-full;
}

:deep(.el-card__body) {
  @apply p-4;
}

:deep(.el-radio-button__inner) {
  @apply border-0;
}

:deep(.el-radio-button:first-child .el-radio-button__inner) {
  @apply rounded-l-md;
}

:deep(.el-radio-button:last-child .el-radio-button__inner) {
  @apply rounded-r-md;
}
</style>