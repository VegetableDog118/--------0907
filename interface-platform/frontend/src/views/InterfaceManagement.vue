<template>
  <div class="interface-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>接口管理</h2>
        <p>管理系统中的所有接口，包括创建、编辑、删除等操作</p>
      </div>
      <div class="header-right">
        <!-- 技术部权限：生成接口 -->
        <template v-if="userStore.hasRole('tech')">
          <el-button type="primary" :icon="Plus" @click="handleAdd">生成接口</el-button>
        </template>
        
        <!-- 结算部权限：批量操作 -->
        <template v-if="userStore.hasRole('settlement')">
          <el-button 
            type="success" 
            :disabled="selectedRows.length === 0"
            @click="handleBatchPublish"
          >
            批量上架
          </el-button>
          <el-button 
            type="danger" 
            :disabled="selectedRows.length === 0"
            @click="handleBatchUnpublish"
          >
            批量下架
          </el-button>
        </template>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="接口名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入接口名称"
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item label="接口状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="用户管理" value="user" />
            <el-option label="数据查询" value="data" />
            <el-option label="系统配置" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 接口列表 -->
    <el-card class="table-card">
      <el-table 
          :data="tableData" 
          :loading="loading"
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <!-- 多选列 -->
          <el-table-column 
            v-if="userStore.hasRole('settlement')"
            type="selection" 
            width="55"
          />
        <el-table-column prop="name" label="接口名称" min-width="200">
          <template #default="{ row }">
            <div class="interface-name">
              <div class="name">{{ row.name }}</div>
              <div class="description">{{ row.description }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="method" label="请求方法" width="100">
          <template #default="{ row }">
            <el-tag :type="getMethodType(row.method)">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="path" label="接口路径" min-width="250">
          <template #default="{ row }">
            <code class="api-path">{{ row.path }}</code>
          </template>
        </el-table-column>
        
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ getCategoryName(row.category) }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="active"
              inactive-value="inactive"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="callCount" label="调用次数" width="120">
          <template #default="{ row }">
            <span class="call-count">{{ formatNumber(row.callCount) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updateTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :icon="View" @click="handleView(row)">查看</el-button>
            
            <!-- 技术部权限：编辑和删除 -->
            <template v-if="userStore.hasRole('tech')">
              <el-button size="small" type="warning" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
            
            <!-- 结算部权限：上架和下架 -->
            <template v-if="userStore.hasRole('settlement')">
              <el-button 
                v-if="row.status === 'unpublished'" 
                size="small" 
                type="success" 
                @click="handlePublish(row)"
              >
                上架
              </el-button>
              <el-button 
                v-if="row.status === 'published'" 
                size="small" 
                type="danger" 
                @click="handleUnpublish(row)"
              >
                下架
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
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

    <!-- 接口生成向导 -->
    <InterfaceCreationWizard
      v-model="wizardVisible"
      @success="handleWizardSuccess"
    />

    <!-- 接口详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="接口名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入接口名称" />
        </el-form-item>
        
        <el-form-item label="接口描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入接口描述"
          />
        </el-form-item>
        
        <el-form-item label="请求方法" prop="method">
          <el-select v-model="formData.method" placeholder="请选择请求方法">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="接口路径" prop="path">
          <el-input v-model="formData.path" placeholder="请输入接口路径，如：/api/v1/users" />
        </el-form-item>
        
        <el-form-item label="接口分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择接口分类">
            <el-option label="用户管理" value="user" />
            <el-option label="数据查询" value="data" />
            <el-option label="系统配置" value="system" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="接口状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search, View, Edit, Delete } from '@element-plus/icons-vue'
import InterfaceCreationWizard from '@/components/InterfaceCreationWizard.vue'
import { useUserStore } from '@/stores/user'
import { publishInterface, unpublishInterface, batchPublishInterfaces, batchUnpublishInterfaces, getInterfaceManagementList } from '@/api/interface'

// 用户状态管理
const userStore = useUserStore()

// 搜索表单
const searchForm = reactive({
  name: '',
  status: '',
  category: ''
})

// 选中的行数据
const selectedRows = ref<InterfaceItem[]>([])

// 接口数据类型定义
interface InterfaceItem {
  id: string
  name: string
  description: string
  method: string
  path: string
  category: string
  status: string
  callCount: number
  updateTime: string
}

// 表格数据
const tableData = ref<InterfaceItem[]>([])
const loading = ref(false)

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const submitting = ref(false)

// 接口生成向导
const wizardVisible = ref(false)

// 表单数据
const formData = reactive({
  id: '',
  name: '',
  description: '',
  method: 'GET',
  path: '',
  category: '',
  status: 'active'
})

// 表单验证规则
const formRules: FormRules = {
  name: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方法', trigger: 'change' }],
  path: [{ required: true, message: '请输入接口路径', trigger: 'blur' }],
  category: [{ required: true, message: '请选择接口分类', trigger: 'change' }]
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

// 获取分类名称
const getCategoryName = (category: string) => {
  const names: Record<string, string> = {
    user: '用户管理',
    data: '数据查询',
    system: '系统配置'
  }
  return names[category] || category
}

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

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      status: searchForm.status || undefined,
      category: searchForm.category || undefined,
      creator: undefined
    }
    
    const response = await getInterfaceManagementList(params)
    
    // 转换数据格式以适配现有的表格组件
    tableData.value = response.records.map(item => ({
      id: item.id,
      name: item.interfaceName,
      description: item.description || '',
      method: item.requestMethod,
      path: item.interfacePath,
      category: item.categoryId,
      status: item.status === 'published' ? 'active' : 'inactive',
      callCount: 0, // 暂时设为0，后续可以从统计API获取
      updateTime: item.updateTime
    }))
    
    pagination.total = response.total
  } catch (error) {
    console.error('加载接口列表失败:', error)
    ElMessage.error('加载数据失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    status: '',
    category: ''
  })
  handleSearch()
}

