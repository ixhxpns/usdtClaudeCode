import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 数字格式化
 */
export class NumberFormatter {
  /**
   * 格式化金额
   * @param amount 金额
   * @param precision 精度
   * @param currency 货币符号
   * @returns 格式化后的金额
   */
  static formatCurrency(
    amount: number | string,
    precision: number = 2,
    currency: string = '¥'
  ): string {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    if (isNaN(num)) return '0.00'
    
    return `${currency}${num.toLocaleString('zh-CN', {
      minimumFractionDigits: precision,
      maximumFractionDigits: precision
    })}`
  }

  /**
   * 格式化USDT金额
   * @param amount USDT金额
   * @param precision 精度
   * @returns 格式化后的USDT金额
   */
  static formatUSDT(amount: number | string, precision: number = 6): string {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    if (isNaN(num)) return '0.000000'
    
    return `${num.toLocaleString('en-US', {
      minimumFractionDigits: precision,
      maximumFractionDigits: precision
    })} USDT`
  }

  /**
   * 格式化百分比
   * @param value 数值
   * @param precision 精度
   * @returns 格式化后的百分比
   */
  static formatPercent(value: number | string, precision: number = 2): string {
    const num = typeof value === 'string' ? parseFloat(value) : value
    if (isNaN(num)) return '0.00%'
    
    return `${(num * 100).toFixed(precision)}%`
  }

  /**
   * 格式化大数字（K, M, B）
   * @param value 数值
   * @param precision 精度
   * @returns 格式化后的数字
   */
  static formatLargeNumber(value: number | string, precision: number = 1): string {
    const num = typeof value === 'string' ? parseFloat(value) : value
    if (isNaN(num)) return '0'

    if (num >= 1e9) {
      return `${(num / 1e9).toFixed(precision)}B`
    } else if (num >= 1e6) {
      return `${(num / 1e6).toFixed(precision)}M`
    } else if (num >= 1e3) {
      return `${(num / 1e3).toFixed(precision)}K`
    }
    
    return num.toString()
  }

  /**
   * 安全解析数字
   * @param value 输入值
   * @param defaultValue 默认值
   * @returns 解析后的数字
   */
  static safeParseNumber(value: any, defaultValue: number = 0): number {
    if (typeof value === 'number' && !isNaN(value)) {
      return value
    }
    
    if (typeof value === 'string') {
      const parsed = parseFloat(value)
      return isNaN(parsed) ? defaultValue : parsed
    }
    
    return defaultValue
  }
}

/**
 * 日期时间工具
 */
export class DateFormatter {
  /**
   * 格式化日期时间
   * @param date 日期
   * @param format 格式
   * @returns 格式化后的日期时间
   */
  static format(date: string | Date, format: string = 'YYYY-MM-DD HH:mm:ss'): string {
    if (!date) return ''
    return dayjs(date).format(format)
  }

  /**
   * 相对时间
   * @param date 日期
   * @returns 相对时间描述
   */
  static fromNow(date: string | Date): string {
    if (!date) return ''
    return dayjs(date).fromNow()
  }

  /**
   * 获取日期范围
   * @param type 类型
   * @returns 日期范围
   */
  static getDateRange(type: 'today' | 'yesterday' | 'week' | 'month' | 'quarter' | 'year'): [string, string] {
    const now = dayjs()
    
    switch (type) {
      case 'today':
        return [
          now.startOf('day').format('YYYY-MM-DD HH:mm:ss'),
          now.endOf('day').format('YYYY-MM-DD HH:mm:ss')
        ]
      case 'yesterday':
        const yesterday = now.subtract(1, 'day')
        return [
          yesterday.startOf('day').format('YYYY-MM-DD HH:mm:ss'),
          yesterday.endOf('day').format('YYYY-MM-DD HH:mm:ss')
        ]
      case 'week':
        return [
          now.startOf('week').format('YYYY-MM-DD HH:mm:ss'),
          now.endOf('week').format('YYYY-MM-DD HH:mm:ss')
        ]
      case 'month':
        return [
          now.startOf('month').format('YYYY-MM-DD HH:mm:ss'),
          now.endOf('month').format('YYYY-MM-DD HH:mm:ss')
        ]
      case 'quarter':
        return [
          now.startOf('quarter').format('YYYY-MM-DD HH:mm:ss'),
          now.endOf('quarter').format('YYYY-MM-DD HH:mm:ss')
        ]
      case 'year':
        return [
          now.startOf('year').format('YYYY-MM-DD HH:mm:ss'),
          now.endOf('year').format('YYYY-MM-DD HH:mm:ss')
        ]
      default:
        return ['', '']
    }
  }

