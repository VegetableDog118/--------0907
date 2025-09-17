<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { Document, Setting, User, DataAnalysis } from '@element-plus/icons-vue'

const userStore = useUserStore()

// 检查是否为临时登录（演示模式）
const isDemoMode = computed(() => {
  return userStore.token?.startsWith('temp-token-') || false
})
</script>

<template>
  <div class="home-container">
    <!-- 演示模式提示横幅 -->
    <el-alert
      v-if="isDemoMode"
      title="演示模式"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    >
      <template #default>
        <p style="margin: 0;">
          <strong>当前使用临时登录（演示模式）</strong><br>
          您正在使用系统管理员权限浏览系统功能。如需正常登录，请联系系统管理员配置后端服务。
        </p>
      </template>
    </el-alert>
    
    <!-- 欢迎页面内容 -->
    <div class="welcome-content">
      <div class="welcome-header">
        <h1>欢迎使用电力交易中心接口服务平台</h1>
        <p class="welcome-subtitle">企业级数据接口管理与服务平台</p>
      </div>
      
      <div class="user-info-card">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>用户信息</span>
            </div>
          </template>
          <div class="user-details">
            <p><strong>用户名：</strong>{{ userStore.userInfo?.username || '未知' }}</p>
            <p><strong>真实姓名：</strong>{{ userStore.userInfo?.realName || '未知' }}</p>
            <p><strong>角色：</strong>{{ userStore.userInfo?.role || '未知' }}</p>
            <p><strong>企业：</strong>{{ userStore.userInfo?.companyName || '未知' }}</p>
            <p><strong>部门：</strong>{{ userStore.userInfo?.department || '未知' }}</p>
          </div>
        </el-card>
      </div>
      
      <div class="quick-actions">
        <h2>快速操作</h2>
        <div class="action-grid">
          <el-card shadow="hover" class="action-card" @click="$router.push('/interface/catalog')">
            <div class="action-content">
              <el-icon size="32" color="#409EFF"><Document /></el-icon>
              <h3>接口目录</h3>
              <p>浏览和查看可用的数据接口</p>
            </div>
          </el-card>
          
          <el-card shadow="hover" class="action-card" @click="$router.push('/interface/management')">
            <div class="action-content">
              <el-icon size="32" color="#67C23A"><Setting /></el-icon>
              <h3>接口管理</h3>
              <p>管理和配置数据接口</p>
            </div>
          </el-card>
          
          <el-card shadow="hover" class="action-card" @click="$router.push('/user/center')">
            <div class="action-content">
              <el-icon size="32" color="#E6A23C"><User /></el-icon>
              <h3>用户中心</h3>
              <p>管理个人信息和设置</p>
            </div>
          </el-card>
          
          <el-card shadow="hover" class="action-card" @click="$router.push('/statistics')">
            <div class="action-content">
              <el-icon size="32" color="#F56C6C"><DataAnalysis /></el-icon>
              <h3>数据统计</h3>
              <p>查看系统使用统计信息</p>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  padding: var(--spacing-xl);
  max-width: var(--container-max-width);
  margin: 0 auto;
}

.welcome-content {
  margin-top: var(--spacing-xl);
}

.welcome-header {
  text-align: center;
  margin-bottom: var(--spacing-xxxl);
  padding: var(--spacing-xxxl) 0;
  background: linear-gradient(135deg, var(--primary-extra-light) 0%, var(--bg-white) 100%);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-base);
}

.welcome-header h1 {
  color: var(--text-primary);
  font-size: clamp(var(--font-size-extra-large), 4vw, 2.5rem);
  font-weight: 700;
  margin-bottom: var(--spacing-md);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  color: var(--text-secondary);
  font-size: var(--font-size-medium);
  font-weight: 400;
  margin: 0;
  opacity: 0.9;
}

.user-info-card {
  margin-bottom: var(--spacing-xxxl);
}

.user-info-card .el-card {
  border: 1px solid var(--border-extra-light);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-base);
  transition: var(--transition-base);
  overflow: hidden;
}

.user-info-card .el-card:hover {
  box-shadow: var(--shadow-light);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-large);
  font-weight: 600;
  color: var(--text-primary);
}

.user-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
  padding: var(--spacing-md) 0;
}

.user-details p {
  margin: var(--spacing-sm) 0;
  color: var(--text-regular);
  font-size: var(--font-size-base);
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--bg-extra-light);
  border-radius: var(--border-radius-base);
  border-left: 3px solid var(--primary-color);
  transition: var(--transition-base);
}

.user-details p:hover {
  background: var(--primary-extra-light);
  transform: translateX(4px);
}

.user-details p strong {
  color: var(--text-primary);
  font-weight: 600;
  margin-right: var(--spacing-sm);
  min-width: 80px;
}

