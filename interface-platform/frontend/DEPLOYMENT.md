# 接口平台前端部署指南

本文档提供了接口平台前端应用的完整部署指南，支持多种部署方式。

## 📦 构建产物说明

### 构建命令
```bash
npm run build
```

### 构建产物结构
```
dist/
├── assets/           # 静态资源文件（JS、CSS）
│   ├── *.js         # JavaScript文件（已压缩）
│   └── *.css        # CSS样式文件（已压缩）
├── favicon.ico      # 网站图标
└── index.html       # 主页面文件
```

### 构建产物特点
- ✅ 代码已压缩和混淆
- ✅ 支持现代浏览器的ES6+语法
- ✅ CSS已提取并压缩
- ✅ 静态资源文件名包含hash，支持长期缓存
- ✅ 支持Vue Router的History模式

## 🚀 部署方式

### 方式一：传统服务器部署

#### 1. 准备服务器环境
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
# 或
sudo dnf install nginx
```

#### 2. 上传构建产物
```bash
# 创建部署目录
sudo mkdir -p /var/www/interface-platform

# 上传dist目录内容到服务器
scp -r dist/* user@your-server:/var/www/interface-platform/

# 设置正确的文件权限
sudo chown -R www-data:www-data /var/www/interface-platform
sudo chmod -R 755 /var/www/interface-platform
```

#### 3. 配置Nginx
```bash
# 复制nginx配置文件
sudo cp nginx.conf /etc/nginx/sites-available/interface-platform

# 创建软链接启用站点
sudo ln -s /etc/nginx/sites-available/interface-platform /etc/nginx/sites-enabled/

# 测试nginx配置
sudo nginx -t

# 重启nginx
sudo systemctl restart nginx
```

#### 4. 配置防火墙（如果需要）
```bash
# Ubuntu/Debian
sudo ufw allow 'Nginx Full'

# CentOS/RHEL
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 方式二：Docker容器部署

#### 1. 构建Docker镜像
```bash
# 在frontend目录下执行
docker build -t interface-platform-frontend .
```

#### 2. 运行容器
```bash
# 直接运行
docker run -d \
  --name interface-platform-frontend \
  -p 80:80 \
  interface-platform-frontend

# 或使用docker-compose
docker-compose up -d
```

#### 3. 验证部署
```bash
# 检查容器状态
docker ps

# 查看容器日志
docker logs interface-platform-frontend
```

### 方式三：CDN + 对象存储部署

#### 1. 上传到对象存储
```bash
# 以阿里云OSS为例
ossutil cp -r dist/ oss://your-bucket/interface-platform/ --update
```

#### 2. 配置CDN
- 设置源站为对象存储地址
- 配置缓存规则：
  - HTML文件：不缓存
  - JS/CSS文件：缓存1年
  - 图片文件：缓存1个月

## ⚙️ 环境配置

### 环境变量配置

在部署前，需要根据实际的后端服务地址修改环境配置：

#### 开发环境（.env）
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USER_API_BASE_URL=http://localhost:8086/user-service
# ... 其他配置
```

#### 生产环境（.env.production）
```env
VITE_API_BASE_URL=https://api.your-domain.com
VITE_USER_API_BASE_URL=https://api.your-domain.com/user-service
# ... 其他配置
```

### 重新构建（如果修改了环境变量）
```bash
# 使用生产环境配置重新构建
npm run build
```

## 🔧 Nginx配置详解

### 基础配置说明
```nginx
# 静态文件服务
root /var/www/interface-platform/dist;
index index.html;

# Vue Router History模式支持
location / {
    try_files $uri $uri/ /index.html;
}

# 静态资源缓存
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### HTTPS配置（推荐）
```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    # ... 其他配置
}
```

### API代理配置（解决跨域）
```nginx
location /api/ {
    proxy_pass http://backend-server:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

## 🔍 部署验证

### 1. 基础功能验证
- [ ] 访问首页是否正常加载
- [ ] 路由跳转是否正常工作
- [ ] 静态资源是否正确加载
- [ ] 控制台是否有错误信息

### 2. 性能验证
```bash
# 使用curl测试响应时间
curl -w "@curl-format.txt" -o /dev/null -s http://your-domain.com

# 使用lighthouse进行性能测试
npx lighthouse http://your-domain.com --output html --output-path ./lighthouse-report.html
```

### 3. 安全验证
```bash
# 检查安全头
curl -I http://your-domain.com

# 使用在线工具检查SSL配置（如果使用HTTPS）
# https://www.ssllabs.com/ssltest/
```

## 🚨 常见问题

### 1. 页面刷新404错误
**原因**：Vue Router使用History模式，需要服务器支持
**解决**：确保nginx配置了`try_files $uri $uri/ /index.html;`

### 2. 静态资源加载失败
**原因**：资源路径配置错误
**解决**：检查vite.config.ts中的base配置

### 3. API请求跨域错误
**原因**：前后端域名不同导致的跨域问题
**解决**：
- 后端配置CORS
- 或使用nginx代理API请求

### 4. 白屏问题
**原因**：JavaScript执行错误或资源加载失败
**解决**：
- 检查浏览器控制台错误信息
- 确认所有静态资源正确加载
- 检查环境变量配置

## 📊 监控和维护

### 1. 日志监控
```bash
# 查看nginx访问日志
sudo tail -f /var/log/nginx/interface-platform-access.log

# 查看nginx错误日志
sudo tail -f /var/log/nginx/interface-platform-error.log
```

### 2. 性能监控
- 使用Google Analytics或其他分析工具
- 配置前端错误监控（如Sentry）
- 定期进行性能测试

### 3. 更新部署
```bash
# 1. 重新构建
npm run build

# 2. 备份当前版本
sudo cp -r /var/www/interface-platform /var/www/interface-platform.backup

# 3. 部署新版本
sudo cp -r dist/* /var/www/interface-platform/

# 4. 重启服务（如果需要）
sudo systemctl reload nginx
```

## 📞 技术支持

如果在部署过程中遇到问题，请：
1. 检查本文档的常见问题部分
2. 查看相关日志文件
3. 联系技术团队获取支持

---

**部署完成后，请访问 `http://your-domain.com` 验证部署是否成功！**