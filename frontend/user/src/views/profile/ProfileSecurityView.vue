<template>
  <div class="security-view">
    <!-- 安全概覽 -->
    <el-card class="security-overview" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>安全概覽</h3>
          <el-tag :type="securityLevel.type" size="large">
            {{ securityLevel.text }}
          </el-tag>
        </div>
      </template>

      <div class="security-items">
        <div class="security-item">
          <div class="item-info">
            <el-icon class="item-icon" :class="{ verified: user?.email_verified }">
              <Message />
            </el-icon>
            <div class="item-content">
              <div class="item-title">郵箱驗證</div>
              <div class="item-desc">{{ user?.email || '未設置' }}</div>
            </div>
          </div>
          <div class="item-action">
            <el-tag v-if="user?.email_verified" type="success">已驗證</el-tag>
            <el-button 
              v-else 
              type="warning" 
              size="small"
              @click="sendEmailVerification"
              :loading="emailVerifying"
            >
              立即驗證
            </el-button>
          </div>
        </div>

        <div class="security-item">
          <div class="item-info">
            <el-icon class="item-icon" :class="{ verified: user?.phone_verified }">
              <Phone />
            </el-icon>
            <div class="item-content">
              <div class="item-title">手機驗證</div>
              <div class="item-desc">{{ user?.phone || '未設置' }}</div>
            </div>
          </div>
          <div class="item-action">
            <el-tag v-if="user?.phone_verified" type="success">已驗證</el-tag>
            <el-button 
              v-else 
              type="warning" 
              size="small"
              @click="showPhoneVerification"
            >
              立即驗證
            </el-button>
          </div>
        </div>

        <div class="security-item">
          <div class="item-info">
            <el-icon class="item-icon" :class="{ verified: user?.mfa_enabled }">
              <Key />
            </el-icon>
            <div class="item-content">
              <div class="item-title">雙因子認證 (2FA)</div>
              <div class="item-desc">{{ user?.mfa_enabled ? '已啟用' : '未啟用' }}</div>
            </div>
          </div>
          <div class="item-action">
            <el-switch
              v-model="mfaEnabled"
              @change="toggleMFA"
              :loading="mfaLoading"
            />
          </div>
        </div>

        <div class="security-item">
          <div class="item-info">
            <el-icon class="item-icon verified">
              <Lock />
            </el-icon>
            <div class="item-content">
              <div class="item-title">登錄密碼</div>
              <div class="item-desc">定期更換密碼可提高安全性</div>
            </div>
          </div>
          <div class="item-action">
            <el-button size="small" @click="showChangePassword">
              修改密碼
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 登錄記錄 -->
    <el-card class="login-history" shadow="never">
      <template #header>
        <h3>最近登錄記錄</h3>
      </template>

      <el-table 
        :data="loginHistory" 
        v-loading="historyLoading"
        empty-text="暂无登录记录"
      >
        <el-table-column label="登錄時間" min-width="180">
          <template #default="{ row }">
            {{ dayjs(row.login_time).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
        </el-table-column>
        <el-table-column label="IP地址" prop="ip_address" min-width="120" />
        <el-table-column label="地理位置" prop="location" min-width="120" />
        <el-table-column label="設備信息" min-width="200">
          <template #default="{ row }">
            {{ row.device_info?.device || '未知設備' }}
          </template>
        </el-table-column>
        <el-table-column label="登錄狀態" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失敗' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="loginHistory.length > 0" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalHistory"
          layout="prev, pager, next, total"
          @current-change="loadLoginHistory"
        />
      </div>
    </el-card>

    <!-- 安全設置 -->
    <el-card class="security-settings" shadow="never">
      <template #header>
        <h3>安全設置</h3>
      </template>

      <el-form :model="securityForm" label-width="140px">
        <el-form-item label="登錄通知">
          <el-switch
            v-model="securityForm.loginNotifications"
            @change="updateSecuritySettings"
            active-text="開啟"
            inactive-text="關閉"
          />
          <div class="form-tip">
            開啟後，每次登錄都會發送郵件通知
          </div>
        </el-form-item>

        <el-form-item label="二次驗證">
          <el-switch
            v-model="securityForm.twoStepVerification"
            @change="updateSecuritySettings"
            active-text="開啟"
            inactive-text="關閉"
          />
          <div class="form-tip">
            重要操作需要額外的驗證步驟
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密碼對話框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密碼"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="當前密碼" prop="currentPassword">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            placeholder="請輸入當前密碼"
            show-password
          />
        </el-form-item>

        <el-form-item label="新密碼" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="請輸入新密碼"
            show-password
          />
          <password-strength-meter :password="passwordForm.newPassword" />
        </el-form-item>

        <el-form-item label="確認密碼" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="請再次輸入新密碼"
            show-password
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="changePassword"
          :loading="passwordChanging"
        >
          確認修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 手機驗證對話框 -->
    <el-dialog
      v-model="phoneDialogVisible"
      title="手機驗證"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="phone-verification">
        <div class="phone-step" v-if="phoneStep === 1">
          <el-form :model="phoneForm" label-width="80px">
            <el-form-item label="手機號">
              <el-input 
                v-model="phoneForm.phone" 
                placeholder="請輸入手機號碼"
                :disabled="!!user?.phone"
              />
            </el-form-item>
          </el-form>
          <el-button 
            type="primary" 
            @click="sendPhoneCode" 
            :loading="phoneCodeSending"
            block
          >
            發送驗證碼
          </el-button>
        </div>

        <div class="phone-step" v-else>
          <div class="code-input">
            <verification-code-input 
              v-model="phoneForm.code"
              :length="6"
              @complete="verifyPhoneCode"
            />
          </div>
          <div class="resend-area">
            <span>沒收到驗證碼？</span>
            <el-button 
              type="text" 
              @click="sendPhoneCode"
              :disabled="countdown > 0"
            >
              {{ countdown > 0 ? `${countdown}s後重發` : '重新發送' }}
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- MFA設置對話框 -->
    <el-dialog
      v-model="mfaDialogVisible"
      title="雙因子認證設置"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="mfa-setup">
        <div v-if="!user?.mfa_enabled" class="mfa-enable">
          <div class="step-indicator">
            <el-steps :active="mfaStep" simple>
              <el-step title="掃描二維碼" />
              <el-step title="輸入驗證碼" />
              <el-step title="完成設置" />
            </el-steps>
          </div>

          <div class="mfa-content">
            <div v-if="mfaStep === 0" class="qr-step">
              <div class="qr-code">
                <img :src="qrCodeUrl" alt="QR Code" />
              </div>
              <div class="qr-instructions">
                <p>1. 下載並安裝驗證應用（如Google Authenticator）</p>
                <p>2. 打開應用並掃描上方二維碼</p>
                <p>3. 點擊下一步繼續</p>
              </div>
              <el-button type="primary" @click="mfaStep = 1" block>
                下一步
              </el-button>
            </div>

            <div v-else-if="mfaStep === 1" class="verify-step">
              <div class="verify-instructions">
                <p>請輸入驗證應用中顯示的6位數驗證碼：</p>
              </div>
              <verification-code-input 
                v-model="mfaForm.code"
                :length="6"
                @complete="enableMFA"
              />
            </div>

            <div v-else class="success-step">
              <div class="success-icon">
                <el-icon><CircleCheckFilled /></el-icon>
              </div>
              <h3>雙因子認證已啟用！</h3>
              <p>您的賬戶安全性已得到進一步提升。</p>
            </div>
          </div>
        </div>

        <div v-else class="mfa-disable">
          <div class="disable-warning">
            <el-icon><Warning /></el-icon>
            <h3>關閉雙因子認證</h3>
            <p>關閉雙因子認證會降低您賬戶的安全性，請謹慎操作。</p>
          </div>
          
          <el-form label-width="100px">
            <el-form-item label="驗證碼">
              <verification-code-input 
                v-model="mfaForm.code"
                :length="6"
                @complete="disableMFA"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="mfaDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { AuthAPI } from '@/api/auth'
import { dayjs } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PasswordStrengthMeter from '@/components/auth/PasswordStrengthMeter.vue'
import VerificationCodeInput from '@/components/auth/VerificationCodeInput.vue'
import {
  Message,
  Phone,
  Key,
  Lock,
  CircleCheckFilled,
  Warning
} from '@element-plus/icons-vue'

const authStore = useAuthStore()

// 響應式數據
const emailVerifying = ref(false)
const mfaEnabled = ref(false)
const mfaLoading = ref(false)
const historyLoading = ref(false)
const loginHistory = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalHistory = ref(0)

// 密碼修改
const passwordDialogVisible = ref(false)
const passwordChanging = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 手機驗證
const phoneDialogVisible = ref(false)
const phoneStep = ref(1)
const phoneCodeSending = ref(false)
const countdown = ref(0)
const phoneForm = ref({
  phone: '',
  code: ''
})

// MFA設置
const mfaDialogVisible = ref(false)
const mfaStep = ref(0)
const qrCodeUrl = ref('')
const mfaForm = ref({
  code: ''
})

// 安全設置
const securityForm = ref({
  loginNotifications: false,
  twoStepVerification: false
})

// 計算屬性
const user = computed(() => authStore.user)

const securityLevel = computed(() => {
  let score = 0
  if (user.value?.email_verified) score++
  if (user.value?.phone_verified) score++
  if (user.value?.mfa_enabled) score++

  if (score === 3) {
    return { type: 'success', text: '安全等級：高' }
  } else if (score === 2) {
    return { type: 'warning', text: '安全等級：中' }
  } else {
    return { type: 'danger', text: '安全等級：低' }
  }
})

// 密碼驗證規則
const passwordRules: FormRules = {
  currentPassword: [
    { required: true, message: '請輸入當前密碼', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '請輸入新密碼', trigger: 'blur' },
    { min: 8, message: '密碼長度至少8位', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
      message: '密碼必須包含大小寫字母、數字和特殊字符',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '請確認新密碼', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('兩次輸入的密碼不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 方法
const sendEmailVerification = async () => {
  try {
    emailVerifying.value = true
    await authStore.sendEmailVerification()
  } finally {
    emailVerifying.value = false
  }
}

const showPhoneVerification = () => {
  phoneForm.value.phone = user.value?.phone || ''
  phoneStep.value = user.value?.phone ? 2 : 1
  phoneDialogVisible.value = true
}

const sendPhoneCode = async () => {
  try {
    phoneCodeSending.value = true
    await authStore.sendPhoneCode()
    phoneStep.value = 2
    startCountdown()
  } finally {
    phoneCodeSending.value = false
  }
}

const verifyPhoneCode = async () => {
  try {
    await authStore.verifyPhone(phoneForm.value.code)
    phoneDialogVisible.value = false
    phoneForm.value.code = ''
  } catch (error) {
    // 錯誤已在store中處理
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

const toggleMFA = async (enabled: boolean) => {
  if (enabled === user.value?.mfa_enabled) return

  if (enabled) {
    // 啟用MFA
    try {
      const response = await AuthAPI.getMFASetup()
      qrCodeUrl.value = response.qr_code_url
      mfaStep.value = 0
      mfaDialogVisible.value = true
    } catch (error: any) {
      ElMessage.error(error.message || '獲取MFA設置信息失敗')
      mfaEnabled.value = false
    }
  } else {
    // 禁用MFA
    mfaDialogVisible.value = true
  }
}

const enableMFA = async () => {
  try {
    mfaLoading.value = true
    await authStore.toggleMFA(true, mfaForm.value.code)
    mfaStep.value = 2
    setTimeout(() => {
      mfaDialogVisible.value = false
      mfaStep.value = 0
      mfaForm.value.code = ''
    }, 2000)
  } finally {
    mfaLoading.value = false
  }
}

const disableMFA = async () => {
  try {
    mfaLoading.value = true
    await authStore.toggleMFA(false, mfaForm.value.code)
    mfaDialogVisible.value = false
    mfaForm.value.code = ''
  } finally {
    mfaLoading.value = false
  }
}

const showChangePassword = () => {
  passwordDialogVisible.value = true
}

const changePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return

    passwordChanging.value = true
    await authStore.changePassword(passwordForm.value)
    passwordDialogVisible.value = false
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } finally {
    passwordChanging.value = false
  }
}

const updateSecuritySettings = async () => {
  try {
    await AuthAPI.updateSecuritySettings(securityForm.value)
    ElMessage.success('安全設置已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新安全設置失敗')
  }
}

const loadLoginHistory = async (page = 1) => {
  try {
    historyLoading.value = true
    const response = await AuthAPI.getLoginHistory(page, pageSize.value)
    loginHistory.value = response.data
    totalHistory.value = response.pagination.total_items
  } catch (error: any) {
    ElMessage.error(error.message || '載入登錄記錄失敗')
  } finally {
    historyLoading.value = false
  }
}

const loadSecuritySettings = async () => {
  try {
    const settings = await AuthAPI.getSecuritySettings()
    securityForm.value = {
      loginNotifications: settings.login_notifications,
      twoStepVerification: settings.two_step_verification
    }
  } catch (error: any) {
    ElMessage.error(error.message || '載入安全設置失敗')
  }
}

// 初始化
onMounted(() => {
  mfaEnabled.value = user.value?.mfa_enabled || false
  loadLoginHistory()
  loadSecuritySettings()
})
</script>

<style scoped lang="scss">
.security-view {
  max-width: 1000px;
  margin: 0 auto;
  
  .el-card {
    margin-bottom: 24px;
  }
}

.security-overview {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }
}

.security-items {
  .security-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:last-child {
      border-bottom: none;
    }

    .item-info {
      display: flex;
      align-items: center;
      gap: 16px;

      .item-icon {
        width: 40px;
        height: 40px;
        border-radius: 8px;
        background: var(--el-fill-color-light);
        color: var(--el-text-color-secondary);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;

        &.verified {
          background: var(--el-color-success-light-9);
          color: var(--el-color-success);
        }
      }

      .item-content {
        .item-title {
          font-size: 16px;
          font-weight: 500;
          color: var(--el-text-color-primary);
          margin-bottom: 4px;
        }

        .item-desc {
          font-size: 14px;
          color: var(--el-text-color-regular);
        }
      }
    }
  }
}

.login-history {
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .pagination {
    margin-top: 16px;
    text-align: center;
  }
}

.security-settings {
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .form-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
}

.phone-verification {
  .phone-step {
    margin: 20px 0;
  }

  .code-input {
    text-align: center;
    margin: 20px 0;
  }

  .resend-area {
    text-align: center;
    font-size: 14px;
    color: var(--el-text-color-regular);

    .el-button {
      padding: 0;
      margin-left: 8px;
    }
  }
}

.mfa-setup {
  .step-indicator {
    margin-bottom: 24px;
  }

  .qr-step {
    text-align: center;

    .qr-code {
      margin: 20px 0;
      
      img {
        width: 200px;
        height: 200px;
        border: 1px solid var(--el-border-color);
      }
    }

    .qr-instructions {
      text-align: left;
      margin: 20px 0;
      
      p {
        margin: 8px 0;
        color: var(--el-text-color-regular);
      }
    }
  }

  .verify-step {
    text-align: center;

    .verify-instructions {
      margin: 20px 0;
      
      p {
        color: var(--el-text-color-regular);
      }
    }
  }

  .success-step {
    text-align: center;
    padding: 20px 0;

    .success-icon {
      font-size: 48px;
      color: var(--el-color-success);
      margin-bottom: 16px;
    }

    h3 {
      color: var(--el-color-success);
      margin: 16px 0 8px 0;
    }

    p {
      color: var(--el-text-color-regular);
    }
  }

  .mfa-disable {
    .disable-warning {
      text-align: center;
      padding: 20px 0;

      .el-icon {
        font-size: 48px;
        color: var(--el-color-warning);
        margin-bottom: 16px;
      }

      h3 {
        color: var(--el-color-warning);
        margin: 16px 0 8px 0;
      }

      p {
        color: var(--el-text-color-regular);
      }
    }
  }
}

@media (max-width: 768px) {
  .security-items .security-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>