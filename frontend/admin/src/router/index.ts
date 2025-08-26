import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { isAuthenticated, getRedirectPath, setRedirectPath, removeRedirectPath } from '@/utils/auth'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 配置进度条
NProgress.configure({ 
  showSpinner: false,
  minimum: 0.1,
  speed: 500
})

const router = createRouter({
  history: createWebHistory('/admin'),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    // 认证相关路由
    {
      path: '/login',
      name: 'AdminLogin',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: {
        title: '管理员登录',
        requiresGuest: true,
        layout: 'auth'
      }
    },
    // 主应用路由 - 使用AdminLayout布局
    {
      path: '/dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: {
        title: '控制面板',
        requiresAuth: true,
        icon: 'Odometer',
        layout: 'admin'
      }
    },
    // 用户管理
    {
      path: '/users',
      name: 'UserManagement',
      redirect: '/users/list',
      meta: {
        title: '用户管理',
        requiresAuth: true,
        requiresPermission: 'manage_users',
        icon: 'User',
        layout: 'admin'
      },
      children: [
        {
          path: 'list',
          name: 'UserList',
          component: () => import('@/views/users/UserListView.vue'),
          meta: {
            title: '用户列表',
            requiresAuth: true,
            requiresPermission: 'manage_users'
          }
        },
        {
          path: 'detail/:id',
          name: 'UserDetail',
          component: () => import('@/views/users/UserDetailView.vue'),
          meta: {
            title: '用户详情',
            requiresAuth: true,
            requiresPermission: 'manage_users'
          }
        }
      ]
    },
    // KYC管理
    {
      path: '/kyc',
      name: 'KYCManagement',
      redirect: '/kyc/pending',
      meta: {
        title: 'KYC管理',
        requiresAuth: true,
        requiresPermission: 'manage_kyc',
        icon: 'CreditCard',
        layout: 'admin'
      },
      children: [
        {
          path: 'pending',
          name: 'KYCPending',
          component: () => import('@/views/kyc/KYCPendingView.vue'),
          meta: {
            title: '待审核KYC',
            requiresAuth: true,
            requiresPermission: 'manage_kyc'
          }
        },
        {
          path: 'reviewed',
          name: 'KYCReviewed',
          component: () => import('@/views/kyc/KYCReviewedView.vue'),
          meta: {
            title: 'KYC审核记录',
            requiresAuth: true,
            requiresPermission: 'manage_kyc'
          }
        },
        {
          path: 'review/:id',
          name: 'KYCReview',
          component: () => import('@/views/kyc/KYCReviewView.vue'),
          meta: {
            title: 'KYC审核',
            requiresAuth: true,
            requiresPermission: 'manage_kyc'
          }
        },
        {
          path: 'management',
          name: 'KYCManagementMain',
          component: () => import('@/views/kyc/KycManagementView.vue'),
          meta: {
            title: 'KYC管理',
            requiresAuth: true,
            requiresPermission: 'manage_kyc'
          }
        }
      ]
    },
    // 订单管理
    {
      path: '/orders',
      name: 'OrderManagement',
      redirect: '/orders/list',
      meta: {
        title: '订单管理',
        requiresAuth: true,
        requiresPermission: 'manage_orders',
        icon: 'List',
        layout: 'admin'
      },
      children: [
        {
          path: 'list',
          name: 'OrderList',
          component: () => import('@/views/orders/OrderListView.vue'),
          meta: {
            title: '订单列表',
            requiresAuth: true,
            requiresPermission: 'manage_orders'
          }
        },
        {
          path: 'detail/:id',
          name: 'OrderDetail',
          component: () => import('@/views/orders/OrderDetailView.vue'),
          meta: {
            title: '订单详情',
            requiresAuth: true,
            requiresPermission: 'manage_orders'
          }
        }
      ]
    },
    // 提现管理
    {
      path: '/withdrawals',
      name: 'WithdrawalManagement',
      redirect: '/withdrawals/pending',
      meta: {
        title: '提现管理',
        requiresAuth: true,
        requiresPermission: 'manage_withdrawals',
        icon: 'Money',
        layout: 'admin'
      },
      children: [
        {
          path: 'pending',
          name: 'WithdrawalPending',
          component: () => import('@/views/withdrawals/WithdrawalPendingView.vue'),
          meta: {
            title: '待处理提现',
            requiresAuth: true,
            requiresPermission: 'manage_withdrawals'
          }
        },
        {
          path: 'history',
          name: 'WithdrawalHistory',
          component: () => import('@/views/withdrawals/WithdrawalHistoryView.vue'),
          meta: {
            title: '提现记录',
            requiresAuth: true,
            requiresPermission: 'manage_withdrawals'
          }
        }
      ]
    },
    // 财务管理
    {
      path: '/finance',
      name: 'FinanceManagement',
      redirect: '/finance/overview',
      meta: {
        title: '财务管理',
        requiresAuth: true,
        requiresPermission: 'manage_finance',
        icon: 'Wallet',
        layout: 'admin'
      },
      children: [
        {
          path: 'overview',
          name: 'FinanceOverview',
          component: () => import('@/views/finance/FinanceOverviewView.vue'),
          meta: {
            title: '财务概览',
            requiresAuth: true,
            requiresPermission: 'manage_finance'
          }
        },
        {
          path: 'transactions',
          name: 'TransactionHistory',
          component: () => import('@/views/finance/TransactionHistoryView.vue'),
          meta: {
            title: '交易记录',
            requiresAuth: true,
            requiresPermission: 'manage_finance'
          }
        }
      ]
    },
    // 系统管理
    {
      path: '/system',
      name: 'SystemManagement',
      redirect: '/system/config',
      meta: {
        title: '系统管理',
        requiresAuth: true,
        requiresPermission: 'manage_system',
        icon: 'Setting',
        layout: 'admin'
      },
      children: [
        {
          path: 'config',
          name: 'SystemConfig',
          component: () => import('@/views/system/SystemConfigView.vue'),
          meta: {
            title: '系统配置',
            requiresAuth: true,
            requiresPermission: 'manage_system'
          }
        },
        {
          path: 'announcements',
          name: 'AnnouncementManagement',
          component: () => import('@/views/system/AnnouncementManagementView.vue'),
          meta: {
            title: '公告管理',
            requiresAuth: true,
            requiresPermission: 'manage_announcements'
          }
        },
        {
          path: 'logs',
          name: 'SystemLogs',
          component: () => import('@/views/system/SystemLogsView.vue'),
          meta: {
            title: '系统日志',
            requiresAuth: true,
            requiresPermission: 'view_logs'
          }
        }
      ]
    },
    // 管理员管理（仅超级管理员可访问）
    {
      path: '/admins',
      name: 'AdminManagement',
      redirect: '/admins/list',
      meta: {
        title: '管理员管理',
        requiresAuth: true,
        requiresRole: 'super_admin',
        icon: 'UserFilled',
        layout: 'admin'
      },
      children: [
        {
          path: 'list',
          name: 'AdminList',
          component: () => import('@/views/admins/AdminListView.vue'),
          meta: {
            title: '管理员列表',
            requiresAuth: true,
            requiresRole: 'super_admin'
          }
        },
        {
          path: 'create',
          name: 'AdminCreate',
          component: () => import('@/views/admins/AdminCreateView.vue'),
          meta: {
            title: '创建管理员',
            requiresAuth: true,
            requiresRole: 'super_admin'
          }
        }
      ]
    },
    // 个人设置
    {
      path: '/profile',
      name: 'AdminProfile',
      component: () => import('@/views/profile/ProfileView.vue'),
      meta: {
        title: '个人设置',
        requiresAuth: true,
        layout: 'admin'
      }
    },
    // 404页面
    {
      path: '/:pathMatch(.*)*',
      name: 'AdminNotFound',
      component: () => import('@/views/error/NotFoundView.vue'),
      meta: {
        title: '页面不存在',
        layout: 'admin'
      }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 开始进度条
  NProgress.start()
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - USDT交易平台管理后台`
  }

  // 检查认证要求
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresGuest = to.matched.some(record => record.meta.requiresGuest)
  const requiresPermission = to.meta.requiresPermission as string
  const requiresRole = to.meta.requiresRole as string
  
  const authenticated = isAuthenticated()

  // 如果需要游客状态但已登录，重定向到控制面板
  if (requiresGuest && authenticated) {
    next('/dashboard')
    return
  }

  // 如果需要认证但未登录，重定向到登录页
  if (requiresAuth && !authenticated) {
    // 保存原始要访问的路径
    if (to.path !== '/login') {
      setRedirectPath(to.fullPath)
    }
    
    ElMessage.warning('请先登录')
    next('/login')
    return
  }

  // 如果需要特定权限
  if (requiresPermission && authenticated) {
    try {
      const authStore = useAuthStore()
      
      if (!authStore.hasPermission(requiresPermission)) {
        ElMessage.error('权限不足，无法访问该页面')
        next('/dashboard')
        return
      }
    } catch (error) {
      console.error('检查权限失败:', error)
      next('/login')
      return
    }
  }

  // 如果需要特定角色
  if (requiresRole && authenticated) {
    try {
      const authStore = useAuthStore()
      
      if (!authStore.hasRole(requiresRole as any)) {
        ElMessage.error('权限不足，无法访问该页面')
        next('/dashboard')
        return
      }
    } catch (error) {
      console.error('检查角色失败:', error)
      next('/login')
      return
    }
  }

  next()
})

router.afterEach((to, from) => {
  // 完成进度条
  NProgress.done()
  
  // 处理登录后的重定向
  if (to.name === 'AdminDashboard' && from.name === 'AdminLogin') {
    const redirectPath = getRedirectPath()
    if (redirectPath && redirectPath !== '/dashboard') {
      removeRedirectPath()
      router.replace(redirectPath)
    }
  }
  
  // 滚动到顶部
  window.scrollTo(0, 0)
  
  // 记录页面访问日志（如果已登录）
  if (isAuthenticated()) {
    const authStore = useAuthStore()
    authStore.logAdminAction('page_visit', to.path, {
      from: from.path,
      title: to.meta.title
    }).catch(() => {
      // 忽略日志记录错误
    })
  }
})

// 路由错误处理
router.onError((error) => {
  console.error('管理后台路由错误:', error)
  NProgress.done()
  ElMessage.error('页面加载失败，请重试')
})

export default router