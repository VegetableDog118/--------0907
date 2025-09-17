# 电力交易中心接口服务平台

## 项目简介

电力交易中心接口服务平台是一个专业的数据接口管理和服务平台，为电力交易参与者提供标准化的数据接口服务。平台整合电力交易核心数据（检修计划、机组状态、系统负荷等），通过RESTful API对外提供统一的数据服务。

## 技术架构

### 前端技术栈
- **框架**: Vue.js 3.x + TypeScript
- **路由**: Vue Router 4.x
- **状态管理**: Pinia
- **UI组件库**: Element Plus
- **构建工具**: Vite
- **HTTP客户端**: Axios
- **样式**: SCSS + CSS3

### 后端技术栈
- **框架**: Spring Boot 2.7.x + Spring Cloud 2021.x
- **网关**: Spring Cloud Gateway
- **服务注册与发现**: Nacos
- **配置中心**: Nacos Config
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x
- **连接池**: HikariCP
- **ORM框架**: MyBatis Plus
- **安全框架**: Spring Security + JWT
- **API文档**: SpringDoc OpenAPI 3

### 运维技术栈
- **容器化**: Docker + Docker Compose
- **负载均衡**: Nginx
- **监控**: Prometheus + Grafana（待实现）
- **日志**: ELK Stack（待实现）

## 项目结构

```
interface-platform/
├── frontend/                    # 前端Vue.js应用
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── components/         # 通用组件
│   │   ├── stores/             # Pinia状态管理
│   │   ├── router/             # 路由配置
│   │   └── assets/             # 静态资源
│   ├── package.json
│   └── vite.config.ts
├── backend/                     # 后端微服务
│   ├── common/                 # 公共模块
│   ├── gateway-service/        # 网关服务
│   ├── user-service/           # 用户管理服务
│   ├── interface-service/      # 接口管理服务
│   ├── approval-service/       # 审批服务
│   ├── datasource-service/     # 数据源管理服务
│   ├── auth-service/           # 认证服务
│   ├── notification-service/   # 通知服务
│   └── pom.xml                # Maven父项目配置
├── database/                   # 数据库脚本
│   └── init.sql               # 初始化脚本
├── config/                     # 配置文件
│   ├── nginx/                 # Nginx配置
│   ├── mysql/                 # MySQL配置
│   └── redis/                 # Redis配置
├── docs/                       # 项目文档
├── docker-compose.yml          # Docker编排文件
└── README.md                   # 项目说明文档
```

## 核心功能模块

### 1. 用户管理模块
- 企业用户注册与认证
- 基于角色的权限控制(RBAC)
- 用户信息管理
- API密钥管理

### 2. 接口管理模块
- 动态接口生成（基于数据库表结构）
- 接口配置与参数设置
- 接口生命周期管理（未上架→已上架→已下架）
- 批量操作支持

### 3. 订阅审批模块
- 接口订阅申请
- 一级审批流程
- 批量审批处理
- 申请历史管理

### 4. 接口目录模块
- 分类展示接口
- 接口搜索与筛选
- 批量订阅功能
- 接口详情查看

### 5. 数据源管理模块
- 多数据源配置
- 连接池管理
- 动态SQL执行
- 查询结果缓存

### 6. 网关服务模块
- 统一路由转发
- 认证鉴权
- 限流控制
- 动态路由管理

## 快速开始

### 环境要求

- Node.js 18+
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- MySQL 8.0+
- Redis 7.x+

### 本地开发环境搭建

#### 1. 克隆项目
```bash
git clone <repository-url>
cd interface-platform
```

#### 2. 启动基础服务（使用Docker Compose）
```bash
# 启动MySQL、Redis、Nacos等基础服务
docker-compose up -d mysql redis nacos

# 等待服务启动完成（约2-3分钟）
docker-compose logs -f nacos
```

#### 3. 前端开发
```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173
```

#### 4. 后端开发
```bash
cd backend

# 编译项目
mvn clean compile

# 启动网关服务
cd gateway-service
mvn spring-boot:run

# 启动其他服务（在新的终端窗口中）
cd ../user-service
mvn spring-boot:run

# 重复启动其他服务...
```

