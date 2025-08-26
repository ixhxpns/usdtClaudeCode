<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center">
        <div class="mx-auto h-24 w-24 bg-gray-200 dark:bg-gray-700 rounded-lg flex items-center justify-center">
          <el-icon :size="48" class="text-gray-400">
            <Warning />
          </el-icon>
        </div>
        <h1 class="mt-6 text-6xl font-bold text-gray-900 dark:text-white">404</h1>
        <h2 class="mt-2 text-xl font-semibold text-gray-600 dark:text-gray-400">
          页面不存在
        </h2>
        <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">
          抱歉，您访问的页面不存在或已被移除
        </p>
      </div>

      <div class="flex justify-center space-x-4">
        <el-button @click="$router.back()">
          返回上页
        </el-button>
        <el-button type="primary" @click="$router.push('/dashboard')">
          回到首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Warning } from '@element-plus/icons-vue'
</script>