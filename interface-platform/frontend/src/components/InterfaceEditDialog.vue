<template>
  <el-dialog
    v-model="visible"
    title="编辑接口"
    width="800px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="edit-container">
      <el-form
        :model="formData"
        :rules="formRules"
        ref="formRef"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="接口名称" prop="interfaceName" required>
              <el-input v-model="formData.interfaceName" placeholder="请输入接口名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接口路径" prop="interfacePath" required>
              <el-input v-model="formData.interfacePath" placeholder="如：/api/v1/data/query" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="业务分类" prop="categoryId" required>
              <el-select v-model="formData.categoryId" placeholder="请选择业务分类" style="width: 100%">
                <el-option label="日前现货" value="day_ahead_spot" />
                <el-option label="预测" value="forecast" />
                <el-option label="辅助服务" value="ancillary_service" />
                <el-option label="电网运行" value="grid_operation" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求方法" prop="requestMethod">
              <el-radio-group v-model="formData.requestMethod">
                <el-radio label="GET">GET</el-radio>
                <el-radio label="POST">POST</el-radio>
                <el-radio label="PUT">PUT</el-radio>
                <el-radio label="DELETE">DELETE</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="接口描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入接口描述"
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="限流配置" prop="rateLimit">
              <el-input-number
                v-model="formData.rateLimit"
                :min="1"
                :max="10000"
                placeholder="每分钟请求数"
                style="width: 100%"
              />
              <div class="form-tip">每分钟最大请求次数</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时时间" prop="timeout">
              <el-input-number
                v-model="formData.timeout"
                :min="1"
                :max="300"
                placeholder="秒"
                style="width: 100%"
              />
              <div class="form-tip">接口超时时间（秒）</div>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="SQL模板" prop="sqlTemplate">
          <el-input
            v-model="formData.sqlTemplate"
            type="textarea"
            :rows="6"
            placeholder="请输入SQL模板"
            class="sql-input"
          />
        </el-form-item>
      </el-form>
      
      <!-- 参数配置 -->
      <div class="param-section">
        <div class="section-header">
          <h4>接口参数</h4>
          <el-button type="primary" size="small" @click="addParameter">
            <el-icon><Plus /></el-icon>
            添加参数
          </el-button>
        </div>
        
        <el-table :data="formData.parameters" class="param-table">
          <el-table-column label="参数名" width="150">
            <template #default="{ row, $index }">
              <el-input v-model="row.paramName" size="small" placeholder="参数名" />
            </template>
          </el-table-column>
          
          <el-table-column label="参数类型" width="120">
            <template #default="{ row, $index }">
              <el-select v-model="row.paramType" size="small" style="width: 100%">
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
              <el-select v-model="row.paramLocation" size="small" style="width: 100%">
                <el-option label="查询参数" value="query" />
                <el-option label="请求体" value="body" />
                <el-option label="路径参数" value="path" />
                <el-option label="请求头" value="header" />
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
                type="danger"
                size="small"
                @click="removeParameter($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as interfaceApi from '@/api/interface'

// Props & Emits
interface Props {
  modelValue: boolean
  interfaceData?: any
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

const submitting = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = reactive({
  id: '',
  interfaceName: '',
  interfacePath: '',
  description: '',
  categoryId: '',
  requestMethod: 'POST',
  rateLimit: 1000,
  timeout: 30,
  sqlTemplate: '',
  parameters: [] as any[]
})

// 表单验证规则
const formRules: FormRules = {
  interfaceName: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  interfacePath: [{ required: true, message: '请输入接口路径', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择业务分类', trigger: 'change' }]
}

// 监听接口数据变化
watch(
  () => props.interfaceData,
  (newData) => {
    if (newData && visible.value) {
      loadInterfaceData(newData)
    }
  },
  { immediate: true }
)

// 监听弹窗显示状态
watch(visible, (newVisible) => {
  if (newVisible && props.interfaceData) {
    loadInterfaceData(props.interfaceData)
  }
})

// 方法
const loadInterfaceData = async (data: any) => {
  try {
    // 如果传入的是接口ID，则获取详细信息
    let interfaceDetail
    if (typeof data === 'string') {
      const response = await interfaceApi.getInterfaceDetail(data)
      interfaceDetail = response.interface
    } else {
      interfaceDetail = data
    }
    
    // 填充表单数据
    Object.assign(formData, {
      id: interfaceDetail.id,
      interfaceName: interfaceDetail.interfaceName || interfaceDetail.name,
      interfacePath: interfaceDetail.interfacePath || interfaceDetail.path,
      description: interfaceDetail.description || '',
      categoryId: interfaceDetail.categoryId || interfaceDetail.category,
      requestMethod: interfaceDetail.requestMethod || 'POST',
      rateLimit: interfaceDetail.rateLimit || 1000,
      timeout: interfaceDetail.timeout || 30,
      sqlTemplate: interfaceDetail.sqlTemplate || ''
    })
    
    // 获取参数信息
    if (typeof data === 'string') {
      const parameters = await interfaceApi.getInterfaceParameters(data)
      formData.parameters = parameters.map(param => ({
        id: param.id,
        paramName: param.paramName,
        paramType: param.paramType,
        paramLocation: param.paramLocation,
        required: param.required,
        description: param.description || '',
        defaultValue: param.defaultValue || ''
      }))
    } else {
      formData.parameters = []
    }
  } catch (error) {
    console.error('加载接口数据失败:', error)
    ElMessage.error('加载接口数据失败')
  }
}

const addParameter = () => {
  formData.parameters.push({
    paramName: '',
    paramType: 'string',
    paramLocation: 'body',
    required: false,
    description: '',
    defaultValue: ''
  })
}

const removeParameter = (index: number) => {
  formData.parameters.splice(index, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    await interfaceApi.updateInterface({
      id: formData.id,
      interfaceName: formData.interfaceName,
      interfacePath: formData.interfacePath,
      description: formData.description,
      categoryId: formData.categoryId,
      requestMethod: formData.requestMethod as 'GET' | 'POST' | 'PUT' | 'DELETE',
      rateLimit: formData.rateLimit,
      timeout: formData.timeout,
      sqlTemplate: formData.sqlTemplate,
      parameters: formData.parameters.map(param => ({
        paramName: param.paramName,
        paramType: param.paramType,
        paramLocation: param.paramLocation,
        required: param.required,
        description: param.description,
        defaultValue: param.defaultValue
      }))
    })
    
    ElMessage.success('接口更新成功')
    emit('success')
    handleClose()
  } catch (error) {
    console.error('接口更新失败:', error)
    ElMessage.error('接口更新失败')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  visible.value = false
  // 重置表单
  Object.assign(formData, {
    id: '',
    interfaceName: '',
    interfacePath: '',
    description: '',
    categoryId: '',
    requestMethod: 'POST',
    rateLimit: 1000,
    timeout: 30,
    sqlTemplate: '',
    parameters: []
  })
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.edit-container {
  max-height: 600px;
  overflow-y: auto;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.sql-input {
  font-family: 'Courier New', monospace;
}

.param-section {
  margin-top: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.section-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.param-table {
  width: 100%;
}

.dialog-footer {
  text-align: right;
}
</style>