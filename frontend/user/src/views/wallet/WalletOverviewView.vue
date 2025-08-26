<template>
  <div class="wallet-overview">
    <!-- 錢包餘額卡片 -->
    <el-row :gutter="24" class="balance-cards">
      <el-col :span="8">
        <el-card class="balance-card total-balance" shadow="hover">
          <div class="balance-content">
            <div class="balance-icon">
              <el-icon><Wallet /></el-icon>
            </div>
            <div class="balance-info">
              <div class="balance-label">總餘額</div>
              <div class="balance-value">
                ${{ formatNumber(totalBalance) }}
                <span class="balance-currency">USD</span>
              </div>
              <div class="balance-change">
                <el-icon :class="balanceChange >= 0 ? 'positive' : 'negative'">
                  <ArrowUp v-if="balanceChange >= 0" />
                  <ArrowDown v-else />
                </el-icon>
                <span :class="balanceChange >= 0 ? 'positive' : 'negative'">
                  {{ balanceChange >= 0 ? '+' : '' }}{{ formatNumber(balanceChange) }}
                </span>
                <span class="change-period">24h</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="balance-card available-balance" shadow="hover">
          <div class="balance-content">
            <div class="balance-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="balance-info">
              <div class="balance-label">可用餘額</div>
              <div class="balance-value">
                ${{ formatNumber(availableBalance) }}
              </div>
              <div class="balance-desc">可用於交易和提現</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="balance-card frozen-balance" shadow="hover">
          <div class="balance-content">
            <div class="balance-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="balance-info">
              <div class="balance-label">凍結餘額</div>
              <div class="balance-value">
                ${{ formatNumber(frozenBalance) }}
              </div>
              <div class="balance-desc">訂單中或待處理</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24">
      <!-- 錢包資產分佈 -->
      <el-col :span="12">
        <el-card class="asset-distribution" shadow="never">
          <template #header>
            <h3>資產分佈</h3>
          </template>

          <div class="asset-list">
            <div 
              v-for="asset in assetDistribution" 
              :key="asset.currency"
              class="asset-item"
            >
              <div class="asset-info">
                <div class="asset-icon">
                  <img :src="asset.icon" :alt="asset.currency" />
                </div>
                <div class="asset-details">
                  <div class="asset-name">
                    {{ asset.name }}
                    <span class="asset-symbol">{{ asset.currency }}</span>
                  </div>
                  <div class="asset-balance">
                    {{ formatNumber(asset.balance) }}
                    <span class="asset-value">
                      ≈ ${{ formatNumber(asset.usdValue) }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="asset-percentage">
                {{ (asset.percentage).toFixed(1) }}%
              </div>
            </div>
          </div>

          <div class="chart-container">
            <v-chart 
              v-if="chartOption"
              :option="chartOption"
              style="width: 100%; height: 200px;"
              autoresize
            />
          </div>
        </el-card>
      </el-col>

      <!-- 最近交易 -->
      <el-col :span="12">
        <el-card class="recent-transactions" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>最近交易</h3>
              <el-button text @click="goToTransactions">
                查看全部
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>

          <div class="transaction-list">
            <div 
              v-for="transaction in recentTransactions" 
              :key="transaction.id"
              class="transaction-item"
            >
              <div class="transaction-icon">
                <el-icon :class="getTransactionIconClass(transaction.type)">
                  <component :is="getTransactionIcon(transaction.type)" />
                </el-icon>
              </div>
              <div class="transaction-info">
                <div class="transaction-type">
                  {{ getTransactionTypeText(transaction.type) }}
                </div>
                <div class="transaction-time">
                  {{ dayjs(transaction.created_at).format('MM-DD HH:mm') }}
                </div>
              </div>
              <div class="transaction-amount">
                <div 
                  class="amount-value"
                  :class="transaction.type === 'DEPOSIT' ? 'positive' : 'negative'"
                >
                  {{ transaction.type === 'DEPOSIT' ? '+' : '-' }}${{ formatNumber(transaction.amount) }}
                </div>
                <div class="amount-status">
                  <el-tag 
                    :type="getStatusType(transaction.status)" 
                    size="small"
                  >
                    {{ getStatusText(transaction.status) }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div v-if="recentTransactions.length === 0" class="empty-transactions">
              <el-empty description="暂无交易记录" :image-size="80" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-card class="quick-actions" shadow="never">
      <template #header>
        <h3>快速操作</h3>
      </template>

      <div class="action-buttons">
        <el-button 
          type="primary" 
          size="large"
          @click="goToDeposit"
          class="action-btn deposit-btn"
        >
          <el-icon><Plus /></el-icon>
          <span>充值</span>
        </el-button>

        <el-button 
          size="large"
          @click="goToWithdraw"
          class="action-btn withdraw-btn"
        >
          <el-icon><Minus /></el-icon>
          <span>提現</span>
        </el-button>

        <el-button 
          type="success" 
          size="large"
          @click="goToTrading"
          class="action-btn trading-btn"
        >
          <el-icon><TrendCharts /></el-icon>
          <span>交易</span>
        </el-button>

        <el-button 
          size="large"
          @click="goToTransfer"
          class="action-btn transfer-btn"
        >
          <el-icon><Switch /></el-icon>
          <span>轉帳</span>
        </el-button>
      </div>
    </el-card>

    <!-- 安全提示 -->
    <el-alert
      v-if="showSecurityTip"
      title="安全提示"
      type="warning"
      :closable="false"
      class="security-tip"
    >
      <template #default>
        <div class="tip-content">
          <p>為了保護您的資產安全，建議您：</p>
          <ul>
            <li v-if="!user?.email_verified">
              <router-link to="/profile/security">完成郵箱驗證</router-link>
            </li>
            <li v-if="!user?.phone_verified">
              <router-link to="/profile/security">完成手機驗證</router-link>
            </li>
            <li v-if="!user?.mfa_enabled">
              <router-link to="/profile/security">啟用雙因子認證</router-link>
            </li>
            <li v-if="kycStatus !== 'approved'">
              <router-link to="/kyc/status">完成KYC身份驗證</router-link>
            </li>
          </ul>
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { WalletApi } from '@/api/wallet'
import { ElMessage } from 'element-plus'
import { dayjs } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'

// 註冊 ECharts 組件
use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent])

