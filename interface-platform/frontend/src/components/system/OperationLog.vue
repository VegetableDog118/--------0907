<template>
  <div class="operation-log">
    <el-card>
      <template #header>
        <div class="section-header">
          <h4>操作日志管理</h4>
          <div class="header-actions">
            <el-button type="danger" @click="showClearDialog">
              <el-icon><Delete /></el-icon>
              清理日志
            </el-button>
            <el-button @click="exportLogs">
              <el-icon><Download /></el-icon>
              导出日志
            </el-button>
            <el-button @click="loadLogList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="用户名">
            <el-input 
              v-model="filterForm.username" 
              placeholder="输入用户名" 
              clearable 
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="操作模块">
            <el-select v-model="filterForm.module" placeholder="选择模块" clearable style="width: 150px">
              <el-option label="用户管理" value="user" />
              <el-option label="角色管理" value="role" />
              <el-option label="接口管理" value="interface" />
              <el-option label="申请管理" value="application" />
              <el-option label="系统配置" value="config" />
              <el-option label="数据源" value="datasource" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作类型">
            <el-select v-model="filterForm.operation" placeholder="选择操作" clearable style="width: 120px">
              <el-option label="查询" value="SELECT" />
              <el-option label="新增" value="INSERT" />
              <el-option label="更新" value="UPDATE" />
              <el-option label="删除" value="DELETE" />
              <el-option label="登录" value="LOGIN" />
              <el-option label="登出" value="LOGOUT" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 350px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadLogList">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 日志列表 -->
      <el-table
        :data="logList"
        v-loading="loading"
        empty-text="暂无日志数据"
        class="log-table"
        @row-click="viewLogDetail"
      >
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="operation" label="操作" width="80">
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operation)">{{ getOperationName(row.operation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            <el-tag :type="getModuleTagType(row.module)">{{ getModuleName(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="请求方法" width="120" />
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="executeTime" label="执行时间" width="100">
          <template #default="{ row }">
            <span :class="getExecuteTimeClass(row.executeTime)">{{ row.executeTime }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="viewLogDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="logList.length > 0">
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

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="日志详情" width="800px">
      <el-descriptions :column="2" border v-if="selectedLog">
        <el-descriptions-item label="用户名">{{ selectedLog.username }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ selectedLog.userId }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="getOperationTagType(selectedLog.operation)">{{ getOperationName(selectedLog.operation) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">
          <el-tag :type="getModuleTagType(selectedLog.module)">{{ getModuleName(selectedLog.module) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ selectedLog.method }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ selectedLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="执行时间">
          <span :class="getExecuteTimeClass(selectedLog.executeTime)">{{ selectedLog.executeTime }}ms</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedLog.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ selectedLog.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ selectedLog.createTime }}</el-descriptions-item>
        <el-descriptions-item label="用户代理" :span="2">
          <div class="user-agent">{{ selectedLog.userAgent }}</div>
        </el-descriptions-item>
      </el-descriptions>
      
      <div class="log-params" v-if="selectedLog?.params">
        <h4>请求参数</h4>
        <el-input
          v-model="selectedLog.params"
          type="textarea"
          :rows="6"
          readonly
          class="params-textarea"
        />
      </div>
      
      <div class="log-error" v-if="selectedLog?.errorMsg">
        <h4>错误信息</h4>
        <el-alert
          :title="selectedLog.errorMsg"
          type="error"
          :closable="false"
          show-icon
        />
      </div>
    </el-dialog>

    <!-- 清理日志弹窗 -->
    <el-dialog v-model="clearDialogVisible" title="清理日志" width="500px">
      <el-form :model="clearForm" label-width="120px">
        <el-form-item label="清理策略">
          <el-radio-group v-model="clearForm.strategy">
            <el-radio value="days">按天数清理</el-radio>
            <el-radio value="date">按日期清理</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="clearForm.strategy === 'days'" label="保留天数">
          <el-input-number 
            v-model="clearForm.days" 
            :min="1" 
            :max="365" 
            placeholder="请输入保留天数"
          />
          <div class="form-tip">将删除 {{ clearForm.days }} 天前的所有日志</div>
        </el-form-item>
        <el-form-item v-if="clearForm.strategy === 'date'" label="清理日期">
          <el-date-picker
            v-model="clearForm.beforeDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
          <div class="form-tip">将删除此日期之前的所有日志</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmClearLogs" :loading="clearing">确认清理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Delete, Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as systemApi from '@/api/system'
import type { OperationLog } from '@/api/system'

// 响应式数据
const loading = ref(false)
const clearing = ref(false)
const detailDialogVisible = ref(false)
const clearDialogVisible = ref(false)
const selectedLog = ref<OperationLog | null>(null)
const dateRange = ref<[string, string] | null>(null)

// 筛选表单
const filterForm = reactive({
  username: '',
  module: '',
  operation: ''
})

// 清理表单
const clearForm = reactive({
  strategy: 'days',
  days: 30,
  beforeDate: ''
})

// 日志列表数据
const logList = ref<OperationLog[]>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 方法
const getOperationTagType = (operation: string) => {
  const typeMap: Record<string, string> = {
    SELECT: 'info',
    INSERT: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: 'primary',
    LOGOUT: 'info'
  }
  return typeMap[operation] || 'info'
}

const getOperationName = (operation: string) => {
  const nameMap: Record<string, string> = {
    SELECT: '查询',
    INSERT: '新增',
    UPDATE: '更新',
    DELETE: '删除',
    LOGIN: '登录',
    LOGOUT: '登出'
  }
  return nameMap[operation] || operation
}

const getModuleTagType = (module: string) => {
  const typeMap: Record<string, string> = {
    user: 'primary',
    role: 'success',
    interface: 'warning',
    application: 'info',
    config: 'danger',
    datasource: 'primary'
  }
  return typeMap[module] || 'info'
}

const getModuleName = (module: string) => {
  const nameMap: Record<string, string> = {
    user: '用户管理',
    role: '角色管理',
    interface: '接口管理',
    application: '申请管理',
    config: '系统配置',
    datasource: '数据源'
  }
  return nameMap[module] || module
}

const getExecuteTimeClass = (time: number) => {
  if (time < 100) return 'fast-time'
  if (time < 500) return 'normal-time'
  return 'slow-time'
}

const loadLogList = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      username: filterForm.username,
      module: filterForm.module,
      operation: filterForm.operation
    }
    
    if (dateRange.value) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    
    const response = await systemApi.getOperationLogList(params)
    logList.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('加载日志列表失败:', error)
    ElMessage.error('加载日志列表失败')
    
    // 使用模拟数据
    logList.value = [
      {
        id: '1',
        userId: '1',
        username: 'admin',
        operation: 'LOGIN',
        module: 'user',
        method: 'POST /api/auth/login',
        params: JSON.stringify({ username: 'admin' }),
        ip: '192.168.1.100',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        executeTime: 156,
        status: 'SUCCESS',
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: '2',
        userId: '1',
        username: 'admin',
        operation: 'INSERT',
        module: 'user',
        method: 'POST /api/system/users',
        params: JSON.stringify({ username: 'test_user', email: 'test@example.com' }),
        ip: '192.168.1.100',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        executeTime: 234,
        status: 'SUCCESS',
        createTime: '2024-01-15 10:25:00'
      },
      {
        id: '3',
        userId: '2',
        username: 'settlement_user',
        operation: 'UPDATE',
        module: 'interface',
        method: 'PUT /api/interfaces/IF001',
        params: JSON.stringify({ status: 'published' }),
        ip: '192.168.1.101',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        executeTime: 89,
        status: 'SUCCESS',
        createTime: '2024-01-15 10:20:00'
      },
      {
        id: '4',
        userId: '3',
        username: 'tech_user',
        operation: 'DELETE',
        module: 'interface',
        method: 'DELETE /api/interfaces/IF999',
        params: JSON.stringify({ id: 'IF999' }),
        ip: '192.168.1.102',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        executeTime: 1234,
        status: 'ERROR',
        errorMsg: '接口不存在或已被删除',
        createTime: '2024-01-15 10:15:00'
      },
      {
        id: '5',
        userId: '1',
        username: 'admin',
        operation: 'UPDATE',
        module: 'config',
        method: 'PUT /api/system/configs/1',
        params: JSON.stringify({ key: 'system.title', value: '接口平台管理系统 v2.0' }),
        ip: '192.168.1.100',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        executeTime: 67,
        status: 'SUCCESS',
        createTime: '2024-01-15 10:10:00'
      }
    ]
    pagination.total = logList.value.length
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.username = ''
  filterForm.module = ''
  filterForm.operation = ''
  dateRange.value = null
  loadLogList()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadLogList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadLogList()
}

