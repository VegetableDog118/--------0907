# Gateway Service - 网关服务

## 概述

网关服务是电力交易中心接口服务平台的统一入口，基于Spring Cloud Gateway实现，提供路由转发、认证鉴权、限流控制、日志监控等核心功能。

## 核心功能

### 1. 动态路由管理
- **静态路由配置**：支持通过配置文件定义基础路由规则
- **动态路由注册**：支持运行时动态添加、修改、删除路由
- **接口路由自动化**：接口上架时自动注册对应路由
- **负载均衡**：集成Spring Cloud LoadBalancer实现服务负载均衡

### 2. 认证鉴权
- **Token验证**：集成auth-service进行JWT Token验证
- **权限校验**：基于用户角色和权限进行访问控制
- **用户信息传递**：将用户信息添加到请求头传递给下游服务
- **白名单机制**：支持配置不需要认证的路径

### 3. 分布式限流
- **Redis限流**：基于Redis实现分布式限流控制
- **多维度限流**：支持按IP、用户、API、用户+API等维度限流
- **滑动窗口算法**：使用Lua脚本实现高性能滑动窗口限流
- **限流信息透明**：在响应头中返回限流状态信息

### 4. 请求监控
- **请求日志**：详细记录请求响应信息
- **性能监控**：监控请求耗时和性能指标
- **慢请求告警**：自动识别和告警慢请求
- **异常记录**：完整记录异常信息便于问题排查

### 5. 路由热更新
- **Nacos集成**：与Nacos配置中心集成实现配置热更新
- **配置监听**：实时监听路由配置变化
- **无缝更新**：支持无重启更新路由配置
- **配置管理**：提供路由配置的增删改查接口

### 6. 统一异常处理
- **全局异常捕获**：统一处理网关层面的异常
- **标准化响应**：返回统一格式的错误响应
- **异常分类**：根据异常类型返回相应的HTTP状态码
- **详细日志**：记录详细的异常信息便于排查

## 技术架构

### 核心依赖
- Spring Cloud Gateway：网关核心框架
- Spring Cloud LoadBalancer：负载均衡
- Nacos Discovery：服务发现
- Nacos Config：配置管理
- Redis：分布式缓存和限流
- WebClient：响应式HTTP客户端

### 主要组件
```
gateway-service/
├── config/
│   ├── GatewayConfig.java          # 网关基础配置
│   └── WebClientConfig.java        # WebClient配置
├── controller/
│   └── RouteController.java        # 路由管理接口
├── filter/
│   ├── AuthGatewayFilterFactory.java      # 认证过滤器
│   ├── RateLimitGatewayFilterFactory.java # 限流过滤器
│   └── LoggingGatewayFilterFactory.java   # 日志过滤器
├── service/
│   ├── DynamicRouteService.java    # 动态路由服务
│   └── RouteRefreshService.java    # 路由热更新服务
├── exception/
│   └── GlobalExceptionHandler.java # 全局异常处理
└── GatewayApplication.java         # 启动类
```

## 配置说明

### 应用配置 (application.yml)
```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
    gateway:
      routes:
        # 静态路由配置
  redis:
    host: localhost
    port: 6379
```

### 路由配置示例
```json
[
  {
    "id": "user-service",
    "uri": "lb://user-service",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/api/v1/users/**"
        }
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {
          "parts": "2"
        }
      }
    ]
  }
]
```

## API接口

### 路由管理接口

#### 添加路由
```http
POST /gateway/routes/add
Content-Type: application/json

{
  "routeId": "test-route",
  "uri": "lb://test-service",
  "path": "/api/test/**",
  "method": "GET",
  "stripPrefix": 2
}
```

#### 删除路由
```http
DELETE /gateway/routes/delete/{routeId}
```

#### 更新路由
```http
PUT /gateway/routes/update
Content-Type: application/json

{
  "routeId": "test-route",
  "uri": "lb://test-service-v2",
  "path": "/api/v2/test/**"
}
```

#### 刷新路由
```http
POST /gateway/routes/refresh
```

#### 添加接口路由
```http
POST /gateway/routes/interface
Content-Type: application/json

{
  "interfaceId": "12345",
  "path": "/api/dynamic/data",
  "method": "GET",
  "targetService": "interface-service"
}
```

## 过滤器配置

### 认证过滤器
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: protected-route
          uri: lb://protected-service
          predicates:
            - Path=/api/protected/**
          filters:
            - name: Auth
              args:
                enabled: true
```

### 限流过滤器
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limited-route
          uri: lb://api-service
          predicates:
            - Path=/api/public/**
          filters:
            - name: RateLimit
              args:
                limit: 100
                window: 60
                keyType: ip
```

### 日志过滤器
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: logged-route
          uri: lb://api-service
          predicates:
            - Path=/api/**
          filters:
            - name: Logging
              args:
                logRequest: true
                logResponse: true
                logPerformance: true
```

## 监控指标

### Actuator端点
- `/actuator/health`：健康检查
- `/actuator/gateway/routes`：查看当前路由
- `/actuator/gateway/filters`：查看可用过滤器

### 性能指标
- 请求总数
- 平均响应时间
- 错误率统计
- 限流触发次数
- 慢请求统计

## 部署说明

### 环境要求
- JDK 11+
- Redis 5.0+
- Nacos 2.0+

### 启动步骤
1. 启动Redis服务
2. 启动Nacos服务
3. 配置Nacos中的路由配置
4. 启动网关服务

### Docker部署
```dockerfile
FROM openjdk:11-jre-slim
COPY target/gateway-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 故障排查

### 常见问题
1. **路由不生效**：检查路由配置和服务注册状态
2. **认证失败**：检查Token格式和auth-service连通性
3. **限流异常**：检查Redis连接和Lua脚本执行
4. **性能问题**：查看慢请求日志和性能指标

### 日志级别
```yaml
logging:
  level:
    com.powertrading.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## 扩展开发

### 自定义过滤器
```java
@Component
public class CustomGatewayFilterFactory extends AbstractGatewayFilterFactory<CustomGatewayFilterFactory.Config> {
    // 实现自定义过滤器逻辑
}
```

### 自定义断言
```java
@Component
public class CustomRoutePredicateFactory extends AbstractRoutePredicateFactory<CustomRoutePredicateFactory.Config> {
    // 实现自定义断言逻辑
}
```

## 版本信息
- 版本：1.0.0
- 更新时间：2024-01-15
- 维护团队：电力交易中心技术团队