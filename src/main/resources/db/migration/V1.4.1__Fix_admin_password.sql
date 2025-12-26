-- Fix admin password hash (password: admin)
-- The previous hash was incorrect (was hash for "password", not "admin")
UPDATE users
SET password = '$2a$10$C2Bwr2sjb3/5W2lC2MkAoeGRYTS6tLkBK8VM7tnOnXYxD4jw46mUu'
WHERE username = 'admin';
