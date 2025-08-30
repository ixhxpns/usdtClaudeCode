import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { AuthAPI } from '@/api/auth'
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
import type { 
  User, 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest,
  RegisterResponse,
  PasswordChangeRequest 
} from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<User | null>(getUserInfo())
  const isLoading = ref(false)
  const loginAttempts = ref(0)
  const maxLoginAttempts = 5
  const lockoutTime = ref(0)

  // 计算属性
  const isAuthenticated = computed(() => checkAuthenticated() && user.value !== null)
  const isEmailVerified = computed(() => user.value?.email_verified || false)
  const isPhoneVerified = computed(() => user.value?.phone_verified || false)
  const isMfaEnabled = computed(() => user.value?.mfa_enabled || false)
  const userStatus = computed(() => user.value?.status || 'inactive')
  const isLocked = computed(() => lockoutTime.value > Date.now())

  // 登录
  const login = async (credentials: LoginRequest) => {
    if (isLocked.value) {
      const remainingTime = Math.ceil((lockoutTime.value - Date.now()) / 1000 / 60)
      throw new Error(`账户已锁定，请 ${remainingTime} 分钟后重试`)
    }

    if (loginAttempts.value >= maxLoginAttempts) {
      lockoutTime.value = Date.now() + 15 * 60 * 1000 // 锁定15分钟
      throw new Error('登录失败次数过多，账户已锁定15分钟')
    }

    try {
      isLoading.value = true
      
      // 加密密码
      const encryptedCredentials = {
        ...credentials,
        password: encryptSensitiveData(credentials.password)
      }

      const response = await AuthAPI.login(encryptedCredentials)
      
      // 保存认证信息
      setToken(response.accessToken)
      setUserInfo(response.user)
      user.value = response.user
      
      // 重置登录尝试次数
      loginAttempts.value = 0
      lockoutTime.value = 0
      
      // 通知认证状态变化
      notifyAuthStateChange(true)
      
      ElMessage.success('登录成功')
      return response
    } catch (error: any) {
      loginAttempts.value++
      const message = error.message || '登录失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 注册
  const register = async (data: RegisterRequest) => {
    try {
      isLoading.value = true
      
      // 加密密码
      const encryptedData = {
        ...data,
        password: encryptSensitiveData(data.password),
        confirm_password: encryptSensitiveData(data.confirm_password)
      }

      const response = await AuthAPI.register(encryptedData)
      
      // 自动登录
      setToken(response.accessToken)
      setUserInfo(response.user)
      user.value = response.user
      
      // 通知认证状态变化
      notifyAuthStateChange(true)
      
      // 移除這裡的ElMessage，讓組件處理成功消息
      return response
    } catch (error: any) {
      // 移除這裡的ElMessage，讓組件處理錯誤消息
      throw error
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
        await AuthAPI.logout().catch(() => {
          // 即使服务器登出失败，也要清除本地数据
        })
      }
      
      // 清除本地认证数据
      clearAuth()
      user.value = null
      loginAttempts.value = 0
      lockoutTime.value = 0
      
      // 通知认证状态变化
      notifyAuthStateChange(false)
      
      if (showMessage) {
        ElMessage.success('已安全退出')
      }
    } catch (error) {
      console.error('登出错误:', error)
      // 即使登出失败，也要清除本地数据
      clearAuth()
      user.value = null
      notifyAuthStateChange(false)
    } finally {
      isLoading.value = false
    }
  }

  // 刷新用户信息
  const refreshUserInfo = async () => {
    try {
      isLoading.value = true
      const response = await AuthAPI.getUserProfile()
      
      setUserInfo(response)
      user.value = response
      
      return response
    } catch (error: any) {
      const message = error.message || '获取用户信息失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 修改密码
  const changePassword = async (data: PasswordChangeRequest) => {
    try {
      isLoading.value = true
      
      // 加密密码
      const encryptedData = {
        current_password: encryptSensitiveData(data.current_password),
        new_password: encryptSensitiveData(data.new_password),
        confirm_password: encryptSensitiveData(data.confirm_password)
      }

      await AuthAPI.changePassword(encryptedData)
      
      ElMessage.success('密码修改成功，请重新登录')
      
      // 密码修改成功后需要重新登录
      setTimeout(() => {
        logout(false)
        window.location.href = '/login'
      }, 2000)
    } catch (error: any) {
      const message = error.message || '密码修改失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 发送邮箱验证
  const sendEmailVerification = async () => {
    try {
      isLoading.value = true
      await AuthAPI.sendEmailVerification()
      ElMessage.success('验证邮件已发送，请查收')
    } catch (error: any) {
      const message = error.message || '发送验证邮件失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 验证邮箱
  const verifyEmail = async (token: string) => {
    try {
      isLoading.value = true
      await AuthAPI.verifyEmail(token)
      
      // 刷新用户信息
      await refreshUserInfo()
      
      ElMessage.success('邮箱验证成功')
    } catch (error: any) {
      const message = error.message || '邮箱验证失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 发送手机验证码
  const sendPhoneCode = async () => {
    try {
      isLoading.value = true
      await AuthAPI.sendPhoneCode()
      ElMessage.success('验证码已发送')
    } catch (error: any) {
      const message = error.message || '发送验证码失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 验证手机号
  const verifyPhone = async (code: string) => {
    try {
      isLoading.value = true
      await AuthAPI.verifyPhone(code)
      
      // 刷新用户信息
      await refreshUserInfo()
      
      ElMessage.success('手机号验证成功')
    } catch (error: any) {
      const message = error.message || '手机号验证失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 启用/禁用双因子认证
  const toggleMFA = async (enabled: boolean, code?: string) => {
    try {
      isLoading.value = true
      
      if (enabled) {
        await AuthAPI.enableMFA({ code: code || '' })
        ElMessage.success('双因子认证已启用')
      } else {
        await AuthAPI.disableMFA({ code: code || '' })
        ElMessage.success('双因子认证已禁用')
      }
      
      // 刷新用户信息
      await refreshUserInfo()
    } catch (error: any) {
      const message = error.message || '操作失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 重置密码
  const resetPassword = async (email: string) => {
    try {
      isLoading.value = true
      await AuthAPI.resetPassword({ email })
      ElMessage.success('重置密码邮件已发送')
    } catch (error: any) {
      const message = error.message || '发送重置密码邮件失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 确认重置密码
  const confirmResetPassword = async (token: string, newPassword: string, confirmPassword: string) => {
    try {
      isLoading.value = true
      
      const encryptedData = {
        token,
        new_password: encryptSensitiveData(newPassword),
        confirm_password: encryptSensitiveData(confirmPassword)
      }

      await AuthAPI.confirmResetPassword(encryptedData)
      ElMessage.success('密码重置成功，请使用新密码登录')
    } catch (error: any) {
      const message = error.message || '密码重置失败'
      ElMessage.error(message)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 初始化认证状态
  const initialize = () => {
    const savedUser = getUserInfo()
    if (savedUser && checkAuthenticated()) {
      user.value = savedUser
      notifyAuthStateChange(true)
    } else {
      clearAuth()
      user.value = null
      notifyAuthStateChange(false)
    }
  }

  return {
    // 状态
    user: readonly(user),
    isLoading: readonly(isLoading),
    loginAttempts: readonly(loginAttempts),
    lockoutTime: readonly(lockoutTime),
    
    // 计算属性
    isAuthenticated,
    isEmailVerified,
    isPhoneVerified,
    isMfaEnabled,
    userStatus,
    isLocked,
    
    // 方法
    login,
    register,
    logout,
    refreshUserInfo,
    changePassword,
    sendEmailVerification,
    verifyEmail,
    sendPhoneCode,
    verifyPhone,
    toggleMFA,
    resetPassword,
    confirmResetPassword,
    initialize
  }
})