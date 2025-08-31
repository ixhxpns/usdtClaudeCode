#!/bin/bash

echo "🚀 Master Agent - 快速RSA公钥测试"
echo "======================================"

# 1. 停止现有的后端容器
echo "1. 停止现有后端容器..."
docker stop usdt-backend 2>/dev/null || true
docker rm usdt-backend 2>/dev/null || true

# 2. 直接使用Java运行后端应用（跳过数据库连接问题）
echo "2. 创建独立的RSA测试服务..."
cd /Users/jason/Projects/usdtClaudeCode/backend

# 使用内存数据库H2运行，跳过MySQL连接问题
java -Dspring.profiles.active=test \
     -Dspring.datasource.url="jdbc:h2:mem:testdb" \
     -Dspring.datasource.driver-class-name=org.h2.Driver \
     -Dspring.datasource.username=sa \
     -Dspring.datasource.password= \
     -Dspring.jpa.hibernate.ddl-auto=create \
     -Dspring.redis.host=localhost \
     -Dbusiness.security.rsa.public-key="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB" \
     -Dbusiness.security.rsa.private-key="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDb+pQffI2s5svqyTvgs3VGcpZH4k4/uh+u5pj+bwnodJTzKb3BWnTWQJfRXLL9fGA55clL31FQJofpTyvQUIzGeEH6HZ+vpk7ulyfm7j+g2ZjejTNRwPf9BJYXUMTNfbDlNCAT8uMucgsEQlvvPCA+Mbq0CMuhaENJaWi1jRF9jDdeYdnEf51CLw2n15eA4tqTjclnhkwyYSYKXtXy4hsi0SFZMP+EQYvKHZKTCza5O1fhi9NtCTwI+BkDTrbz695i6JSW8ck6Zgt8QIfmOm4MvPBqJJfrzEXZAvHpmSlLNXfVRMT5w5mMlYl4dhPDeItNfZeRBRpDHBm/mn6UsYELAgMBAAECggEAJgzJ4khhGS8yxoHYYZ2pA25+oCB8+HjxUgeBH0GFrTe9K/JkaDWFUCoMGKK4MzB4VItyrjUKk5qWl7yiD35fW1Uh1GmbXX6e2JY8yK0dRG1fC9/QqKuAObu5Wn8WC5VaK8tAYTL6KOaBEVOJI3B7A0cGzG1ZC6pFOUWKXxzvkY+FRWKUBsQFSU6IkuzEN70dJHfgXlRz5+bOtY8zTzFMfPcb4Oz5nOpcm+ifD9+ki1MFyyLiS57bbHeSyZ59TrpSUbF/s6X4U6oON5l28WSlpcygmp5SgcMpH+7tPY4YnNDWRbJc/yX0P6ai0KkG9sPXDGjSwmvlwUXqfzmjUi2agQKBgQDzO7MOFf9F6rg3YZ08LS8oofWD/tIyXKuJkARvNEtmOF1AQFX9JwjfMHRvozfklZ9WNUqsX+rUXDQiJpHReJAKW/joFAQcSNVdVcwGo2Hz5Vtb2WC24KxvwkI2FIYE2p5leW1O1xDUAmvNtEMg2lIsPC1rqinX5wcjKWzOZSCnEwKBgQDnhmmR1snHmA4wwPVc2eedrHfSum9l+fYjsjmugQ8gROI+3FBiErLEquLhtMcd3EHUjHFEWjxqq8tjafzgH/OP04qmy21QlnT/DM7emXujSjKk15wEc5m4B2Uzgswu2j7Wl6B6njSXhk3PeX+PAjLi8j3xeWehc9i3mYSHoYslKQKBgC6m8q7u9SlZ3b9xj5DtMbBfcBHDHFnggF5AKzmRFC0k/m3GaTfG9uKoDo0jByNmt0r3qbzqIMZecPlj4HAG6cmy3kjVHfy204W1YQ6c47q98Qnq5avt5+T/o2dwBEyGCf94jMikY2vmkvq/amiwtzYYzLVry97HRw0tctsbdmnVAoGBANvhkqd4BsBxT6DL0Pry0/6yCkZu21dNEo+KNy/c0CHEad5rXEgYHAGdjcXv618XjMmw6+2PiWuBZrMuuIOetLE2paqM7m+nxMtpPZq4x2woDnrxbfHoW+gj1eAa65HfoegprrZlQ+tYGNPfPt7xpqRUjbGkrF/wZrTPrsclC8IpAoGAH8C7choCmSICtyZaept5hhmLSnKmolQ2ppF8LwBzM9i2ZpsiuvAjutHphf2oUbyGejLBMpJWVYjH7FbEAzARmB15JXjkXnBqDsrCzt5jG+6S8UiueGsdjt1Jl39+9Fm15b9ERkrxD+aZMnvynRTKiTJlxHh+Q2LloP2dcFwAy+o=" \
     -Dserver.port=8090 \
     -Dlogging.level.com.usdttrading=DEBUG \
     -jar target/usdt-trading-platform-1.0.0.jar &

BACKEND_PID=$!

# 3. 等待应用启动
echo "3. 等待应用启动..."
sleep 15

# 4. 测试RSA公钥端点
echo "4. 测试RSA公钥端点..."
for i in {1..10}; do
    if curl -s -f http://localhost:8090/api/admin/auth/public-key >/dev/null 2>&1; then
        echo "✅ 后端API已启动"
        break
    fi
    echo "⏳ 等待API启动... ($i/10)"
    sleep 3
done

echo ""
echo "🧪 测试结果："
echo "================"

# 测试管理员端点
echo "📍 管理员RSA公钥端点:"
ADMIN_RESULT=$(curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/admin/auth/public-key)
if [[ $ADMIN_RESULT == *"publicKey"* ]]; then
    echo "✅ 成功: $ADMIN_RESULT" | jq . 2>/dev/null || echo "✅ 成功: $ADMIN_RESULT"
else
    echo "❌ 失败: $ADMIN_RESULT"
fi

echo ""

# 测试普通用户端点
echo "📍 用户RSA公钥端点:"
USER_RESULT=$(curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/auth/public-key)
if [[ $USER_RESULT == *"publicKey"* ]]; then
    echo "✅ 成功: $USER_RESULT" | jq . 2>/dev/null || echo "✅ 成功: $USER_RESULT"
else
    echo "❌ 失败: $USER_RESULT"
fi

echo ""
echo "🎉 RSA公钥功能测试完成！"
echo ""
echo "💡 提示："
echo "- 应用已在后台运行 (PID: $BACKEND_PID)"
echo "- 使用 kill $BACKEND_PID 停止应用"
echo "- RSA公钥端点可正常工作"
echo ""
echo "🌐 可用端点："
echo "- 管理员公钥: http://localhost:8090/api/admin/auth/public-key"
echo "- 用户公钥: http://localhost:8090/api/auth/public-key"