import { AdminHttpClient } from '@/utils/http'
import type { ApiResponse } from '@/types/api'

// 管理員API接口類型定義
export interface AdminOrderListParams {
  page: number
  pageSize: number
  orderId?: string
  userId?: string
  username?: string
  status?: string
  orderType?: string
  startDate?: string
  endDate?: string
  minAmount?: number
  maxAmount?: number
}

export interface AdminOrderItem {
  id: string
  orderNumber: string
  user: {
    id: string
    username: string
    email: string
    avatar?: string
  }
  orderType: 'BUY' | 'SELL'
  amount: number
  usdtAmount: number
  price: number
  status: string
  paymentMethod?: string
  createdAt: string
  completedAt?: string
  riskScore?: number
}

export interface AdminOrderDetail extends AdminOrderItem {
  paymentInfo?: {
    method: string
    account: string
    bankName?: string
    accountHolder?: string
  }
  paymentProof?: string[]
  timeline: {
    status: string
    timestamp: string
    operator?: string
    remarks?: string
  }[]
  disputeInfo?: {
    reason: string
    description: string
    evidence: string[]
    submittedAt: string
  }
}

export interface AdminKycListParams {
  page: number
  pageSize: number
  userId?: string
  username?: string
  status?: string
  riskLevel?: string
  submittedStartDate?: string
  submittedEndDate?: string
  reviewedStartDate?: string
  reviewedEndDate?: string
}

export interface AdminKycItem {
  id: string
  user: {
    id: string
    username: string
    email: string
    realName?: string
  }
  status: 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  submittedAt: string
  reviewedAt?: string
  reviewer?: {
    id: string
    name: string
  }
  waitingTime: number // 等待時間（分鐘）
}

export interface AdminKycDetail extends AdminKycItem {
  documents: {
    type: string
    frontImage: string
    backImage?: string
    uploadedAt: string
  }[]
  personalInfo: {
    realName: string
    idNumber: string
    birthDate: string
    gender: string
    nationality: string
    address: string
    occupation?: string
    income?: string
  }
  riskAssessment: {
    score: number
    factors: string[]
    recommendation: string
  }
  reviewHistory: {
    status: string
    reviewer: string
    timestamp: string
    remarks?: string
  }[]
}

export interface OrderStatistics {
  totalOrders: number
  pendingOrders: number
  completedOrders: number
  disputedOrders: number
  totalVolume: number
  todayVolume: number
  avgProcessingTime: number
  successRate: number
}

export interface OrderAnalytics {
  dailyStats: {
    date: string
    orderCount: number
    volume: number
    successRate: number
  }[]
  statusDistribution: {
    status: string
    count: number
    percentage: number
  }[]
  paymentMethodStats: {
    method: string
    count: number
    volume: number
  }[]
}

export interface DisputeOrder {
  id: string
  orderNumber: string
  disputeReason: string
  submittedBy: 'BUYER' | 'SELLER'
  status: 'PENDING' | 'INVESTIGATING' | 'RESOLVED'
  createdAt: string
}

export interface UserListParams {
  page: number
  pageSize: number
  userId?: string
  username?: string
  email?: string
  status?: string
  kycStatus?: string
  startDate?: string
  endDate?: string
}

export interface UserItem {
  id: string
  username: string
  email: string
  realName?: string
  phone?: string
  phoneVerified: boolean
  emailVerified: boolean
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED'
  kycStatus: 'NOT_SUBMITTED' | 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  lastLoginAt?: string
  avatar?: string
}

export interface WithdrawalListParams {
  page: number
  pageSize: number
  withdrawalId?: string
  userInfo?: string
  status?: string
  priority?: string
  minAmount?: number
  maxAmount?: number
  startDate?: string
  endDate?: string
}

export interface WithdrawalItem {
  id: string
  user: {
    id: string
    username: string
    email: string
    avatar?: string
  }
  amount: number
  fee: number
  paymentMethod: string
  paymentAccount: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'FAILED'
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  waitTime: number // 等待時間（分鐘）
  createdAt: string
  reviewedAt?: string
  completedAt?: string
  riskScore?: number
  reviewer?: {
    id: string
    name: string
  }
}

// 管理員API服務類
export class AdminApi {
  /**
   * 獲取訂單列表
   */
  static async getOrderList(params: AdminOrderListParams): Promise<{
    data: AdminOrderItem[]
    total: number
    statistics: OrderStatistics
  }> {
    return AdminHttpClient.get('/admin/orders', params)
  }

  /**
   * 獲取訂單詳情
   */
  static async getOrderDetail(orderId: string): Promise<AdminOrderDetail> {
    return AdminHttpClient.get<AdminOrderDetail>(`/admin/orders/${orderId}`)
  }

  /**
   * 訂單操作（完成/取消/爭議處理等）
   */
  static async updateOrderStatus(orderId: string, data: {
    status: string
    remarks?: string
    action?: string
  }): Promise<void> {
    return AdminHttpClient.post(`/admin/orders/${orderId}/status`, data)
  }

