<template>
  <AuthLayout
    form-title="忘记密码"
    form-subtitle="请输入您的邮箱地址，我们将发送重置密码的链接"
    :loading="loading"
    loading-text="发送中，请稍候..."
  >
    <!-- 导航链接 -->
    <template #navigation>
      <p class="text-sm text-gray-600 dark:text-gray-400">
        记起密码了？
        <router-link 
          to="/login" 
          class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 transition-colors"
        >
          返回登录
        </router-link>
      </p>
    </template>

    <!-- 忘记密码表单 -->
    <div v-if="!emailSent" class="space-y-6">
        <el-form 
          ref="formRef"
          :model="form" 
          :rules="rules"
          @submit.prevent="handleSubmit"
        >
          <el-form-item prop="email">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              邮箱地址
            </label>
            <el-input
              v-model="form.email"
              type="email"
              size="large"
              placeholder="请输入注册时的邮箱地址"
              :prefix-icon="Message"
              :disabled="loading"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="w-full"
              :loading="loading"
              @click="handleSubmit"
            >
              发送重置链接
            </el-button>
          </el-form-item>
        </el-form>

        <div class="text-center">
          <router-link 
            to="/login"
            class="text-sm font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400"
          >
            返回登录
          </router-link>
        </div>
    </div>

    <!-- 邮件发送成功状态 -->
    <div v-else class="text-center space-y-6">
      <div class="w-16 h-16 mx-auto bg-green-100 rounded-full flex items-center justify-center">
        <el-icon class="text-2xl text-green-600">
          <Message />
        </el-icon>
      </div>
      <div>
        <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">
          邮件已发送
        </h3>
        <p class="text-sm text-gray-600 dark:text-gray-400">
          我们已向 <span class="font-medium">{{ form.email }}</span> 发送了重置密码的链接，请查收邮件并按照提示操作。
        </p>
      </div>
      <div class="space-y-3">
        <el-button 
          type="primary" 
          size="large" 
          class="w-full"
          @click="emailSent = false"
        >
          重新发送
        </el-button>
        <router-link 
          to="/login"
          class="block text-sm font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400"
        >
          返回登录
        </router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElForm, ElMessage, ElIcon } from 'element-plus'
import { Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { Validator } from '@/utils/common'

const authStore = useAuthStore()
const formRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)
const emailSent = ref(false)

const form = reactive({
  email: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { validator: (rule: any, value: string, callback: Function) => {
      if (value && !Validator.isEmail(value)) {
        callback(new Error('请输入有效的邮箱地址'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true
    await authStore.resetPassword(form.email)
    emailSent.value = true
    ElMessage.success('重置密码邮件发送成功')
  } catch (error) {
    console.error('重置密码失败:', error)
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>