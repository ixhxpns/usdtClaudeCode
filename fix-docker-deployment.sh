#!/bin/bash

echo "🚀 Master Agent - 修复Docker部署问题"
echo "======================================"

# 1. 停止所有服务
echo "1. 停止现有服务..."
docker-compose down -v 2>/dev/null || true
docker system prune -f 2>/dev/null || true

# 2. 创建必要的目录
echo "2. 创建数据和日志目录..."
mkdir -p {data,logs}/{mysql,redis,backend,admin,user,nginx}
mkdir -p data/{uploads,temp,nginx-cache}
chmod -R 755 data logs

# 3. 使用我们的RSA密钥配置环境变量
echo "3. 配置环境变量..."
cat > .env.docker << 'EOF'
# Docker部署环境变量
SPRING_PROFILES_ACTIVE=docker
TZ=Asia/Taipei

# RSA密钥配置 (使用之前生成的密钥)
BUSINESS_SECURITY_RSA_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB
BUSINESS_SECURITY_RSA_PRIVATE_KEY=MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDb+pQffI2s5svqyTvgs3VGcpZH4k4/uh+u5pj+bwnodJTzKb3BWnTWQJfRXLL9fGA55clL31FQJofpTyvQUIzGeEH6HZ+vpk7ulyfm7j+g2ZjejTNRwPf9BJYXUMTNfbDlNCAT8uMucgsEQlvvPCA+Mbq0CMuhaENJaWi1jRF9jDdeYdnEf51CLw2n15eA4tqTjclnhkwyYSYKXtXy4hsi0SFZMP+EQYvKHZKTCza5O1fhi9NtCTwI+BkDTrbz695i6JSW8ck6Zgt8QIfmOm4MvPBqJJfrzEXZAvHpmSlLNXfVRMT5w5mMlYl4dhPDeItNfZeRBRpDHBm/mn6UsYELAgMBAAECggEAJgzJ4khhGS8yxoHYYZ2pA25+oCB8+HjxUgeBH0GFrTe9K/JkaDWFUCoMGKK4MzB4VItyrjUKk5qWl7yiD35fW1Uh1GmbXX6e2JY8yK0dRG1fC9/QqKuAObu5Wn8WC5VaK8tAYTL6KOaBEVOJI3B7A0cGzG1ZC6pFOUWKXxzvkY+FRWKUBsQFSU6IkuzEN70dJHfgXlRz5+bOtY8zTzFMfPcb4Oz5nOpcm+ifD9+ki1MFyyLiS57bbHeSyZ59TrpSUbF/s6X4U6oON5l28WSlpcygmp5SgcMpH+7tPY4YnNDWRbJc/yX0P6ai0KkG9sPXDGjSwmvlwUXqfzmjUi2agQKBgQDzO7MOFf9F6rg3YZ08LS8oofWD/tIyXKuJkARvNEtmOF1AQFX9JwjfMHRvozfklZ9WNUqsX+rUXDQiJpHReJAKW/joFAQcSNVdVcwGo2Hz5Vtb2WC24KxvwkI2FIYE2p5leW1O1xDUAmvNtEMg2lIsPC1rqinX5wcjKWzOZSCnEwKBgQDnhmmR1snHmA4wwPVc2eedrHfSum9l+fYjsjmugQ8gROI+3FBiErLEquLhtMcd3EHUjHFEWjxqq8tjafzgH/OP04qmy21QlnT/DM7emXujSjKk15wEc5m4B2Uzgswu2j7Wl6B6njSXhk3PeX+PAjLi8j3xeWehc9i3mYSHoYslKQKBgC6m8q7u9SlZ3b9xj5DtMbBfcBHDHFnggF5AKzmRFC0k/m3GaTfG9uKoDo0jByNmt0r3qbzqIMZecPlj4HAG6cmy3kjVHfy204W1YQ6c47q98Qnq5avt5+T/o2dwBEyGCf94jMikY2vmkvq/amiwtzYYzLVry97HRw0tctsbdmnVAoGBANvhkqd4BsBxT6DL0Pry0/6yCkZu21dNEo+KNy/c0CHEad5rXEgYHAGdjcXv618XjMmw6+2PiWuBZrMuuIOetLE2paqM7m+nxMtpPZq4x2woDnrxbfHoW+gj1eAa65HfoegprrZlQ+tYGNPfPt7xpqRUjbGkrF/wZrTPrsclC8IpAoGAH8C7choCmSICtyZaept5hhmLSnKmolQ2ppF8LwBzM9i2ZpsiuvAjutHphf2oUbyGejLBMpJWVYjH7FbEAzARmB15JXjkXnBqDsrCzt5jG+6S8UiueGsdjt1Jl39+9Fm15b9ERkrxD+aZMnvynRTKiTJlxHh+Q2LloP2dcFwAy+o=

