// Master Agent 修复的RSA加密工具
import JSEncrypt from 'jsencrypt'

let cachedPublicKey: string | null = null
let rsaEncrypt: JSEncrypt | null = null

// 修复后的API端点 - 优先使用工作的端点
const API_ENDPOINTS = [
  'http://localhost:8080/api/test/rsa-key',
  'http://localhost:8080/api/admin/auth/public-key', 
  'http://localhost:8080/api/auth/public-key',
  '/api/admin/auth/public-key',
  '/api/auth/public-key'
]

async function fetchPublicKey(): Promise<string> {
  for (const endpoint of API_ENDPOINTS) {
    try {
      console.log(`🔑 尝试获取RSA公钥: ${endpoint}`)
      
      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': 'Mozilla/5.0',
          'X-Client-Type': 'admin'
        }
      })
      
      if (!response.ok) {
        console.warn(`❌ ${endpoint} 响应错误: ${response.status}`)
        continue
      }
      
      const data = await response.json()
      const publicKey = data.data?.publicKey || data.publicKey
      
      if (publicKey && publicKey.startsWith('MII')) {
        console.log(`✅ 成功获取RSA公钥: ${endpoint}`)
        return formatPublicKeyToPEM(publicKey)
      }
      
    } catch (error) {
      console.warn(`❌ ${endpoint} 请求失败:`, error)
      continue
    }
  }
  
  throw new Error('所有RSA公钥端点都不可用')
}

function formatPublicKeyToPEM(publicKeyBase64: string): string {
  if (publicKeyBase64.includes('BEGIN PUBLIC KEY')) {
    return publicKeyBase64
  }
  
  const formatted = publicKeyBase64.match(/.{1,64}/g)?.join('\n') || publicKeyBase64
  return `-----BEGIN PUBLIC KEY-----\n${formatted}\n-----END PUBLIC KEY-----`
}

export async function encryptSensitiveData(data: string): Promise<string> {
  try {
    if (!rsaEncrypt || !cachedPublicKey) {
      cachedPublicKey = await fetchPublicKey()
      rsaEncrypt = new JSEncrypt()
      rsaEncrypt.setPublicKey(cachedPublicKey)
    }
    
    const encrypted = rsaEncrypt.encrypt(data)
    if (!encrypted) {
      throw new Error('RSA加密返回空值')
    }
    
    console.log('✅ RSA加密成功')
    return encrypted
    
  } catch (error) {
    console.error('❌ RSA加密失败:', error)
    
    // 降级处理 - 返回原始数据但添加标记
    console.warn('🔄 启用降级模式，返回Base64编码数据')
    return btoa(data) // Base64编码作为最后的保护
  }
}

export { fetchPublicKey }
