# 企业数据接口服务平台

## 项目简介

企业数据接口服务平台是一个基于微服务架构的数据接口管理和服务平台，为企业提供统一的数据接口管理、用户权限控制、接口调用监控和数据服务等功能。

## 功能特性

### 核心功能
- 🔐 **用户认证与授权** - 基于JWT的用户认证和角色权限管理
- 📊 **接口管理** - 接口注册、配置、版本管理和生命周期管理
- 🔍 **数据源管理** - 多种数据源接入和统一管理
- ✅ **审批流程** - 接口申请、审批和发布流程管理
- 📈 **监控统计** - 接口调用统计、性能监控和报表分析
- 🔔 **通知服务** - 系统通知和消息推送

### 技术特性
- 微服务架构，服务独立部署和扩展
- 统一网关，提供路由、限流、认证等功能
- 前后端分离，支持多端访问
- 容器化部署，支持Docker和Kubernetes
- 高可用设计，支持集群部署

## 技术栈

### 后端技术
- **框架**: Spring Boot 2.7+
- **微服务**: Spring Cloud Gateway
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.0+
- **构建工具**: Maven 3.8+
- **JDK版本**: Java 17+

### 前端技术
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite 4+
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **样式**: Tailwind CSS

### 基础设施
- **容器化**: Docker + Docker Compose
- **网关**: Spring Cloud Gateway
- **服务发现**: 内置服务注册
- **配置管理**: Spring Cloud Config

## 项目结构

```
interface-platform/
├── backend/                    # 后端微服务
│   ├── gateway-service/        # API网关服务
│   ├── user-service/          # 用户管理服务
│   ├── interface-service/     # 接口管理服务
│   ├── approval-service/      # 审批流程服务
│   ├── datasource-service/    # 数据源管理服务
│   ├── notification-service/  # 通知服务
│   ├── auth-service/         # 认证服务
│   ├── common/               # 公共模块
│   └── pom.xml              # Maven父项目配置
├── frontend/                 # 前端应用
│   ├── src/                 # 源代码
│   ├── public/              # 静态资源
│   └── package.json         # 前端依赖配置
├── config/                  # 配置文件
│   ├── mysql/              # MySQL配置
│   └── redis/              # Redis配置
├── database/               # 数据库脚本
│   ├── init.sql           # 初始化脚本
│   └── create_test_users.sql # 测试用户数据
├── docs/                   # 项目文档
└── docker-compose.yml      # Docker编排文件
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+

### 安装步骤

#### 1. 克隆项目
```bash
git clone <repository-url>
cd interface-platform
```

#### 2. 数据库初始化
```bash
# 创建数据库
mysql -u root -p < database/init.sql

# 导入测试数据
mysql -u root -p interface_platform < database/create_test_users.sql
```

#### 3. 启动后端服务
```bash
cd backend

# 启动所有服务
./start-all-services.sh

# 或者单独启动服务
cd gateway-service && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd interface-service && mvn spring-boot:run
```

#### 4. 启动前端应用
```bash
cd frontend
npm install
npm run dev
```

#### 5. 访问应用
- 前端应用: http://localhost:5173
- API网关: http://localhost:8080
- 用户服务: http://localhost:8086
- 接口服务: http://localhost:8083

### Docker部署

```bash
# 使用Docker Compose启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 停止服务
docker-compose down
```

## 测试账号

| 角色 | 用户名 | 密码 | 权限说明 |
|------|--------|------|----------|
| 系统管理员 | admin | admin123 | 系统管理、用户管理、接口管理等全部权限 |
| 数据消费者 | consumer | consumer123 | 接口浏览、接口调用、接口订阅等 |
| 结算部 | finance | finance123 | 接口管理、审批处理、统计查看等 |
| 技术部 | tech | tech123 | 接口开发、数据源管理、接口测试等 |

## API文档

### 主要接口

- **用户认证**: `POST /user-service/api/v1/users/login`
- **用户信息**: `GET /user-service/api/v1/users/profile`
- **接口列表**: `GET /interface-service/api/v1/interfaces`
- **接口详情**: `GET /interface-service/api/v1/interfaces/{id}`

### 接口调用示例

```bash
# 用户登录
curl -X POST http://localhost:8080/user-service/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"admin123"}'

# 获取接口列表
curl -X GET http://localhost:8080/interface-service/api/v1/interfaces \
  -H "Authorization: Bearer <token>"
```

## 开发指南

### 代码规范
- 后端遵循阿里巴巴Java开发手册
- 前端遵循Vue.js官方风格指南
- 使用ESLint和Prettier进行代码格式化
- 提交代码前请运行测试用例

### 分支管理
- `main`: 主分支，用于生产环境
- `develop`: 开发分支，用于集成测试
- `feature/*`: 功能分支，用于新功能开发
- `hotfix/*`: 热修复分支，用于紧急修复

### 提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

## 部署说明

### 生产环境部署

1. **环境准备**
   - 服务器配置: 4核8G内存以上
   - 操作系统: CentOS 7+ / Ubuntu 18+
   - 数据库: MySQL 8.0集群
   - 缓存: Redis 7.0集群

2. **应用部署**
   ```bash
   # 构建应用
   mvn clean package -DskipTests
   
   # 部署到服务器
   docker build -t interface-platform .
   docker run -d --name interface-platform -p 8080:8080 interface-platform
   ```

3. **监控配置**
   - 应用监控: Spring Boot Actuator
   - 日志收集: ELK Stack
   - 性能监控: Prometheus + Grafana

## 故障排除

### 常见问题

1. **服务启动失败**
   - 检查端口是否被占用
   - 确认数据库连接配置
   - 查看服务启动日志

2. **前端页面无法访问**
   - 确认前端服务是否启动
   - 检查网络连接和防火墙设置
   - 验证API接口是否正常

3. **登录失败**
   - 确认用户名密码是否正确
   - 检查用户服务是否正常运行
   - 验证数据库用户数据

### 日志查看
```bash
# 查看服务日志
docker logs interface-platform

# 查看实时日志
tail -f logs/application.log
```

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 项目维护者: [Your Name]
- 邮箱: [your.email@example.com]
- 项目地址: [https://github.com/your-username/interface-platform]

## 更新日志

### v1.0.0 (2024-01-15)
- 🎉 初始版本发布
- ✨ 完成用户认证和权限管理
- ✨ 实现接口管理和数据源管理
- ✨ 添加审批流程和通知服务
- 📝 完善项目文档和部署指南

---

**感谢使用企业数据接口服务平台！**