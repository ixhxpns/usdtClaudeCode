<template>
  <div class="p-6">
    <!-- Page Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        控制面板
      </h1>
      <p class="mt-1 text-gray-600 dark:text-gray-400">
        欢迎回来，{{ adminDisplayName }}
      </p>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- Total Users -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
              总用户数
            </p>
            <p class="text-2xl font-bold text-gray-900 dark:text-white">
              1,234
            </p>
            <div class="flex items-center mt-2">
              <el-icon class="text-green-500 mr-1" :size="16">
                <TrendCharts />
              </el-icon>
              <span class="text-sm text-green-500">+12.3%</span>
            </div>
          </div>
          <div class="p-3 bg-blue-100 dark:bg-blue-900 rounded-lg">
            <el-icon :size="24" class="text-blue-600 dark:text-blue-400">
              <User />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- Active Orders -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
              活跃订单
            </p>
            <p class="text-2xl font-bold text-gray-900 dark:text-white">
              89
            </p>
            <div class="flex items-center mt-2">
              <el-icon class="text-green-500 mr-1" :size="16">
                <TrendCharts />
              </el-icon>
              <span class="text-sm text-green-500">+5.1%</span>
            </div>
          </div>
          <div class="p-3 bg-green-100 dark:bg-green-900 rounded-lg">
            <el-icon :size="24" class="text-green-600 dark:text-green-400">
              <List />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- Pending Withdrawals -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
              待处理提现
            </p>
            <p class="text-2xl font-bold text-gray-900 dark:text-white">
              23
            </p>
            <div class="flex items-center mt-2">
              <el-icon class="text-orange-500 mr-1" :size="16">
                <Warning />
              </el-icon>
              <span class="text-sm text-orange-500">需要处理</span>
            </div>
          </div>
          <div class="p-3 bg-orange-100 dark:bg-orange-900 rounded-lg">
            <el-icon :size="24" class="text-orange-600 dark:text-orange-400">
              <Money />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- KYC Reviews -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
              待审核KYC
            </p>
            <p class="text-2xl font-bold text-gray-900 dark:text-white">
              15
            </p>
            <div class="flex items-center mt-2">
              <el-icon class="text-red-500 mr-1" :size="16">
                <Clock />
              </el-icon>
              <span class="text-sm text-red-500">等待审核</span>
            </div>
          </div>
          <div class="p-3 bg-red-100 dark:bg-red-900 rounded-lg">
            <el-icon :size="24" class="text-red-600 dark:text-red-400">
              <CreditCard />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions and Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
      <!-- Quick Actions -->
      <div class="lg:col-span-1">
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            快速操作
          </h3>
          <div class="space-y-3">
            <el-button
              type="primary"
              class="w-full justify-start"
              :icon="User"
              @click="quickAction('users')"
            >
              用户管理
            </el-button>
            <el-button
              type="warning"
              class="w-full justify-start"
              :icon="CreditCard"
              @click="quickAction('kyc')"
            >
              KYC审核
            </el-button>
            <el-button
              type="success"
              class="w-full justify-start"
              :icon="Money"
              @click="quickAction('withdrawals')"
            >
              提现处理
            </el-button>
            <el-button
              type="info"
              class="w-full justify-start"
              :icon="Setting"
              @click="quickAction('system')"
            >
              系统设置
            </el-button>
          </div>
        </div>
      </div>

      <!-- Charts Placeholder -->
      <div class="lg:col-span-2">
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            数据统计
          </h3>
          <div class="h-64 flex items-center justify-center text-gray-400">
            <div class="text-center">
              <el-icon :size="48" class="mb-4">
                <TrendCharts />
              </el-icon>
              <p>数据图表开发中...</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Recent Users -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            最新注册用户
          </h3>
        </div>
        <div class="p-6">
          <div class="space-y-4">
            <div v-for="i in 5" :key="i" class="flex items-center justify-between">
              <div class="flex items-center">
                <el-avatar :size="32" :icon="User" class="mr-3" />
                <div>
                  <p class="text-sm font-medium text-gray-900 dark:text-white">
                    user{{ i }}@example.com
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    {{ formatTime(new Date()) }}
                  </p>
                </div>
              </div>
              <el-tag size="small" type="success">已验证</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- Pending Tasks -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            待处理任务
          </h3>
        </div>
        <div class="p-6">
          <div class="space-y-4">
            <div class="flex items-center justify-between p-3 bg-orange-50 dark:bg-orange-900/20 rounded-lg">
              <div class="flex items-center">
                <el-icon class="text-orange-500 mr-3" :size="20">
                  <Money />
                </el-icon>
                <div>
                  <p class="text-sm font-medium text-gray-900 dark:text-white">
                    23个提现申请待处理
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    需要及时处理
                  </p>
                </div>
              </div>
              <el-button size="small" type="warning" @click="quickAction('withdrawals')">
                处理
              </el-button>
            </div>

            <div class="flex items-center justify-between p-3 bg-red-50 dark:bg-red-900/20 rounded-lg">
              <div class="flex items-center">
                <el-icon class="text-red-500 mr-3" :size="20">
                  <CreditCard />
                </el-icon>
                <div>
                  <p class="text-sm font-medium text-gray-900 dark:text-white">
                    15个KYC申请待审核
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    需要审核
                  </p>
                </div>
              </div>
              <el-button size="small" type="danger" @click="quickAction('kyc')">
                审核
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  User,
  List,
  Money,
  CreditCard,
  Setting,
  TrendCharts,
  Warning,
  Clock
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { DateFormatter } from '@/utils/common'

// Hooks
const router = useRouter()
const authStore = useAuthStore()

// Computed
const adminDisplayName = computed(() => {
  const admin = authStore.admin
  if (!admin) return '管理员'
  
  return admin.username || '管理员'
})

// Methods
const quickAction = (action: string) => {
  switch (action) {
    case 'users':
      router.push('/users/list')
      break
    case 'kyc':
      router.push('/kyc/pending')
      break
    case 'withdrawals':
      router.push('/withdrawals/pending')
      break
    case 'system':
      router.push('/system/config')
      break
  }
}

const formatTime = (date: Date) => {
  return DateFormatter.format(date, 'MM-DD HH:mm')
}
</script>

<style scoped>
.el-button.justify-start {
  justify-content: flex-start;
  text-align: left;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .grid-cols-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .grid-cols-4 {
    grid-template-columns: repeat(1, 1fr);
  }
}
</style>