import {
  Wallet,
  Money,
  Lock,
  ArrowUp,
  ArrowDown,
  ArrowRight,
  Plus,
  Minus,
  TrendCharts,
  Switch,
  Upload,
  Download,
  RefreshRight
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 響應式數據
const totalBalance = ref(0)
const availableBalance = ref(0)
const frozenBalance = ref(0)
const balanceChange = ref(0)
const assetDistribution = ref<any[]>([])
const recentTransactions = ref<any[]>([])
const kycStatus = ref('pending')
const loading = ref(false)
const chartOption = ref(null)

// 計算屬性
const user = computed(() => authStore.user)
const showSecurityTip = computed(() => {
  return !user.value?.email_verified || 
         !user.value?.phone_verified || 
         !user.value?.mfa_enabled || 
         kycStatus.value !== 'approved'
})

// 方法
const formatNumber = (num: number) => {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num)
}

const getTransactionIcon = (type: string) => {
  switch (type) {
    case 'DEPOSIT':
      return Upload
    case 'WITHDRAWAL':
      return Download
    case 'TRANSFER_IN':
      return RefreshRight
    case 'TRANSFER_OUT':
      return RefreshRight
    default:
      return Money
  }
}

const getTransactionIconClass = (type: string) => {
  switch (type) {
    case 'DEPOSIT':
    case 'TRANSFER_IN':
      return 'positive-icon'
    case 'WITHDRAWAL':
    case 'TRANSFER_OUT':
      return 'negative-icon'
    default:
      return 'neutral-icon'
  }
}

const getTransactionTypeText = (type: string) => {
  switch (type) {
    case 'DEPOSIT':
      return '充值'
    case 'WITHDRAWAL':
      return '提現'
    case 'TRANSFER_IN':
      return '轉入'
    case 'TRANSFER_OUT':
      return '轉出'
    case 'TRADING_BUY':
      return '買入'
    case 'TRADING_SELL':
      return '賣出'
    default:
      return '交易'
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'PENDING':
      return '處理中'
    case 'FAILED':
      return '失敗'
    case 'CANCELLED':
      return '已取消'
    default:
      return '未知'
  }
}

