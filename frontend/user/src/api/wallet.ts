import { HttpClient } from '@/utils/http'
import type { ApiResponse } from '@/types/api'

// 錢包相關API接口
export interface WalletBalance {
  total_balance: number
  available_balance: number
  frozen_balance: number
  daily_change: number
}

export interface TransactionListParams {
  pageNum: number
  pageSize: number
  type?: string
  status?: string
  startDate?: string
  endDate?: string
}

export interface TransactionRecord {
  id: string
  type: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'TRADING_BUY' | 'TRADING_SELL'
  amount: number
  fee: number
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  description: string
  created_at: string
  completed_at?: string
  transaction_hash?: string
}

export interface WithdrawalRequest {
  amount: number
  payment_method: string
  payment_account: string
  payment_name?: string
  remarks?: string
  verification_code?: string
}

export interface DepositRequest {
  amount: number
  payment_method: string
}

export interface WithdrawalResponse {
  withdrawal_id: string
  amount: number
  fee: number
  net_amount: number
  status: string
  estimated_completion: string
}

export interface DepositResponse {
  deposit_id: string
  amount: number
  payment_method: string
  payment_address?: string
  payment_qr_code?: string
  expires_at: string
}

export interface PaymentInfo {
  bank_name: string
  account_holder: string
  account_number: string
  swift_code?: string
  routing_number?: string
}

export interface WalletStatistics {
  total_deposits: number
  total_withdrawals: number
  total_trading_volume: number
  profit_loss: number
}

export interface DepositMethods {
  [key: string]: {
    name: string
    enabled: boolean
    min_amount: number
    max_amount: number
    fee: string
    processing_time: string
  }
}

export interface TransactionDetail {
  id: string
  type: string
  amount: number
  fee: number
  net_amount: number
  status: string
  description: string
  created_at: string
  completed_at?: string
  cancelled_at?: string
  transaction_hash?: string
  block_confirmations?: number
  payment_info?: PaymentInfo
  remarks?: string
}

export interface AssetDistribution {
  currency: string
  name: string
  balance: number
  usdValue: number
  percentage: number
  icon: string
  color?: string
}

// 錢包服務類
export class WalletApi {
  /**
   * 獲取錢包餘額
   */
  static async getBalance(): Promise<WalletBalance> {
    return HttpClient.get<WalletBalance>('/wallet/balance')
  }

  /**
   * 獲取資產分佈
   */
  static async getAssetDistribution(): Promise<AssetDistribution[]> {
    return HttpClient.get<AssetDistribution[]>('/wallet/assets')
  }

  /**
   * 獲取交易記錄
   */
  static async getTransactions(params: TransactionListParams): Promise<{
    data: TransactionRecord[]
    pagination: {
      current_page: number
      total_pages: number
      total_items: number
      per_page: number
    }
  }> {
    return HttpClient.get('/wallet/transactions', params)
  }

  /**
   * 獲取最近交易記錄
   */
  static async getRecentTransactions(limit = 10): Promise<{
    data: TransactionRecord[]
  }> {
    return HttpClient.get('/wallet/transactions/recent', { limit })
  }

  /**
   * 獲取交易詳情
   */
  static async getTransactionDetail(transactionId: string): Promise<TransactionDetail> {
    return HttpClient.get<TransactionDetail>(`/wallet/transactions/${transactionId}`)
  }

  /**
   * 創建提現請求
   */
  static async createWithdrawal(request: WithdrawalRequest): Promise<WithdrawalResponse> {
    return HttpClient.post<WithdrawalResponse>('/wallet/withdraw', request)
  }

  /**
   * 創建充值請求
   */
  static async createDeposit(request: DepositRequest): Promise<DepositResponse> {
    return HttpClient.post<DepositResponse>('/wallet/deposit', request)
  }

  /**
   * 取消提現
   */
  static async cancelWithdrawal(withdrawalId: string, reason?: string): Promise<void> {
    return HttpClient.post<void>(`/wallet/withdrawals/${withdrawalId}/cancel`, { reason })
  }

  /**
   * 確認充值
   */
  static async confirmDeposit(depositId: string, payment_proof?: any): Promise<void> {
    return HttpClient.post<void>(`/wallet/deposits/${depositId}/confirm`, { payment_proof })
  }

  /**
   * 獲取支付方式
   */
  static async getDepositMethods(): Promise<DepositMethods> {
    return HttpClient.get<DepositMethods>('/wallet/deposit-methods')
  }

  /**
   * 獲取提現方式
   */
  static async getWithdrawalMethods(): Promise<DepositMethods> {
    return HttpClient.get<DepositMethods>('/wallet/withdrawal-methods')
  }

  /**
   * 獲取錢包統計
   */
  static async getWalletStatistics(period = '30d'): Promise<WalletStatistics> {
    return HttpClient.get<WalletStatistics>('/wallet/statistics', { period })
  }

  /**
   * 獲取提現限額
   */
  static async getWithdrawalLimits(): Promise<{
    daily_limit: number
    daily_used: number
    daily_remaining: number
    monthly_limit: number
    monthly_used: number
    monthly_remaining: number
    min_amount: number
    max_amount: number
  }> {
    return HttpClient.get('/wallet/withdrawal-limits')
  }

