<template>
  <el-dialog
    v-model="visible"
    title="接口生成向导"
    width="900px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="wizard-container">
      <!-- 步骤条 -->
      <el-steps :active="currentStep" align-center class="wizard-steps">
        <el-step title="MySQL表选择" description="从MySQL数据库中选择结构化表" />
        <el-step title="接口配置" description="配置接口基本信息" />
        <el-step title="参数设置" description="设置接口参数" />
        <el-step title="预览确认" description="预览并确认生成" />
      </el-steps>

      <!-- 步骤内容 -->
      <div class="wizard-content">
        <!-- 第一步：MySQL表选择 -->
        <div v-if="currentStep === 0" class="step-content">
          <h4>MySQL表选择</h4>
          <p class="step-description">从MySQL数据库中选择结构化表生成接口</p>
          
          <!-- MySQL表列表 -->
          <el-table 
            :data="mysqlTables" 
            @selection-change="handleTableSelection"
            v-loading="loadingTables"
            class="mysql-table-list">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="tableName" label="表名" width="200" />
            <el-table-column prop="tableComment" label="表说明" />
            <el-table-column prop="tableType" label="表类型" width="120">
              <template #default="scope">
                <el-tag :type="getTableTypeColor(scope.row.tableType)">
                  {{ scope.row.tableType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recordCount" label="记录数" width="100" />
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button 
                  type="text" 
                  @click="previewTableStructure(scope.row)"
                  size="small">
                  预览结构
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 表结构预览弹窗 -->
          <el-dialog 
            v-model="showTablePreview" 
            title="表结构预览" 
            width="800px">
            <el-table :data="selectedTableColumns">
              <el-table-column prop="columnName" label="字段名" />
              <el-table-column prop="dataType" label="数据类型" />
              <el-table-column prop="columnComment" label="字段说明" />
              <el-table-column prop="isPrimaryKey" label="主键" width="80">
                <template #default="scope">
                  <el-icon v-if="scope.row.isPrimaryKey" color="#409EFF">
                    <Key />
                  </el-icon>
                </template>
              </el-table-column>
            </el-table>
          </el-dialog>
        </div>

        <!-- 第二步：接口配置 -->
        <div v-if="currentStep === 1" class="step-content">
          <h4>接口基本配置</h4>
          <el-form :model="wizardData.interface" :rules="interfaceRules" ref="interfaceFormRef" label-width="120px">
            <el-form-item label="接口名称" prop="name" required>
              <el-input v-model="wizardData.interface.name" placeholder="请输入接口名称" />
            </el-form-item>
            
            <el-form-item label="接口路径" prop="path" required>
              <el-input v-model="wizardData.interface.path" placeholder="接口路径将自动生成" readonly>
                <template #prepend>/px-phzhb-external-share/dataproduct/</template>
              </el-input>
              <div class="form-tip">路径将根据接口名称和业务分类自动生成</div>
            </el-form-item>
            
            <el-form-item label="接口描述" prop="description">
              <el-input
                v-model="wizardData.interface.description"
                type="textarea"
                :rows="3"
                placeholder="请输入接口描述"
              />
            </el-form-item>
            
            <el-form-item label="业务分类" prop="categoryId" required>
              <el-select v-model="wizardData.interface.categoryId" placeholder="请选择业务分类" style="width: 100%" @change="handleCategoryChange">
                <el-option label="日前现货" value="day_ahead_spot">
                  <span style="color: #1890ff;">●</span> 日前现货
                </el-option>
                <el-option label="预测" value="forecast">
                  <span style="color: #52c41a;">●</span> 预测
                </el-option>
                <el-option label="辅助服务" value="ancillary_service">
                  <span style="color: #faad14;">●</span> 辅助服务
                </el-option>
                <el-option label="电网运行" value="grid_operation">
                  <span style="color: #f5222d;">●</span> 电网运行
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="请求方法" prop="method">
              <el-radio-group v-model="wizardData.interface.method">
                <el-radio label="GET">GET</el-radio>
                <el-radio label="POST">POST</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="限流配置" prop="rateLimit">
              <el-input-number
                v-model="wizardData.interface.rateLimit"
                :min="1"
                :max="10000"
                placeholder="每分钟请求数"
              />
              <span class="form-tip">每分钟最大请求次数</span>
            </el-form-item>
            
            <el-form-item label="超时时间" prop="timeout">
              <el-input-number
                v-model="wizardData.interface.timeout"
                :min="1"
                :max="300"
                placeholder="秒"
              />
              <span class="form-tip">接口超时时间（秒）</span>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第三步：参数设置 -->
        <div v-if="currentStep === 2" class="step-content">
          <h4>接口参数配置</h4>
          
          <!-- 标准参数说明 -->
          <el-alert
            title="标准参数说明"
            type="info"
            :closable="false"
            style="margin-bottom: 16px;"
          >
            <template #default>
              所有接口必须包含以下标准参数：<br>
              • <strong>dataTime</strong>：查询日期，格式YYYY-MM-DD，必需参数<br>
              • <strong>appId</strong>：应用ID，用户身份标识，必需参数
            </template>
          </el-alert>
          
          <div class="param-config">
            <div class="param-header">
              <span>基于表字段自动生成参数，您可以调整参数配置：</span>
              <el-button type="primary" size="small" @click="addParameter">
                <el-icon><Plus /></el-icon>
                添加业务参数
              </el-button>
            </div>
            
            <el-table :data="wizardData.parameters" class="param-table">
              <el-table-column label="参数名" width="150">
                <template #default="{ row, $index }">
                  <el-input v-model="row.name" size="small" />
                </template>
              </el-table-column>
              
              <el-table-column label="参数类型" width="120">
                <template #default="{ row, $index }">
                  <el-select v-model="row.type" size="small">
                    <el-option label="字符串" value="string" />
                    <el-option label="整数" value="integer" />
                    <el-option label="数字" value="number" />
                    <el-option label="布尔" value="boolean" />
                    <el-option label="日期" value="date" />
                    <el-option label="日期时间" value="datetime" />
                  </el-select>
                </template>
              </el-table-column>
              
              <el-table-column label="参数位置" width="120">
                <template #default="{ row, $index }">
                  <el-select v-model="row.location" size="small">
                    <el-option label="查询参数" value="query" />
                    <el-option label="请求体" value="body" />
                    <el-option label="路径参数" value="path" />
                  </el-select>
                </template>
              </el-table-column>
              
              <el-table-column label="是否必需" width="100">
                <template #default="{ row, $index }">
                  <el-switch v-model="row.required" size="small" />
                </template>
              </el-table-column>
              
              <el-table-column label="描述">
                <template #default="{ row, $index }">
                  <el-input v-model="row.description" size="small" placeholder="参数描述" />
                </template>
              </el-table-column>
              
              <el-table-column label="默认值" width="120">
                <template #default="{ row, $index }">
                  <el-input v-model="row.defaultValue" size="small" placeholder="默认值" />
                </template>
              </el-table-column>
              
              <el-table-column label="操作" width="80">
                <template #default="{ row, $index }">
                  <el-button
                    v-if="!row.isStandard"
                    type="danger"
                    size="small"
                    @click="removeParameter($index)"
                  >
                    删除
                  </el-button>
                  <el-tag v-else type="warning" size="small">标准参数</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 第四步：预览确认 -->
        <div v-if="currentStep === 3" class="step-content">
          <h4>预览确认</h4>
          <div class="preview-content">
            <el-descriptions title="接口信息" :column="2" border>
              <el-descriptions-item label="接口名称">{{ wizardData.interface.name }}</el-descriptions-item>
              <el-descriptions-item label="接口路径">{{ wizardData.interface.path }}</el-descriptions-item>
              <el-descriptions-item label="请求方法">{{ wizardData.interface.method }}</el-descriptions-item>
              <el-descriptions-item label="业务分类">{{ getCategoryName(wizardData.interface.categoryId) }}</el-descriptions-item>
              <el-descriptions-item label="MySQL表">{{ wizardData.tableName }}</el-descriptions-item>
              <el-descriptions-item label="表类型">{{ getSelectedTableType() }}</el-descriptions-item>
              <el-descriptions-item label="限流配置">{{ wizardData.interface.rateLimit }} 次/分钟</el-descriptions-item>
              <el-descriptions-item label="超时时间">{{ wizardData.interface.timeout }} 秒</el-descriptions-item>
              <el-descriptions-item label="接口描述" :span="2">{{ wizardData.interface.description || '无' }}</el-descriptions-item>
            </el-descriptions>
            
            <h5 style="margin-top: 20px;">参数列表</h5>
            <el-table :data="wizardData.parameters" size="small">
              <el-table-column prop="name" label="参数名" />
              <el-table-column prop="type" label="类型" />
              <el-table-column prop="location" label="位置" />
              <el-table-column prop="required" label="必需">
                <template #default="{ row }">
                  <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                    {{ row.required ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" />
              <el-table-column prop="defaultValue" label="默认值" />
            </el-table>
            
            <!-- SQL预览 -->
            <h5 style="margin-top: 20px;">生成的SQL模板</h5>
            <el-input
              v-model="generatedSql"
              type="textarea"
              :rows="6"
              readonly
              class="sql-preview"
            />
            
            <!-- 接口路径预览 -->
            <h5 style="margin-top: 20px;">完整接口路径</h5>
            <el-input
              :value="`/px-phzhb-external-share/dataproduct/${wizardData.interface.path}`"
              readonly
              class="path-preview"
            />
            
            <!-- MySQL表信息 -->
            <h5 style="margin-top: 20px;">MySQL表信息</h5>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="表名">{{ wizardData.tableName }}</el-descriptions-item>
              <el-descriptions-item label="表说明">{{ getSelectedTableComment() }}</el-descriptions-item>
              <el-descriptions-item label="表类型">{{ getSelectedTableType() }}</el-descriptions-item>
              <el-descriptions-item label="记录数">{{ getSelectedTableRecordCount() }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="wizard-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentStep === 3" type="primary" @click="handleSubmit" :loading="submitting">
          生成接口
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Plus, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as interfaceApi from '@/api/interface'

// Props & Emits
interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const currentStep = ref(0)
const submitting = ref(false)
const loadingTables = ref(false)
const interfaceFormRef = ref<FormInstance>()
const showTablePreview = ref(false)

// MySQL表相关
const mysqlTables = ref<any[]>([])
const selectedTable = ref<any>(null)
const selectedTableColumns = ref<any[]>([])

// 向导数据
const wizardData = reactive({
  tableName: '',
  interface: {
    name: '',
    path: '',
    description: '',
    categoryId: '',
    method: 'POST',
    rateLimit: 1000,
    timeout: 30
  },
  parameters: [
    // 标准参数 - 不可删除
    {
      name: 'dataTime',
      type: 'date',
      location: 'query',
      required: true,
      description: '查询日期，格式YYYY-MM-DD',
      defaultValue: '',
      isStandard: true
    },
    {
      name: 'appId',
      type: 'string',
      location: 'query',
      required: true,
      description: '应用ID，用户身份标识',
      defaultValue: '',
      isStandard: true
    }
  ]
})

// 表单验证规则
const interfaceRules: FormRules = {
  name: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入接口路径', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择业务分类', trigger: 'change' }]
}

// 计算属性
const generatedSql = computed(() => {
  if (!wizardData.tableName) return ''
  
  const tableName = wizardData.tableName
  const params = wizardData.parameters.filter(p => p.name)
  
  let sql = `SELECT * FROM ${tableName}`
  
  if (params.length > 0) {
    const conditions = params
      .filter(p => p.location === 'query' || p.location === 'body')
      .map(p => `${p.name} = #{${p.name}}`)
      .join(' AND ')
    
    if (conditions) {
      sql += ` WHERE ${conditions}`
    }
  }
  
  return sql
})

// 方法
const handleTableSelection = (selection: any[]) => {
  if (selection.length > 0) {
    selectedTable.value = selection[0]
    wizardData.tableName = selectedTable.value.tableName
  } else {
    selectedTable.value = null
    wizardData.tableName = ''
  }
}

const getTableTypeColor = (tableType: string) => {
  const colorMap: Record<string, string> = {
    '24小时表': 'primary',
    '288点表': 'success',
    '设备表': 'warning',
    '统计表': 'info'
  }
  return colorMap[tableType] || 'info'
}

const previewTableStructure = async (table: any) => {
  try {
    const response = await interfaceApi.getTableStructure(table.tableName)
    
    // 检查响应数据格式并提取 columns
    if (Array.isArray(response)) {
      selectedTableColumns.value = response
    } else if (response && response.columns && Array.isArray(response.columns)) {
      selectedTableColumns.value = response.columns
    } else {
      console.error('获取表结构失败: 响应数据格式不正确', response)
      selectedTableColumns.value = []
    }
    
    showTablePreview.value = true
  } catch (error) {
    console.error('获取表结构失败:', error)
    ElMessage.error('获取表结构失败')
    
    // 降级到模拟数据
    selectedTableColumns.value = [
      { columnName: 'id', dataType: 'varchar(32)', columnComment: 'ID', isPrimaryKey: true },
      { columnName: 'data_time', dataType: 'datetime', columnComment: '数据时间', isPrimaryKey: false },
      { columnName: 'value', dataType: 'decimal(10,2)', columnComment: '数值', isPrimaryKey: false }
    ]
    showTablePreview.value = true
  }
}

const loadTableParameters = async () => {
  if (!wizardData.tableName) return
  
  try {
    const response = await interfaceApi.getTableStructure(wizardData.tableName)
    
    // 检查响应数据格式
    let columns = []
    if (Array.isArray(response)) {
      // 如果直接返回数组
      columns = response
    } else if (response && response.columns && Array.isArray(response.columns)) {
      // 如果返回的是 TableStructureInfo 对象
      columns = response.columns
    } else {
      console.error('获取表结构失败: 响应数据格式不正确', response)
      ElMessage.error('获取表结构失败: 数据格式错误')
      return
    }
    
    // 保留标准参数，添加表字段参数
    const standardParams = wizardData.parameters.filter(p => p.isStandard)
    const tableParams = columns.map((col: any) => ({
      name: col.columnName,
      type: mapColumnTypeToParamType(col.dataType),
      location: 'body',
      required: !col.isNullable,
      description: col.columnComment || '',
      defaultValue: '',
      isStandard: false
    }))
    
    wizardData.parameters = [...standardParams, ...tableParams]
  } catch (error) {
    console.error('获取表结构失败:', error)
    ElMessage.error('获取表结构失败')
  }
}

const mapColumnTypeToParamType = (columnType: string): string => {
  if (columnType.includes('int')) return 'integer'
  if (columnType.includes('decimal') || columnType.includes('float')) return 'number'
  if (columnType.includes('date')) return columnType.includes('time') ? 'datetime' : 'date'
  if (columnType.includes('bool')) return 'boolean'
  return 'string'
}

const addParameter = () => {
  wizardData.parameters.push({
    name: '',
    type: 'string',
    location: 'body',
    required: false,
    description: '',
    defaultValue: '',
    isStandard: false
  })
}

const removeParameter = (index: number) => {
  // 不允许删除标准参数
  if (wizardData.parameters[index].isStandard) {
    ElMessage.warning('标准参数不可删除')
    return
  }
  wizardData.parameters.splice(index, 1)
}

const getCategoryName = (categoryId: string): string => {
  const nameMap: Record<string, string> = {
    day_ahead_spot: '日前现货',
    forecast: '预测',
    ancillary_service: '辅助服务',
    grid_operation: '电网运行'
  }
  return nameMap[categoryId] || categoryId
}

const getSelectedTableType = (): string => {
  return selectedTable.value?.tableType || '未知'
}

const getSelectedTableComment = (): string => {
  return selectedTable.value?.tableComment || '无说明'
}

const getSelectedTableRecordCount = (): string => {
  return selectedTable.value?.recordCount?.toLocaleString() || '0'
}

// 生成接口路径
const generateInterfacePath = () => {
  if (!wizardData.interface.name || !wizardData.interface.categoryId) {
    return ''
  }
  
  // 将接口名称转换为路径格式
  const pathName = wizardData.interface.name
    .toLowerCase()
    .replace(/[\s\u4e00-\u9fa5]+/g, '-') // 替换空格和中文为连字符
    .replace(/[^a-z0-9-]/g, '') // 移除非字母数字和连字符的字符
    .replace(/-+/g, '-') // 合并多个连字符
    .replace(/^-|-$/g, '') // 移除开头和结尾的连字符
  
  // 根据业务分类生成路径
  const categoryPaths: Record<string, string> = {
    day_ahead_spot: 'spot',
    forecast: 'forecast', 
    ancillary_service: 'ancillary',
    grid_operation: 'grid'
  }
  
  const categoryPath = categoryPaths[wizardData.interface.categoryId] || 'common'
  return `${categoryPath}/${pathName}`
}

// 处理分类变更
const handleCategoryChange = () => {
  // 自动生成路径
  wizardData.interface.path = generateInterfacePath()
}

// 监听接口名称变化，自动生成路径
watch(() => wizardData.interface.name, () => {
  wizardData.interface.path = generateInterfacePath()
})

// 初始化MySQL表列表
const initMysqlTables = async () => {
  loadingTables.value = true
  try {
    const response = await interfaceApi.getMysqlTables()
    mysqlTables.value = response
  } catch (error) {
    console.error('获取MySQL表列表失败:', error)
    ElMessage.error('获取MySQL表列表失败')
    
    // 降级到模拟数据
    mysqlTables.value = [
      { tableName: 'px_market_member', tableComment: '市场成员表', tableType: '统计表', recordCount: 1250 },
      { tableName: 'px_trading_data_24h', tableComment: '24小时交易数据表', tableType: '24小时表', recordCount: 8760 },
      { tableName: 'px_forecast_data_288', tableComment: '288点预测数据表', tableType: '288点表', recordCount: 105120 },
      { tableName: 'px_device_info', tableComment: '设备信息表', tableType: '设备表', recordCount: 500 }
    ]
  } finally {
    loadingTables.value = false
  }
}

// 组件挂载时初始化MySQL表
watch(visible, (newVal) => {
  if (newVal) {
    initMysqlTables()
  }
})

// 监听表选择变化，自动加载参数
watch(() => wizardData.tableName, (newTableName) => {
  if (newTableName) {
    loadTableParameters()
  }
})

const nextStep = async () => {
  // 验证当前步骤
  if (currentStep.value === 0) {
    if (!wizardData.tableName) {
      ElMessage.warning('请选择MySQL表')
      return
    }
  } else if (currentStep.value === 1) {
    if (!interfaceFormRef.value) return
    const valid = await interfaceFormRef.value.validate().catch(() => false)
    if (!valid) return
    
    // 步骤2：验证接口配置
    try {
      const config = {
        interfaceName: wizardData.interface.name,
        interfacePath: `/px-phzhb-external-share/dataproduct/${wizardData.interface.path}`,
        description: wizardData.interface.description,
        categoryId: wizardData.interface.categoryId,
        requestMethod: wizardData.interface.method as 'GET' | 'POST',
        rateLimit: wizardData.interface.rateLimit,
        timeout: wizardData.interface.timeout
      }
      
      await interfaceApi.validateInterfaceConfig(config)
    } catch (error) {
      console.error('接口配置验证失败:', error)
      ElMessage.error('接口配置验证失败，请检查配置信息')
      return
    }
  } else if (currentStep.value === 2) {
    // 步骤3：生成参数模板（可选验证）
    const hasBusinessParams = wizardData.parameters.some(p => !p.isStandard && p.name)
    if (!hasBusinessParams) {
      ElMessage.warning('请至少添加一个业务参数')
      return
    }
  }
  
  currentStep.value++
}

const prevStep = () => {
  currentStep.value--
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    // 构建接口生成请求
    const request = {
      tableName: wizardData.tableName,
      interfaceConfig: {
        name: wizardData.interface.name,
        description: wizardData.interface.description,
        businessType: wizardData.interface.categoryId,
        method: wizardData.interface.method,
        rateLimit: wizardData.interface.rateLimit,
        timeout: wizardData.interface.timeout
      },
      parameters: wizardData.parameters.map(param => ({
        name: param.name,
        type: param.type,
        location: param.location,
        required: param.required,
        description: param.description,
        defaultValue: param.defaultValue,
        isStandard: param.isStandard
      }))
    }
    
    // 调用接口生成API
    const interfaceId = await interfaceApi.generateInterface(request)
    
    ElMessage.success(`接口生成成功！接口ID: ${interfaceId}，当前状态：未上架`)
    emit('success')
    handleClose()
  } catch (error) {
    console.error('接口生成失败:', error)
    
    // 提取详细错误信息
    let errorMessage = '接口生成失败'
    if (error && error.message) {
      errorMessage = `接口生成失败: ${error.message}`
    } else if (error && typeof error === 'string') {
      errorMessage = `接口生成失败: ${error}`
    }
    
    ElMessage.error({
      message: errorMessage,
      duration: 5000, // 显示5秒，让用户有足够时间阅读错误信息
      showClose: true
    })
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  visible.value = false
  // 重置数据
  currentStep.value = 0
  wizardData.tableName = ''
  Object.assign(wizardData.interface, {
    name: '',
    path: '',
    description: '',
    categoryId: '',
    method: 'POST',
    rateLimit: 1000,
    timeout: 30
  })
  // 重置参数时保留标准参数
  wizardData.parameters = [
    {
      name: 'dataTime',
      type: 'date',
      location: 'query',
      required: true,
      description: '查询日期，格式YYYY-MM-DD',
      defaultValue: '',
      isStandard: true
    },
    {
      name: 'appId',
      type: 'string',
      location: 'query',
      required: true,
      description: '应用ID，用户身份标识',
      defaultValue: '',
      isStandard: true
    }
  ]
  selectedTable.value = null
  selectedTableColumns.value = []
  showTablePreview.value = false
}
</script>

<style scoped>
.wizard-container {
  padding: 20px 0;
}

.wizard-steps {
  margin-bottom: 30px;
}

.wizard-content {
  min-height: 400px;
  padding: 20px 0;
}

.step-content h4 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 16px;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.param-config {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.param-table {
  width: 100%;
}

.preview-content {
  max-height: 500px;
  overflow-y: auto;
}

.sql-preview {
  font-family: 'Courier New', monospace;
  background-color: #f5f7fa;
}

.path-preview {
  font-family: 'Courier New', monospace;
  background-color: #f0f9ff;
  color: #1890ff;
  font-weight: 600;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.param-table :deep(.el-input) {
  width: 100%;
}

.param-table :deep(.el-select) {
  width: 100%;
}

.wizard-steps :deep(.el-step__title) {
  font-size: 14px;
}

.wizard-steps :deep(.el-step__description) {
  font-size: 12px;
  color: #909399;
}

.wizard-footer {
  text-align: right;
}

.step-description {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.5;
}

.mysql-table-list {
  margin-top: 16px;
}

.mysql-table-list :deep(.el-table__header) {
  background-color: #f5f7fa;
}

.mysql-table-list :deep(.el-table__row:hover) {
  background-color: #f0f9ff;
}

.mysql-table-list :deep(.el-table__row.current-row) {
  background-color: #e6f7ff;
}
</style>