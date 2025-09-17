<template>
  <div class="data-statistics">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>数据统计</h2>
        <p>系统数据统计分析和可视化展示</p>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
        <el-button type="primary" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出报告
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="metrics-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-icon api-calls">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-number">{{ formatNumber(metrics.apiCalls) }}</div>
                <div class="metric-label">API调用总数</div>
                <div class="metric-change positive">
                  <el-icon><CaretTop /></el-icon>
                  +{{ metrics.apiCallsGrowth }}%
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-icon active-users">
                <el-icon><User /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-number">{{ formatNumber(metrics.activeUsers) }}</div>
                <div class="metric-label">活跃用户数</div>
                <div class="metric-change positive">
                  <el-icon><CaretTop /></el-icon>
                  +{{ metrics.activeUsersGrowth }}%
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-icon success-rate">
                <el-icon><Check /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-number">{{ metrics.successRate }}%</div>
                <div class="metric-label">成功率</div>
                <div class="metric-change negative">
                  <el-icon><CaretBottom /></el-icon>
                  -{{ metrics.successRateChange }}%
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="metric-card">
            <div class="metric-content">
              <div class="metric-icon avg-response">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-number">{{ metrics.avgResponseTime }}ms</div>
                <div class="metric-label">平均响应时间</div>
                <div class="metric-change positive">
                  <el-icon><CaretBottom /></el-icon>
                  -{{ metrics.responseTimeImprovement }}%
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <!-- API调用趋势图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>API调用趋势</span>
              <el-select v-model="apiTrendPeriod" size="small" style="width: 100px;">
                <el-option label="7天" value="7d" />
                <el-option label="30天" value="30d" />
                <el-option label="90天" value="90d" />
              </el-select>
            </div>
          </template>
          <div class="chart-container" ref="apiTrendChart"></div>
        </el-card>
      </el-col>
      
      <!-- 接口状态分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>接口状态分布</span>
          </template>
          <div class="chart-container" ref="statusDistributionChart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 用户活跃度热力图 -->
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>用户活跃度热力图</span>
              <div class="chart-controls">
                <el-radio-group v-model="heatmapType" size="small">
                  <el-radio-button label="hourly">按小时</el-radio-button>
                  <el-radio-button label="daily">按天</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div class="chart-container large" ref="heatmapChart"></div>
        </el-card>
      </el-col>
      
      <!-- 热门接口排行 -->
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>热门接口排行</span>
          </template>
          <div class="ranking-list">
            <div
              v-for="(item, index) in topInterfaces"
              :key="item.name"
              class="ranking-item"
            >
              <div class="rank-number" :class="getRankClass(index)">
                {{ index + 1 }}
              </div>
              <div class="interface-info">
                <div class="interface-name">{{ item.name }}</div>
                <div class="interface-path">{{ item.path }}</div>
              </div>
              <div class="call-count">
                {{ formatNumber(item.calls) }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细统计表格 -->
    <el-card class="table-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>接口详细统计</span>
          <div class="table-controls">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索接口"
              size="small"
              style="width: 200px; margin-right: 10px;"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="statusFilter" placeholder="状态筛选" size="small" style="width: 120px;">
              <el-option label="全部" value="" />
              <el-option label="正常" value="normal" />
              <el-option label="异常" value="error" />
            </el-select>
          </div>
        </div>
      </template>
      
      <el-table :data="filteredTableData" v-loading="tableLoading" stripe>
        <el-table-column prop="name" label="接口名称" min-width="200">
          <template #default="{ row }">
            <div class="interface-cell">
              <div class="name">{{ row.name }}</div>
              <div class="path">{{ row.path }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="method" label="方法" width="80">
          <template #default="{ row }">
            <el-tag :type="getMethodType(row.method)" size="small">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="totalCalls" label="总调用数" width="120" sortable>
          <template #default="{ row }">
            <span class="number-cell">{{ formatNumber(row.totalCalls) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="successRate" label="成功率" width="100" sortable>
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="row.successRate"
                :color="getProgressColor(row.successRate)"
                :show-text="false"
                :stroke-width="6"
              />
              <span class="percentage-text">{{ row.successRate }}%</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="avgResponseTime" label="平均响应时间" width="140" sortable>
          <template #default="{ row }">
            <span class="time-cell" :class="getResponseTimeClass(row.avgResponseTime)">
              {{ row.avgResponseTime }}ms
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="errorRate" label="错误率" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="getErrorRateType(row.errorRate)" size="small">
              {{ row.errorRate }}%
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="lastCall" label="最后调用" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastCall) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Download, TrendCharts, User, Check, Timer, CaretTop, CaretBottom,
  Search, View
} from '@element-plus/icons-vue'

// 日期范围
const dateRange = ref(['2024-01-01', '2024-01-31'])

// 核心指标
const metrics = reactive({
  apiCalls: 1234567,
  apiCallsGrowth: 12.5,
  activeUsers: 8945,
  activeUsersGrowth: 8.3,
  successRate: 99.2,
  successRateChange: 0.3,
  avgResponseTime: 245,
  responseTimeImprovement: 15.2
})

// 图表配置
const apiTrendPeriod = ref('30d')
const heatmapType = ref('hourly')

// 接口统计数据类型定义
interface InterfaceStatItem {
  name: string
  path: string
  method: string
  totalCalls: number
  successRate: number
  avgResponseTime: number
  errorRate: number
  lastCall: string
}

// 表格数据
const searchKeyword = ref('')
const statusFilter = ref('')
const tableLoading = ref(false)
const tableData = ref<InterfaceStatItem[]>([])

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 热门接口数据
const topInterfaces = ref([
  { name: '用户登录', path: '/api/v1/auth/login', calls: 45230 },
  { name: '获取用户信息', path: '/api/v1/user/profile', calls: 32145 },
  { name: '数据查询', path: '/api/v1/data/query', calls: 28967 },
  { name: '文件上传', path: '/api/v1/upload', calls: 21543 },
  { name: '系统配置', path: '/api/v1/config', calls: 18234 }
])

// 图表引用
const apiTrendChart = ref()
const statusDistributionChart = ref()
const heatmapChart = ref()

// 过滤后的表格数据
const filteredTableData = computed(() => {
  let data = tableData.value
  
  if (searchKeyword.value) {
    data = data.filter(item => 
      item.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      item.path.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  if (statusFilter.value) {
    data = data.filter(item => {
      if (statusFilter.value === 'normal') {
        return item.errorRate < 5
      } else if (statusFilter.value === 'error') {
        return item.errorRate >= 5
      }
      return true
    })
  }
  
  return data
})

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return 'rank-other'
}

// 获取请求方法类型
const getMethodType = (method: string) => {
  const types: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return types[method] || 'info'
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 95) return '#67c23a'
  if (percentage >= 90) return '#e6a23c'
  return '#f56c6c'
}

// 获取响应时间样式
const getResponseTimeClass = (time: number) => {
  if (time <= 200) return 'time-good'
  if (time <= 500) return 'time-normal'
  return 'time-slow'
}

// 获取错误率类型
const getErrorRateType = (rate: number) => {
  if (rate <= 1) return 'success'
  if (rate <= 5) return 'warning'
  return 'danger'
}

// 初始化图表
const initCharts = () => {
  // 这里应该使用实际的图表库（如 ECharts）来初始化图表
  // 由于没有引入图表库，这里只是占位
  console.log('初始化图表')
}

// 加载表格数据
const loadTableData = async () => {
  tableLoading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 模拟数据
    const mockData = [
      {
        name: '用户登录接口',
        path: '/api/v1/auth/login',
        method: 'POST',
        totalCalls: 45230,
        successRate: 99.5,
        avgResponseTime: 156,
        errorRate: 0.5,
        lastCall: '2024-01-15 14:30:25'
      },
      {
        name: '用户信息查询',
        path: '/api/v1/user/profile',
        method: 'GET',
        totalCalls: 32145,
        successRate: 98.8,
        avgResponseTime: 89,
        errorRate: 1.2,
        lastCall: '2024-01-15 14:29:18'
      },
      {
        name: '数据统计接口',
        path: '/api/v1/statistics',
        method: 'GET',
        totalCalls: 28967,
        successRate: 97.2,
        avgResponseTime: 234,
        errorRate: 2.8,
        lastCall: '2024-01-15 14:28:45'
      },
      {
        name: '文件上传接口',
        path: '/api/v1/upload',
        method: 'POST',
        totalCalls: 21543,
        successRate: 95.6,
        avgResponseTime: 1245,
        errorRate: 4.4,
        lastCall: '2024-01-15 14:25:12'
      },
      {
        name: '数据导出接口',
        path: '/api/v1/export',
        method: 'GET',
        totalCalls: 8934,
        successRate: 92.3,
        avgResponseTime: 2156,
        errorRate: 7.7,
        lastCall: '2024-01-15 14:20:33'
      }
    ]
    
    tableData.value = mockData
    pagination.total = mockData.length
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    tableLoading.value = false
  }
}

// 日期变更处理
const handleDateChange = () => {
  loadTableData()
  // 重新加载图表数据
}

// 导出报告
const handleExport = () => {
  ElMessage.success('报告导出功能开发中')
}

// 查看详情
const handleViewDetail = (row: any) => {
  ElMessage.info(`查看 ${row.name} 的详细统计`)
}

// 分页处理
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadTableData()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadTableData()
}

