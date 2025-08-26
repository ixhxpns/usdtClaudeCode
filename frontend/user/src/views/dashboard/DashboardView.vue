<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <header class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo and Title -->
          <div class="flex items-center">
            <div class="h-8 w-8 bg-blue-600 rounded-lg flex items-center justify-center mr-3">
              <el-icon :size="16" class="text-white">
                <CreditCard />
              </el-icon>
            </div>
            <h1 class="text-xl font-semibold text-gray-900 dark:text-white">
              USDT交易平台
            </h1>
          </div>

          <!-- User Menu -->
          <div class="flex items-center space-x-4">
            <el-button 
              type="primary" 
              :icon="Plus" 
              size="small"
            >
              快速交易
            </el-button>
            
            <el-dropdown @command="handleUserMenu">
              <div class="flex items-center space-x-2 cursor-pointer">
                <el-avatar :size="32" :icon="UserFilled" />
                <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {{ userDisplayName }}
                </span>
                <el-icon class="text-gray-400">
                  <ArrowDown />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile" :icon="User">
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="settings" :icon="Setting">
                    设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout" :icon="SwitchButton">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Welcome Section -->
      <div class="mb-8">
        <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">
          欢迎回来，{{ userDisplayName }}
        </h2>
        <p class="text-gray-600 dark:text-gray-400">
          今天是 {{ formatDate(new Date()) }}，祝您交易愉快！
        </p>
      </div>

      <!-- Quick Stats -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-2 bg-green-100 dark:bg-green-900 rounded-lg">
              <el-icon :size="20" class="text-green-600 dark:text-green-400">
                <Wallet />
              </el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                USDT余额
              </p>
              <p class="text-lg font-semibold text-gray-900 dark:text-white">
                0.000000
              </p>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-2 bg-blue-100 dark:bg-blue-900 rounded-lg">
              <el-icon :size="20" class="text-blue-600 dark:text-blue-400">
                <TrendCharts />
              </el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                今日交易
              </p>
              <p class="text-lg font-semibold text-gray-900 dark:text-white">
                0
              </p>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-2 bg-purple-100 dark:bg-purple-900 rounded-lg">
              <el-icon :size="20" class="text-purple-600 dark:text-purple-400">
                <Document />
              </el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                KYC状态
              </p>
              <p class="text-lg font-semibold text-gray-900 dark:text-white">
                待提交
              </p>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div class="flex items-center">
            <div class="p-2 bg-orange-100 dark:bg-orange-900 rounded-lg">
              <el-icon :size="20" class="text-orange-600 dark:text-orange-400">
                <Bell />
              </el-icon>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                未读消息
              </p>
              <p class="text-lg font-semibold text-gray-900 dark:text-white">
                0
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Cards -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <!-- Quick Actions -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
            快速操作
          </h3>
          <div class="space-y-3">
            <el-button
              type="primary"
              class="w-full justify-start"
              :icon="Plus"
              @click="handleQuickAction('deposit')"
            >
              充值USDT
            </el-button>
            <el-button
              class="w-full justify-start"
              :icon="Minus"
              @click="handleQuickAction('withdraw')"
            >
              提现USDT
            </el-button>
            <el-button
              class="w-full justify-start"
              :icon="TrendCharts"
              @click="handleQuickAction('trade')"
            >
              开始交易
            </el-button>
            <el-button
              class="w-full justify-start"
              :icon="Document"
              @click="handleQuickAction('kyc')"
            >
              完成KYC认证
            </el-button>
          </div>
        </div>

        <!-- Account Status -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
            账户状态
          </h3>
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600 dark:text-gray-400">邮箱验证</span>
              <el-tag :type="authStore.isEmailVerified ? 'success' : 'warning'" size="small">
                {{ authStore.isEmailVerified ? '已验证' : '待验证' }}
              </el-tag>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600 dark:text-gray-400">手机验证</span>
              <el-tag :type="authStore.isPhoneVerified ? 'success' : 'warning'" size="small">
                {{ authStore.isPhoneVerified ? '已验证' : '待验证' }}
              </el-tag>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600 dark:text-gray-400">双因子认证</span>
              <el-tag :type="authStore.isMfaEnabled ? 'success' : 'info'" size="small">
                {{ authStore.isMfaEnabled ? '已启用' : '未启用' }}
              </el-tag>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600 dark:text-gray-400">KYC认证</span>
              <el-tag type="warning" size="small">
                待提交
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent Activity -->
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-medium text-gray-900 dark:text-white">
            最近活动
          </h3>
        </div>
        <div class="p-6">
          <div class="text-center py-8">
            <el-icon :size="48" class="text-gray-400 mb-4">
              <Document />
            </el-icon>
            <p class="text-gray-500 dark:text-gray-400 mb-4">
              暂无活动记录
            </p>
            <el-button type="primary" @click="handleQuickAction('trade')">
              开始您的第一笔交易
            </el-button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Minus,
  User,
  UserFilled,
  Setting,
  SwitchButton,
  ArrowDown,
  CreditCard,
  Wallet,
  TrendCharts,
  Document,
  Bell
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { DateFormatter } from '@/utils/common'

// Hooks
const router = useRouter()
const authStore = useAuthStore()

// Computed
const userDisplayName = computed(() => {
  const user = authStore.user
  if (!user) return '用户'
  
  // 优先显示昵称，其次邮箱前缀
  if (user.email) {
    return user.email.split('@')[0]
  }
  
  return '用户'
})

// Methods
const handleUserMenu = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/profile/security')
      break
    case 'logout':
      await authStore.logout()
      router.push('/login')
      break
  }
}

const handleQuickAction = (action: string) => {
  switch (action) {
    case 'deposit':
      router.push('/wallet/deposit')
      break
    case 'withdraw':
      if (!authStore.isEmailVerified) {
        ElMessage.warning('请先完成邮箱验证')
        router.push('/profile/security')
        return
      }
      router.push('/wallet/withdraw')
      break
    case 'trade':
      if (!authStore.isEmailVerified) {
        ElMessage.warning('请先完成邮箱验证')
        router.push('/profile/security')
        return
      }
      router.push('/trading/spot')
      break
    case 'kyc':
      router.push('/kyc/status')
      break
  }
}

const formatDate = (date: Date) => {
  return DateFormatter.format(date, 'YYYY年MM月DD日 dddd')
}
</script>

<style scoped>
/* 自定义样式 */
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