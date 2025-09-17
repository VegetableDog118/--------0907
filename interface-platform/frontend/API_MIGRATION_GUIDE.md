# 前端API切换指南

## 概述

本文档记录了将前端服务从Mock API切换到真实后端API的完整过程和相关配置更改。

## 更改内容

### 1. API服务文件修改

#### 修改的文件：
- `src/api/interface.ts` - 接口相关API
- `src/api/user.ts` - 用户相关API
- `src/api/approval.ts` - 审批相关API
- `src/utils/request.ts` - 请求工具配置

#### 主要更改：
- 移除了所有 `import.meta.env.DEV` 条件判断
- 移除了Mock API的动态导入和调用
- 直接使用真实的后端API接口
- 更新了axios实例配置，不再区分开发和生产环境

### 2. 环境配置更新

#### 文件：`.env`

```env
# API 基础配置 - 后端微服务地址
VITE_API_BASE_URL=http://localhost:8080
VITE_USER_API_BASE_URL=http://localhost:8086
VITE_INTERFACE_API_BASE_URL=http://localhost:8087
VITE_APPROVAL_API_BASE_URL=http://localhost:8088
VITE_DATASOURCE_API_BASE_URL=http://localhost:8089
VITE_AUTH_API_BASE_URL=http://localhost:8085
VITE_NOTIFICATION_API_BASE_URL=http://localhost:8090
```

#### 更改说明：
- 更新了各个微服务的端口配置
- 移除了用户服务URL中的 `/user-service` 路径
- 为每个服务分配了独立的端口

### 3. 应用入口文件修改

#### 文件：`src/main.ts`

- 移除了Mock API服务的初始化代码
- 简化了应用启动流程
- 不再区分开发和生产环境的初始化逻辑

### 4. Mock服务处理

#### 保留的Mock文件（仅供参考）：
- `src/mock/` 目录下的所有文件保持不变
- 这些文件不再被主应用使用，但保留作为开发参考

## 后端服务要求

### 必需的后端服务：

1. **网关服务** - `http://localhost:8080`
2. **用户服务** - `http://localhost:8086`
3. **接口服务** - `http://localhost:8087`
4. **审批服务** - `http://localhost:8088`
5. **数据源服务** - `http://localhost:8089`
6. **认证服务** - `http://localhost:8085`
7. **通知服务** - `http://localhost:8090`

### API接口要求：

所有后端服务需要实现以下接口规范：

#### 响应格式：
```json
{
  "code": 200,
  "data": {},
  "message": "success"
}
```

#### 错误处理：
- 401: 未授权
- 403: 权限不足
- 404: 资源不存在
- 500: 服务器错误

## 测试验证

### 启动前端服务：
```bash
cd interface-platform/frontend
npm run dev
```

### 验证项目：
1. 访问 `http://localhost:5173`
2. 检查浏览器控制台是否有网络错误
3. 测试登录功能
4. 测试接口列表加载
5. 测试接口管理功能

## 回滚方案

如需回滚到Mock API模式，可以：

1. 恢复 `src/main.ts` 中的Mock服务初始化代码
2. 在各API文件中恢复 `import.meta.env.DEV` 条件判断
3. 恢复 `src/utils/request.ts` 中的条件判断逻辑

## 注意事项

1. **CORS配置**：确保后端服务配置了正确的CORS策略
2. **认证Token**：确保后端服务支持Bearer Token认证
3. **接口路径**：确保后端接口路径与前端调用一致
4. **数据格式**：确保后端返回的数据格式与前端期望一致

## 更新日期

- 创建时间：2024年1月15日
- 最后更新：2024年1月15日
- 更新人：系统管理员