const goToDeposit = () => {
  router.push('/wallet/deposit')
}

const goToWithdraw = () => {
  router.push('/wallet/withdraw')
}

const goToTrading = () => {
  router.push('/trading/usdt')
}

const goToTransfer = () => {
  router.push('/wallet/transfer')
}

const goToTransactions = () => {
  router.push('/wallet/transactions')
}

const loadWalletData = async () => {
  try {
    loading.value = true

    // 載入錢包餘額
    const balanceResponse = await WalletApi.getBalance()
    totalBalance.value = balanceResponse.total_balance
    availableBalance.value = balanceResponse.available_balance
    frozenBalance.value = balanceResponse.frozen_balance
    balanceChange.value = balanceResponse.daily_change

    // 載入資產分佈
    const assetsResponse = await WalletApi.getAssetDistribution()
    assetDistribution.value = assetsResponse

    // 載入最近交易
    const transactionsResponse = await WalletApi.getRecentTransactions(5)
    recentTransactions.value = transactionsResponse.data

    // 載入KYC狀態
    // const kycResponse = await KycApi.getKycStatus()
    // kycStatus.value = kycResponse.status

  } catch (error: any) {
    ElMessage.error(error.message || '載入錢包數據失敗')
  } finally {
    loading.value = false
  }
}

const initAssetChart = () => {
  if (assetDistribution.value.length === 0) return
    
  chartOption.value = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: ${c} ({d}%)'
    },
    series: [
      {
        name: '資產分佈',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: assetDistribution.value.map(asset => ({
          value: asset.usdValue,
          name: asset.currency,
          itemStyle: {
            color: asset.color || '#409EFF'
          }
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          show: false
        },
        labelLine: {
          show: false
        }
      }
    ]
  }
}

// 模擬數據（實際使用時應該從API獲取）
const loadMockData = () => {
  totalBalance.value = 12450.68
  availableBalance.value = 11230.45
  frozenBalance.value = 1220.23
  balanceChange.value = 156.78

  assetDistribution.value = [
    {
      currency: 'USDT',
      name: 'Tether',
      balance: 10000.00,
      usdValue: 10000.00,
      percentage: 80.3,
      icon: '/icons/usdt.png',
      color: '#26A17B'
    },
    {
      currency: 'USD',
      name: 'US Dollar',
      balance: 2450.68,
      usdValue: 2450.68,
      percentage: 19.7,
      icon: '/icons/usd.png',
      color: '#1890FF'
    }
  ]

  recentTransactions.value = [
    {
      id: '1',
      type: 'DEPOSIT',
      amount: 1000.00,
      status: 'COMPLETED',
      created_at: '2024-01-15T10:30:00Z'
    },
    {
      id: '2',
      type: 'TRADING_BUY',
      amount: 500.00,
      status: 'COMPLETED',
      created_at: '2024-01-14T15:20:00Z'
    },
    {
      id: '3',
      type: 'WITHDRAWAL',
      amount: 200.00,
      status: 'PENDING',
      created_at: '2024-01-14T09:15:00Z'
    },
    {
      id: '4',
      type: 'TRADING_SELL',
      amount: 300.00,
      status: 'COMPLETED',
      created_at: '2024-01-13T16:45:00Z'
    }
  ]
}

// 生命週期
onMounted(() => {
  // loadWalletData()
  loadMockData()
  initAssetChart()
})
</script>

<style scoped lang="scss">
.wallet-overview {
  max-width: 1200px;
  margin: 0 auto;

  .el-card {
    margin-bottom: 24px;
  }
}

