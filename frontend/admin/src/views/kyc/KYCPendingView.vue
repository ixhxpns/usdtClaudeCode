<template>
  <div class="kyc-pending-view">
    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <h3 class="text-lg font-medium">待审核KYC</h3>
          <div class="flex items-center space-x-2">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索用户..." 
              class="w-64" 
              size="small"
              @keyup.enter="loadData"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="loadData" size="small">搜索</el-button>
            <el-button @click="refreshData" :loading="loading" size="small">
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filters mb-4">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable size="small">
              <el-option label="全部" value="" />
              <el-option label="低风险" value="low" />
              <el-option label="中风险" value="medium" />
              <el-option label="高风险" value="high" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
            />
          </el-col>
          <el-col :span="6">
            <el-button type="primary" @click="applyFilters" size="small">
              筛选
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- KYC列表 -->
      <el-table :data="tableData" :loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="p-4">
              <el-row :gutter="20">
                <el-col :span="12">
                  <h4 class="mb-2 font-medium">个人信息</h4>
                  <el-descriptions :column="1" size="small" border>
                    <el-descriptions-item label="真实姓名">
                      {{ row.realName }}
                    </el-descriptions-item>
                    <el-descriptions-item label="身份证号">
                      {{ row.idNumber }}
                    </el-descriptions-item>
                    <el-descriptions-item label="性别">
                      {{ row.gender === 'male' ? '男' : '女' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="出生日期">
                      {{ row.birthDate }}
                    </el-descriptions-item>
                    <el-descriptions-item label="国籍">
                      {{ row.nationality }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-col>
                <el-col :span="12">
                  <h4 class="mb-2 font-medium">证件照片</h4>
                  <div class="flex space-x-4">
                    <div class="text-center">
                      <img 
                        :src="row.idCardFront" 
                        alt="身份证正面" 
                        class="w-32 h-20 object-cover border cursor-pointer"
                        @click="previewImage(row.idCardFront)"
                      />
                      <p class="text-sm text-gray-500 mt-1">身份证正面</p>
                    </div>
                    <div class="text-center">
                      <img 
                        :src="row.idCardBack" 
                        alt="身份证背面" 
                        class="w-32 h-20 object-cover border cursor-pointer"
                        @click="previewImage(row.idCardBack)"
                      />
                      <p class="text-sm text-gray-500 mt-1">身份证背面</p>
                    </div>
                    <div class="text-center" v-if="row.selfiePhoto">
                      <img 
                        :src="row.selfiePhoto" 
                        alt="手持身份证" 
                        class="w-32 h-20 object-cover border cursor-pointer"
                        @click="previewImage(row.selfiePhoto)"
                      />
                      <p class="text-sm text-gray-500 mt-1">手持身份证</p>
                    </div>
                  </div>
                </el-col>
              </el-row>
              <div class="mt-4 flex space-x-2">
                <el-button 
                  type="success" 
                  size="small" 
                  @click="approveKyc(row)"
                >
                  通过
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="rejectKyc(row)"
                >
                  拒绝
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="idType" label="证件类型" width="100">
          <template #default="{ row }">
            {{ getIdTypeText(row.idType) }}
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)" size="small">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button 
              type="success" 
              link 
              size="small" 
              @click="approveKyc(row)"
            >
              通过
            </el-button>
            <el-button 
              type="danger" 
              link 
              size="small" 
              @click="rejectKyc(row)"
            >
              拒绝
            </el-button>
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click="viewUserDetail(row.userId)"
            >
              查看用户
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper mt-4 flex justify-center">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      :title="reviewAction === 'approve' ? '通过KYC审核' : '拒绝KYC审核'"
      width="500px"
    >
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="备注">
          <el-input
            v-model="reviewForm.remark"
            type="textarea"
            :rows="4"
            :placeholder="reviewAction === 'approve' ? '通过原因（可选）' : '拒绝原因（必填）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button 
          :type="reviewAction === 'approve' ? 'success' : 'danger'" 
          @click="confirmReview"
          :loading="reviewing"
        >
          确认{{ reviewAction === 'approve' ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="图片预览"
      width="800px"
      center
    >
      <div class="text-center">
        <img :src="previewImageUrl" alt="预览" class="max-w-full max-h-96" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const reviewing = ref(false)
const reviewDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const previewImageUrl = ref('')
const searchKeyword = ref('')
const reviewAction = ref('approve')
const selectedKyc = ref(null)

const filters = reactive({
  riskLevel: '',
  dateRange: []
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const reviewForm = reactive({
  remark: ''
})

const tableData = ref([])

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const getIdTypeText = (type: string) => {
  const typeMap = {
    id_card: '身份证',
    passport: '护照',
    driving_license: '驾驶证'
  }
  return typeMap[type] || type
}

const getRiskLevelType = (level: string) => {
  const levelMap = {
    low: 'success',
    medium: 'warning',
    high: 'danger'
  }
  return levelMap[level] || 'info'
}

const getRiskLevelText = (level: string) => {
  const levelMap = {
    low: '低风险',
    medium: '中风险',
    high: '高风险'
  }
  return levelMap[level] || level
}

const applyFilters = () => {
  pagination.current = 1
  loadData()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.current = page
  loadData()
}

const previewImage = (imageUrl: string) => {
  previewImageUrl.value = imageUrl
  previewDialogVisible.value = true
}

const approveKyc = (row: any) => {
  selectedKyc.value = row
  reviewAction.value = 'approve'
  reviewForm.remark = ''
  reviewDialogVisible.value = true
}

const rejectKyc = (row: any) => {
  selectedKyc.value = row
  reviewAction.value = 'reject'
  reviewForm.remark = ''
  reviewDialogVisible.value = true
}

const confirmReview = async () => {
  if (reviewAction.value === 'reject' && !reviewForm.remark.trim()) {
    ElMessage.error('拒绝时必须填写拒绝原因')
    return
  }

  try {
    reviewing.value = true
    // TODO: 调用API提交审核结果
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    ElMessage.success(`KYC审核${reviewAction.value === 'approve' ? '通过' : '拒绝'}成功`)
    reviewDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  } finally {
    reviewing.value = false
  }
}

const viewUserDetail = (userId: string) => {
  router.push(`/users/${userId}`)
}

const refreshData = () => {
  loadData()
}

const loadData = async () => {
  try {
    loading.value = true
    // TODO: 调用API获取待审核KYC列表
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    // 模拟数据
    tableData.value = [
      {
        id: 'KYC001',
        userId: 'USER001',
        username: 'testuser',
        email: 'test@example.com',
        realName: '张三',
        idType: 'id_card',
        idNumber: '110101199001011234',
        gender: 'male',
        birthDate: '1990-01-01',
        nationality: '中国',
        riskLevel: 'low',
        submitTime: '2024-01-20 10:30:00',
        idCardFront: 'https://via.placeholder.com/300x200',
        idCardBack: 'https://via.placeholder.com/300x200',
        selfiePhoto: 'https://via.placeholder.com/300x200'
      }
    ]
    pagination.total = 1
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.kyc-pending-view {
  max-width: 1400px;
}

.filters .el-select,
.filters .el-date-picker {
  width: 100%;
}
</style>