onMounted(async () => {
  await loadTableData()
  await nextTick()
  initCharts()
})
</script>

<style scoped>
.data-statistics {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left h2 {
  margin: 0 0 5px 0;
  color: #303133;
}

.header-left p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.metrics-cards {
  margin-bottom: 20px;
}

.metric-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.metric-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 20px;
  color: white;
}

.metric-icon.api-calls {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.metric-icon.active-users {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.metric-icon.success-rate {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.metric-icon.avg-response {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.metric-number {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.metric-change {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
}

.metric-change.positive {
  color: #67c23a;
}

.metric-change.negative {
  color: #f56c6c;
}

.chart-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.table-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.chart-container {
  height: 300px;
  background: #f8f9fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.chart-container.large {
  height: 400px;
}

.ranking-list {
  padding: 10px 0;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  margin-right: 12px;
}

.rank-number.rank-1 {
  background: #ffd700;
  color: white;
}

.rank-number.rank-2 {
  background: #c0c0c0;
  color: white;
}

.rank-number.rank-3 {
  background: #cd7f32;
  color: white;
}

.rank-number.rank-other {
  background: #f0f0f0;
  color: #666;
}

.interface-info {
  flex: 1;
}

.interface-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.interface-path {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.call-count {
  font-weight: 600;
  color: #409eff;
}

.table-card {
  border-radius: 8px;
}

.interface-cell .name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.interface-cell .path {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.number-cell {
  font-weight: 600;
  color: #409eff;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.percentage-text {
  font-size: 12px;
  color: #606266;
  min-width: 35px;
}

.time-cell {
  font-weight: 500;
}

.time-cell.time-good {
  color: #67c23a;
}

.time-cell.time-normal {
  color: #e6a23c;
}

.time-cell.time-slow {
  color: #f56c6c;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>