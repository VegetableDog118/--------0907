<template>
  <div class="interface-stats">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ stats.totalInterfaces }}</div>
              <div class="stats-label">总接口数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon active">
              <el-icon><Check /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ stats.activeInterfaces }}</div>
              <div class="stats-label">活跃接口</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon calls">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ formatNumber(stats.totalCalls) }}</div>
              <div class="stats-label">总调用次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon success">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ stats.successRate }}%</div>
              <div class="stats-label">成功率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 趋势图表 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>接口调用趋势</span>
            </div>
          </template>
          <div class="chart-container" ref="callTrendChart"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>接口分类统计</span>
            </div>
          </template>
          <div class="chart-container" ref="categoryChart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Document, Check, TrendCharts, CircleCheck } from '@element-plus/icons-vue'

// 统计数据
const stats = reactive({
  totalInterfaces: 0,
  activeInterfaces: 0,
  totalCalls: 0,
  successRate: 0
})

// 图表引用
const callTrendChart = ref()
const categoryChart = ref()

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 模拟数据，实际应该调用API
    stats.totalInterfaces = 156
    stats.activeInterfaces = 142
    stats.totalCalls = 2847593
    stats.successRate = 99.2
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 初始化图表
const initCharts = async () => {
  await nextTick()
  // 这里可以集成图表库如ECharts
  // 暂时显示占位内容
  if (callTrendChart.value) {
    callTrendChart.value.innerHTML = '<div style="height: 200px; display: flex; align-items: center; justify-content: center; color: #999;">调用趋势图表</div>'
  }
  if (categoryChart.value) {
    categoryChart.value.innerHTML = '<div style="height: 200px; display: flex; align-items: center; justify-content: center; color: #999;">分类统计图表</div>'
  }
}

onMounted(() => {
  loadStats()
  initCharts()
})
</script>

<style scoped>
.interface-stats {
  padding: 20px;
}

.stats-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stats-item {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 24px;
}

.stats-icon.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stats-icon.calls {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.stats-icon.success {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}

.stats-content {
  flex: 1;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.chart-container {
  height: 200px;
  width: 100%;
}
</style>