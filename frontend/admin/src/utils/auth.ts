// 认证相关工具函数

const TOKEN_KEY = 'usdt_access_token'
const REFRESH_TOKEN_KEY = 'usdt_refresh_token'
const USER_KEY = 'usdt_user_info'

// Token管理
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

// 用户信息管理
export function getUserInfo(): any {
  const userStr = localStorage.getItem(USER_KEY)
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch (error) {
      console.error('解析用户信息失败:', error)
      return null
    }
  }
  return null
}

export function setUserInfo(user: any): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUserInfo(): void {
  localStorage.removeItem(USER_KEY)
}

// 检查是否已登录
export function isAuthenticated(): boolean {
  const token = getToken()
  if (!token) return false
  
  try {
    // 简单检查token格式（JWT的基本结构）
    const parts = token.split('.')
    if (parts.length !== 3) return false
    
    // 解码payload检查过期时间
    const payload = JSON.parse(atob(parts[1]))
    const now = Math.floor(Date.now() / 1000)
    
    return payload.exp > now
  } catch (error) {
    console.error('Token验证失败:', error)
    return false
  }
}

// 获取Token过期时间
export function getTokenExpiry(): number | null {
  const token = getToken()
  if (!token) return null
  
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    
    const payload = JSON.parse(atob(parts[1]))
    return payload.exp * 1000 // 转换为毫秒
  } catch (error) {
    console.error('获取Token过期时间失败:', error)
    return null
  }
}

// 检查Token是否即将过期（5分钟内）
export function isTokenExpiringSoon(): boolean {
  const expiry = getTokenExpiry()
  if (!expiry) return true
  
  const now = Date.now()
  const fiveMinutes = 5 * 60 * 1000
  
  return (expiry - now) < fiveMinutes
}

// 清除所有认证相关数据
export function clearAuth(): void {
  removeToken()
  removeUserInfo()
  
  // 清除其他可能的敏感数据
  localStorage.removeItem('remember_me')
  
  // 清除会话存储
  sessionStorage.clear()
}

// 设置记住登录状态
export function setRememberMe(remember: boolean): void {
  if (remember) {
    localStorage.setItem('remember_me', 'true')
  } else {
    localStorage.removeItem('remember_me')
  }
}

export function getRememberMe(): boolean {
  return localStorage.getItem('remember_me') === 'true'
}

// 获取登录重定向路径
export function getRedirectPath(): string {
  return sessionStorage.getItem('redirect_path') || '/dashboard'
}

export function setRedirectPath(path: string): void {
  sessionStorage.setItem('redirect_path', path)
}

export function removeRedirectPath(): void {
  sessionStorage.removeItem('redirect_path')
}

// 检查权限
export function hasPermission(permission: string): boolean {
  const user = getUserInfo()
  if (!user || !user.permissions) return false
  
  return user.permissions.includes(permission) || user.permissions.includes('*')
}

export function hasAnyPermission(permissions: string[]): boolean {
  return permissions.some(permission => hasPermission(permission))
}

export function hasAllPermissions(permissions: string[]): boolean {
  return permissions.every(permission => hasPermission(permission))
}

// 检查角色
export function hasRole(role: string): boolean {
  const user = getUserInfo()
  if (!user || !user.roles) return false
  
  return user.roles.includes(role)
}

export function hasAnyRole(roles: string[]): boolean {
  return roles.some(role => hasRole(role))
}

// 获取用户角色
export function getUserRoles(): string[] {
  const user = getUserInfo()
  return user?.roles || []
}

// 获取用户权限
export function getUserPermissions(): string[] {
  const user = getUserInfo()
  return user?.permissions || []
}

// JWT解码工具
export function decodeJWT(token: string): any {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      throw new Error('Invalid JWT format')
    }
    
    const payload = parts[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded)
  } catch (error) {
    console.error('JWT解码失败:', error)
    return null
  }
}

// 安全存储（加密）- 简单的Base64编码，实际应用中应使用更强的加密
export function secureStorage(key: string, value?: string): string | null {
  if (value !== undefined) {
    // 存储
    const encoded = btoa(encodeURIComponent(value))
    localStorage.setItem(key, encoded)
    return null
  } else {
    // 获取
    const encoded = localStorage.getItem(key)
    if (!encoded) return null
    
    try {
      return decodeURIComponent(atob(encoded))
    } catch (error) {
      console.error('安全存储解码失败:', error)
      return null
    }
  }
}

// 登录状态监听器
export type AuthStateChangeListener = (isAuthenticated: boolean) => void

const authStateListeners: AuthStateChangeListener[] = []

export function addAuthStateListener(listener: AuthStateChangeListener): void {
  authStateListeners.push(listener)
}

export function removeAuthStateListener(listener: AuthStateChangeListener): void {
  const index = authStateListeners.indexOf(listener)
  if (index > -1) {
    authStateListeners.splice(index, 1)
  }
}

export function notifyAuthStateChange(isAuthenticated: boolean): void {
  authStateListeners.forEach(listener => {
    try {
      listener(isAuthenticated)
    } catch (error) {
      console.error('认证状态监听器执行失败:', error)
    }
  })
}