  /**
   * 检查日期是否有效
   * @param date 日期
   * @returns 是否有效
   */
  static isValid(date: string | Date): boolean {
    return dayjs(date).isValid()
  }
}

/**
 * 字符串工具
 */
export class StringHelper {
  /**
   * 截取字符串
   * @param str 字符串
   * @param length 长度
   * @param suffix 后缀
   * @returns 截取后的字符串
   */
  static truncate(str: string, length: number, suffix: string = '...'): string {
    if (!str || str.length <= length) return str
    return str.substring(0, length) + suffix
  }

  /**
   * 首字母大写
   * @param str 字符串
   * @returns 首字母大写的字符串
   */
  static capitalize(str: string): string {
    if (!str) return str
    return str.charAt(0).toUpperCase() + str.slice(1)
  }

  /**
   * 驼峰转下划线
   * @param str 驼峰字符串
   * @returns 下划线字符串
   */
  static camelToSnake(str: string): string {
    return str.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`)
  }

  /**
   * 下划线转驼峰
   * @param str 下划线字符串
   * @returns 驼峰字符串
   */
  static snakeToCamel(str: string): string {
    return str.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
  }

  /**
   * 生成随机字符串
   * @param length 长度
   * @param charset 字符集
   * @returns 随机字符串
   */
  static random(length: number = 8, charset?: string): string {
    const defaultCharset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    const chars = charset || defaultCharset
    let result = ''
    
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    
    return result
  }

  /**
   * 安全的JSON解析
   * @param str JSON字符串
   * @param defaultValue 默认值
   * @returns 解析结果
   */
  static safeParseJSON(str: string, defaultValue: any = null): any {
    try {
      return JSON.parse(str)
    } catch {
      return defaultValue
    }
  }
}

/**
 * 验证工具
 */
export class Validator {
  /**
   * 邮箱验证
   * @param email 邮箱
   * @returns 是否有效
   */
  static isEmail(email: string): boolean {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return regex.test(email)
  }

  /**
   * 手机号验证（中国大陆）
   * @param phone 手机号
   * @returns 是否有效
   */
  static isPhone(phone: string): boolean {
    const regex = /^1[3-9]\d{9}$/
    return regex.test(phone)
  }

  /**
   * 身份证验证（中国大陆）
   * @param idCard 身份证号
   * @returns 是否有效
   */
  static isIdCard(idCard: string): boolean {
    const regex = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
    return regex.test(idCard)
  }

  /**
   * URL验证
   * @param url URL
   * @returns 是否有效
   */
  static isUrl(url: string): boolean {
    try {
      new URL(url)
      return true
    } catch {
      return false
    }
  }

  /**
   * 密码强度验证
   * @param password 密码
   * @param minLength 最小长度
   * @returns 验证结果
   */
  static checkPassword(password: string, minLength: number = 8): {
    valid: boolean
    errors: string[]
  } {
    const errors: string[] = []

    if (password.length < minLength) {
      errors.push(`密码长度至少${minLength}位`)
    }

    if (!/[a-z]/.test(password)) {
      errors.push('密码必须包含小写字母')
    }

    if (!/[A-Z]/.test(password)) {
      errors.push('密码必须包含大写字母')
    }

    if (!/\d/.test(password)) {
      errors.push('密码必须包含数字')
    }

    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      errors.push('密码必须包含特殊字符')
    }

    return {
      valid: errors.length === 0,
      errors
    }
  }
}

/**
 * 文件工具
 */
export class FileHelper {
  /**
   * 格式化文件大小
   * @param size 文件大小（字节）
   * @returns 格式化后的文件大小
   */
  static formatSize(size: number): string {
    if (size === 0) return '0 B'
    
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(size) / Math.log(k))
    
    return `${parseFloat((size / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
  }

  /**
   * 获取文件扩展名
   * @param filename 文件名
   * @returns 扩展名
   */
  static getExtension(filename: string): string {
    const lastDot = filename.lastIndexOf('.')
    return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : ''
  }

  /**
   * 检查文件类型
   * @param file 文件对象
   * @param allowedTypes 允许的类型
   * @returns 是否允许
   */
  static isAllowedType(file: File, allowedTypes: string[]): boolean {
    const extension = this.getExtension(file.name)
    const mimeType = file.type.toLowerCase()
    
    return allowedTypes.some(type => {
      if (type.startsWith('.')) {
        // 扩展名匹配
        return extension === type.substring(1)
      } else if (type.includes('/')) {
        // MIME类型匹配
        return mimeType === type || mimeType.startsWith(type.replace('*', ''))
      }
      return false
    })
  }

  /**
   * 文件转Base64
   * @param file 文件对象
   * @returns Promise<Base64字符串>
   */
  static toBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = error => reject(error)
      reader.readAsDataURL(file)
    })
  }
}

