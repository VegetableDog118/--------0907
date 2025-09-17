# 接口平台前端部署包

本目录包含了接口平台前端应用的完整部署包，支持多种部署方式。

## 📁 文件说明

```
frontend/
├── dist/                    # 构建产物（生产环境静态文件）
├── .env.production         # 生产环境配置文件
├── nginx.conf              # Nginx配置文件
├── Dockerfile              # Docker镜像构建文件
├── docker-compose.yml      # Docker Compose配置
├── deploy.sh               # 自动部署脚本
├── DEPLOYMENT.md           # 详细部署文档
└── README-DEPLOY.md        # 本文件
```

## 🚀 快速部署

### 方式一：使用自动部署脚本（推荐）

```bash
# 生产环境部署
./deploy.sh production

# 测试环境部署
./deploy.sh staging
```

### 方式二：Docker快速部署

```bash
# 构建并运行
docker-compose up -d

# 或者手动构建
docker build -t interface-platform-frontend .
docker run -d -p 80:80 --name frontend interface-platform-frontend
```

### 方式三：传统服务器部署

1. 将 `dist/` 目录内容上传到服务器
2. 配置Nginx（参考 `nginx.conf`）
3. 启动服务

## ⚙️ 配置说明

### 环境变量配置

部署前请根据实际情况修改 `.env.production` 文件中的API地址：

```env
# 修改为实际的后端服务地址
VITE_API_BASE_URL=https://your-api-domain.com
VITE_USER_API_BASE_URL=https://your-api-domain.com/user-service
# ... 其他配置
```

### Nginx配置

`nginx.conf` 文件包含了完整的Nginx配置，包括：
- 静态文件服务
- Vue Router History模式支持
- 缓存策略
- 安全头设置
- API代理（可选）

## 📋 部署检查清单

部署完成后，请检查以下项目：

- [ ] 网站首页能正常访问
- [ ] 路由跳转功能正常
- [ ] 静态资源加载正常
- [ ] API接口调用正常
- [ ] 浏览器控制台无错误
- [ ] 移动端适配正常

## 🔧 故障排除

### 常见问题

1. **页面刷新404错误**
   - 检查Nginx配置中的 `try_files` 设置
   - 确保支持Vue Router的History模式

2. **静态资源加载失败**
   - 检查文件路径和权限
   - 确认Nginx配置正确

3. **API请求失败**
   - 检查环境变量配置
   - 确认后端服务可访问
   - 检查跨域配置

### 日志查看

```bash
# Nginx日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# Docker容器日志
docker logs interface-platform-frontend
```

## 📞 技术支持

如需详细的部署说明，请参考 `DEPLOYMENT.md` 文件。

如遇到问题，请联系技术团队获取支持。

---

**部署成功后，访问 `http://your-domain.com` 即可使用接口平台！**