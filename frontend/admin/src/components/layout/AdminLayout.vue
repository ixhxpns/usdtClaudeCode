<template>
  <div class="admin-layout">
    <!-- 側邊欄 -->
    <aside 
      :class="[
        'admin-sidebar',
        { 'sidebar-collapsed': !sidebarExpanded }
      ]"
    >
      <div class="sidebar-header">
        <div class="logo">
          <img src="/admin-logo.svg" alt="Admin Panel" class="logo-image" />
          <span v-show="sidebarExpanded" class="logo-text">管理後台</span>
        </div>
        <button 
          class="sidebar-toggle"
          @click="toggleSidebar"
        >
          <el-icon>
            <Expand v-if="!sidebarExpanded" />
            <Fold v-else />
          </el-icon>
        </button>
      </div>

      <nav class="sidebar-nav">
        <ul class="nav-list">
          <li 
            v-for="item in navigationItems" 
            :key="item.name"
            class="nav-item"
            v-show="hasPermissionForItem(item)"
          >
            <router-link
              v-if="!item.children"
              :to="item.path"
              class="nav-link"
              :class="{ active: $route.path === item.path }"
            >
              <el-icon class="nav-icon">
                <component :is="item.icon" />
              </el-icon>
              <span v-show="sidebarExpanded" class="nav-text">{{ item.title }}</span>
              <el-badge v-if="item.badge" :value="item.badge" class="nav-badge" />
            </router-link>

            <!-- 子菜單 -->
            <div v-else class="nav-group">
              <div class="nav-group-header" @click="toggleSubMenu(item.name)">
                <el-icon class="nav-icon">
                  <component :is="item.icon" />
                </el-icon>
                <span v-show="sidebarExpanded" class="nav-text">{{ item.title }}</span>
                <el-badge v-if="item.badge" :value="item.badge" class="nav-badge" />
                <el-icon v-show="sidebarExpanded" class="nav-arrow" :class="{ expanded: expandedSubMenus.includes(item.name) }">
                  <ArrowDown />
                </el-icon>
              </div>
              <ul v-show="sidebarExpanded && expandedSubMenus.includes(item.name)" class="sub-nav-list">
                <li 
                  v-for="child in item.children" 
                  :key="child.name" 
                  class="sub-nav-item"
                  v-show="hasPermissionForItem(child)"
                >
                  <router-link :to="child.path" class="sub-nav-link">
                    <span>{{ child.title }}</span>
                    <el-badge v-if="child.badge" :value="child.badge" class="nav-badge" />
                  </router-link>
                </li>
              </ul>
            </div>
          </li>
        </ul>
      </nav>
    </aside>

    <!-- 主內容區域 -->
    <div class="main-content">
      <!-- 頂部導航欄 -->
      <header class="admin-header">
        <div class="header-left">
          <h1 class="page-title">{{ currentPageTitle }}</h1>
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path">
                <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
                <span v-else>{{ item.title }}</span>
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        
        <div class="header-right">
          <!-- 系統狀態指示器 -->
          <div class="system-status">
            <el-tooltip content="系統狀態" placement="bottom">
              <div :class="['status-indicator', systemStatus]">
                <el-icon><CircleCheckFilled v-if="systemStatus === 'healthy'" /><Warning v-else /></el-icon>
              </div>
            </el-tooltip>
          </div>

          <!-- 待處理通知 -->
          <el-badge :value="pendingCount" :hidden="pendingCount === 0" class="notification-badge">
            <el-button circle class="header-btn" @click="showPendingTasks">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>

          <!-- 主題切換 -->
          <el-button circle class="header-btn" @click="toggleTheme">
            <el-icon>
              <Sunny v-if="isDark" />
              <Moon v-else />
            </el-icon>
          </el-button>

          <!-- 管理員菜單 -->
          <el-dropdown class="admin-dropdown" trigger="click">
            <div class="admin-info">
              <el-avatar :size="36" :src="admin?.avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <div v-show="sidebarExpanded" class="admin-details">
                <span class="admin-name">{{ admin?.name || 'Admin' }}</span>
                <span class="admin-role">{{ roleDisplayName }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon><User /></el-icon>
                  個人設置
                </el-dropdown-item>
                <el-dropdown-item @click="viewLogs" v-if="canViewLogs">
                  <el-icon><Document /></el-icon>
                  操作日誌
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>
                  登出
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 內容區域 -->
      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 待處理任務抽屜 -->
    <el-drawer
      v-model="showPendingDrawer"
      title="待處理任務"
      direction="rtl"
      size="400px"
    >
      <div class="pending-tasks">
        <div v-for="task in pendingTasks" :key="task.id" class="task-item">
          <div class="task-icon">
            <el-icon><component :is="task.icon" /></el-icon>
          </div>
          <div class="task-content">
            <div class="task-title">{{ task.title }}</div>
            <div class="task-desc">{{ task.description }}</div>
            <div class="task-time">{{ formatTime(task.createdAt) }}</div>
          </div>
          <el-button size="small" @click="handleTask(task)">處理</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { dayjs } from 'element-plus'
import {
  Odometer,
  User,
  UserFilled,
  List,
  Money,
  Wallet,
  CreditCard,
  Setting,
  Bell,
  Moon,
  Sunny,
  ArrowDown,
  Expand,
  Fold,
  SwitchButton,
  Document,
  CircleCheckFilled,
  Warning
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

// 響應式狀態
const sidebarExpanded = ref(true)
const expandedSubMenus = ref<string[]>(['users', 'kyc', 'orders', 'system'])
const showPendingDrawer = ref(false)
const systemStatus = ref<'healthy' | 'warning' | 'error'>('healthy')
const pendingTasks = ref<any[]>([])

// 計算屬性
const admin = computed(() => authStore.admin)
const isDark = computed(() => themeStore.isDark)
const currentPageTitle = computed(() => route.meta.title as string || '控制面板')
const roleDisplayName = computed(() => {
  switch (authStore.adminRole) {
    case 'super_admin': return '超級管理員'
    case 'admin': return '管理員'
    case 'moderator': return '審核員'
    case 'viewer': return '查看員'
    default: return '未知角色'
  }
})

const canViewLogs = computed(() => authStore.hasPermission('view_logs'))
const pendingCount = computed(() => pendingTasks.value.length)

const breadcrumbItems = computed(() => {
  const items = []
  const pathArray = route.path.split('/').filter(p => p)
  
  items.push({ title: '首頁', path: '/dashboard' })
  
  if (pathArray.length > 1) {
    items.push({ title: currentPageTitle.value, path: '' })
  }
  
  return items
})

// 導航菜單項目
const navigationItems = [
  {
    name: 'dashboard',
    path: '/dashboard',
    title: '控制面板',
    icon: Odometer
  },
  {
    name: 'users',
    title: '用戶管理',
    icon: User,
    permission: 'manage_users',
    children: [
      { name: 'user-list', path: '/users/list', title: '用戶列表', permission: 'manage_users' },
      { name: 'user-detail', path: '/users/detail', title: '用戶詳情', permission: 'manage_users' }
    ]
  },
  {
    name: 'kyc',
    title: 'KYC管理',
    icon: CreditCard,
    permission: 'manage_kyc',
    children: [
      { name: 'kyc-pending', path: '/kyc/pending', title: '待審核KYC', permission: 'manage_kyc', badge: 0 },
      { name: 'kyc-reviewed', path: '/kyc/reviewed', title: 'KYC審核記錄', permission: 'manage_kyc' },
      { name: 'kyc-management', path: '/kyc/management', title: 'KYC管理', permission: 'manage_kyc' }
    ]
  },
  {
    name: 'orders',
    title: '訂單管理',
    icon: List,
    permission: 'manage_orders',
    children: [
      { name: 'order-list', path: '/orders/list', title: '訂單列表', permission: 'manage_orders' },
      { name: 'order-detail', path: '/orders/detail', title: '訂單詳情', permission: 'manage_orders' }
    ]
  },
  {
    name: 'withdrawals',
    title: '提現管理',
    icon: Money,
    permission: 'manage_withdrawals',
    children: [
      { name: 'withdrawal-pending', path: '/withdrawals/pending', title: '待處理提現', permission: 'manage_withdrawals', badge: 0 },
      { name: 'withdrawal-history', path: '/withdrawals/history', title: '提現記錄', permission: 'manage_withdrawals' }
    ]
  },
  {
    name: 'finance',
    title: '財務管理',
    icon: Wallet,
    permission: 'manage_finance',
    children: [
      { name: 'finance-overview', path: '/finance/overview', title: '財務概覽', permission: 'manage_finance' },
      { name: 'transaction-history', path: '/finance/transactions', title: '交易記錄', permission: 'manage_finance' }
    ]
  },
  {
    name: 'system',
    title: '系統管理',
    icon: Setting,
    permission: 'manage_system',
    children: [
      { name: 'system-config', path: '/system/config', title: '系統配置', permission: 'manage_system' },
      { name: 'announcement-management', path: '/system/announcements', title: '公告管理', permission: 'manage_announcements' },
      { name: 'system-logs', path: '/system/logs', title: '系統日誌', permission: 'view_logs' }
    ]
  },
  {
    name: 'admins',
    title: '管理員管理',
    icon: UserFilled,
    role: 'super_admin',
    children: [
      { name: 'admin-list', path: '/admins/list', title: '管理員列表', role: 'super_admin' },
      { name: 'admin-create', path: '/admins/create', title: '創建管理員', role: 'super_admin' }
    ]
  }
]

// 方法
const hasPermissionForItem = (item: any) => {
  if (item.role) {
    return authStore.hasRole(item.role)
  }
  if (item.permission) {
    return authStore.hasPermission(item.permission)
  }
  return true
}

const toggleSidebar = () => {
  sidebarExpanded.value = !sidebarExpanded.value
}

const toggleSubMenu = (menuName: string) => {
  const index = expandedSubMenus.value.indexOf(menuName)
  if (index > -1) {
    expandedSubMenus.value.splice(index, 1)
  } else {
    expandedSubMenus.value.push(menuName)
  }
}

const toggleTheme = () => {
  themeStore.toggleTheme()
}

const showPendingTasks = () => {
  showPendingDrawer.value = true
}

const goToProfile = () => {
  router.push('/profile')
}

const viewLogs = () => {
  router.push('/system/logs')
}

const logout = async () => {
  await authStore.logout()
  router.push('/login')
}

const handleTask = (task: any) => {
  // 處理具體任務
  router.push(task.path)
  showPendingDrawer.value = false
}

const formatTime = (time: string) => {
  return dayjs(time).fromNow()
}

const loadPendingTasks = async () => {
  // 載入待處理任務
  try {
    // 這裡應該調用API獲取待處理任務
    pendingTasks.value = []
  } catch (error) {
    console.error('載入待處理任務失敗:', error)
  }
}

const checkSystemStatus = async () => {
  // 檢查系統狀態
  try {
    // 這裡應該調用API檢查系統狀態
    systemStatus.value = 'healthy'
  } catch (error) {
    console.error('檢查系統狀態失敗:', error)
    systemStatus.value = 'error'
  }
}

// 初始化
onMounted(() => {
  loadPendingTasks()
  checkSystemStatus()
  
  // 定期刷新待處理任務和系統狀態
  const interval = setInterval(() => {
    loadPendingTasks()
    checkSystemStatus()
  }, 30000) // 30秒刷新一次

  // 組件卸載時清除定時器
  onUnmounted(() => {
    clearInterval(interval)
  })
})

// 監聽路由變化，記錄操作日誌
watch(route, (to, from) => {
  if (to.path !== from.path) {
    authStore.logAdminAction('page_visit', to.path, {
      from: from.path,
      title: to.meta.title
    })
  }
})
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  background: var(--el-bg-color);
}

.admin-sidebar {
  width: 280px;
  background: var(--el-bg-color-page);
  border-right: 1px solid var(--el-border-color);
  transition: width 0.3s ease;
  overflow: hidden;

  &.sidebar-collapsed {
    width: 64px;
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-color-primary);
  color: white;

  .logo {
    display: flex;
    align-items: center;
    gap: 8px;

    .logo-image {
      width: 32px;
      height: 32px;
    }

    .logo-text {
      font-weight: 600;
      font-size: 16px;
    }
  }

  .sidebar-toggle {
    background: none;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: white;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-radius: 4px;
    }
  }
}

