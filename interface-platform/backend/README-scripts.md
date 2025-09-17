# 接口平台后端服务启动脚本使用说明

## 脚本文件

- `start-all-services.sh` - 启动所有微服务
- `stop-all-services.sh` - 停止所有微服务

## 使用方法

### 启动所有服务

```bash
./start-all-services.sh
```

**功能特性：**
- 自动检查Java和Maven环境
- 编译整个项目
- 按依赖顺序启动所有7个微服务
- 实时显示启动进度和状态
- 自动端口冲突检测
- 彩色输出和进度提示
- 生成详细的启动日志

**启动的服务：**
1. Gateway Service (端口 8080) - 网关服务
2. Auth Service (端口 8081) - 认证服务
3. Interface Service (端口 8083) - 接口服务
4. Notification Service (端口 8084) - 通知服务
5. Approval Service (端口 8085) - 审批服务
6. User Service (端口 8087) - 用户服务
7. DataSource Service (端口 8088) - 数据源服务

### 停止所有服务

```bash
./stop-all-services.sh
```

**停止选项：**
- 普通停止：`./stop-all-services.sh`
- 强制停止：`./stop-all-services.sh --force`
- 查看帮助：`./stop-all-services.sh --help`

**功能特性：**
- 优雅停止所有服务
- 自动清理PID文件
- 端口释放检查
- 强制停止选项
- 孤立进程清理

## 日志和进程管理

### 日志文件位置
- 日志目录：`backend/logs/`
- 每个服务的日志：`logs/{service-name}.log`

### PID文件位置
- PID目录：`backend/pids/`
- 每个服务的PID：`pids/{service-name}.pid`

## 常见问题

### 1. 端口冲突
如果遇到端口冲突，脚本会自动跳过已占用的端口。可以手动停止冲突的进程：
```bash
# 查看端口占用
lsof -i:8080

# 停止特定进程
kill <PID>
```

### 2. 服务启动失败
检查对应服务的日志文件：
```bash
tail -f logs/{service-name}.log
```

### 3. 强制停止所有服务
```bash
./stop-all-services.sh --force
```

### 4. 检查服务状态
```bash
# 检查所有端口
for port in 8080 8081 8083 8084 8085 8087 8088; do
  echo "Port $port: $(lsof -ti:$port || echo 'free')"
done
```

## 环境要求

- Java 17+ (GraalVM)
- Maven 3.6+
- 足够的内存（建议8GB+）
- 可用端口：8080, 8081, 8083-8085, 8087-8088

## 网关访问

启动成功后，可以通过网关访问所有服务：
- 网关地址：http://localhost:8080
- 各服务通过网关路由访问

## 注意事项

1. 首次启动可能需要较长时间（Maven依赖下载）
2. 确保数据库服务（MySQL、Redis等）已启动
3. 建议在启动前检查系统资源使用情况
4. 如果修改了代码，重启前建议先停止所有服务

## 故障排除

如果脚本执行遇到问题：

1. 检查脚本权限：`ls -la *.sh`
2. 查看详细错误：`bash -x ./start-all-services.sh`
3. 检查Java环境：`echo $JAVA_HOME`
4. 检查Maven：`mvn -version`

---

**提示：** 使用这些脚本可以大大简化微服务的管理，避免手动逐个启动和停止服务的繁琐操作。