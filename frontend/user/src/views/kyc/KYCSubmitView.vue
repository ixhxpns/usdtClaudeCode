<template>
  <div class="kyc-submit-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">提交 KYC 验证</h1>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
        完成身份验证以使用完整的交易功能
      </p>
    </div>

    <!-- 进度指示器 -->
    <div class="mt-6">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="个人信息" description="填写基本信息" />
        <el-step title="身份证明" description="上传身份证件" />
        <el-step title="人脸识别" description="完成人脸验证" />
        <el-step title="提交审核" description="等待审核结果" />
      </el-steps>
    </div>

    <!-- 表单内容 -->
    <div class="mt-8">
      <el-card>
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="120px"
          label-position="left"
          size="large"
        >
          <!-- 步骤1：个人信息 -->
          <div v-if="currentStep === 0" class="form-step">
            <h2 class="step-title">个人基本信息</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <el-form-item label="姓名" prop="fullName">
                <el-input
                  v-model="formData.fullName"
                  placeholder="请输入真实姓名"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="性别" prop="gender">
                <el-select v-model="formData.gender" placeholder="请选择性别">
                  <el-option label="男" value="male" />
                  <el-option label="女" value="female" />
                  <el-option label="其他" value="other" />
                </el-select>
              </el-form-item>

              <el-form-item label="出生日期" prop="dateOfBirth">
                <el-date-picker
                  v-model="formData.dateOfBirth"
                  type="date"
                  placeholder="请选择出生日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDate"
                  class="w-full"
                />
              </el-form-item>

              <el-form-item label="国籍" prop="nationality">
                <el-select 
                  v-model="formData.nationality" 
                  placeholder="请选择国籍"
                  filterable
                >
                  <el-option 
                    v-for="country in countries" 
                    :key="country.code"
                    :label="country.name" 
                    :value="country.code" 
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="证件类型" prop="idType">
                <el-select v-model="formData.idType" placeholder="请选择证件类型">
                  <el-option label="身份证" value="ID_CARD" />
                  <el-option label="护照" value="PASSPORT" />
                  <el-option label="驾驶证" value="DRIVER_LICENSE" />
                </el-select>
              </el-form-item>

              <el-form-item label="证件号码" prop="idNumber">
                <el-input
                  v-model="formData.idNumber"
                  placeholder="请输入证件号码"
                  maxlength="30"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="地址" prop="address" class="md:col-span-2">
                <el-input
                  v-model="formData.address"
                  type="textarea"
                  placeholder="请输入详细地址"
                  :rows="3"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </div>
          </div>

          <!-- 步骤2：身份证明 -->
          <div v-if="currentStep === 1" class="form-step">
            <h2 class="step-title">身份证明文件</h2>
            
            <!-- 上传说明 -->
            <el-alert
              title="上传要求"
              type="info"
              :closable="false"
              class="mb-6"
            >
              <ul class="list-disc list-inside space-y-1 text-sm">
                <li>文件格式：JPG、PNG、PDF，大小不超过5MB</li>
                <li>图片清晰，四角完整，无遮挡</li>
                <li>证件信息清晰可见，无反光</li>
                <li>手持证件照请确保人脸和证件都清晰可见</li>
              </ul>
            </el-alert>

            <div class="space-y-6">
              <!-- 身份证正面 -->
              <div class="upload-section">
                <h3 class="upload-title">身份证正面 *</h3>
                <el-upload
                  ref="idCardFrontUploadRef"
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :data="{ type: 'id_card_front' }"
                  :file-list="idCardFrontFiles"
                  :on-success="(response, file) => handleUploadSuccess(response, file, 'idCardFront')"
                  :on-error="handleUploadError"
                  :before-upload="beforeUpload"
                  :on-remove="(file) => handleRemove(file, 'idCardFront')"
                  list-type="picture-card"
                  accept="image/*,.pdf"
                  :limit="1"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </div>

              <!-- 身份证背面 -->
              <div class="upload-section">
                <h3 class="upload-title">身份证背面 *</h3>
                <el-upload
                  ref="idCardBackUploadRef"
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :data="{ type: 'id_card_back' }"
                  :file-list="idCardBackFiles"
                  :on-success="(response, file) => handleUploadSuccess(response, file, 'idCardBack')"
                  :on-error="handleUploadError"
                  :before-upload="beforeUpload"
                  :on-remove="(file) => handleRemove(file, 'idCardBack')"
                  list-type="picture-card"
                  accept="image/*,.pdf"
                  :limit="1"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </div>

              <!-- 手持证件照 -->
              <div class="upload-section">
                <h3 class="upload-title">手持证件照 *</h3>
                <p class="text-sm text-gray-500 mb-3">
                  请手持身份证与您的脸部一起拍照，确保人脸和证件信息都清晰可见
                </p>
                <el-upload
                  ref="selfieUploadRef"
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :data="{ type: 'selfie' }"
                  :file-list="selfieFiles"
                  :on-success="(response, file) => handleUploadSuccess(response, file, 'selfie')"
                  :on-error="handleUploadError"
                  :before-upload="beforeUpload"
                  :on-remove="(file) => handleRemove(file, 'selfie')"
                  list-type="picture-card"
                  accept="image/*"
                  :limit="1"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </div>

              <!-- 地址证明（可选） -->
              <div class="upload-section">
                <h3 class="upload-title">地址证明（可选）</h3>
                <p class="text-sm text-gray-500 mb-3">
                  可上传水电费账单、银行对账单等包含您姓名和地址的文件
                </p>
                <el-upload
                  ref="addressProofUploadRef"
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :data="{ type: 'proof_of_address' }"
                  :file-list="addressProofFiles"
                  :on-success="(response, file) => handleUploadSuccess(response, file, 'addressProof')"
                  :on-error="handleUploadError"
                  :before-upload="beforeUpload"
                  :on-remove="(file) => handleRemove(file, 'addressProof')"
                  list-type="picture-card"
                  accept="image/*,.pdf"
                  :limit="1"
                >
                  <el-icon><Plus /></el-icon>
                </el-upload>
              </div>
            </div>
          </div>

          <!-- 步骤3：人脸识别 -->
          <div v-if="currentStep === 2" class="form-step">
            <h2 class="step-title">人脸识别验证</h2>
            
            <div class="text-center py-8">
              <div class="face-recognition-container">
                <div v-if="!faceVerified" class="space-y-6">
                  <div class="face-icon">
                    <el-icon :size="80" class="text-blue-500">
                      <UserFilled />
                    </el-icon>
                  </div>
                  <p class="text-lg text-gray-700 dark:text-gray-300">
                    请点击下方按钮开始人脸识别验证
                  </p>
                  <el-button 
                    type="primary" 
                    size="large"
                    @click="startFaceRecognition"
                    :loading="faceRecognitionLoading"
                  >
                    开始人脸识别
                  </el-button>
                </div>
                
                <div v-else class="space-y-4">
                  <div class="face-success">
                    <el-icon :size="80" class="text-green-500">
                      <CircleCheckFilled />
                    </el-icon>
                  </div>
                  <p class="text-lg text-green-600">
                    人脸识别验证成功！
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- 步骤4：确认提交 -->
          <div v-if="currentStep === 3" class="form-step">
            <h2 class="step-title">确认提交</h2>
            
            <div class="space-y-6">
              <!-- 信息确认 -->
              <el-card>
                <template #header>
                  <span class="font-medium">请确认您的信息</span>
                </template>
                
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="姓名">
                    {{ formData.fullName }}
                  </el-descriptions-item>
                  <el-descriptions-item label="性别">
                    {{ getGenderLabel(formData.gender) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="出生日期">
                    {{ formData.dateOfBirth }}
                  </el-descriptions-item>
                  <el-descriptions-item label="国籍">
                    {{ getNationalityLabel(formData.nationality) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="证件类型">
                    {{ getIdTypeLabel(formData.idType) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="证件号码">
                    {{ maskIdNumber(formData.idNumber) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="地址" span="2">
                    {{ formData.address }}
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>

              <!-- 文件确认 -->
              <el-card>
                <template #header>
                  <span class="font-medium">已上传文件</span>
                </template>
                
                <div class="space-y-3">
                  <div class="flex items-center justify-between">
                    <span>身份证正面</span>
                    <el-tag type="success" v-if="formData.idCardFront">已上传</el-tag>
                    <el-tag type="danger" v-else>未上传</el-tag>
                  </div>
                  <div class="flex items-center justify-between">
                    <span>身份证背面</span>
                    <el-tag type="success" v-if="formData.idCardBack">已上传</el-tag>
                    <el-tag type="danger" v-else>未上传</el-tag>
                  </div>
                  <div class="flex items-center justify-between">
                    <span>手持证件照</span>
                    <el-tag type="success" v-if="formData.selfie">已上传</el-tag>
                    <el-tag type="danger" v-else>未上传</el-tag>
                  </div>
                  <div class="flex items-center justify-between">
                    <span>地址证明</span>
                    <el-tag type="success" v-if="formData.addressProof">已上传</el-tag>
                    <el-tag type="info" v-else>可选</el-tag>
                  </div>
                  <div class="flex items-center justify-between">
                    <span>人脸识别</span>
                    <el-tag type="success" v-if="faceVerified">已完成</el-tag>
                    <el-tag type="danger" v-else>未完成</el-tag>
                  </div>
                </div>
              </el-card>

              <!-- 服务条款 -->
              <el-card>
                <el-checkbox v-model="agreedToTerms" size="large">
                  我已阅读并同意
                  <el-link type="primary" @click="showTerms">《服务条款》</el-link>
                  和
                  <el-link type="primary" @click="showPrivacyPolicy">《隐私政策》</el-link>
                </el-checkbox>
              </el-card>
            </div>
          </div>
        </el-form>

        <!-- 操作按钮 -->
        <div class="mt-8 flex justify-between">
          <el-button 
            v-if="currentStep > 0" 
            @click="prevStep"
            size="large"
          >
            上一步
          </el-button>
          <div></div>
          <div class="space-x-3">
            <el-button @click="saveDraft" :loading="savingDraft">
              保存草稿
            </el-button>
            <el-button 
              v-if="currentStep < 3"
              type="primary" 
              @click="nextStep"
              size="large"
            >
              下一步
            </el-button>
            <el-button 
              v-else
              type="primary" 
              @click="submitKyc"
              size="large"
              :loading="submitting"
              :disabled="!canSubmit"
            >
              提交审核
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  UserFilled,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import { submitKycApplication, uploadKycDocument, saveDraftKyc } from '@/api/kyc'
import { getAuthToken } from '@/utils/auth'

// 响应式状态
const router = useRouter()
const formRef = ref()
const currentStep = ref(0)
const submitting = ref(false)
const savingDraft = ref(false)
const faceRecognitionLoading = ref(false)
const faceVerified = ref(false)
const agreedToTerms = ref(false)

// 文件上传相关
const idCardFrontFiles = ref([])
const idCardBackFiles = ref([])
const selfieFiles = ref([])
const addressProofFiles = ref([])

// 表单数据
const formData = reactive({
  fullName: '',
  gender: '',
  dateOfBirth: '',
  nationality: '',
  idType: '',
  idNumber: '',
  address: '',
  idCardFront: '',
  idCardBack: '',
  selfie: '',
  addressProof: ''
})

// 表单验证规则
const formRules = {
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  dateOfBirth: [
    { required: true, message: '请选择出生日期', trigger: 'change' }
  ],
  nationality: [
    { required: true, message: '请选择国籍', trigger: 'change' }
  ],
  idType: [
    { required: true, message: '请选择证件类型', trigger: 'change' }
  ],
  idNumber: [
    { required: true, message: '请输入证件号码', trigger: 'blur' },
    { min: 6, max: 30, message: '证件号码长度在 6 到 30 个字符', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' },
    { min: 10, max: 200, message: '地址长度在 10 到 200 个字符', trigger: 'blur' }
  ]
}

// 国家列表
const countries = ref([
  { code: 'CN', name: '中国' },
  { code: 'US', name: '美国' },
  { code: 'JP', name: '日本' },
  { code: 'KR', name: '韩国' },
  { code: 'SG', name: '新加坡' },
  { code: 'HK', name: '香港' },
  { code: 'TW', name: '台湾' },
  // 更多国家...
])

// 上传配置
const uploadAction = `${import.meta.env.VITE_API_URL}/api/kyc/upload`
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${getAuthToken()}`
}))

// 计算属性
const canSubmit = computed(() => {
  return formData.idCardFront && 
         formData.idCardBack && 
         formData.selfie && 
         faceVerified.value && 
         agreedToTerms.value
})

// 方法
const disabledDate = (time: Date) => {
  // 禁用18岁以下和未来日期
  const eighteenYearsAgo = new Date()
  eighteenYearsAgo.setFullYear(eighteenYearsAgo.getFullYear() - 18)
  return time.getTime() > Date.now() || time.getTime() > eighteenYearsAgo.getTime()
}

const beforeUpload = (file: File) => {
  const isValidType = file.type.startsWith('image/') || file.type === 'application/pdf'
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isValidType) {
    ElMessage.error('只能上传图片或PDF文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }
  return true
}

const handleUploadSuccess = (response: any, file: any, type: string) => {
  if (response.success) {
    formData[type as keyof typeof formData] = response.data.fileUrl
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(response.message || '文件上传失败')
  }
}

const handleUploadError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('文件上传失败，请重试')
}

const handleRemove = (file: any, type: string) => {
  formData[type as keyof typeof formData] = ''
}

const startFaceRecognition = async () => {
  try {
    faceRecognitionLoading.value = true
    
    // 模拟人脸识别过程
    await new Promise(resolve => setTimeout(resolve, 3000))
    
    // 实际项目中这里应该调用人脸识别API
    faceVerified.value = true
    ElMessage.success('人脸识别验证成功')
  } catch (error) {
    ElMessage.error('人脸识别失败，请重试')
  } finally {
    faceRecognitionLoading.value = false
  }
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    // 验证第一步表单
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  } else if (currentStep.value === 1) {
    // 验证文件上传
    if (!formData.idCardFront || !formData.idCardBack || !formData.selfie) {
      ElMessage.warning('请上传必需的证件文件')
      return
    }
  } else if (currentStep.value === 2) {
    // 验证人脸识别
    if (!faceVerified.value) {
      ElMessage.warning('请完成人脸识别验证')
      return
    }
  }
  
  currentStep.value++
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const saveDraft = async () => {
  try {
    savingDraft.value = true
    const response = await saveDraftKyc(formData)
    
    if (response.data.success) {
      ElMessage.success('草稿已保存')
    } else {
      ElMessage.error(response.data.message || '保存草稿失败')
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败，请重试')
  } finally {
    savingDraft.value = false
  }
}

const submitKyc = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请完成所有必需步骤')
    return
  }

  try {
    const confirmed = await ElMessageBox.confirm(
      '提交后将无法修改，确认提交KYC申请吗？',
      '确认提交',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    if (!confirmed) return

    submitting.value = true
    const response = await submitKycApplication({
      ...formData,
      faceVerified: faceVerified.value
    })
    
    if (response.data.success) {
      ElMessage.success('KYC申请提交成功')
      router.push('/kyc/status')
    } else {
      ElMessage.error(response.data.message || 'KYC申请提交失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交KYC失败:', error)
      ElMessage.error('提交失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

const getGenderLabel = (gender: string) => {
  const map = { male: '男', female: '女', other: '其他' }
  return map[gender as keyof typeof map] || gender
}

const getNationalityLabel = (code: string) => {
  const country = countries.value.find(c => c.code === code)
  return country?.name || code
}

const getIdTypeLabel = (type: string) => {
  const map = { 
    ID_CARD: '身份证', 
    PASSPORT: '护照', 
    DRIVER_LICENSE: '驾驶证' 
  }
  return map[type as keyof typeof map] || type
}

const maskIdNumber = (idNumber: string) => {
  if (idNumber.length <= 6) return idNumber
  return idNumber.slice(0, 3) + '****' + idNumber.slice(-3)
}

const showTerms = () => {
  ElMessage.info('服务条款页面开发中')
}

const showPrivacyPolicy = () => {
  ElMessage.info('隐私政策页面开发中')
}

// 生命周期
onMounted(() => {
  // 可以在这里加载草稿数据
})
</script>

<style scoped>
.kyc-submit-view {
  @apply max-w-4xl mx-auto p-6;
}

.page-header {
  @apply text-center;
}

.form-step {
  @apply space-y-6;
}

.step-title {
  @apply text-xl font-semibold text-gray-900 dark:text-white mb-6 pb-3 border-b border-gray-200 dark:border-gray-700;
}

.upload-section {
  @apply space-y-3;
}

.upload-title {
  @apply text-base font-medium text-gray-900 dark:text-white;
}

.face-recognition-container {
  @apply bg-gray-50 dark:bg-gray-800 rounded-lg p-8;
}

.face-icon, .face-success {
  @apply flex justify-center;
}

/* 上传组件样式调整 */
:deep(.el-upload--picture-card) {
  @apply w-32 h-32;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  @apply w-32 h-32;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .kyc-submit-view {
    @apply p-4;
  }
  
  .grid {
    @apply grid-cols-1;
  }
  
  :deep(.el-form--large .el-form-item__label) {
    @apply text-sm;
  }
}
</style>