-- 测试数据
-- 用于单元测试和集成测试的初始数据

-- 插入测试数据源
INSERT INTO datasources (id, name, url, username, password, driver_class_name, status, description, max_pool_size, min_pool_size, connection_timeout, idle_timeout, max_lifetime, created_by, updated_by) VALUES
(1, 'test-mysql-datasource', 'jdbc:mysql://localhost:3306/test_db', 'test_user', 'test_password', 'com.mysql.cj.jdbc.Driver', 'ACTIVE', '测试MySQL数据源', 10, 2, 30000, 600000, 1800000, 'test_admin', 'test_admin'),
(2, 'test-postgresql-datasource', 'jdbc:postgresql://localhost:5432/test_db', 'postgres', 'postgres', 'org.postgresql.Driver', 'ACTIVE', '测试PostgreSQL数据源', 15, 3, 30000, 600000, 1800000, 'test_admin', 'test_admin'),
(3, 'test-oracle-datasource', 'jdbc:oracle:thin:@localhost:1521:XE', 'test_user', 'test_password', 'oracle.jdbc.OracleDriver', 'INACTIVE', '测试Oracle数据源', 8, 1, 30000, 600000, 1800000, 'test_admin', 'test_admin'),
(4, 'test-h2-datasource', 'jdbc:h2:mem:testdb', 'sa', '', 'org.h2.Driver', 'ACTIVE', '测试H2内存数据源', 5, 1, 30000, 600000, 1800000, 'test_admin', 'test_admin');

-- 插入数据源监控指标
INSERT INTO datasource_metrics (datasource_id, connection_success_count, connection_failure_count, query_count, slow_query_count, total_query_time, max_query_time, min_query_time) VALUES
(1, 150, 5, 1000, 25, 500000, 5000, 10),
(2, 200, 2, 800, 15, 400000, 3000, 15),
(3, 50, 20, 200, 10, 150000, 8000, 20),
(4, 100, 1, 500, 5, 250000, 2000, 5);

-- 插入健康检查记录
INSERT INTO datasource_health_checks (datasource_id, status, response_time, error_message, check_time) VALUES
(1, 'HEALTHY', 50, NULL, CURRENT_TIMESTAMP - INTERVAL '1' MINUTE),
(1, 'HEALTHY', 45, NULL, CURRENT_TIMESTAMP - INTERVAL '2' MINUTE),
(2, 'HEALTHY', 60, NULL, CURRENT_TIMESTAMP - INTERVAL '1' MINUTE),
(2, 'HEALTHY', 55, NULL, CURRENT_TIMESTAMP - INTERVAL '2' MINUTE),
(3, 'UNHEALTHY', 0, 'Connection timeout', CURRENT_TIMESTAMP - INTERVAL '1' MINUTE),
(3, 'UNHEALTHY', 0, 'Connection refused', CURRENT_TIMESTAMP - INTERVAL '2' MINUTE),
(4, 'HEALTHY', 30, NULL, CURRENT_TIMESTAMP - INTERVAL '1' MINUTE);

-- 插入查询缓存记录
INSERT INTO query_cache (cache_key, datasource_id, sql_hash, result_data, row_count, cache_time, expire_time, hit_count, last_hit_time) VALUES
('cache_key_1', 1, 'hash_1', '{"columns":["id","name"],"rows":[{"id":1,"name":"Alice"}]}', 1, CURRENT_TIMESTAMP - INTERVAL '10' MINUTE, CURRENT_TIMESTAMP + INTERVAL '50' MINUTE, 5, CURRENT_TIMESTAMP - INTERVAL '1' MINUTE),
('cache_key_2', 2, 'hash_2', '{"columns":["id","email"],"rows":[{"id":1,"email":"test@example.com"}]}', 1, CURRENT_TIMESTAMP - INTERVAL '5' MINUTE, CURRENT_TIMESTAMP + INTERVAL '55' MINUTE, 3, CURRENT_TIMESTAMP - INTERVAL '2' MINUTE),
('cache_key_3', 1, 'hash_3', '{"columns":["count"],"rows":[{"count":100}]}', 1, CURRENT_TIMESTAMP - INTERVAL '15' MINUTE, CURRENT_TIMESTAMP + INTERVAL '45' MINUTE, 10, CURRENT_TIMESTAMP - INTERVAL '3' MINUTE);

-- 插入慢查询记录
INSERT INTO slow_queries (datasource_id, sql_text, execution_time, parameters, execution_date, user_id) VALUES
(1, 'SELECT * FROM large_table WHERE complex_condition = ?', 5000, '{"complex_condition":"value1"}', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 'test_user_1'),
(1, 'SELECT COUNT(*) FROM users u JOIN orders o ON u.id = o.user_id WHERE u.created_at > ?', 3500, '{"created_at":"2023-01-01"}', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'test_user_2'),
(2, 'UPDATE products SET price = price * 1.1 WHERE category = ?', 4200, '{"category":"electronics"}', CURRENT_TIMESTAMP - INTERVAL '3' HOUR, 'test_user_1'),
(3, 'SELECT * FROM audit_log WHERE log_date BETWEEN ? AND ?', 8000, '{"start_date":"2023-01-01","end_date":"2023-12-31"}', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, 'test_user_3');

