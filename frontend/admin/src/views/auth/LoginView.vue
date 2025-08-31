<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-700 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <!-- Logo and Title -->
      <div class="text-center">
        <div class="mx-auto h-16 w-16 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg">
          <el-icon :size="32" class="text-white">
            <Monitor />
          </el-icon>
        </div>
        <h2 class="mt-6 text-3xl font-extrabold text-white">
          管理后台登录
        </h2>
        <p class="mt-2 text-sm text-slate-300">
          USDT交易平台 - 管理控制面板
        </p>
      </div>

      <!-- Login Form -->
      <div class="bg-white rounded-xl shadow-2xl border border-slate-200">
        <div class="p-8">
          <el-form 
            ref="loginFormRef"
            :model="loginForm" 
            :rules="loginRules"
            class="space-y-6"
            @submit.prevent="handleLogin"
          >
            <!-- Username Field -->
            <el-form-item prop="username">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                管理员账号
              </label>
              <el-input
                v-model="loginForm.username"
                size="large"
                placeholder="请输入管理员账号"
                :prefix-icon="User"
                :disabled="authStore.isLoading"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon class="text-gray-400">
                    <User />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- Password Field -->
            <el-form-item prop="password">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                登录密码
              </label>
              <el-input
                v-model="loginForm.password"
                type="password"
                size="large"
                placeholder="请输入登录密码"
                show-password
                :disabled="authStore.isLoading"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon class="text-gray-400">
                    <Lock />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- MFA Code Field (if needed) -->
            <el-form-item v-if="showMfaField" prop="mfa_code">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                双因子认证码
              </label>
              <el-input
                v-model="loginForm.mfa_code"
                size="large"
                placeholder="请输入6位验证码"
                maxlength="6"
                :disabled="authStore.isLoading"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon class="text-gray-400">
                    <Lock />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- Login Attempts Warning -->
            <div v-if="authStore.loginAttempts > 0" class="text-center">
              <el-alert
                :title="`登录失败 ${authStore.loginAttempts} 次，剩余 ${maxLoginAttempts - authStore.loginAttempts} 次机会`"
                type="warning"
                :closable="false"
                show-icon
                class="mb-4"
              />
            </div>

            <!-- Lockout Warning -->
            <div v-if="authStore.isLocked" class="text-center">
              <el-alert
                :title="`账户已锁定，请 ${Math.ceil(lockoutTimeRemaining / 60)} 分钟后重试`"
                type="error"
                :closable="false"
                show-icon
                class="mb-4"
              />
            </div>

            <!-- Connection Error Warning -->
            <div v-if="showConnectionError" class="text-center">
              <el-alert
                title="连接问题检测"
                type="warning"
                :closable="true"
                show-icon
                class="mb-4"
                @close="showConnectionError = false"
              >
                <template #default>
                  <p>检测到后端服务连接问题，这可能影响登录功能。</p>
                  <el-button size="small" @click="openDiagnostic">🔧 打开诊断工具</el-button>
                </template>
              </el-alert>
            </div>

            <!-- Login Button -->
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="w-full h-12 font-medium"
                :loading="authStore.isLoading"
                :disabled="authStore.isLocked"
                @click="handleLogin"
              >
                <span v-if="!authStore.isLoading">登录管理后台</span>
                <span v-else>登录中...</span>
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- Security Notice -->
        <div class="bg-gray-50 px-8 py-4 rounded-b-xl border-t">
          <div class="flex items-center text-xs text-gray-500">
            <el-icon class="mr-2 text-amber-500">
              <Warning />
            </el-icon>
            <span>
              请妥善保管您的管理员账号信息，勿与他人共享
            </span>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="text-center">
        <p class="text-xs text-slate-400">
          © 2024 USDT交易平台. 管理后台系统
        </p>
        <p class="text-xs text-slate-400 mt-1">
          如遇问题请联系系统管理员
        </p>
      </div>
    </div>

    <!-- 系统诊断工具 -->
    <AdminDiagnostic 
      ref="diagnosticRef" 
      :auto-show="false" 
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { Monitor, User, Lock, Warning } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { Validator } from '@/utils/common'
import { checkAPIHealth } from '@/utils/crypto'
import AdminDiagnostic from '@/components/AdminDiagnostic.vue'
import type { AdminLoginRequest } from '@/types/admin'

// Hooks
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Refs
const loginFormRef = ref<InstanceType<typeof ElForm>>()
const diagnosticRef = ref<InstanceType<typeof AdminDiagnostic>>()

// Reactive data
const loginForm = reactive<AdminLoginRequest>({
  username: '',
  password: '',
  mfa_code: ''
})

const showMfaField = ref(false)
const showConnectionError = ref(false)
const maxLoginAttempts = 5
const lockoutTimer = ref<NodeJS.Timeout>()
const lockoutTimeRemaining = ref(0)

// Form validation rules
const loginRules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, message: '账号长度不能少于3位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  mfa_code: [
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (showMfaField.value && (!value || value.length !== 6)) {
          callback(new Error('请输入6位验证码'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// Methods
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    
    await authStore.login({
      username: loginForm.username,
      password: loginForm.password,
      mfa_code: loginForm.mfa_code || undefined
    })

    // 登录成功，重定向
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (error: any) {
    console.error('管理员登录失败:', error)
    
    // 检查是否是连接问题
    if (error.message?.includes('连接') || error.message?.includes('404') || error.message?.includes('网络')) {
      showConnectionError.value = true
    }
    
    // 如果是MFA错误，显示MFA输入框
    if (error.message?.includes('MFA') || error.message?.includes('双因子')) {
      showMfaField.value = true
      ElMessage.warning('请输入双因子认证码')
    }
  }
}

// 打开诊断工具
const openDiagnostic = () => {
  if (diagnosticRef.value) {
    diagnosticRef.value.openDiagnostic?.()
  }
}

// 检查API健康状态
const checkAPIStatus = async () => {
  try {
    const health = await checkAPIHealth()
    if (!health.success && !health.partialSuccess) {
      showConnectionError.value = true
      console.warn('⚠️ API健康检查失败，建议使用诊断工具')
    }
  } catch (error) {
    console.warn('⚠️ 无法检查API状态:', error)
    // 不显示错误，避免在开发环境中造成困扰
  }
}

// 更新锁定倒计时
const updateLockoutTimer = () => {
  if (authStore.isLocked) {
    lockoutTimeRemaining.value = Math.max(0, authStore.lockoutTime - Date.now()) / 1000
    
    if (lockoutTimeRemaining.value > 0) {
      lockoutTimer.value = setTimeout(updateLockoutTimer, 1000)
    }
  }
}

// Lifecycle
onMounted(async () => {
  // 如果已经登录，重定向到控制面板
  if (authStore.isAuthenticated) {
    router.push('/dashboard')
    return
  }

  // 开始锁定计时器
  if (authStore.isLocked) {
    updateLockoutTimer()
  }
  
  // 延迟检查API状态，避免阻塞页面加载
  setTimeout(() => {
    checkAPIStatus()
  }, 2000)
})

onUnmounted(() => {
  if (lockoutTimer.value) {
    clearTimeout(lockoutTimer.value)
  }
})
</script>

<style scoped>
/* 自定义样式 */
.el-form-item {
  margin-bottom: 1.5rem;
}

.el-input {
  --el-input-height: 48px;
}

.el-button {
  font-weight: 500;
}

/* 背景动画效果 */
@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.mx-auto.h-16 {
  animation: float 3s ease-in-out infinite;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .max-w-md {
    margin: 0 1rem;
  }
}
</style>