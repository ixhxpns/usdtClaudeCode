import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * Master Agent创建的独立RSA公钥测试服务
 * 模拟USDT Trading Platform的RSA公钥API端点
 */
public class RSATestServer {
    
    // Master Agent 生成的RSA密钥对
    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB";
    private static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDb+pQffI2s5svqyTvgs3VGcpZH4k4/uh+u5pj+bwnodJTzKb3BWnTWQJfRXLL9fGA55clL31FQJofpTyvQUIzGeEH6HZ+vpk7ulyfm7j+g2ZjejTNRwPf9BJYXUMTNfbDlNCAT8uMucgsEQlvvPCA+Mbq0CMuhaENJaWi1jRF9jDdeYdnEf51CLw2n15eA4tqTjclnhkwyYSYKXtXy4hsi0SFZMP+EQYvKHZKTCza5O1fhi9NtCTwI+BkDTrbz695i6JSW8ck6Zgt8QIfmOm4MvPBqJJfrzEXZAvHpmSlLNXfVRMT5w5mMlYl4dhPDeItNfZeRBRpDHBm/mn6UsYELAgMBAAECggEAJgzJ4khhGS8yxoHYYZ2pA25+oCB8+HjxUgeBH0GFrTe9K/JkaDWFUCoMGKK4MzB4VItyrjUKk5qWl7yiD35fW1Uh1GmbXX6e2JY8yK0dRG1fC9/QqKuAObu5Wn8WC5VaK8tAYTL6KOaBEVOJI3B7A0cGzG1ZC6pFOUWKXxzvkY+FRWKUBsQFSU6IkuzEN70dJHfgXlRz5+bOtY8zTzFMfPcb4Oz5nOpcm+ifD9+ki1MFyyLiS57bbHeSyZ59TrpSUbF/s6X4U6oON5l28WSlpcygmp5SgcMpH+7tPY4YnNDWRbJc/yX0P6ai0KkG9sPXDGjSwmvlwUXqfzmjUi2agQKBgQDzO7MOFf9F6rg3YZ08LS8oofWD/tIyXKuJkARvNEtmOF1AQFX9JwjfMHRvozfklZ9WNUqsX+rUXDQiJpHReJAKW/joFAQcSNVdVcwGo2Hz5Vtb2WC24KxvwkI2FIYE2p5leW1O1xDUAmvNtEMg2lIsPC1rqinX5wcjKWzOZSCnEwKBgQDnhmmR1snHmA4wwPVc2eedrHfSum9l+fYjsjmugQ8gROI+3FBiErLEquLhtMcd3EHUjHFEWjxqq8tjafzgH/OP04qmy21QlnT/DM7emXujSjKk15wEc5m4B2Uzgswu2j7Wl6B6njSXhk3PeX+PAjLi8j3xeWehc9i3mYSHoYslKQKBgC6m8q7u9SlZ3b9xj5DtMbBfcBHDHFnggF5AKzmRFC0k/m3GaTfG9uKoDo0jByNmt0r3qbzqIMZecPlj4HAG6cmy3kjVHfy204W1YQ6c47q98Qnq5avt5+T/o2dwBEyGCf94jMikY2vmkvq/amiwtzYYzLVry97HRw0tctsbdmnVAoGBANvhkqd4BsBxT6DL0Pry0/6yCkZu21dNEo+KNy/c0CHEad5rXEgYHAGdjcXv618XjMmw6+2PiWuBZrMuuIOetLE2paqM7m+nxMtpPZq4x2woDnrxbfHoW+gj1eAa65HfoegprrZlQ+tYGNPfPt7xpqRUjbGkrF/wZrTPrsclC8IpAoGAH8C7choCmSICtyZaept5hhmLSnKmolQ2ppF8LwBzM9i2ZpsiuvAjutHphf2oUbyGejLBMpJWVYjH7FbEAzARmB15JXjkXnBqDsrCzt5jG+6S8UiueGsdjt1Jl39+9Fm15b9ERkrxD+aZMnvynRTKiTJlxHh+Q2LloP2dcFwAy+o=";

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Master Agent RSA测试服务器启动中...");
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8090), 0);
        
        // 管理员RSA公钥端点
        server.createContext("/api/admin/auth/public-key", new RSAHandler());
        // 用户RSA公钥端点
        server.createContext("/api/auth/public-key", new RSAHandler());
        // 健康检查端点
        server.createContext("/api/actuator/health", new HealthHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("✅ RSA测试服务器已启动在端口 8090");
        System.out.println("📍 管理员端点: http://localhost:8090/api/admin/auth/public-key");
        System.out.println("📍 用户端点: http://localhost:8090/api/auth/public-key");
        System.out.println("🏥 健康检查: http://localhost:8090/api/actuator/health");
        System.out.println("🔧 按 Ctrl+C 停止服务");
    }
    
    static class RSAHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 设置CORS头
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, User-Agent");
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                String response = "{\"code\":405,\"message\":\"Method not allowed\",\"success\":false}";
                exchange.sendResponseHeaders(405, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
                return;
            }
            
            try {
                // 构建成功响应
                String jsonResponse = String.format(
                    "{\"code\":200,\"success\":true,\"message\":\"获取RSA公钥成功\",\"data\":{" +
                    "\"publicKey\":\"%s\"," +
                    "\"keyType\":\"RSA\"," +
                    "\"keySize\":\"2048\"," +
                    "\"algorithm\":\"RSA/ECB/PKCS1Padding\"," +
                    "\"server\":\"Master Agent RSA Test Server\"," +
                    "\"timestamp\":%d}}",
                    PUBLIC_KEY, System.currentTimeMillis()
                );
                
                exchange.sendResponseHeaders(200, jsonResponse.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes("UTF-8"));
                os.close();
                
                System.out.println("✅ RSA公钥请求处理成功: " + exchange.getRequestURI().getPath());
                
            } catch (Exception e) {
                String errorResponse = "{\"code\":500,\"message\":\"服务器内部错误\",\"success\":false}";
                exchange.sendResponseHeaders(500, errorResponse.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes("UTF-8"));
                os.close();
                
                System.err.println("❌ 处理RSA请求时发生错误: " + e.getMessage());
            }
        }
    }
    
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            
            String healthResponse = "{\"status\":\"UP\",\"components\":{\"rsa\":{\"status\":\"UP\",\"details\":{\"keyLength\":2048,\"algorithm\":\"RSA\"}}}}";
            
            exchange.sendResponseHeaders(200, healthResponse.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(healthResponse.getBytes("UTF-8"));
            os.close();
        }
    }
}