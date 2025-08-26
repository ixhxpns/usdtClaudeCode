<template>
  <div class="kyc-status-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">KYC 验证状态</h1>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">查看您的身份验证状态和进度</p>
    </div>

    <!-- 状态卡片 -->
    <div class="mt-6">
      <el-card class="status-card" :body-style="{ padding: '24px' }">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-4">
            <!-- 状态图标 -->
            <div class="status-icon" :class="statusIconClass">
              <el-icon :size="32">
                <component :is="statusIcon" />
              </el-icon>
            </div>
            
            <!-- 状态信息 -->
            <div>
              <h2 class="text-xl font-semibold text-gray-900 dark:text-white">
                {{ statusTitle }}
              </h2>
              <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                {{ statusDescription }}
              </p>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex space-x-3">
            <el-button 
              v-if="kycStatus === 'not_submitted' || kycStatus === 'rejected'"
              type="primary" 
              @click="handleSubmitKYC"
              :loading="loading"
            >
              {{ kycStatus === 'not_submitted' ? '开始认证' : '重新提交' }}
            </el-button>
            <el-button 
              v-if="kycStatus === 'pending' && kycData?.documents?.length > 0"
              @click="handleViewDocuments"
            >
              查看文档
            </el-button>
            <el-button 
              @click="handleRefresh"
              :loading="refreshing"
            >
              刷新状态
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 进度条 -->
    <div class="mt-6" v-if="kycStatus !== 'not_submitted'">
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-lg font-medium">验证进度</span>
            <span class="text-sm text-gray-500">{{ progressPercentage }}%</span>
          </div>
        </template>
        
        <el-steps :active="currentStep" :process-status="stepStatus" finish-status="success">
          <el-step title="提交资料" description="上传身份证明文件" />
          <el-step title="初步审核" description="系统自动验证文件" />
          <el-step title="人工审核" description="专业团队人工审核" />
          <el-step title="审核完成" description="验证完成" />
        </el-steps>
      </el-card>
    </div>

    <!-- KYC详细信息 -->
    <div class="mt-6" v-if="kycData">
      <el-card>
        <template #header>
          <span class="text-lg font-medium">KYC 详细信息</span>
        </template>
        
        <div class="space-y-6">
          <!-- 基本信息 -->
          <div>
            <h3 class="text-base font-medium text-gray-900 dark:text-white mb-4">基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">
                {{ kycData.fullName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="身份证号">
                {{ maskedIdNumber(kycData.idNumber) }}
              </el-descriptions-item>
              <el-descriptions-item label="出生日期">
                {{ formatDate(kycData.dateOfBirth) }}
              </el-descriptions-item>
              <el-descriptions-item label="国籍">
                {{ kycData.nationality || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="提交时间">
                {{ formatDateTime(kycData.submittedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="审核时间" v-if="kycData.reviewedAt">
                {{ formatDateTime(kycData.reviewedAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 文档列表 -->
          <div v-if="kycData.documents && kycData.documents.length > 0">
            <h3 class="text-base font-medium text-gray-900 dark:text-white mb-4">上传文档</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div 
                v-for="doc in kycData.documents" 
                :key="doc.id"
                class="document-card"
              >
                <el-card class="h-full" :body-style="{ padding: '16px' }">
                  <div class="text-center">
                    <div class="document-preview mb-3">
                      <el-image
                        v-if="isImageFile(doc.fileName)"
                        :src="doc.fileUrl"
                        :preview-src-list="[doc.fileUrl]"
                        fit="cover"
                        class="w-full h-32 rounded-md"
                        :hide-on-click-modal="true"
                      />
                      <div v-else class="w-full h-32 bg-gray-100 dark:bg-gray-700 rounded-md flex items-center justify-center">
                        <el-icon :size="40" class="text-gray-400">
                          <Document />
                        </el-icon>
                      </div>
                    </div>
                    <p class="text-sm font-medium text-gray-900 dark:text-white">
                      {{ getDocumentTypeName(doc.type) }}
                    </p>
                    <p class="text-xs text-gray-500 mt-1">
                      {{ doc.fileName }}
                    </p>
                    <div class="mt-2">
                      <el-tag :type="getDocumentStatusType(doc.status)" size="small">
                        {{ getDocumentStatusName(doc.status) }}
                      </el-tag>
                    </div>
                  </div>
                </el-card>
              </div>
            </div>
          </div>

          <!-- 审核反馈 -->
          <div v-if="kycData.rejectionReason">
            <h3 class="text-base font-medium text-gray-900 dark:text-white mb-4">审核反馈</h3>
            <el-alert
              :title="kycStatus === 'rejected' ? '审核未通过' : '需要补充材料'"
              :description="kycData.rejectionReason"
              type="warning"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <div v-if="!kycData && !loading" class="mt-6">
      <el-empty 
        description="暂无KYC数据"
        :image-size="200"
      >
        <el-button type="primary" @click="handleSubmitKYC">
          开始KYC认证
        </el-button>
      </el-empty>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center py-12">
      <el-loading />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Check, 
  Clock, 
  Close, 
  Warning, 
  Document,
  Finished,
  Loading
} from '@element-plus/icons-vue'
import { getKycStatus, refreshKycStatus } from '@/api/kyc'
import type { KycData, KycStatus } from '@/types/user'

// 响应式状态
const router = useRouter()
const loading = ref(false)
const refreshing = ref(false)
const kycData = ref<KycData | null>(null)
const kycStatus = ref<KycStatus>('not_submitted')

// 计算属性
const statusIcon = computed(() => {
  switch (kycStatus.value) {
    case 'approved':
      return Check
    case 'pending':
      return Clock
    case 'rejected':
      return Close
    case 'under_review':
      return Loading
    default:
      return Warning
  }
})

const statusIconClass = computed(() => {
  switch (kycStatus.value) {
    case 'approved':
      return 'bg-green-100 text-green-600 dark:bg-green-900 dark:text-green-400'
    case 'pending':
      return 'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400'
    case 'rejected':
      return 'bg-red-100 text-red-600 dark:bg-red-900 dark:text-red-400'
    case 'under_review':
      return 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900 dark:text-yellow-400'
    default:
      return 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
  }
})

const statusTitle = computed(() => {
  switch (kycStatus.value) {
    case 'approved':
      return 'KYC 验证已通过'
    case 'pending':
      return 'KYC 验证待审核'
    case 'rejected':
      return 'KYC 验证未通过'
    case 'under_review':
      return 'KYC 验证审核中'
    default:
      return '未提交 KYC 验证'
  }
})

const statusDescription = computed(() => {
  switch (kycStatus.value) {
    case 'approved':
      return '恭喜！您的身份验证已通过，可以使用所有平台功能'
    case 'pending':
      return '您的KYC资料已提交，正在等待系统处理'
    case 'rejected':
      return '您的KYC资料审核未通过，请查看反馈并重新提交'
    case 'under_review':
      return '您的KYC资料正在人工审核中，请耐心等待'
    default:
      return '完成身份验证后可使用交易和提现功能'
  }
})

const currentStep = computed(() => {
  switch (kycStatus.value) {
    case 'pending':
      return 1
    case 'under_review':
      return 2
    case 'approved':
      return 4
    case 'rejected':
      return 2
    default:
      return 0
  }
})

const stepStatus = computed(() => {
  if (kycStatus.value === 'rejected') {
    return 'error'
  }
  return 'process'
})

const progressPercentage = computed(() => {
  switch (kycStatus.value) {
    case 'pending':
      return 25
    case 'under_review':
      return 75
    case 'approved':
      return 100
    case 'rejected':
      return 50
    default:
      return 0
  }
})

// 方法
const fetchKycStatus = async () => {
  try {
    loading.value = true
    const response = await getKycStatus()
    
    if (response.data.success) {
      kycData.value = response.data.data
      kycStatus.value = response.data.data?.status || 'not_submitted'
    } else {
      ElMessage.error(response.data.message || '获取KYC状态失败')
    }
  } catch (error) {
    console.error('获取KYC状态失败:', error)
    ElMessage.error('获取KYC状态失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  try {
    refreshing.value = true
    const response = await refreshKycStatus()
    
    if (response.data.success) {
      kycData.value = response.data.data
      kycStatus.value = response.data.data?.status || 'not_submitted'
      ElMessage.success('状态已刷新')
    } else {
      ElMessage.error(response.data.message || '刷新状态失败')
    }
  } catch (error) {
    console.error('刷新状态失败:', error)
    ElMessage.error('刷新状态失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

const handleSubmitKYC = () => {
  router.push('/kyc/submit')
}

const handleViewDocuments = () => {
  // 可以展开文档查看模态框或跳转到文档页面
  ElMessage.info('文档查看功能开发中')
}

const maskedIdNumber = (idNumber: string | undefined): string => {
  if (!idNumber) return '-'
  if (idNumber.length <= 6) return idNumber
  return idNumber.slice(0, 3) + '****' + idNumber.slice(-3)
}

const formatDate = (date: string | undefined): string => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatDateTime = (date: string | undefined): string => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const isImageFile = (fileName: string): boolean => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const extension = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return imageExtensions.includes(extension)
}

const getDocumentTypeName = (type: string): string => {
  const typeMap: Record<string, string> = {
    'id_card_front': '身份证正面',
    'id_card_back': '身份证背面',
    'passport': '护照',
    'driver_license': '驾驶证',
    'selfie': '手持身份证照片',
    'proof_of_address': '地址证明'
  }
  return typeMap[type] || type
}

const getDocumentStatusName = (status: string): string => {
  const statusMap: Record<string, string> = {
    'pending': '待审核',
    'approved': '已通过',
    'rejected': '未通过',
    'under_review': '审核中'
  }
  return statusMap[status] || status
}

const getDocumentStatusType = (status: string): string => {
  const typeMap: Record<string, string> = {
    'pending': 'info',
    'approved': 'success',
    'rejected': 'danger',
    'under_review': 'warning'
  }
  return typeMap[status] || 'info'
}

// 生命周期
onMounted(() => {
  fetchKycStatus()
})
</script>

<style scoped>
.kyc-status-view {
  @apply max-w-6xl mx-auto p-6;
}

.page-header {
  @apply mb-6;
}

.status-card {
  @apply shadow-sm;
}

.status-icon {
  @apply w-16 h-16 rounded-full flex items-center justify-center;
}

.document-card {
  @apply transition-all duration-200 hover:shadow-md;
}

.document-preview {
  @apply overflow-hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .kyc-status-view {
    @apply p-4;
  }
  
  .status-card .flex {
    @apply flex-col space-x-0 space-y-4;
  }
  
  .status-card .flex .flex:first-child {
    @apply flex-row space-x-4 space-y-0;
  }
}

/* 暗黑模式支持 */
.dark .status-card {
  @apply bg-gray-800 border-gray-700;
}

.dark .document-card {
  @apply bg-gray-800 border-gray-700;
}
</style>