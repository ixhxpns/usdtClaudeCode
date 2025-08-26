<template>
  <AuthLayout
    form-title="创建新账户"
    form-subtitle="开始您的数字资产交易之旅"
    :loading="authStore.isLoading"
    loading-text="注册中，请稍候..."
  >
    <!-- 导航链接 -->
    <template #navigation>
      <p class="text-sm text-gray-600 dark:text-gray-400">
        已有账户？
        <router-link 
          to="/login" 
          class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 transition-colors"
        >
          立即登录
        </router-link>
      </p>
    </template>

    <!-- 注册表单 -->
    <el-form 
      ref="registerFormRef"
      :model="registerForm" 
      :rules="registerRules"
      class="space-y-6"
      @submit.prevent="handleRegister"
    >
      <!-- 用户名字段 -->
      <el-form-item prop="username" class="form-group">
        <label class="form-label">
          用户名
          <span class="text-red-500">*</span>
        </label>
        <el-input
          v-model="registerForm.username"
          size="large"
          placeholder="请输入用户名（4-20字符）"
          :prefix-icon="User"
          :disabled="authStore.isLoading"
          :loading="checkingUsername"
          @blur="checkUsernameAvailability"
        >
          <template #suffix>
            <el-icon v-if="usernameStatus === 'valid'" class="text-green-500">
              <CircleCheckFilled />
            </el-icon>
            <el-icon v-else-if="usernameStatus === 'invalid'" class="text-red-500">
              <CircleCloseFilled />
            </el-icon>
          </template>
        </el-input>
        <div v-if="usernameMessage" class="mt-1 text-xs" :class="usernameStatus === 'valid' ? 'text-green-600' : 'text-red-500'">
          {{ usernameMessage }}
        </div>
      </el-form-item>

      <!-- 邮箱字段 -->
      <el-form-item prop="email" class="form-group">
        <label class="form-label">
          邮箱地址
          <span class="text-red-500">*</span>
        </label>
        <el-input
          v-model="registerForm.email"
          type="email"
          size="large"
          placeholder="请输入邮箱地址"
          :prefix-icon="Message"
          :disabled="authStore.isLoading"
          :loading="checkingEmail"
          @blur="checkEmailAvailability"
        >
          <template #suffix>
            <el-icon v-if="emailStatus === 'valid'" class="text-green-500">
              <CircleCheckFilled />
            </el-icon>
            <el-icon v-else-if="emailStatus === 'invalid'" class="text-red-500">
              <CircleCloseFilled />
            </el-icon>
          </template>
        </el-input>
        <div v-if="emailMessage" class="mt-1 text-xs" :class="emailStatus === 'valid' ? 'text-green-600' : 'text-red-500'">
          {{ emailMessage }}
        </div>
      </el-form-item>

      <!-- 密码字段 -->
      <el-form-item prop="password" class="form-group">
        <label class="form-label">
          密码
          <span class="text-red-500">*</span>
        </label>
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large"
          placeholder="请输入密码（至少8位）"
          :prefix-icon="Lock"
          show-password
          :disabled="authStore.isLoading"
        />
        <!-- 密码强度指示器 -->
        <PasswordStrengthMeter 
          :password="registerForm.password"
          :min-length="8"
          :require-uppercase="true"
          :require-lowercase="true"
          :require-numbers="true"
          :require-special-chars="true"
        />
      </el-form-item>

      <!-- 确认密码字段 -->
      <el-form-item prop="confirm_password" class="form-group">
        <label class="form-label">
          确认密码
          <span class="text-red-500">*</span>
        </label>
        <el-input
          v-model="registerForm.confirm_password"
          type="password"
          size="large"
          placeholder="请再次输入密码"
          :prefix-icon="Lock"
          show-password
          :disabled="authStore.isLoading"
        >
          <template #suffix>
            <el-icon v-if="registerForm.confirm_password && registerForm.password === registerForm.confirm_password" class="text-green-500">
              <CircleCheckFilled />
            </el-icon>
            <el-icon v-else-if="registerForm.confirm_password && registerForm.password !== registerForm.confirm_password" class="text-red-500">
              <CircleCloseFilled />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 手机号码字段 -->
      <el-form-item prop="phone" class="form-group">
        <label class="form-label">
          手机号码
          <span class="text-gray-500 text-xs">（可选）</span>
        </label>
        <el-input
          v-model="registerForm.phone"
          size="large"
          placeholder="请输入手机号码"
          :prefix-icon="Phone"
          :disabled="authStore.isLoading"
        />
      </el-form-item>

      <!-- 邮箱验证码字段 -->
      <el-form-item prop="verification_code" class="form-group">
        <label class="form-label">
          邮箱验证码
          <span class="text-red-500">*</span>
        </label>
        <div class="flex space-x-2">
          <VerificationCodeInput
            v-model="registerForm.verification_code"
            :length="6"
            :disabled="authStore.isLoading"
            :error="verificationError"
            :show-resend="true"
            :resend-countdown="60"
            :resending="sendingCode"
            hint="请输入发送到您邮箱的6位验证码"
            @complete="handleVerificationCodeComplete"
            @resend="sendVerificationCode"
            class="flex-1"
          />
        </div>
      </el-form-item>

      <!-- 服务条款同意 -->
      <el-form-item prop="agree_terms" class="form-group">
        <el-checkbox 
          v-model="registerForm.agree_terms" 
          :disabled="authStore.isLoading"
          size="large"
        >
          <span class="text-sm text-gray-600 dark:text-gray-400">
            我已阅读并同意
            <el-button type="text" @click="showTerms = true" class="p-0 text-blue-600 hover:text-blue-500">
              《用户服务协议》
            </el-button>
            和
            <el-button type="text" @click="showPrivacy = true" class="p-0 text-blue-600 hover:text-blue-500">
              《隐私政策》
            </el-button>
          </span>
        </el-checkbox>
      </el-form-item>

      <!-- 注册按钮 -->
      <el-form-item class="form-group">
        <el-button
          type="primary"
          size="large"
          class="w-full register-button"
          :loading="authStore.isLoading"
          :disabled="!isFormValid"
          @click="handleRegister"
        >
          <template #loading>
            <el-icon class="animate-spin">
              <Loading />
            </el-icon>
          </template>
          <span v-if="!authStore.isLoading">创建账户</span>
          <span v-else>创建中...</span>
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 安全提示 -->
    <template #footer>
      <div class="text-center space-y-4">
        <!-- 社交登录预留 -->
        <div class="social-login-placeholder">
          <div class="flex items-center space-x-4 mb-4">
            <div class="flex-1 h-px bg-gray-300 dark:bg-gray-600"></div>
            <span class="text-xs text-gray-500 dark:text-gray-400">或</span>
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
        <div class="security-notice bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4">
          <div class="flex items-center space-x-2 mb-2">
            <el-icon class="text-blue-600" :size="16">
              <SuccessFilled />
            </el-icon>
            <span class="text-sm font-medium text-blue-900 dark:text-blue-100">安全提示</span>
          </div>
          <ul class="text-xs text-blue-800 dark:text-blue-200 space-y-1">
            <li>• 我们采用银行级SSL加密保护您的数据</li>
            <li>• 您的密码将经过RSA加密后传输</li>
            <li>• 建议启用双因子认证提高账户安全性</li>
          </ul>
        </div>

        <!-- 版权信息 -->
        <div class="text-xs text-gray-500 dark:text-gray-400">
          <p>© 2024 USDT Trading. 保留所有权利.</p>
          <p class="mt-1">创建账户即表示您同意接收我们的服务通知邮件</p>
        </div>
      </div>
    </template>
  </AuthLayout>

  <!-- 服务条款对话框 -->
  <el-dialog v-model="showTerms" title="用户服务协议" width="80%" max-width="800px">
    <div class="prose dark:prose-invert max-w-none">
      <h3>服务条款</h3>
      <p>欢迎使用USDT交易平台。请仔细阅读以下条款和条件...</p>
      <!-- 这里应该是完整的服务条款内容 -->
    </div>
    <template #footer>
      <el-button @click="showTerms = false">关闭</el-button>
      <el-button type="primary" @click="showTerms = false; registerForm.agree_terms = true">
        同意并关闭
      </el-button>
    </template>
  </el-dialog>

  <!-- 隐私政策对话框 -->
  <el-dialog v-model="showPrivacy" title="隐私政策" width="80%" max-width="800px">
    <div class="prose dark:prose-invert max-w-none">
      <h3>隐私政策</h3>
      <p>我们重视您的隐私权。本政策说明我们如何收集、使用和保护您的个人信息...</p>
      <!-- 这里应该是完整的隐私政策内容 -->
    </div>
    <template #footer>
      <el-button @click="showPrivacy = false">关闭</el-button>
      <el-button type="primary" @click="showPrivacy = false; registerForm.agree_terms = true">
        同意并关闭
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { 
  Message, 
  Lock, 
  Phone, 
  SuccessFilled, 
  User,
  Loading,
  CircleCheckFilled,
  CircleCloseFilled,
  Star
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { AuthAPI } from '@/api/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import PasswordStrengthMeter from '@/components/auth/PasswordStrengthMeter.vue'
import VerificationCodeInput from '@/components/auth/VerificationCodeInput.vue'
import { validateEmail, validateUsername, validatePhone, validatePassword } from '@/utils/validators'
import type { RegisterRequest } from '@/types/user'

// Hooks
const router = useRouter()
const authStore = useAuthStore()

// Refs
const registerFormRef = ref<InstanceType<typeof ElForm>>()
const sendingCode = ref(false)
const checkingUsername = ref(false)
const checkingEmail = ref(false)
const verificationError = ref('')
const showTerms = ref(false)
const showPrivacy = ref(false)

// 用户名和邮箱验证状态
const usernameStatus = ref<'valid' | 'invalid' | ''>('')
const usernameMessage = ref('')
const emailStatus = ref<'valid' | 'invalid' | ''>('')
const emailMessage = ref('')

// 社交登录提供商（预留）
const socialProviders = [
  { name: 'Google', icon: Star },
  { name: 'Apple', icon: Star },
  { name: 'GitHub', icon: Star }
]

// Reactive data
const registerForm = reactive<RegisterRequest & { username: string }>({
  username: '',
  email: '',
  password: '',
  confirm_password: '',
  phone: '',
  verification_code: '',
  agree_terms: false
})

// 表单验证状态
const isFormValid = computed(() => {
  return registerForm.username &&
         registerForm.email &&
         registerForm.password &&
         registerForm.confirm_password &&
         registerForm.verification_code &&
         registerForm.agree_terms &&
         usernameStatus.value === 'valid' &&
         emailStatus.value === 'valid' &&
         registerForm.password === registerForm.confirm_password
})

// 表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度应在4-20字符之间', trigger: 'blur' },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (value && !validateUsername(value)) {
          callback(new Error('用户名只能包含字母、数字和下划线'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (value && !validateEmail(value)) {
          callback(new Error('请输入有效的邮箱地址'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        const result = validatePassword(value)
        if (!result.valid) {
          callback(new Error(result.message))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  confirm_password: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  phone: [
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (value && !validatePhone(value)) {
          callback(new Error('请输入有效的手机号码'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  verification_code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  agree_terms: [
    { 
      validator: (rule: any, value: boolean, callback: Function) => {
        if (!value) {
          callback(new Error('请阅读并同意用户协议'))
        } else {
          callback()
        }
      }, 
      trigger: 'change' 
    }
  ]
}

// 检查用户名可用性
const checkUsernameAvailability = async () => {
  if (!registerForm.username || !validateUsername(registerForm.username)) {
    usernameStatus.value = ''
    usernameMessage.value = ''
    return
  }

  try {
    checkingUsername.value = true
    const response = await AuthAPI.checkUsernameAvailability(registerForm.username)
    
    if (response.available) {
      usernameStatus.value = 'valid'
      usernameMessage.value = '用户名可用'
    } else {
      usernameStatus.value = 'invalid'
      usernameMessage.value = '用户名已被使用'
    }
  } catch (error) {
    usernameStatus.value = 'invalid'
    usernameMessage.value = '检查用户名可用性失败'
  } finally {
    checkingUsername.value = false
  }
}

// 检查邮箱可用性
const checkEmailAvailability = async () => {
  if (!registerForm.email || !validateEmail(registerForm.email)) {
    emailStatus.value = ''
    emailMessage.value = ''
    return
  }

  try {
    checkingEmail.value = true
    const response = await AuthAPI.checkEmailAvailability(registerForm.email)
    
    if (response.available) {
      emailStatus.value = 'valid'
      emailMessage.value = '邮箱可用'
    } else {
      emailStatus.value = 'invalid'
      emailMessage.value = '邮箱已被注册'
    }
  } catch (error) {
    emailStatus.value = 'invalid'
    emailMessage.value = '检查邮箱可用性失败'
  } finally {
    checkingEmail.value = false
  }
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!registerForm.email || emailStatus.value !== 'valid') {
    ElMessage.warning('请先输入有效的邮箱地址')
    return
  }

  try {
    sendingCode.value = true
    await AuthAPI.sendEmailVerification({ email: registerForm.email })
    ElMessage.success('验证码已发送到您的邮箱')
    verificationError.value = ''
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
    verificationError.value = error.message || '发送验证码失败'
  } finally {
    sendingCode.value = false
  }
}

// 验证码输入完成
const handleVerificationCodeComplete = (code: string) => {
  registerForm.verification_code = code
  verificationError.value = ''
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    // 表单验证
    await registerFormRef.value.validate()
    
    // 检查必要的验证状态
    if (usernameStatus.value !== 'valid') {
      ElMessage.error('请确认用户名可用')
      return
    }
    
    if (emailStatus.value !== 'valid') {
      ElMessage.error('请确认邮箱可用')
      return
    }

    // 注册
    const { username, ...registerData } = registerForm
    await authStore.register(registerData)
    
    ElMessage.success('注册成功，欢迎加入我们！')
    
    // 注册成功，跳转到仪表板或邮箱验证页面
    if (!authStore.isEmailVerified) {
      router.push('/verify-email')
    } else {
      router.push('/dashboard')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    verificationError.value = error.message || '注册失败'
  }
}

// 监听密码变化以触发确认密码验证
watch(
  () => registerForm.password,
  () => {
    if (registerForm.confirm_password && registerFormRef.value) {
      registerFormRef.value.validateField('confirm_password')
    }
  }
)
</script>

<style scoped>
.form-group {
  @apply mb-6;
}

.form-label {
  @apply block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2;
}

.register-button {
  @apply h-12 font-semibold text-base rounded-xl transition-all duration-200;
}

.register-button:hover:not(:disabled) {
  @apply transform -translate-y-0.5 shadow-lg;
}

.social-login-btn {
  @apply w-12 h-12 rounded-full border-2 border-gray-300 dark:border-gray-600;
  @apply bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700;
  @apply transition-colors duration-200;
}

.security-notice {
  @apply border border-blue-200 dark:border-blue-800;
}

/* 响应式调整 */
@media (max-width: 640px) {
  .form-group {
    @apply mb-4;
  }
  
  .register-button {
    @apply h-11 text-sm;
  }
}

/* Element Plus 组件样式覆写 */
:deep(.el-input__inner) {
  @apply h-12 text-base;
}

:deep(.el-checkbox__label) {
  @apply text-sm leading-relaxed;
}

:deep(.el-form-item__error) {
  @apply text-xs;
}
</style>