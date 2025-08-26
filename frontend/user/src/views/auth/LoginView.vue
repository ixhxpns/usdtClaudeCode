<template>
  <AuthLayout
    form-title="欢迎回来"
    form-subtitle="请登录您的账户继续交易"
    :loading="authStore.isLoading"
    loading-text="登录中，请稍候..."
  >
    <!-- 导航链接 -->
    <template #navigation>
      <p class="text-sm text-gray-600 dark:text-gray-400">
        还没有账户？
        <router-link 
          to="/register" 
          class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 transition-colors"
        >
          立即注册
        </router-link>
      </p>
    </template>

    <!-- 登录表单 -->
    <el-form 
      ref="loginFormRef"
      :model="loginForm" 
      :rules="loginRules"
      class="space-y-6"
      @submit.prevent="handleLogin"
    >
      <!-- 登录方式切换 -->
      <div class="login-type-switcher">
        <div class="flex items-center justify-center space-x-4 mb-6">
          <el-radio-group v-model="loginType" size="large" class="login-type-group">
            <el-radio-button label="email">邮箱登录</el-radio-button>
            <el-radio-button label="username">用户名登录</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 邮箱/用户名字段 -->
      <el-form-item :prop="loginType === 'email' ? 'email' : 'username'" class="form-group">
        <label class="form-label">
          {{ loginType === 'email' ? '邮箱地址' : '用户名' }}
          <span class="text-red-500">*</span>
        </label>
        <el-input
          v-if="loginType === 'email'"
          v-model="loginForm.email"
          type="email"
          size="large"
          placeholder="请输入邮箱地址"
          :prefix-icon="Message"
          :disabled="authStore.isLoading"
          autocomplete="email"
          @keyup.enter="handleLogin"
        />
        <el-input
          v-else
          v-model="loginForm.username"
          size="large"
          placeholder="请输入用户名"
          :prefix-icon="User"
          :disabled="authStore.isLoading"
          autocomplete="username"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <!-- 密码字段 -->
      <el-form-item prop="password" class="form-group">
        <label class="form-label">
          密码
          <span class="text-red-500">*</span>
        </label>
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          placeholder="请输入密码"
          :prefix-icon="Lock"
          show-password
          :disabled="authStore.isLoading"
          autocomplete="current-password"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <!-- 图形验证码 -->
      <el-form-item v-if="showCaptcha" prop="captcha" class="form-group">
        <label class="form-label">
          验证码
          <span class="text-red-500">*</span>
        </label>
        <div class="flex space-x-2">
          <el-input
            v-model="loginForm.captcha"
            size="large"
            placeholder="请输入验证码"
            :prefix-icon="SuccessFilled"
            :disabled="authStore.isLoading"
            class="flex-1"
            @keyup.enter="handleLogin"
          />
          <div 
            class="captcha-image cursor-pointer"
            @click="refreshCaptcha"
            :title="'点击刷新验证码'"
          >
            <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" class="h-12 w-24 border rounded object-cover" />
            <div v-else class="h-12 w-24 border rounded bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
              <el-icon class="text-gray-400">
                <Refresh />
              </el-icon>
            </div>
          </div>
        </div>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">点击图片刷新验证码</p>
      </el-form-item>

      <!-- MFA验证码 -->
      <el-form-item v-if="showMFA" prop="mfa_code" class="form-group">
        <label class="form-label">
          双因子认证码
          <span class="text-red-500">*</span>
        </label>
        <VerificationCodeInput
          v-model="loginForm.mfa_code"
          :length="6"
          :disabled="authStore.isLoading"
          :show-resend="false"
          hint="请输入您的双因子认证器生成的6位验证码"
          @complete="handleMFAComplete"
        />
      </el-form-item>

      <!-- 记住登录和忘记密码 -->
      <div class="form-options flex items-center justify-between">
        <el-checkbox 
          v-model="loginForm.remember_me" 
          :disabled="authStore.isLoading"
          size="large"
        >
          <span class="text-sm text-gray-600 dark:text-gray-400">记住我</span>
        </el-checkbox>
        
        <el-button
          type="text"
          @click="$router.push('/forgot-password')"
          class="text-sm text-blue-600 hover:text-blue-500 dark:text-blue-400"
        >
          忘记密码？
        </el-button>
      </div>

      <!-- 登录失败提示 -->
      <div v-if="authStore.loginAttempts > 0" class="login-attempts-warning">
        <el-alert
          :title="`登录失败 ${authStore.loginAttempts} 次`"
          :description="authStore.isLocked ? 
            `账户已锁定，请稍后重试` : 
            `还可尝试 ${maxLoginAttempts - authStore.loginAttempts} 次`"
          :type="authStore.isLocked ? 'error' : 'warning'"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 登录按钮 -->
      <el-form-item class="form-group">
        <el-button
          type="primary"
          size="large"
          class="w-full login-button"
          :loading="authStore.isLoading"
          :disabled="authStore.isLocked"
          @click="handleLogin"
        >
          <template #loading>
            <el-icon class="animate-spin">
              <Loading />
            </el-icon>
          </template>
          <span v-if="!authStore.isLoading">
            {{ authStore.isLocked ? '账户已锁定' : '登录账户' }}
          </span>
          <span v-else>登录中...</span>
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 底部信息 -->
    <template #footer>
      <div class="text-center space-y-4">
        <!-- 社交登录预留 -->
        <div class="social-login-placeholder">
          <div class="flex items-center space-x-4 mb-4">
            <div class="flex-1 h-px bg-gray-300 dark:bg-gray-600"></div>
            <span class="text-xs text-gray-500 dark:text-gray-400">或使用以下方式登录</span>
            <div class="flex-1 h-px bg-gray-300 dark:bg-gray-600"></div>
          </div>
          <div class="flex justify-center space-x-4">
            <el-button 
              v-for="provider in socialProviders" 
              :key="provider.name"
              :disabled="true"
              size="large"
              class="social-login-btn"
              :title="`${provider.name}登录（即将推出）`"
            >
              <el-icon>
                <component :is="provider.icon" />
              </el-icon>
            </el-button>
          </div>
          <p class="text-xs text-gray-400 mt-2">社交登录功能即将推出</p>
        </div>

        <!-- 安全提示 -->
        <div class="security-notice bg-green-50 dark:bg-green-900/20 rounded-lg p-4">
          <div class="flex items-center space-x-2 mb-2">
            <el-icon class="text-green-600" :size="16">
              <SuccessFilled />
            </el-icon>
            <span class="text-sm font-medium text-green-900 dark:text-green-100">登录安全</span>
          </div>
          <ul class="text-xs text-green-800 dark:text-green-200 space-y-1">
            <li>• 系统会记录您的登录设备和IP地址</li>
            <li>• 异常登录活动将触发安全验证</li>
            <li>• 建议定期更改密码并启用双因子认证</li>
          </ul>
        </div>

        <!-- 版权信息 -->
        <div class="text-xs text-gray-500 dark:text-gray-400">
          <p>© 2024 USDT Trading. 保留所有权利.</p>
        </div>
      </div>
    </template>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { 
  Message, 
  Lock, 
  SuccessFilled, 
  User,
  Loading,
  Refresh,
  Star
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import VerificationCodeInput from '@/components/auth/VerificationCodeInput.vue'
import { validateEmail, validateUsername } from '@/utils/validators'
import type { LoginRequest } from '@/types/user'

// Hooks
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Refs
const loginFormRef = ref<InstanceType<typeof ElForm>>()
const loginType = ref<'email' | 'username'>('email')
const showCaptcha = ref(false)
const showMFA = ref(false)
const captchaUrl = ref('')
const maxLoginAttempts = 5

// 社交登录提供商（预留）
const socialProviders = [
  { name: 'Google', icon: Star },
  { name: 'Apple', icon: Star },
  { name: 'WeChat', icon: Star }
]

// Reactive data
const loginForm = reactive<LoginRequest & { username?: string; captcha?: string }>({
  email: '',
  username: '',
  password: '',
  mfa_code: '',
  remember_me: false,
  captcha: ''
})

// 表单验证规则
const loginRules = computed(() => ({
  email: [
    { 
      required: loginType.value === 'email', 
      message: '请输入邮箱地址', 
      trigger: 'blur' 
    },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (loginType.value === 'email' && value && !validateEmail(value)) {
          callback(new Error('请输入有效的邮箱地址'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  username: [
    { 
      required: loginType.value === 'username', 
      message: '请输入用户名', 
      trigger: 'blur' 
    },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (loginType.value === 'username' && value && !validateUsername(value)) {
          callback(new Error('请输入有效的用户名'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 1, message: '密码不能为空', trigger: 'blur' }
  ],
  captcha: [
    { 
      required: showCaptcha.value, 
      message: '请输入验证码', 
      trigger: 'blur' 
    },
    { 
      len: 4, 
      message: '验证码长度为4位', 
      trigger: 'blur' 
    }
  ],
  mfa_code: [
    { 
      required: showMFA.value, 
      message: '请输入双因子认证码', 
      trigger: 'blur' 
    },
    { 
      len: 6, 
      message: '认证码长度为6位', 
      trigger: 'blur' 
    }
  ]
}))

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    // 这里应该调用获取验证码的API
    // const response = await AuthAPI.getCaptcha()
    // captchaUrl.value = response.captcha_url
    
    // 模拟验证码URL
    captchaUrl.value = `data:image/svg+xml;base64,${btoa(`
      <svg width="96" height="48" xmlns="http://www.w3.org/2000/svg">
        <rect width="96" height="48" fill="#f0f0f0"/>
        <text x="48" y="30" text-anchor="middle" fill="#666" font-size="16" font-family="monospace">
          ${Math.random().toString(36).substring(2, 6).toUpperCase()}
        </text>
      </svg>
    `)}`
  } catch (error) {
    ElMessage.error('获取验证码失败')
  }
}

// MFA验证码输入完成
const handleMFAComplete = (code: string) => {
  loginForm.mfa_code = code
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    // 表单验证
    await loginFormRef.value.validate()
    
    // 准备登录数据
    const loginData: LoginRequest = {
      email: loginType.value === 'email' ? loginForm.email : loginForm.username || '',
      password: loginForm.password,
      remember_me: loginForm.remember_me
    }

    // 添加可选字段
    if (showCaptcha.value && loginForm.captcha) {
      (loginData as any).captcha = loginForm.captcha
    }
    
    if (showMFA.value && loginForm.mfa_code) {
      loginData.mfa_code = loginForm.mfa_code
    }

    // 执行登录
    await authStore.login(loginData)
    
    ElMessage.success('登录成功，欢迎回来！')
    
    // 跳转到指定页面或仪表板
    const redirectTo = route.query.redirect as string || '/dashboard'
    router.push(redirectTo)
    
  } catch (error: any) {
    console.error('登录失败:', error)
    
    // 根据错误类型显示不同的处理逻辑
    if (error.code === 'CAPTCHA_REQUIRED') {
      showCaptcha.value = true
      refreshCaptcha()
      ElMessage.warning('请完成验证码验证')
    } else if (error.code === 'MFA_REQUIRED') {
      showMFA.value = true
      ElMessage.info('请输入双因子认证码')
    } else if (error.code === 'ACCOUNT_LOCKED') {
      ElMessage.error('账户已被锁定，请联系客服')
    } else if (error.code === 'TOO_MANY_ATTEMPTS') {
      showCaptcha.value = true
      refreshCaptcha()
    }
  }
}

// 组件挂载时的初始化
onMounted(() => {
  // 如果登录失败次数过多，显示验证码
  if (authStore.loginAttempts >= 3) {
    showCaptcha.value = true
    refreshCaptcha()
  }
  
  // 从URL参数中获取邮箱（比如从注册页面跳转过来）
  const emailParam = route.query.email as string
  if (emailParam && validateEmail(emailParam)) {
    loginForm.email = emailParam
    loginType.value = 'email'
  }
})
</script>

<style scoped>
.form-group {
  @apply mb-6;
}

.form-label {
  @apply block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2;
}

.login-type-group {
  @apply rounded-xl bg-gray-100 dark:bg-gray-700 p-1;
}

.login-type-group :deep(.el-radio-button__inner) {
  @apply border-0 bg-transparent text-sm font-medium px-4 py-2 rounded-lg;
  @apply text-gray-600 dark:text-gray-300;
  @apply transition-all duration-200;
}

.login-type-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  @apply bg-white dark:bg-gray-600 text-blue-600 dark:text-blue-400 shadow-sm;
}

.login-button {
  @apply h-12 font-semibold text-base rounded-xl transition-all duration-200;
}

.login-button:hover:not(:disabled) {
  @apply transform -translate-y-0.5 shadow-lg;
}

.form-options {
  @apply mb-4;
}

.login-attempts-warning {
  @apply mb-4;
}

.captcha-image {
  @apply transition-transform duration-200 hover:scale-105;
}

.social-login-btn {
  @apply w-12 h-12 rounded-full border-2 border-gray-300 dark:border-gray-600;
  @apply bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700;
  @apply transition-colors duration-200;
}

.security-notice {
  @apply border border-green-200 dark:border-green-800;
}

/* 响应式调整 */
@media (max-width: 640px) {
  .form-group {
    @apply mb-4;
  }
  
  .login-button {
    @apply h-11 text-sm;
  }
  
  .login-type-group :deep(.el-radio-button__inner) {
    @apply px-3 py-1.5 text-xs;
  }
}

/* Element Plus 组件样式覆写 */
:deep(.el-input__inner) {
  @apply h-12 text-base;
}

:deep(.el-checkbox__label) {
  @apply text-sm;
}

:deep(.el-form-item__error) {
  @apply text-xs;
}

:deep(.el-alert) {
  @apply rounded-lg;
}
</style>