  /**
   * 獲取充值地址
   */
  static async getDepositAddress(currency: string): Promise<{
    address: string
    qr_code: string
    network: string
    min_confirmations: number
  }> {
    return HttpClient.get(`/wallet/deposit-address/${currency}`)
  }

  /**
   * 驗證提現地址
   */
  static async validateWithdrawalAddress(address: string, currency: string): Promise<{
    valid: boolean
    address_type?: string
    network?: string
  }> {
    return HttpClient.post('/wallet/validate-address', { address, currency })
  }

  /**
   * 內部轉賬
   */
  static async internalTransfer(request: {
    recipient_username: string
    amount: number
    remarks?: string
    verification_code?: string
  }): Promise<{
    transfer_id: string
    status: string
    amount: number
    fee: number
  }> {
    return HttpClient.post('/wallet/internal-transfer', request)
  }

  /**
   * 獲取手續費信息
   */
  static async getFeeInfo(): Promise<{
    deposit_fees: Record<string, string>
    withdrawal_fees: Record<string, string>
    trading_fees: {
      maker_fee: string
      taker_fee: string
    }
    transfer_fee: string
  }> {
    return HttpClient.get('/wallet/fee-info')
  }

  /**
   * 獲取錢包地址白名單
   */
  static async getWhitelistAddresses(): Promise<{
    id: string
    label: string
    address: string
    currency: string
    created_at: string
  }[]> {
    return HttpClient.get('/wallet/whitelist-addresses')
  }

  /**
   * 添加錢包地址到白名單
   */
  static async addWhitelistAddress(request: {
    label: string
    address: string
    currency: string
    verification_code: string
  }): Promise<{
    id: string
    status: string
  }> {
    return HttpClient.post('/wallet/whitelist-addresses', request)
  }

  /**
   * 刪除白名單地址
   */
  static async removeWhitelistAddress(addressId: string, verification_code: string): Promise<void> {
    return HttpClient.delete(`/wallet/whitelist-addresses/${addressId}`, {
      verification_code
    })
  }

  /**
   * 導出交易記錄
   */
  static async exportTransactions(params: {
    format: 'csv' | 'excel'
    startDate?: string
    endDate?: string
    type?: string
    status?: string
  }): Promise<{
    download_url: string
    expires_at: string
  }> {
    return HttpClient.post('/wallet/export-transactions', params)
  }

  /**
   * 獲取錢包安全設置
   */
  static async getSecuritySettings(): Promise<{
    withdrawal_verification: boolean
    large_amount_notification: boolean
    unknown_ip_notification: boolean
    daily_limit_notification: boolean
  }> {
    return HttpClient.get('/wallet/security-settings')
  }

  /**
   * 更新錢包安全設置
   */
  static async updateSecuritySettings(settings: {
    withdrawal_verification?: boolean
    large_amount_notification?: boolean
    unknown_ip_notification?: boolean
    daily_limit_notification?: boolean
  }): Promise<void> {
    return HttpClient.patch('/wallet/security-settings', settings)
  }

  /**
   * 获取地址簿
   */
  static async getAddressBook(): Promise<{
    id: string
    label: string
    address: string
    currency: string
    created_at: string
  }[]> {
    return this.getWhitelistAddresses()
  }

  /**
   * 发送提现验证码
   */
  static async sendWithdrawVerificationCode(request: {
    type: 'email' | 'sms'
  }): Promise<void> {
    return HttpClient.post('/wallet/send-withdraw-verification', request)
  }

  /**
   * 创建提现订单
   */
  static async createWithdrawOrder(request: WithdrawalRequest): Promise<WithdrawalResponse> {
    return this.createWithdrawal(request)
  }
}

// 导出便捷函数以保持向后兼容
export const getWalletBalance = WalletApi.getBalance
export const getDepositMethods = WalletApi.getDepositMethods
export const getDepositAddress = WalletApi.getDepositAddress
export const createDeposit = WalletApi.createDeposit
export const createDepositOrder = WalletApi.createDeposit // 别名
export const getDepositHistory = WalletApi.getTransactions // 别名，使用交易记录API
export const getWalletStatistics = WalletApi.getWalletStatistics
export const getTransactions = WalletApi.getTransactions
export const getTransactionDetail = WalletApi.getTransactionDetail
export const createWithdrawal = WalletApi.createWithdrawal
export const createWithdrawOrder = WalletApi.createWithdrawOrder
export const getWithdrawHistory = WalletApi.getTransactions // 别名，使用交易记录API
export const getWithdrawalLimits = WalletApi.getWithdrawalLimits
export const getWithdrawalMethods = WalletApi.getWithdrawalMethods
export const getWithdrawMethods = WalletApi.getWithdrawalMethods // 别名
export const getAssetDistribution = WalletApi.getAssetDistribution
export const getRecentTransactions = WalletApi.getRecentTransactions
export const getWalletTransactions = WalletApi.getTransactions // 别名
export const getTransactionStatistics = WalletApi.getWalletStatistics // 别名
export const exportTransactionHistory = WalletApi.exportTransactions
export const getAddressBook = WalletApi.getAddressBook
export const sendWithdrawVerificationCode = WalletApi.sendWithdrawVerificationCode
export const cancelTransaction = WalletApi.cancelWithdrawal

export default WalletApi