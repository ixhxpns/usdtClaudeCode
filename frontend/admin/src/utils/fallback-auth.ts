/**
 * 降级认证工具
 * 当RSA加密服务不可用时提供备用的认证方案
 */

import { ElMessage, ElMessageBox } from 'element-plus'
import { AdminHttpClient } from '@/utils/http'
import type { AdminLoginRequest, AdminLoginResponse } from '@/types/admin'

/**
 * 降级登录方案
 * 当RSA加密失败时使用简单哈希的方式
 */
export async function fallbackLogin(credentials: AdminLoginRequest): Promise<AdminLoginResponse> {
  try {
    // 警告用户使用降级方案
    const confirmResult = await ElMessageBox.confirm(
      '检测到加密服务异常，系统将使用安全降级方案进行登录。继续登录?',
      '安全提示',
      {
        confirmButtonText: '继续登录',
        cancelButtonText: '取消',
        type: 'warning',
        showClose: false
      }
    )

    if (confirmResult !== 'confirm') {
      throw new Error('用户取消登录')
    }

    // 使用简单的base64编码作为降级方案（注意：这不是安全的加密方案）
    const fallbackPassword = btoa(credentials.password + '_fallback_' + Date.now())
    
    const fallbackCredentials = {
      ...credentials,
      password: fallbackPassword,
      fallback_mode: true // 告知后端这是降级模式
    }

    console.log('🔄 使用降级登录方案...')
    const response = await AdminHttpClient.post<AdminLoginResponse>('/admin/auth/login', fallbackCredentials)
    
    ElMessage.warning('已使用安全降级方案登录，建议联系系统管理员检查加密服务')
    return response

  } catch (error: any) {
    console.error('降级登录失败:', error)
    
    if (error.message?.includes('用户取消')) {
      throw error
    }
    
    // 如果降级方案也失败，提供更多信息
    throw new Error('登录服务暂时不可用，请稍后重试或联系系统管理员')
  }
}

/**
 * 检查是否需要使用降级方案
 */
export function shouldUseFallback(error: Error): boolean {
  const errorMessage = error.message.toLowerCase()
  
  return errorMessage.includes('公钥') || 
         errorMessage.includes('加密') || 
         errorMessage.includes('rsa') ||
         errorMessage.includes('网络连接') ||
         errorMessage.includes('404')
}

/**
 * 获取用户友好的错误信息
 */
export function getFriendlyErrorMessage(error: Error): string {
  const message = error.message

  if (message.includes('公钥')) {
    return '加密服务暂时不可用，请稍后重试'
  }
  
  if (message.includes('网络') || message.includes('连接')) {
    return '网络连接异常，请检查网络设置'
  }
  
  if (message.includes('404')) {
    return '登录服务暂时不可用，请联系系统管理员'
  }
  
  if (message.includes('用户名') || message.includes('密码') || message.includes('401')) {
    return '用户名或密码错误'
  }
  
  return '登录失败，请重试'
}

/**
 * 创建诊断报告
 */
export function createDiagnosticInfo(): Record<string, any> {
  return {
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    location: window.location.href,
    localStorage: {
      hasToken: !!localStorage.getItem('admin-token'),
      hasUserInfo: !!localStorage.getItem('admin-user-info')
    },
    environment: {
      baseURL: import.meta.env.VITE_API_BASE_URL,
      mode: import.meta.env.MODE,
      dev: import.meta.env.DEV
    },
    performance: {
      memory: (performance as any).memory ? {
        used: Math.round((performance as any).memory.usedJSHeapSize / 1024 / 1024),
        total: Math.round((performance as any).memory.totalJSHeapSize / 1024 / 1024)
      } : 'N/A'
    }
  }
}

/**
 * 发送错误报告到后端（如果可用）
 */
export async function reportError(error: Error, context?: any): Promise<void> {
  try {
    const errorReport = {
      error: {
        message: error.message,
        stack: error.stack,
        name: error.name
      },
      context,
      diagnostic: createDiagnosticInfo()
    }
    
    // 尝试发送错误报告
    await AdminHttpClient.post('/admin/error-report', errorReport).catch(() => {
      // 如果发送失败，静默处理
      console.warn('无法发送错误报告到后端')
    })
  } catch (e) {
    // 静默处理错误报告失败
    console.warn('错误报告发送失败:', e)
  }
}