-- 用户管理模块数据库初始化脚本
-- 创建时间: 2024-01-15
-- 作者: PowerTrading Team

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS interface_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE interface_platform;

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（加密）',
  `company_name` varchar(100) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(18) NOT NULL COMMENT '统一社会信用代码',
  `contact_name` varchar(20) NOT NULL COMMENT '联系人姓名',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号码',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱地址',
  `department` varchar(50) DEFAULT NULL COMMENT '部门',
  `position` varchar(50) DEFAULT NULL COMMENT '职位',
  `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '角色（ADMIN/USER）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/ACTIVE/LOCKED/REJECTED）',
  `app_id` varchar(64) DEFAULT NULL COMMENT 'API应用ID',
  `app_secret` varchar(128) DEFAULT NULL COMMENT 'API应用密钥',
  `login_fail_count` int NOT NULL DEFAULT '0' COMMENT '登录失败次数',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志（0-未删除，1-已删除）',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_credit_code` (`credit_code`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_status` (`status`),
  KEY `idx_role` (`role`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_company_name` (`company_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `role_id` varchar(32) NOT NULL COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL COMMENT '角色描述',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/INACTIVE）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志（0-未删除，1-已删除）',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
  `permission_id` varchar(32) NOT NULL COMMENT '权限ID',
  `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
  `resource_type` varchar(20) NOT NULL COMMENT '资源类型（MENU/BUTTON/API）',
  `resource_path` varchar(200) DEFAULT NULL COMMENT '资源路径',
  `parent_id` varchar(32) DEFAULT NULL COMMENT '父权限ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(200) DEFAULT NULL COMMENT '权限描述',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/INACTIVE）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志（0-未删除，1-已删除）',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色权限关联表
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` varchar(32) NOT NULL COMMENT '角色ID',
  `permission_id` varchar(32) NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 用户登录日志表
DROP TABLE IF EXISTS `user_login_logs`;
CREATE TABLE `user_login_logs` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `login_type` varchar(20) NOT NULL COMMENT '登录方式（USERNAME/PHONE/EMAIL）',
  `login_ip` varchar(45) NOT NULL COMMENT '登录IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `login_status` varchar(20) NOT NULL COMMENT '登录状态（SUCCESS/FAILED）',
  `fail_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

-- 插入初始角色数据
INSERT INTO `roles` (`role_id`, `role_name`, `role_code`, `description`) VALUES
('role_admin', '系统管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('role_user', '普通用户', 'USER', '普通用户，拥有基本权限');

-- 插入初始权限数据
INSERT INTO `permissions` (`permission_id`, `permission_name`, `permission_code`, `resource_type`, `resource_path`, `parent_id`, `sort_order`, `description`) VALUES
-- 用户管理权限
('perm_user_mgmt', '用户管理', 'user:*', 'MENU', '/user', NULL, 1, '用户管理模块'),
('perm_user_read', '查看用户', 'user:read', 'API', '/api/v1/users/**', 'perm_user_mgmt', 1, '查看用户信息'),
('perm_user_write', '编辑用户', 'user:write', 'API', '/api/v1/users/**', 'perm_user_mgmt', 2, '编辑用户信息'),
('perm_user_delete', '删除用户', 'user:delete', 'API', '/api/v1/users/**', 'perm_user_mgmt', 3, '删除用户'),
('perm_user_read_self', '查看个人信息', 'user:read:self', 'API', '/api/v1/users/profile', 'perm_user_mgmt', 4, '查看个人信息'),
('perm_user_write_self', '编辑个人信息', 'user:write:self', 'API', '/api/v1/users/profile', 'perm_user_mgmt', 5, '编辑个人信息'),

-- 接口管理权限
('perm_interface_mgmt', '接口管理', 'interface:*', 'MENU', '/interface', NULL, 2, '接口管理模块'),
('perm_interface_read', '查看接口', 'interface:read', 'API', '/api/v1/interfaces/**', 'perm_interface_mgmt', 1, '查看接口信息'),
('perm_interface_write', '编辑接口', 'interface:write', 'API', '/api/v1/interfaces/**', 'perm_interface_mgmt', 2, '编辑接口信息'),
('perm_interface_delete', '删除接口', 'interface:delete', 'API', '/api/v1/interfaces/**', 'perm_interface_mgmt', 3, '删除接口'),
('perm_interface_call', '调用接口', 'interface:call', 'API', '/api/v1/interfaces/call/**', 'perm_interface_mgmt', 4, '调用接口'),

-- 数据源管理权限
('perm_datasource_mgmt', '数据源管理', 'datasource:*', 'MENU', '/datasource', NULL, 3, '数据源管理模块'),
('perm_datasource_read', '查看数据源', 'datasource:read', 'API', '/api/v1/datasources/**', 'perm_datasource_mgmt', 1, '查看数据源信息'),
('perm_datasource_write', '编辑数据源', 'datasource:write', 'API', '/api/v1/datasources/**', 'perm_datasource_mgmt', 2, '编辑数据源信息'),
('perm_datasource_delete', '删除数据源', 'datasource:delete', 'API', '/api/v1/datasources/**', 'perm_datasource_mgmt', 3, '删除数据源'),

-- 系统管理权限
('perm_system_mgmt', '系统管理', 'system:*', 'MENU', '/system', NULL, 4, '系统管理模块'),
('perm_system_read', '查看系统信息', 'system:read', 'API', '/api/v1/system/**', 'perm_system_mgmt', 1, '查看系统信息'),
('perm_system_write', '系统配置', 'system:write', 'API', '/api/v1/system/**', 'perm_system_mgmt', 2, '系统配置管理');

-- 插入角色权限关联数据
-- 管理员拥有所有权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT 'role_admin', `permission_id` FROM `permissions` WHERE `deleted` = 0;

-- 普通用户权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES
('role_user', 'perm_user_read_self'),
('role_user', 'perm_user_write_self'),
('role_user', 'perm_interface_read'),
('role_user', 'perm_interface_call');

-- 插入默认管理员用户（密码：Admin123!）
INSERT INTO `users` (
  `user_id`, `username`, `password`, `company_name`, `credit_code`, 
  `contact_name`, `phone`, `email`, `role`, `status`, 
  `app_id`, `app_secret`
) VALUES (
  'admin_001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXgwkOBbYbqnhHGOw3qF7XuVtGi', 
  '系统管理', '000000000000000000', '系统管理员', '13800000000', 
  'admin@powertrading.com', 'ADMIN', 'ACTIVE', 
  'app_admin_001', 'admin_secret_key_2024'
);

-- 创建索引优化查询性能
CREATE INDEX idx_users_company_status ON users(company_name, status);
CREATE INDEX idx_users_role_status ON users(role, status);
CREATE INDEX idx_login_logs_user_time ON user_login_logs(user_id, login_time);

-- 添加表注释
ALTER TABLE users COMMENT = '用户表 - 存储用户基本信息和认证信息';
ALTER TABLE roles COMMENT = '角色表 - 定义系统角色';
ALTER TABLE permissions COMMENT = '权限表 - 定义系统权限';
ALTER TABLE role_permissions COMMENT = '角色权限关联表 - 角色和权限的多对多关系';
ALTER TABLE user_login_logs COMMENT = '用户登录日志表 - 记录用户登录行为';