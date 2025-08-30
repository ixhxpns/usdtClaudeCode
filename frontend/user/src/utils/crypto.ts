import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt'

// 缓存RSA公钥和加密实例
let cachedPublicKey: string | null = null
let rsaEncrypt: JSEncrypt | null = null
let publicKeyPromise: Promise<string> | null = null

/**
 * 从服务器获取RSA公钥
 */
async function fetchPublicKey(): Promise<string> {
  try {
    const response = await fetch('/api/auth/public-key')
    const data = await response.json()
    
    if (!data.success) {
      throw new Error(data.message || '获取公钥失败')
    }
    
    const publicKey = data.data.publicKey
    // 格式化为PEM格式
    const pemKey = `-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`
    
    console.log('成功获取RSA公钥')
    return pemKey
  } catch (error) {
    console.error('获取RSA公钥失败:', error)
    throw new Error('无法获取加密公钥，请检查网络连接')
  }
}

/**
 * 获取或缓存RSA公钥
 */
async function getPublicKey(): Promise<string> {
  if (cachedPublicKey) {
    return cachedPublicKey
  }
  
  // 避免重复请求
  if (!publicKeyPromise) {
    publicKeyPromise = fetchPublicKey()
  }
  
  try {
    cachedPublicKey = await publicKeyPromise
    return cachedPublicKey
  } catch (error) {
    // 重置Promise以便重试
    publicKeyPromise = null
    throw error
  }
}

/**
 * 初始化RSA加密实例
 */
async function initRSAEncrypt(): Promise<JSEncrypt> {
  if (rsaEncrypt) {
    return rsaEncrypt
  }
  
  const publicKey = await getPublicKey()
  rsaEncrypt = new JSEncrypt()
  rsaEncrypt.setPublicKey(publicKey)
  
  return rsaEncrypt
}

/**
 * RSA加密
 * @param data 要加密的数据
 * @returns 加密后的字符串
 */
export async function rsaEncryptData(data: string): Promise<string> {
  try {
    const encryptInstance = await initRSAEncrypt()
    const encrypted = encryptInstance.encrypt(data)
    
    if (!encrypted) {
      // 清除缓存，下次重新获取公钥
      cachedPublicKey = null
      rsaEncrypt = null
      publicKeyPromise = null
      throw new Error('RSA加密失败，请重试')
    }
    
    return encrypted
  } catch (error) {
    console.error('RSA加密错误:', error)
    throw error
  }
}

/**
 * 加密敏感数据（密码、支付密码等）
 * @param data 敏感数据
 * @returns 加密后的数据
 */
export async function encryptSensitiveData(data: string): Promise<string> {
  try {
    return await rsaEncryptData(data)
  } catch (error) {
    console.error('敏感数据加密失败:', error)
    throw error
  }
}

/**
 * 预加载RSA公钥（可选，用于提升首次加密性能）
 */
export async function preloadPublicKey(): Promise<void> {
  try {
    await getPublicKey()
    console.log('RSA公钥预加载完成')
  } catch (error) {
    console.warn('RSA公钥预加载失败:', error)
  }
}

/**
 * 清除公钥缓存（用于密钥轮换等场景）
 */
export function clearPublicKeyCache(): void {
  cachedPublicKey = null
  rsaEncrypt = null
  publicKeyPromise = null
  console.log('RSA公钥缓存已清除')
}

/**
 * AES加密
 * @param data 要加密的数据
 * @param key 加密密钥
 * @returns 加密后的字符串
 */
export function aesEncrypt(data: string, key: string): string {
  try {
    const encrypted = CryptoJS.AES.encrypt(data, key).toString()
    return encrypted
  } catch (error) {
    console.error('AES加密失败:', error)
    throw error
  }
}

/**
 * AES解密
 * @param encryptedData 加密的数据
 * @param key 解密密钥
 * @returns 解密后的字符串
 */
export function aesDecrypt(encryptedData: string, key: string): string {
  try {
    const decrypted = CryptoJS.AES.decrypt(encryptedData, key)
    return decrypted.toString(CryptoJS.enc.Utf8)
  } catch (error) {
    console.error('AES解密失败:', error)
    throw error
  }
}

/**
 * MD5哈希
 * @param data 要哈希的数据
 * @returns MD5哈希值
 */
export function md5Hash(data: string): string {
  return CryptoJS.MD5(data).toString()
}

/**
 * SHA256哈希
 * @param data 要哈希的数据
 * @returns SHA256哈希值
 */
export function sha256Hash(data: string): string {
  return CryptoJS.SHA256(data).toString()
}

/**
 * 生成随机盐值
 * @param length 盐值长度
 * @returns 随机盐值
 */
export function generateSalt(length: number = 32): string {
  const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let salt = ''
  for (let i = 0; i < length; i++) {
    salt += charset.charAt(Math.floor(Math.random() * charset.length))
  }
  return salt
}

/**
 * 生成UUID
 * @returns UUID字符串
 */
export function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