-- 插入配置变更历史
INSERT INTO datasource_config_history (datasource_id, operation_type, old_config, new_config, change_reason, changed_by, change_time) VALUES
(1, 'CREATE', NULL, '{"name":"test-mysql-datasource","url":"jdbc:mysql://localhost:3306/test_db","maxPoolSize":10}', '创建新数据源', 'test_admin', CURRENT_TIMESTAMP - INTERVAL '1' DAY),
(1, 'UPDATE', '{"maxPoolSize":10}', '{"maxPoolSize":15}', '增加连接池大小以提高性能', 'test_admin', CURRENT_TIMESTAMP - INTERVAL '12' HOUR),
(2, 'CREATE', NULL, '{"name":"test-postgresql-datasource","url":"jdbc:postgresql://localhost:5432/test_db","maxPoolSize":15}', '创建PostgreSQL数据源', 'test_admin', CURRENT_TIMESTAMP - INTERVAL '2' DAY),
(3, 'UPDATE', '{"status":"ACTIVE"}', '{"status":"INACTIVE"}', '数据库维护，暂时停用', 'test_admin', CURRENT_TIMESTAMP - INTERVAL '6' HOUR);

-- 插入测试用户数据
INSERT INTO test_users (id, username, email, full_name, status) VALUES
(1, 'alice', 'alice@example.com', 'Alice Johnson', 'ACTIVE'),
(2, 'bob', 'bob@example.com', 'Bob Smith', 'ACTIVE'),
(3, 'charlie', 'charlie@example.com', 'Charlie Brown', 'INACTIVE'),
(4, 'diana', 'diana@example.com', 'Diana Prince', 'ACTIVE'),
(5, 'eve', 'eve@example.com', 'Eve Wilson', 'ACTIVE');

-- 插入测试订单数据
INSERT INTO test_orders (id, user_id, order_number, total_amount, status) VALUES
(1, 1, 'ORD-2023-001', 299.99, 'COMPLETED'),
(2, 1, 'ORD-2023-002', 149.50, 'PENDING'),
(3, 2, 'ORD-2023-003', 599.00, 'COMPLETED'),
(4, 2, 'ORD-2023-004', 89.99, 'SHIPPED'),
(5, 4, 'ORD-2023-005', 1299.99, 'PROCESSING'),
(6, 4, 'ORD-2023-006', 49.99, 'CANCELLED'),
(7, 5, 'ORD-2023-007', 199.99, 'COMPLETED');

-- 插入测试产品数据
INSERT INTO test_products (id, name, description, price, category, stock_quantity, status) VALUES
(1, 'Laptop Pro 15', '高性能笔记本电脑，适合专业用户', 1299.99, 'Electronics', 50, 'ACTIVE'),
(2, 'Wireless Mouse', '无线光学鼠标，人体工学设计', 29.99, 'Electronics', 200, 'ACTIVE'),
(3, 'Office Chair', '舒适的办公椅，可调节高度', 199.99, 'Furniture', 25, 'ACTIVE'),
(4, 'Coffee Maker', '自动咖啡机，支持多种咖啡类型', 89.99, 'Appliances', 30, 'ACTIVE'),
(5, 'Smartphone X', '最新款智能手机，5G网络支持', 699.99, 'Electronics', 100, 'ACTIVE'),
(6, 'Desk Lamp', 'LED台灯，护眼设计', 39.99, 'Furniture', 75, 'ACTIVE'),
(7, 'Bluetooth Speaker', '便携式蓝牙音箱，高音质', 59.99, 'Electronics', 80, 'ACTIVE'),
(8, 'Gaming Keyboard', '机械键盘，RGB背光', 129.99, 'Electronics', 40, 'ACTIVE'),
(9, 'Water Bottle', '不锈钢保温水瓶', 19.99, 'Accessories', 150, 'ACTIVE'),
(10, 'Tablet Stand', '可调节平板电脑支架', 24.99, 'Accessories', 60, 'DISCONTINUED');

-- 更新时间戳（模拟不同的创建和更新时间）
UPDATE datasources SET 
    created_at = CURRENT_TIMESTAMP - INTERVAL '7' DAY,
    updated_at = CURRENT_TIMESTAMP - INTERVAL '1' DAY
WHERE id = 1;

UPDATE datasources SET 
    created_at = CURRENT_TIMESTAMP - INTERVAL '5' DAY,
    updated_at = CURRENT_TIMESTAMP - INTERVAL '2' DAY
WHERE id = 2;

UPDATE datasources SET 
    created_at = CURRENT_TIMESTAMP - INTERVAL '10' DAY,
    updated_at = CURRENT_TIMESTAMP - INTERVAL '6' HOUR
WHERE id = 3;

UPDATE datasources SET 
    created_at = CURRENT_TIMESTAMP - INTERVAL '3' DAY,
    updated_at = CURRENT_TIMESTAMP - INTERVAL '1' HOUR
WHERE id = 4;