/**
 * URL工具
 */
export class UrlHelper {
  /**
   * 解析URL参数
   * @param url URL字符串
   * @returns 参数对象
   */
  static parseQuery(url?: string): Record<string, string> {
    const query = (url || window.location.search).split('?')[1]
    if (!query) return {}
    
    const params: Record<string, string> = {}
    query.split('&').forEach(param => {
      const [key, value] = param.split('=')
      if (key) {
        params[decodeURIComponent(key)] = decodeURIComponent(value || '')
      }
    })
    
    return params
  }

  /**
   * 构建URL参数
   * @param params 参数对象
   * @returns 查询字符串
   */
  static buildQuery(params: Record<string, any>): string {
    const query = Object.keys(params)
      .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&')
    
    return query ? `?${query}` : ''
  }

  /**
   * 合并URL和参数
   * @param url 基础URL
   * @param params 参数对象
   * @returns 完整URL
   */
  static combineUrl(url: string, params?: Record<string, any>): string {
    if (!params || Object.keys(params).length === 0) return url
    
    const query = this.buildQuery(params)
    const separator = url.includes('?') ? '&' : ''
    
    return `${url}${separator}${query.substring(1)}`
  }
}

/**
 * 深度克隆
 * @param obj 要克隆的对象
 * @returns 克隆后的对象
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') return obj
  
  if (obj instanceof Date) return new Date(obj.getTime()) as T
  if (obj instanceof Array) return obj.map(item => deepClone(item)) as T
  
  if (typeof obj === 'object') {
    const copy = {} as T
    Object.keys(obj as object).forEach(key => {
      copy[key as keyof T] = deepClone((obj as any)[key])
    })
    return copy
  }
  
  return obj
}

/**
 * 防抖函数
 * @param fn 要执行的函数
 * @param delay 延迟时间
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout>
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn.apply(this, args), delay)
  }
}

/**
 * 节流函数
 * @param fn 要执行的函数
 * @param delay 延迟时间
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0
  
  return (...args: Parameters<T>) => {
    const now = Date.now()
    if (now - lastCall >= delay) {
      lastCall = now
      fn.apply(this, args)
    }
  }
}

/**
 * 等待指定时间
 * @param ms 毫秒数
 * @returns Promise
 */
export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}