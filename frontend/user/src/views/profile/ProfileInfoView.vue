<template>
  <div class="profile-info-view">
    <el-card class="profile-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>個人信息</h3>
          <el-button type="primary" @click="editMode = !editMode">
            <el-icon><Edit /></el-icon>
            {{ editMode ? '取消編輯' : '編輯資料' }}
          </el-button>
        </div>
      </template>

      <div class="profile-content">
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :on-success="handleAvatarSuccess"
            :disabled="!editMode"
          >
            <el-avatar :size="120" :src="form.avatar" class="avatar">
              <el-icon v-if="!form.avatar"><User /></el-icon>
            </el-avatar>
            <div v-if="editMode" class="avatar-overlay">
              <el-icon><Camera /></el-icon>
              <span>更換頭像</span>
            </div>
          </el-upload>
        </div>

        <div class="profile-form">
          <el-form
            ref="profileFormRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            :disabled="!editMode"
          >
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="用戶名" prop="username">
                  <el-input 
                    v-model="form.username" 
                    :disabled="true"
                    placeholder="用戶名不可修改"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="電子郵箱" prop="email">
                  <div class="input-with-status">
                    <el-input 
                      v-model="form.email" 
                      :disabled="true"
                      placeholder="電子郵箱"
                    />
                    <el-tag 
                      :type="user?.email_verified ? 'success' : 'warning'"
                      size="small"
                      class="status-tag"
                    >
                      {{ user?.email_verified ? '已驗證' : '未驗證' }}
                    </el-tag>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="真實姓名" prop="realName">
                  <el-input 
                    v-model="form.realName" 
                    placeholder="請輸入真實姓名"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手機號碼" prop="phone">
                  <div class="input-with-status">
                    <el-input 
                      v-model="form.phone" 
                      placeholder="請輸入手機號碼"
                    />
                    <el-tag 
                      :type="user?.phone_verified ? 'success' : 'warning'"
                      size="small"
                      class="status-tag"
                    >
                      {{ user?.phone_verified ? '已驗證' : '未驗證' }}
                    </el-tag>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="出生日期" prop="birthDate">
                  <el-date-picker
                    v-model="form.birthDate"
                    type="date"
                    placeholder="選擇出生日期"
                    style="width: 100%"
                    :disabled-date="disabledDate"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性別" prop="gender">
                  <el-radio-group v-model="form.gender">
                    <el-radio label="MALE">男</el-radio>
                    <el-radio label="FEMALE">女</el-radio>
                    <el-radio label="OTHER">其他</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="地址" prop="address">
                  <el-input 
                    v-model="form.address" 
                    type="textarea"
                    :rows="3"
                    placeholder="請輸入詳細地址"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="職業" prop="occupation">
                  <el-input 
                    v-model="form.occupation" 
                    placeholder="請輸入職業"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="年收入" prop="annualIncome">
                  <el-select 
                    v-model="form.annualIncome" 
                    placeholder="請選擇年收入範圍"
                    style="width: 100%"
                  >
                    <el-option label="10萬以下" value="BELOW_100K" />
                    <el-option label="10-50萬" value="100K_500K" />
                    <el-option label="50-100萬" value="500K_1M" />
                    <el-option label="100萬以上" value="ABOVE_1M" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <div v-if="editMode" class="form-actions">
              <el-button @click="editMode = false">取消</el-button>
              <el-button 
                type="primary" 
                @click="saveProfile" 
                :loading="loading"
              >
                保存更改
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </el-card>

    <!-- 賬戶統計卡片 -->
    <el-row :gutter="24" class="stats-section">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon">
              <el-icon><Wallet /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">賬戶餘額</div>
              <div class="stat-value">${{ accountBalance }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">交易次數</div>
              <div class="stat-value">{{ tradingCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">註冊時間</div>
              <div class="stat-value">{{ registrationDate }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-card class="actions-card" shadow="never">
      <template #header>
        <h3>快速操作</h3>
      </template>
      <div class="quick-actions">
        <el-button 
          v-if="!user?.email_verified" 
          type="warning" 
          @click="sendEmailVerification"
          :loading="emailVerifying"
        >
          <el-icon><Message /></el-icon>
          驗證郵箱
        </el-button>
        <el-button 
          v-if="!user?.phone_verified" 
          type="warning" 
          @click="verifyPhone"
        >
          <el-icon><Phone /></el-icon>
          驗證手機
        </el-button>
        <el-button type="primary" @click="goToKyc">
          <el-icon><CreditCard /></el-icon>
          完成KYC認證
        </el-button>
        <el-button @click="goToSecurity">
          <el-icon><Lock /></el-icon>
          安全設置
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/auth'
import { dayjs } from 'element-plus'
import type { FormInstance, FormRules, UploadProps } from 'element-plus'
import {
  Edit,
  User,
  Camera,
  Wallet,
  TrendCharts,
  Calendar,
  Message,
  Phone,
  CreditCard,
  Lock
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 響應式數據
const profileFormRef = ref<FormInstance>()
const editMode = ref(false)
const loading = ref(false)
const emailVerifying = ref(false)
const accountBalance = ref('0.00')
const tradingCount = ref(0)

// 表單數據
const form = ref({
  avatar: '',
  username: '',
  email: '',
  realName: '',
  phone: '',
  birthDate: '',
  gender: 'MALE',
  address: '',
  occupation: '',
  annualIncome: ''
})

// 計算屬性
const user = computed(() => authStore.user)
const uploadUrl = computed(() => `${import.meta.env.VITE_API_BASE_URL}/users/upload-avatar`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${getToken()}`
}))
const registrationDate = computed(() => {
  return user.value?.created_at ? dayjs(user.value.created_at).format('YYYY-MM-DD') : '--'
})

// 表單驗證規則
const rules: FormRules = {
  realName: [
    { required: true, message: '請輸入真實姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名長度應在2-50個字符之間', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '請輸入有效的手機號碼', trigger: 'blur' }
  ]
}

// 方法
const disabledDate = (time: Date) => {
  // 禁用未來日期和過於久遠的日期
  return time.getTime() > Date.now() || time.getTime() < new Date('1900-01-01').getTime()
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('頭像只能是 JPG 或 PNG 格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('頭像大小不能超過 2MB!')
    return false
  }
  return true
}

const handleAvatarSuccess: UploadProps['onSuccess'] = (response) => {
  form.value.avatar = response.url
  ElMessage.success('頭像上傳成功')
}

const saveProfile = async () => {
  if (!profileFormRef.value) return

  try {
    const valid = await profileFormRef.value.validate()
    if (!valid) return

    loading.value = true
    
    // 調用API保存用戶資料
    // await UserAPI.updateProfile(form.value)
    
    await authStore.refreshUserInfo()
    editMode.value = false
    ElMessage.success('個人資料更新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失敗')
  } finally {
    loading.value = false
  }
}

const sendEmailVerification = async () => {
  try {
    emailVerifying.value = true
    await authStore.sendEmailVerification()
  } catch (error) {
    // 錯誤已在store中處理
  } finally {
    emailVerifying.value = false
  }
}

const verifyPhone = async () => {
  try {
    await ElMessageBox.prompt('請輸入手機驗證碼', '手機驗證', {
      confirmButtonText: '驗證',
      cancelButtonText: '取消',
      inputPattern: /^\d{6}$/,
      inputErrorMessage: '請輸入6位數字驗證碼'
    })
    // 在實際項目中，這裡應該先發送驗證碼
    // const { value } = result
    // await authStore.verifyPhone(value)
  } catch (error) {
    console.error('手機驗證失敗:', error)
  }
}

const goToKyc = () => {
  router.push('/kyc/status')
}

const goToSecurity = () => {
  router.push('/profile/security')
}

const loadUserProfile = async () => {
  if (user.value) {
    form.value = {
      avatar: user.value.avatar || '',
      username: user.value.username || '',
      email: user.value.email || '',
      realName: user.value.real_name || '',
      phone: user.value.phone || '',
      birthDate: user.value.birth_date || '',
      gender: user.value.gender || 'MALE',
      address: user.value.address || '',
      occupation: user.value.occupation || '',
      annualIncome: user.value.annual_income || ''
    }
  }
}

const loadAccountStats = async () => {
  try {
    // 載入賬戶統計數據
    // const stats = await UserAPI.getAccountStats()
    // accountBalance.value = stats.balance
    // tradingCount.value = stats.tradingCount
  } catch (error) {
    console.error('載入賬戶統計失敗:', error)
  }
}

// 生命週期
onMounted(() => {
  loadUserProfile()
  loadAccountStats()
})
</script>

<style scoped lang="scss">
.profile-info-view {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 24px;
  
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

.profile-content {
  display: flex;
  gap: 32px;

  .avatar-section {
    flex-shrink: 0;
    text-align: center;

    .avatar-uploader {
      position: relative;
      display: inline-block;

      .avatar {
        border: 2px solid var(--el-border-color);
        transition: all 0.3s ease;

        &:hover {
          border-color: var(--el-color-primary);
        }
      }

      .avatar-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.6);
        border-radius: 50%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: white;
        opacity: 0;
        transition: opacity 0.3s ease;
        cursor: pointer;

        &:hover {
          opacity: 1;
        }

        span {
          font-size: 12px;
          margin-top: 4px;
        }
      }
    }
  }

  .profile-form {
    flex: 1;

    .input-with-status {
      display: flex;
      align-items: center;
      gap: 8px;

      .el-input {
        flex: 1;
      }

      .status-tag {
        flex-shrink: 0;
      }
    }
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.stats-section {
  margin-bottom: 24px;

  .stat-card {
    .stat-item {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        background: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
      }

      .stat-content {
        flex: 1;

        .stat-label {
          font-size: 14px;
          color: var(--el-text-color-regular);
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 20px;
          font-weight: 600;
          color: var(--el-text-color-primary);
        }
      }
    }
  }
}

.actions-card {
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .quick-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .profile-content {
    flex-direction: column;
    gap: 24px;
  }

  .stats-section {
    .el-col {
      margin-bottom: 16px;
    }
  }

  .quick-actions {
    flex-direction: column;

    .el-button {
      justify-content: flex-start;
    }
  }
}
</style>