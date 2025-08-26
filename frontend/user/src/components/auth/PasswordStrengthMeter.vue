<template>
  <div v-if="password" class="password-strength-meter mt-2">
    <!-- Strength Bars -->
    <div class="flex items-center space-x-1 mb-2">
      <div 
        v-for="i in 5" 
        :key="i"
        class="flex-1 h-1.5 rounded transition-colors duration-200"
        :class="i <= strength.score ? getStrengthColor() : 'bg-gray-200 dark:bg-gray-600'"
      />
    </div>

    <!-- Strength Text and Score -->
    <div class="flex items-center justify-between mb-2">
      <span 
        class="text-sm font-medium transition-colors duration-200"
        :class="getStrengthTextColor()"
      >
        {{ getStrengthText() }}
      </span>
      <span class="text-xs text-gray-500 dark:text-gray-400">
        {{ strength.score }}/5
      </span>
    </div>

    <!-- Requirements List -->
    <div class="space-y-1">
      <div 
        v-for="requirement in requirements" 
        :key="requirement.key"
        class="flex items-center space-x-2 text-xs"
      >
        <el-icon 
          :size="12" 
          :class="requirement.met ? 'text-green-500' : 'text-gray-400'"
        >
          <Check v-if="requirement.met" />
          <Close v-else />
        </el-icon>
        <span 
          :class="requirement.met ? 'text-green-600 dark:text-green-400' : 'text-gray-500 dark:text-gray-400'"
        >
          {{ requirement.label }}
        </span>
      </div>
    </div>

    <!-- Additional Feedback -->
    <div v-if="strength.feedback.length > 0" class="mt-2">
      <p class="text-xs text-gray-600 dark:text-gray-400 mb-1">建议:</p>
      <ul class="text-xs text-gray-500 dark:text-gray-400 space-y-0.5">
        <li v-for="tip in strength.feedback" :key="tip" class="flex items-start space-x-1">
          <span class="inline-block w-1 h-1 bg-gray-400 rounded-full mt-1.5 flex-shrink-0"></span>
          <span>{{ tip }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'

interface Props {
  password: string
  minLength?: number
  requireUppercase?: boolean
  requireLowercase?: boolean
  requireNumbers?: boolean
  requireSpecialChars?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  minLength: 8,
  requireUppercase: true,
  requireLowercase: true,
  requireNumbers: true,
  requireSpecialChars: true
})

interface PasswordStrength {
  score: number
  feedback: string[]
  entropy: number
  crackTime: string
}

// 计算密码强度
const strength = computed((): PasswordStrength => {
  if (!props.password) {
    return { score: 0, feedback: [], entropy: 0, crackTime: '' }
  }

  let score = 0
  const feedback: string[] = []
  
  // 长度检查
  if (props.password.length >= props.minLength) {
    score += 1
  } else {
    feedback.push(`至少需要 ${props.minLength} 个字符`)
  }
  
  // 大写字母
  if (/[A-Z]/.test(props.password)) {
    score += 1
  } else if (props.requireUppercase) {
    feedback.push('包含大写字母')
  }
  
  // 小写字母
  if (/[a-z]/.test(props.password)) {
    score += 1
  } else if (props.requireLowercase) {
    feedback.push('包含小写字母')
  }
  
  // 数字
  if (/\d/.test(props.password)) {
    score += 1
  } else if (props.requireNumbers) {
    feedback.push('包含数字')
  }
  
  // 特殊字符
  if (/[!@#$%^&*(),.?":{}|<>]/.test(props.password)) {
    score += 1
  } else if (props.requireSpecialChars) {
    feedback.push('包含特殊字符 (!@#$%^&*等)')
  }
  
  // 额外加分项
  if (props.password.length >= 12) score += 0.5
  if (props.password.length >= 16) score += 0.5
  if (/[!@#$%^&*()_+={}\[\]|\\:";'<>,.?/~`]/.test(props.password)) score += 0.5
  
  // 减分项
  if (/(.)\1{2,}/.test(props.password)) {
    score -= 1
    feedback.push('避免连续重复字符')
  }
  
  if (/123|abc|qwe/i.test(props.password)) {
    score -= 1
    feedback.push('避免常见字符序列')
  }
  
  // 计算熵值和破解时间
  const entropy = calculateEntropy(props.password)
  const crackTime = estimateCrackTime(entropy)
  
  return {
    score: Math.max(0, Math.min(5, Math.floor(score))),
    feedback,
    entropy,
    crackTime
  }
})

// 密码要求检查
const requirements = computed(() => [
  {
    key: 'length',
    label: `至少 ${props.minLength} 个字符`,
    met: props.password.length >= props.minLength
  },
  {
    key: 'uppercase',
    label: '包含大写字母',
    met: /[A-Z]/.test(props.password)
  },
  {
    key: 'lowercase',
    label: '包含小写字母',
    met: /[a-z]/.test(props.password)
  },
  {
    key: 'numbers',
    label: '包含数字',
    met: /\d/.test(props.password)
  },
  {
    key: 'special',
    label: '包含特殊字符',
    met: /[!@#$%^&*(),.?":{}|<>]/.test(props.password)
  }
])

// 获取强度条颜色
const getStrengthColor = () => {
  const colors = [
    'bg-red-400',
    'bg-red-400',
    'bg-orange-400',
    'bg-yellow-400',
    'bg-green-400',
    'bg-green-500'
  ]
  return colors[strength.value.score] || 'bg-gray-200'
}

// 获取强度文字颜色
const getStrengthTextColor = () => {
  const colors = [
    'text-red-500',
    'text-red-500',
    'text-orange-500',
    'text-yellow-500',
    'text-green-500',
    'text-green-600'
  ]
  return colors[strength.value.score] || 'text-gray-500'
}

// 获取强度文字
const getStrengthText = () => {
  const texts = ['很弱', '弱', '一般', '较强', '强', '很强']
  return texts[strength.value.score] || '无'
}

// 计算密码熵值
const calculateEntropy = (password: string): number => {
  if (!password) return 0
  
  let charset = 0
  if (/[a-z]/.test(password)) charset += 26
  if (/[A-Z]/.test(password)) charset += 26
  if (/\d/.test(password)) charset += 10
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) charset += 32
  
  return Math.log2(Math.pow(charset, password.length))
}

// 估算破解时间
const estimateCrackTime = (entropy: number): string => {
  if (entropy === 0) return '立即'
  
  // 假设每秒可以尝试1亿次
  const attemptsPerSecond = 100000000
  const averageAttempts = Math.pow(2, entropy - 1)
  const seconds = averageAttempts / attemptsPerSecond
  
  if (seconds < 60) return '不到1分钟'
  if (seconds < 3600) return `${Math.ceil(seconds / 60)}分钟`
  if (seconds < 86400) return `${Math.ceil(seconds / 3600)}小时`
  if (seconds < 31536000) return `${Math.ceil(seconds / 86400)}天`
  if (seconds < 3153600000) return `${Math.ceil(seconds / 31536000)}年`
  return '几个世纪'
}
</script>

<style scoped>
.password-strength-meter {
  @apply transition-all duration-200;
}
</style>