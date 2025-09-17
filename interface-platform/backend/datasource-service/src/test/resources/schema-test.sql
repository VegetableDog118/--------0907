-- 测试数据库表结构
-- 用于单元测试和集成测试的数据库表定义

-- 数据源表
CREATE TABLE IF NOT EXISTS datasources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    url VARCHAR(500) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    driver_class_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    max_pool_size INT DEFAULT 10,
    min_pool_size INT DEFAULT 1,
    connection_timeout INT DEFAULT 30000,
    idle_timeout INT DEFAULT 600000,
    max_lifetime INT DEFAULT 1800000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- 数据源监控指标表
CREATE TABLE IF NOT EXISTS datasource_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    datasource_id BIGINT NOT NULL,
    connection_success_count BIGINT DEFAULT 0,
    connection_failure_count BIGINT DEFAULT 0,
    query_count BIGINT DEFAULT 0,
    slow_query_count BIGINT DEFAULT 0,
    total_query_time BIGINT DEFAULT 0,
    max_query_time BIGINT DEFAULT 0,
    min_query_time BIGINT DEFAULT 0,
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (datasource_id) REFERENCES datasources(id) ON DELETE CASCADE
);

-- 数据源健康检查记录表
CREATE TABLE IF NOT EXISTS datasource_health_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    datasource_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_time BIGINT NOT NULL,
    error_message TEXT,
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (datasource_id) REFERENCES datasources(id) ON DELETE CASCADE
);

-- 查询缓存表
CREATE TABLE IF NOT EXISTS query_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(255) NOT NULL UNIQUE,
    datasource_id BIGINT NOT NULL,
    sql_hash VARCHAR(64) NOT NULL,
    result_data CLOB,
    row_count INT DEFAULT 0,
    cache_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP,
    hit_count BIGINT DEFAULT 0,
    last_hit_time TIMESTAMP,
    FOREIGN KEY (datasource_id) REFERENCES datasources(id) ON DELETE CASCADE
);

-- 慢查询记录表
CREATE TABLE IF NOT EXISTS slow_queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    datasource_id BIGINT NOT NULL,
    sql_text CLOB NOT NULL,
    execution_time BIGINT NOT NULL,
    parameters TEXT,
    execution_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(100),
    FOREIGN KEY (datasource_id) REFERENCES datasources(id) ON DELETE CASCADE
);

-- 数据源配置变更历史表
CREATE TABLE IF NOT EXISTS datasource_config_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    datasource_id BIGINT NOT NULL,
    operation_type VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE
    old_config CLOB,
    new_config CLOB,
    change_reason VARCHAR(500),
    changed_by VARCHAR(100),
    change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (datasource_id) REFERENCES datasources(id) ON DELETE CASCADE
);

-- 测试用户表（用于查询测试）
CREATE TABLE IF NOT EXISTS test_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 测试订单表（用于查询测试）
CREATE TABLE IF NOT EXISTS test_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES test_users(id)
);

-- 测试产品表（用于查询测试）
CREATE TABLE IF NOT EXISTS test_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    stock_quantity INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_datasources_status ON datasources(status);
CREATE INDEX IF NOT EXISTS idx_datasources_name ON datasources(name);
CREATE INDEX IF NOT EXISTS idx_datasource_metrics_datasource_id ON datasource_metrics(datasource_id);
CREATE INDEX IF NOT EXISTS idx_health_checks_datasource_id ON datasource_health_checks(datasource_id);
CREATE INDEX IF NOT EXISTS idx_health_checks_check_time ON datasource_health_checks(check_time);
CREATE INDEX IF NOT EXISTS idx_query_cache_datasource_id ON query_cache(datasource_id);
CREATE INDEX IF NOT EXISTS idx_query_cache_expire_time ON query_cache(expire_time);
CREATE INDEX IF NOT EXISTS idx_slow_queries_datasource_id ON slow_queries(datasource_id);
CREATE INDEX IF NOT EXISTS idx_slow_queries_execution_date ON slow_queries(execution_date);
CREATE INDEX IF NOT EXISTS idx_config_history_datasource_id ON datasource_config_history(datasource_id);
CREATE INDEX IF NOT EXISTS idx_test_users_username ON test_users(username);
CREATE INDEX IF NOT EXISTS idx_test_users_status ON test_users(status);
CREATE INDEX IF NOT EXISTS idx_test_orders_user_id ON test_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_test_orders_status ON test_orders(status);
CREATE INDEX IF NOT EXISTS idx_test_products_category ON test_products(category);
CREATE INDEX IF NOT EXISTS idx_test_products_status ON test_products