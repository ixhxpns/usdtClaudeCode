<template>
  <div class="kyc-view">
    <!-- KYC狀態卡片 -->
    <div class="status-section">
      <div class="status-card" :class="kycStatus.toLowerCase()">
        <div class="status-header">
          <div class="status-icon">
            <i :class="getStatusIcon()"></i>
          </div>
          <div class="status-info">
            <h2>{{ getStatusTitle() }}</h2>
            <p>{{ getStatusDescription() }}</p>
          </div>
        </div>
        
        <div v-if="kycData.level" class="kyc-level">
          <span class="level-badge">{{ getLevelText() }}</span>
          <div class="level-benefits">
            <div class="benefit-item">
              <span>日提現額度</span>
              <span>{{ formatPrice(getLevelLimits().dailyWithdraw) }}</span>
            </div>
            <div class="benefit-item">
              <span>月提現額度</span>
              <span>{{ formatPrice(getLevelLimits().monthlyWithdraw) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- KYC步驟 -->
    <div class="steps-section">
      <div class="steps-header">
        <h3>身份驗證流程</h3>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
      </div>
      
      <div class="steps-list">
        <div 
          v-for="(step, index) in kycSteps" 
          :key="step.key"
          :class="['step-item', getStepStatus(step.key)]"
        >
          <div class="step-number">
            <span v-if="getStepStatus(step.key) === 'completed'">✓</span>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="step-content">
            <h4>{{ step.title }}</h4>
            <p>{{ step.description }}</p>
            <div v-if="step.requirements" class="step-requirements">
              <span class="requirements-label">所需材料：</span>
              <span class="requirements-text">{{ step.requirements.join('、') }}</span>
            </div>
          </div>
          <div class="step-action">
            <button 
              v-if="canProcessStep(step.key)"
              :class="['action-btn', getStepStatus(step.key)]"
              @click="handleStepAction(step.key)"
            >
              {{ getStepActionText(step.key) }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- KYC表單 -->
    <div v-if="showKycForm" class="form-section">
      <div class="form-card">
        <div class="form-header">
          <h3>{{ getCurrentStepTitle() }}</h3>
          <p>請準確填寫以下信息，確保與身份證件一致</p>
        </div>
        
        <!-- 基本信息表單 -->
        <div v-if="currentStep === 'basic'" class="basic-form">
          <div class="form-grid">
            <div class="form-group">
              <label>真實姓名 *</label>
              <input 
                v-model="basicForm.realName" 
                type="text" 
                placeholder="請輸入身份證上的真實姓名"
                :readonly="isReadonly"
              >
            </div>
            
            <div class="form-group">
              <label>性別 *</label>
              <select v-model="basicForm.gender" :disabled="isReadonly">
                <option value="">請選擇性別</option>
                <option value="MALE">男</option>
                <option value="FEMALE">女</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>出生日期 *</label>
              <input 
                v-model="basicForm.birthDate" 
                type="date" 
                :readonly="isReadonly"
              >
            </div>
            
            <div class="form-group">
              <label>身份證號 *</label>
              <input 
                v-model="basicForm.idNumber" 
                type="text" 
                placeholder="請輸入身份證號碼"
                :readonly="isReadonly"
              >
            </div>
            
            <div class="form-group">
              <label>聯絡電話 *</label>
              <input 
                v-model="basicForm.phone" 
                type="tel" 
                placeholder="請輸入手機號碼"
                :readonly="isReadonly"
              >
            </div>
            
            <div class="form-group full-width">
              <label>居住地址 *</label>
              <input 
                v-model="basicForm.address" 
                type="text" 
                placeholder="請輸入詳細居住地址"
                :readonly="isReadonly"
              >
            </div>
          </div>
          
          <div v-if="!isReadonly" class="form-actions">
            <button class="submit-btn" @click="submitBasicInfo" :disabled="!canSubmitBasic">
              {{ submitting ? '提交中...' : '保存並繼續' }}
            </button>
          </div>
        </div>
        
        <!-- 文件上傳表單 -->
        <div v-if="currentStep === 'documents'" class="documents-form">
          <div class="upload-grid">
            <!-- 身份證正面 -->
            <div class="upload-item">
              <label>身份證正面 *</label>
              <div class="upload-area" @click="triggerFileInput('idFront')">
                <div v-if="!documents.idFront" class="upload-placeholder">
                  <i class="icon-upload"></i>
                  <p>點擊上傳身份證正面</p>
                  <span class="upload-hint">支持 JPG、PNG，大小不超過5MB</span>
                </div>
                <div v-else class="upload-preview">
                  <img :src="documents.idFront.preview" alt="身份證正面">
                  <div class="upload-overlay">
                    <button @click.stop="removeDocument('idFront')">重新上傳</button>
                  </div>
                </div>
              </div>
              <input 
                ref="idFrontInput" 
                type="file" 
                accept="image/*" 
                @change="handleFileSelect('idFront', $event)"
                hidden
              >
            </div>
            
            <!-- 身份證背面 -->
            <div class="upload-item">
              <label>身份證背面 *</label>
              <div class="upload-area" @click="triggerFileInput('idBack')">
                <div v-if="!documents.idBack" class="upload-placeholder">
                  <i class="icon-upload"></i>
                  <p>點擊上傳身份證背面</p>
                  <span class="upload-hint">支持 JPG、PNG，大小不超過5MB</span>
                </div>
                <div v-else class="upload-preview">
                  <img :src="documents.idBack.preview" alt="身份證背面">
                  <div class="upload-overlay">
                    <button @click.stop="removeDocument('idBack')">重新上傳</button>
                  </div>
                </div>
              </div>
              <input 
                ref="idBackInput" 
                type="file" 
                accept="image/*" 
                @change="handleFileSelect('idBack', $event)"
                hidden
              >
            </div>
            
            <!-- 手持身份證自拍 -->
            <div class="upload-item full-width">
              <label>手持身份證自拍 *</label>
              <div class="upload-area" @click="triggerFileInput('selfie')">
                <div v-if="!documents.selfie" class="upload-placeholder">
                  <i class="icon-upload"></i>
                  <p>點擊上傳手持身份證自拍照</p>
                  <span class="upload-hint">請清晰拍攝本人手持身份證的照片</span>
                </div>
                <div v-else class="upload-preview">
                  <img :src="documents.selfie.preview" alt="手持身份證自拍">
                  <div class="upload-overlay">
                    <button @click.stop="removeDocument('selfie')">重新上傳</button>
                  </div>
                </div>
              </div>
              <input 
                ref="selfieInput" 
                type="file" 
                accept="image/*" 
                @change="handleFileSelect('selfie', $event)"
                hidden
              >
            </div>
          </div>
          
          <div class="upload-tips">
            <h4>拍照要求</h4>
            <ul>
              <li>照片清晰，光線充足，無反光</li>
              <li>身份證件完整顯示，四個角不能缺失</li>
              <li>手持身份證自拍需要清楚顯示人臉和身份證信息</li>
              <li>文件大小不超過5MB，支持JPG、PNG格式</li>
            </ul>
          </div>
          
          <div class="form-actions">
            <button class="submit-btn" @click="submitDocuments" :disabled="!canSubmitDocuments">
              {{ uploading ? '上傳中...' : '提交文件' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- KYC等級說明 -->
    <div class="levels-section">
      <div class="levels-header">
        <h3>KYC等級說明</h3>
        <p>完成不同等級的身份驗證，可享受相應的服務權益</p>
      </div>
      
      <div class="levels-grid">
        <div 
          v-for="level in kycLevels" 
          :key="level.level"
          :class="['level-card', { current: kycData.level === level.level }]"
        >
          <div class="level-header">
            <span class="level-number">{{ level.level }}</span>
            <h4>{{ level.name }}</h4>
          </div>
          
          <div class="level-description">
            <p>{{ level.description }}</p>
          </div>
          
          <div class="level-benefits">
            <div class="benefit">
              <span class="label">日提現額度</span>
              <span class="value">{{ formatPrice(level.dailyWithdraw) }}</span>
            </div>
            <div class="benefit">
              <span class="label">月提現額度</span>
              <span class="value">{{ formatPrice(level.monthlyWithdraw) }}</span>
            </div>
          </div>
          
          <div class="level-requirements">
            <span class="requirements-label">認證要求</span>
            <ul>
              <li v-for="req in level.requirements" :key="req">{{ req }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '@/utils/http'

export default {
  name: 'KycView',
  setup() {
    // 響應式數據
    const kycStatus = ref('NOT_STARTED')
    const kycData = reactive({})
    const showKycForm = ref(false)
    const currentStep = ref('basic')
    const submitting = ref(false)
    const uploading = ref(false)
    
    const basicForm = reactive({
      realName: '',
      gender: '',
      birthDate: '',
      idNumber: '',
      phone: '',
      address: ''
    })
    
    const documents = reactive({
      idFront: null,
      idBack: null,
      selfie: null
    })
    
    // 計算屬性
    const progressPercent = computed(() => {
      const completedSteps = kycSteps.filter(step => getStepStatus(step.key) === 'completed').length
      return Math.round((completedSteps / kycSteps.length) * 100)
    })
    
    const isReadonly = computed(() => {
      return ['PENDING', 'APPROVED'].includes(kycStatus.value)
    })
    
    const canSubmitBasic = computed(() => {
      return basicForm.realName && 
             basicForm.gender && 
             basicForm.birthDate && 
             basicForm.idNumber && 
             basicForm.phone && 
             basicForm.address && 
             !submitting.value
    })
    
    const canSubmitDocuments = computed(() => {
      return documents.idFront && 
             documents.idBack && 
             documents.selfie && 
             !uploading.value
    })
    
    // 靜態數據
    const kycSteps = [
      {
        key: 'basic',
        title: '基本信息',
        description: '填寫真實姓名、身份證號等基本信息',
        requirements: ['身份證', '手機號碼']
      },
      {
        key: 'documents',
        title: '身份證件',
        description: '上傳身份證正反面及手持身份證自拍照',
        requirements: ['身份證正面', '身份證背面', '手持身份證自拍']
      },
      {
        key: 'review',
        title: '人工審核',
        description: '我們會在1-3個工作日內完成審核',
        requirements: null
      }
    ]
    
    const kycLevels = [
      {
        level: 1,
        name: '基礎認證',
        description: '完成身份基本信息驗證',
        dailyWithdraw: 10000,
        monthlyWithdraw: 100000,
        requirements: ['身份證正反面', '手持身份證自拍']
      },
      {
        level: 2,
        name: '進階認證', 
        description: '銀行賬戶和地址驗證',
        dailyWithdraw: 100000,
        monthlyWithdraw: 1000000,
        requirements: ['基礎認證', '銀行賬戶信息', '地址證明']
      },
      {
        level: 3,
        name: '專業認證',
        description: '收入來源和資金來源驗證',
        dailyWithdraw: 500000,
        monthlyWithdraw: 5000000,
        requirements: ['進階認證', '收入證明', '資金來源說明']
      }
    ]
    
    // 方法
    const formatPrice = (price) => {
      return new Intl.NumberFormat('zh-TW', {
        style: 'currency',
        currency: 'TWD',
        minimumFractionDigits: 0
      }).format(price || 0)
    }
    
    const getStatusIcon = () => {
      const icons = {
        'NOT_STARTED': 'icon-info',
        'PENDING': 'icon-clock',
        'UNDER_REVIEW': 'icon-review',
        'APPROVED': 'icon-check-circle',
        'REJECTED': 'icon-x-circle',
        'REQUIRES_SUPPLEMENT': 'icon-alert'
      }
      return icons[kycStatus.value] || 'icon-info'
    }
    
    const getStatusTitle = () => {
      const titles = {
        'NOT_STARTED': '開始身份驗證',
        'PENDING': '等待審核',
        'UNDER_REVIEW': '審核中',
        'APPROVED': '驗證通過',
        'REJECTED': '驗證失敗',
        'REQUIRES_SUPPLEMENT': '需要補充材料'
      }
      return titles[kycStatus.value] || '身份驗證'
    }
    
    const getStatusDescription = () => {
      const descriptions = {
        'NOT_STARTED': '完成身份驗證，提升交易額度',
        'PENDING': '您的資料已提交，請等待審核',
        'UNDER_REVIEW': '我們正在審核您的資料，請耐心等待',
        'APPROVED': '您的身份已成功驗證，可以正常交易',
        'REJECTED': '您的驗證材料未通過審核，請重新提交',
        'REQUIRES_SUPPLEMENT': '請根據要求補充相關材料'
      }
      return descriptions[kycStatus.value] || ''
    }
    
    const getLevelText = () => {
      const level = kycData.level || 1
      const levelData = kycLevels.find(l => l.level === level)
      return levelData ? `等級 ${level} - ${levelData.name}` : '等級 1'
    }
    
    const getLevelLimits = () => {
      const level = kycData.level || 1
      return kycLevels.find(l => l.level === level) || kycLevels[0]
    }
    
    const getStepStatus = (stepKey) => {
      switch (stepKey) {
        case 'basic':
          return kycData.basicCompleted ? 'completed' : (kycStatus.value === 'NOT_STARTED' ? 'active' : 'pending')
        case 'documents':
          return kycData.documentsCompleted ? 'completed' : (kycData.basicCompleted ? 'active' : 'pending')
        case 'review':
          return kycStatus.value === 'APPROVED' ? 'completed' : 
                 (['PENDING', 'UNDER_REVIEW'].includes(kycStatus.value) ? 'active' : 'pending')
        default:
          return 'pending'
      }
    }
    
    const canProcessStep = (stepKey) => {
      return getStepStatus(stepKey) === 'active' || 
             (stepKey === 'basic' && kycStatus.value === 'REQUIRES_SUPPLEMENT')
    }
    
    const getStepActionText = (stepKey) => {
      const status = getStepStatus(stepKey)
      if (status === 'completed') return '已完成'
      if (status === 'active') {
        return stepKey === 'basic' ? '填寫信息' : 
               stepKey === 'documents' ? '上傳文件' : '審核中'
      }
      return '等待中'
    }
    
    const getCurrentStepTitle = () => {
      const step = kycSteps.find(s => s.key === currentStep.value)
      return step ? step.title : ''
    }
    
    const handleStepAction = (stepKey) => {
      if (stepKey === 'basic' || stepKey === 'documents') {
        currentStep.value = stepKey
        showKycForm.value = true
        loadKycData()
      }
    }
    
    // 文件處理
    const triggerFileInput = (type) => {
      const input = document.querySelector(`input[ref="${type}Input"]`)
      if (input) input.click()
    }
    
    const handleFileSelect = (type, event) => {
      const file = event.target.files[0]
      if (!file) return
      
      // 驗證文件
      if (!validateFile(file)) return
      
      // 創建預覽
      const reader = new FileReader()
      reader.onload = (e) => {
        documents[type] = {
          file: file,
          preview: e.target.result
        }
      }
      reader.readAsDataURL(file)
    }
    
    const removeDocument = (type) => {
      documents[type] = null
      // 清除文件輸入
      const input = document.querySelector(`input[ref="${type}Input"]`)
      if (input) input.value = ''
    }
    
    const validateFile = (file) => {
      // 檢查文件類型
      if (!file.type.startsWith('image/')) {
        alert('請選擇圖片文件')
        return false
      }
      
      // 檢查文件大小 (5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('文件大小不能超過5MB')
        return false
      }
      
      return true
    }
    
    // API 調用
    const loadKycStatus = async () => {
      try {
        const response = await api.get('/kyc/status')
        if (response.data.success) {
          const data = response.data.data
          kycStatus.value = data.status || 'NOT_STARTED'
          Object.assign(kycData, data)
        }
      } catch (error) {
        console.error('獲取KYC狀態失敗:', error)
      }
    }
    
    const loadKycData = async () => {
      if (kycData.basicCompleted) {
        Object.assign(basicForm, kycData.basicInfo || {})
      }
    }
    
    const submitBasicInfo = async () => {
      if (!canSubmitBasic.value) return
      
      submitting.value = true
      try {
        const response = await api.post('/kyc/basic', basicForm)
        
        if (response.data.success) {
          kycData.basicCompleted = true
          showKycForm.value = false
          loadKycStatus() // 重新加載狀態
          alert('基本信息提交成功')
        } else {
          alert('提交失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('提交基本信息失敗:', error)
        alert('提交失敗，請稍後重試')
      } finally {
        submitting.value = false
      }
    }
    
    const submitDocuments = async () => {
      if (!canSubmitDocuments.value) return
      
      uploading.value = true
      try {
        const formData = new FormData()
        formData.append('frontImage', documents.idFront.file)
        formData.append('backImage', documents.idBack.file)
        formData.append('selfieImage', documents.selfie.file)
        
        const response = await api.post('/kyc/upload-id', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (response.data.success) {
          kycData.documentsCompleted = true
          showKycForm.value = false
          loadKycStatus() // 重新加載狀態
          alert('文件上傳成功，請等待審核')
        } else {
          alert('上傳失敗: ' + response.data.message)
        }
      } catch (error) {
        console.error('上傳文件失敗:', error)
        alert('上傳失敗，請稍後重試')
      } finally {
        uploading.value = false
      }
    }
    
    // 生命週期
    onMounted(() => {
      loadKycStatus()
    })
    
    return {
      kycStatus,
      kycData,
      showKycForm,
      currentStep,
      submitting,
      uploading,
      basicForm,
      documents,
      progressPercent,
      isReadonly,
      canSubmitBasic,
      canSubmitDocuments,
      kycSteps,
      kycLevels,
      formatPrice,
      getStatusIcon,
      getStatusTitle,
      getStatusDescription,
      getLevelText,
      getLevelLimits,
      getStepStatus,
      canProcessStep,
      getStepActionText,
      getCurrentStepTitle,
      handleStepAction,
      triggerFileInput,
      handleFileSelect,
      removeDocument,
      submitBasicInfo,
      submitDocuments
    }
  }
}
</script>

<style scoped>
.kyc-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.status-section {
  margin-bottom: 32px;
}

.status-card {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #6b7280;
}

.status-card.not_started {
  border-left-color: #6b7280;
}

.status-card.pending {
  border-left-color: #f59e0b;
}

.status-card.approved {
  border-left-color: #10b981;
}

.status-card.rejected {
  border-left-color: #ef4444;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.status-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 32px;
}

.status-info h2 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 24px;
}

.status-info p {
  margin: 0;
  color: #6b7280;
  font-size: 16px;
}

.kyc-level {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f9fafb;
  padding: 20px;
  border-radius: 8px;
}

.level-badge {
  background: #2563eb;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 500;
}

.level-benefits {
  display: flex;
  gap: 32px;
}

.benefit-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.benefit-item span:first-child {
  font-size: 14px;
  color: #6b7280;
}

.benefit-item span:last-child {
  font-weight: 600;
  color: #1a1a1a;
}

.steps-section {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 32px;
}

.steps-header {
  margin-bottom: 32px;
}

.steps-header h3 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #10b981;
  transition: width 0.3s ease;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.step-item.completed {
  background: #f0fdf4;
  border-color: #10b981;
}

.step-item.active {
  background: #eff6ff;
  border-color: #2563eb;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  color: #6b7280;
  font-weight: 600;
  flex-shrink: 0;
}

.step-item.completed .step-number {
  background: #10b981;
  color: white;
}

.step-item.active .step-number {
  background: #2563eb;
  color: white;
}

.step-content {
  flex: 1;
}

.step-content h4 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.step-content p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.step-requirements {
  margin-top: 8px;
  font-size: 12px;
}

.requirements-label {
  color: #6b7280;
}

.requirements-text {
  color: #4b5563;
}

.step-action {
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #374151;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f9fafb;
}

.action-btn.active {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}

.action-btn.completed {
  background: #10b981;
  color: white;
  border-color: #10b981;
  cursor: default;
}

.form-section {
  margin-bottom: 32px;
}

.form-card {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-header {
  margin-bottom: 32px;
  text-align: center;
}

.form-header h3 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.form-header p {
  margin: 0;
  color: #6b7280;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 32px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.form-group input,
.form-group select {
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 16px;
}

.form-group input:read-only {
  background: #f9fafb;
  color: #6b7280;
}

.upload-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.upload-item.full-width {
  grid-column: 1 / -1;
}

.upload-item label {
  display: block;
  margin-bottom: 12px;
  font-weight: 500;
  color: #374151;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area:hover {
  border-color: #2563eb;
  background: #f8fafc;
}

.upload-placeholder i {
  font-size: 32px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.upload-placeholder p {
  margin: 0 0 4px 0;
  color: #4b5563;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #9ca3af;
}

.upload-preview {
  position: relative;
  width: 100%;
  height: 100%;
}

.upload-preview img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 6px;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 6px;
}

.upload-preview:hover .upload-overlay {
  opacity: 1;
}

.upload-overlay button {
  padding: 8px 16px;
  background: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.upload-tips {
  background: #fef3c7;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #f59e0b;
  margin-bottom: 24px;
}

.upload-tips h4 {
  margin: 0 0 12px 0;
  color: #92400e;
}

.upload-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #92400e;
}

.upload-tips li {
  margin-bottom: 4px;
}

.form-actions {
  text-align: center;
}

.submit-btn {
  padding: 16px 32px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.levels-section {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.levels-header {
  text-align: center;
  margin-bottom: 32px;
}

.levels-header h3 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
}

.levels-header p {
  margin: 0;
  color: #6b7280;
}

.levels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.level-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
  transition: all 0.2s;
}

.level-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.level-card.current {
  border-color: #2563eb;
  background: #eff6ff;
}

.level-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.level-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #2563eb;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.level-header h4 {
  margin: 0;
  color: #1a1a1a;
}

.level-description {
  margin-bottom: 20px;
}

.level-description p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.level-benefits {
  margin-bottom: 20px;
}

.benefit {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.benefit .label {
  color: #6b7280;
  font-size: 14px;
}

.benefit .value {
  font-weight: 600;
  color: #1a1a1a;
}

.level-requirements .requirements-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}

.level-requirements ul {
  margin: 0;
  padding-left: 20px;
  color: #6b7280;
  font-size: 14px;
}

.level-requirements li {
  margin-bottom: 4px;
}

@media (max-width: 768px) {
  .kyc-view {
    padding: 16px;
  }
  
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .upload-grid {
    grid-template-columns: 1fr;
  }
  
  .levels-grid {
    grid-template-columns: 1fr;
  }
  
  .level-benefits {
    flex-direction: column;
    gap: 16px;
  }
}
</style>