<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

// 检查是否需要显示导航栏
const showNavigation = computed(() => {
  const hideNavRoutes = ['/login', '/register', '/404', '/403']
  return !hideNavRoutes.includes(route.path) && userStore.isLoggedIn
})

// 处理登出
const handleLogout = async () => {
  try {
    await userStore.performLogout()
    ElMessage.success('已成功登出')
  } catch (error) {
    console.error('登出失败:', error)
    ElMessage.error('登出失败')
  }
}

// 初始化
onMounted(async () => {
  try {
    // 从本地存储恢复登录状态
    userStore.initFromStorage()
    
    // 如果有token，验证其有效性
    if (userStore.token && userStore.isLoggedIn) {
      try {
        await userStore.checkTokenExpiry()
      } catch (error) {
        console.warn('Token验证失败，将重新登录:', error)
        userStore.logout()
      }
    }
  } catch (error) {
    console.error('应用初始化失败:', error)
  }
})
</script>

<template>
  <div id="app">
    <!-- 导航栏 -->
    <el-container v-if="showNavigation">
      <el-header class="app-header">
        <div class="header-left">
          <img src="@/assets/logo.svg" alt="Logo" class="logo" />
          <span class="app-title">电力交易中心接口服务平台</span>
        </div>
        
        <el-menu
          mode="horizontal"
          :default-active="route.path"
          class="header-menu"
          router
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/interface/catalog">接口目录</el-menu-item>
          <el-menu-item index="/interface/management" v-if="userStore.hasRole('tech') || userStore.hasRole('settlement')">接口管理</el-menu-item>
          <el-menu-item index="/application/approval" v-if="userStore.hasRole('settlement')">申请审批</el-menu-item>
          <el-menu-item index="/statistics" v-if="userStore.hasRole('admin')">数据统计</el-menu-item>
          <el-menu-item index="/user/center">用户中心</el-menu-item>
          <el-menu-item index="/system/management" v-if="userStore.hasRole('admin')">系统管理</el-menu-item>
        </el-menu>
        
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32">
                {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userDisplayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/user/center')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="app-main">
        <RouterView />
      </el-main>
    </el-container>
    
    <!-- 无导航栏页面 -->
    <div v-else class="no-nav-container">
      <RouterView />
    </div>
  </div>
</template>

<style>
/* App.vue 组件样式 */
#app {
  min-height: 100vh;
  font-family: var(--font-family);
  color: var(--text-primary);
  background-color: var(--bg-page);
}

/* 头部导航样式 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-extra-light);
  box-shadow: var(--shadow-lighter);
  padding: 0 var(--spacing-xl);
  height: var(--header-height) !important;
  position: sticky;
  top: 0;
  z-index: var(--z-index-sticky);
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0; /* 防止flex项目溢出 */
}

.logo {
  width: 32px;
  height: 32px;
  margin-right: var(--spacing-md);
  flex-shrink: 0;
}

.app-title {
  font-size: var(--font-size-large);
  font-weight: 600;
  color: var(--text-primary);
  margin-right: var(--spacing-xxxl);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-menu {
  flex: 1;
  border-bottom: none;
  min-width: 0; /* 防止菜单溢出 */
}

.header-menu .el-menu-item {
  font-weight: 500;
  transition: var(--transition-base);
  border-radius: var(--border-radius-base);
  margin: 0 var(--spacing-xs);
}

.header-menu .el-menu-item:hover {
  background-color: var(--bg-extra-light);
  color: var(--primary-color);
}

.header-menu .el-menu-item.is-active {
  background-color: var(--primary-extra-light);
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.header-menu .el-sub-menu__title {
  font-weight: 500;
  transition: var(--transition-base);
  border-radius: var(--border-radius-base);
  margin: 0 var(--spacing-xs);
}

.header-menu .el-sub-menu__title:hover {
  background-color: var(--bg-extra-light);
  color: var(--primary-color);
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius-base);
  transition: var(--transition-base);
  min-width: 0;
}

.user-info:hover {
  background-color: var(--bg-light);
}

.username {
  margin: 0 var(--spacing-sm);
  color: var(--text-primary);
  font-size: var(--font-size-base);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

/* 主内容区域样式 */
.app-main {
  padding: var(--spacing-xl);
  background-color: var(--bg-page);
  min-height: calc(100vh - var(--header-height));
  overflow-x: auto;
}

.no-nav-container {
  min-height: 100vh;
  background-color: var(--bg-page);
}

/* Element Plus 组件样式覆盖 */
.el-menu--horizontal {
  border-bottom: none;
}

.el-menu--horizontal > .el-menu-item {
  border-bottom: 2px solid transparent;
  transition: var(--transition-base);
}

.el-menu--horizontal > .el-menu-item.is-active {
  border-bottom-color: var(--primary-color);
  color: var(--primary-color);
  background-color: var(--primary-extra-light);
}

.el-menu--horizontal > .el-sub-menu {
  border-bottom: 2px solid transparent;
}

.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title {
  border-bottom-color: var(--primary-color);
  color: var(--primary-color);
}

/* 下拉菜单样式 */
.el-dropdown-menu {
  border: 1px solid var(--border-extra-light);
  border-radius: var(--border-radius-base);
  box-shadow: var(--shadow-light);
  padding: var(--spacing-xs) 0;
}

.el-dropdown-menu__item {
  padding: var(--spacing-sm) var(--spacing-lg);
  transition: var(--transition-base);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.el-dropdown-menu__item:hover {
  background-color: var(--bg-extra-light);
  color: var(--primary-color);
}

.el-dropdown-menu__item.is-divided {
  border-top: 1px solid var(--border-extra-light);
  margin-top: var(--spacing-xs);
  padding-top: var(--spacing-md);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .app-title {
    margin-right: var(--spacing-xl);
  }
  
  .header-menu .el-menu-item {
    padding: 0 var(--spacing-md);
  }
}

@media (max-width: 992px) {
  .app-header {
    padding: 0 var(--spacing-lg);
  }
  
  .app-title {
    font-size: var(--font-size-medium);
    margin-right: var(--spacing-lg);
  }
  
  .header-menu {
    display: none; /* 在平板上隐藏菜单，可以考虑添加移动端菜单 */
  }
}

@media (max-width: 768px) {
  .app-header {
    padding: 0 var(--spacing-md);
  }
  
  .app-main {
    padding: var(--spacing-lg);
  }
  
  .app-title {
    font-size: var(--font-size-base);
    margin-right: var(--spacing-md);
    max-width: 150px;
  }
  
  .username {
    display: none; /* 在移动端隐藏用户名 */
  }
}

@media (max-width: 480px) {
  .app-header {
    padding: 0 var(--spacing-sm);
  }
  
  .app-main {
    padding: var(--spacing-md);
  }
  
  .logo {
    width: 24px;
    height: 24px;
    margin-right: var(--spacing-sm);
  }
  
  .app-title {
    font-size: var(--font-size-small);
    margin-right: var(--spacing-sm);
    max-width: 100px;
  }
}

/* 打印样式 */
@media print {
  .app-header {
    display: none;
  }
  
  .app-main {
    padding: 0;
    background: white;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .app-header {
    border-bottom: 2px solid var(--text-primary);
  }
  
  .header-menu .el-menu-item.is-active {
    border-bottom-width: 3px;
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .header-menu .el-menu-item,
  .user-info,
  .el-dropdown-menu__item {
    transition: none;
  }
}
</style>
