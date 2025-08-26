<template>
  <div class="user-detail-view">
    <!-- 面包屑导航 -->
    <el-breadcrumb class="mb-4">
      <el-breadcrumb-item to="/users">用户管理</el-breadcrumb-item>
      <el-breadcrumb-item>用户详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="loading" class="text-center py-8">
      <el-icon :size="32" class="is-loading">
        <Loading />
      </el-icon>
      <p class="mt-2 text-gray-500">加载中...</p>
    </div>

    <div v-else-if="userDetail" class="space-y-6">
      <!-- 用户基础信息 -->
      <el-card>
        <template #header>
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium">基础信息</h3>
            <div class="flex items-center space-x-2">
              <el-tag :type="getStatusType(userDetail.status)">
                {{ getStatusText(userDetail.status) }}
              </el-tag>
              <el-dropdown @command="handleUserAction">
                <el-button type="primary" size="small">
                  操作 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item 
                      command="active" 
                      v-if="userDetail.status !== 'active'"
                    >
                      激活账户
                    </el-dropdown-item>
                    <el-dropdown-item 
                      command="suspend" 
                      v-if="userDetail.status === 'active'"
                    >
                      冻结账户
                    </el-dropdown-item>
                    <el-dropdown-item command="reset-password">
                      重置密码
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户ID">
                {{ userDetail.id }}
              </el-descriptions-item>
              <el-descriptions-item label="用户名">
                {{ userDetail.username }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">
                {{ userDetail.email }}
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                {{ userDetail.phone || '未绑定' }}
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">
                {{ formatTime(userDetail.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="最后登录">
                {{ formatTime(userDetail.lastLoginAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col :span="12">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="实名状态">
                <el-tag :type="getKycStatusType(userDetail.kycStatus)">
                  {{ getKycStatusText(userDetail.kycStatus) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="账户等级">
                VIP {{ userDetail.level || 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="注册IP">
                {{ userDetail.registerIp }}
              </el-descriptions-item>
              <el-descriptions-item label="登录IP">
                {{ userDetail.lastLoginIp }}
              </el-descriptions-item>
              <el-descriptions-item label="风险等级">
                <el-tag :type="getRiskLevelType(userDetail.riskLevel)">
                  {{ getRiskLevelText(userDetail.riskLevel) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
      </el-card>

      <!-- 资产信息 -->
      <el-card>
        <template #header>
          <h3 class="text-lg font-medium">资产信息</h3>
        </template>
        <el-table :data="userDetail.wallets" stripe>
          <el-table-column prop="currency" label="币种" width="100" />
          <el-table-column prop="balance" label="可用余额" align="right">
            <template #default="{ row }">
              {{ formatAmount(row.balance) }}
            </template>
          </el-table-column>
          <el-table-column prop="frozen" label="冻结余额" align="right">
            <template #default="{ row }">
              {{ formatAmount(row.frozen) }}
            </template>
          </el-table-column>
          <el-table-column prop="total" label="总余额" align="right">
            <template #default="{ row }">
              {{ formatAmount(row.balance + row.frozen) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 交易统计 -->
      <el-card>
        <template #header>
          <h3 class="text-lg font-medium">交易统计</h3>
        </template>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ userDetail.stats?.totalOrders || 0 }}</div>
              <div class="stat-label">总订单数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ formatAmount(userDetail.stats?.totalVolume || 0) }}</div>
              <div class="stat-label">总交易量</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ formatAmount(userDetail.stats?.totalFees || 0) }}</div>
              <div class="stat-label">总手续费</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ userDetail.stats?.lastTradeTime ? formatTime(userDetail.stats.lastTradeTime) : '无' }}</div>
              <div class="stat-label">最后交易</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 最近活动 -->
      <el-card>
        <template #header>
          <h3 class="text-lg font-medium">最近活动</h3>
        </template>
        <el-table :data="userDetail.recentActivities" stripe>
          <el-table-column prop="time" label="时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.time) }}
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getActivityTypeText(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="ip" label="IP地址" width="150" />
        </el-table>
      </el-card>
    </div>

    <div v-else class="text-center py-8">
      <p class="text-gray-500">用户不存在</p>
      <el-button @click="$router.back()" class="mt-4">返回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, ArrowDown } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const userDetail = ref(null)

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatAmount = (amount: number) => {
  return amount?.toFixed(8) || '0.00000000'
}

const getStatusType = (status: string) => {
  const statusMap = {
    active: 'success',
    suspended: 'danger',
    pending: 'warning'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap = {
    active: '正常',
    suspended: '冻结',
    pending: '待审核'
  }
  return statusMap[status] || status
}

const getKycStatusType = (status: string) => {
  const statusMap = {
    verified: 'success',
    pending: 'warning',
    rejected: 'danger',
    none: 'info'
  }
  return statusMap[status] || 'info'
}

const getKycStatusText = (status: string) => {
  const statusMap = {
    verified: '已认证',
    pending: '审核中',
    rejected: '已拒绝',
    none: '未认证'
  }
  return statusMap[status] || status
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

const getActivityTypeText = (type: string) => {
  const typeMap = {
    login: '登录',
    trade: '交易',
    withdraw: '提现',
    deposit: '充值',
    kyc: 'KYC'
  }
  return typeMap[type] || type
}

const handleUserAction = async (command: string) => {
  try {
    switch (command) {
      case 'active':
        await ElMessageBox.confirm('确定要激活此用户吗？', '确认操作')
        // TODO: 调用API激活用户
        ElMessage.success('用户已激活')
        loadUserDetail()
        break
      case 'suspend':
        await ElMessageBox.confirm('确定要冻结此用户吗？', '确认操作')
        // TODO: 调用API冻结用户
        ElMessage.success('用户已冻结')
        loadUserDetail()
        break
      case 'reset-password':
        await ElMessageBox.confirm('确定要重置此用户的密码吗？', '确认操作')
        // TODO: 调用API重置密码
        ElMessage.success('密码重置邮件已发送')
        break
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const loadUserDetail = async () => {
  try {
    loading.value = true
    const userId = route.params.id
    
    // TODO: 调用API获取用户详情
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    // 模拟数据
    userDetail.value = {
      id: userId,
      username: 'testuser',
      email: 'test@example.com',
      phone: '13800138000',
      status: 'active',
      kycStatus: 'verified',
      level: 1,
      riskLevel: 'low',
      registerIp: '192.168.1.1',
      lastLoginIp: '192.168.1.2',
      createdAt: '2024-01-01 10:00:00',
      lastLoginAt: '2024-01-20 15:30:00',
      wallets: [
        { currency: 'USDT', balance: 1000.123456, frozen: 0 },
        { currency: 'BTC', balance: 0.001234, frozen: 0 }
      ],
      stats: {
        totalOrders: 25,
        totalVolume: 50000.12,
        totalFees: 50.12,
        lastTradeTime: '2024-01-20 14:30:00'
      },
      recentActivities: [
        {
          time: '2024-01-20 15:30:00',
          type: 'login',
          description: '用户登录',
          ip: '192.168.1.2'
        }
      ]
    }
  } catch (error) {
    console.error('加载用户详情失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUserDetail()
})
</script>

<style scoped>
.user-detail-view {
  max-width: 1200px;
}

.stat-item {
  text-align: center;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.stat-label {
  color: #6b7280;
  font-size: 0.875rem;
}
</style>