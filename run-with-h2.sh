#!/bin/bash

echo "🚀 Master Agent - 使用H2数据库启动服务"
echo "======================================"

# 停止所有现有服务
echo "1. 停止现有服务..."
docker stop usdt-backend-fixed usdt-backend 2>/dev/null || true
docker rm usdt-backend-fixed usdt-backend 2>/dev/null || true
pkill -f "RSATestServer" 2>/dev/null || true

# 直接使用JAR运行，使用H2内存数据库
echo "2. 启动后端服务（使用H2数据库）..."
cd /Users/jason/Projects/usdtClaudeCode/backend

nohup java \
    -Xms512m -Xmx1024m \
    -Dspring.profiles.active=h2 \
    -Dspring.datasource.url="jdbc:h2:mem:usdttrading;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" \
    -Dspring.datasource.driver-class-name=org.h2.Driver \
    -Dspring.datasource.username=sa \
    -Dspring.datasource.password= \
    -Dspring.jpa.hibernate.ddl-auto=create-drop \
    -Dspring.jpa.show-sql=false \
    -Dspring.h2.console.enabled=true \
    -Dserver.port=8090 \
    -Dserver.servlet.context-path=/api \
    -Dbusiness.security.rsa.public-key="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB" \
    -Dbusiness.security.rsa.private-key="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDb+pQffI2s5svqyTvgs3VGcpZH4k4/uh+u5pj+bwnodJTzKb3BWnTWQJfRXLL9fGA55clL31FQJofpTyvQUIzGeEH6HZ+vpk7ulyfm7j+g2ZjejTNRwPf9BJYXUMTNfbDlNCAT8uMucgsEQlvvPCA+Mbq0CMuhaENJaWi1jRF9jDdeYdnEf51CLw2n15eA4tqTjclnhkwyYSYKXtXy4hsi0SFZMP+EQYvKHZKTCza5O1fhi9NtCTwI+BkDTrbz695i6JSW8ck6Zgt8QIfmOm4MvPBqJJfrzEXZAvHpmSlLNXfVRMT5w5mMlYl4dhPDeItNfZeRBRpDHBm/mn6UsYELAgMBAAECggEAJgzJ4khhGS8yxoHYYZ2pA25+oCB8+HjxUgeBH0GFrTe9K/JkaDWFUCoMGKK4MzB4VItyrjUKk5qWl7yiD35fW1Uh1GmbXX6e2JY8yK0dRG1fC9/QqKuAObu5Wn8WC5VaK8tAYTL6KOaBEVOJI3B7A0cGzG1ZC6pFOUWKXxzvkY+FRWKUBsQFSU6IkuzEN70dJHfgXlRz5+bOtY8zTzFMfPcb4Oz5nOpcm+ifD9+ki1MFyyLiS57bbHeSyZ59TrpSUbF/s6X4U6oON5l28WSlpcygmp5SgcMpH+7tPY4YnNDWRbJc/yX0P6ai0KkG9sPXDGjSwmvlwUXqfzmjUi2agQKBgQDzO7MOFf9F6rg3YZ08LS8oofWD/tIyXKuJkARvNEtmOF1AQFX9JwjfMHRvozfklZ9WNUqsX+rUXDQiJpHReJAKW/joFAQcSNVdVcwGo2Hz5Vtb2WC24KxvwkI2FIYE2p5leW1O1xDUAmvNtEMg2lIsPC1rqinX5wcjKWzOZSCnEwKBgQDnhmmR1snHmA4wwPVc2eedrHfSum9l+fYjsjmugQ8gROI+3FBiErLEquLhtMcd3EHUjHFEWjxqq8tjafzgH/OP04qmy21QlnT/DM7emXujSjKk15wEc5m4B2Uzgswu2j7Wl6B6njSXhk3PeX+PAjLi8j3xeWehc9i3mYSHoYslKQKBgC6m8q7u9SlZ3b9xj5DtMbBfcBHDHFnggF5AKzmRFC0k/m3GaTfG9uKoDo0jByNmt0r3qbzqIMZecPlj4HAG6cmy3kjVHfy204W1YQ6c47q98Qnq5avt5+T/o2dwBEyGCf94jMikY2vmkvq/amiwtzYYzLVry97HRw0tctsbdmnVAoGBANvhkqd4BsBxT6DL0Pry0/6yCkZu21dNEo+KNy/c0CHEad5rXEgYHAGdjcXv618XjMmw6+2PiWuBZrMuuIOetLE2paqM7m+nxMtpPZq4x2woDnrxbfHoW+gj1eAa65HfoegprrZlQ+tYGNPfPt7xpqRUjbGkrF/wZrTPrsclC8IpAoGAH8C7choCmSICtyZaept5hhmLSnKmolQ2ppF8LwBzM9i2ZpsiuvAjutHphf2oUbyGejLBMpJWVYjH7FbEAzARmB15JXjkXnBqDsrCzt5jG+6S8UiueGsdjt1Jl39+9Fm15b9ERkrxD+aZMnvynRTKiTJlxHh+Q2LloP2dcFwAy+o=" \
    -Dlogging.level.com.usdttrading=INFO \
    -Dlogging.level.org.springframework=WARN \
    -Dlogging.level.org.hibernate=WARN \
    -jar target/usdt-trading-platform-1.0.0.jar > backend.log 2>&1 &

BACKEND_PID=$!
echo "后端服务PID: $BACKEND_PID"

# 等待服务启动
echo "3. 等待服务启动..."
sleep 20

# 测试API
echo "4. 测试API端点..."
for i in {1..30}; do
    if curl -s -f http://localhost:8090/api/admin/auth/public-key >/dev/null 2>&1; then
        echo "✅ 后端API已启动"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ API启动超时，查看日志:"
        tail -20 backend.log
        exit 1
    fi
    echo "⏳ 等待API启动... ($i/30)"
    sleep 2
done

echo ""
echo "🎉 服务启动完成！"
echo "==================="
echo ""
echo "✅ 后端服务: http://localhost:8090"
echo "✅ RSA公钥API: http://localhost:8090/api/admin/auth/public-key"
echo "✅ H2控制台: http://localhost:8090/api/h2-console"
echo ""
echo "🧪 测试命令:"
echo "curl -s http://localhost:8090/api/admin/auth/public-key | jq ."
echo ""
echo "📊 测试结果:"
curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/admin/auth/public-key | jq .
echo ""
echo "🛠️  停止服务: kill $BACKEND_PID"
echo "📋 查看日志: tail -f backend.log"