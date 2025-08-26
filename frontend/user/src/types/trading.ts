// 交易相关的类型定义

export type OrderType = 'buy' | 'sell'
export type OrderStatus = 'pending' | 'partial' | 'completed' | 'cancelled' | 'failed'
export type TransactionType = 'deposit' | 'withdraw' | 'buy' | 'sell' | 'fee' | 'bonus'
export type WithdrawalStatus = 'pending' | 'processing' | 'completed' | 'failed' | 'cancelled'

// 订单相关
export interface Order {
  id: number
  user_id: number
  type: OrderType
  amount: string
  price?: string
  total_amount: string
  filled_amount: string
  status: OrderStatus
  created_at: string
  updated_at: string
  completed_at?: string
}

export interface CreateOrderRequest {
  type: OrderType
  amount: string
  price?: string
  order_type: 'market' | 'limit'
}

export interface OrderListParams {
  status?: OrderStatus
  type?: OrderType
  start_date?: string
  end_date?: string
  page?: number
  size?: number
}

// 钱包相关
export interface Wallet {
  id: number
  user_id: number
  currency: 'USDT' | 'TRX'
  balance: string
  frozen_balance: string
  total_balance: string
  address?: string
  created_at: string
  updated_at: string
}

export interface WalletTransaction {
  id: number
  wallet_id: number
  type: TransactionType
  amount: string
  fee: string
  balance_before: string
  balance_after: string
  status: string
  description: string
  transaction_hash?: string
  created_at: string
}

export interface DepositRequest {
  currency: 'USDT' | 'TRX'
}

export interface DepositResponse {
  address: string
  qr_code: string
  currency: string
}

export interface WithdrawRequest {
  currency: 'USDT' | 'TRX'
  amount: string
  to_address: string
  password: string
  mfa_code?: string
}

export interface Withdrawal {
  id: number
  user_id: number
  currency: 'USDT' | 'TRX'
  amount: string
  fee: string
  to_address: string
  transaction_hash?: string
  status: WithdrawalStatus
  created_at: string
  processed_at?: string
}

// 价格相关
export interface PriceData {
  usdt_cny: string
  usdt_usd: string
  change_24h: string
  change_percent_24h: string
  updated_at: string
}

export interface PriceHistory {
  timestamp: string
  price: string
  volume?: string
}

export interface PriceAlert {
  id: number
  user_id: number
  price: string
  condition: 'above' | 'below'
  enabled: boolean
  triggered: boolean
  created_at: string
}