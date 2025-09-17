<template>
  <div class="system-config">
    <el-card>
      <template #header>
        <div class="section-header">
          <h4>系统配置管理</h4>
          <div class="header-actions">
            <el-button @click="loadConfigList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="配置项">
            <el-input 
              v-model="filterForm.key" 
              placeholder="输入配置项" 
              clearable 
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="filterForm.category" placeholder="选择分类" clearable style="width: 150px">
              <el-option label="系统设置" value="system" />
              <el-option label="接口配置" value="interface" />
              <el-option label="安全配置" value="security" />
              <el-option label="邮件配置" value="email" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadConfigList">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 配置列表 -->
      <el-table
        :data="configList"
        v-loading="loading"
        empty-text="暂无配置数据"
        class="config-table"
      >
        <el-table-column prop="key" label="配置项" width="200" />
        <el-table-column prop="value" label="配置值" min-width="200">
          <template #default="{ row }">
            <div class="config-value">
              <span v-if="!row.editing">{{ getDisplayValue(row) }}</span>
              <el-input 
                v-else
                v-model="row.tempValue"
                :type="getInputType(row.type)"
                :rows="row.type === 'textarea' ? 3 : undefined"
                size="small"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">{{ getCategoryName(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160" />
        <el-table-column prop="updateBy" label="更新人" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div v-if="!row.editing">
              <el-button size="small" type="primary" @click="editConfig(row)">编辑</el-button>
              <el-button size="small" @click="viewConfig(row)">详情</el-button>
            </div>
            <div v-else>
              <el-button size="small" type="success" @click="saveConfig(row)">保存</el-button>
              <el-button size="small" @click="cancelEdit(row)">取消</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="configList.length > 0">
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

    <!-- 配置详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="配置详情" width="600px">
      <el-descriptions :column="1" border v-if="selectedConfig">
        <el-descriptions-item label="配置项">{{ selectedConfig.key }}</el-descriptions-item>
        <el-descriptions-item label="配置值">
          <div class="config-value-detail">
            {{ getDisplayValue(selectedConfig) }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ selectedConfig.description }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="getTypeTagType(selectedConfig.type)">{{ getTypeName(selectedConfig.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          <el-tag :type="getCategoryTagType(selectedConfig.category)">{{ getCategoryName(selectedConfig.category) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ selectedConfig.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="更新人">{{ selectedConfig.updateBy }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as systemApi from '@/api/system'
import type { SystemConfig, UpdateConfigRequest } from '@/api/system'

// 扩展配置项类型，添加编辑状态
interface ConfigItem extends SystemConfig {
  editing?: boolean
  tempValue?: string
}

// 响应式数据
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedConfig = ref<ConfigItem | null>(null)

// 筛选表单
const filterForm = reactive({
  key: '',
  category: ''
})

// 配置列表数据
const configList = ref<ConfigItem[]>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 方法
const getDisplayValue = (config: ConfigItem) => {
  if (config.type === 'password') {
    return '******'
  }
  if (config.type === 'boolean') {
    return config.value === 'true' ? '是' : '否'
  }
  return config.value
}

const getInputType = (type: string) => {
  switch (type) {
    case 'number':
      return 'number'
    case 'password':
      return 'password'
    case 'textarea':
      return 'textarea'
    default:
      return 'text'
  }
}

const getTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    string: 'primary',
    number: 'success',
    boolean: 'warning',
    password: 'danger',
    textarea: 'info'
  }
  return typeMap[type] || 'info'
}

const getTypeName = (type: string) => {
  const nameMap: Record<string, string> = {
    string: '字符串',
    number: '数字',
    boolean: '布尔值',
    password: '密码',
    textarea: '文本域'
  }
  return nameMap[type] || type
}

const getCategoryTagType = (category: string) => {
  const typeMap: Record<string, string> = {
    system: 'primary',
    interface: 'success',
    security: 'danger',
    email: 'warning',
    other: 'info'
  }
  return typeMap[category] || 'info'
}

const getCategoryName = (category: string) => {
  const nameMap: Record<string, string> = {
    system: '系统设置',
    interface: '接口配置',
    security: '安全配置',
    email: '邮件配置',
    other: '其他'
  }
  return nameMap[category] || category
}

const loadConfigList = async () => {
  loading.value = true
  try {
    const response = await systemApi.getConfigList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      key: filterForm.key,
      category: filterForm.category
    })
    
    configList.value = response.records.map(item => ({
      ...item,
      editing: false,
      tempValue: item.value
    }))
    pagination.total = response.total
  } catch (error) {
    console.error('加载配置列表失败:', error)
    ElMessage.error('加载配置列表失败')
    
    // 使用模拟数据
    configList.value = [
      {
        id: '1',
        key: 'system.title',
        value: '接口平台管理系统',
        description: '系统标题',
        type: 'string',
        category: 'system',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: '接口平台管理系统'
      },
      {
        id: '2',
        key: 'system.version',
        value: '1.0.0',
        description: '系统版本号',
        type: 'string',
        category: 'system',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: '1.0.0'
      },
      {
        id: '3',
        key: 'interface.timeout',
        value: '30000',
        description: '接口超时时间(毫秒)',
        type: 'number',
        category: 'interface',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: '30000'
      },
      {
        id: '4',
        key: 'security.login.max_attempts',
        value: '5',
        description: '登录最大尝试次数',
        type: 'number',
        category: 'security',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: '5'
      },
      {
        id: '5',
        key: 'security.session.timeout',
        value: '7200',
        description: '会话超时时间(秒)',
        type: 'number',
        category: 'security',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: '7200'
      },
      {
        id: '6',
        key: 'email.smtp.host',
        value: 'smtp.example.com',
        description: 'SMTP服务器地址',
        type: 'string',
        category: 'email',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: 'smtp.example.com'
      },
      {
        id: '7',
        key: 'email.smtp.port',
        value: '587',
        description: 'SMTP服务器端口',
        type: 'number',
        category: 'email',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: '587'
      },
      {
        id: '8',
        key: 'email.smtp.username',
        value: 'noreply@example.com',
        description: 'SMTP用户名',
        type: 'string',
        category: 'email',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: 'noreply@example.com'
      },
      {
        id: '9',
        key: 'email.smtp.password',
        value: 'password123',
        description: 'SMTP密码',
        type: 'password',
        category: 'email',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: 'password123'
      },
      {
        id: '10',
        key: 'system.maintenance.enabled',
        value: 'false',
        description: '维护模式开关',
        type: 'boolean',
        category: 'system',
        updateTime: '2024-01-15 10:30:00',
        updateBy: 'admin',
        editing: false,
        tempValue: 'false'
      }
    ]
    pagination.total = configList.value.length
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.key = ''
  filterForm.category = ''
  loadConfigList()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadConfigList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadConfigList()
}

const viewConfig = (config: ConfigItem) => {
  selectedConfig.value = config
  detailDialogVisible.value = true
}

const editConfig = (config: ConfigItem) => {
  config.editing = true
  config.tempValue = config.value
}

const cancelEdit = (config: ConfigItem) => {
  config.editing = false
  config.tempValue = config.value
}

const saveConfig = async (config: ConfigItem) => {
  try {
    // 验证输入
    if (!config.tempValue?.trim()) {
      ElMessage.error('配置值不能为空')
      return
    }
    
    if (config.type === 'number' && isNaN(Number(config.tempValue))) {
      ElMessage.error('请输入有效的数字')
      return
    }
    
    if (config.type === 'boolean' && !['true', 'false'].includes(config.tempValue)) {
      ElMessage.error('布尔值只能是 true 或 false')
      return
    }
    
    await ElMessageBox.confirm(
      `确定要修改配置项 "${config.key}" 吗？`,
      '修改配置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const updateData: UpdateConfigRequest = {
      id: config.id,
      value: config.tempValue
    }
    
    await systemApi.updateConfig(updateData)
    
    config.value = config.tempValue
    config.editing = false
    config.updateTime = new Date().toLocaleString()
    
    ElMessage.success('配置更新成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新配置失败:', error)
      ElMessage.error('更新配置失败')
    }
  }
}

// 生命周期
onMounted(() => {
  loadConfigList()
})
</script>

<style scoped>
.system-config {
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

.config-table {
  margin-bottom: 20px;
}

.config-value {
  max-width: 200px;
  word-break: break-all;
}

.config-value-detail {
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
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
  
  .config-value {
    max-width: 150px;
  }
}
</style>