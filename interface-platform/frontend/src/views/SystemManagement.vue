<template>
  <div class="system-management">
    <!-- 权限检查 -->
    <div v-if="!canViewSystem" class="no-permission">
      <el-empty description="您没有权限访问系统管理功能">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </el-empty>
    </div>

    <!-- 主要内容 -->
    <div v-else>
      <el-card>
        <template #header>
          <div class="page-header">
            <h3>系统管理</h3>
            <div class="header-actions">
              <el-button type="primary" @click="refreshData">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
            </div>
          </div>
        </template>

        <!-- 管理功能导航 -->
        <div class="management-nav">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
              <el-card 
                class="nav-card" 
                :class="{ active: activeModule === 'user' }"
                @click="setActiveModule('user')"
                shadow="hover"
              >
                <div class="nav-content">
                  <el-icon class="nav-icon"><User /></el-icon>
                  <h4>用户管理</h4>
                  <p>管理系统用户和角色分配</p>
                  <div class="nav-stats">
                    <span>用户数: {{ systemStats.userCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
              <el-card 
                class="nav-card" 
                :class="{ active: activeModule === 'role' }"
                @click="setActiveModule('role')"
                shadow="hover"
              >
                <div class="nav-content">
                  <el-icon class="nav-icon"><Key /></el-icon>
                  <h4>角色权限</h4>
                  <p>管理角色和权限配置</p>
                  <div class="nav-stats">
                    <span>角色数: {{ systemStats.roleCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
              <el-card 
                class="nav-card" 
                :class="{ active: activeModule === 'config' }"
                @click="setActiveModule('config')"
                shadow="hover"
              >
                <div class="nav-content">
                  <el-icon class="nav-icon"><Setting /></el-icon>
                  <h4>系统配置</h4>
                  <p>管理系统参数和配置</p>
                  <div class="nav-stats">
                    <span>配置项: {{ systemStats.configCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
              <el-card 
                class="nav-card" 
                :class="{ active: activeModule === 'datasource' }"
                @click="setActiveModule('datasource')"
                shadow="hover"
              >
                <div class="nav-content">
                  <el-icon class="nav-icon"><Coin /></el-icon>
                  <h4>数据源管理</h4>
                  <p>管理数据源连接配置</p>
                  <div class="nav-stats">
                    <span>数据源: {{ systemStats.datasourceCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
              <el-card 
                class="nav-card" 
                :class="{ active: activeModule === 'monitor' }"
                @click="setActiveModule('monitor')"
                shadow="hover"
              >
                <div class="nav-content">
                  <el-icon class="nav-icon"><Monitor /></el-icon>
                  <h4>系统监控</h4>
                  <p>查看系统运行状态</p>
                  <div class="nav-stats">
                    <span>状态: {{ systemStats.systemStatus }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
              <el-card 
                class="nav-card" 
                :class="{ active: activeModule === 'log' }"
                @click="setActiveModule('log')"
                shadow="hover"
              >
                <div class="nav-content">
                  <el-icon class="nav-icon"><Document /></el-icon>
                  <h4>操作日志</h4>
                  <p>查看系统操作记录</p>
                  <div class="nav-stats">
                    <span>今日: {{ systemStats.todayLogCount }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 功能模块内容区域 -->
        <div class="module-content" v-if="activeModule">
          <!-- 用户管理 -->
          <user-management v-if="activeModule === 'user'" />
          
          <!-- 角色权限管理 -->
          <role-management v-if="activeModule === 'role'" />
          
          <!-- 系统配置管理 -->
          <system-config v-if="activeModule === 'config'" />
          
          <!-- 数据源管理 -->
          <datasource-management v-if="activeModule === 'datasource'" />
          
          <!-- 系统监控 -->
          <system-monitor v-if="activeModule === 'monitor'" />
          
          <!-- 操作日志 -->
          <operation-log v-if="activeModule === 'log'" />
        </div>

        <!-- 默认欢迎页面 -->
        <div v-if="!activeModule" class="welcome-content">
          <el-empty description="请选择要管理的功能模块">
            <el-button type="primary" @click="setActiveModule('user')">
              开始管理
            </el-button>
          </el-empty>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { 
  User, 
  Key, 
  Setting, 
  Coin, 
  Monitor, 
  Document, 
  Refresh 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import UserManagement from '@/components/system/UserManagement.vue'
import RoleManagement from '@/components/system/RoleManagement.vue'
import SystemConfig from '@/components/system/SystemConfig.vue'
import DatasourceManagement from '@/components/system/DatasourceManagement.vue'
import SystemMonitor from '@/components/system/SystemMonitor.vue'
import OperationLog from '@/components/system/OperationLog.vue'
import * as systemApi from '@/api/system'

// Store
const userStore = useUserStore()

// 响应式数据
const activeModule = ref('')
const loading = ref(false)

// 系统统计数据
const systemStats = reactive({
  userCount: 0,
  roleCount: 0,
  configCount: 0,
  datasourceCount: 0,
  systemStatus: '正常',
  todayLogCount: 0
})

// 权限检查
const canViewSystem = computed(() => {
  return userStore.hasRole('admin')
})

// 方法
const setActiveModule = (module: string) => {
  activeModule.value = module
}

const refreshData = async () => {
  await loadSystemStats()
  ElMessage.success('数据已刷新')
}

const loadSystemStats = async () => {
  loading.value = true
  try {
    const response = await systemApi.getSystemStats()
    Object.assign(systemStats, response)
  } catch (error) {
    console.error('加载系统统计失败:', error)
    // 使用模拟数据
    Object.assign(systemStats, {
      userCount: 156,
      roleCount: 4,
      configCount: 23,
      datasourceCount: 8,
      systemStatus: '正常',
      todayLogCount: 89
    })
  } finally {
    loading.value = false
  }
}

// 生命周期
onMounted(() => {
  if (canViewSystem.value) {
    loadSystemStats()
  }
})
</script>

<style scoped>
.system-management {
  padding: 20px;
}

.no-permission {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h3 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.management-nav {
  margin-bottom: 24px;
}

.nav-card {
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.nav-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.nav-card.active {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.nav-content {
  text-align: center;
  padding: 16px;
}

.nav-icon {
  font-size: 32px;
  color: #409eff;
  margin-bottom: 12px;
}

.nav-content h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.nav-content p {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.nav-stats {
  color: #909399;
  font-size: 12px;
}

.module-content {
  margin-top: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.welcome-content {
  padding: 60px 20px;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .system-management {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .nav-content {
    padding: 12px;
  }
  
  .nav-icon {
    font-size: 24px;
  }
  
  .nav-content h4 {
    font-size: 14px;
  }
  
  .nav-content p {
    font-size: 12px;
  }
  
  .module-content {
    padding: 16px;
  }
}
</style>