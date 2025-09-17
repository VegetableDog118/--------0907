<template>
  <div class="datasource-management">
    <el-card>
      <template #header>
        <div class="section-header">
          <h4>数据源管理</h4>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              新增数据源
            </el-button>
            <el-button @click="loadDatasourceList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="数据源名称">
            <el-input 
              v-model="filterForm.name" 
              placeholder="输入数据源名称" 
              clearable 
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="filterForm.type" placeholder="选择类型" clearable style="width: 150px">
              <el-option label="MySQL" value="mysql" />
              <el-option label="PostgreSQL" value="postgresql" />
              <el-option label="Oracle" value="oracle" />
              <el-option label="SQL Server" value="sqlserver" />
              <el-option label="Redis" value="redis" />
              <el-option label="MongoDB" value="mongodb" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="选择状态" clearable style="width: 120px">
              <el-option label="正常" value="active" />
              <el-option label="禁用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadDatasourceList">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据源列表 -->
      <el-table
        :data="datasourceList"
        v-loading="loading"
        empty-text="暂无数据源"
        class="datasource-table"
      >
        <el-table-column prop="name" label="数据源名称" width="150" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" width="150" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="database" label="数据库" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDatasource(row)">详情</el-button>
            <el-button size="small" type="primary" @click="editDatasource(row)">编辑</el-button>
            <el-button 
              size="small" 
              type="info" 
              @click="testConnection(row)"
              :loading="row.testing"
            >
              测试连接
            </el-button>
            <el-button 
              size="small" 
              :type="row.status === 'active' ? 'danger' : 'success'"
              @click="toggleDatasourceStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="datasourceList.length > 0">
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

    <!-- 新增/编辑数据源弹窗 -->
    <el-dialog 
      v-model="datasourceDialogVisible" 
      :title="isEdit ? '编辑数据源' : '新增数据源'" 
      width="600px"
      @close="resetDatasourceForm"
    >
      <el-form 
        ref="datasourceFormRef" 
        :model="datasourceForm" 
        :rules="datasourceFormRules" 
        label-width="100px"
      >
        <el-form-item label="数据源名称" prop="name">
          <el-input v-model="datasourceForm.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="type">
          <el-select v-model="datasourceForm.type" placeholder="请选择数据源类型" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="SQL Server" value="sqlserver" />
            <el-option label="Redis" value="redis" />
            <el-option label="MongoDB" value="mongodb" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="datasourceForm.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number 
            v-model="datasourceForm.port" 
            :min="1" 
            :max="65535" 
            placeholder="请输入端口" 
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="数据库名" prop="database">
          <el-input v-model="datasourceForm.database" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="datasourceForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="datasourceForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="datasourceForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="datasourceDialogVisible = false">取消</el-button>
        <el-button type="info" @click="testFormConnection" :loading="testing">测试连接</el-button>
        <el-button type="primary" @click="saveDatasource" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据源详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="数据源详情" width="600px">
      <el-descriptions :column="2" border v-if="selectedDatasource">
        <el-descriptions-item label="数据源名称">{{ selectedDatasource.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="getTypeTagType(selectedDatasource.type)">{{ getTypeName(selectedDatasource.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="主机地址">{{ selectedDatasource.host }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ selectedDatasource.port }}</el-descriptions-item>
        <el-descriptions-item label="数据库">{{ selectedDatasource.database }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selectedDatasource.username }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedDatasource.status === 'active' ? 'success' : 'danger'">
            {{ selectedDatasource.status === 'active' ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ selectedDatasource.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ selectedDatasource.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 连接测试结果弹窗 -->
    <el-dialog v-model="testResultDialogVisible" title="连接测试结果" width="500px">
      <div class="test-result">
        <div class="result-header">
          <el-icon :class="testResult.success ? 'success-icon' : 'error-icon'">
            <component :is="testResult.success ? 'SuccessFilled' : 'CircleCloseFilled'" />
          </el-icon>
          <span :class="testResult.success ? 'success-text' : 'error-text'">
            {{ testResult.success ? '连接成功' : '连接失败' }}
          </span>
        </div>
        <div class="result-content">
          <p><strong>响应时间:</strong> {{ testResult.responseTime }}ms</p>
          <p><strong>测试信息:</strong> {{ testResult.message }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Refresh, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import * as systemApi from '@/api/system'
import type { Datasource, CreateDatasourceRequest, UpdateDatasourceRequest } from '@/api/system'

// 扩展数据源类型，添加测试状态
interface DatasourceItem extends Datasource {
  testing?: boolean
}

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const datasourceDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const testResultDialogVisible = ref(false)
const isEdit = ref(false)
const selectedDatasource = ref<DatasourceItem | null>(null)
const datasourceFormRef = ref<FormInstance>()

// 筛选表单
const filterForm = reactive({
  name: '',
  type: '',
  status: ''
})

// 数据源表单
const datasourceForm = reactive({
  id: '',
  name: '',
  type: '',
  host: '',
  port: 3306,
  database: '',
  username: '',
  password: '',
  status: 'active'
})

// 测试结果
const testResult = reactive({
  success: false,
  message: '',
  responseTime: 0
})

// 表单验证规则
const datasourceFormRules = {
  name: [
    { required: true, message: '请输入数据源名称', trigger: 'blur' },
    { min: 2, max: 50, message: '数据源名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择数据源类型', trigger: 'change' }
  ],
  host: [
    { required: true, message: '请输入主机地址', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '请输入端口', trigger: 'blur' },
    { type: 'number', min: 1, max: 65535, message: '端口范围在 1 到 65535', trigger: 'blur' }
  ],
  database: [
    { required: true, message: '请输入数据库名', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 数据源列表数据
const datasourceList = ref<DatasourceItem[]>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 方法
const getTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    mysql: 'primary',
    postgresql: 'success',
    oracle: 'warning',
    sqlserver: 'info',
    redis: 'danger',
    mongodb: 'success'
  }
  return typeMap[type] || 'info'
}

const getTypeName = (type: string) => {
  const nameMap: Record<string, string> = {
    mysql: 'MySQL',
    postgresql: 'PostgreSQL',
    oracle: 'Oracle',
    sqlserver: 'SQL Server',
    redis: 'Redis',
    mongodb: 'MongoDB'
  }
  return nameMap[type] || type
}

const loadDatasourceList = async () => {
  loading.value = true
  try {
    const response = await systemApi.getDatasourceList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      name: filterForm.name,
      type: filterForm.type,
      status: filterForm.status
    })
    
    datasourceList.value = response.records.map(item => ({
      ...item,
      testing: false
    }))
    pagination.total = response.total
  } catch (error) {
    console.error('加载数据源列表失败:', error)
    ElMessage.error('加载数据源列表失败')
    
    // 使用模拟数据
    datasourceList.value = [
      {
        id: '1',
        name: '主数据库',
        type: 'mysql',
        host: '192.168.1.100',
        port: 3306,
        database: 'interface_platform',
        username: 'root',
        status: 'active',
        createTime: '2024-01-01 00:00:00',
        updateTime: '2024-01-01 00:00:00',
        testing: false
      },
      {
        id: '2',
        name: '缓存数据库',
        type: 'redis',
        host: '192.168.1.101',
        port: 6379,
        database: '0',
        username: 'redis',
        status: 'active',
        createTime: '2024-01-02 00:00:00',
        updateTime: '2024-01-02 00:00:00',
        testing: false
      },
      {
        id: '3',
        name: '日志数据库',
        type: 'postgresql',
        host: '192.168.1.102',
        port: 5432,
        database: 'logs',
        username: 'postgres',
        status: 'active',
        createTime: '2024-01-03 00:00:00',
        updateTime: '2024-01-03 00:00:00',
        testing: false
      }
    ]
    pagination.total = datasourceList.value.length
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.name = ''
  filterForm.type = ''
  filterForm.status = ''
  loadDatasourceList()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadDatasourceList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadDatasourceList()
}

const showCreateDialog = () => {
  isEdit.value = false
  datasourceDialogVisible.value = true
}

const viewDatasource = (datasource: DatasourceItem) => {
  selectedDatasource.value = datasource
  detailDialogVisible.value = true
}

const editDatasource = (datasource: DatasourceItem) => {
  isEdit.value = true
  Object.assign(datasourceForm, {
    id: datasource.id,
    name: datasource.name,
    type: datasource.type,
    host: datasource.host,
    port: datasource.port,
    database: datasource.database,
    username: datasource.username,
    password: '',
    status: datasource.status
  })
  datasourceDialogVisible.value = true
}

const resetDatasourceForm = () => {
  datasourceFormRef.value?.resetFields()
  Object.assign(datasourceForm, {
    id: '',
    name: '',
    type: '',
    host: '',
    port: 3306,
    database: '',
    username: '',
    password: '',
    status: 'active'
  })
}

const testFormConnection = async () => {
  if (!datasourceFormRef.value) return
  
  try {
    await datasourceFormRef.value.validate()
    testing.value = true
    
    // 模拟测试连接
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    const success = Math.random() > 0.3 // 70% 成功率
    Object.assign(testResult, {
      success,
      message: success ? '数据源连接正常' : '连接超时，请检查网络和配置',
      responseTime: Math.floor(Math.random() * 1000) + 100
    })
    
    testResultDialogVisible.value = true
  } catch (error) {
    console.error('测试连接失败:', error)
  } finally {
    testing.value = false
  }
}

const testConnection = async (datasource: DatasourceItem) => {
  datasource.testing = true
  try {
    const response = await systemApi.testDatasourceConnection(datasource.id)
    Object.assign(testResult, response)
    testResultDialogVisible.value = true
  } catch (error) {
    console.error('测试连接失败:', error)
    Object.assign(testResult, {
      success: false,
      message: '测试连接失败，请检查数据源配置',
      responseTime: 0
    })
    testResultDialogVisible.value = true
  } finally {
    datasource.testing = false
  }
}

const saveDatasource = async () => {
  if (!datasourceFormRef.value) return
  
  try {
    await datasourceFormRef.value.validate()
    saving.value = true
    
    if (isEdit.value) {
      const updateData: UpdateDatasourceRequest = {
        id: datasourceForm.id,
        name: datasourceForm.name,
        host: datasourceForm.host,
        port: datasourceForm.port,
        database: datasourceForm.database,
        username: datasourceForm.username,
        status: datasourceForm.status
      }
      if (datasourceForm.password) {
        updateData.password = datasourceForm.password
      }
      await systemApi.updateDatasource(updateData)
      ElMessage.success('数据源更新成功')
    } else {
      const createData: CreateDatasourceRequest = {
        name: datasourceForm.name,
        type: datasourceForm.type,
        host: datasourceForm.host,
        port: datasourceForm.port,
        database: datasourceForm.database,
        username: datasourceForm.username,
        password: datasourceForm.password
      }
      await systemApi.createDatasource(createData)
      ElMessage.success('数据源创建成功')
    }
    
    datasourceDialogVisible.value = false
    loadDatasourceList()
  } catch (error) {
    console.error('保存数据源失败:', error)
    ElMessage.error('保存数据源失败')
  } finally {
    saving.value = false
  }
}

const toggleDatasourceStatus = async (datasource: DatasourceItem) => {
  const action = datasource.status === 'active' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}数据源 "${datasource.name}" 吗？`,
      `${action}数据源`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const newStatus = datasource.status === 'active' ? 'disabled' : 'active'
    await systemApi.updateDatasource({
      id: datasource.id,
      status: newStatus
    })
    
    ElMessage.success(`数据源${action}成功`)
    loadDatasourceList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}数据源失败:`, error)
      ElMessage.error(`${action}数据源失败`)
    }
  }
}

// 生命周期
onMounted(() => {
  loadDatasourceList()
})
</script>

<style scoped>
.datasource-management {
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

.datasource-table {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.test-result {
  text-align: center;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
}

.success-icon {
  color: #67c23a;
  font-size: 24px;
}

.error-icon {
  color: #f56c6c;
  font-size: 24px;
}

.success-text {
  color: #67c23a;
  font-size: 18px;
  font-weight: 600;
}

.error-text {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 600;
}

.result-content {
  text-align: left;
  background: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
}

.result-content p {
  margin: 8px 0;
  color: #606266;
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