.sidebar-nav {
  padding: 16px 0;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  margin-bottom: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  color: var(--el-text-color-regular);
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;

  &:hover,
  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }

  .nav-icon {
    margin-right: 12px;
    font-size: 18px;
  }

  .nav-text {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
  }

  .nav-badge {
    margin-left: auto;
  }
}

.nav-group {
  .nav-group-header {
    display: flex;
    align-items: center;
    padding: 12px 24px;
    color: var(--el-text-color-regular);
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: var(--el-fill-color-light);
    }

    .nav-icon {
      margin-right: 12px;
      font-size: 18px;
    }

    .nav-text {
      flex: 1;
      font-size: 14px;
      font-weight: 500;
    }

    .nav-badge {
      margin-right: 8px;
    }

    .nav-arrow {
      transition: transform 0.3s ease;

      &.expanded {
        transform: rotate(180deg);
      }
    }
  }
}

.sub-nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
  background: var(--el-fill-color-lighter);
}

.sub-nav-item {
  .sub-nav-link {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 24px 8px 56px;
    color: var(--el-text-color-regular);
    text-decoration: none;
    font-size: 13px;
    transition: all 0.3s ease;

    &:hover {
      background: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }

    &.router-link-active {
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color);

  .header-left {
    flex: 1;

    .page-title {
      margin: 0 0 4px 0;
      font-size: 20px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .breadcrumb {
      font-size: 12px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .system-status {
      .status-indicator {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        
        &.healthy {
          background: var(--el-color-success-light-9);
          color: var(--el-color-success);
        }

        &.warning {
          background: var(--el-color-warning-light-9);
          color: var(--el-color-warning);
        }

        &.error {
          background: var(--el-color-danger-light-9);
          color: var(--el-color-danger);
        }
      }
    }

    .header-btn {
      border: none;
      background: var(--el-fill-color-lighter);

      &:hover {
        background: var(--el-fill-color-light);
      }
    }

    .notification-badge {
      .el-button {
        position: relative;
      }
    }

    .admin-dropdown {
      .admin-info {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 12px;
        border-radius: 6px;
        cursor: pointer;
        transition: background-color 0.3s ease;

        &:hover {
          background: var(--el-fill-color-lighter);
        }

        .admin-details {
          display: flex;
          flex-direction: column;
          align-items: flex-start;

          .admin-name {
            font-size: 14px;
            font-weight: 500;
            color: var(--el-text-color-primary);
          }

          .admin-role {
            font-size: 12px;
            color: var(--el-text-color-secondary);
          }
        }
      }
    }
  }
}

.content-area {
  flex: 1;
  overflow: auto;
  padding: 24px;
  background: var(--el-bg-color);
}

.pending-tasks {
  .task-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &:last-child {
      border-bottom: none;
    }

    .task-icon {
      margin-right: 12px;
      font-size: 20px;
      color: var(--el-color-primary);
    }

    .task-content {
      flex: 1;

      .task-title {
        font-weight: 500;
        color: var(--el-text-color-primary);
        margin-bottom: 4px;
      }

      .task-desc {
        font-size: 13px;
        color: var(--el-text-color-regular);
        margin-bottom: 4px;
      }

      .task-time {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &:not(.sidebar-collapsed) {
      transform: translateX(0);
    }
  }

  .main-content {
    width: 100%;
  }

  .admin-header {
    padding: 0 16px;

    .page-title {
      font-size: 18px;
    }
  }

  .content-area {
    padding: 16px;
  }
}
</style>