### 生产环境部署

#### 使用Docker Compose一键部署
```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 访问地址
- 前端应用: http://localhost
- 网关服务: http://localhost:8080
- Nacos控制台: http://localhost:8848/nacos（用户名/密码: nacos/nacos）

## 开发指南

### 前端开发

#### 页面开发规范
1. 使用Vue 3 Composition API
2. 统一使用TypeScript
3. 遵循Element Plus设计规范
4. 组件命名采用PascalCase
5. 文件命名采用kebab-case

#### 状态管理
- 使用Pinia进行状态管理
- 按模块划分store
- 异步操作统一处理

#### 路由配置
- 支持路由守卫
- 基于角色的权限控制
- 动态路由加载

### 后端开发

#### 项目结构规范
```
service-name/
├── src/main/java/com/powertrading/service/
│   ├── controller/         # 控制器层
│   ├── service/           # 业务逻辑层
│   ├── mapper/            # 数据访问层
│   ├── entity/            # 实体类
│   ├── dto/               # 数据传输对象
│   ├── vo/                # 视图对象
│   ├── config/            # 配置类
│   └── ServiceApplication.java
└── src/main/resources/
    ├── mapper/            # MyBatis映射文件
    ├── application.yml    # 配置文件
    └── bootstrap.yml      # 启动配置
```

#### 开发规范
1. 统一使用RESTful API设计
2. 统一异常处理
3. 统一响应格式
4. 参数校验使用Bean Validation
5. 数据库操作使用MyBatis Plus

#### API文档
- 使用SpringDoc OpenAPI 3生成API文档
- 访问地址: http://localhost:8080/swagger-ui.html

## 数据库设计

### 核心表结构

1. **用户管理**
   - users: 用户表
   - roles: 角色表
   - permissions: 权限表
   - role_permissions: 角色权限关联表

2. **接口管理**
   - interfaces: 接口表
   - interface_categories: 接口分类表
   - interface_parameters: 接口参数表

3. **数据源管理**
   - data_sources: 数据源表

4. **订阅管理**
   - subscription_applications: 订阅申请表
   - user_interface_subscriptions: 用户接口订阅表

5. **系统日志**
   - operation_logs: 操作日志表
   - api_call_logs: API调用日志表

6. **通知管理**
   - notifications: 通知表
   - notification_templates: 通知模板表

### 数据库初始化
```bash
# 执行初始化脚本
mysql -u root -p < database/init.sql
```

## 配置说明

### 环境配置

项目支持多环境配置：
- `dev`: 开发环境
- `test`: 测试环境
- `prod`: 生产环境
- `docker`: Docker环境

### 关键配置项

#### Nacos配置
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: interface-platform
      config:
        server-addr: localhost:8848
        namespace: interface-platform
        file-extension: yml
```

#### 数据库配置
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/interface_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: platform
    password: platform123
```

#### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000ms
```

## 测试

### 前端测试
```bash
cd frontend

# 运行单元测试
npm run test:unit

# 运行端到端测试
npm run test:e2e
```

### 后端测试
```bash
cd backend

# 运行所有测试
mvn test

# 运行特定服务测试
cd user-service
mvn test
```

## 监控与日志

### 应用监控
- Spring Boot Actuator健康检查
- 访问地址: http://localhost:8080/actuator/health

### 日志配置
- 统一日志格式
- 按服务分离日志文件
- 支持日志级别动态调整

## 常见问题

### Q: 启动时Nacos连接失败？
A: 确保Nacos服务已启动，检查网络连接和配置地址。

### Q: 前端页面空白？
A: 检查后端服务是否正常启动，查看浏览器控制台错误信息。

### Q: 数据库连接失败？
A: 检查MySQL服务状态，确认数据库用户权限和连接参数。

### Q: Redis连接超时？
A: 检查Redis服务状态，确认网络连接和配置参数。

## 贡献指南

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 版本历史

- **v1.0.0** (2024-01-15)
  - 初始版本发布
  - 实现核心功能模块
  - 支持Docker部署

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目维护者: 技术团队
- 邮箱: tech@powertrading.com
- 项目地址: [GitHub Repository](https://github.com/powertrading/interface-platform)