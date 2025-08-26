/**
 * 验证工具函数
 * 提供各种字段的验证逻辑
 */

// 邮箱验证正则表达式
const EMAIL_REGEX = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/

// 用户名验证正则表达式 (字母、数字、下划线，4-20位)
const USERNAME_REGEX = /^[a-zA-Z0-9_]{4,20}$/

// 手机号验证正则表达式 (中国大陆手机号)
const PHONE_REGEX = /^1[3-9]\d{9}$/

// 密码强度验证接口
interface PasswordValidationResult {
  valid: boolean
  message: string
  score: number
  requirements: {
    length: boolean
    uppercase: boolean
    lowercase: boolean
    numbers: boolean
    specialChars: boolean
  }
}

/**
 * 验证邮箱地址
 */
export const validateEmail = (email: string): boolean => {
  if (!email) return false
  return EMAIL_REGEX.test(email)
}

/**
 * 验证用户名
 */
export const validateUsername = (username: string): boolean => {
  if (!username) return false
  return USERNAME_REGEX.test(username)
}

/**
 * 验证手机号
 */
export const validatePhone = (phone: string): boolean => {
  if (!phone) return false
  return PHONE_REGEX.test(phone)
}

/**
 * 验证密码强度
 */
export const validatePassword = (password: string): PasswordValidationResult => {
  const result: PasswordValidationResult = {
    valid: false,
    message: '',
    score: 0,
    requirements: {
      length: false,
      uppercase: false,
      lowercase: false,
      numbers: false,
      specialChars: false
    }
  }

  if (!password) {
    result.message = '请输入密码'
    return result
  }

  // 长度检查 (至少8位)
  if (password.length >= 8) {
    result.requirements.length = true
    result.score++
  } else {
    result.message = '密码长度至少需要8位'
    return result
  }

  // 大写字母检查
  if (/[A-Z]/.test(password)) {
    result.requirements.uppercase = true
    result.score++
  }

  // 小写字母检查
  if (/[a-z]/.test(password)) {
    result.requirements.lowercase = true
    result.score++
  }

  // 数字检查
  if (/\d/.test(password)) {
    result.requirements.numbers = true
    result.score++
  }

  // 特殊字符检查
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
    result.requirements.specialChars = true
    result.score++
  }

  // 基本安全要求：长度 + 至少两种字符类型
  const typeCount = [
    result.requirements.uppercase,
    result.requirements.lowercase,
    result.requirements.numbers,
    result.requirements.specialChars
  ].filter(Boolean).length

  if (result.requirements.length && typeCount >= 2) {
    result.valid = true
    result.message = '密码强度合格'
  } else if (typeCount < 2) {
    result.message = '密码需要包含至少两种字符类型（大写字母、小写字母、数字、特殊字符）'
  }

  return result
}

/**
 * 验证确认密码
 */
export const validateConfirmPassword = (password: string, confirmPassword: string): boolean => {
  return password === confirmPassword && password.length > 0
}

/**
 * 验证验证码 (数字，指定长度)
 */
export const validateVerificationCode = (code: string, length: number = 6): boolean => {
  if (!code) return false
  const regex = new RegExp(`^\\d{${length}}$`)
  return regex.test(code)
}

/**
 * 验证身份证号码
 */
export const validateIdCard = (idCard: string): boolean => {
  if (!idCard) return false
  
  // 18位身份证号码正则
  const regex = /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/
  
  if (!regex.test(idCard)) return false
  
  // 校验位验证
  const factors = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checkCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
  
  let sum = 0
  for (let i = 0; i < 17; i++) {
    sum += parseInt(idCard.charAt(i)) * factors[i]
  }
  
  const remainder = sum % 11
  const checkCode = checkCodes[remainder]
  
  return checkCode === idCard.charAt(17).toUpperCase()
}

/**
 * 验证护照号码
 */
export const validatePassport = (passport: string): boolean => {
  if (!passport) return false
  // 护照号码一般为6-20位字母数字组合
  const regex = /^[a-zA-Z0-9]{6,20}$/
  return regex.test(passport)
}

/**
 * 验证银行卡号
 */
export const validateBankCard = (cardNumber: string): boolean => {
  if (!cardNumber) return false
  
  // 移除空格和破折号
  const cleanNumber = cardNumber.replace(/[\s-]/g, '')
  
  // 检查是否为纯数字且长度在13-19位之间
  if (!/^\d{13,19}$/.test(cleanNumber)) return false
  
  // Luhn算法验证
  let sum = 0
  let alternate = false
  
  for (let i = cleanNumber.length - 1; i >= 0; i--) {
    let n = parseInt(cleanNumber.charAt(i))
    
    if (alternate) {
      n *= 2
      if (n > 9) n = (n % 10) + 1
    }
    
    sum += n
    alternate = !alternate
  }
  
  return (sum % 10) === 0
}

/**
 * 验证金额 (正数，最多两位小数)
 */
export const validateAmount = (amount: string | number): boolean => {
  if (!amount) return false
  
  const amountStr = typeof amount === 'number' ? amount.toString() : amount
  const regex = /^\d+(\.\d{1,2})?$/
  
  return regex.test(amountStr) && parseFloat(amountStr) > 0
}

/**
 * 验证交易密码 (6位数字)
 */
export const validateTradingPassword = (password: string): boolean => {
  if (!password) return false
  return /^\d{6}$/.test(password)
}

/**
 * 验证邀请码
 */
export const validateInviteCode = (code: string): boolean => {
  if (!code) return false
  // 邀请码一般为6-12位字母数字组合
  return /^[a-zA-Z0-9]{6,12}$/.test(code)
}

/**
 * 验证URL
 */
export const validateUrl = (url: string): boolean => {
  if (!url) return false
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 验证IP地址
 */
export const validateIP = (ip: string): boolean => {
  if (!ip) return false
  const ipv4Regex = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
  return ipv4Regex.test(ip)
}

/**
 * 通用验证器类
 */
export class Validator {
  static isEmail = validateEmail
  static isUsername = validateUsername
  static isPhone = validatePhone
  static isIdCard = validateIdCard
  static isPassport = validatePassport
  static isBankCard = validateBankCard
  static isAmount = validateAmount
  static isTradingPassword = validateTradingPassword
  static isInviteCode = validateInviteCode
  static isUrl = validateUrl
  static isIP = validateIP
  
  /**
   * 检查密码强度
   */
  static checkPassword(password: string, minLength: number = 8) {
    const result = validatePassword(password)
    return {
      valid: result.valid,
      errors: result.valid ? [] : [result.message],
      score: result.score,
      requirements: result.requirements
    }
  }
  
  /**
   * 验证必填字段
   */
  static required(value: any, fieldName: string = '字段') {
    if (value === null || value === undefined || value === '') {
      return { valid: false, message: `${fieldName}不能为空` }
    }
    return { valid: true, message: '' }
  }
  
  /**
   * 验证字符串长度
   */
  static length(value: string, min: number, max?: number, fieldName: string = '字段') {
    if (!value) {
      return { valid: false, message: `${fieldName}不能为空` }
    }
    
    if (value.length < min) {
      return { valid: false, message: `${fieldName}长度不能少于${min}位` }
    }
    
    if (max && value.length > max) {
      return { valid: false, message: `${fieldName}长度不能超过${max}位` }
    }
    
    return { valid: true, message: '' }
  }
  
  /**
   * 验证数值范围
   */
  static range(value: number, min: number, max: number, fieldName: string = '数值') {
    if (value < min || value > max) {
      return { valid: false, message: `${fieldName}应在${min}-${max}之间` }
    }
    return { valid: true, message: '' }
  }
}

export default Validator