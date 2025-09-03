import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { AdminHttpClient } from '@/utils/http'
import { 
  getToken, 
  setToken, 
  removeToken,
  getUserInfo,
  setUserInfo,
  removeUserInfo,
  isAuthenticated as checkAuthenticated,
  clearAuth,
  notifyAuthStateChange
} from '@/utils/auth'
import { encryptSensitiveData } from '@/utils/crypto'
import { fallbackLogin, shouldUseFallback, getFriendlyErrorMessage, reportError } from '@/utils/fallback-auth'
import type { 
  Admin, 
  AdminLoginRequest, 
  AdminLoginResponse,
  AdminRole
} from '@/types/admin'

export const useAuthStore = defineStore('adminAuth', () => {
  // 状态
  const admin = ref<Admin | null>(getUserInfo())
  const isLoading = ref(false)
  const loginAttempts = ref(0)
  const maxLoginAttempts = 5
  const lockoutTime = ref(0)
  const permissions = ref<string[]>([])

  // 计算属性
  const isAuthenticated = computed(() => checkAuthenticated() && admin.value !== null)
  const adminRole = computed(() => admin.value?.role || 'viewer')
  const adminStatus = computed(() => admin.value?.status || 'inactive')
  const isLocked = computed(() => lockoutTime.value > Date.now())
  const isSuperAdmin = computed(() => adminRole.value === 'super_admin')
  const isAdmin = computed(() => ['super_admin', 'admin'].includes(adminRole.value))
  const canManageUsers = computed(() => 
    isSuperAdmin.value || permissions.value.includes('manage_users')
  )
  const canManageOrders = computed(() => 
    isSuperAdmin.value || permissions.value.includes('manage_orders')
  )
  const canManageWithdrawals = computed(() => 
    isSuperAdmin.value || permissions.value.includes('manage_withdrawals')
  )
  const canManageSystem = computed(() => 
    isSuperAdmin.value || permissions.value.includes('manage_system')
  )

  // 登录
  const login = async (credentials: AdminLoginRequest) => {
    if (isLocked.value) {
      const remainingTime = Math.ceil((lockoutTime.value - Date.now()) / 1000 / 60)
      throw new Error(`账户已锁定，请 ${remainingTime} 分钟后重试`)
    }

    if (loginAttempts.value >= maxLoginAttempts) {
      lockoutTime.value = Date.now() + 30 * 60 * 1000 // 锁定30分钟
      throw new Error('登录失败次数过多，账户已锁定30分钟')
    }

    try {
      isLoading.value = true
      
      // 先检测API健康状态
      console.log('🔍 检测API健康状态...');
      const healthCheck = await import('@/utils/crypto').then(m => m.checkAPIHealth());
      console.log('API健康检查结果:', healthCheck);
      
      let response: AdminLoginResponse;
      
      try {
        // 尝试正常的RSA加密登录流程
        console.log('🔐 尝试RSA加密登录...');
        const encryptedPassword = await encryptSensitiveData(credentials.password);
        console.log('✅ RSA密码加密成功');
        
        const encryptedCredentials = {
          ...credentials,
          password: encryptedPassword
        }

        response = await AdminHttpClient.post<AdminLoginResponse>('/admin/auth/login', encryptedCredentials);
        
      } catch (encryptError: any) {
        console.warn('⚠️ RSA登录失败:', encryptError.message);
        
        // 检查是否应该使用降级方案
        if (shouldUseFallback(encryptError)) {
          console.log('🔄 尝试降级登录方案...');
          
          // 报告错误用于诊断
          reportError(encryptError, {
            username: credentials.username,
            step: 'rsa_encryption'
          });
          
          try {
            response = await fallbackLogin(credentials);
            console.log('✅ 降级登录成功');
          } catch (fallbackError: any) {
            console.error('❌ 降级登录也失败:', fallbackError.message);
            throw fallbackError;
          }
        } else {
          // 不适合降级的错误，直接抛出
          throw encryptError;
        }
      }
      
      // 保存认证信息
      setToken(response.access_token)
      setUserInfo(response.admin)
      admin.value = response.admin
      permissions.value = response.admin.permissions || []
      
      // 重置登录尝试次数
      loginAttempts.value = 0
      lockoutTime.value = 0
      
      // 通知认证状态变化
      notifyAuthStateChange(true)
      
      ElMessage.success('管理员登录成功')
      return response
    } catch (error: any) {
      loginAttempts.value++
      
      // 提供用户友好的错误信息
      const message = getFriendlyErrorMessage(error);
      
      console.error('管理员登录失败:', {
        error: error.message,
        attempts: loginAttempts.value,
        credentials: {
          username: credentials.username,
          hasPassword: !!credentials.password,
          hasMfaCode: !!credentials.mfa_code
        }
      });
      
      // 发送错误报告用于诊断
      reportError(error, {
        username: credentials.username,
        attempts: loginAttempts.value,
        step: 'login_final'
      });
      
      ElMessage.error(message);
      throw new Error(message);
    } finally {
      isLoading.value = false
    }
  }

  // 登出
  const logout = async (showMessage = true) => {
    try {
      isLoading.value = true
      
      // 如果有token，调用服务器登出接口
      if (getToken()) {
        await AdminHttpClient.post('/admin/auth/logout').catch(() => {
          // 即使服务器登出失败，也要清除本地数据
        })
      }
      
      // 清除本地认证数据
      clearAuth()
      admin.value = null
      permissions.value = []
      loginAttempts.value = 0
      lockoutTime.value = 0
      
      // 通知认证状态变化
      notifyAuthStateChange(false)
      
      if (showMessage) {
        ElMessage.success('已安全退出')
      }
    } catch (error) {
      console.error('管理员登出错误:', error)
      // 即使登出失败，也要清除本地数据
      clearAuth()
      admin.value = null
      permissions.value = []
      notifyAuthStateChange(false)
    } finally {
      isLoading.value = false
    }
  }

  // 刷新管理员信息
  const refreshAdminInfo = async () => {
    try {
      isLoading.value = true
      const response = await AdminHttpClient.get<Admin>('/admin/auth/me')
      
      setUserInfo(response)
      admin.value = response
      permissions.value = response.permissions || []
      
      return response
    } catch (error: any) {
      const message = error.message || '获取管理员信息失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 修改密码
  const changePassword = async (currentPassword: string, newPassword: string) => {
    try {
      isLoading.value = true
      
      // 加密密码
      const [encryptedCurrentPassword, encryptedNewPassword] = await Promise.all([
        encryptSensitiveData(currentPassword),
        encryptSensitiveData(newPassword)
      ])
      
      const encryptedData = {
        current_password: encryptedCurrentPassword,
        new_password: encryptedNewPassword
      }

      await AdminHttpClient.post('/admin/auth/change-password', encryptedData)
      
      ElMessage.success('密码修改成功，请重新登录')
      
      // 密码修改成功后需要重新登录
      setTimeout(() => {
        logout(false)
        window.location.href = '/admin/login'
      }, 2000)
    } catch (error: any) {
      const message = error.message || '密码修改失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 检查权限
  const hasPermission = (permission: string) => {
    if (isSuperAdmin.value) return true
    return permissions.value.includes(permission)
  }

  // 检查任一权限
  const hasAnyPermission = (permissionList: string[]) => {
    if (isSuperAdmin.value) return true
    return permissionList.some(permission => permissions.value.includes(permission))
  }

  // 检查所有权限
  const hasAllPermissions = (permissionList: string[]) => {
    if (isSuperAdmin.value) return true
    return permissionList.every(permission => permissions.value.includes(permission))
  }

  // 检查角色
  const hasRole = (role: AdminRole) => {
    return adminRole.value === role
  }

  // 检查任一角色
  const hasAnyRole = (roleList: AdminRole[]) => {
    return roleList.includes(adminRole.value)
  }

  // 获取操作日志
  const getAdminLogs = async (params?: {
    start_date?: string
    end_date?: string
    page?: number
    size?: number
  }) => {
    try {
      const response = await AdminHttpClient.get('/admin/logs', params)
      return response
    } catch (error: any) {
      const message = error.message || '获取操作日志失败'
      ElMessage.error(message)
      throw error
    }
  }

  // 记录操作日志
  const logAdminAction = async (action: string, resource: string, details?: any) => {
    try {
      await AdminHttpClient.post('/admin/logs', {
        action,
        resource,
        details: details || {}
      })
    } catch (error) {
      // 日志记录失败不影响主要功能
      console.error('记录操作日志失败:', error)
    }
  }

  // 初始化认证状态
  const initialize = () => {
    const savedAdmin = getUserInfo()
    if (savedAdmin && checkAuthenticated()) {
      admin.value = savedAdmin
      permissions.value = savedAdmin.permissions || []
      notifyAuthStateChange(true)
    } else {
      clearAuth()
      admin.value = null
      permissions.value = []
      notifyAuthStateChange(false)
    }
  }

  // 验证会话有效性
  const validateSession = async () => {
    try {
      await AdminHttpClient.get('/admin/auth/me')
      return true
    } catch (error) {
      // 会话无效，清除本地数据
      await logout(false)
      return false
    }
  }

  return {
    // 状态
    admin: readonly(admin),
    isLoading: readonly(isLoading),
    loginAttempts: readonly(loginAttempts),
    lockoutTime: readonly(lockoutTime),
    permissions: readonly(permissions),
    
    // 计算属性
    isAuthenticated,
    adminRole,
    adminStatus,
    isLocked,
    isSuperAdmin,
    isAdmin,
    canManageUsers,
    canManageOrders,
    canManageWithdrawals,
    canManageSystem,
    
    // 方法
    login,
    logout,
    refreshAdminInfo,
    changePassword,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    hasAnyRole,
    getAdminLogs,
    logAdminAction,
    initialize,
    validateSession
  }
})