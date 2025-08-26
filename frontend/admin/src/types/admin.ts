// 管理后台相关的类型定义

export type AdminRole = 'super_admin' | 'admin' | 'operator' | 'viewer'
export type AdminStatus = 'active' | 'inactive' | 'locked'
export type UserStatus = 'active' | 'inactive' | 'locked' | 'pending'
export type KycStatus = 'pending' | 'approved' | 'rejected' | 'reviewing' | 'resubmit'
export type OrderStatus = 'pending' | 'partial' | 'completed' | 'cancelled' | 'failed'
export type WithdrawalStatus = 'pending' | 'processing' | 'completed' | 'failed' | 'cancelled'

// 管理员相关
export interface Admin {
  id: number
  username: string
  email: string
  role: AdminRole
  status: AdminStatus
  last_login_at?: string
  login_count: number
  permissions: string[]
  created_at: string
  updated_at: string
}

export interface AdminLoginRequest {
  username: string
  password: string
  mfa_code?: string
}

export interface AdminLoginResponse {
  access_token: string
  refresh_token: string
  expires_in: number
  admin: Admin
}

// 用户管理相关
export interface UserManagement {
  id: number
  email: string
  phone?: string
  status: UserStatus
  email_verified: boolean
  phone_verified: boolean
  kyc_status: KycStatus
  balance: {
    usdt: string
    trx: string
  }
  last_login_at?: string
  registration_ip?: string
  created_at: string
  updated_at: string
}

export interface UserDetail extends UserManagement {
  profile: {
    first_name?: string
    last_name?: string
    birth_date?: string
    country?: string
    address?: string
  }
  kyc?: {
    id_type: string
    id_number: string
    status: KycStatus
    submitted_at?: string
    reviewed_at?: string
    reject_reason?: string
  }
  security: {
    mfa_enabled: boolean
    login_attempts: number
    locked_until?: string
  }
}

export interface UserUpdateRequest {
  status?: UserStatus
  email_verified?: boolean
  phone_verified?: boolean
  notes?: string
}

// KYC管理相关
export interface KycReview {
  id: number
  user_id: number
  user_email: string
  id_type: string
  id_number: string
  first_name: string
  last_name: string
  birth_date: string
  country: string
  document_front_url?: string
  document_back_url?: string
  selfie_url?: string
  status: KycStatus
  submitted_at: string
  reviewed_at?: string
  reviewer_id?: number
  reviewer_name?: string
  reject_reason?: string
  notes?: string
}

export interface KycReviewRequest {
  status: 'approved' | 'rejected'
  reject_reason?: string
  notes?: string
}

// 订单管理相关
export interface OrderManagement {
  id: number
  user_id: number
  user_email: string
  type: 'buy' | 'sell'
  amount: string
  price?: string
  total_amount: string
  filled_amount: string
  status: OrderStatus
  fee: string
  created_at: string
  updated_at: string
  completed_at?: string
}

// 提现管理相关
export interface WithdrawalManagement {
  id: number
  user_id: number
  user_email: string
  currency: 'USDT' | 'TRX'
  amount: string
  fee: string
  to_address: string
  transaction_hash?: string
  status: WithdrawalStatus
  created_at: string
  processed_at?: string
  processor_id?: number
  processor_name?: string
  notes?: string
}

export interface WithdrawalProcessRequest {
  status: 'processing' | 'completed' | 'failed' | 'cancelled'
  transaction_hash?: string
  notes?: string
}

// 系统配置相关
export interface SystemConfig {
  id: number
  key: string
  value: string
  description: string
  type: 'string' | 'number' | 'boolean' | 'json'
  category: string
  editable: boolean
  created_at: string
  updated_at: string
}

export interface ConfigUpdateRequest {
  value: string
}

// 系统统计相关
export interface DashboardStats {
  users: {
    total: number
    active: number
    new_today: number
    growth_rate: string
  }
  orders: {
    total: number
    today: number
    volume_24h: string
    growth_rate: string
  }
  withdrawals: {
    pending: number
    processing: number
    completed_today: number
    total_amount_24h: string
  }
  revenue: {
    total: string
    today: string
    growth_rate: string
  }
}

export interface ChartData {
  labels: string[]
  datasets: Array<{
    label: string
    data: number[]
    backgroundColor?: string
    borderColor?: string
    borderWidth?: number
  }>
}

// 审计日志相关
export interface AuditLog {
  id: number
  user_id?: number
  admin_id?: number
  action: string
  resource: string
  resource_id?: string
  details: Record<string, any>
  ip_address: string
  user_agent: string
  created_at: string
}

export interface AuditLogFilters {
  user_id?: number
  admin_id?: number
  action?: string
  resource?: string
  start_date?: string
  end_date?: string
}

// 公告管理相关
export interface Announcement {
  id: number
  title: string
  content: string
  type: 'info' | 'warning' | 'success' | 'error'
  priority: 'low' | 'medium' | 'high' | 'urgent'
  published: boolean
  publish_at?: string
  expire_at?: string
  created_by: number
  created_by_name: string
  created_at: string
  updated_at: string
}

export interface AnnouncementRequest {
  title: string
  content: string
  type: 'info' | 'warning' | 'success' | 'error'
  priority: 'low' | 'medium' | 'high' | 'urgent'
  published: boolean
  publish_at?: string
  expire_at?: string
}