# 数据库配置
MYSQL_DATABASE=usdttrading
MYSQL_USER=usdtuser
MYSQL_PASSWORD=usdtpass123!

# 端口配置
BACKEND_PORT=8090
FRONTEND_ADMIN_PORT=3000
FRONTEND_USER_PORT=3001

# 日志配置
LOG_MAX_SIZE=100m
LOG_MAX_FILES=5
LOG_LEVEL=DEBUG
EOF

# 4. 创建Spring Boot配置文件
echo "4. 创建Spring Boot Docker配置..."
mkdir -p backend/src/main/resources
cat > backend/src/main/resources/application-docker.yml << 'EOF'
server:
  port: 8080
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true

spring:
  application:
    name: usdt-trading-platform
  
  # 数据库配置 - Docker环境
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/usdttrading?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: UsdtTrading123!
    hikari:
      pool-name: USDTTradingPool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 60000
      validation-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
      
  # Redis配置 - Docker环境  
  redis:
    host: redis
    port: 6379
    timeout: 6000ms
    lettuce:
      pool:
        max-active: 10
        max-wait: -1ms
        max-idle: 8
        min-idle: 2

  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        format_sql: false
        jdbc:
          time_zone: UTC

# RSA密钥配置
business:
  security:
    rsa:
      public-key: ${BUSINESS_SECURITY_RSA_PUBLIC_KEY}
      private-key: ${BUSINESS_SECURITY_RSA_PRIVATE_KEY}

# 日志配置
logging:
  level:
    com.usdttrading: DEBUG
    org.springframework: INFO
    org.hibernate: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /app/logs/application.log

# 管理端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
  health:
    redis:
      enabled: true
    db:
      enabled: true
EOF

# 5. 先启动基础服务
echo "5. 启动基础服务..."
docker-compose --env-file .env.docker up -d mysql redis

# 等待数据库完全启动
echo "6. 等待数据库启动..."
for i in {1..30}; do
    if docker exec usdt-mysql mysqladmin ping -h localhost -u root -pUsdtTrading123! --silent 2>/dev/null; then
        echo "✅ MySQL已启动"
        break
    fi
    echo "⏳ 等待MySQL启动... ($i/30)"
    sleep 2
done

# 等待Redis启动
for i in {1..10}; do
    if docker exec usdt-redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo "✅ Redis已启动"
        break
    fi
    echo "⏳ 等待Redis启动... ($i/10)"
    sleep 1
done

# 7. 重新构建后端
echo "7. 重新构建后端..."
cd backend
mvn clean package -DskipTests -q
docker build -t usdt-backend:latest .
cd ..

# 8. 启动后端服务
echo "8. 启动后端服务..."
docker-compose --env-file .env.docker up -d backend

# 9. 等待后端启动
echo "9. 等待后端服务启动..."
for i in {1..60}; do
    if curl -s -f http://localhost:8090/api/admin/auth/public-key >/dev/null 2>&1; then
        echo "✅ 后端API已启动"
        break
    fi
    echo "⏳ 等待后端API启动... ($i/60)"
    sleep 3
done

# 10. 验证服务
echo "10. 验证服务状态..."
echo "Docker容器状态:"
docker-compose --env-file .env.docker ps

echo ""
echo "测试RSA公钥端点:"
curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/admin/auth/public-key | jq . || echo "❌ API端点未响应"

echo ""
echo "🎉 Docker部署修复完成!"
echo "✅ 管理员前端: http://localhost:3000"
echo "✅ 后端API: http://localhost:8090"
echo "✅ RSA公钥端点: http://localhost:8090/api/admin/auth/public-key"