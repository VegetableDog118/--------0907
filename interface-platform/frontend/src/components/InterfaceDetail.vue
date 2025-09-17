<template>
  <div class="interface-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="detail-header">
          <h3>{{ interfaceData?.interfaceName }}</h3>
          <div class="header-actions">
            <el-tag :type="getStatusTagType(interfaceData?.status)">{{ getStatusText(interfaceData?.status) }}</el-tag>
            <el-button type="primary" @click="handleSubscribe" v-if="!isSubscribed">
              <el-icon><Plus /></el-icon>
              订阅接口
            </el-button>
            <el-button type="success" disabled v-else>
              <el-icon><Check /></el-icon>
              已订阅
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="接口名称">{{ interfaceData?.interfaceName }}</el-descriptions-item>
        <el-descriptions-item label="接口路径">{{ interfaceData?.interfacePath }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">
          <el-tag :type="getMethodTagType(interfaceData?.requestMethod)">{{ interfaceData?.requestMethod }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="接口版本">{{ interfaceData?.version }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ interfaceData?.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="超时时间">{{ interfaceData?.timeout }}秒</el-descriptions-item>
        <el-descriptions-item label="限流配置">
          {{ interfaceData?.rateLimit ? `${interfaceData.rateLimit}次/分钟` : '无限制' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(interfaceData?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ interfaceData?.description || '暂无描述' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 参数信息 -->
      <el-divider content-position="left">
        <h4>参数信息</h4>
      </el-divider>
      
      <el-table :data="parameters" border>
        <el-table-column prop="paramName" label="参数名" width="150" />
        <el-table-column prop="paramType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getParamTypeTagType(row.paramType)">{{ row.paramType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paramLocation" label="位置" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getLocationTagType(row.paramLocation)">{{ row.paramLocation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="required" label="必需" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.required ? 'danger' : 'info'">
              {{ row.required ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="example" label="示例" width="150" show-overflow-tooltip />
        <el-table-column prop="defaultValue" label="默认值" width="120" show-overflow-tooltip />
      </el-table>

      <!-- 响应格式 -->
      <el-divider content-position="left">
        <h4>响应格式</h4>
      </el-divider>
      
      <el-card class="response-format">
        <pre><code>{{ formatResponseExample() }}</code></pre>
      </el-card>

      <!-- 接口测试 -->
      <el-divider content-position="left">
        <h4>接口测试</h4>
      </el-divider>
      
      <el-card class="test-section">
        <el-form :model="testForm" label-width="120px">
          <el-form-item
            v-for="param in parameters.filter(p => p.required)"
            :key="param.id"
            :label="param.paramName"
            :required="param.required"
          >
            <el-input
              v-model="testForm[param.paramName]"
              :placeholder="param.example || `请输入${param.paramName}`"
              :type="getInputType(param.paramType)"
            />
            <div class="param-hint">{{ param.description }}</div>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="testInterface" :loading="testLoading">
              <el-icon><CaretRight /></el-icon>
              测试接口
            </el-button>
            <el-button @click="resetTestForm">重置</el-button>
          </el-form-item>
        </el-form>
        
        <el-divider v-if="testResult" />
        
        <div v-if="testResult" class="test-result">
          <h5>测试结果：</h5>
          <el-card>
            <pre><code>{{ JSON.stringify(testResult, null, 2) }}</code></pre>
          </el-card>
        </div>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Plus, Check, CaretRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInterfaceDetail, getInterfaceParameters, testInterfaceCall, type InterfaceInfo, type InterfaceParameter } from '@/api/interface'
import { submitSubscriptionApplication } from '@/api/approval'
import { useUserStore } from '@/stores/user'

interface Props {
  interfaceId: string
  isSubscribed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isSubscribed: false
})

const emit = defineEmits<{
  subscribe: [interfaceId: string]
}>()

const userStore = useUserStore()
const loading = ref(false)
const testLoading = ref(false)
const interfaceData = ref<InterfaceInfo | null>(null)
const parameters = ref<InterfaceParameter[]>([])
const testForm = reactive<Record<string, any>>({})
const testResult = ref<any>(null)

// 计算属性
const getStatusTagType = (status?: string) => {
  const typeMap: Record<string, string> = {
    published: 'success',
    unpublished: 'info',
    offline: 'danger'
  }
  return typeMap[status || ''] || 'info'
}

const getStatusText = (status?: string) => {
  const textMap: Record<string, string> = {
    published: '已上架',
    unpublished: '未上架',
    offline: '已下架'
  }
  return textMap[status || ''] || '未知'
}

const getMethodTagType = (method?: string) => {
  const typeMap: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger'
  }
  return typeMap[method || ''] || 'info'
}

const getParamTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    string: 'primary',
    integer: 'success',
    number: 'success',
    boolean: 'warning',
    date: 'info',
    datetime: 'info'
  }
  return typeMap[type] || 'info'
}

const getLocationTagType = (location: string) => {
  const typeMap: Record<string, string> = {
    query: 'primary',
    body: 'success',
    header: 'warning',
    path: 'danger'
  }
  return typeMap[location] || 'info'
}

const getInputType = (paramType: string) => {
  const typeMap: Record<string, string> = {
    integer: 'number',
    number: 'number',
    date: 'date',
    datetime: 'datetime-local'
  }
  return typeMap[paramType] || 'text'
}

// 方法
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatResponseExample = () => {
  const example = {
    success: true,
    code: 200,
    message: '请求成功',
    data: {
      // 根据接口返回的实际数据结构
    },
    timestamp: new Date().toISOString()
  }
  return JSON.stringify(example, null, 2)
}

const loadInterfaceDetail = async () => {
  if (!props.interfaceId) return
  
  loading.value = true
  try {
    const response = await getInterfaceDetail(props.interfaceId)
    interfaceData.value = response.interface
    parameters.value = response.parameters
    
    // 初始化测试表单
    initTestForm()
  } catch (error) {
    console.error('加载接口详情失败:', error)
    ElMessage.error('加载接口详情失败')
  } finally {
    loading.value = false
  }
}

const initTestForm = () => {
  parameters.value.forEach(param => {
    if (param.defaultValue) {
      testForm[param.paramName] = param.defaultValue
    }
  })
}

const handleSubscribe = async () => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入申请理由', '订阅申请', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '请详细说明申请该接口的业务需求和使用场景',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim().length < 10) {
          return '申请理由不能少于10个字符'
        }
        return true
      }
    })
    
    await submitSubscriptionApplication({
      interfaceIds: [props.interfaceId],
      reason: reason as string
    })
    
    ElMessage.success('申请已提交，请等待审批')
    emit('subscribe', props.interfaceId)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交申请失败:', error)
      ElMessage.error('提交申请失败')
    }
  }
}

const testInterface = async () => {
  testLoading.value = true
  try {
    const result = await testInterfaceCall(props.interfaceId, testForm)
    testResult.value = result
    ElMessage.success('接口测试成功')
  } catch (error) {
    console.error('接口测试失败:', error)
    ElMessage.error('接口测试失败')
    testResult.value = { error: '测试失败', details: error }
  } finally {
    testLoading.value = false
  }
}

const resetTestForm = () => {
  Object.keys(testForm).forEach(key => {
    delete testForm[key]
  })
  initTestForm()
  testResult.value = null
}

// 生命周期
onMounted(() => {
  loadInterfaceDetail()
})
</script>

<style scoped>
.interface-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-header h3 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.response-format {
  background-color: #f8f9fa;
}

.response-format pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
}

.test-section {
  margin-top: 20px;
}

.param-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.test-result {
  margin-top: 20px;
}

.test-result h5 {
  margin: 0 0 12px 0;
  color: #409eff;
}

.test-result pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .header-actions {
    justify-content: center;
  }
}
</style>