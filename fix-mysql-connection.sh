#!/bin/bash

echo "🔧 Master Agent - 修复MySQL连接问题"
echo "=================================="

# 1. 停止所有相关服务
echo "1. 停止相关服务..."
pkill -f "RSATestServer" 2>/dev/null || true
docker stop usdt-backend 2>/dev/null || true

# 2. 检查网络连接
echo "2. 检查网络连接..."
if docker exec usdt-mysql mysql -u root -pUsdtTrading123! -e "SELECT 1;" >/dev/null 2>&1; then
    echo "✅ MySQL容器内连接正常"
else
    echo "❌ MySQL容器内连接失败"
    exit 1
fi

# 3. 从Docker网络内测试连接
echo "3. 测试Docker网络连接..."
docker run --rm --network usdtclaudecode_usdt-network mysql:8.0.35 mysql -h mysql -u root -pUsdtTrading123! -e "SELECT 'Network connection OK';" 2>/dev/null || echo "❌ Docker网络连接失败"

# 4. 创建简化的应用配置
echo "4. 创建简化应用配置..."
cat > /Users/jason/Projects/usdtClaudeCode/backend/src/main/resources/application-simple.yml << 'EOF'
server:
  port: 8090
  servlet:
    context-path: /api

spring:
  application:
    name: usdt-trading-platform
  
  # 简化的数据库配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/usdttrading?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=60000&socketTimeout=60000&autoReconnect=true&useLocalSessionState=true&rewriteBatchedStatements=true
    username: root
    password: UsdtTrading123!
    
    # Druid连接池配置
    druid:
      initial-size: 1
      min-idle: 1
      max-active: 10
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      keep-alive-between-time-millis: 60000
      validation-query: SELECT 1
      validation-query-timeout: 3
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: false
      
  # 简化的Redis配置
  redis:
    host: redis
    port: 6379
    timeout: 6000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0

  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false

# RSA密钥配置
business:
  security:
    rsa:
      public-key: ${BUSINESS_SECURITY_RSA_PUBLIC_KEY}
      private-key: ${BUSINESS_SECURITY_RSA_PRIVATE_KEY}

# 日志配置
logging:
  level:
    com.usdttrading: INFO
    org.springframework: WARN
    org.hibernate: WARN
    com.mysql: WARN
    com.alibaba.druid: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
EOF

# 5. 更新Dockerfile添加等待逻辑
echo "5. 更新Dockerfile..."
cat > /Users/jason/Projects/usdtClaudeCode/backend/Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre

# 安装必要工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    tzdata \
    bash \
    mysql-client \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 复制JAR文件
COPY target/usdt-trading-platform-1.0.0.jar app.jar

# 复制等待脚本
COPY wait-for-services.sh /app/wait-for-services.sh

# 设置权限
RUN mkdir -p logs uploads temp \
    && chown -R appuser:appuser /app \
    && chmod -R 755 /app \
    && chmod +x /app/wait-for-services.sh

# 切换到应用用户
USER appuser

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.awt.headless=true -Dspring.profiles.active=simple"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/api/admin/auth/public-key || exit 1

# 启动命令
CMD ["/app/wait-for-services.sh"]
EOF

# 6. 创建等待服务脚本
echo "6. 创建等待服务脚本..."
cat > /Users/jason/Projects/usdtClaudeCode/backend/wait-for-services.sh << 'EOF'
#!/bin/bash

echo "🚀 等待依赖服务启动..."

# 等待MySQL
echo "⏳ 等待MySQL..."
for i in {1..60}; do
    if mysql -h mysql -u root -pUsdtTrading123! -e "SELECT 1;" >/dev/null 2>&1; then
        echo "✅ MySQL已就绪"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "❌ MySQL启动超时"
        exit 1
    fi
    echo "等待MySQL... ($i/60)"
    sleep 2
done

# 等待Redis
echo "⏳ 等待Redis..."
for i in {1..30}; do
    if echo "PING" | nc redis 6379 | grep -q PONG; then
        echo "✅ Redis已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "⚠️ Redis连接失败，但继续启动"
        break
    fi
    echo "等待Redis... ($i/30)"
    sleep 1
done

echo "🎯 启动应用..."
exec java $JAVA_OPTS -jar /app/app.jar
EOF

chmod +x /Users/jason/Projects/usdtClaudeCode/backend/wait-for-services.sh

# 7. 重新构建应用
echo "7. 重新构建应用..."
cd /Users/jason/Projects/usdtClaudeCode/backend
mvn clean package -DskipTests -q

if [ ! -f target/usdt-trading-platform-1.0.0.jar ]; then
    echo "❌ JAR文件构建失败"
    exit 1
fi

echo "✅ JAR文件构建成功"

# 8. 重新构建Docker镜像
echo "8. 重新构建Docker镜像..."
docker build -t usdt-backend:latest .

# 9. 使用环境变量启动后端
echo "9. 启动后端服务..."
cd /Users/jason/Projects/usdtClaudeCode

