USE usdt_trading_platform;

-- Final update for admin password to Admin123 (without exclamation mark)
-- BCrypt hash for 'Admin123' generated via API
UPDATE admins 
SET password = '$2a$10$V.Qz0iijUyEs4m1dQZeFKehiMBQeWZ9s3c2nwsMkug3LoZC6FuMF.'
WHERE username = 'admin';

-- Verify the update worked
SELECT username, LENGTH(password) as password_length, SUBSTRING(password, 1, 25) as password_prefix
FROM admins 
WHERE username = 'admin';