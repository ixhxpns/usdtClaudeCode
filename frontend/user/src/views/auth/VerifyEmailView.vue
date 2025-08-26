<template>
  <AuthLayout
    form-title="验证您的邮箱"
    form-subtitle="我们已向您的邮箱发送了验证码"
    :loading="authStore.isLoading"
    loading-text="验证中，请稍候..."
  >
    <!-- 导航信息 -->
    <template #navigation>
      <div class="text-center">
        <div class="inline-flex items-center space-x-2 text-sm text-gray-600 dark:text-gray-400">
          <el-icon :size="16" class="text-blue-500">
            <Message />
          </el-icon>
          <span>验证码已发送至</span>
          <strong class="text-gray-900 dark:text-white">{{ maskedEmail }}</strong>
        </div>
      </div>
    </template>

    <!-- 验证表单 -->
    <div class="space-y-6">
      <!-- 进度指示器 -->
      <div class="verification-progress">
        <div class="flex items-center justify-center mb-6">
          <div class="flex items-center space-x-4">
            <!-- 步骤1：发送邮件 -->
            <div class="flex items-center">
              <div class="w-8 h-8 bg-green-500 rounded-full flex items-center justify-center">
                <el-icon :size="16" class="text-white">
                  <Check />
                </el-icon>
              </div>
              <span class="ml-2 text-sm text-green-600 font-medium">邮件已发送</span>
            </div>
            
            <!-- 连接线 -->
            <div class="w-8 h-0.5 bg-gray-300 dark:bg-gray-600"></div>
            
            <!-- 步骤2：验证邮箱 -->
            <div class="flex items-center">
              <div class="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center animate-pulse">
                <el-icon :size="16" class="text-white">
                  <SuccessFilled />
                </el-icon>
              </div>
              <span class="ml-2 text-sm text-blue-600 font-medium">验证邮箱</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 验证码输入区域 -->
      <div class="verification-input-section">
        <el-form 
          ref="verifyFormRef"
          :model="verifyForm" 
          :rules="verifyRules"
          @submit.prevent="handleVerifyEmail"
        >
          <el-form-item prop="token">
            <label class="form-label text-center block mb-4">
              请输入6位验证码
            </label>
            <VerificationCodeInput
              v-model="verifyForm.token"
              :length="6"
              :disabled="authStore.isLoading || verifySuccess"
              :error="verificationError"
              :show-resend="true"
              :resend-countdown="60"
              :resending="sendingCode"
              hint="请输入发送到您邮箱的6位验证码"
              @complete="handleVerificationComplete"
              @resend="handleResendCode"
              class="mb-6"
            />
            
            <!-- 验证状态 -->
            <div v-if="verifySuccess" class="text-center">
              <div class="inline-flex items-center space-x-2 text-green-600">
                <el-icon :size="20">
                  <CircleCheckFilled />
                </el-icon>
                <span class="font-medium">邮箱验证成功！</span>
              </div>
            </div>
          </el-form-item>

          <!-- 验证按钮 -->
          <el-form-item v-if="!verifySuccess">
            <el-button
              type="primary"
              size="large"
              class="w-full verify-button"
              :loading="authStore.isLoading"
              :disabled="!canVerify"
              @click="handleVerifyEmail"
            >
              <template #loading>
                <el-icon class="animate-spin">
                  <Loading />
                </el-icon>
              </template>
              <span v-if="!authStore.isLoading">验证邮箱</span>
              <span v-else>验证中...</span>
            </el-button>
          </el-form-item>

          <!-- 继续按钮 -->
          <el-form-item v-if="verifySuccess">
            <el-button
              type="success"
              size="large"
              class="w-full"
              @click="handleContinue"
            >
              继续到仪表板
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 帮助信息 -->
      <div class="help-section">
        <!-- 未收到验证码 -->
        <el-card class="bg-gray-50 dark:bg-gray-800 border-0">
          <div class="text-center">
            <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">
              未收到验证码？
            </h4>
            <div class="space-y-2 text-xs text-gray-600 dark:text-gray-400">
              <p>• 请检查您的垃圾邮件文件夹</p>
              <p>• 验证码有效期为10分钟</p>
              <p>• 如果仍未收到，请点击重新发送</p>
            </div>
            
            <!-- 更换邮箱按钮 -->
            <div class="mt-4">
              <el-button
                type="text"
                size="small"
                @click="showChangeEmailDialog = true"
                class="text-blue-600 hover:text-blue-500"
              >
                更换邮箱地址
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 安全提示 -->
        <div class="security-tip bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 mt-4">
          <div class="flex items-start space-x-2">
            <el-icon class="text-blue-600 mt-0.5" :size="16">
              <InfoFilled />
            </el-icon>
            <div>
              <h4 class="text-sm font-medium text-blue-900 dark:text-blue-100 mb-1">
                安全提示
              </h4>
              <div class="text-xs text-blue-800 dark:text-blue-200 space-y-1">
                <p>• 请勿将验证码分享给他人</p>
                <p>• 如非本人操作，请立即联系客服</p>
                <p>• 验证完成后将自动启用邮箱安全功能</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部信息 -->
    <template #footer>
      <div class="text-center space-y-4">
        <!-- 客服联系 -->
        <div class="text-sm text-gray-500 dark:text-gray-400">
          <span>需要帮助？</span>
          <el-button type="text" size="small" class="text-blue-600 hover:text-blue-500 ml-1">
            联系客服
          </el-button>
        </div>

        <!-- 版权信息 -->
        <div class="text-xs text-gray-400 dark:text-gray-500">
          <p>© 2024 USDT Trading. 保留所有权利.</p>
        </div>
      </div>
    </template>
  </AuthLayout>

  <!-- 更换邮箱对话框 -->
  <el-dialog
    v-model="showChangeEmailDialog"
    title="更换邮箱地址"
    width="90%"
    style="max-width: 500px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="changeEmailFormRef"
      :model="changeEmailForm"
      :rules="changeEmailRules"
      label-width="80px"
    >
      <el-form-item label="当前邮箱" prop="currentEmail">
        <el-input
          v-model="changeEmailForm.currentEmail"
          disabled
          size="large"
        />
      </el-form-item>
      
      <el-form-item label="新邮箱" prop="newEmail">
        <el-input
          v-model="changeEmailForm.newEmail"
          type="email"
          size="large"
          placeholder="请输入新的邮箱地址"
        />
      </el-form-item>
      
      <div class="text-xs text-gray-500 mb-4">
        注意：更换邮箱后需要重新进行邮箱验证
      </div>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="showChangeEmailDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="changingEmail"
          @click="handleChangeEmail"
        >
          确认更换
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElForm, ElMessage } from 'element-plus'
import { 
  Message, 
  SuccessFilled, 
  Check,
  Loading,
  CircleCheckFilled,
  InfoFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import VerificationCodeInput from '@/components/auth/VerificationCodeInput.vue'
import { validateEmail } from '@/utils/validators'

// Hooks
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Refs
const verifyFormRef = ref<InstanceType<typeof ElForm>>()
const changeEmailFormRef = ref<InstanceType<typeof ElForm>>()
const sendingCode = ref(false)
const verifySuccess = ref(false)
const verificationError = ref('')
const showChangeEmailDialog = ref(false)
const changingEmail = ref(false)

// Reactive data
const verifyForm = reactive({
  token: ''
})

const changeEmailForm = reactive({
  currentEmail: authStore.user?.email || '',
  newEmail: ''
})

// 计算属性
const maskedEmail = computed(() => {
  const email = authStore.user?.email || ''
  if (!email) return ''
  
  const [username, domain] = email.split('@')
  if (username.length <= 2) return email
  
  const maskedUsername = username[0] + '*'.repeat(username.length - 2) + username[username.length - 1]
  return `${maskedUsername}@${domain}`
})

const canVerify = computed(() => {
  return verifyForm.token.length === 6 && !authStore.isLoading
})

// 表单验证规则
const verifyRules = {
  token: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
}

const changeEmailRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱地址', trigger: 'blur' },
    { 
      validator: (rule: any, value: string, callback: Function) => {
        if (value && !validateEmail(value)) {
          callback(new Error('请输入有效的邮箱地址'))
        } else if (value === changeEmailForm.currentEmail) {
          callback(new Error('新邮箱不能与当前邮箱相同'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// 验证码输入完成
const handleVerificationComplete = (token: string) => {
  verifyForm.token = token
  verificationError.value = ''
  
  // 自动验证
  if (token.length === 6) {
    handleVerifyEmail()
  }
}

// 重新发送验证码
const handleResendCode = async () => {
  try {
    sendingCode.value = true
    await authStore.sendEmailVerification()
    verificationError.value = ''
  } catch (error: any) {
    verificationError.value = error.message || '发送验证码失败'
  } finally {
    sendingCode.value = false
  }
}

// 验证邮箱
const handleVerifyEmail = async () => {
  if (!verifyFormRef.value) return

  try {
    await verifyFormRef.value.validate()
    
    await authStore.verifyEmail(verifyForm.token)
    
    verifySuccess.value = true
    verificationError.value = ''
    
    ElMessage.success('邮箱验证成功！')
    
    // 3秒后自动跳转
    setTimeout(() => {
      handleContinue()
    }, 3000)
    
  } catch (error: any) {
    console.error('邮箱验证失败:', error)
    verificationError.value = error.message || '验证失败，请重试'
    
    // 清空验证码以便重新输入
    verifyForm.token = ''
  }
}

// 继续到仪表板
const handleContinue = () => {
  const redirectTo = route.query.redirect as string || '/dashboard'
  router.push(redirectTo)
}

// 更换邮箱
const handleChangeEmail = async () => {
  if (!changeEmailFormRef.value) return

  try {
    await changeEmailFormRef.value.validate()
    
    changingEmail.value = true
    
    // 这里应该调用更换邮箱的API
    // await AuthAPI.changeEmail(changeEmailForm.newEmail)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    ElMessage.success('邮箱更换成功，请重新进行验证')
    
    showChangeEmailDialog.value = false
    
    // 重新发送验证码到新邮箱
    await handleResendCode()
    
  } catch (error: any) {
    ElMessage.error(error.message || '更换邮箱失败')
  } finally {
    changingEmail.value = false
  }
}

// 组件挂载时初始化
onMounted(async () => {
  // 如果用户已经验证过邮箱，直接跳转
  if (authStore.isEmailVerified) {
    ElMessage.info('您的邮箱已经验证过了')
    handleContinue()
    return
  }
  
  // 如果没有用户信息，跳转到登录页面
  if (!authStore.user) {
    router.push('/login')
    return
  }
  
  // 从URL参数中获取token（比如从邮件链接点击进入）
  const tokenParam = route.query.token as string
  if (tokenParam && tokenParam.length === 6) {
    verifyForm.token = tokenParam
    // 自动验证
    await handleVerifyEmail()
  }
  
  // 设置当前邮箱到更换表单
  changeEmailForm.currentEmail = authStore.user?.email || ''
})
</script>

<style scoped>
.form-label {
  @apply text-sm font-medium text-gray-700 dark:text-gray-300;
}

.verification-progress {
  @apply mb-8;
}

.verification-input-section {
  @apply mb-8;
}

.verify-button {
  @apply h-12 font-semibold text-base rounded-xl transition-all duration-200;
}

.verify-button:hover:not(:disabled) {
  @apply transform -translate-y-0.5 shadow-lg;
}

.help-section {
  @apply space-y-4;
}

.security-tip {
  @apply border border-blue-200 dark:border-blue-800;
}

/* 响应式调整 */
@media (max-width: 640px) {
  .verification-progress {
    @apply mb-6;
  }
  
  .verification-progress .flex {
    @apply flex-col space-x-0 space-y-4;
  }
  
  .verification-progress .w-8.h-0 {
    @apply w-0 h-8 rotate-90;
  }
}

/* Element Plus 组件样式覆写 */
:deep(.el-form-item__error) {
  @apply text-xs text-center;
}

:deep(.el-card__body) {
  @apply p-4;
}
</style>