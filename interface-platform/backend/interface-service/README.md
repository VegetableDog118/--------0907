# Interface Service - 接口管理服务

## 概述

Interface Service是接口平台的核心业务模块，负责接口的全生命周期管理，包括接口创建、配置、发布、监控和维护等功能。

## 功能特性

### 1. 接口生成四步骤向导
- **数据源选择**: 支持多种数据源类型选择
- **接口配置**: 配置接口基本信息和业务逻辑
- **参数设置**: 灵活的参数配置和验证规则
- **预览确认**: 实时预览生成的接口和SQL

### 2. 接口配置和参数管理
- 支持标准参数模板
- 动态参数验证
- 参数类型转换
- 参数默认值设置

### 3. 接口状态管理
- **未上架(DRAFT)**: 草稿状态，可编辑
- **已上架(PUBLISHED)**: 发布状态，对外提供服务
- **已下架(OFFLINE)**: 下线状态，停止服务

### 4. 批量操作功能
- 批量上架接口
- 批量下架接口
- 批量删除接口
- 批量复制接口
- 批量更新分类

### 5. 动态路由注册
- 与Gateway Service集成
- 自动路由注册和注销
- 路径规范化生成
- 负载均衡支持

### 6. SQL模板生成和执行
- 智能SQL模板生成
- 高级SQL模板支持
- SQL优化建议
- 统计分析SQL模板

### 7. 接口分类管理
- 分层分类结构
- 分类权限控制
- 分类统计信息

### 8. 搜索筛选分页功能
- 全文搜索
- 多维度筛选
- 高级搜索
- 搜索建议
- 热门关键词

### 9. 数据源集成
- 与DataSource Service深度集成
- 支持多种数据库类型
- 数据源连接测试
- 表结构自动获取

## 技术架构

### 技术栈
- **框架**: Spring Boot 2.7+
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **安全**: Spring Security
- **测试**: JUnit 5, Mockito
- **文档**: Swagger/OpenAPI 3

### 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Interface Service                        │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer                                           │
│  ┌─────────────────┐ ┌─────────────────┐ ┌──────────────┐   │
│  │ Interface       │ │ Search          │ │ Batch        │   │
│  │ Management      │ │ Controller      │ │ Operation    │   │
│  │ Controller      │ │                 │ │ Controller   │   │
│  └─────────────────┘ └─────────────────┘ └──────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  ┌─────────────────┐ ┌─────────────────┐ ┌──────────────┐   │
│  │ Interface       │ │ Interface       │ │ Interface    │   │
│  │ Management      │ │ Search          │ │ Status       │   │
│  │ Service         │ │ Service         │ │ Service      │   │
│  └─────────────────┘ └─────────────────┘ └──────────────┘   │
│  ┌─────────────────┐ ┌─────────────────┐ ┌──────────────┐   │
│  │ Interface       │ │ Interface       │ │ DataSource   │   │
│  │ Category        │ │ Execution       │ │ Integration  │   │
│  │ Service         │ │ Service         │ │ Service      │   │
│  └─────────────────┘ └─────────────────┘ └──────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer                                           │
│  ┌─────────────────┐ ┌─────────────────┐                   │
│  │ Interface Info  │ │ Interface       │                   │
│  │ Repository      │ │ Category        │                   │
│  │                 │ │ Repository      │                   │
│  └─────────────────┘ └─────────────────┘                   │
├─────────────────────────────────────────────────────────────┤
│  Utils Layer                                                │
│  ┌─────────────────┐ ┌─────────────────┐                   │
│  │ Interface Path  │ │ SQL Template    │                   │
│  │ Generator       │ │ Generator       │                   │
│  └─────────────────┘ └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/powertrading/interfaces/
│   │       ├── controller/          # 控制器层
│   │       │   ├── InterfaceManagementController.java
│   │       │   ├── InterfaceSearchController.java
│   │       │   ├── InterfaceStatusController.java
│   │       │   ├── InterfaceExecutionController.java
│   │       │   ├── InterfaceBatchOperationController.java
│   │       │   └── DataSourceIntegrationController.java
│   │       ├── service/             # 服务层
│   │       │   ├── InterfaceManagementService.java
│   │       │   ├── InterfaceSearchService.java
│   │       │   ├── InterfaceStatusService.java
│   │       │   ├── InterfaceExecutionService.java
│   │       │   ├── InterfaceCategoryService.java
│   │       │   ├── InterfaceBatchOperationService.java
│   │       │   └── DataSourceIntegrationService.java
│   │       ├── repository/          # 数据访问层
│   │       │   ├── InterfaceInfoRepository.java
│   │       │   └── InterfaceCategoryRepository.java
│   │       ├── entity/              # 实体类
│   │       │   ├── InterfaceInfo.java
│   │       │   └── InterfaceCategory.java
│   │       ├── dto/                 # 数据传输对象
│   │       │   └── InterfaceGenerationRequest.java
│   │       ├── utils/               # 工具类
│   │       │   ├── InterfacePathGenerator.java
│   │       │   └── SqlTemplateGenerator.java
│   │       └── config/              # 配置类
│   │           └── InterfaceServiceConfig.java
│   └── resources/
│       ├── application.yml          # 应用配置
│       └── db/migration/            # 数据库迁移脚本
└── test/
    └── java/
        └── com/powertrading/interfaces/
            ├── service/             # 服务层测试
            │   ├── InterfaceManagementServiceTest.java
            │   └── InterfaceSearchServiceTest.java
            └── controller/          # 控制器集成测试
                └── InterfaceManagementControllerIntegrationTest.java
