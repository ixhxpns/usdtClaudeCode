import { HttpClient } from '@/utils/http'
import type { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest,
  RegisterResponse,
  User,
  PasswordChangeRequest,
  ResetPasswordRequest,
  ConfirmResetPasswordRequest,
  EmailVerificationRequest,
  PhoneVerificationRequest,
  MFAToggleRequest,
  MFASetupResponse
} from '@/types/user'

/**
 * 认证API服务类
 * 封装所有认证相关的API调用
 */
export class AuthAPI {
  /**
   * 用户登录
   */
  static async login(credentials: LoginRequest): Promise<LoginResponse> {
    return await HttpClient.post<LoginResponse>('/auth/login', credentials)
  }

  /**
   * 用户注册
   */
  static async register(data: RegisterRequest): Promise<RegisterResponse> {
    return await HttpClient.post<RegisterResponse>('/auth/register', data)
  }

  /**
   * 用户登出
   */
  static async logout(): Promise<void> {
    return await HttpClient.post<void>('/auth/logout')
  }

  /**
   * 刷新访问令牌
   */
  static async refreshToken(): Promise<{ accessToken: string; expiresIn: number }> {
    return await HttpClient.post<{ accessToken: string; expiresIn: number }>('/auth/refresh')
  }

  /**
   * 获取用户信息
   */
  static async getUserProfile(): Promise<User> {
    return await HttpClient.get<User>('/users/profile')
  }

  /**
   * 发送邮箱验证码
   */
  static async sendEmailVerification(data: EmailVerificationRequest): Promise<{
    message: string
    email: string
    expiryTime: number
  }> {
    return await HttpClient.post<{
      message: string
      email: string
      expiryTime: number
    }>('/auth/send-email-verification', data)
  }

  /**
   * 验证邮箱
   */
  static async verifyEmail(token: string): Promise<void> {
    return await HttpClient.post<void>('/auth/verify-email', { token })
  }

  /**
   * 发送手机验证码
   */
  static async sendPhoneCode(data?: PhoneVerificationRequest): Promise<void> {
    return await HttpClient.post<void>('/auth/send-phone-code', data)
  }

  /**
   * 验证手机号
   */
  static async verifyPhone(code: string): Promise<void> {
    return await HttpClient.post<void>('/auth/verify-phone', { code })
  }

  /**
   * 修改密码
   */
  static async changePassword(data: PasswordChangeRequest): Promise<void> {
    return await HttpClient.post<void>('/users/change-password', data)
  }

  /**
   * 发送重置密码邮件
   */
  static async resetPassword(data: ResetPasswordRequest): Promise<void> {
    return await HttpClient.post<void>('/auth/reset-password', data)
  }

  /**
   * 确认重置密码
   */
  static async confirmResetPassword(data: ConfirmResetPasswordRequest): Promise<void> {
    return await HttpClient.post<void>('/auth/confirm-reset-password', data)
  }

  /**
   * 获取MFA设置信息
   */
  static async getMFASetup(): Promise<MFASetupResponse> {
    return await HttpClient.get<MFASetupResponse>('/auth/mfa/setup')
  }

  /**
   * 启用MFA
   */
  static async enableMFA(data: MFAToggleRequest): Promise<void> {
    return await HttpClient.post<void>('/auth/enable-mfa', data)
  }

  /**
   * 禁用MFA
   */
  static async disableMFA(data: MFAToggleRequest): Promise<void> {
    return await HttpClient.post<void>('/auth/disable-mfa', data)
  }

  /**
   * MFA验证
   */
  static async verifyMFA(code: string): Promise<{ access_token: string; expires_in: number }> {
    return await HttpClient.post<{ access_token: string; expires_in: number }>('/auth/verify-mfa', { code })
  }

  /**
   * 检查用户名是否可用
   */
  static async checkUsernameAvailability(username: string): Promise<{ available: boolean }> {
    return await HttpClient.get<{ available: boolean }>(`/auth/check-username?username=${encodeURIComponent(username)}`)
  }

  /**
   * 检查邮箱是否可用
   */
  static async checkEmailAvailability(email: string): Promise<{ available: boolean }> {
    return await HttpClient.get<{ available: boolean }>(`/auth/check-email?email=${encodeURIComponent(email)}`)
  }

  /**
   * 获取登录历史
   */
  static async getLoginHistory(page = 1, limit = 20): Promise<{
    data: Array<{
      id: string
      ip_address: string
      user_agent: string
      device_info: object
      location: string
      login_time: string
      status: 'success' | 'failed'
    }>
    pagination: {
      current_page: number
      total_pages: number
      total_items: number
      per_page: number
    }
  }> {
    return await HttpClient.get(`/users/login-history?page=${page}&limit=${limit}`)
  }

  /**
   * 获取安全设置
   */
  static async getSecuritySettings(): Promise<{
    email_verified: boolean
    phone_verified: boolean
    mfa_enabled: boolean
    password_last_changed: string
    login_notifications: boolean
    two_step_verification: boolean
  }> {
    return await HttpClient.get('/users/security-settings')
  }

  /**
   * 更新安全设置
   */
  static async updateSecuritySettings(settings: {
    login_notifications?: boolean
    two_step_verification?: boolean
  }): Promise<void> {
    return await HttpClient.patch('/users/security-settings', settings)
  }
}

// 导出默认实例
export default AuthAPI