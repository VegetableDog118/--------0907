-- 创建测试用户账号脚本
-- 执行时间: 2024-01-15
-- 用途: 为系统创建不同角色的测试账号

USE `interface_platform`;

-- 删除现有测试用户（如果存在）
DELETE FROM `users` WHERE `username` IN ('admin', 'consumer', 'finance', 'tech');

-- 创建测试用户
-- 密码都使用BCrypt加密，明文密码分别为: admin123, consumer123, finance123, tech123
-- BCrypt加密后的密码: $2a$10$R9y46eW5aoLt8WUUm6MID.LmJKmTuO/qXlpZYHpmn1DebXCYcDuTm (对应admin123)
-- BCrypt加密后的密码: $2a$10$bio8XiMhB6G.gEcgP7dRfuFPHSINC9bYSIWNtnyPv6W7EBXUv00by (对应consumer123)
-- BCrypt加密后的密码: $2a$10$kP/D9i3I.75IYJcBTgxEsumKO5Gy/Lp3ZdRFIDbKBKqgp3faXhX7S (对应finance123)
-- BCrypt加密后的密码: $2a$10$SAj9CxuTguiRMvbND6FEH.eXWO5yDjKsLaDFspP3tEVnN/Af/3Yly (对应tech123)

INSERT INTO `users` (
    `id`, `username`, `password`, `real_name`, `company_name`, `credit_code`, 
    `phone`, `email`, `department`, `position`, `role`, `status`, 
    `app_id`, `api_key_enabled`, `create_time`, `update_time`, `create_by`
) VALUES 
-- 管理员账号
(
    'admin002', 'admin', '$2a$10$R9y46eW5aoLt8WUUm6MID.LmJKmTuO/qXlpZYHpmn1DebXCYcDuTm', 
    '系统管理员', '电力交易中心', '91110000000000010X', 
    '13800000011', 'admin2@powertrading.com', '信息技术部', '系统管理员', 
    'admin', 'active', 'admin-app-002', 1, NOW(), NOW(), 'system'
),
-- 数据消费者账号
(
    'consumer001', 'consumer', '$2a$10$bio8XiMhB6G.gEcgP7dRfuFPHSINC9bYSIWNtnyPv6W7EBXUv00by', 
    '数据消费者', '某电力公司', '91110000000000011X', 
    '13800000012', 'consumer@powertrading.com', '数据分析部', '数据分析师', 
    'consumer', 'active', 'consumer-app-001', 1, NOW(), NOW(), 'system'
),
-- 结算部账号
(
    'finance001', 'finance', '$2a$10$kP/D9i3I.75IYJcBTgxEsumKO5Gy/Lp3ZdRFIDbKBKqgp3faXhX7S', 
    '结算部经理', '电力交易中心', '91110000000000012X', 
    '13800000013', 'finance@powertrading.com', '结算部', '结算经理', 
    'settlement', 'active', 'finance-app-001', 1, NOW(), NOW(), 'system'
),
-- 技术部账号
(
    'tech001', 'tech', '$2a$10$SAj9CxuTguiRMvbND6FEH.eXWO5yDjKsLaDFspP3tEVnN/Af/3Yly', 
    '技术部工程师', '电力交易中心', '91110000000000013X', 
    '13800000014', 'tech@powertrading.com', '技术部', '系统工程师', 
    'tech', 'active', 'tech-app-001', 1, NOW(), NOW(), 'system'
);

-- 验证插入结果
SELECT 
    username, real_name, company_name, role, status, 
    phone, email, department, position, create_time
FROM `users` 
WHERE `username` IN ('admin', 'consumer', 'finance', 'tech')
ORDER BY `username`;

-- 显示账号信息
SELECT 
    '账号创建完成' as message,
    COUNT(*) as total_users
FROM `users` 
WHERE `username` IN ('admin', 'consumer', 'finance', 'tech');

SELECT 
    '登录信息' as info,
    'admin/admin123 - 系统管理员' as admin_account,
    'consumer/consumer123 - 数据消费者' as consumer_account,
    'finance/finance123 - 结算部' as finance_account,
    'tech/tech123 - 技术部' as tech_account;