<template>
  <div class="verification-code-input">
    <!-- 验证码输入框 -->
    <div class="flex items-center justify-center space-x-2 mb-4">
      <input
        v-for="(digit, index) in digits"
        :key="index"
        :ref="(el) => setInputRef(el, index)"
        v-model="digits[index]"
        type="text"
        maxlength="1"
        :class="[
          'w-12 h-12 text-center text-lg font-semibold border-2 rounded-lg',
          'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500',
          'transition-colors duration-200',
          digit ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20' : 'border-gray-300 dark:border-gray-600',
          error ? 'border-red-500 bg-red-50 dark:bg-red-900/20' : '',
          'dark:bg-gray-700 dark:text-white'
        ]"
        :disabled="disabled"
        @input="handleInput(index, $event)"
        @keydown="handleKeyDown(index, $event)"
        @paste="handlePaste"
      />
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="text-center">
      <p class="text-sm text-red-500">{{ error }}</p>
    </div>

    <!-- 重发验证码 -->
    <div v-if="showResend" class="text-center">
      <div v-if="countdown > 0" class="text-sm text-gray-500 dark:text-gray-400">
        {{ countdown }} 秒后可重新发送
      </div>
      <el-button
        v-else
        type="text"
        :loading="resending"
        @click="handleResend"
        class="text-blue-600 hover:text-blue-500 dark:text-blue-400"
      >
        重新发送验证码
      </el-button>
    </div>

    <!-- 提示信息 -->
    <div v-if="hint" class="text-center mt-2">
      <p class="text-xs text-gray-500 dark:text-gray-400">{{ hint }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'

interface Props {
  modelValue?: string
  length?: number
  disabled?: boolean
  error?: string
  showResend?: boolean
  resendCountdown?: number
  resending?: boolean
  hint?: string
  autoFocus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  length: 6,
  disabled: false,
  error: '',
  showResend: true,
  resendCountdown: 60,
  resending: false,
  hint: '',
  autoFocus: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'complete': [value: string]
  'resend': []
}>()

// 数字输入数组
const digits = ref<string[]>(Array(props.length).fill(''))
const inputRefs = ref<(HTMLInputElement | null)[]>([])
const countdown = ref(0)
const countdownTimer = ref<NodeJS.Timeout>()

// 设置输入框引用
const setInputRef = (el: HTMLInputElement | null, index: number) => {
  inputRefs.value[index] = el
}

// 处理输入
const handleInput = (index: number, event: Event) => {
  const target = event.target as HTMLInputElement
  const value = target.value.replace(/[^0-9]/g, '') // 只允许数字
  
  if (value.length > 1) {
    // 处理粘贴多个字符的情况
    const chars = value.split('').slice(0, props.length - index)
    chars.forEach((char, i) => {
      if (index + i < props.length) {
        digits.value[index + i] = char
      }
    })
    
    // 移动到下一个空的输入框或最后一个输入框
    const nextIndex = Math.min(index + chars.length, props.length - 1)
    focusInput(nextIndex)
  } else {
    digits.value[index] = value
    
    // 自动移动到下一个输入框
    if (value && index < props.length - 1) {
      focusInput(index + 1)
    }
  }
  
  updateModelValue()
}

// 处理键盘事件
const handleKeyDown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Backspace') {
    if (!digits.value[index] && index > 0) {
      // 如果当前输入框为空，删除前一个输入框的内容
      digits.value[index - 1] = ''
      focusInput(index - 1)
    } else {
      digits.value[index] = ''
    }
    updateModelValue()
  } else if (event.key === 'ArrowLeft' && index > 0) {
    focusInput(index - 1)
  } else if (event.key === 'ArrowRight' && index < props.length - 1) {
    focusInput(index + 1)
  } else if (event.key === 'Enter') {
    const code = digits.value.join('')
    if (code.length === props.length) {
      emit('complete', code)
    }
  }
}

// 处理粘贴
const handlePaste = (event: ClipboardEvent) => {
  event.preventDefault()
  const paste = event.clipboardData?.getData('text') || ''
  const numbers = paste.replace(/[^0-9]/g, '').slice(0, props.length)
  
  if (numbers) {
    const chars = numbers.split('')
    chars.forEach((char, index) => {
      if (index < props.length) {
        digits.value[index] = char
      }
    })
    
    // 聚焦到最后一个填充的输入框
    const lastIndex = Math.min(chars.length - 1, props.length - 1)
    focusInput(lastIndex)
    
    updateModelValue()
  }
}

// 聚焦输入框
const focusInput = (index: number) => {
  nextTick(() => {
    const input = inputRefs.value[index]
    if (input) {
      input.focus()
      input.select()
    }
  })
}

// 更新模型值
const updateModelValue = () => {
  const value = digits.value.join('')
  emit('update:modelValue', value)
  
  // 如果验证码输入完成
  if (value.length === props.length) {
    emit('complete', value)
  }
}

// 处理重发验证码
const handleResend = () => {
  emit('resend')
  startCountdown()
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = props.resendCountdown
  countdownTimer.value = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer.value)
    }
  }, 1000)
}

// 清空验证码
const clear = () => {
  digits.value = Array(props.length).fill('')
  updateModelValue()
  if (props.autoFocus) {
    focusInput(0)
  }
}

// 设置验证码
const setValue = (value: string) => {
  const chars = value.slice(0, props.length).split('')
  chars.forEach((char, index) => {
    digits.value[index] = char
  })
  // 填充剩余位置
  for (let i = chars.length; i < props.length; i++) {
    digits.value[i] = ''
  }
  updateModelValue()
}

// 监听外部值变化
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue !== digits.value.join('')) {
      setValue(newValue)
    }
  },
  { immediate: true }
)

// 监听错误状态
watch(
  () => props.error,
  (hasError) => {
    if (!hasError) {
      // 错误清除时，聚焦到第一个空输入框
      const emptyIndex = digits.value.findIndex(d => !d)
      if (emptyIndex !== -1) {
        focusInput(emptyIndex)
      }
    }
  }
)

// 组件挂载时自动聚焦
onMounted(() => {
  if (props.autoFocus) {
    focusInput(0)
  }
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
})

// 暴露方法
defineExpose({
  clear,
  setValue,
  focus: () => focusInput(0)
})
</script>

<style scoped>
.verification-code-input input {
  -webkit-appearance: none;
  -moz-appearance: textfield;
}

.verification-code-input input::-webkit-outer-spin-button,
.verification-code-input input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* 移动端适配 */
@media (max-width: 640px) {
  .verification-code-input input {
    @apply w-10 h-10 text-base;
  }
}</style>