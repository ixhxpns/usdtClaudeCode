import { HttpClient } from '@/utils/http'
import type { ApiResponse } from '@/types/api'

// KYC相關API接口
export interface KycInfo {
  id: number
  userId: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  level?: number
  realName?: string
  idNumber?: string
  phone?: string
  email?: string
  address?: string
  idCardFront?: string
  idCardBack?: string
  selfieWithId?: string
  bankAccount?: string
  bankName?: string
  rejectReason?: string
  submittedAt?: string
  reviewedAt?: string
  createdAt: string
  updatedAt: string
}

export interface KycSubmitRequest {
  realName: string
  idNumber: string
  phone: string
  email: string
  address: string
  bankAccount: string
  bankName: string
}

export interface KycDocumentUploadResponse {
  fileId: string
  fileName: string
  fileUrl: string
  uploadTime: string
}

export interface KycStatus {
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'NOT_SUBMITTED'
  level: number
  canSubmit: boolean
  canResubmit: boolean
  rejectReason?: string
  submittedAt?: string
  reviewedAt?: string
  nextSteps?: string[]
}

export interface KycLevelInfo {
  level: number
  name: string
  requirements: string[]
  benefits: string[]
  limits: {
    dailyTrade: number
    singleTrade: number
    monthlyWithdrawal: number
  }
}

// KYC服務類
export class KycApi {
  // 獲取KYC狀態
  static async getKycStatus(): Promise<KycStatus> {
    return HttpClient.get<KycStatus>('/kyc/status')
  }

  // 獲取KYC詳細信息
  static async getKycInfo(): Promise<KycInfo> {
    return HttpClient.get<KycInfo>('/kyc/info')
  }

  // 提交KYC基本信息
  static async submitKycInfo(request: KycSubmitRequest): Promise<string> {
    return HttpClient.post<string>('/kyc/submit', request)
  }

  // 上傳KYC文檔
  static async uploadKycDocument(
    documentType: 'idCardFront' | 'idCardBack' | 'selfieWithId',
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<KycDocumentUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('documentType', documentType)
    
    return HttpClient.upload<KycDocumentUploadResponse>('/kyc/upload-document', formData, onProgress)
  }

  // 重新提交KYC
  static async resubmitKyc(request: KycSubmitRequest): Promise<string> {
    return HttpClient.put<string>('/kyc/resubmit', request)
  }

  // 獲取KYC等級信息
  static async getKycLevels(): Promise<KycLevelInfo[]> {
    return HttpClient.get<KycLevelInfo[]>('/kyc/levels')
  }

  // 獲取當前用戶的KYC等級詳情
  static async getCurrentKycLevel(): Promise<KycLevelInfo> {
    return HttpClient.get<KycLevelInfo>('/kyc/current-level')
  }

  // 提交KYC申请
  static async submitKycApplication(request: KycSubmitRequest): Promise<string> {
    return HttpClient.post<string>('/kyc/application', request)
  }

  // 保存KYC草稿
  static async saveDraftKyc(request: Partial<KycSubmitRequest>): Promise<string> {
    return HttpClient.post<string>('/kyc/draft', request)
  }
}

// 导出便捷函数以保持向后兼容
export const getKycStatus = KycApi.getKycStatus
export const getKycInfo = KycApi.getKycInfo
export const submitKycInfo = KycApi.submitKycInfo
export const uploadKycDocument = KycApi.uploadKycDocument
export const resubmitKyc = KycApi.resubmitKyc
export const getKycLevels = KycApi.getKycLevels
export const getCurrentKycLevel = KycApi.getCurrentKycLevel
export const submitKycApplication = KycApi.submitKycApplication
export const saveDraftKyc = KycApi.saveDraftKyc

// 刷新KYC状态的别名函数
export const refreshKycStatus = KycApi.getKycStatus

export default KycApi