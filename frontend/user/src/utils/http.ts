import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { ElMessage, ElLoading } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ApiResponse, ApiErrorResponse } from '@/types/api'
import { getToken, removeToken } from '@/utils/auth'

// 创建axios实例
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
http.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    // 添加认证token
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加请求ID用于追踪
    if (config.headers) {
      config.headers['X-Request-ID'] = generateRequestId()
    }

    // 添加客户端版本信息
    if (config.headers) {
      config.headers['X-Client-Version'] = import.meta.env.VITE_APP_VERSION || '1.0.0'
    }

    return config
  },
  (error: AxiosError) => {
    console.error('Request interceptor error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response

    // 检查业务状态码
    if (data.success === false) {
      const errorResponse = data as ApiErrorResponse
      
      // 处理认证失败
      if (data.code === 401) {
        const authStore = useAuthStore()
        authStore.logout()
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
        return Promise.reject(new Error('Unauthorized'))
      }

      // 处理其他业务错误
      const errorMessage = errorResponse.message || '请求失败'
      ElMessage.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }

    return response
  },
  (error: AxiosError<ApiErrorResponse>) => {
    // 网络错误处理
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络设置')
      return Promise.reject(error)
    }

    const { status, data } = error.response

    switch (status) {
      case 400:
        ElMessage.error(data?.message || '请求参数错误')
        break
      case 401:
        const authStore = useAuthStore()
        authStore.logout()
        removeToken()
        ElMessage.error('登录已过期，请重新登录')
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
        break
      case 403:
        ElMessage.error('权限不足，无法访问该资源')
        break
      case 404:
        ElMessage.error('请求的资源不存在')
        break
      case 422:
        // 表单验证错误
        if (data?.errors && data.errors.length > 0) {
          const firstError = data.errors[0]
          ElMessage.error(firstError.message || '表单验证失败')
        } else {
          ElMessage.error(data?.message || '请求参数验证失败')
        }
        break
      case 429:
        ElMessage.error('请求频率过高，请稍后重试')
        break
      case 500:
        ElMessage.error('服务器内部错误，请稍后重试')
        break
      case 502:
        ElMessage.error('网关错误，请稍后重试')
        break
      case 503:
        ElMessage.error('服务暂时不可用，请稍后重试')
        break
      default:
        ElMessage.error(data?.message || `请求失败 (${status})`)
    }

    return Promise.reject(error)
  }
)

// 生成请求ID
function generateRequestId(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

// 请求方法封装
export class HttpClient {
  private static loadingInstance: any = null
  private static loadingCount = 0

  // GET请求
  static async get<T = any>(
    url: string,
    params?: Record<string, any>,
    config?: AxiosRequestConfig
  ): Promise<T> {
    try {
      const response = await http.get<ApiResponse<T>>(url, {
        params,
        ...config
      })
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  // POST请求
  static async post<T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    try {
      const response = await http.post<ApiResponse<T>>(url, data, config)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  // PUT请求
  static async put<T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    try {
      const response = await http.put<ApiResponse<T>>(url, data, config)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  // DELETE请求
  static async delete<T = any>(
    url: string,
    config?: AxiosRequestConfig
  ): Promise<T> {
    try {
      const response = await http.delete<ApiResponse<T>>(url, config)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  // PATCH请求
  static async patch<T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    try {
      const response = await http.patch<ApiResponse<T>>(url, data, config)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  // 上传文件
  static async upload<T = any>(
    url: string,
    formData: FormData,
    onProgress?: (progress: number) => void
  ): Promise<T> {
    try {
      const response = await http.post<ApiResponse<T>>(url, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(progress)
          }
        }
      })
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  // 下载文件
  static async download(url: string, filename?: string): Promise<void> {
    try {
      const response = await http.get(url, {
        responseType: 'blob'
      })
      
      const blob = new Blob([response.data])
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
    } catch (error) {
      throw error
    }
  }

  // 显示全局loading
  static showLoading(text = '加载中...') {
    this.loadingCount++
    if (this.loadingCount === 1) {
      this.loadingInstance = ElLoading.service({
        lock: true,
        text,
        background: 'rgba(0, 0, 0, 0.7)'
      })
    }
  }

  // 隐藏全局loading
  static hideLoading() {
    this.loadingCount = Math.max(0, this.loadingCount - 1)
    if (this.loadingCount === 0 && this.loadingInstance) {
      this.loadingInstance.close()
      this.loadingInstance = null
    }
  }

  // 带loading的请求
  static async requestWithLoading<T>(
    requestFn: () => Promise<T>,
    loadingText = '处理中...'
  ): Promise<T> {
    try {
      this.showLoading(loadingText)
      const result = await requestFn()
      return result
    } finally {
      this.hideLoading()
    }
  }
}

// 别名导出，保持向后兼容
export const api = http

export default http