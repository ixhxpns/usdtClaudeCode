<template>
  <div class="admin-diagnostic">
    <el-card v-if="showDiagnostic" class="diagnostic-card">
      <template #header>
        <div class="card-header">
          <span>🔧 系统诊断工具</span>
          <el-button 
            type="text" 
            size="small" 
            @click="showDiagnostic = false"
          >
            ×
          </el-button>
        </div>
      </template>
      
      <div class="diagnostic-content">
        <div class="diagnostic-section">
          <h4>🌐 API连接状态</h4>
          <div v-if="apiHealthLoading" class="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            检查中...
          </div>
          <div v-else>
            <div v-for="endpoint in apiHealthResults" :key="endpoint.url" class="endpoint-status">
              <div class="endpoint-info">
                <span 
                  :class="['status-indicator', endpoint.success ? 'success' : 'error']"
                >
                  {{ endpoint.success ? '✅' : '❌' }}
                </span>
                <span class="endpoint-name">{{ endpoint.name }}</span>
                <span class="endpoint-url">{{ endpoint.url }}</span>
              </div>
              <div class="endpoint-details">
                <span v-if="endpoint.status" class="status-code">
                  HTTP {{ endpoint.status }}
                </span>
                <span v-if="endpoint.error" class="error-message">
                  {{ endpoint.error }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="diagnostic-section">
          <h4>🔐 RSA加密测试</h4>
          <div class="crypto-test">
            <el-button 
              :loading="rsaTestLoading" 
              @click="testRSAEncryption"
              size="small"
            >
              测试RSA加密
            </el-button>
            <div v-if="rsaTestResult" class="test-result">
              <div :class="['result-status', rsaTestResult.success ? 'success' : 'error']">
                {{ rsaTestResult.success ? '✅ RSA加密正常' : '❌ RSA加密失败' }}
              </div>
              <div v-if="rsaTestResult.message" class="result-message">
                {{ rsaTestResult.message }}
              </div>
            </div>
          </div>
        </div>

        <div class="diagnostic-section">
          <h4>📊 系统信息</h4>
          <div class="system-info">
            <div class="info-item">
              <strong>前端版本:</strong> {{ frontendVersion }}
            </div>
            <div class="info-item">
              <strong>API基础URL:</strong> {{ apiBaseUrl }}
            </div>
            <div class="info-item">
              <strong>当前时间:</strong> {{ currentTime }}
            </div>
            <div class="info-item">
              <strong>登录尝试:</strong> {{ loginAttempts }}/{{ maxAttempts }}
            </div>
          </div>
        </div>

        <div class="diagnostic-actions">
          <el-button @click="refreshDiagnostic" :loading="refreshing" size="small">
            🔄 刷新检查
          </el-button>
          <el-button @click="exportDiagnosticReport" size="small" type="primary">
            📋 导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 触发按钮 -->
    <el-button 
      v-if="!showDiagnostic"
      class="diagnostic-trigger"
      type="info"
      size="small"
      @click="openDiagnostic"
    >
      🔧 诊断
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { checkAPIHealth, testPublicKeyConnection, rsaEncryptData } from '@/utils/crypto'
import { useAuthStore } from '@/stores/auth'

// Props
interface Props {
  autoShow?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  autoShow: false
})

// State
const showDiagnostic = ref(props.autoShow)
const apiHealthLoading = ref(false)
const apiHealthResults = ref<any[]>([])
const rsaTestLoading = ref(false)
const rsaTestResult = ref<any>(null)
const refreshing = ref(false)

// Store
const authStore = useAuthStore()

// Computed
const frontendVersion = computed(() => import.meta.env.VITE_APP_VERSION || '1.0.0')
const apiBaseUrl = computed(() => import.meta.env.VITE_API_BASE_URL || '/api')
const currentTime = computed(() => new Date().toLocaleString())
const loginAttempts = computed(() => authStore.loginAttempts)
const maxAttempts = computed(() => 5)

