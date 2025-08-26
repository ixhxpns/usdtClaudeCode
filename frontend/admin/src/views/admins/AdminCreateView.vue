<template>
  <div class="admin-create-view">
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">创建管理员</h1>
      <el-button @click="$router.go(-1)">返回</el-button>
    </div>
    <div class="mt-6">
      <el-card>
        <template #header>
          <span>新建管理员</span>
        </template>
        <div class="text-center text-gray-500 py-8">
          创建管理员功能开发中...
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
// 创建管理员视图
</script>

<style scoped>
.admin-create-view {
  padding: 24px;
}
</style>