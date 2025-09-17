# 部署指南

本文档提供了电力交易中心接口平台的详细部署指南，包括环境准备、配置说明、部署步骤和故障排除。

## 目录

- [系统要求](#系统要求)
- [环境准备](#环境准备)
- [配置说明](#配置说明)
- [部署步骤](#部署步骤)
- [Docker部署](#docker部署)
- [监控配置](#监控配置)
- [故障排除](#故障排除)
- [性能优化](#性能优化)

## 系统要求

### 硬件要求

- **CPU**: 4核心以上
- **内存**: 8GB以上（推荐16GB）
- **存储**: 100GB以上可用空间
- **网络**: 稳定的网络连接

### 软件要求

- **操作系统**: Linux (Ubuntu 18.04+, CentOS 7+) 或 macOS
- **Java**: JDK 17或更高版本
- **Node.js**: 18.x或更高版本
- **MySQL**: 8.0或更高版本
- **Redis**: 6.0或更高版本
- **Maven**: 3.8或更高版本
- **Git**: 2.0或更高版本

## 环境准备

### 1. 安装Java

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# 验证安装
java -version
```

### 2. 安装Node.js

```bash
# 使用NodeSource仓库
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证安装
node --version
npm --version
```

### 3. 安装MySQL

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server

# 启动MySQL服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

### 4. 安装Redis

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install redis-server

# 启动Redis服务
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 验证安装
redis-cli ping
```

### 5. 安装Maven

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install maven

# 验证安装
mvn --version
```

## 配置说明

### 1. 数据库配置

创建数据库和用户：

```sql
-- 连接到MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE interface_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'platform'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON interface_platform.* TO 'platform'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 环境变量配置

复制环境变量模板并配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入实际的配置值：

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=interface_platform
DB_USERNAME=platform
DB_PASSWORD=your_secure_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# JWT配置
JWT_SECRET=your_very_secure_jwt_secret_key_here
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=86400

# 其他配置...
```

### 3. 应用配置

每个微服务的配置文件位于 `backend/{service-name}/src/main/resources/application.yml`。

**重要**: 生产环境中，敏感信息应通过环境变量传递，而不是硬编码在配置文件中。

## 部署步骤

### 1. 克隆项目

```bash
git clone <repository-url>
cd interface-platform
```

### 2. 构建后端服务

```bash
# 进入后端目录
cd backend

# 构建所有服务
mvn clean package -DskipTests

# 或者单独构建每个服务
cd gateway-service && mvn clean package -DskipTests
cd ../auth-service && mvn clean package -DskipTests
cd ../user-service && mvn clean package -DskipTests
cd ../interface-service && mvn clean package -DskipTests
cd ../datasource-service && mvn clean package -DskipTests
cd ../notification-service && mvn clean package -DskipTests
cd ../approval-service && mvn clean package -DskipTests
```

### 3. 构建前端应用

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 构建生产版本
npm run build
```

### 4. 启动服务

按以下顺序启动服务：

```bash
# 1. 启动认证服务
cd backend/auth-service
java -jar target/auth-service-*.jar

# 2. 启动用户服务
cd ../user-service
java -jar target/user-service-*.jar

# 3. 启动数据源服务
cd ../datasource-service
java -jar target/datasource-service-*.jar

# 4. 启动接口服务
cd ../interface-service
java -jar target/interface-service-*.jar

# 5. 启动通知服务
cd ../notification-service
java -jar target/notification-service-*.jar

# 6. 启动审批服务
cd ../approval-service
java -jar target/approval-service-*.jar

# 7. 启动网关服务
cd ../gateway-service
java -jar target/gateway-service-*.jar
```

### 5. 部署前端应用

```bash
# 使用Nginx部署前端
sudo cp -r frontend/dist/* /var/www/html/

# 或者使用Node.js服务器
cd frontend
npm run preview
```

## Docker部署

### 1. 创建Docker网络

```bash
docker network create interface-platform-network
```

### 2. 启动基础服务

```bash
# 启动MySQL
docker run -d --name mysql \
  --network interface-platform-network \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=interface_platform \
  -e MYSQL_USER=platform \
  -e MYSQL_PASSWORD=platform123 \
  -p 3306:3306 \
  mysql:8.0

# 启动Redis
docker run -d --name redis \
  --network interface-platform-network \
  -p 6379:6379 \
  redis:7-alpine
```

### 3. 构建应用镜像

```bash
# 构建后端服务镜像
cd backend
docker build -t interface-platform/gateway-service gateway-service/
docker build -t interface-platform/auth-service auth-service/
docker build -t interface-platform/user-service user-service/
docker build -t interface-platform/interface-service interface-service/
docker build -t interface-platform/datasource-service datasource-service/
docker build -t interface-platform/notification-service notification-service/
docker build -t interface-platform/approval-service approval-service/

# 构建前端镜像
cd ../frontend
docker build -t interface-platform/frontend .
```

### 4. 使用Docker Compose

创建 `docker-compose.yml` 文件并启动：

```bash
docker-compose up -d
```

## 监控配置

### 1. 健康检查

所有服务都提供健康检查端点：

- Gateway: http://localhost:8080/actuator/health
- Auth Service: http://localhost:8081/auth-service/actuator/health
- User Service: http://localhost:8086/user-service/actuator/health
- Interface Service: http://localhost:8083/actuator/health
- DataSource Service: http://localhost:8088/datasource-service/actuator/health
- Notification Service: http://localhost:8084/notification/actuator/health
- Approval Service: http://localhost:8085/actuator/health

### 2. Prometheus监控

所有服务都暴露Prometheus指标：

- 指标端点: `/actuator/prometheus`
- 配置Prometheus收集这些指标
- 使用Grafana创建监控仪表板

### 3. 日志监控

- 日志文件位置: `logs/{service-name}.log`
- 建议使用ELK Stack或类似工具进行日志聚合和分析

## 故障排除

### 常见问题

#### 1. 服务启动失败

**问题**: 服务无法启动

**解决方案**:
- 检查Java版本是否正确
- 验证数据库连接配置
- 确认端口未被占用
- 查看日志文件获取详细错误信息

#### 2. 数据库连接失败

**问题**: 无法连接到数据库

**解决方案**:
- 验证MySQL服务是否运行
- 检查数据库用户权限
- 确认网络连接
- 验证连接字符串格式

#### 3. Redis连接失败

**问题**: 无法连接到Redis

**解决方案**:
- 确认Redis服务状态
- 检查Redis配置
- 验证网络连接
- 确认Redis密码设置

#### 4. 前端无法访问后端API

**问题**: 前端请求后端API失败

**解决方案**:
- 检查网关服务状态
- 验证CORS配置
- 确认API路由配置
- 检查防火墙设置

### 日志分析

#### 查看服务日志

```bash
# 查看特定服务日志
tail -f logs/gateway-service.log
tail -f logs/auth-service.log

# 搜索错误日志
grep -i error logs/*.log
grep -i exception logs/*.log
```

#### 常见错误模式

- `Connection refused`: 服务未启动或端口配置错误
- `Access denied`: 数据库权限问题
- `Timeout`: 网络或性能问题
- `ClassNotFoundException`: 依赖包缺失

## 性能优化

### 1. JVM调优

```bash
# 生产环境JVM参数示例
java -Xms2g -Xmx4g -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/heapdump \
     -jar your-service.jar
```

### 2. 数据库优化

- 创建适当的索引
- 配置连接池参数
- 定期分析慢查询
- 优化SQL语句

### 3. Redis优化

- 配置合适的内存策略
- 设置适当的过期时间
- 监控内存使用情况
- 使用Redis集群（如需要）

### 4. 网络优化

- 启用HTTP/2
- 配置适当的超时时间
- 使用连接池
- 启用压缩

## 安全建议

### 1. 网络安全

- 使用防火墙限制访问
- 启用HTTPS
- 配置适当的CORS策略
- 使用VPN或专用网络

### 2. 应用安全

- 定期更新依赖包
- 使用强密码策略
- 启用访问日志
- 实施API限流

### 3. 数据安全

- 加密敏感数据
- 定期备份数据
- 限制数据库访问权限
- 监控异常访问

## 维护指南

### 1. 定期维护任务

- 清理日志文件
- 更新系统补丁
- 备份数据库
- 监控系统性能

### 2. 升级流程

- 在测试环境验证
- 创建数据备份
- 按服务依赖顺序升级
- 验证功能正常

### 3. 监控指标

- CPU和内存使用率
- 数据库连接数
- API响应时间
- 错误率统计

## 联系支持

如果遇到部署问题，请联系技术支持团队：

- 邮箱: support@powertrading.com
- 文档: 查看项目README和API文档
- 问题跟踪: 在项目仓库创建Issue