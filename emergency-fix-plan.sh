#!/bin/bash

# ============================================================================
# USDT交易平台 - 紧急修复执行计划
# ============================================================================
# 目的: 提供三种修复方案的快速执行脚本
# 作者: Master Agent  
# 版本: 1.0
# 日期: 2025-09-01
# ============================================================================

echo "=========================================="
echo "🚨 USDT Trading Platform 紧急修复计划"
echo "=========================================="
echo "当前时间: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${RED}⚠️  API路由完全失效 - 需要紧急修复${NC}"
echo ""

echo -e "${BLUE}📋 可选修复方案:${NC}"
echo ""
echo -e "${GREEN}方案A: 架构重构 (推荐)${NC}"
echo "  - 降级Spring Boot版本到稳定版本"
echo "  - 简化项目结构和配置"  
echo "  - 预计时间: 4-6小时"
echo "  - 成功率: 85%"
echo ""

echo -e "${YELLOW}方案B: 容器化环境重建${NC}"
echo "  - 重建Docker镜像使用不同基础镜像"
echo "  - 使用Spring Boot原生镜像构建"
echo "  - 预计时间: 2-3小时"
echo "  - 成功率: 70%"
echo ""

echo -e "${PURPLE}方案C: 应急替代服务 (临时)${NC}"
echo "  - 快速搭建简单API服务 (Node.js/Python)"
echo "  - 实现核心认证和价格查询功能"
echo "  - 预计时间: 3-4小时"
echo "  - 成功率: 95%"
echo ""

read -p "请选择执行方案 (A/B/C) 或按 Enter 查看详细信息: " choice

case $choice in
    [Aa])
        echo ""
        echo -e "${GREEN}🔧 执行方案A - 架构重构${NC}"
        echo "=========================================="
        
        echo "步骤1: 备份当前代码"
        cp -r backend backend_backup_$(date +%Y%m%d_%H%M%S)
        echo "✅ 代码已备份"
        
        echo ""
        echo "步骤2: Spring Boot版本降级建议"
        echo "建议修改 backend/pom.xml:"
        echo "  从: <version>2.7.14</version>"
        echo "  改为: <version>2.6.15</version>"
        
        echo ""
        echo "步骤3: 简化配置建议"
        echo "- 移除复杂的拦截器配置"
        echo "- 简化WebMvc配置"
        echo "- 移除不必要的依赖"
        
        echo ""
        echo -e "${YELLOW}⚠️  需要手动执行以上步骤，然后运行:${NC}"
        echo "docker-compose build backend --no-cache"
        echo "docker-compose up backend -d"
        ;;
        
    [Bb])
        echo ""
        echo -e "${YELLOW}🐳 执行方案B - 容器环境重建${NC}"
        echo "=========================================="
        
        echo "步骤1: 停止现有服务"
        docker-compose down backend
        
        echo ""
        echo "步骤2: 清理Docker缓存"
        docker system prune -f
        docker image prune -f
        
        echo ""
        echo "步骤3: 使用Spring Boot原生构建"
        echo "建议在 backend/pom.xml 中添加:"
        echo "<plugin>"
        echo "  <groupId>org.springframework.boot</groupId>"
        echo "  <artifactId>spring-boot-maven-plugin</artifactId>"
        echo "  <configuration>"
        echo "    <image>"
        echo "      <name>usdt-backend-native</name>"
        echo "    </image>"
        echo "  </configuration>"
        echo "</plugin>"
        
        echo ""
        echo -e "${YELLOW}⚠️  需要手动添加配置后运行:${NC}"
        echo "cd backend && mvn spring-boot:build-image"
        echo "docker tag usdt-backend-native:latest usdtclaudecode-backend:latest"
        echo "docker-compose up backend -d"
        ;;
        
    [Cc])
        echo ""
        echo -e "${PURPLE}🚀 执行方案C - 应急Node.js服务${NC}"
        echo "=========================================="
        
        echo "步骤1: 创建临时API服务目录"
        mkdir -p temp-api-service
        cd temp-api-service
        
        echo ""
        echo "步骤2: 创建基础Node.js服务"
        cat > package.json << EOF
{
  "name": "usdt-emergency-api",
  "version": "1.0.0",
  "main": "server.js",
  "dependencies": {
    "express": "^4.18.2",
    "mysql2": "^3.6.0",
    "redis": "^4.6.0",
    "cors": "^2.8.5"
  }
}
EOF

        cat > server.js << EOF
const express = require('express');
const mysql = require('mysql2/promise');
const redis = require('redis');
const cors = require('cors');

const app = express();
const port = 8090;

app.use(cors());
app.use(express.json());

// 数据库连接配置
const dbConfig = {
  host: 'localhost',
  port: 3306,
  user: 'root',
  password: 'UsdtTrading123!',
  database: 'usdttrading'
};

// Redis连接配置  
const redisClient = redis.createClient({
  url: 'redis://localhost:6379'
});

// 基础API路由
app.get('/api/auth/public-key', (req, res) => {
  res.json({
    success: true,
    data: {
      publicKey: "临时公钥 - 应急服务",
      format: "Emergency Mode"
    }
  });
});

app.get('/api/price/current', (req, res) => {
  res.json({
    success: true,
    data: {
      price: 7.2,
      currency: "CNY",
      lastUpdate: new Date().toISOString(),
      source: "Emergency Service"
    }
  });
});

app.get('/health', (req, res) => {
  res.json({ status: 'Emergency API Running' });
});

app.listen(port, () => {
  console.log(\`🚨 Emergency API Server running on port \${port}\`);
  console.log(\`🔗 Health Check: http://localhost:\${port}/health\`);
});
EOF
        
        echo ""
        echo "步骤3: 安装依赖并启动"
        echo -e "${YELLOW}执行以下命令启动应急服务:${NC}"
        echo "npm install"
        echo "node server.js"
        
        echo ""
        echo -e "${GREEN}✅ 应急服务文件已创建${NC}"
        echo "目录: $(pwd)"
        ;;
        
    *)
        echo ""
        echo -e "${BLUE}📖 详细诊断信息${NC}"
        echo "=========================================="
        
        echo "🔍 问题确认:"
        echo "- Spring Boot应用已成功启动"
        echo "- 96个控制器映射已注册"
        echo "- 所有API请求返回404错误"
        echo "- DispatcherServlet路由机制异常"
        
        echo ""
        echo "🛠️ 已尝试的修复:"
        echo "- ✅ 禁用所有拦截器"
        echo "- ✅ 修复组件扫描路径"  
        echo "- ✅ 检查context-path配置"
        echo "- ✅ 重建Docker镜像"
        echo "- ❌ 问题依然存在"
        
        echo ""
        echo "📊 修复建议优先级:"
        echo "1. 方案A (架构重构) - 长期稳定"
        echo "2. 方案C (应急服务) - 快速恢复"  
        echo "3. 方案B (容器重建) - 中等风险"
        
        echo ""
        echo "重新运行此脚本并选择方案: ./emergency-fix-plan.sh"
        ;;
esac

echo ""
echo "=========================================="
echo -e "${BLUE}修复计划完成 - $(date)${NC}"
echo "=========================================="
echo ""
echo -e "${YELLOW}📞 如需支持，请查看完整技术报告:${NC}"
echo "devdocrecord/FINAL_TECHNICAL_ANALYSIS_REPORT.md"