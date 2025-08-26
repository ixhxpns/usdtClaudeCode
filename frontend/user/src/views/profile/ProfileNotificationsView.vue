<template>
  <div class="notifications-view">
    <el-card>
      <template #header>
        <div class="card-header flex justify-between items-center">
          <h3 class="text-lg font-medium">通知设置</h3>
        </div>
      </template>

      <div class="space-y-6">
        <!-- 邮件通知设置 -->
        <div class="setting-group">
          <h4 class="text-base font-medium mb-4">邮件通知</h4>
          <div class="space-y-3">
            <div class="flex justify-between items-center">
              <div>
                <label class="font-medium">登录提醒</label>
                <p class="text-sm text-gray-500">账户登录时发送邮件通知</p>
              </div>
              <el-switch v-model="emailSettings.loginAlert" />
            </div>
            <div class="flex justify-between items-center">
              <div>
                <label class="font-medium">交易确认</label>
                <p class="text-sm text-gray-500">交易完成后发送邮件确认</p>
              </div>
              <el-switch v-model="emailSettings.tradeConfirm" />
            </div>
            <div class="flex justify-between items-center">
              <div>
                <label class="font-medium">充值提醒</label>
                <p class="text-sm text-gray-500">充值到账后发送邮件通知</p>
              </div>
              <el-switch v-model="emailSettings.depositAlert" />
            </div>
          </div>
        </div>

        <!-- 系统通知设置 -->
        <div class="setting-group">
          <h4 class="text-base font-medium mb-4">系统通知</h4>
          <div class="space-y-3">
            <div class="flex justify-between items-center">
              <div>
                <label class="font-medium">系统公告</label>
                <p class="text-sm text-gray-500">接收系统维护和更新公告</p>
              </div>
              <el-switch v-model="systemSettings.announcements" />
            </div>
            <div class="flex justify-between items-center">
              <div>
                <label class="font-medium">安全提醒</label>
                <p class="text-sm text-gray-500">安全相关的重要提醒</p>
              </div>
              <el-switch v-model="systemSettings.securityAlerts" />
            </div>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="pt-4 border-t">
          <el-button type="primary" @click="saveSettings" :loading="saving">
            保存设置
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const saving = ref(false)

const emailSettings = reactive({
  loginAlert: true,
  tradeConfirm: true,
  depositAlert: true
})

const systemSettings = reactive({
  announcements: true,
  securityAlerts: true
})

const saveSettings = async () => {
  try {
    saving.value = true
    // TODO: 调用API保存设置
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    ElMessage.success('设置保存成功')
  } catch (error) {
    console.error('保存设置失败:', error)
    ElMessage.error('保存设置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  // TODO: 加载用户通知设置
})
</script>

<style scoped>
.notifications-view {
  max-width: 800px;
}

.setting-group {
  padding: 1rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-group:last-child {
  border-bottom: none;
}
</style>