  /**
   * 獲取訂單分析數據
   */
  static async getOrderAnalytics(params: {
    period: string
    startDate?: string
    endDate?: string
  }): Promise<OrderAnalytics> {
    return AdminHttpClient.get('/admin/orders/analytics', params)
  }

  /**
   * 獲取爭議訂單列表
   */
  static async getDisputeOrders(params: {
    page: number
    pageSize: number
    status?: string
  }): Promise<{
    data: DisputeOrder[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/orders/disputes', params)
  }

  /**
   * 處理訂單爭議
   */
  static async resolveDispute(disputeId: string, data: {
    resolution: string
    winner: 'BUYER' | 'SELLER'
    remarks: string
  }): Promise<void> {
    return AdminHttpClient.post(`/admin/orders/disputes/${disputeId}/resolve`, data)
  }

  /**
   * 獲取KYC審核列表
   */
  static async getKycList(params: AdminKycListParams): Promise<{
    data: AdminKycItem[]
    total: number
    statistics: {
      pendingCount: number
      approvedToday: number
      rejectedToday: number
      avgReviewTime: number
    }
  }> {
    return AdminHttpClient.get('/admin/kyc', params)
  }

  /**
   * 獲取KYC審核詳情
   */
  static async getKycDetail(kycId: string): Promise<AdminKycDetail> {
    return AdminHttpClient.get<AdminKycDetail>(`/admin/kyc/${kycId}`)
  }

  /**
   * 審核KYC
   */
  static async reviewKyc(kycId: string, data: {
    decision: 'APPROVED' | 'REJECTED'
    remarks?: string
    riskLevel?: string
  }): Promise<void> {
    return AdminHttpClient.post(`/admin/kyc/${kycId}/review`, data)
  }

  /**
   * 批量審核KYC
   */
  static async batchReviewKyc(data: {
    kycIds: string[]
    decision: 'APPROVED' | 'REJECTED'
    remarks?: string
  }): Promise<void> {
    return AdminHttpClient.post('/admin/kyc/batch-review', data)
  }

  /**
   * 獲取用戶列表
   */
  static async getUserList(params: UserListParams): Promise<{
    data: UserItem[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/users', params)
  }

  /**
   * 獲取用戶統計
   */
  static async getUserStatistics(): Promise<{
    totalUsers: number
    activeUsers: number
    kycApprovedUsers: number
    newUsersToday: number
    monthlyGrowth: number
  }> {
    return AdminHttpClient.get('/admin/users/statistics')
  }

  /**
   * 禁用用戶
   */
  static async disableUser(userId: string, reason?: string): Promise<void> {
    return AdminHttpClient.post(`/admin/users/${userId}/disable`, { reason })
  }

  /**
   * 啟用用戶
   */
  static async enableUser(userId: string): Promise<void> {
    return AdminHttpClient.post(`/admin/users/${userId}/enable`)
  }

  /**
   * 重置用戶密碼
   */
  static async resetUserPassword(userId: string): Promise<{
    newPassword: string
  }> {
    return AdminHttpClient.post(`/admin/users/${userId}/reset-password`)
  }

  /**
   * 刪除用戶
   */
  static async deleteUser(userId: string): Promise<void> {
    return AdminHttpClient.delete(`/admin/users/${userId}`)
  }

  /**
   * 創建用戶
   */
  static async createUser(data: {
    username: string
    email: string
    password: string
    realName?: string
    phone?: string
    status: string
  }): Promise<{
    id: string
    username: string
  }> {
    return AdminHttpClient.post('/admin/users', data)
  }

  /**
   * 批量操作用戶
   */
  static async batchOperateUsers(data: {
    userIds: string[]
    operation: 'ENABLE' | 'DISABLE' | 'DELETE'
    reason?: string
  }): Promise<void> {
    return AdminHttpClient.post('/admin/users/batch-operate', data)
  }

  /**
   * 導出用戶數據
   */
  static async exportUsers(params: any): Promise<{
    downloadUrl: string
    expiresAt: string
  }> {
    return AdminHttpClient.post('/admin/users/export', params)
  }

  /**
   * 獲取提現列表
   */
  static async getWithdrawalList(params: WithdrawalListParams): Promise<{
    data: WithdrawalItem[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/withdrawals', params)
  }

  /**
   * 獲取提現統計
   */
  static async getWithdrawalStatistics(): Promise<{
    pendingCount: number
    pendingAmount: number
    todayProcessed: number
    avgProcessTime: number
    todayApproved: number
    todayRejected: number
  }> {
    return AdminHttpClient.get('/admin/withdrawals/statistics')
  }

  /**
   * 審核提現
   */
  static async reviewWithdrawal(withdrawalId: string, data: {
    decision: 'APPROVED' | 'REJECTED' | 'HOLD'
    remarks?: string
    processType?: 'AUTO' | 'MANUAL'
  }): Promise<void> {
    return AdminHttpClient.post(`/admin/withdrawals/${withdrawalId}/review`, data)
  }

  /**
   * 批量審核提現
   */
  static async batchApproveWithdrawals(withdrawalIds: string[]): Promise<void> {
    return AdminHttpClient.post('/admin/withdrawals/batch-approve', { withdrawalIds })
  }

  /**
   * 批量拒絕提現
   */
  static async batchRejectWithdrawals(withdrawalIds: string[], reason: string): Promise<void> {
    return AdminHttpClient.post('/admin/withdrawals/batch-reject', {
      withdrawalIds,
      reason
    })
  }

  /**
   * 智能分配提現審核
   */
  static async autoAssignWithdrawals(): Promise<void> {
    return AdminHttpClient.post('/admin/withdrawals/auto-assign')
  }

  /**
   * 獲取系統統計
   */
  static async getSystemStatistics(): Promise<{
    overview: {
      totalUsers: number
      totalOrders: number
      totalVolume: number
      activeUsers: number
    }
    trends: {
      userGrowth: number[]
      volumeGrowth: number[]
      orderGrowth: number[]
    }
    alerts: {
      type: string
      message: string
      level: 'INFO' | 'WARNING' | 'ERROR'
      timestamp: string
    }[]
  }> {
    return AdminHttpClient.get('/admin/statistics')
  }

  /**
   * 獲取系統配置
   */
  static async getSystemConfig(): Promise<{
    trading: {
      buyFeeRate: number
      sellFeeRate: number
      minOrderAmount: number
      maxOrderAmount: number
    }
    withdrawal: {
      minAmount: number
      maxAmount: number
      feeRate: number
      dailyLimit: number
    }
    kyc: {
      autoApprove: boolean
      requireDocuments: string[]
      riskThreshold: number
    }
  }> {
    return AdminHttpClient.get('/admin/system/config')
  }

  /**
   * 更新系統配置
   */
  static async updateSystemConfig(config: any): Promise<void> {
    return AdminHttpClient.put('/admin/system/config', config)
  }

  /**
   * 獲取操作日誌
   */
  static async getOperationLogs(params: {
    page: number
    pageSize: number
    operator?: string
    action?: string
    resource?: string
    startDate?: string
    endDate?: string
  }): Promise<{
    data: {
      id: string
      operator: string
      action: string
      resource: string
      details: any
      timestamp: string
      ipAddress: string
    }[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/logs/operations', params)
  }

  /**
   * 獲取系統日誌
   */
  static async getSystemLogs(params: {
    page: number
    pageSize: number
    level?: string
    module?: string
    startDate?: string
    endDate?: string
  }): Promise<{
    data: {
      id: string
      level: string
      module: string
      message: string
      timestamp: string
      details?: any
    }[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/logs/system', params)
  }

  /**
   * 獲取公告列表
   */
  static async getAnnouncements(params: {
    page: number
    pageSize: number
    status?: string
    type?: string
  }): Promise<{
    data: {
      id: string
      title: string
      content: string
      type: string
      status: string
      publishedAt?: string
      createdAt: string
    }[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/announcements', params)
  }

  /**
   * 創建公告
   */
  static async createAnnouncement(data: {
    title: string
    content: string
    type: string
    publishAt?: string
    expireAt?: string
  }): Promise<{
    id: string
  }> {
    return AdminHttpClient.post('/admin/announcements', data)
  }

  /**
   * 更新公告
   */
  static async updateAnnouncement(id: string, data: any): Promise<void> {
    return AdminHttpClient.put(`/admin/announcements/${id}`, data)
  }

  /**
   * 刪除公告
   */
  static async deleteAnnouncement(id: string): Promise<void> {
    return AdminHttpClient.delete(`/admin/announcements/${id}`)
  }

  /**
   * 發布公告
   */
  static async publishAnnouncement(id: string): Promise<void> {
    return AdminHttpClient.post(`/admin/announcements/${id}/publish`)
  }

  /**
   * 獲取管理員列表
   */
  static async getAdminList(params: {
    page: number
    pageSize: number
    username?: string
    role?: string
    status?: string
  }): Promise<{
    data: {
      id: string
      username: string
      email: string
      role: string
      status: string
      lastLoginAt?: string
      createdAt: string
    }[]
    total: number
  }> {
    return AdminHttpClient.get('/admin/admins', params)
  }

  /**
   * 創建管理員
   */
  static async createAdmin(data: {
    username: string
    email: string
    password: string
    role: string
    permissions: string[]
  }): Promise<{
    id: string
  }> {
    return AdminHttpClient.post('/admin/admins', data)
  }

  /**
   * 更新管理員
   */
  static async updateAdmin(id: string, data: {
    role?: string
    permissions?: string[]
    status?: string
  }): Promise<void> {
    return AdminHttpClient.put(`/admin/admins/${id}`, data)
  }

  /**
   * 刪除管理員
   */
  static async deleteAdmin(id: string): Promise<void> {
    return AdminHttpClient.delete(`/admin/admins/${id}`)
  }
}

export default AdminApi