// Methods
const openDiagnostic = async () => {
  showDiagnostic.value = true
  await refreshDiagnostic()
}

const refreshDiagnostic = async () => {
  refreshing.value = true
  try {
    await Promise.all([
      checkAPIHealth_(),
      testRSAConnection()
    ])
  } finally {
    refreshing.value = false
  }
}

const checkAPIHealth_ = async () => {
  apiHealthLoading.value = true
  try {
    const result = await checkAPIHealth()
    apiHealthResults.value = result.details
    console.log('API健康检查结果:', result)
  } catch (error) {
    console.error('API健康检查失败:', error)
    ElMessage.error('API健康检查失败')
  } finally {
    apiHealthLoading.value = false
  }
}

const testRSAConnection = async () => {
  try {
    const isConnected = await testPublicKeyConnection()
    console.log('RSA公钥连接测试:', isConnected)
  } catch (error) {
    console.error('RSA连接测试失败:', error)
  }
}

const testRSAEncryption = async () => {
  rsaTestLoading.value = true
  rsaTestResult.value = null
  
  try {
    const testData = 'test_password_123'
    const encrypted = await rsaEncryptData(testData)
    
    if (encrypted && encrypted.length > 0) {
      rsaTestResult.value = {
        success: true,
        message: `加密成功，密文长度: ${encrypted.length}`
      }
    } else {
      rsaTestResult.value = {
        success: false,
        message: '加密返回空结果'
      }
    }
  } catch (error: any) {
    rsaTestResult.value = {
      success: false,
      message: error.message || '加密测试失败'
    }
  } finally {
    rsaTestLoading.value = false
  }
}

const exportDiagnosticReport = () => {
  const report = {
    timestamp: new Date().toISOString(),
    frontend: {
      version: frontendVersion.value,
      apiBaseUrl: apiBaseUrl.value
    },
    apiHealth: apiHealthResults.value,
    rsaTest: rsaTestResult.value,
    auth: {
      loginAttempts: loginAttempts.value,
      isLocked: authStore.isLocked
    }
  }
  
  const blob = new Blob([JSON.stringify(report, null, 2)], {
    type: 'application/json'
  })
  
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `admin-diagnostic-${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('诊断报告已导出')
}

// Lifecycle
onMounted(() => {
  if (props.autoShow) {
    refreshDiagnostic()
  }
})
</script>

<style scoped>
.admin-diagnostic {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
}

.diagnostic-trigger {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.diagnostic-card {
  width: 400px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.diagnostic-content {
  font-size: 14px;
}

.diagnostic-section {
  margin-bottom: 16px;
}

.diagnostic-section h4 {
  margin: 0 0 8px 0;
  color: #409eff;
  font-size: 14px;
}

.loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.endpoint-status {
  margin-bottom: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.endpoint-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.status-indicator {
  font-size: 16px;
}

.endpoint-name {
  font-weight: bold;
  color: #303133;
}

.endpoint-url {
  color: #909399;
  font-family: monospace;
  font-size: 12px;
}

.endpoint-details {
  font-size: 12px;
  margin-left: 24px;
}

.status-code {
  color: #67c23a;
  font-weight: bold;
}

.error-message {
  color: #f56c6c;
}

.crypto-test {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.test-result {
  padding: 8px;
  border-radius: 4px;
  background: #f5f7fa;
}

.result-status.success {
  color: #67c23a;
  font-weight: bold;
}

.result-status.error {
  color: #f56c6c;
  font-weight: bold;
}

.result-message {
  margin-top: 4px;
  color: #606266;
  font-size: 12px;
}

.system-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item {
  display: flex;
  gap: 8px;
}

.info-item strong {
  min-width: 80px;
  color: #303133;
}

.diagnostic-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .admin-diagnostic {
    position: relative;
    top: auto;
    right: auto;
    margin: 16px;
  }
  
  .diagnostic-card {
    width: 100%;
    max-width: none;
  }
}
</style>