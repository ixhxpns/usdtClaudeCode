<template>
  <div class="user-list-view">
    <!-- 搜索和過濾條件 -->
    <el-card class="filter-card" shadow="never">
      <el-form 
        :model="filterForm" 
        inline 
        label-width="80px"
        @submit.prevent="loadUsers"
      >
        <el-form-item label="用戶ID">
          <el-input 
            v-model="filterForm.userId" 
            placeholder="請輸入用戶ID"
            clearable
            @clear="loadUsers"
          />
        </el-form-item>

        <el-form-item label="用戶名">
          <el-input 
            v-model="filterForm.username" 
            placeholder="請輸入用戶名"
            clearable
            @clear="loadUsers"
          />
        </el-form-item>

        <el-form-item label="郵箱">
          <el-input 
            v-model="filterForm.email" 
            placeholder="請輸入郵箱"
            clearable
            @clear="loadUsers"
          />
        </el-form-item>

        <el-form-item label="狀態">
          <el-select 
            v-model="filterForm.status" 
            placeholder="選擇用戶狀態"
            clearable
            @clear="loadUsers"
          >
            <el-option label="全部" value="" />
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="鎖定" value="LOCKED" />
          </el-select>
        </el-form-item>

        <el-form-item label="KYC狀態">
          <el-select 
            v-model="filterForm.kycStatus" 
            placeholder="選擇KYC狀態"
            clearable
            @clear="loadUsers"
          >
            <el-option label="全部" value="" />
            <el-option label="未提交" value="NOT_SUBMITTED" />
            <el-option label="待審核" value="PENDING" />
            <el-option label="已通過" value="APPROVED" />
            <el-option label="已拒絕" value="REJECTED" />
          </el-select>
        </el-form-item>

        <el-form-item label="註冊時間">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="loadUsers"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="loadUsers">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
          <el-button type="success" @click="exportUsers">
            <el-icon><Download /></el-icon>
            導出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 統計卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.totalUsers }}</div>
            <div class="stat-label">總用戶數</div>
          </div>
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.activeUsers }}</div>
            <div class="stat-label">活躍用戶</div>
          </div>
          <div class="stat-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.kycApprovedUsers }}</div>
            <div class="stat-label">KYC通過</div>
          </div>
          <div class="stat-icon">
            <el-icon><CreditCard /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.newUsersToday }}</div>
            <div class="stat-label">今日新增</div>
          </div>
          <div class="stat-icon">
            <el-icon><Plus /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用戶列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <h3>用戶列表</h3>
          <div class="table-actions">
            <el-button type="primary" @click="showCreateUser">
              <el-icon><Plus /></el-icon>
              新增用戶
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="userList"
        v-loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="50" />
        
        <el-table-column 
          prop="id" 
          label="用戶ID" 
          width="80"
          sortable="custom"
        />

        <el-table-column label="用戶信息" min-width="200">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-details">
                <div class="username">{{ row.username }}</div>
                <div class="email">{{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="真實姓名" prop="realName" width="120" />

        <el-table-column label="手機號碼" width="140">
          <template #default="{ row }">
            <div class="phone-info">
              {{ row.phone || '--' }}
              <el-tag v-if="row.phoneVerified" type="success" size="small">
                已驗證
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="狀態" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="KYC狀態" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getKycStatusType(row.kycStatus)">
              {{ getKycStatusText(row.kycStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="註冊時間" width="180" sortable="custom">
          <template #default="{ row }">
            {{ dayjs(row.createdAt).format('YYYY-MM-DD HH:mm') }}
          </template>
        </el-table-column>

        <el-table-column label="最後登錄" width="180">
          <template #default="{ row }">
            {{ row.lastLoginAt ? dayjs(row.lastLoginAt).format('YYYY-MM-DD HH:mm') : '--' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small"
              @click="viewUser(row)"
            >
              查看
            </el-button>
            <el-dropdown @command="(command) => handleUserAction(command, row)">
              <el-button size="small">
                更多
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon>
                    編輯
                  </el-dropdown-item>
                  <el-dropdown-item 
                    :command="row.status === 'ACTIVE' ? 'disable' : 'enable'"
                  >
                    <el-icon>
                      <Lock v-if="row.status === 'ACTIVE'" />
                      <Unlock v-else />
                    </el-icon>
                    {{ row.status === 'ACTIVE' ? '禁用' : '啟用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="resetPassword" divided>
                    <el-icon><Key /></el-icon>
                    重置密碼
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" class="danger-item">
                    <el-icon><Delete /></el-icon>
                    刪除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 批量操作 -->
    <el-card v-if="selectedUsers.length > 0" class="batch-actions" shadow="never">
      <div class="batch-info">
        已選擇 {{ selectedUsers.length }} 個用戶
      </div>
      <div class="batch-buttons">
        <el-button type="warning" @click="batchDisableUsers">
          <el-icon><Lock /></el-icon>
          批量禁用
        </el-button>
        <el-button @click="batchEnableUsers">
          <el-icon><Unlock /></el-icon>
          批量啟用
        </el-button>
        <el-button type="danger" @click="batchDeleteUsers">
          <el-icon><Delete /></el-icon>
          批量刪除
        </el-button>
      </div>
    </el-card>

    <!-- 創建用戶對話框 -->
    <el-dialog
      v-model="createUserVisible"
      title="新增用戶"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createUserFormRef"
        :model="createUserForm"
        :rules="createUserRules"
        label-width="100px"
      >
        <el-form-item label="用戶名" prop="username">
          <el-input v-model="createUserForm.username" />
        </el-form-item>

        <el-form-item label="郵箱" prop="email">
          <el-input v-model="createUserForm.email" />
        </el-form-item>

        <el-form-item label="密碼" prop="password">
          <el-input 
            v-model="createUserForm.password" 
            type="password"
            show-password
          />
        </el-form-item>

        <el-form-item label="真實姓名" prop="realName">
          <el-input v-model="createUserForm.realName" />
        </el-form-item>

        <el-form-item label="手機號碼" prop="phone">
          <el-input v-model="createUserForm.phone" />
        </el-form-item>

        <el-form-item label="狀態" prop="status">
          <el-select v-model="createUserForm.status">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createUserVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="createUser"
          :loading="createUserLoading"
        >
          創建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { AdminApi } from '@/api/admin'
import { dayjs } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Download,
  User,
  UserFilled,
  CreditCard,
  Plus,
  Edit,
  Lock,
  Unlock,
  Key,
  Delete,
  ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 響應式數據
const loading = ref(false)
const userList = ref<any[]>([])
const selectedUsers = ref<any[]>([])
const dateRange = ref<[string, string] | null>(null)

// 過濾表單
const filterForm = reactive({
  userId: '',
  username: '',
  email: '',
  status: '',
  kycStatus: ''
})

// 分頁信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 統計信息
const statistics = reactive({
  totalUsers: 0,
  activeUsers: 0,
  kycApprovedUsers: 0,
  newUsersToday: 0
})

// 創建用戶
const createUserVisible = ref(false)
const createUserLoading = ref(false)
const createUserFormRef = ref<FormInstance>()
const createUserForm = reactive({
  username: '',
  email: '',
  password: '',
  realName: '',
  phone: '',
  status: 'ACTIVE'
})

// 表單驗證規則
const createUserRules: FormRules = {
  username: [
    { required: true, message: '請輸入用戶名', trigger: 'blur' },
    { min: 3, max: 20, message: '用戶名長度應在3-20個字符之間', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '請輸入郵箱', trigger: 'blur' },
    { type: 'email', message: '請輸入有效的郵箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '請輸入密碼', trigger: 'blur' },
    { min: 8, message: '密碼長度至少8位', trigger: 'blur' }
  ]
}

// 方法
const getStatusType = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'DISABLED':
      return 'warning'
    case 'LOCKED':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return '正常'
    case 'DISABLED':
      return '禁用'
    case 'LOCKED':
      return '鎖定'
    default:
      return '未知'
  }
}

const getKycStatusType = (status: string) => {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const getKycStatusText = (status: string) => {
  switch (status) {
    case 'NOT_SUBMITTED':
      return '未提交'
    case 'PENDING':
      return '待審核'
    case 'APPROVED':
      return '已通過'
    case 'REJECTED':
      return '已拒絕'
    default:
      return '未知'
  }
}

const loadUsers = async () => {
  try {
    loading.value = true
    
    const params = {
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      ...filterForm,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }

    // const response = await AdminApi.getUserList(params)
    // userList.value = response.data
    // pagination.total = response.total

    // 模擬數據
    loadMockUsers()

  } catch (error: any) {
    ElMessage.error(error.message || '載入用戶列表失敗')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    // const response = await AdminApi.getUserStatistics()
    // Object.assign(statistics, response)

    // 模擬數據
    Object.assign(statistics, {
      totalUsers: 2845,
      activeUsers: 2630,
      kycApprovedUsers: 1892,
      newUsersToday: 23
    })
  } catch (error: any) {
    console.error('載入統計信息失敗:', error)
  }
}

const loadMockUsers = () => {
  // 模擬用戶數據
  userList.value = [
    {
      id: 1,
      username: 'john_doe',
      email: 'john@example.com',
      realName: 'John Doe',
      phone: '+1234567890',
      phoneVerified: true,
      status: 'ACTIVE',
      kycStatus: 'APPROVED',
      createdAt: '2024-01-15T10:30:00Z',
      lastLoginAt: '2024-01-20T15:45:00Z',
      avatar: ''
    },
    {
      id: 2,
      username: 'jane_smith',
      email: 'jane@example.com',
      realName: 'Jane Smith',
      phone: '+9876543210',
      phoneVerified: false,
      status: 'DISABLED',
      kycStatus: 'PENDING',
      createdAt: '2024-01-14T09:20:00Z',
      lastLoginAt: '2024-01-19T11:30:00Z',
      avatar: ''
    }
  ]
  pagination.total = 2
}

const resetFilter = () => {
  Object.assign(filterForm, {
    userId: '',
    username: '',
    email: '',
    status: '',
    kycStatus: ''
  })
  dateRange.value = null
  pagination.currentPage = 1
  loadUsers()
}

const handleSelectionChange = (selection: any[]) => {
  selectedUsers.value = selection
}

const handleSortChange = ({ prop, order }: any) => {
  // 處理排序
  loadUsers()
}

const viewUser = (user: any) => {
  router.push(`/users/detail/${user.id}`)
}

const handleUserAction = async (command: string, user: any) => {
  switch (command) {
    case 'edit':
      // 編輯用戶
      break
    case 'disable':
      await disableUser(user)
      break
    case 'enable':
      await enableUser(user)
      break
    case 'resetPassword':
      await resetUserPassword(user)
      break
    case 'delete':
      await deleteUser(user)
      break
  }
}

const disableUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(`確定要禁用用戶 "${user.username}" 嗎？`, '確認操作')
    // await AdminApi.disableUser(user.id)
    ElMessage.success('用戶已禁用')
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '禁用用戶失敗')
    }
  }
}

const enableUser = async (user: any) => {
  try {
    // await AdminApi.enableUser(user.id)
    ElMessage.success('用戶已啟用')
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '啟用用戶失敗')
  }
}

const resetUserPassword = async (user: any) => {
  try {
    await ElMessageBox.confirm(`確定要重置用戶 "${user.username}" 的密碼嗎？`, '確認操作')
    // await AdminApi.resetUserPassword(user.id)
    ElMessage.success('密碼重置成功，新密碼已發送至用戶郵箱')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重置密碼失敗')
    }
  }
}

const deleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(
      `確定要刪除用戶 "${user.username}" 嗎？此操作不可撤銷！`,
      '危險操作',
      { type: 'error' }
    )
    // await AdminApi.deleteUser(user.id)
    ElMessage.success('用戶已刪除')
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '刪除用戶失敗')
    }
  }
}