# 启动后端容器，并传入RSA密钥
docker run -d \
    --name usdt-backend-fixed \
    --network usdtclaudecode_usdt-network \
    -p 8090:8090 \
    -e BUSINESS_SECURITY_RSA_PUBLIC_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB" \
    -e BUSINESS_SECURITY_RSA_PRIVATE_KEY="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDb+pQffI2s5svqyTvgs3VGcpZH4k4/uh+u5pj+bwnodJTzKb3BWnTWQJfRXLL9fGA55clL31FQJofpTyvQUIzGeEH6HZ+vpk7ulyfm7j+g2ZjejTNRwPf9BJYXUMTNfbDlNCAT8uMucgsEQlvvPCA+Mbq0CMuhaENJaWi1jRF9jDdeYdnEf51CLw2n15eA4tqTjclnhkwyYSYKXtXy4hsi0SFZMP+EQYvKHZKTCza5O1fhi9NtCTwI+BkDTrbz695i6JSW8ck6Zgt8QIfmOm4MvPBqJJfrzEXZAvHpmSlLNXfVRMT5w5mMlYl4dhPDeItNfZeRBRpDHBm/mn6UsYELAgMBAAECggEAJgzJ4khhGS8yxoHYYZ2pA25+oCB8+HjxUgeBH0GFrTe9K/JkaDWFUCoMGKK4MzB4VItyrjUKk5qWl7yiD35fW1Uh1GmbXX6e2JY8yK0dRG1fC9/QqKuAObu5Wn8WC5VaK8tAYTL6KOaBEVOJI3B7A0cGzG1ZC6pFOUWKXxzvkY+FRWKUBsQFSU6IkuzEN70dJHfgXlRz5+bOtY8zTzFMfPcb4Oz5nOpcm+ifD9+ki1MFyyLiS57bbHeSyZ59TrpSUbF/s6X4U6oON5l28WSlpcygmp5SgcMpH+7tPY4YnNDWRbJc/yX0P6ai0KkG9sPXDGjSwmvlwUXqfzmjUi2agQKBgQDzO7MOFf9F6rg3YZ08LS8oofWD/tIyXKuJkARvNEtmOF1AQFX9JwjfMHRvozfklZ9WNUqsX+rUXDQiJpHReJAKW/joFAQcSNVdVcwGo2Hz5Vtb2WC24KxvwkI2FIYE2p5leW1O1xDUAmvNtEMg2lIsPC1rqinX5wcjKWzOZSCnEwKBgQDnhmmR1snHmA4wwPVc2eedrHfSum9l+fYjsjmugQ8gROI+3FBiErLEquLhtMcd3EHUjHFEWjxqq8tjafzgH/OP04qmy21QlnT/DM7emXujSjKk15wEc5m4B2Uzgswu2j7Wl6B6njSXhk3PeX+PAjLi8j3xeWehc9i3mYSHoYslKQKBgC6m8q7u9SlZ3b9xj5DtMbBfcBHDHFnggF5AKzmRFC0k/m3GaTfG9uKoDo0jByNmt0r3qbzqIMZecPlj4HAG6cmy3kjVHfy204W1YQ6c47q98Qnq5avt5+T/o2dwBEyGCf94jMikY2vmkvq/amiwtzYYzLVry97HRw0tctsbdmnVAoGBANvhkqd4BsBxT6DL0Pry0/6yCkZu21dNEo+KNy/c0CHEad5rXEgYHAGdjcXv618XjMmw6+2PiWuBZrMuuIOetLE2paqM7m+nxMtpPZq4x2woDnrxbfHoW+gj1eAa65HfoegprrZlQ+tYGNPfPt7xpqRUjbGkrF/wZrTPrsclC8IpAoGAH8C7choCmSICtyZaept5hhmLSnKmolQ2ppF8LwBzM9i2ZpsiuvAjutHphf2oUbyGejLBMpJWVYjH7FbEAzARmB15JXjkXnBqDsrCzt5jG+6S8UiueGsdjt1Jl39+9Fm15b9ERkrxD+aZMnvynRTKiTJlxHh+Q2LloP2dcFwAy+o=" \
    usdt-backend:latest

echo "10. 等待应用启动..."
sleep 30

# 11. 测试API端点
echo "11. 测试API端点..."
for i in {1..20}; do
    if curl -s -f http://localhost:8090/api/admin/auth/public-key >/dev/null 2>&1; then
        echo "✅ 后端API已启动并正常工作"
        break
    fi
    if [ $i -eq 20 ]; then
        echo "❌ API启动超时"
        echo "查看日志:"
        docker logs usdt-backend-fixed --tail=20
        exit 1
    fi
    echo "⏳ 等待API启动... ($i/20)"
    sleep 3
done

echo ""
echo "🎉 MySQL连接问题修复完成！"
echo "=================================="
echo "✅ 后端服务: http://localhost:8090"
echo "✅ RSA公钥: http://localhost:8090/api/admin/auth/public-key"
echo ""
echo "测试命令:"
echo "curl -s http://localhost:8090/api/admin/auth/public-key | jq ."