// 用户相关的类型定义

export type UserStatus = 'active' | 'inactive' | 'locked' | 'pending'
export type Gender = 'male' | 'female' | 'other'
export type IdType = 'id_card' | 'passport' | 'driver_license'
export type KycStatus = 'pending' | 'approved' | 'rejected' | 'reviewing' | 'resubmit'

export interface User {
  id: number
  email: string
  phone?: string
  status: UserStatus
  email_verified: boolean
  phone_verified: boolean
  mfa_enabled: boolean
  last_login_at?: string
  created_at: string
  updated_at: string
}

export interface UserProfile {
  id: number
  user_id: number
  first_name?: string
  last_name?: string
  nickname?: string
  avatar?: string
  birth_date?: string
  gender?: Gender
  country?: string
  city?: string
  address?: string
  postal_code?: string
  created_at: string
  updated_at: string
}

export interface UserKyc {
  id: number
  user_id: number
  id_type: IdType
  id_number: string
  first_name: string
  last_name: string
  birth_date: string
  country: string
  document_front_url?: string
  document_back_url?: string
  selfie_url?: string
  status: KycStatus
  submitted_at?: string
  reviewed_at?: string
  reject_reason?: string
  created_at: string
  updated_at: string
}

// 认证相关
export interface LoginRequest {
  email: string
  password: string
  mfa_code?: string
  remember_me?: boolean
}

export interface LoginResponse {
  access_token: string
  refresh_token: string
  expires_in: number
  user: User
}

export interface RegisterRequest {
  email: string
  password: string
  confirm_password: string
  phone?: string
  verification_code: string
  agree_terms: boolean
}

export interface RegisterResponse {
  user: User
  access_token: string
  refresh_token: string
  expires_in: number
}

export interface PasswordChangeRequest {
  current_password: string
  new_password: string
  confirm_password: string
}

export interface ResetPasswordRequest {
  email: string
}

export interface ConfirmResetPasswordRequest {
  token: string
  new_password: string
  confirm_password: string
}

// 邮箱验证相关
export interface EmailVerificationRequest {
  email?: string
}

// 手机验证相关
export interface PhoneVerificationRequest {
  phone?: string
}

// MFA相关
export interface MFAToggleRequest {
  code: string
}

export interface MFASetupResponse {
  secret: string
  qr_code: string
  backup_codes: string[]
}

// 用户名和邮箱检查响应
export interface AvailabilityResponse {
  available: boolean
  message?: string
}

// 登录历史
export interface LoginHistory {
  id: string
  ip_address: string
  user_agent: string
  device_info: {
    device_type: string
    os: string
    browser: string
  }
  location: string
  login_time: string
  status: 'success' | 'failed'
}

export interface LoginHistoryResponse {
  data: LoginHistory[]
  pagination: {
    current_page: number
    total_pages: number
    total_items: number
    per_page: number
  }
}

// 安全设置
export interface SecuritySettings {
  email_verified: boolean
  phone_verified: boolean
  mfa_enabled: boolean
  password_last_changed: string
  login_notifications: boolean
  two_step_verification: boolean
}

// Token刷新响应
export interface TokenRefreshResponse {
  access_token: string
  expires_in: number
}

// KYC提交请求
export interface KycSubmitRequest {
  id_type: IdType
  id_number: string
  first_name: string
  last_name: string
  birth_date: string
  country: string
  document_front: File
  document_back?: File
  selfie: File
}