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
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    // 认证相关路由
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: {
        title: '用户登录',
        requiresGuest: true,
        layout: 'auth'
      }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: {
        title: '用户注册',
        requiresGuest: true,
        layout: 'auth'
      }
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/auth/ForgotPasswordView.vue'),
      meta: {
        title: '忘记密码',
        requiresGuest: true,
        layout: 'auth'
      }
    },
    {
      path: '/reset-password',
      name: 'ResetPassword',
      component: () => import('@/views/auth/ResetPasswordView.vue'),
      meta: {
        title: '重置密码',
        requiresGuest: true,
        layout: 'auth'
      }
    },
    {
      path: '/verify-email',
      name: 'VerifyEmail',
      component: () => import('@/views/auth/VerifyEmailView.vue'),
      meta: {
        title: '邮箱验证',
        layout: 'auth'
      }
    },
    // 主应用路由
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: {
        title: '仪表板',
        requiresAuth: true,
        icon: 'House'
      }
    },
    // 个人中心
    {
      path: '/profile',
      name: 'Profile',
      redirect: '/profile/info',
      meta: {
        title: '个人中心',
        requiresAuth: true,
        icon: 'User'
      },
      children: [
        {
          path: 'info',
          name: 'ProfileInfo',
          component: () => import('@/views/profile/ProfileInfoView.vue'),
          meta: {
            title: '个人信息',
            requiresAuth: true
          }
        },
        {
          path: 'security',
          name: 'ProfileSecurity',
          component: () => import('@/views/profile/ProfileSecurityView.vue'),
          meta: {
            title: '安全设置',
            requiresAuth: true
          }
        },
        {
          path: 'notifications',
          name: 'ProfileNotifications',
          component: () => import('@/views/profile/ProfileNotificationsView.vue'),
          meta: {
            title: '消息通知',
            requiresAuth: true
          }
        }
      ]
    },
    // KYC验证
    {
      path: '/kyc',
      name: 'KYC',
      redirect: '/kyc/status',
      meta: {
        title: 'KYC验证',
        requiresAuth: true,
        icon: 'CreditCard'
      },
      children: [
        {
          path: 'status',
          name: 'KYCStatus',
          component: () => import('@/views/kyc/KYCStatusView.vue'),
          meta: {
            title: 'KYC状态',
            requiresAuth: true
          }
        },
        {
          path: 'submit',
          name: 'KYCSubmit',
          component: () => import('@/views/kyc/KYCSubmitView.vue'),
          meta: {
            title: '提交KYC',
            requiresAuth: true
          }
        },
        {
          path: 'verification',
          name: 'KYCVerification',
          component: () => import('@/views/kyc/KycView.vue'),
          meta: {
            title: 'KYC驗證',
            requiresAuth: true
          }
        }
      ]
    },
    // 钱包管理
    {
      path: '/wallet',
      name: 'Wallet',
      redirect: '/wallet/overview',
      meta: {
        title: '钱包管理',
        requiresAuth: true,
        icon: 'Wallet'
      },
      children: [
        {
          path: 'overview',
          name: 'WalletOverview',
          component: () => import('@/views/wallet/WalletOverviewView.vue'),
          meta: {
            title: '钱包概览',
            requiresAuth: true
          }
        },
        {
          path: 'balance',
          name: 'WalletBalance',
          component: () => import('@/views/wallet/WalletView.vue'),
          meta: {
            title: '錢包管理',
            requiresAuth: true
          }
        },
        {
          path: 'deposit',
          name: 'WalletDeposit',
          component: () => import('@/views/wallet/WalletDepositView.vue'),
          meta: {
            title: '充值',
            requiresAuth: true
          }
        },
        {
          path: 'withdraw',
          name: 'WalletWithdraw',
          component: () => import('@/views/wallet/WalletWithdrawView.vue'),
          meta: {
            title: '提现',
            requiresAuth: true,
            requiresKYC: true
          }
        },
        {
          path: 'transactions',
          name: 'WalletTransactions',
          component: () => import('@/views/wallet/WalletTransactionsView.vue'),
          meta: {
            title: '交易记录',
            requiresAuth: true
          }
        }
      ]
    },
    // 交易相关
    {
      path: '/trading',
      name: 'Trading',
      redirect: '/trading/spot',
      meta: {
        title: '交易中心',
        requiresAuth: true,
        icon: 'TrendCharts'
      },
      children: [
        {
          path: 'spot',
          name: 'SpotTrading',
          component: () => import('@/views/trading/SpotTradingView.vue'),
          meta: {
            title: '现货交易',
            requiresAuth: true,
            requiresKYC: true
          }
        },
        {
          path: 'usdt',
          name: 'USDTTrading',
          component: () => import('@/views/trading/TradingView.vue'),
          meta: {
            title: 'USDT交易',
            requiresAuth: true,
            requiresKYC: true
          }
        },
        {
          path: 'orders',
          name: 'TradingOrders',
          component: () => import('@/views/trading/TradingOrdersView.vue'),
          meta: {
            title: '订单管理',
            requiresAuth: true
          }
        },
        {
          path: 'order-management',
          name: 'OrderManagement',
          component: () => import('@/views/orders/OrdersView.vue'),
          meta: {
            title: '訂單管理',
            requiresAuth: true
          }
        },
        {
          path: 'history',
          name: 'TradingHistory',
          component: () => import('@/views/trading/TradingHistoryView.vue'),
          meta: {
            title: '交易历史',
            requiresAuth: true
          }
        }
      ]
    },
    // 帮助和支持
    {
      path: '/help',
      name: 'Help',
      component: () => import('@/views/help/HelpView.vue'),
      meta: {
        title: '帮助中心',
        icon: 'QuestionFilled'
      }
    },
    // 404页面
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/NotFoundView.vue'),
      meta: {
        title: '页面不存在'
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
    document.title = `${to.meta.title} - USDT交易平台`
  }

  // 检查认证要求
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresGuest = to.matched.some(record => record.meta.requiresGuest)
  const requiresKYC = to.matched.some(record => record.meta.requiresKYC)
  
  const authenticated = isAuthenticated()

  // 如果需要游客状态但已登录，重定向到仪表板
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

  // 如果需要KYC验证
  if (requiresKYC && authenticated) {
    try {
      const authStore = useAuthStore()
      const user = authStore.user
      
      if (!user) {
        next('/login')
        return
      }

      // 检查KYC状态（这里需要从后端获取最新状态）
      // 暂时简化处理
      const kycStatus = 'approved' // 实际应该从API获取
      
      if (kycStatus !== 'approved') {
        ElMessage.warning('该功能需要完成KYC验证')
        next('/kyc/status')
        return
      }
    } catch (error) {
      console.error('检查KYC状态失败:', error)
      next('/kyc/status')
      return
    }
  }

  next()
})

router.afterEach((to, from) => {
  // 完成进度条
  NProgress.done()
  
  // 处理登录后的重定向
  if (to.name === 'Dashboard' && from.name === 'Login') {
    const redirectPath = getRedirectPath()
    if (redirectPath && redirectPath !== '/dashboard') {
      removeRedirectPath()
      router.replace(redirectPath)
    }
  }
  
  // 滚动到顶部
  window.scrollTo(0, 0)
})

// 路由错误处理
router.onError((error) => {
  console.error('路由错误:', error)
  NProgress.done()
  ElMessage.error('页面加载失败，请重试')
})

export default router