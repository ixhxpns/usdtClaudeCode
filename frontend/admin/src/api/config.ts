// Master Agent 修复的API配置
export const API_BASE_URL = 'http://localhost:8090'
export const API_ENDPOINTS = {
  // 管理员认证端点
  ADMIN_LOGIN: '/api/admin/auth/login',
  ADMIN_LOGOUT: '/api/admin/auth/logout',
  ADMIN_CURRENT_USER: '/api/admin/auth/me',
  ADMIN_PUBLIC_KEY: '/api/admin/auth/public-key',
  
  // 备用端点
  PUBLIC_KEY_FALLBACK: '/api/auth/public-key'
}
