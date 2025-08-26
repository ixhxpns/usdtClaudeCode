// API模組統一導出
export { default as TradingApi } from './trading'
export { default as WalletApi } from './wallet'
export { default as KycApi } from './kyc'

// 導出類型定義
export type {
  // Trading types
  PriceData,
  CreateOrderRequest,
  OrderResponse,
  PaymentConfirmRequest,
  OrderDetail,
  OrderListParams,
  TradingStatistics,
  PaymentMethods,
  TradingLimits
} from './trading'

export type {
  // Wallet types
  WalletBalance,
  TransactionListParams,
  TransactionRecord,
  WithdrawalRequest,
  DepositRequest,
  WithdrawalResponse,
  DepositResponse,
  PaymentInfo,
  WalletStatistics,
  DepositMethods,
  TransactionDetail
} from './wallet'

export type {
  // KYC types
  KycInfo,
  KycSubmitRequest,
  KycDocumentUploadResponse,
  KycStatus,
  KycLevelInfo
} from './kyc'