const showCreateUser = () => {
  createUserVisible.value = true
}

const createUser = async () => {
  if (!createUserFormRef.value) return

  try {
    const valid = await createUserFormRef.value.validate()
    if (!valid) return

    createUserLoading.value = true
    // await AdminApi.createUser(createUserForm)
    
    ElMessage.success('用戶創建成功')
    createUserVisible.value = false
    loadUsers()
    
    // 重置表單
    createUserFormRef.value.resetFields()
  } catch (error: any) {
    ElMessage.error(error.message || '創建用戶失敗')
  } finally {
    createUserLoading.value = false
  }
}

const exportUsers = async () => {
  try {
    // await AdminApi.exportUsers(filterForm)
    ElMessage.success('用戶數據導出中，請稍後查看下載文件')
  } catch (error: any) {
    ElMessage.error(error.message || '導出失敗')
  }
}

const batchDisableUsers = async () => {
  try {
    await ElMessageBox.confirm(`確定要禁用選中的 ${selectedUsers.value.length} 個用戶嗎？`, '批量操作')
    const userIds = selectedUsers.value.map(user => user.id)
    // await AdminApi.batchDisableUsers(userIds)
    ElMessage.success('批量禁用成功')
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量禁用失敗')
    }
  }
}

const batchEnableUsers = async () => {
  try {
    const userIds = selectedUsers.value.map(user => user.id)
    // await AdminApi.batchEnableUsers(userIds)
    ElMessage.success('批量啟用成功')
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '批量啟用失敗')
  }
}

