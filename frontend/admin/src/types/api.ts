// API相关的类型定义

export interface ApiResponse<T = any> {
  success: boolean
  code: number
  message: string
  data: T
  timestamp: string
  request_id: string
}

export interface ApiError {
  field?: string
  message: string
}

export interface ApiErrorResponse {
  success: false
  code: number
  message: string
  errors?: ApiError[]
  timestamp: string
  request_id: string
}

export interface PaginationParams {
  page?: number
  size?: number
  sort?: string
  order?: 'asc' | 'desc'
}

export interface PaginationResponse<T = any> {
  success: boolean
  code: number
  message: string
  data: T[]
  pagination: {
    page: number
    size: number
    total: number
    total_pages: number
    has_next: boolean
    has_prev: boolean
  }
  timestamp: string
  request_id: string
}

// HTTP请求配置
export interface RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  params?: Record<string, any>
  data?: any
  headers?: Record<string, string>
  timeout?: number
  withCredentials?: boolean
}

// 文件上传相关
export interface UploadConfig {
  url: string
  field: string
  accept?: string
  maxSize?: number // MB
  multiple?: boolean
}

export interface UploadResponse {
  filename: string
  url: string
  size: number
  type: string
}