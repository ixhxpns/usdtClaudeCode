<template>
  <div class="order-detail-view">
    <div class="page-header">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">订单详情</h1>
      <el-button @click="$router.go(-1)">返回</el-button>
    </div>
    <div class="mt-6">
      <el-card>
        <template #header>
          <span>订单信息</span>
        </template>
        <div class="text-center text-gray-500 py-8">
          订单详情功能开发中...
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
// 订单详情视图
</script>

<style scoped>
.order-detail-view {
  padding: 24px;
}
</style>