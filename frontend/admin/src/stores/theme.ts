import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { Theme } from '@/types/common'

export const useThemeStore = defineStore('theme', () => {
  // 状态
  const currentTheme = ref<Theme>('auto')
  const isDarkMode = ref(false)
  
  // 计算属性
  const isLightMode = computed(() => !isDarkMode.value)
  const effectiveTheme = computed(() => {
    if (currentTheme.value === 'auto') {
      return isDarkMode.value ? 'dark' : 'light'
    }
    return currentTheme.value
  })

  // 检测系统主题
  const detectSystemTheme = () => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  // 应用主题到DOM
  const applyTheme = (theme: 'light' | 'dark') => {
    const htmlElement = document.documentElement
    const bodyElement = document.body
    
    if (theme === 'dark') {
      htmlElement.classList.add('dark')
      bodyElement.classList.add('dark')
      isDarkMode.value = true
    } else {
      htmlElement.classList.remove('dark')
      bodyElement.classList.remove('dark')
      isDarkMode.value = false
    }
    
    // 设置CSS变量
    htmlElement.setAttribute('data-theme', theme)
  }

  // 设置主题
  const setTheme = (theme: Theme) => {
    currentTheme.value = theme
    localStorage.setItem('theme', theme)
    
    if (theme === 'auto') {
      const systemIsDark = detectSystemTheme()
      applyTheme(systemIsDark ? 'dark' : 'light')
    } else {
      applyTheme(theme)
    }
  }

  // 切换主题
  const toggleTheme = () => {
    if (currentTheme.value === 'light') {
      setTheme('dark')
    } else if (currentTheme.value === 'dark') {
      setTheme('light')
    } else {
      // 如果是auto，切换到相反的固定主题
      setTheme(isDarkMode.value ? 'light' : 'dark')
    }
  }

  // 监听系统主题变化
  const watchSystemTheme = () => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    
    const handleChange = (e: MediaQueryListEvent) => {
      if (currentTheme.value === 'auto') {
        applyTheme(e.matches ? 'dark' : 'light')
      }
    }
    
    mediaQuery.addEventListener('change', handleChange)
    
    // 返回清理函数
    return () => {
      mediaQuery.removeEventListener('change', handleChange)
    }
  }

  // 初始化主题
  const initializeTheme = () => {
    // 从localStorage读取主题设置
    const savedTheme = localStorage.getItem('theme') as Theme
    
    if (savedTheme && ['light', 'dark', 'auto'].includes(savedTheme)) {
      currentTheme.value = savedTheme
    } else {
      currentTheme.value = 'auto'
    }
    
    // 应用主题
    if (currentTheme.value === 'auto') {
      const systemIsDark = detectSystemTheme()
      applyTheme(systemIsDark ? 'dark' : 'light')
    } else {
      applyTheme(currentTheme.value)
    }
    
    // 开始监听系统主题变化
    watchSystemTheme()
  }

  // 获取主题配置
  const getThemeConfig = () => {
    return {
      current: currentTheme.value,
      effective: effectiveTheme.value,
      isDark: isDarkMode.value,
      isLight: isLightMode.value
    }
  }

  // 监听主题变化
  watch(
    () => currentTheme.value,
    (newTheme) => {
      // 发送主题变化事件
      window.dispatchEvent(new CustomEvent('themeChange', {
        detail: {
          theme: newTheme,
          effective: effectiveTheme.value,
          isDark: isDarkMode.value
        }
      }))
    }
  )

  return {
    // 状态
    currentTheme: readonly(currentTheme),
    isDarkMode: readonly(isDarkMode),
    
    // 计算属性
    isLightMode,
    effectiveTheme,
    
    // 方法
    setTheme,
    toggleTheme,
    initializeTheme,
    getThemeConfig,
    detectSystemTheme
  }
})