.quick-actions h2 {
  color: var(--text-primary);
  font-size: var(--font-size-large);
  font-weight: 600;
  margin-bottom: var(--spacing-xl);
  text-align: center;
  position: relative;
}

.quick-actions h2::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  border-radius: var(--border-radius-round);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-xl);
  margin-top: var(--spacing-xl);
}

.action-card {
  cursor: pointer;
  transition: var(--transition-base);
  border: 1px solid var(--border-extra-light);
  border-radius: var(--border-radius-large);
  overflow: hidden;
  position: relative;
  background: var(--bg-white);
}

.action-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  transform: scaleX(0);
  transition: var(--transition-base);
}

.action-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-light);
  border-color: var(--primary-lighter);
}

.action-card:hover::before {
  transform: scaleX(1);
}

.action-card:hover .action-content {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--primary-extra-light) 100%);
}

.action-content {
  text-align: center;
  padding: var(--spacing-xxxl) var(--spacing-xl);
  transition: var(--transition-base);
  position: relative;
}

.action-content .el-icon {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-lg);
  border-radius: var(--border-radius-circle);
  background: var(--bg-extra-light);
  transition: var(--transition-base);
}

.action-card:hover .action-content .el-icon {
  transform: scale(1.1);
  background: var(--bg-white);
  box-shadow: var(--shadow-base);
}

.action-content h3 {
  color: var(--text-primary);
  font-size: var(--font-size-large);
  font-weight: 600;
  margin: var(--spacing-lg) 0 var(--spacing-md) 0;
  transition: var(--transition-base);
}

.action-card:hover .action-content h3 {
  color: var(--primary-color);
}

.action-content p {
  color: var(--text-secondary);
  margin: 0;
  font-size: var(--font-size-base);
  line-height: var(--line-height-large);
  transition: var(--transition-base);
}

.action-card:hover .action-content p {
  color: var(--text-regular);
}

/* 演示模式横幅样式增强 */
.el-alert {
  border-radius: var(--border-radius-large);
  border: 1px solid var(--warning-color);
  box-shadow: var(--shadow-base);
  margin-bottom: var(--spacing-xl);
}

.el-alert--warning {
  background: linear-gradient(135deg, #fef3c7 0%, #fffbeb 100%);
  border-color: var(--warning-color);
}

.el-alert .el-alert__content {
  padding: var(--spacing-md) 0;
}

.el-alert .el-alert__content p {
  margin: var(--spacing-sm) 0;
  line-height: var(--line-height-large);
}

.el-alert .el-alert__content strong {
  color: var(--warning-color);
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .action-grid {
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: var(--spacing-lg);
  }
}

@media (max-width: 768px) {
  .home-container {
    padding: var(--spacing-lg);
  }
  
  .welcome-header {
    padding: var(--spacing-xl) var(--spacing-lg);
    margin-bottom: var(--spacing-xl);
  }
  
  .welcome-header h1 {
    font-size: var(--font-size-extra-large);
  }
  
  .welcome-subtitle {
    font-size: var(--font-size-base);
  }
  
  .user-details {
    grid-template-columns: 1fr;
    gap: var(--spacing-md);
  }
  
  .action-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-lg);
  }
  
  .action-content {
    padding: var(--spacing-xl) var(--spacing-lg);
  }
  
  .quick-actions h2 {
    font-size: var(--font-size-medium);
  }
}

@media (max-width: 480px) {
  .home-container {
    padding: var(--spacing-md);
  }
  
  .welcome-header {
    padding: var(--spacing-lg) var(--spacing-md);
  }
  
  .welcome-header h1 {
    font-size: var(--font-size-large);
  }
  
  .action-content {
    padding: var(--spacing-lg) var(--spacing-md);
  }
  
  .user-details p {
    padding: var(--spacing-sm);
    font-size: var(--font-size-small);
  }
  
  .user-details p strong {
    min-width: 60px;
    font-size: var(--font-size-small);
  }
}

/* 打印样式 */
@media print {
  .action-card {
    break-inside: avoid;
    box-shadow: none;
    border: 1px solid #ddd;
  }
  
  .welcome-header {
    background: white;
    box-shadow: none;
  }
  
  .welcome-header h1 {
    color: black;
    -webkit-text-fill-color: black;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .action-card {
    border: 2px solid var(--text-primary);
  }
  
  .welcome-header h1 {
    -webkit-text-fill-color: var(--text-primary);
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .action-card,
  .action-content,
  .user-details p,
  .action-content .el-icon,
  .action-content h3,
  .action-content p {
    transition: none;
  }
  
  .action-card:hover {
    transform: none;
  }
}
</style>
