import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 使用Spring Security的BCryptPasswordEncoder生成密碼哈希
 * 確保與後端完全兼容
 */
public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Admin123!";
        
        // 生成新的哈希
        String hash = encoder.encode(password);
        System.out.println("密碼: " + password);
        System.out.println("Spring BCrypt哈希: " + hash);
        
        // 驗證哈希
        boolean matches = encoder.matches(password, hash);
        System.out.println("驗證結果: " + matches);
        
        // 測試原來的$2b$哈希
        String originalHash = "$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa";
        boolean originalMatches = encoder.matches(password, originalHash);
        System.out.println("原$2b$哈希驗證: " + originalMatches);
        
        // 測試$2a$哈希
        String hash2a = "$2a$10$anrI.n4dhiN3AIJ2ZzmWQeQRrWf.4HXEMC1EJi4ral7pDbUdiv.9m";
        boolean matches2a = encoder.matches(password, hash2a);
        System.out.println("$2a$哈希驗證: " + matches2a);
    }
}