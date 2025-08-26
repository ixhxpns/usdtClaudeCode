<template>
  <div class="help-view">
    <el-card>
      <template #header>
        <h3 class="text-lg font-medium">帮助中心</h3>
      </template>

      <div class="help-content">
        <el-row :gutter="20">
          <!-- 常见问题 -->
          <el-col :span="12">
            <h4 class="mb-4 text-base font-medium">常见问题</h4>
            <el-collapse v-model="activeNames">
              <el-collapse-item title="如何充值USDT?" name="1">
                <p>您可以通过以下步骤充值USDT：</p>
                <ol class="list-decimal list-inside space-y-1">
                  <li>进入钱包页面</li>
                  <li>选择充值功能</li>
                  <li>选择USDT币种</li>
                  <li>获取充值地址</li>
                  <li>从其他钱包转账到此地址</li>
                </ol>
              </el-collapse-item>
              
              <el-collapse-item title="如何进行交易?" name="2">
                <p>交易步骤如下：</p>
                <ol class="list-decimal list-inside space-y-1">
                  <li>进入交易页面</li>
                  <li>选择交易对</li>
                  <li>输入交易数量和价格</li>
                  <li>确认交易信息</li>
                  <li>提交订单</li>
                </ol>
              </el-collapse-item>
              
              <el-collapse-item title="手续费如何计算?" name="3">
                <p>交易手续费计算规则：</p>
                <ul class="list-disc list-inside space-y-1">
                  <li>普通用户：0.1%</li>
                  <li>VIP用户：根据等级享受折扣</li>
                  <li>手续费从成交金额中扣除</li>
                </ul>
              </el-collapse-item>
              
              <el-collapse-item title="提现多久到账?" name="4">
                <p>提现到账时间：</p>
                <ul class="list-disc list-inside space-y-1">
                  <li>USDT提现：1-6个区块确认</li>
                  <li>其他币种：根据不同网络而定</li>
                  <li>工作日处理更快</li>
                </ul>
              </el-collapse-item>
            </el-collapse>
          </el-col>

          <!-- 联系我们 -->
          <el-col :span="12">
            <h4 class="mb-4 text-base font-medium">联系我们</h4>
            <div class="contact-info space-y-4">
              <div class="contact-item">
                <h5 class="font-medium mb-2">在线客服</h5>
                <p class="text-gray-600">工作时间：9:00-18:00（工作日）</p>
                <el-button type="primary" @click="openCustomerService">
                  联系客服
                </el-button>
              </div>
              
              <div class="contact-item">
                <h5 class="font-medium mb-2">邮箱支持</h5>
                <p class="text-gray-600">support@usdttrading.com</p>
                <p class="text-sm text-gray-500">24小时内回复</p>
              </div>
              
              <div class="contact-item">
                <h5 class="font-medium mb-2">电话支持</h5>
                <p class="text-gray-600">400-123-4567</p>
                <p class="text-sm text-gray-500">工作时间：9:00-18:00</p>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 用户指南 -->
        <div class="user-guide mt-8">
          <h4 class="mb-4 text-base font-medium">用户指南</h4>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card class="guide-card" shadow="hover">
                <div class="text-center">
                  <el-icon class="text-3xl text-blue-500 mb-2">
                    <User />
                  </el-icon>
                  <h5 class="font-medium mb-2">账户注册</h5>
                  <p class="text-sm text-gray-600">了解如何注册和验证账户</p>
                  <el-button link type="primary" @click="openGuide('register')">
                    查看指南
                  </el-button>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="8">
              <el-card class="guide-card" shadow="hover">
                <div class="text-center">
                  <el-icon class="text-3xl text-green-500 mb-2">
                    <Wallet />
                  </el-icon>
                  <h5 class="font-medium mb-2">钱包管理</h5>
                  <p class="text-sm text-gray-600">学习如何管理您的数字资产</p>
                  <el-button link type="primary" @click="openGuide('wallet')">
                    查看指南
                  </el-button>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="8">
              <el-card class="guide-card" shadow="hover">
                <div class="text-center">
                  <el-icon class="text-3xl text-orange-500 mb-2">
                    <TrendCharts />
                  </el-icon>
                  <h5 class="font-medium mb-2">交易教程</h5>
                  <p class="text-sm text-gray-600">掌握基本的交易操作</p>
                  <el-button link type="primary" @click="openGuide('trading')">
                    查看指南
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Wallet, TrendCharts } from '@element-plus/icons-vue'

const activeNames = ref(['1'])

const openCustomerService = () => {
  ElMessage.info('客服功能正在开发中...')
}

const openGuide = (type: string) => {
  ElMessage.info(`${type}指南正在开发中...`)
}
</script>

<style scoped>
.help-view {
  max-width: 1000px;
}

.contact-item {
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.guide-card {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.guide-card .el-card__body {
  padding: 2rem;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>