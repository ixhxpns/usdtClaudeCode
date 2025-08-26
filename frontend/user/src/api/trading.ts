import { HttpClient } from '@/utils/http'
import type { ApiResponse } from '@/types/api'

// 交易相關API接口
export interface PriceData {
  buyPrice: number
  sellPrice: number
  basePrice: number
  spreadPercent: number
  lastUpdate: string
}

export interface CreateOrderRequest {
  amount: number
  paymentMethod?: string
  receivingAccount?: string
  receivingBank?: string
}

export interface OrderResponse {
  orderId: number
  orderNumber: string
  amount: number
  usdtAmount: number
  price: number
  status: string
  paymentMethod?: string
  receivingAccount?: string
  receivingBank?: string
}

export interface PaymentConfirmRequest {
  paymentProof?: Record<string, string>
}

export interface OrderDetail {
  id: number
  orderNumber: string
  orderType: 'BUY' | 'SELL'
  amount: number
  usdtAmount: number
  price: number
  status: string
  paymentMethod?: string
  receivingAccount?: string
  receivingBank?: string
  createdAt: string
  paymentConfirmTime?: string
  completedAt?: string
  cancelReason?: string
}

export interface OrderListParams {
  pageNum: number
  pageSize: number
  orderType?: string
  status?: string
  startDate?: string
  endDate?: string
}

export interface TradingStatistics {
  totalTrades: number
  totalVolume: number
  avgOrderSize: number
  successRate: number
}

export interface PaymentMethods {
  [key: string]: {
    name: string
    enabled: boolean
    fee: string
  }
}

export interface TradingLimits {
  minBuy: number
  maxBuy: number
  minSell: number
  maxSell: number
  dailyRemaining: number
  message?: string
}

// 交易服務類
export class TradingApi {
  // 獲取當前價格
  static async getCurrentPrice(): Promise<PriceData> {
    return HttpClient.get<PriceData>('/trading/current-price')
  }

  // 創建買入訂單
  static async createBuyOrder(request: CreateOrderRequest): Promise<OrderResponse> {
    return HttpClient.post<OrderResponse>('/trading/buy-order', request)
  }

  // 創建賣出訂單
  static async createSellOrder(request: CreateOrderRequest): Promise<OrderResponse> {
    return HttpClient.post<OrderResponse>('/trading/sell-order', request)
  }

  // 確認支付
  static async confirmPayment(orderId: number, request: PaymentConfirmRequest): Promise<string> {
    return HttpClient.post<string>(`/trading/orders/${orderId}/confirm-payment`, request)
  }

  // 取消訂單
  static async cancelOrder(orderId: number, reason: string): Promise<string> {
    return HttpClient.post<string>(`/trading/orders/${orderId}/cancel`, { reason })
  }

  // 獲取訂單詳情
  static async getOrderDetail(orderId: number): Promise<OrderDetail> {
    return HttpClient.get<OrderDetail>(`/trading/orders/${orderId}`)
  }

  // 獲取用戶訂單列表
  static async getUserOrders(params: OrderListParams): Promise<any> {
    return HttpClient.get('/trading/user-orders', params)
  }

  // 獲取交易統計
  static async getTradingStatistics(period: string = '30d'): Promise<TradingStatistics> {
    return HttpClient.get<TradingStatistics>('/trading/statistics', { period })
  }

  // 獲取支付方式
  static async getPaymentMethods(): Promise<PaymentMethods> {
    return HttpClient.get<PaymentMethods>('/trading/payment-methods')
  }

  // 獲取交易限額
  static async getTradingLimits(): Promise<TradingLimits> {
    return HttpClient.get<TradingLimits>('/trading/limits')
  }

  // 获取交易对列表
  static async getTradingPairs(): Promise<{
    symbol: string
    baseAsset: string
    quoteAsset: string
    status: string
    minPrice: number
    maxPrice: number
    minQty: number
    maxQty: number
  }[]> {
    return HttpClient.get('/trading/pairs')
  }

  // 获取行情数据
  static async getTicker(symbol: string): Promise<{
    symbol: string
    price: number
    change24h: number
    changePercent24h: number
    volume24h: number
    high24h: number
    low24h: number
    lastUpdate: string
  }> {
    return HttpClient.get(`/trading/ticker/${symbol}`)
  }

  // 获取订单簿
  static async getOrderBook(symbol: string): Promise<{
    bids: [number, number][]
    asks: [number, number][]
    lastUpdate: string
  }> {
    return HttpClient.get(`/trading/orderbook/${symbol}`)
  }

  // 获取最近成交记录
  static async getRecentTrades(symbol: string): Promise<{
    id: string
    price: number
    quantity: number
    side: 'BUY' | 'SELL'
    timestamp: string
  }[]> {
    return HttpClient.get(`/trading/trades/${symbol}`)
  }

  // 创建订单（通用）
  static async createOrder(request: {
    symbol: string
    side: 'BUY' | 'SELL'
    type: 'MARKET' | 'LIMIT'
    quantity: number
    price?: number
  }): Promise<{
    orderId: string
    status: string
    filledQuantity: number
    remainingQuantity: number
  }> {
    return HttpClient.post('/trading/order', request)
  }

  // 获取开放订单
  static async getOpenOrders(symbol?: string): Promise<{
    orderId: string
    symbol: string
    side: 'BUY' | 'SELL'
    type: string
    quantity: number
    price: number
    filledQuantity: number
    status: string
    createdAt: string
  }[]> {
    const params = symbol ? { symbol } : {}
    return HttpClient.get('/trading/open-orders', params)
  }

  // 获取订单历史
  static async getOrderHistory(params: {
    symbol?: string
    side?: 'BUY' | 'SELL'
    status?: string
    startTime?: string
    endTime?: string
    pageSize?: number
    page?: number
  } = {}): Promise<{
    data: {
      orderId: string
      symbol: string
      side: 'BUY' | 'SELL'
      type: string
      quantity: number
      price: number
      filledQuantity: number
      status: string
      createdAt: string
      updatedAt: string
    }[]
    pagination: {
      total: number
      page: number
      pageSize: number
      totalPages: number
    }
  }> {
    return HttpClient.get('/trading/order-history', params)
  }
}

// 导出便捷函数以保持向后兼容
export const getCurrentPrice = TradingApi.getCurrentPrice
export const createBuyOrder = TradingApi.createBuyOrder
export const createSellOrder = TradingApi.createSellOrder
export const confirmPayment = TradingApi.confirmPayment
export const cancelOrder = TradingApi.cancelOrder
export const getOrderDetail = TradingApi.getOrderDetail
export const getUserOrders = TradingApi.getUserOrders
export const getTradingStatistics = TradingApi.getTradingStatistics
export const getPaymentMethods = TradingApi.getPaymentMethods
export const getTradingLimits = TradingApi.getTradingLimits
export const getTradingPairs = TradingApi.getTradingPairs
export const getTicker = TradingApi.getTicker
export const getOrderBook = TradingApi.getOrderBook
export const getRecentTrades = TradingApi.getRecentTrades
export const createOrder = TradingApi.createOrder
export const getOpenOrders = TradingApi.getOpenOrders
export const getOrderHistory = TradingApi.getOrderHistory

// 重新导出钱包相关函数以便在交易视图中使用
export { getWalletBalance } from './wallet'

export default TradingApi