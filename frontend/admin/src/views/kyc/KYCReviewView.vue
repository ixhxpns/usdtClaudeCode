<template>
  <div class="kyc-review-view">
    <el-card>
      <template #header>
        <h3 class="text-lg font-medium">KYC审核记录</h3>
      </template>

      <!-- 筛选条件 -->
      <div class="filters mb-4">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-select v-model="filters.status" placeholder="审核状态" clearable size="small">
              <el-option label="全部" value="" />
              <el-option label="通过" value="approved" />
              <el-option label="拒绝" value="rejected" />
            </el-select>
          </el-col>
          <el-col :span="8">
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
            <el-input 
              v-model="filters.keyword" 
              placeholder="搜索用户..." 
              size="small"
            />
          </el-col>
          <el-col :span="4">
            <el-button type="primary" @click="applyFilters" size="small">
              筛选
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 审核记录表格 -->
      <el-table :data="tableData" :loading="loading" stripe>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="submitTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime" label="审核时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.reviewTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewer" label="审核人" width="120" />
        <el-table-column prop="remark" label="审核备注" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              size="small" 
              @click="viewDetail(row)"
            >
              查看详情
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)

const filters = reactive({
  status: '',
  dateRange: [],
  keyword: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const tableData = ref([])

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusType = (status: string) => {
  const statusMap = {
    approved: 'success',
    rejected: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap = {
    approved: '通过',
    rejected: '拒绝'
  }
  return statusMap[status] || status
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

const viewDetail = (row: any) => {
  // TODO: 打开详情对话框或跳转到详情页面
  ElMessage.info('查看详情功能开发中...')
}

const loadData = async () => {
  try {
    loading.value = true
    // TODO: 调用API获取KYC审核记录
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    // 模拟数据
    tableData.value = [
      {
        id: 'KYC001',
        userId: 'USER001',
        username: 'testuser',
        realName: '张三',
        submitTime: '2024-01-20 10:30:00',
        reviewTime: '2024-01-20 15:30:00',
        status: 'approved',
        reviewer: 'admin',
        remark: '信息完整，通过审核'
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
.kyc-review-view {
  max-width: 1200px;
}

.filters .el-select,
.filters .el-date-picker,
.filters .el-input {
  width: 100%;
}
</style>