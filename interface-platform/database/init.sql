-- 电力交易中心接口服务平台数据库初始化脚本
-- 创建时间: 2024-01-15
-- 版本: v1.0

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `interface_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `interface_platform`;

-- ========================================
-- 用户管理相关表
-- ========================================

-- 用户表
CREATE TABLE `users` (
    `id` varchar(32) NOT NULL COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(255) NOT NULL COMMENT '密码（加密）',
    `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
    `company_name` varchar(100) NOT NULL COMMENT '企业名称',
    `credit_code` varchar(18) NOT NULL COMMENT '统一社会信用代码',
    `phone` varchar(11) NOT NULL COMMENT '手机号码',
    `email` varchar(100) NOT NULL COMMENT '邮箱地址',
    `department` varchar(50) DEFAULT NULL COMMENT '部门信息',
    `position` varchar(50) DEFAULT NULL COMMENT '职位信息',
    `role` enum('admin','settlement','tech','consumer') NOT NULL DEFAULT 'consumer' COMMENT '用户角色',
    `status` enum('active','inactive','pending') NOT NULL DEFAULT 'pending' COMMENT '用户状态',
    `app_id` varchar(32) DEFAULT NULL COMMENT 'API调用标识',
    `api_key_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'API密钥是否启用',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `login_count` int NOT NULL DEFAULT 0 COMMENT '登录次数',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_credit_code` (`credit_code`),
    UNIQUE KEY `uk_app_id` (`app_id`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE `roles` (
    `id` varchar(32) NOT NULL COMMENT '角色ID',
    `role_code` varchar(50) NOT NULL COMMENT '角色编码',
    `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    `description` varchar(200) DEFAULT NULL COMMENT '角色描述',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
CREATE TABLE `permissions` (
    `id` varchar(32) NOT NULL COMMENT '权限ID',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    `resource_type` enum('menu','button','api') NOT NULL COMMENT '资源类型',
    `resource_path` varchar(200) DEFAULT NULL COMMENT '资源路径',
    `parent_id` varchar(32) DEFAULT NULL COMMENT '父权限ID',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE `role_permissions` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `role_id` varchar(32) NOT NULL COMMENT '角色ID',
    `permission_id` varchar(32) NOT NULL COMMENT '权限ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ========================================
-- 接口管理相关表
-- ========================================

-- 接口分类表
CREATE TABLE `interface_categories` (
    `id` varchar(32) NOT NULL COMMENT '分类ID',
    `category_code` varchar(50) NOT NULL COMMENT '分类编码',
    `category_name` varchar(50) NOT NULL COMMENT '分类名称',
    `description` varchar(200) DEFAULT NULL COMMENT '分类描述',
    `color` varchar(7) DEFAULT NULL COMMENT '分类颜色',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口分类表';

-- 接口表
CREATE TABLE `interfaces` (
    `id` varchar(32) NOT NULL COMMENT '接口ID',
    `interface_name` varchar(100) NOT NULL COMMENT '接口名称',
    `interface_path` varchar(200) NOT NULL COMMENT '接口路径',
    `description` text COMMENT '接口描述',
    `category_id` varchar(32) NOT NULL COMMENT '分类ID',
    `data_source_id` varchar(32) NOT NULL COMMENT '数据源ID',
    `table_name` varchar(100) NOT NULL COMMENT '数据表名',
    `request_method` enum('GET','POST','PUT','DELETE') NOT NULL DEFAULT 'POST' COMMENT '请求方法',
    `status` enum('unpublished','published','offline') NOT NULL DEFAULT 'unpublished' COMMENT '接口状态',
    `version` varchar(10) NOT NULL DEFAULT '1.0' COMMENT '接口版本',
    `sql_template` text COMMENT 'SQL模板',
    `response_format` json COMMENT '响应格式定义',
    `rate_limit` int DEFAULT NULL COMMENT '限流配置（每分钟请求数）',
    `timeout` int NOT NULL DEFAULT 30 COMMENT '超时时间（秒）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `publish_time` datetime DEFAULT NULL COMMENT '上架时间',
    `offline_time` datetime DEFAULT NULL COMMENT '下架时间',
    `create_by` varchar(32) NOT NULL COMMENT '创建人',
    `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
    `publish_by` varchar(32) DEFAULT NULL COMMENT '上架人',
    `offline_by` varchar(32) DEFAULT NULL COMMENT '下架人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interface_path` (`interface_path`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口表';

-- 接口参数表
CREATE TABLE `interface_parameters` (
    `id` varchar(32) NOT NULL COMMENT '参数ID',
    `interface_id` varchar(32) NOT NULL COMMENT '接口ID',
    `param_name` varchar(50) NOT NULL COMMENT '参数名称',
    `param_type` enum('string','integer','number','boolean','date','datetime') NOT NULL COMMENT '参数类型',
    `param_location` enum('query','body','header','path') NOT NULL DEFAULT 'body' COMMENT '参数位置',
    `description` varchar(200) DEFAULT NULL COMMENT '参数描述',
    `required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必需',
    `default_value` varchar(100) DEFAULT NULL COMMENT '默认值',
    `validation_rule` varchar(200) DEFAULT NULL COMMENT '校验规则',
    `example` varchar(100) DEFAULT NULL COMMENT '示例值',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_interface_id` (`interface_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口参数表';

-- ========================================
-- 数据源管理相关表
-- ========================================

-- 数据源表
CREATE TABLE `data_sources` (
    `id` varchar(32) NOT NULL COMMENT '数据源ID',
    `source_name` varchar(100) NOT NULL COMMENT '数据源名称',
    `source_type` enum('mysql','postgresql','oracle','sqlserver') NOT NULL COMMENT '数据源类型',
    `host` varchar(100) NOT NULL COMMENT '主机地址',
    `port` int NOT NULL COMMENT '端口',
    `database_name` varchar(100) NOT NULL COMMENT '数据库名',
    `username` varchar(100) NOT NULL COMMENT '用户名',
    `password` varchar(255) NOT NULL COMMENT '密码（加密）',
    `jdbc_url` varchar(500) NOT NULL COMMENT 'JDBC连接URL',
    `driver_class` varchar(200) NOT NULL COMMENT '驱动类名',
    `max_pool_size` int NOT NULL DEFAULT 20 COMMENT '最大连接池大小',
    `min_idle` int NOT NULL DEFAULT 5 COMMENT '最小空闲连接数',
    `connection_timeout` int NOT NULL DEFAULT 30000 COMMENT '连接超时时间（毫秒）',
    `idle_timeout` int NOT NULL DEFAULT 600000 COMMENT '空闲超时时间（毫秒）',
    `max_lifetime` int NOT NULL DEFAULT 1800000 COMMENT '连接最大生存时间（毫秒）',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(32) NOT NULL COMMENT '创建人',
    `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_source_name` (`source_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源表';

-- ========================================
-- 订阅申请相关表
-- ========================================

-- 订阅申请表
CREATE TABLE `subscription_applications` (
    `id` varchar(32) NOT NULL COMMENT '申请ID',
    `user_id` varchar(32) NOT NULL COMMENT '申请用户ID',
    `interface_ids` json NOT NULL COMMENT '申请接口ID列表',
    `reason` text NOT NULL COMMENT '申请理由',
    `business_scenario` text COMMENT '业务场景描述',
    `estimated_calls` int DEFAULT NULL COMMENT '预计每日调用次数',
    `status` enum('pending','approved','rejected') NOT NULL DEFAULT 'pending' COMMENT '申请状态',
    `submit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `process_time` datetime DEFAULT NULL COMMENT '处理时间',
    `process_by` varchar(32) DEFAULT NULL COMMENT '处理人',
    `process_comment` text COMMENT '处理意见',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅申请表';

-- 用户接口订阅表
CREATE TABLE `user_interface_subscriptions` (
    `id` varchar(32) NOT NULL COMMENT 'ID',
    `user_id` varchar(32) NOT NULL COMMENT '用户ID',
    `interface_id` varchar(32) NOT NULL COMMENT '接口ID',
    `application_id` varchar(32) NOT NULL COMMENT '申请ID',
    `status` enum('active','inactive','expired','cancelled') NOT NULL DEFAULT 'active' COMMENT '订阅状态',
    `subscribe_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订阅时间',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
    `call_count` int NOT NULL DEFAULT 0 COMMENT '调用次数',
    `last_call_time` datetime DEFAULT NULL COMMENT '最后调用时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_interface` (`user_id`, `interface_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_interface_id` (`interface_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户接口订阅表';

-- ========================================
-- 系统日志相关表
-- ========================================

-- 操作日志表
CREATE TABLE `operation_logs` (
    `id` varchar(32) NOT NULL COMMENT '日志ID',
    `user_id` varchar(32) DEFAULT NULL COMMENT '操作用户ID',
    `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
    `operation` varchar(100) NOT NULL COMMENT '操作类型',
    `method` varchar(200) DEFAULT NULL COMMENT '操作方法',
    `params` text COMMENT '操作参数',
    `result` text COMMENT '操作结果',
    `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
    `execution_time` int DEFAULT NULL COMMENT '执行时间（毫秒）',
    `status` enum('success','failure','error') NOT NULL COMMENT '操作状态',
    `error_message` text COMMENT '错误信息',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation` (`operation`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- API调用日志表
CREATE TABLE `api_call_logs` (
    `id` varchar(32) NOT NULL COMMENT '日志ID',
    `request_id` varchar(32) DEFAULT NULL COMMENT '请求ID',
    `user_id` varchar(32) DEFAULT NULL COMMENT '调用用户ID',
    `app_id` varchar(32) DEFAULT NULL COMMENT '应用ID',
    `interface_id` varchar(32) NOT NULL COMMENT '接口ID',
    `interface_path` varchar(200) NOT NULL COMMENT '接口路径',
    `request_method` varchar(10) NOT NULL COMMENT '请求方法',
    `request_params` text COMMENT '请求参数',
    `response_data` text COMMENT '响应数据',
    `response_code` int NOT NULL COMMENT '响应状态码',
    `response_message` varchar(500) DEFAULT NULL COMMENT '响应消息',
    `execution_time` int NOT NULL COMMENT '执行时间（毫秒）',
    `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_interface_id` (`interface_id`),
    KEY `idx_response_code` (`response_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用日志表';

-- ========================================
-- 通知相关表
-- ========================================

-- 通知表
CREATE TABLE `notifications` (
    `id` varchar(32) NOT NULL COMMENT '通知ID',
    `user_id` varchar(32) NOT NULL COMMENT '接收用户ID',
    `title` varchar(200) NOT NULL COMMENT '通知标题',
    `content` text NOT NULL COMMENT '通知内容',
    `type` enum('system','approval','subscription','api') NOT NULL COMMENT '通知类型',
    `status` enum('unread','read') NOT NULL DEFAULT 'unread' COMMENT '阅读状态',
    `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 通知模板表
CREATE TABLE `notification_templates` (
    `id` varchar(32) NOT NULL COMMENT '模板ID',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_type` enum('email','sms','system') NOT NULL COMMENT '模板类型',
    `title_template` varchar(200) NOT NULL COMMENT '标题模板',
    `content_template` text NOT NULL COMMENT '内容模板',
    `variables` json COMMENT '模板变量定义',
    `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- ========================================
-- 初始化数据
-- ========================================

-- 初始化接口分类数据
INSERT INTO `interface_categories` (`id`, `category_code`, `category_name`, `description`, `color`, `sort_order`) VALUES
('cat001', 'day_ahead_spot', '日前现货', '日前现货市场相关数据接口', '#1890ff', 1),
('cat002', 'forecast', '预测', '负荷预测、新能源预测等预测类数据接口', '#52c41a', 2),
('cat003', 'ancillary_service', '辅助服务', '调频、调压、备用等辅助服务数据接口', '#faad14', 3),
('cat004', 'grid_operation', '电网运行', '电网运行状态、约束情况等运行数据接口', '#f5222d', 4);

-- 初始化角色数据
INSERT INTO `roles` (`id`, `role_code`, `role_name`, `description`) VALUES
('role001', 'admin', '系统管理员', '拥有系统所有权限'),
('role002', 'settlement', '结算部', '负责接口上架下架、订阅审批'),
('role003', 'tech', '技术部', '负责接口生成、配置、测试'),
('role004', 'consumer', '数据消费者', '可浏览接口目录、订阅接口、调用API');

-- 初始化管理员用户
INSERT INTO `users` (`id`, `username`, `password`, `real_name`, `company_name`, `credit_code`, `phone`, `email`, `role`, `status`, `app_id`, `create_by`) VALUES
('admin001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyZOzx8Qb6.1JCnJdXXnfYPXJzO', '系统管理员', '电力交易中心', '91110000000000000X', '13800000000', 'admin@powertrading.com', 'admin', 'active', 'admin-app-id-001', 'system');

-- 初始化系统配置
CREATE TABLE `system_configs` (
    `id` varchar(32) NOT NULL COMMENT '配置ID',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `config_value` text COMMENT '配置值',
    `config_type` enum('string','number','boolean','json') NOT NULL DEFAULT 'string' COMMENT '配置类型',
    `description` varchar(200) DEFAULT NULL COMMENT '配置描述',
    `editable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可编辑',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 初始化系统配置数据
INSERT INTO `system_configs` (`id`, `config_key`, `config_value`, `config_type`, `description`) VALUES
('cfg001', 'system.name', '电力交易中心接口服务平台', 'string', '系统名称'),
('cfg002', 'system.version', '1.0.0', 'string', '系统版本'),
('cfg003', 'api.rate_limit.default', '1000', 'number', '默认API限流配置（每分钟）'),
('cfg004', 'api.timeout.default', '30', 'number', '默认API超时时间（秒）'),
('cfg005', 'notification.email.enabled', 'true', 'boolean', '是否启用邮件通知'),
('cfg006', 'notification.sms.enabled', 'false', 'boolean', '是否启用短信通知');