// 状态变更
const handleStatusChange = async (row: any) => {
  try {
    // 模拟API调用
    ElMessage.success(`接口状态已${row.status === 'active' ? '启用' : '禁用'}`)
  } catch (error) {
    ElMessage.error('状态更新失败')
    // 回滚状态
    row.status = row.status === 'active' ? 'inactive' : 'active'
  }
}

// 新增 - 打开接口生成向导
const handleAdd = () => {
  wizardVisible.value = true
}

// 向导成功回调
const handleWizardSuccess = () => {
  ElMessage.success('接口生成成功')
  loadData() // 重新加载数据
}

// 查看
const handleView = (row: any) => {
  // 跳转到接口详情页面
  ElMessage.info('跳转到接口详情页面')
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑接口'
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除接口 "${row.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 模拟API调用
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    // 用户取消删除
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitting.value = true
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success(formData.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleDialogClose = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

// 选择变更
const handleSelectionChange = (selection: InterfaceItem[]) => {
  selectedRows.value = selection
}

// 上架接口
const handlePublish = async (row: InterfaceItem) => {
  try {
    await ElMessageBox.confirm('确认要上架该接口吗？上架后用户可以订阅和调用该接口。', '上架确认', {
      confirmButtonText: '确认上架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await publishInterface(row.id)
    ElMessage.success('接口上架成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('上架接口失败:', error)
      ElMessage.error('上架接口失败')
    }
  }
}

// 下架接口
const handleUnpublish = async (row: InterfaceItem) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入下架原因', '下架确认', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      inputPlaceholder: '请详细说明下架原因',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim().length < 5) {
          return '下架原因不能少于5个字符'
        }
        return true
      }
    })
    
    await unpublishInterface(row.id, reason as string)
    ElMessage.success('接口下架成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下架接口失败:', error)
      ElMessage.error('下架接口失败')
    }
  }
}

// 批量上架
const handleBatchPublish = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要上架的接口')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确认要批量上架选中的 ${selectedRows.value.length} 个接口吗？`, '批量上架确认', {
      confirmButtonText: '确认上架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const result = await batchPublishInterfaces(selectedRows.value.map(row => row.id))
    
    if (result.failedCount > 0) {
      ElMessage.warning(`批量上架完成：成功 ${result.successCount} 个，失败 ${result.failedCount} 个`)
    } else {
      ElMessage.success('批量上架成功')
    }
    
    selectedRows.value = []
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量上架失败:', error)
      ElMessage.error('批量上架失败')
    }
  }
}

// 批量下架
const handleBatchUnpublish = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要下架的接口')
    return
  }
  
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入批量下架原因', '批量下架确认', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      inputPlaceholder: '请详细说明批量下架原因',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim().length < 5) {
          return '下架原因不能少于5个字符'
        }
        return true
      }
    })
    
    const result = await batchUnpublishInterfaces(selectedRows.value.map(row => row.id), reason as string)
    
    if (result.failedCount > 0) {
      ElMessage.warning(`批量下架完成：成功 ${result.successCount} 个，失败 ${result.failedCount} 个`)
    } else {
      ElMessage.success('批量下架成功')
    }
    
    selectedRows.value = []
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量下架失败:', error)
      ElMessage.error('批量下架失败')
    }
  }
}

// 分页变更
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.interface-management {
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

.search-card {
  margin-bottom: 20px;
}

.table-card {
  border-radius: 8px;
}

.interface-name .name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.interface-name .description {
  font-size: 12px;
  color: #909399;
}

.api-path {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: #e6a23c;
}

.call-count {
  font-weight: 600;
  color: #409eff;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>