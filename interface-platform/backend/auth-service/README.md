# 认证服务 (Auth Service)

PowerTrading接口平台认证服务，提供统一的认证和授权功能。

## 目录

- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [API接口文档](#api接口文档)
- [配置说明](#配置说明)
- [部署指南](#部署指南)
- [测试指南](#测试指南)
- [监控和运维](#监控和运维)
- [常见问题](#常见问题)

## 功能特性

### 核心功能

- **JWT Token管理**：生成、验证、刷新JWT Token
- **API密钥认证**：基于AppId的API调用认证
- **多重认证支持**：支持JWT、API密钥、混合认证模式
- **安全策略**：Token过期策略、黑名单管理、账户锁定
- **权限缓存**：基于Redis的高性能权限缓存机制
- **审计日志**：完整的认证和授权操作审计
- **网关集成**：与API网关无缝集成

### 安全特性

- **密码强度检查**：支持自定义密码策略
- **登录失败保护**：自动账户锁定和IP黑名单
- **可疑活动检测**：实时监控和告警
- **Token黑名单**：支持Token撤销和黑名单管理
- **IP白名单**：支持IP访问控制
- **双因子认证**：可选的2FA支持

## 技术架构

### 技术栈

- **框架**：Spring Boot 2.7.18
- **安全**：Spring Security + JWT
- **缓存**：Redis
- **数据库**：MySQL 8.0
- **文档**：SpringDoc OpenAPI 3
- **监控**：Spring Boot Actuator + Prometheus
- **测试**：JUnit 5 + Mockito + TestContainers

### 架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │────│  Auth Service   │────│     Redis       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                │
                        ┌─────────────────┐
                        │     MySQL       │
                        └─────────────────┘
```

## 快速开始

### 环境要求

- Java 11+
- Maven 3.6+
- Redis 6.0+
- MySQL 8.0+

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd auth-service
```

2. **配置数据库**
```sql
CREATE DATABASE interface_platform;
```

3. **启动Redis**
```bash
redis-server
```

4. **配置应用**
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interface_platform
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

5. **启动应用**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

6. **访问文档**
- Swagger UI: http://localhost:8081/auth-service/swagger-ui.html
- API Docs: http://localhost:8081/auth-service/v3/api-docs

## API接口文档

### 认证接口

#### 1. 统一认证

**POST** `/api/auth/authenticate`

统一的认证接口，支持JWT Token、API密钥、混合认证模式。

**请求参数：**
```json
{
  "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "apiKey": "app-key-123456",
  "authMode": "auto",
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "requestPath": "/api/users",
  "requestMethod": "GET",
  "strictMode": false
}
```

**参数说明：**
- `jwtToken`: JWT Token（可选）
- `apiKey`: API密钥（可选）
- `authMode`: 认证模式（auto/jwt/apikey/mixed）
- `clientIp`: 客户端IP地址
- `userAgent`: 用户代理字符串
- `requestPath`: 请求路径（用于权限检查）
- `requestMethod`: 请求方法（用于权限检查）
- `strictMode`: 严格模式（默认false）

**响应示例：**
```json
{
  "success": true,
  "userId": "user-001",
  "username": "john.doe",
  "companyName": "PowerTrading Ltd",
  "appId": "app-001",
  "appName": "Trading App",
  "roles": ["user", "trader"],
  "permissions": ["trade:read", "trade:write"],
  "authType": "JWT",
  "expirationTime": "2024-01-15T15:30:00",
  "remainingTime": 3600,
  "errorMessage": null
}
```

#### 2. 快速认证

**GET** `/api/auth/quick`

快速验证Token有效性，用于高频调用场景。

**请求参数：**
- `token`: JWT Token（必需）

**响应示例：**
```json
{
  "valid": true,
  "userId": "user-001",
  "expiresIn": 3600
}
```

### 权限检查接口

#### 3. 权限检查

**POST** `/api/auth/check-permission`

检查用户是否具有指定权限。

**请求参数：**
```json
{
  "permissions": ["user:read", "user:write"],
  "requiredPermission": "user:read"
}
```

**响应示例：**
```json
{
  "hasPermission": true
}
```

#### 4. API权限检查

**POST** `/api/auth/check-api-permission`

检查用户是否有权限访问指定API接口。

**请求参数：**
```json
{
  "permissions": ["user:read", "user:write"],
  "endpoint": "/api/users",
  "method": "GET"
}
```

**响应示例：**
```json
{
  "hasPermission": true,
  "requiredPermission": "user:read"
}
```

### 统计和监控接口

#### 5. 认证统计

**GET** `/api/auth/statistics`

获取认证服务的统计信息。

**响应示例：**
```json
{
  "totalRequests": 10000,
  "successRequests": 9500,
  "failedRequests": 500,
  "jwtRequests": 6000,
  "apikeyRequests": 3000,
  "mixedRequests": 1000,
  "successRate": 95.0,
  "avgResponseTime": 50
}
```

#### 6. 用户审计日志

**GET** `/api/auth/audit-logs/{userId}`

获取指定用户的审计日志。

**请求参数：**
- `userId`: 用户ID（路径参数）
- `limit`: 返回记录数限制（查询参数，默认100）

**响应示例：**
```json
[
  {
    "timestamp": "2024-01-15T10:30:00",
    "action": "LOGIN_SUCCESS",
    "userId": "user-001",
    "username": "john.doe",
    "clientIp": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "details": "JWT authentication successful"
  }
]
```

#### 7. 每日统计

**GET** `/api/auth/daily-statistics`

获取指定日期的统计信息。

**请求参数：**
- `date`: 日期（YYYY-MM-DD格式）

**响应示例：**
```json
{
  "date": "2024-01-15",
  "totalLogins": 1000,
  "successfulLogins": 950,
  "failedLogins": 50,
  "uniqueUsers": 800,
  "peakHour": "14:00",
  "avgSessionDuration": 3600
}
```

#### 8. 健康检查

**GET** `/api/auth/health`

服务健康状态检查。

**响应示例：**
```json
{
  "status": "UP",
  "service": "Multi-Auth Service",
  "version": "2.0.0",
  "timestamp": "2024-01-15T10:30:00",
  "dependencies": {
    "redis": "UP",
    "database": "UP"
  }
}
```

### 安全策略接口

#### 9. 锁定账户

**POST** `/api/security/lock-account/{userId}`

手动锁定用户账户。

**请求参数：**
```json
{
  "reason": "Suspicious activity detected"
}
```

#### 10. 解锁账户

**POST** `/api/security/unlock-account/{userId}`

解锁用户账户。

#### 11. IP黑名单管理

**POST** `/api/security/blacklist-ip`

添加IP到黑名单。

**请求参数：**
```json
{
  "ip": "192.168.1.100",
  "reason": "Multiple failed login attempts"
}
```

**DELETE** `/api/security/blacklist-ip/{ip}`

从黑名单移除IP。

### 权限缓存接口

#### 12. 缓存用户权限

**POST** `/api/cache/user-permissions/{userId}`

缓存用户权限信息。

**请求参数：**
```json
{
  "permissions": ["user:read", "user:write", "user:delete"]
}
```

#### 13. 获取缓存统计

**GET** `/api/cache/statistics`

获取缓存命中率等统计信息。

**响应示例：**
```json
{
  "userPermissionsHitRate": 95.5,
  "userRolesHitRate": 92.3,
  "apiPermissionsHitRate": 98.1,
  "overallHitRate": 95.3,
  "totalHits": 95000,
  "totalMisses": 5000
}
```

## 配置说明

### 核心配置

```yaml
# JWT配置
jwt:
  secret: your-secret-key
  expiration: 3600 # 1小时
  refresh-expiration: 86400 # 24小时
  issuer: PowerTrading-Interface-Platform

# 安全策略配置
security:
  policy:
    max-login-attempts: 5
    account-lock-duration: 30 # 分钟
    password:
      min-length: 8
      require-uppercase: true
      require-lowercase: true
      require-numbers: true
      require-symbols: true

# 缓存配置
cache:
  redis:
    default-ttl: 1800 # 30分钟
    user-permissions-ttl: 1800
    role-permissions-ttl: 3600
```

### 环境配置

- **开发环境**：`application-dev.yml`
- **测试环境**：`application-test.yml`
- **生产环境**：`application-prod.yml`

## 部署指南

### Docker部署

1. **构建镜像**
```bash
mvn clean package
docker build -t powertrading/auth-service:2.0.0 .
```

2. **运行容器**
```bash
docker run -d \
  --name auth-service \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/interface_platform \
  -e SPRING_REDIS_HOST=redis \
  powertrading/auth-service:2.0.0
```

### Kubernetes部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: powertrading/auth-service:2.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mysql://mysql:3306/interface_platform"
        - name: SPRING_REDIS_HOST
          value: "redis"
```

## 测试指南

### 单元测试

```bash
# 运行单元测试
mvn test

# 生成测试报告
mvn test jacoco:report
```

### 集成测试

```bash
# 运行集成测试
mvn verify -P integration-test

# 使用TestContainers运行完整集成测试
mvn verify -P integration-test -Dtest.containers=true
```

### 性能测试

```bash
# 使用JMeter进行性能测试
jmeter -n -t auth-service-performance-test.jmx -l results.jtl
```

## 监控和运维

### 健康检查

- **应用健康**：`GET /auth-service/actuator/health`
- **详细信息**：`GET /auth-service/actuator/info`
- **指标数据**：`GET /auth-service/actuator/metrics`
- **Prometheus**：`GET /auth-service/actuator/prometheus`

### 日志配置

```yaml
logging:
  level:
    com.powertrading.auth: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/auth-service.log
```

### 关键指标

- **认证成功率**：`auth_success_rate`
- **平均响应时间**：`auth_response_time`
- **缓存命中率**：`cache_hit_rate`
- **活跃用户数**：`active_users`
- **Token生成速率**：`token_generation_rate`

## 常见问题

### Q1: JWT Token过期如何处理？

A: 使用刷新Token机制：
1. 客户端检测到Token过期（401响应）
2. 使用refresh token调用刷新接口
3. 获取新的access token和refresh token
4. 重试原始请求

### Q2: 如何实现单点登录（SSO）？

A: 通过共享JWT Token实现：
1. 用户在一个应用登录后获得JWT Token
2. 其他应用验证同一个JWT Token
3. 通过统一的用户信息和权限管理

### Q3: 如何处理高并发场景？

A: 采用以下策略：
1. Redis缓存减少数据库查询
2. JWT无状态认证减少服务器负担
3. 异步处理审计日志
4. 水平扩展多个服务实例

### Q4: 安全最佳实践？

A: 建议遵循：
1. 使用HTTPS传输
2. 定期轮换JWT密钥
3. 设置合理的Token过期时间
4. 启用账户锁定和IP黑名单
5. 监控异常登录行为

### Q5: 如何备份和恢复？

A: 备份策略：
1. 数据库定期备份
2. Redis数据持久化
3. 配置文件版本控制
4. 定期测试恢复流程

## 联系方式

- **开发团队**：PowerTrading Development Team
- **邮箱**：dev@powertrading.com
- **文档**：[内部Wiki链接]
- **问题反馈**：[Issue Tracker链接]

## 版本历史

- **v2.0.0** (2024-01-15)
  - 重构认证架构
  - 新增多重认证支持
  - 优化缓存机制
  - 完善安全策略

- **v1.0.0** (2023-12-01)
  - 初始版本发布
  - 基础JWT认证功能
  - API密钥认证

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。