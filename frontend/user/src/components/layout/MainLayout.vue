<template>
  <div class="main-layout">
    <!-- 側邊欄 -->
    <aside 
      :class="[
        'sidebar',
        { 'sidebar-collapsed': !sidebarExpanded }
      ]"
    >
      <div class="sidebar-header">
        <div class="logo">
          <img src="/logo.svg" alt="USDT Trading" class="logo-image" />
          <span v-show="sidebarExpanded" class="logo-text">USDT 交易平台</span>
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
            </router-link>

            <!-- 子菜單 -->
            <div v-else class="nav-group">
              <div class="nav-group-header" @click="toggleSubMenu(item.name)">
                <el-icon class="nav-icon">
                  <component :is="item.icon" />
                </el-icon>
                <span v-show="sidebarExpanded" class="nav-text">{{ item.title }}</span>
                <el-icon v-show="sidebarExpanded" class="nav-arrow" :class="{ expanded: expandedSubMenus.includes(item.name) }">
                  <ArrowDown />
                </el-icon>
              </div>
              <ul v-show="sidebarExpanded && expandedSubMenus.includes(item.name)" class="sub-nav-list">
                <li v-for="child in item.children" :key="child.name" class="sub-nav-item">
                  <router-link :to="child.path" class="sub-nav-link">
                    <span>{{ child.title }}</span>
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
      <header class="top-header">
        <div class="header-left">
          <h1 class="page-title">{{ currentPageTitle }}</h1>
        </div>
        
        <div class="header-right">
          <!-- 通知圖標 -->
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
            <el-button circle class="header-btn">
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

          <!-- 用戶菜單 -->
          <el-dropdown class="user-dropdown" trigger="click">
            <div class="user-info">
              <el-avatar :size="32" :src="user?.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ user?.username || 'User' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon><User /></el-icon>
                  個人資料
                </el-dropdown-item>
                <el-dropdown-item @click="goToSecurity">
                  <el-icon><Lock /></el-icon>
                  安全設置
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import {
  House,
  User,
  Wallet,
  TrendCharts,
  CreditCard,
  Bell,
  Moon,
  Sunny,
  ArrowDown,
  Expand,
  Fold,
  Lock,
  SwitchButton,
  QuestionFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

// 響應式狀態
const sidebarExpanded = ref(true)
const expandedSubMenus = ref<string[]>(['wallet', 'trading'])
const notificationCount = ref(0)

// 計算屬性
const user = computed(() => authStore.user)
const isDark = computed(() => themeStore.isDark)
const currentPageTitle = computed(() => route.meta.title as string || '儀表板')

// 導航菜單項目
const navigationItems = [
  {
    name: 'dashboard',
    path: '/dashboard',
    title: '儀表板',
    icon: House
  },
  {
    name: 'wallet',
    title: '錢包管理',
    icon: Wallet,
    children: [
      { name: 'wallet-overview', path: '/wallet/overview', title: '錢包概覽' },
      { name: 'wallet-balance', path: '/wallet/balance', title: '餘額查詢' },
      { name: 'wallet-deposit', path: '/wallet/deposit', title: '充值' },
      { name: 'wallet-withdraw', path: '/wallet/withdraw', title: '提現' },
      { name: 'wallet-transactions', path: '/wallet/transactions', title: '交易記錄' }
    ]
  },
  {
    name: 'trading',
    title: '交易中心',
    icon: TrendCharts,
    children: [
      { name: 'spot-trading', path: '/trading/spot', title: '現貨交易' },
      { name: 'usdt-trading', path: '/trading/usdt', title: 'USDT交易' },
      { name: 'trading-orders', path: '/trading/orders', title: '訂單管理' },
      { name: 'trading-history', path: '/trading/history', title: '交易歷史' }
    ]
  },
  {
    name: 'kyc',
    title: 'KYC驗證',
    icon: CreditCard,
    children: [
      { name: 'kyc-status', path: '/kyc/status', title: 'KYC狀態' },
      { name: 'kyc-submit', path: '/kyc/submit', title: '提交KYC' },
      { name: 'kyc-verification', path: '/kyc/verification', title: 'KYC驗證' }
    ]
  },
  {
    name: 'profile',
    title: '個人中心',
    icon: User,
    children: [
      { name: 'profile-info', path: '/profile/info', title: '個人信息' },
      { name: 'profile-security', path: '/profile/security', title: '安全設置' },
      { name: 'profile-notifications', path: '/profile/notifications', title: '消息通知' }
    ]
  },
  {
    name: 'help',
    path: '/help',
    title: '幫助中心',
    icon: QuestionFilled
  }
]

// 方法
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

const goToProfile = () => {
  router.push('/profile/info')
}

const goToSecurity = () => {
  router.push('/profile/security')
}

const logout = async () => {
  await authStore.logout()
  router.push('/login')
}

// 初始化
onMounted(() => {
  // 加載通知數量
  // 這裡可以添加獲取通知數量的API調用
})
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  background: var(--el-bg-color);
}

.sidebar {
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
      color: var(--el-text-color-primary);
    }
  }

  .sidebar-toggle {
    background: none;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: var(--el-text-color-regular);

    &:hover {
      color: var(--el-color-primary);
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
    font-size: 14px;
    font-weight: 500;
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
    display: block;
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

.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color);

  .header-left {
    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

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

    .user-dropdown {
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        border-radius: 6px;
        cursor: pointer;
        transition: background-color 0.3s ease;

        &:hover {
          background: var(--el-fill-color-lighter);
        }

        .username {
          font-size: 14px;
          font-weight: 500;
          color: var(--el-text-color-primary);
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .sidebar {
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

  .top-header {
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