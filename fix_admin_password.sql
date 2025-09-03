USE usdt_trading_platform;

-- Update admin password to Admin123 (without exclamation mark)
-- BCrypt hash for 'Admin123'
UPDATE admins 
SET password = '$2a$10$jUTn63thDb8rPlCIRAMR0eTsJwWXnXv1ICeecbT3jv2KVO1kq2deG'
WHERE username = 'admin';

-- Verify the update
SELECT username, LENGTH(password) as password_length, SUBSTRING(password, 1, 20) as password_prefix
FROM admins 
WHERE username = 'admin';