const viewLogDetail = (log: OperationLog) => {
  selectedLog.value = log
  detailDialogVisible.value = true
}

const showClearDialog = () => {
  clearDialogVisible.value = true
}

const confirmClearLogs = async () => {
  try {
    let beforeDate = ''
    if (clearForm.strategy === 'days') {
      const date = new Date()
      date.setDate(date.getDate() - clearForm.days)
      beforeDate = date.toISOString().split('T')[0]
    } else {
      beforeDate = clearForm.beforeDate
    }
    
    if (!beforeDate) {
      ElMessage.error('请选择清理日期')
      return
    }
    
    await ElMessageBox.confirm(
      `确定要清理 ${beforeDate} 之前的所有日志吗？此操作不可恢复！`,
      '确认清理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    clearing.value = true
    await systemApi.clearOperationLogs(beforeDate)
    
    ElMessage.success('日志清理成功')
    clearDialogVisible.value = false
    loadLogList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清理日志失败:', error)
      ElMessage.error('清理日志失败')
    }
  } finally {
    clearing.value = false
  }
}

const exportLogs = () => {
  // 模拟导出功能
  const params = new URLSearchParams({
    username: filterForm.username,
    module: filterForm.module,
    operation: filterForm.operation,
    ...(dateRange.value && {
      startTime: dateRange.value[0],
      endTime: dateRange.value[1]
    })
  })
  
  // 这里应该调用真实的导出API
  ElMessage.success('导出功能开发中...')
}

// 生命周期
onMounted(() => {
  loadLogList()
})
</script>

<style scoped>
.operation-log {
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
  gap: 12px;
}

.filter-bar {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.log-table {
  margin-bottom: 20px;
}

.log-table :deep(.el-table__row) {
  cursor: pointer;
}

.log-table :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

.fast-time {
  color: #67c23a;
  font-weight: 600;
}

.normal-time {
  color: #e6a23c;
  font-weight: 600;
}

.slow-time {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.user-agent {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.log-params {
  margin-top: 24px;
}

.log-params h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.params-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.log-error {
  margin-top: 24px;
}

.log-error h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
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
    justify-content: flex-end;
    flex-wrap: wrap;
  }
  
  .filter-bar :deep(.el-form) {
    flex-direction: column;
  }
  
  .filter-bar :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 12px;
  }
}
</style>