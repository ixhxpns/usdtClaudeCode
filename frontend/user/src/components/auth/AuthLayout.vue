<template>
  <div class="auth-layout min-h-screen flex">
    <!-- 左侧装饰面板 - 桌面端显示 -->
    <div class="hidden lg:flex lg:flex-1 lg:flex-col lg:justify-center lg:px-8 xl:px-12 bg-gradient-to-br from-blue-600 via-blue-700 to-purple-700 relative overflow-hidden">
      <!-- 背景装饰 -->
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-0 left-0 w-full h-full">
          <div class="floating-shapes">
            <div class="shape shape-1"></div>
            <div class="shape shape-2"></div>
            <div class="shape shape-3"></div>
            <div class="shape shape-4"></div>
          </div>
        </div>
      </div>

      <!-- 内容 -->
      <div class="relative z-10 text-white">
        <div class="max-w-md">
          <!-- Logo和标题 -->
          <div class="mb-8">
            <div class="flex items-center space-x-3 mb-6">
              <div class="w-12 h-12 bg-white/20 backdrop-blur-sm rounded-xl flex items-center justify-center">
                <el-icon :size="24" class="text-white">
                  <TrendCharts />
                </el-icon>
              </div>
              <h1 class="text-2xl font-bold">{{ appName }}</h1>
            </div>
            <h2 class="text-3xl font-bold leading-tight mb-4">
              {{ title || '安全可靠的' }}<br>
              {{ subtitle || '数字资产交易平台' }}
            </h2>
            <p class="text-blue-100 text-lg leading-relaxed">
              {{ description || '专业的USDT交易服务，为您提供安全、快捷、透明的数字资产交易体验。' }}
            </p>
          </div>

          <!-- 特性列表 -->
          <div class="space-y-4">
            <div 
              v-for="feature in features" 
              :key="feature.title"
              class="flex items-center space-x-3"
            >
              <div class="w-8 h-8 bg-white/20 backdrop-blur-sm rounded-lg flex items-center justify-center flex-shrink-0">
                <el-icon :size="16" class="text-white">
                  <component :is="feature.icon" />
                </el-icon>
              </div>
              <div>
                <h3 class="font-semibold">{{ feature.title }}</h3>
                <p class="text-sm text-blue-100 opacity-90">{{ feature.description }}</p>
              </div>
            </div>
          </div>

          <!-- 信任标识 -->
          <div class="mt-12">
            <div class="flex items-center space-x-6 opacity-60">
              <div class="text-xs">
                <div class="font-semibold mb-1">安全保障</div>
                <div class="text-blue-200">银行级加密</div>
              </div>
              <div class="text-xs">
                <div class="font-semibold mb-1">合规运营</div>
                <div class="text-blue-200">监管认可</div>
              </div>
              <div class="text-xs">
                <div class="font-semibold mb-1">7×24服务</div>
                <div class="text-blue-200">实时支持</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧表单区域 -->
    <div class="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-8 xl:px-12 bg-white dark:bg-gray-900">
      <div class="mx-auto w-full max-w-md">
        <!-- 移动端Logo -->
        <div class="lg:hidden text-center mb-8">
          <div class="inline-flex items-center space-x-2">
            <div class="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
              <el-icon :size="20" class="text-white">
                <TrendCharts />
              </el-icon>
            </div>
            <span class="text-xl font-bold text-gray-900 dark:text-white">{{ appName }}</span>
          </div>
        </div>

        <!-- 表单标题 -->
        <div class="text-center mb-8">
          <h2 class="text-3xl font-bold text-gray-900 dark:text-white">
            {{ formTitle }}
          </h2>
          <p v-if="formSubtitle" class="mt-2 text-sm text-gray-600 dark:text-gray-400">
            {{ formSubtitle }}
          </p>
          
          <!-- 导航链接 -->
          <div v-if="$slots.navigation" class="mt-4">
            <slot name="navigation" />
          </div>
        </div>

        <!-- 表单内容 -->
        <div class="form-container">
          <slot />
        </div>

        <!-- 底部信息 -->
        <div v-if="$slots.footer || showDefaultFooter" class="mt-8">
          <slot name="footer">
            <div v-if="showDefaultFooter" class="text-center">
              <div class="text-xs text-gray-500 dark:text-gray-400 space-y-1">
                <p>创建账户即表示您同意我们的服务条款</p>
                <p>© 2024 {{ appName }}. 保留所有权利.</p>
              </div>
            </div>
          </slot>
        </div>
      </div>
    </div>

    <!-- 加载遮罩 -->
    <div 
      v-if="loading" 
      class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg p-6 flex items-center space-x-3">
        <el-icon class="animate-spin text-blue-600" :size="20">
          <Loading />
        </el-icon>
        <span class="text-gray-900 dark:text-white">{{ loadingText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { 
  TrendCharts, 
  SuccessFilled, 
  Lock, 
  Clock, 
  Service,
  Loading
} from '@element-plus/icons-vue'

interface Props {
  title?: string
  subtitle?: string
  description?: string
  formTitle: string
  formSubtitle?: string
  appName?: string
  loading?: boolean
  loadingText?: string
  showDefaultFooter?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '安全可靠的',
  subtitle: '数字资产交易平台',
  description: '专业的USDT交易服务，为您提供安全、快捷、透明的数字资产交易体验。',
  appName: 'USDT Trading',
  loadingText: '处理中...',
  showDefaultFooter: true
})

// 平台特性
const features = computed(() => [
  {
    icon: SuccessFilled,
    title: '安全保障',
    description: '多重安全防护，资金安全有保障'
  },
  {
    icon: Lock,
    title: '隐私保护',
    description: '银行级加密，保护您的隐私数据'
  },
  {
    icon: Clock,
    title: '实时交易',
    description: '7×24小时不间断交易服务'
  },
  {
    icon: Service,
    title: '专业服务',
    description: '专业团队提供全天候客户支持'
  }
])
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  min-height: 100dvh; /* 支持动态视口高度 */
}

.form-container {
  @apply bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 p-8;
}

/* 浮动动画装饰 */
.floating-shapes {
  position: relative;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 80px;
  height: 80px;
  top: 20%;
  right: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 120px;
  height: 120px;
  top: 60%;
  right: 20%;
  animation-delay: 2s;
}

.shape-3 {
  width: 60px;
  height: 60px;
  bottom: 30%;
  left: 15%;
  animation-delay: 4s;
}

.shape-4 {
  width: 100px;
  height: 100px;
  top: 40%;
  left: 10%;
  animation-delay: 1s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
    opacity: 0.7;
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
    opacity: 1;
  }
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .form-container {
    @apply p-6 shadow-lg;
  }
}

@media (max-width: 640px) {
  .form-container {
    @apply p-4 rounded-xl shadow-md;
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .form-container {
    @apply bg-gray-800 border-gray-700;
  }
}
</style>