const batchDeleteUsers = async () => {
  try {
    await ElMessageBox.confirm(
      `確定要刪除選中的 ${selectedUsers.value.length} 個用戶嗎？此操作不可撤銷！`,
      '危險操作',
      { type: 'error' }
    )
    const userIds = selectedUsers.value.map(user => user.id)
    // await AdminApi.batchDeleteUsers(userIds)
    ElMessage.success('批量刪除成功')
    loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量刪除失敗')
    }
  }
}

// 初始化
onMounted(() => {
  loadUsers()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.user-list-view {
  .filter-card {
    margin-bottom: 24px;

    .el-form-item {
      margin-bottom: 16px;
    }
  }

  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      padding: 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;

      .stat-content {
        .stat-value {
          font-size: 32px;
          font-weight: 600;
          color: var(--el-color-primary);
          margin-bottom: 8px;
        }

        .stat-label {
          font-size: 14px;
          color: var(--el-text-color-secondary);
        }
      }

      .stat-icon {
        font-size: 48px;
        color: var(--el-color-primary-light-5);
      }
    }
  }

  .table-card {
    margin-bottom: 24px;

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .user-details {
        .username {
          font-weight: 500;
          color: var(--el-text-color-primary);
          margin-bottom: 4px;
        }

        .email {
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }
      }
    }

    .phone-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .danger-item {
      color: var(--el-color-danger);
    }

    .pagination-wrapper {
      margin-top: 24px;
      text-align: center;
    }
  }

  .batch-actions {
    position: sticky;
    bottom: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--el-color-warning-light-9);
    border: 1px solid var(--el-color-warning);

    .batch-info {
      font-weight: 500;
      color: var(--el-color-warning-dark-2);
    }

    .batch-buttons {
      display: flex;
      gap: 12px;
    }
  }
}

@media (max-width: 768px) {
  .user-list-view {
    .stats-row {
      .el-col {
        margin-bottom: 16px;
      }
    }

    .filter-card {
      .el-form {
        .el-form-item {
          width: 100%;
        }
      }
    }

    .table-header {
      flex-direction: column;
      gap: 16px;
      align-items: flex-start !important;
    }

    .batch-actions {
      flex-direction: column;
      gap: 16px;
      align-items: flex-start !important;
    }
  }
}
</style>