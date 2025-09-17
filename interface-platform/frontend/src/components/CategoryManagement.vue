<template>
  <div class="category-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>接口分类管理</h3>
          <el-button type="primary" @click="handleAddCategory">
            <el-icon><Plus /></el-icon>
            新增分类
          </el-button>
        </div>
      </template>

      <!-- 分类列表 -->
      <el-table
        :data="categoryList"
        v-loading="loading"
        row-key="id"
        default-expand-all
      >
        <el-table-column prop="categoryCode" label="分类编码" width="150" />
        <el-table-column prop="categoryName" label="分类名称" width="200" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="color" label="颜色" width="100">
          <template #default="{ row }">
            <div class="color-display">
              <div 
                class="color-block" 
                :style="{ backgroundColor: row.color }"
              ></div>
              <span>{{ row.color }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="interfaceCount" label="接口数量" width="100">
          <template #default="{ row }">
            <el-tag type="info">{{ row.interfaceCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEditCategory(row)">编辑</el-button>
            <el-button 
              size="small" 
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="handleDeleteCategory(row)"
              :disabled="(row.interfaceCount || 0) > 0"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑分类弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        :model="formData"
        :rules="formRules"
        ref="formRef"
        label-width="100px"
      >
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input 
            v-model="formData.categoryCode" 
            placeholder="请输入分类编码"
            :disabled="isEdit"
          />
          <div class="form-tip">分类编码创建后不可修改</div>
        </el-form-item>
        
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item label="分类描述" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
        
        <el-form-item label="分类颜色" prop="color">
          <div class="color-picker-wrapper">
            <el-color-picker v-model="formData.color" />
            <el-input 
              v-model="formData.color" 
              placeholder="#1890ff"
              style="width: 200px; margin-left: 10px;"
            />
          </div>
        </el-form-item>
        
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number 
            v-model="formData.sortOrder" 
            :min="0" 
            :max="999"
            placeholder="排序值"
          />
          <div class="form-tip">数值越小排序越靠前</div>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as interfaceApi from '@/api/interface'

// 响应式数据
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 分类列表数据
const categoryList = ref<any[]>([])

// 表单数据
const formData = reactive({
  id: '',
  categoryCode: '',
  categoryName: '',
  description: '',
  color: '#1890ff',
  sortOrder: 0,
  status: 1
})

// 表单验证规则
const formRules: FormRules = {
  categoryCode: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[a-z_]+$/, message: '分类编码只能包含小写字母和下划线', trigger: 'blur' }
  ],
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择分类颜色', trigger: 'blur' },
    { pattern: /^#[0-9a-fA-F]{6}$/, message: '请输入有效的颜色值', trigger: 'blur' }
  ]
}

// 方法
const loadCategoryList = async () => {
  loading.value = true
  try {
    const response = await interfaceApi.getInterfaceCategories()
    categoryList.value = response
  } catch (error) {
    console.error('加载分类列表失败:', error)
    ElMessage.error('加载分类列表失败')
    
    // 降级到模拟数据
    categoryList.value = [
      {
        id: 'cat001',
        categoryCode: 'day_ahead_spot',
        categoryName: '日前现货',
        description: '日前现货市场相关数据接口',
        color: '#1890ff',
        sortOrder: 1,
        status: 1,
        interfaceCount: 5,
        createTime: '2024-01-15 10:30:00'
      },
      {
        id: 'cat002',
        categoryCode: 'forecast',
        categoryName: '预测',
        description: '负荷预测、新能源预测等预测类数据接口',
        color: '#52c41a',
        sortOrder: 2,
        status: 1,
        interfaceCount: 3,
        createTime: '2024-01-15 10:31:00'
      },
      {
        id: 'cat003',
        categoryCode: 'ancillary_service',
        categoryName: '辅助服务',
        description: '调频、调压、备用等辅助服务数据接口',
        color: '#faad14',
        sortOrder: 3,
        status: 1,
        interfaceCount: 2,
        createTime: '2024-01-15 10:32:00'
      },
      {
        id: 'cat004',
        categoryCode: 'grid_operation',
        categoryName: '电网运行',
        description: '电网运行状态、约束情况等运行数据接口',
        color: '#f5222d',
        sortOrder: 4,
        status: 1,
        interfaceCount: 4,
        createTime: '2024-01-15 10:33:00'
      }
    ]
  } finally {
    loading.value = false
  }
}

const handleAddCategory = () => {
  isEdit.value = false
  resetFormData()
  dialogVisible.value = true
}

const handleEditCategory = (row: any) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    categoryCode: row.categoryCode,
    categoryName: row.categoryName,
    description: row.description || '',
    color: row.color || '#1890ff',
    sortOrder: row.sortOrder || 0,
    status: row.status
  })
  dialogVisible.value = true
}

const handleToggleStatus = async (row: any) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}分类"${row.categoryName}"吗？`, '确认操作', {
      type: 'warning'
    })
    
    // TODO: 调用API切换状态
    // await interfaceApi.toggleCategoryStatus(row.id)
    
    ElMessage.success(`分类${action}成功`)
    loadCategoryList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`分类${action}失败:`, error)
      ElMessage.error(`分类${action}失败`)
    }
  }
}

const handleDeleteCategory = async (row: any) => {
  if ((row.interfaceCount || 0) > 0) {
    ElMessage.warning('该分类下还有接口，无法删除')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确认删除分类"${row.categoryName}"吗？此操作不可恢复！`, '确认删除', {
      type: 'error'
    })
    
    // TODO: 调用API删除分类
    // await interfaceApi.deleteCategory(row.id)
    
    ElMessage.success('分类删除成功')
    loadCategoryList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('分类删除失败:', error)
      ElMessage.error('分类删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    if (isEdit.value) {
      // TODO: 调用API更新分类
      // await interfaceApi.updateCategory(formData)
    } else {
      // TODO: 调用API创建分类
      // await interfaceApi.createCategory(formData)
    }
    
    ElMessage.success(`分类${isEdit.value ? '更新' : '创建'}成功`)
    handleDialogClose()
    loadCategoryList()
  } catch (error) {
    console.error(`分类${isEdit.value ? '更新' : '创建'}失败:`, error)
    ElMessage.error(`分类${isEdit.value ? '更新' : '创建'}失败`)
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  dialogVisible.value = false
  resetFormData()
  formRef.value?.clearValidate()
}

const resetFormData = () => {
  Object.assign(formData, {
    id: '',
    categoryCode: '',
    categoryName: '',
    description: '',
    color: '#1890ff',
    sortOrder: 0,
    status: 1
  })
}

// 生命周期
onMounted(() => {
  loadCategoryList()
})
</script>

<style scoped>
.category-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.color-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-block {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.color-picker-wrapper {
  display: flex;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.dialog-footer {
  text-align: right;
}
</style>