.balance-cards {
  margin-bottom: 24px;

  .balance-card {
    height: 160px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-4px);
    }

    &.total-balance {
      border-left: 4px solid var(--el-color-primary);
    }

    &.available-balance {
      border-left: 4px solid var(--el-color-success);
    }

    &.frozen-balance {
      border-left: 4px solid var(--el-color-warning);
    }

    .balance-content {
      display: flex;
      align-items: center;
      height: 100%;
      gap: 16px;

      .balance-icon {
        width: 64px;
        height: 64px;
        border-radius: 12px;
        background: var(--el-fill-color-lighter);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        color: var(--el-color-primary);
      }

      .balance-info {
        flex: 1;

        .balance-label {
          font-size: 14px;
          color: var(--el-text-color-regular);
          margin-bottom: 8px;
        }

        .balance-value {
          font-size: 28px;
          font-weight: 600;
          color: var(--el-text-color-primary);
          margin-bottom: 8px;

          .balance-currency {
            font-size: 16px;
            font-weight: 400;
            color: var(--el-text-color-secondary);
            margin-left: 8px;
          }
        }

        .balance-change {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 14px;

          .positive {
            color: var(--el-color-success);
          }

          .negative {
            color: var(--el-color-danger);
          }

          .change-period {
            color: var(--el-text-color-secondary);
          }
        }

        .balance-desc {
          font-size: 12px;
          color: var(--el-text-color-secondary);
          margin-top: 4px;
        }
      }
    }
  }
}

.asset-distribution {
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .asset-list {
    margin-bottom: 24px;

    .asset-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 0;
      border-bottom: 1px solid var(--el-border-color-lighter);

      &:last-child {
        border-bottom: none;
      }

      .asset-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .asset-icon {
          width: 40px;
          height: 40px;

          img {
            width: 100%;
            height: 100%;
            border-radius: 50%;
          }
        }

        .asset-details {
          .asset-name {
            font-size: 14px;
            font-weight: 500;
            color: var(--el-text-color-primary);
            margin-bottom: 4px;

            .asset-symbol {
              color: var(--el-text-color-secondary);
              margin-left: 8px;
            }
          }

          .asset-balance {
            font-size: 13px;
            color: var(--el-text-color-regular);

            .asset-value {
              color: var(--el-text-color-secondary);
              margin-left: 8px;
            }
          }
        }
      }

      .asset-percentage {
        font-size: 16px;
        font-weight: 500;
        color: var(--el-color-primary);
      }
    }
  }
}

.recent-transactions {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .el-button {
      padding: 0;
    }
  }

  .transaction-list {
    .transaction-item {
      display: flex;
      align-items: center;
      padding: 16px 0;
      border-bottom: 1px solid var(--el-border-color-lighter);

      &:last-child {
        border-bottom: none;
      }

      .transaction-icon {
        width: 40px;
        height: 40px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;

        &.positive-icon {
          background: var(--el-color-success-light-9);
          color: var(--el-color-success);
        }

        &.negative-icon {
          background: var(--el-color-danger-light-9);
          color: var(--el-color-danger);
        }

        &.neutral-icon {
          background: var(--el-fill-color-light);
          color: var(--el-text-color-secondary);
        }
      }

      .transaction-info {
        flex: 1;

        .transaction-type {
          font-size: 14px;
          font-weight: 500;
          color: var(--el-text-color-primary);
          margin-bottom: 4px;
        }

        .transaction-time {
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }
      }

      .transaction-amount {
        text-align: right;

        .amount-value {
          font-size: 14px;
          font-weight: 500;
          margin-bottom: 4px;

          &.positive {
            color: var(--el-color-success);
          }

          &.negative {
            color: var(--el-color-danger);
          }
        }
      }
    }

    .empty-transactions {
      padding: 40px 0;
      text-align: center;
    }
  }
}

.quick-actions {
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .action-buttons {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;

    .action-btn {
      flex: 1;
      min-width: 120px;
      height: 80px;
      flex-direction: column;
      gap: 8px;
      font-size: 16px;
      border-radius: 12px;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }

      .el-icon {
        font-size: 24px;
      }

      &.deposit-btn {
        background: linear-gradient(135deg, var(--el-color-primary), #409EFF);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #337ECC, var(--el-color-primary));
        }
      }

      &.trading-btn {
        background: linear-gradient(135deg, var(--el-color-success), #67C23A);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #529B2E, var(--el-color-success));
        }
      }
    }
  }
}

.security-tip {
  .tip-content {
    p {
      margin: 0 0 8px 0;
    }

    ul {
      margin: 0;
      padding-left: 20px;

      li {
        margin-bottom: 4px;

        a {
          color: var(--el-color-primary);
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .balance-cards {
    .el-col {
      margin-bottom: 16px;
    }
  }

  .action-buttons {
    flex-direction: column;

    .action-btn {
      min-width: auto;
    }
  }
}
</style>