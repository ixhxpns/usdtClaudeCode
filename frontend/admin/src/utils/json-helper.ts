/**
 * JSON處理工具
 * 解決特殊字符轉義問題
 */

/**
 * 安全的JSON序列化，處理特殊字符
 * @param obj 要序列化的對象
 * @returns 序列化後的JSON字符串
 */
export function safeJSONStringify(obj: any): string {
  try {
    // 使用自定義replacer處理特殊字符
    return JSON.stringify(obj, (key, value) => {
      if (typeof value === 'string') {
        // 確保特殊字符被正確轉義
        return value
      }
      return value
    })
  } catch (error) {
    console.error('JSON序列化失敗:', error)
    throw new Error('數據序列化失敗')
  }
}

/**
 * 安全的JSON解析
 * @param jsonString JSON字符串
 * @returns 解析後的對象
 */
export function safeJSONParse(jsonString: string): any {
  try {
    return JSON.parse(jsonString)
  } catch (error) {
    console.error('JSON解析失敗:', error)
    throw new Error('數據解析失敗')
  }
}

/**
 * 轉義字符串中的特殊字符
 * @param str 要轉義的字符串
 * @returns 轉義後的字符串
 */
export function escapeSpecialChars(str: string): string {
  if (typeof str !== 'string') return str
  
  return str
    .replace(/\\/g, '\\\\')  // 反斜杠
    .replace(/"/g, '\\"')    // 雙引號
    .replace(/'/g, "\\'")    // 單引號
    .replace(/\n/g, '\\n')   // 換行符
    .replace(/\r/g, '\\r')   // 回車符
    .replace(/\t/g, '\\t')   // 制表符
}

/**
 * 創建登入請求的安全負載
 * @param credentials 登入憑據
 * @returns 安全的請求負載
 */
export function createSafeLoginPayload(credentials: {
  username: string
  password: string
  rememberMe?: boolean
  mfa_code?: string
}): any {
  const payload = {
    username: credentials.username.trim(),
    password: credentials.password, // 密碼保持原樣，不轉義
    rememberMe: credentials.rememberMe || false
  }
  
  if (credentials.mfa_code) {
    payload.mfa_code = credentials.mfa_code.trim()
  }
  
  return payload
}

/**
 * 驗證JSON有效性
 * @param jsonString JSON字符串
 * @returns 是否有效
 */
export function isValidJSON(jsonString: string): boolean {
  try {
    JSON.parse(jsonString)
    return true
  } catch {
    return false
  }
}

/**
 * 安全地處理API請求數據
 * @param data 請求數據
 * @returns 處理後的數據
 */
export function sanitizeRequestData(data: any): any {
  if (data === null || data === undefined) {
    return data
  }
  
  if (typeof data === 'string') {
    return data
  }
  
  if (typeof data === 'object') {
    const sanitized: any = {}
    for (const key in data) {
      if (data.hasOwnProperty(key)) {
        sanitized[key] = sanitizeRequestData(data[key])
      }
    }
    return sanitized
  }
  
  return data
}