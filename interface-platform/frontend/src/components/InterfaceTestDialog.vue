<template>
  <el-dialog
    v-model="visible"
    title="接口测试"
    width="1000px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="test-container">
      <!-- 接口信息 -->
      <el-card class="interface-info">
        <template #header>
          <h4>接口信息</h4>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="接口名称">{{ interfaceInfo.interfaceName }}</el-descriptions-item>
          <el-descriptions-item label="接口路径">{{ interfaceInfo.interfacePath }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">
            <el-tag :type="getMethodTagType(interfaceInfo.requestMethod)">{{ interfaceInfo.requestMethod }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="超时时间">{{ interfaceInfo.timeout }}秒</el-descriptions-item>
          <el-descriptions-item label="接口描述" :span="2">{{ interfaceInfo.description || '无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 参数配置 -->
      <el-card class="param-config">
        <template #header>
          <h4>参数配置</h4>
        </template>
        
        <el-tabs v-model="activeTab">
          <!-- Query参数 -->
          <el-tab-pane label="Query参数" name="query" v-if="queryParams.length > 0">
            <div class="param-list">
              <div v-for="param in queryParams" :key="param.id" class="param-item">
                <div class="param-header">
                  <span class="param-name">{{ param.paramName }}</span>
                  <el-tag v-if="param.required" type="danger" size="small">必需</el-tag>
                  <el-tag :type="getParamTypeTag(param.paramType)" size="small">{{ param.paramType }}</el-tag>
                </div>
                <div class="param-description">{{ param.description || '无描述' }}</div>
                <el-input
                  v-model="testParams.query[param.paramName]"
                  :placeholder="param.defaultValue || `请输入${param.paramName}`"
                  class="param-input"
                />
              </div>
            </div>
          </el-tab-pane>

          <!-- Body参数 -->
          <el-tab-pane label="Body参数" name="body" v-if="bodyParams.length > 0">
            <div class="param-list">
              <div v-for="param in bodyParams" :key="param.id" class="param-item">
                <div class="param-header">
                  <span class="param-name">{{ param.paramName }}</span>
                  <el-tag v-if="param.required" type="danger" size="small">必需</el-tag>
                  <el-tag :type="getParamTypeTag(param.paramType)" size="small">{{ param.paramType }}</el-tag>
                </div>
                <div class="param-description">{{ param.description || '无描述' }}</div>
                <el-input
                  v-model="testParams.body[param.paramName]"
                  :placeholder="param.defaultValue || `请输入${param.paramName}`"
                  class="param-input"
                />
              </div>
            </div>
          </el-tab-pane>

          <!-- JSON格式 -->
          <el-tab-pane label="JSON格式" name="json">
            <div class="json-editor">
              <el-input
                v-model="jsonParams"
                type="textarea"
                :rows="10"
                placeholder="请输入JSON格式的参数"
                class="json-input"
              />
              <div class="json-actions">
                <el-button size="small" @click="formatJson">格式化</el-button>
                <el-button size="small" @click="generateSampleJson">生成示例</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <!-- 测试按钮 -->
      <div class="test-actions">
        <el-button type="primary" @click="handleTest" :loading="testing">
          <el-icon><CaretRight /></el-icon>
          发送请求
        </el-button>
        <el-button @click="handleReset">重置参数</el-button>
      </div>

      <!-- 响应结果 -->
      <el-card class="response-result" v-if="hasResponse">
        <template #header>
          <div class="response-header">
            <h4>响应结果</h4>
            <div class="response-status">
              <el-tag :type="getStatusTagType(responseData.status)">{{ responseData.status }}</el-tag>
              <span class="response-time">{{ responseData.time }}ms</span>
            </div>
          </div>
        </template>
        
        <el-tabs v-model="responseTab">
          <!-- 响应数据 -->
          <el-tab-pane label="响应数据" name="data">
            <div class="response-content">
              <pre class="json-content">{{ formatResponseData(responseData.data) }}</pre>
            </div>
          </el-tab-pane>

          <!-- 响应头 -->
          <el-tab-pane label="响应头" name="headers">
            <el-table :data="responseHeaders" size="small">
              <el-table-column prop="name" label="名称" width="200" />
              <el-table-column prop="value" label="值" />
            </el-table>
          </el-tab-pane>

          <!-- 请求信息 -->
          <el-tab-pane label="请求信息" name="request">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="请求URL">{{ requestInfo.url }}</el-descriptions-item>
              <el-descriptions-item label="请求方法">{{ requestInfo.method }}</el-descriptions-item>
              <el-descriptions-item label="请求参数">
                <pre class="json-content">{{ formatResponseData(requestInfo.params) }}</pre>
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { CaretRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as interfaceApi from '@/api/interface'

// Props & Emits
interface Props {
  modelValue: boolean
  interfaceId?: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const testing = ref(false)
const activeTab = ref('query')
const responseTab = ref('data')
const hasResponse = ref(false)

// 接口信息
const interfaceInfo = reactive({
  id: '',
  interfaceName: '',
  interfacePath: '',
  requestMethod: 'POST',
  timeout: 30,
  description: ''
})

// 参数信息
const parameters = ref<any[]>([])
const queryParams = computed(() => parameters.value.filter(p => p.paramLocation === 'query'))
const bodyParams = computed(() => parameters.value.filter(p => p.paramLocation === 'body'))

// 测试参数
const testParams = reactive({
  query: {} as Record<string, any>,
  body: {} as Record<string, any>
})

const jsonParams = ref('')

// 响应数据
const responseData = reactive({
  status: 0,
  time: 0,
  data: null as any
})

const responseHeaders = ref<Array<{ name: string; value: string }>>([])

const requestInfo = reactive({
  url: '',
  method: '',
  params: {} as any
})

// 监听接口ID变化
watch(
  () => props.interfaceId,
  (newId) => {
    if (newId && visible.value) {
      loadInterfaceData(newId)
    }
  },
  { immediate: true }
)

// 监听弹窗显示状态
watch(visible, (newVisible) => {
  if (newVisible && props.interfaceId) {
    loadInterfaceData(props.interfaceId)
  }
})

// 方法
const loadInterfaceData = async (interfaceId: string) => {
  try {
    const response = await interfaceApi.getInterfaceDetail(interfaceId)
    
    // 填充接口信息
    Object.assign(interfaceInfo, {
      id: response.interface.id,
      interfaceName: response.interface.interfaceName,
      interfacePath: response.interface.interfacePath,
      requestMethod: response.interface.requestMethod,
      timeout: response.interface.timeout,
      description: response.interface.description
    })
    
    // 填充参数信息
    parameters.value = response.parameters
    
    // 初始化测试参数
    resetTestParams()
    
    // 设置默认标签页
    if (queryParams.value.length > 0) {
      activeTab.value = 'query'
    } else if (bodyParams.value.length > 0) {
      activeTab.value = 'body'
    } else {
      activeTab.value = 'json'
    }
  } catch (error) {
    console.error('加载接口数据失败:', error)
    ElMessage.error('加载接口数据失败')
  }
}

const resetTestParams = () => {
  // 重置query参数
  testParams.query = {}
  queryParams.value.forEach(param => {
    testParams.query[param.paramName] = param.defaultValue || ''
  })
  
  // 重置body参数
  testParams.body = {}
  bodyParams.value.forEach(param => {
    testParams.body[param.paramName] = param.defaultValue || ''
  })
  
  // 生成示例JSON
  generateSampleJson()
}

const generateSampleJson = () => {
  const sampleData: Record<string, any> = {}
  
  parameters.value.forEach(param => {
    let sampleValue: any
    switch (param.paramType) {
      case 'integer':
        sampleValue = 123
        break
      case 'number':
        sampleValue = 123.45
        break
      case 'boolean':
        sampleValue = true
        break
      case 'date':
        sampleValue = '2024-01-15'
        break
      case 'datetime':
        sampleValue = '2024-01-15 10:30:00'
        break
      default:
        sampleValue = param.defaultValue || `示例${param.paramName}`
    }
    sampleData[param.paramName] = sampleValue
  })
  
  jsonParams.value = JSON.stringify(sampleData, null, 2)
}

const formatJson = () => {
  try {
    const parsed = JSON.parse(jsonParams.value)
    jsonParams.value = JSON.stringify(parsed, null, 2)
  } catch {
    ElMessage.error('JSON格式错误')
  }
}

const handleTest = async () => {
  testing.value = true
  const startTime = Date.now()
  
  try {
    // 准备测试参数
    let testData: any = {}
    
    if (activeTab.value === 'json') {
      try {
        testData = JSON.parse(jsonParams.value)
      } catch {
        ElMessage.error('JSON格式错误')
        return
      }
    } else {
      // 合并query和body参数
      testData = { ...testParams.query, ...testParams.body }
    }
    
    // 记录请求信息
    requestInfo.url = interfaceInfo.interfacePath
    requestInfo.method = interfaceInfo.requestMethod
    requestInfo.params = testData
    
    // 调用测试接口
    const response = await interfaceApi.testInterfaceCall(interfaceInfo.id, testData)
    
    const endTime = Date.now()
    
    // 设置响应数据
    responseData.status = 200
    responseData.time = endTime - startTime
    responseData.data = response
    
    // 模拟响应头
    responseHeaders.value = [
      { name: 'Content-Type', value: 'application/json' },
      { name: 'Content-Length', value: JSON.stringify(response).length.toString() },
      { name: 'Date', value: new Date().toISOString() }
    ]
    
    hasResponse.value = true
    responseTab.value = 'data'
    
    ElMessage.success('接口测试成功')
  } catch (error: any) {
    const endTime = Date.now()
    
    responseData.status = error.response?.status || 500
    responseData.time = endTime - startTime
    responseData.data = error.response?.data || { error: error.message }
    
    responseHeaders.value = [
      { name: 'Content-Type', value: 'application/json' },
      { name: 'Date', value: new Date().toISOString() }
    ]
    
    hasResponse.value = true
    responseTab.value = 'data'
    
    console.error('接口测试失败:', error)
    ElMessage.error('接口测试失败')
  } finally {
    testing.value = false
  }
}

const handleReset = () => {
  resetTestParams()
  hasResponse.value = false
  ElMessage.success('参数已重置')
}

const handleClose = () => {
  visible.value = false
  hasResponse.value = false
}

// 工具方法
const getMethodTagType = (method: string) => {
  const typeMap: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return typeMap[method] || 'info'
}

const getParamTypeTag = (type: string) => {
  const typeMap: Record<string, string> = {
    string: 'info',
    integer: 'success',
    number: 'success',
    boolean: 'warning',
    date: 'primary',
    datetime: 'primary'
  }
  return typeMap[type] || 'info'
}

const getStatusTagType = (status: number) => {
  if (status >= 200 && status < 300) return 'success'
  if (status >= 400 && status < 500) return 'warning'
  if (status >= 500) return 'danger'
  return 'info'
}

const formatResponseData = (data: any) => {
  if (typeof data === 'string') return data
  return JSON.stringify(data, null, 2)
}
</script>

<style scoped>
.test-container {
  max-height: 700px;
  overflow-y: auto;
}

.interface-info {
  margin-bottom: 20px;
}

.param-config {
  margin-bottom: 20px;
}

.param-list {
  max-height: 300px;
  overflow-y: auto;
}

.param-item {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.param-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.param-name {
  font-weight: 500;
  color: #303133;
}

.param-description {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.param-input {
  width: 100%;
}

.json-editor {
  position: relative;
}

.json-input {
  font-family: 'Courier New', monospace;
}

.json-actions {
  margin-top: 10px;
  text-align: right;
}

.test-actions {
  text-align: center;
  margin: 20px 0;
}

.response-result {
  margin-top: 20px;
}

.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.response-header h4 {
  margin: 0;
}

.response-status {
  display: flex;
  align-items: center;
  gap: 10px;
}

.response-time {
  font-size: 12px;
  color: #909399;
}

.response-content {
  max-height: 400px;
  overflow-y: auto;
}

.json-content {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.dialog-footer {
  text-align: right;
}
</style>