```

## API文档

### 接口管理相关API

#### 1. 获取接口列表
```http
GET /api/interfaces?page=1&size=20
```

#### 2. 获取接口详情
```http
GET /api/interfaces/{id}
```

#### 3. 更新接口配置
```http
PUT /api/interfaces/{id}/configuration
Content-Type: application/json

{
  "interfaceName": "用户查询接口",
  "description": "根据条件查询用户信息",
  "categoryId": 1,
  "dataSourceId": 1,
  "tableName": "users",
  "parameters": [
    {
      "paramName": "userId",
      "paramType": "integer",
      "required": true,
      "description": "用户ID"
    }
  ]
}
```

#### 4. 删除接口
```http
DELETE /api/interfaces/{id}
```

### 接口搜索相关API

#### 1. 搜索接口
```http
POST /api/interfaces/search
Content-Type: application/json

{
  "keyword": "用户",
  "categoryId": 1,
  "status": "PUBLISHED",
  "page": 1,
  "size": 20
}
```

#### 2. 获取搜索建议
```http
GET /api/interfaces/search/suggestions?keyword=用户
```

### 接口状态管理API

#### 1. 上架接口
```http
POST /api/interfaces/{id}/publish
```

#### 2. 下架接口
```http
POST /api/interfaces/{id}/offline
```

### 批量操作API

#### 1. 批量上架
```http
POST /api/interfaces/batch/publish
Content-Type: application/json

{
  "interfaceIds": [1, 2, 3],
  "publishTime": "2024-01-15T10:00:00"
}
```

## 配置说明

### application.yml
```yaml
spring:
  application:
    name: interface-service
  datasource:
    url: jdbc:mysql://localhost:3306/interface_platform
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

server:
  port: 8082

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.powertrading.interfaces: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 自定义配置
interface:
  service:
    # 接口路径前缀
    path-prefix: /api/data
    # 默认分页大小
    default-page-size: 20
    # 最大分页大小
    max-page-size: 100
    # SQL模板缓存时间(秒)
    sql-template-cache-ttl: 3600
    # 接口执行超时时间(毫秒)
    execution-timeout: 30000
