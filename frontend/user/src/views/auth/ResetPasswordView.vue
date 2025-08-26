<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center">
        <div class="mx-auto h-12 w-12 bg-blue-600 rounded-lg flex items-center justify-center">
          <el-icon :size="24" class="text-white">
            <Key />
          </el-icon>
        </div>
        <h2 class="mt-6 text-3xl font-extrabold text-gray-900 dark:text-white">
          重置密码
        </h2>
        <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
          请输入您的新密码
        </p>
      </div>

      <el-card class="shadow-xl border-0">
        <div class="p-8">
          <p class="text-center text-sm text-gray-500 mb-8">
            密码重置功能开发中...
          </p>
          <router-link 
            to="/login"
            class="block text-center text-blue-600 hover:text-blue-500"
          >
            返回登录页面
          </router-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Key } from '@element-plus/icons-vue'
</script>