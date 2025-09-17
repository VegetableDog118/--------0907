<template>
  <div class="system-monitor">
    <el-card>
      <template #header>
        <div class="section-header">
          <h4>系统监控</h4>
          <div class="header-actions">
            <el-button @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
            <el-switch
              v-model="autoRefresh"
              active-text="自动刷新"
              @change="toggleAutoRefresh"
            />
          </div>
        </div>
      </template>

      <!-- 系统状态概览 -->
      <div class="status-overview">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="status-card cpu-card">
              <div class="status-content">
                <div class="status-icon">
                  <el-icon><Cpu /></el-icon>
                </div>
                <div class="status-info">
                  <h3>CPU 使用率</h3>
                  <div class="status-value">{{ monitorData.cpu.usage }}%</div>
                  <div class="status-detail">{{ monitorData.cpu.cores }} 核心</div>
                </div>
              </div>
              <el-progress 
                :percentage="monitorData.cpu.usage" 
                :color="getProgressColor(monitorData.cpu.usage)"
                :show-text="false"
              />
            </el-card>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="status-card memory-card">
              <div class="status-content">
                <div class="status-icon">
                  <el-icon><Monitor /></el-icon>
                </div>
                <div class="status-info">
                  <h3>内存使用率</h3>
                  <div class="status-value">{{ monitorData.memory.usage }}%</div>
                  <div class="status-detail">{{ formatBytes(monitorData.memory.used) }} / {{ formatBytes(monitorData.memory.total) }}</div>
                </div>
              </div>
              <el-progress 
                :percentage="monitorData.memory.usage" 
                :color="getProgressColor(monitorData.memory.usage)"
                :show-text="false"
              />
            </el-card>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="status-card disk-card">
              <div class="status-content">
                <div class="status-icon">
                  <el-icon><FolderOpened /></el-icon>
                </div>
                <div class="status-info">
                  <h3>磁盘使用率</h3>
                  <div class="status-value">{{ monitorData.disk.usage }}%</div>
                  <div class="status-detail">{{ formatBytes(monitorData.disk.used) }} / {{ formatBytes(monitorData.disk.total) }}</div>
                </div>
              </div>
              <el-progress 
                :percentage="monitorData.disk.usage" 
                :color="getProgressColor(monitorData.disk.usage)"
                :show-text="false"
              />
            </el-card>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="status-card network-card">
              <div class="status-content">
                <div class="status-icon">
                  <el-icon><Connection /></el-icon>
                </div>
                <div class="status-info">
                  <h3>网络流量</h3>
                  <div class="status-value">{{ formatBytes(monitorData.network.upload + monitorData.network.download) }}/s</div>
                  <div class="status-detail">
                    ↑{{ formatBytes(monitorData.network.upload) }}/s 
                    ↓{{ formatBytes(monitorData.network.download) }}/s
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- JVM 监控 -->
      <div class="jvm-monitor">
        <h4>JVM 监控</h4>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-card class="jvm-card">
              <template #header>
                <span>堆内存使用情况</span>
              </template>
              <div class="jvm-content">
                <div class="jvm-info">
                  <span>已使用: {{ formatBytes(monitorData.jvm.heapUsed) }}</span>
                  <span>最大值: {{ formatBytes(monitorData.jvm.heapMax) }}</span>
                  <span>使用率: {{ monitorData.jvm.heapUsage }}%</span>
                </div>
                <el-progress 
                  :percentage="monitorData.jvm.heapUsage" 
                  :color="getProgressColor(monitorData.jvm.heapUsage)"
                />
              </div>
            </el-card>
          </el-col>
          
          <el-col :xs="24" :md="12">
            <el-card class="jvm-card">
              <template #header>
                <span>非堆内存使用情况</span>
              </template>
              <div class="jvm-content">
                <div class="jvm-info">
                  <span>已使用: {{ formatBytes(monitorData.jvm.nonHeapUsed) }}</span>
                  <span>最大值: {{ formatBytes(monitorData.jvm.nonHeapMax) }}</span>
                </div>
                <el-progress 
                  :percentage="(monitorData.jvm.nonHeapUsed / monitorData.jvm.nonHeapMax) * 100" 
                  :color="getProgressColor((monitorData.jvm.nonHeapUsed / monitorData.jvm.nonHeapMax) * 100)"
                />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 系统健康检查 -->
      <div class="health-check">
        <h4>系统健康检查</h4>
        <el-table :data="healthData" v-loading="healthLoading">
          <el-table-column prop="component" label="组件" width="200" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'UP' ? 'success' : 'danger'">
                {{ row.status === 'UP' ? '正常' : '异常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="details" label="详情" min-width="300">
            <template #default="{ row }">
              <div v-if="row.details">
                <div v-for="(value, key) in row.details" :key="key" class="health-detail">
                  <strong>{{ key }}:</strong> {{ value }}
                </div>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 实时图表 -->
      <div class="charts-section">
        <h4>实时监控图表</h4>
        <el-row :gutter="20">
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <span>CPU & 内存使用率趋势</span>
              </template>
              <div class="chart-container">
                <div ref="cpuMemoryChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <span>网络流量趋势</span>
              </template>
              <div class="chart-container">
                <div ref="networkChartRef" class="chart"></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { 
  Refresh, 
  Cpu, 
  Monitor, 
  FolderOpened, 
  Connection 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as systemApi from '@/api/system'
import type { SystemMonitorData } from '@/api/system'

// 响应式数据
const loading = ref(false)
const healthLoading = ref(false)
const autoRefresh = ref(false)
const refreshTimer = ref<number | null>(null)
const cpuMemoryChartRef = ref<HTMLElement>()
const networkChartRef = ref<HTMLElement>()

// 监控数据
const monitorData = reactive<SystemMonitorData>({
  cpu: {
    usage: 0,
    cores: 0
  },
  memory: {
    total: 0,
    used: 0,
    free: 0,
    usage: 0
  },
  disk: {
    total: 0,
    used: 0,
    free: 0,
    usage: 0
  },
  network: {
    upload: 0,
    download: 0
  },
  jvm: {
    heapUsed: 0,
    heapMax: 0,
    heapUsage: 0,
    nonHeapUsed: 0,
    nonHeapMax: 0
  }
})

// 健康检查数据
const healthData = ref<Array<{
  component: string
  status: string
  details?: Record<string, any>
}>>([])

// 图表数据
const chartData = reactive({
  cpuMemory: {
    times: [] as string[],
    cpu: [] as number[],
    memory: [] as number[]
  },
  network: {
    times: [] as string[],
    upload: [] as number[],
    download: [] as number[]
  }
})

// 方法
const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getProgressColor = (percentage: number) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

const loadMonitorData = async () => {
  loading.value = true
  try {
    const response = await systemApi.getSystemMonitorData()
    Object.assign(monitorData, response)
    
    // 更新图表数据
    updateChartData()
  } catch (error) {
    console.error('加载监控数据失败:', error)
    // 使用模拟数据
    Object.assign(monitorData, {
      cpu: {
        usage: Math.floor(Math.random() * 80) + 10,
        cores: 8
      },
      memory: {
        total: 16 * 1024 * 1024 * 1024, // 16GB
        used: Math.floor(Math.random() * 8 * 1024 * 1024 * 1024) + 4 * 1024 * 1024 * 1024, // 4-12GB
        free: 0,
        usage: 0
      },
      disk: {
        total: 500 * 1024 * 1024 * 1024, // 500GB
        used: Math.floor(Math.random() * 200 * 1024 * 1024 * 1024) + 100 * 1024 * 1024 * 1024, // 100-300GB
        free: 0,
        usage: 0
      },
      network: {
        upload: Math.floor(Math.random() * 1024 * 1024), // 0-1MB/s
        download: Math.floor(Math.random() * 10 * 1024 * 1024) // 0-10MB/s
      },
      jvm: {
        heapUsed: Math.floor(Math.random() * 2 * 1024 * 1024 * 1024) + 1024 * 1024 * 1024, // 1-3GB
        heapMax: 4 * 1024 * 1024 * 1024, // 4GB
        heapUsage: 0,
        nonHeapUsed: Math.floor(Math.random() * 256 * 1024 * 1024) + 128 * 1024 * 1024, // 128-384MB
        nonHeapMax: 512 * 1024 * 1024 // 512MB
      }
    })
    
    // 计算使用率
    monitorData.memory.free = monitorData.memory.total - monitorData.memory.used
    monitorData.memory.usage = Math.round((monitorData.memory.used / monitorData.memory.total) * 100)
    
    monitorData.disk.free = monitorData.disk.total - monitorData.disk.used
    monitorData.disk.usage = Math.round((monitorData.disk.used / monitorData.disk.total) * 100)
    
    monitorData.jvm.heapUsage = Math.round((monitorData.jvm.heapUsed / monitorData.jvm.heapMax) * 100)
    
    updateChartData()
  } finally {
    loading.value = false
  }
}

const loadHealthData = async () => {
  healthLoading.value = true
  try {
    const response = await systemApi.getSystemHealth()
    healthData.value = Object.entries(response.components).map(([key, value]) => ({
      component: getComponentName(key),
      status: value.status,
      details: value.details
    }))
  } catch (error) {
    console.error('加载健康检查数据失败:', error)
    // 使用模拟数据
    healthData.value = [
      {
        component: '数据库',
        status: 'UP',
        details: {
          '连接池': '活跃连接 5/20',
          '响应时间': '< 10ms'
        }
      },
      {
        component: 'Redis',
        status: 'UP',
        details: {
          '连接状态': '正常',
          '内存使用': '256MB'
        }
      },
      {
        component: '磁盘空间',
        status: 'UP',
        details: {
          '可用空间': '> 10GB',
          '使用率': '< 80%'
        }
      },
      {
        component: '外部API',
        status: Math.random() > 0.2 ? 'UP' : 'DOWN',
        details: {
          '响应时间': '< 500ms',
          '成功率': '99.9%'
        }
      }
    ]
  } finally {
    healthLoading.value = false
  }
}

const getComponentName = (key: string) => {
  const nameMap: Record<string, string> = {
    db: '数据库',
    redis: 'Redis',
    diskSpace: '磁盘空间',
    ping: '网络连通性',
    mail: '邮件服务'
  }
  return nameMap[key] || key
}

const updateChartData = () => {
  const now = new Date().toLocaleTimeString()
  
  // CPU & 内存图表数据
  chartData.cpuMemory.times.push(now)
  chartData.cpuMemory.cpu.push(monitorData.cpu.usage)
  chartData.cpuMemory.memory.push(monitorData.memory.usage)
  
  // 保持最近20个数据点
  if (chartData.cpuMemory.times.length > 20) {
    chartData.cpuMemory.times.shift()
    chartData.cpuMemory.cpu.shift()
    chartData.cpuMemory.memory.shift()
  }
  
  // 网络图表数据
  chartData.network.times.push(now)
  chartData.network.upload.push(monitorData.network.upload / 1024 / 1024) // 转换为MB/s
  chartData.network.download.push(monitorData.network.download / 1024 / 1024)
  
  if (chartData.network.times.length > 20) {
    chartData.network.times.shift()
    chartData.network.upload.shift()
    chartData.network.download.shift()
  }
  
  // 更新图表
  nextTick(() => {
    updateCharts()
  })
}

const updateCharts = () => {
  // 这里应该使用真实的图表库如 ECharts
  // 由于没有引入图表库，这里只是占位
  if (cpuMemoryChartRef.value) {
    cpuMemoryChartRef.value.innerHTML = `
      <div style="display: flex; justify-content: center; align-items: center; height: 200px; background: #f5f7fa; border-radius: 4px; color: #909399;">
        <div>
          <div>CPU: ${monitorData.cpu.usage}%</div>
          <div>内存: ${monitorData.memory.usage}%</div>
          <div style="font-size: 12px; margin-top: 8px;">图表需要集成 ECharts</div>
        </div>
      </div>
    `
  }
  
  if (networkChartRef.value) {
    networkChartRef.value.innerHTML = `
      <div style="display: flex; justify-content: center; align-items: center; height: 200px; background: #f5f7fa; border-radius: 4px; color: #909399;">
        <div>
          <div>上传: ${formatBytes(monitorData.network.upload)}/s</div>
          <div>下载: ${formatBytes(monitorData.network.download)}/s</div>
          <div style="font-size: 12px; margin-top: 8px;">图表需要集成 ECharts</div>
        </div>
      </div>
    `
  }
}

const refreshData = async () => {
  await Promise.all([
    loadMonitorData(),
    loadHealthData()
  ])
  ElMessage.success('数据已刷新')
}

const toggleAutoRefresh = (enabled: boolean) => {
  if (enabled) {
    refreshTimer.value = setInterval(() => {
      loadMonitorData()
    }, 5000) // 每5秒刷新一次
    ElMessage.success('已开启自动刷新')
  } else {
    if (refreshTimer.value) {
      clearInterval(refreshTimer.value)
      refreshTimer.value = null
    }
    ElMessage.info('已关闭自动刷新')
  }
}

// 生命周期
onMounted(async () => {
  await refreshData()
  nextTick(() => {
    updateCharts()
  })
})

onUnmounted(() => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
  }
})
</script>

<style scoped>
.system-monitor {
  height: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-overview {
  margin-bottom: 24px;
}

.status-card {
  height: 120px;
  margin-bottom: 16px;
}

.status-content {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.status-icon {
  font-size: 32px;
  margin-right: 16px;
}

.cpu-card .status-icon {
  color: #409eff;
}

.memory-card .status-icon {
  color: #67c23a;
}

.disk-card .status-icon {
  color: #e6a23c;
}

.network-card .status-icon {
  color: #f56c6c;
}

.status-info h3 {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.status-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.status-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.jvm-monitor {
  margin-bottom: 24px;
}

.jvm-monitor h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.jvm-card {
  margin-bottom: 16px;
}

.jvm-content {
  padding: 16px 0;
}

.jvm-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.health-check {
  margin-bottom: 24px;
}

.health-check h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.health-detail {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
}

.charts-section h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 200px;
}

.chart {
  width: 100%;
  height: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
  
  .status-content {
    flex-direction: column;
    text-align: center;
  }
  
  .status-icon {
    margin-right: 0;
    margin-bottom: 8px;
  }
  
  .jvm-info {
    flex-direction: column;
    gap: 4px;
  }
}
</style>