```

## 部署指南

### Docker部署

#### 1. 构建镜像
```bash
# 在项目根目录执行
docker build -t interface-service:latest .
```

#### 2. 运行容器
```bash
docker run -d \
  --name interface-service \
  -p 8082:8082 \
  -e DB_HOST=mysql \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  -e REDIS_HOST=redis \
  -e RABBITMQ_HOST=rabbitmq \
  interface-service:latest
```

### Docker Compose部署

```yaml
version: '3.8'
services:
  interface-service:
    build: .
    ports:
      - "8082:8082"
    environment:
      - DB_HOST=mysql
      - DB_USERNAME=root
      - DB_PASSWORD=password
      - REDIS_HOST=redis
      - RABBITMQ_HOST=rabbitmq
    depends_on:
      - mysql
      - redis
      - rabbitmq
    networks:
      - interface-platform

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=password
      - MYSQL_DATABASE=interface_platform
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - interface-platform

  redis:
    image: redis:7-alpine
    networks:
      - interface-platform

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=password
    ports:
      - "15672:15672"
    networks:
      - interface-platform

volumes:
  mysql_data:

networks:
  interface-platform:
    driver: bridge
```

### Kubernetes部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: interface-service
  labels:
    app: interface-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: interface-service
  template:
    metadata:
      labels:
        app: interface-service
    spec:
      containers:
      - name: interface-service
        image: interface-service:latest
        ports:
        - containerPort: 8082
        env:
        - name: DB_HOST
          value: "mysql-service"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        - name: REDIS_HOST
          value: "redis-service"
        - name: RABBITMQ_HOST
          value: "rabbitmq-service"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: interface-service
spec:
  selector:
    app: interface-service
  ports:
  - protocol: TCP
    port: 8082
    targetPort: 8082
  type: ClusterIP
```

## 开发指南

### 环境要求
- JDK 11+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd interface-service
```

2. **配置数据库**
```sql
CREATE DATABASE interface_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **启动依赖服务**
```bash
# 启动MySQL、Redis、RabbitMQ
docker-compose up -d mysql redis rabbitmq
```

4. **运行应用**
```bash
mvn spring-boot:run
```

5. **运行测试**
```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# 生成测试报告
mvn jacoco:report
```

### 代码规范

1. **命名规范**
   - 类名使用PascalCase
   - 方法名和变量名使用camelCase
   - 常量使用UPPER_SNAKE_CASE
   - 包名使用小写字母

2. **注释规范**
   - 所有public方法必须有JavaDoc注释
   - 复杂业务逻辑需要添加行内注释
   - 类级别注释包含作者和创建时间

3. **异常处理**
   - 使用自定义异常类
   - 异常信息要清晰明确
   - 记录异常日志

## 监控和运维

### 健康检查
- **健康检查端点**: `/actuator/health`
- **指标端点**: `/actuator/metrics`
- **Prometheus端点**: `/actuator/prometheus`

### 日志管理
- 使用SLF4J + Logback
- 支持日志级别动态调整
- 结构化日志输出
- 日志文件轮转

### 性能监控
- JVM指标监控
- 数据库连接池监控
- Redis连接监控
- 接口响应时间监控

## 常见问题

### Q1: 接口创建失败
**A**: 检查数据源连接是否正常，表名是否存在，参数配置是否正确。

### Q2: 搜索功能响应慢
**A**: 检查数据库索引是否创建，考虑启用搜索结果缓存。

### Q3: 批量操作超时
**A**: 调整批量操作的批次大小，增加超时时间配置。

### Q4: 内存使用过高
**A**: 检查缓存配置，调整JVM堆内存大小，优化SQL查询。

## 版本历史

- **v1.0.0** (2024-01-15)
  - 初始版本发布
  - 实现基础接口管理功能
  - 支持接口生成向导
  - 实现搜索和筛选功能

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交代码变更
4. 创建Pull Request
5. 代码审查通过后合并

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 联系方式

- 项目维护者: PowerTrading Team
- 邮箱: dev@powertrading.com
- 文档: https://docs.powertrading.com/interface-service