/**
 * 生成随机字符串
 * @param length 字符串长度
 * @param charset 字符集
 * @returns 随机字符串
 */
export function generateRandomString(
  length: number = 16,
  charset: string = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
): string {
  let result = ''
  for (let i = 0; i < length; i++) {
    result += charset.charAt(Math.floor(Math.random() * charset.length))
  }
  return result
}

/**
 * 本地存储加密
 * @param key 存储键
 * @param value 要存储的值
 * @param encryptionKey 加密密钥（可选，默认使用固定密钥）
 */
export function setEncryptedStorage(
  key: string,
  value: any,
  encryptionKey?: string
): void {
  try {
    const jsonString = JSON.stringify(value)
    const defaultKey = 'USDT_STORAGE_KEY_2024'
    const encrypted = aesEncrypt(jsonString, encryptionKey || defaultKey)
    localStorage.setItem(key, encrypted)
  } catch (error) {
    console.error('加密存储失败:', error)
    throw error
  }
}

/**
 * 本地存储解密
 * @param key 存储键
 * @param encryptionKey 解密密钥（可选，默认使用固定密钥）
 * @returns 解密后的值
 */
export function getEncryptedStorage(key: string, encryptionKey?: string): any {
  try {
    const encrypted = localStorage.getItem(key)
    if (!encrypted) return null
    
    const defaultKey = 'USDT_STORAGE_KEY_2024'
    const decrypted = aesDecrypt(encrypted, encryptionKey || defaultKey)
    return JSON.parse(decrypted)
  } catch (error) {
    console.error('解密存储失败:', error)
    return null
  }
}

/**
 * 密码强度检查
 * @param password 密码
 * @returns 强度评分 (0-4)
 */
export function checkPasswordStrength(password: string): {
  score: number
  feedback: string[]
} {
  const feedback: string[] = []
  let score = 0

  // 长度检查
  if (password.length >= 8) {
    score += 1
  } else {
    feedback.push('密码长度至少8位')
  }

  // 包含小写字母
  if (/[a-z]/.test(password)) {
    score += 1
  } else {
    feedback.push('应包含小写字母')
  }

  // 包含大写字母
  if (/[A-Z]/.test(password)) {
    score += 1
  } else {
    feedback.push('应包含大写字母')
  }

  // 包含数字
  if (/\d/.test(password)) {
    score += 1
  } else {
    feedback.push('应包含数字')
  }

  // 包含特殊字符
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
    score += 1
  } else {
    feedback.push('应包含特殊字符')
  }

  return { score: Math.min(score, 4), feedback }
}

/**
 * 生成安全的随机密码
 * @param length 密码长度
 * @param includeSymbols 是否包含符号
 * @returns 随机密码
 */
export function generateSecurePassword(length: number = 16, includeSymbols: boolean = true): string {
  const lowercase = 'abcdefghijklmnopqrstuvwxyz'
  const uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  const numbers = '0123456789'
  const symbols = '!@#$%^&*(),.?":{}|<>'
  
  let charset = lowercase + uppercase + numbers
  if (includeSymbols) {
    charset += symbols
  }

  let password = ''
  
  // 确保至少包含每种字符类型
  password += lowercase[Math.floor(Math.random() * lowercase.length)]
  password += uppercase[Math.floor(Math.random() * uppercase.length)]
  password += numbers[Math.floor(Math.random() * numbers.length)]
  if (includeSymbols) {
    password += symbols[Math.floor(Math.random() * symbols.length)]
  }

  // 填充剩余长度
  for (let i = password.length; i < length; i++) {
    password += charset[Math.floor(Math.random() * charset.length)]
  }

  // 打乱密码字符顺序
  return password.split('').sort(() => Math.random() - 0.5).join('')
}

/**
 * 数据脱敏
 * @param data 要脱敏的数据
 * @param type 脱敏类型
 * @returns 脱敏后的数据
 */
export function maskSensitiveData(data: string, type: 'email' | 'phone' | 'idcard' | 'bankcard'): string {
  if (!data) return data

  switch (type) {
    case 'email':
      // 邮箱脱敏：保留前2位和@后面的域名
      const emailParts = data.split('@')
      if (emailParts.length === 2) {
        const username = emailParts[0]
        const domain = emailParts[1]
        if (username.length > 2) {
          return `${username.substring(0, 2)}***@${domain}`
        }
      }
      return data

    case 'phone':
      // 手机号脱敏：保留前3位和后4位
      if (data.length >= 7) {
        return `${data.substring(0, 3)}****${data.substring(data.length - 4)}`
      }
      return data

    case 'idcard':
      // 身份证脱敏：保留前6位和后4位
      if (data.length >= 10) {
        return `${data.substring(0, 6)}****${data.substring(data.length - 4)}`
      }
      return data

    case 'bankcard':
      // 银行卡脱敏：保留后4位
      if (data.length > 4) {
        return `**** **** **** ${data.substring(data.length - 4)}`
      }
      return data

    default:
      return data
  }
}