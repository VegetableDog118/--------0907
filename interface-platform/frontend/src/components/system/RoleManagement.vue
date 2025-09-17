<template>
  <div class="role-management">
    <el-card>
      <template #header>
        <div class="section-header">
          <h4>角色权限管理</h4>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              新增角色
            </el-button>
            <el-button @click="loadRoleList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="角色名称">
            <el-input 
              v-model="filterForm.name" 
              placeholder="输入角色名称" 
              clearable 
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="选择状态" clearable style="width: 120px">
              <el-option label="正常" value="active" />
              <el-option label="禁用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadRoleList">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 角色列表 -->
      <el-table
        :data="roleList"
        v-loading="loading"
        empty-text="暂无角色数据"
        class="role-table"
      >
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="description" label="角色描述" min-width="200" />
        <el-table-column prop="permissions" label="权限数量" width="100">
          <template #default="{ row }">
            <el-tag type="info">{{ row.permissions?.length || 0 }} 个</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewRole(row)">详情</el-button>
            <el-button size="small" type="primary" @click="editRole(row)">编辑</el-button>
            <el-button size="small" type="info" @click="configPermissions(row)">配置权限</el-button>
            <el-button 
              size="small" 
              :type="row.status === 'active' ? 'danger' : 'success'"
              @click="toggleRoleStatus(row)"
              :disabled="row.code === 'admin'"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="roleList.length > 0">
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

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog 
      v-model="roleDialogVisible" 
      :title="isEdit ? '编辑角色' : '新增角色'" 
      width="600px"
      @close="resetRoleForm"
    >
      <el-form 
        ref="roleFormRef" 
        :model="roleForm" 
        :rules="roleFormRules" 
        label-width="80px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input 
            v-model="roleForm.code" 
            placeholder="请输入角色编码" 
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input 
            v-model="roleForm.description" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入角色描述" 
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="角色详情" width="700px">
      <el-descriptions :column="2" border v-if="selectedRole">
        <el-descriptions-item label="角色名称">{{ selectedRole.name }}</el-descriptions-item>
        <el-descriptions-item label="角色编码">{{ selectedRole.code }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedRole.status === 'active' ? 'success' : 'danger'">
            {{ selectedRole.status === 'active' ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="权限数量">
          <el-tag type="info">{{ selectedRole.permissions?.length || 0 }} 个</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ selectedRole.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ selectedRole.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="角色描述" :span="2">{{ selectedRole.description }}</el-descriptions-item>
      </el-descriptions>
      
      <div class="permissions-section" v-if="selectedRole?.permissions?.length">
        <h4>角色权限</h4>
        <el-tag 
          v-for="permission in selectedRole.permissions" 
          :key="permission" 
          class="permission-tag"
          type="primary"
        >
          {{ getPermissionName(permission) }}
        </el-tag>
      </div>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog 
      v-model="permissionDialogVisible" 
      title="配置权限" 
      width="800px"
      @close="resetPermissionForm"
    >
      <div class="permission-config">
        <el-form :model="permissionForm" label-width="100px">
          <el-form-item label="角色信息">
            <span class="role-info">{{ selectedRole?.name }} ({{ selectedRole?.code }})</span>
          </el-form-item>
          <el-form-item label="权限配置">
            <el-tree
              ref="permissionTreeRef"
              :data="permissionTree"
              :props="treeProps"
              show-checkbox
              node-key="code"
              :default-checked-keys="permissionForm.permissions"
              class="permission-tree"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import type { FormInstance } from 'element-plus'
import * as systemApi from '@/api/system'
import type { Role, CreateRoleRequest, UpdateRoleRequest } from '@/api/system'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const roleDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const isEdit = ref(false)
const selectedRole = ref<Role | null>(null)
const roleFormRef = ref<FormInstance>()
const permissionTreeRef = ref<InstanceType<typeof ElTree>>()

// 筛选表单
const filterForm = reactive({
  name: '',
  status: ''
})

// 角色表单
const roleForm = reactive({
  id: '',
  name: '',
  code: '',
  description: '',
  status: 'active'
})

// 权限表单
const permissionForm = reactive({
  roleId: '',
  permissions: [] as string[]
})

// 表单验证规则
const roleFormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 20, message: '角色名称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '角色编码只能包含字母、数字和下划线，且以字母开头', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入角色描述', trigger: 'blur' }
  ]
}

// 角色列表数据
const roleList = ref<Role[]>([])

// 权限树数据
const permissionTree = ref<any[]>([])
const treeProps = {
  children: 'children',
  label: 'name'
}

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 方法
const getPermissionName = (code: string) => {
  const permissionMap: Record<string, string> = {
    'system:user:view': '查看用户',
    'system:user:create': '创建用户',
    'system:user:update': '更新用户',
    'system:user:delete': '删除用户',
    'system:role:view': '查看角色',
    'system:role:create': '创建角色',
    'system:role:update': '更新角色',
    'system:role:delete': '删除角色',
    'interface:view': '查看接口',
    'interface:create': '创建接口',
    'interface:update': '更新接口',
    'interface:delete': '删除接口',
    'interface:publish': '发布接口',
    'application:view': '查看申请',
    'application:approve': '审批申请'
  }
  return permissionMap[code] || code
}

const loadRoleList = async () => {
  loading.value = true
  try {
    const response = await systemApi.getRoleList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      name: filterForm.name,
      status: filterForm.status
    })
    
    roleList.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('加载角色列表失败:', error)
    ElMessage.error('加载角色列表失败')
    
    // 使用模拟数据
    roleList.value = [
      {
        id: '1',
        name: '系统管理员',
        code: 'admin',
        description: '系统管理员，拥有所有权限',
        permissions: ['system:user:view', 'system:user:create', 'system:user:update', 'system:user:delete', 'system:role:view', 'system:role:create', 'system:role:update', 'system:role:delete', 'interface:view', 'interface:create', 'interface:update', 'interface:delete', 'interface:publish', 'application:view', 'application:approve'],
        status: 'active',
        createTime: '2024-01-01 00:00:00',
        updateTime: '2024-01-01 00:00:00'
      },
      {
        id: '2',
        name: '结算部',
        code: 'settlement',
        description: '结算部角色，负责接口审批和发布',
        permissions: ['interface:view', 'interface:publish', 'application:view', 'application:approve'],
        status: 'active',
        createTime: '2024-01-02 00:00:00',
        updateTime: '2024-01-02 00:00:00'
      },
      {
        id: '3',
        name: '技术部',
        code: 'tech',
        description: '技术部角色，负责接口开发和管理',
        permissions: ['interface:view', 'interface:create', 'interface:update', 'interface:delete'],
        status: 'active',
        createTime: '2024-01-03 00:00:00',
        updateTime: '2024-01-03 00:00:00'
      },
      {
        id: '4',
        name: '消费者',
        code: 'consumer',
        description: '接口消费者，只能查看和使用接口',
        permissions: ['interface:view'],
        status: 'active',
        createTime: '2024-01-04 00:00:00',
        updateTime: '2024-01-04 00:00:00'
      }
    ]
    pagination.total = roleList.value.length
  } finally {
    loading.value = false
  }
}

const loadPermissionTree = async () => {
  try {
    const permissions = await systemApi.getAllPermissions()
    permissionTree.value = permissions
  } catch (error) {
    console.error('加载权限树失败:', error)
    // 使用模拟数据
    permissionTree.value = [
      {
        id: 'system',
        name: '系统管理',
        code: 'system',
        children: [
          {
            id: 'system:user',
            name: '用户管理',
            code: 'system:user',
            children: [
              { id: 'system:user:view', name: '查看用户', code: 'system:user:view' },
              { id: 'system:user:create', name: '创建用户', code: 'system:user:create' },
              { id: 'system:user:update', name: '更新用户', code: 'system:user:update' },
              { id: 'system:user:delete', name: '删除用户', code: 'system:user:delete' }
            ]
          },
          {
            id: 'system:role',
            name: '角色管理',
            code: 'system:role',
            children: [
              { id: 'system:role:view', name: '查看角色', code: 'system:role:view' },
              { id: 'system:role:create', name: '创建角色', code: 'system:role:create' },
              { id: 'system:role:update', name: '更新角色', code: 'system:role:update' },
              { id: 'system:role:delete', name: '删除角色', code: 'system:role:delete' }
            ]
          }
        ]
      },
      {
        id: 'interface',
        name: '接口管理',
        code: 'interface',
        children: [
          { id: 'interface:view', name: '查看接口', code: 'interface:view' },
          { id: 'interface:create', name: '创建接口', code: 'interface:create' },
          { id: 'interface:update', name: '更新接口', code: 'interface:update' },
          { id: 'interface:delete', name: '删除接口', code: 'interface:delete' },
          { id: 'interface:publish', name: '发布接口', code: 'interface:publish' }
        ]
      },
      {
        id: 'application',
        name: '申请管理',
        code: 'application',
        children: [
          { id: 'application:view', name: '查看申请', code: 'application:view' },
          { id: 'application:approve', name: '审批申请', code: 'application:approve' }
        ]
      }
    ]
  }
}

const resetFilter = () => {
  filterForm.name = ''
  filterForm.status = ''
  loadRoleList()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadRoleList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadRoleList()
}

const showCreateDialog = () => {
  isEdit.value = false
  roleDialogVisible.value = true
}

const viewRole = (role: Role) => {
  selectedRole.value = role
  detailDialogVisible.value = true
}

const editRole = (role: Role) => {
  isEdit.value = true
  Object.assign(roleForm, {
    id: role.id,
    name: role.name,
    code: role.code,
    description: role.description,
    status: role.status
  })
  roleDialogVisible.value = true
}

const configPermissions = (role: Role) => {
  selectedRole.value = role
  permissionForm.roleId = role.id
  permissionForm.permissions = [...(role.permissions || [])]
  permissionDialogVisible.value = true
}

const resetRoleForm = () => {
  roleFormRef.value?.resetFields()
  Object.assign(roleForm, {
    id: '',
    name: '',
    code: '',
    description: '',
    status: 'active'
  })
}

const resetPermissionForm = () => {
  permissionForm.roleId = ''
  permissionForm.permissions = []
}

const saveRole = async () => {
  if (!roleFormRef.value) return
  
  try {
    await roleFormRef.value.validate()
    saving.value = true
    
    if (isEdit.value) {
      const updateData: UpdateRoleRequest = {
        id: roleForm.id,
        name: roleForm.name,
        description: roleForm.description,
        status: roleForm.status
      }
      await systemApi.updateRole(updateData)
      ElMessage.success('角色更新成功')
    } else {
      const createData: CreateRoleRequest = {
        name: roleForm.name,
        code: roleForm.code,
        description: roleForm.description,
        permissions: []
      }
      await systemApi.createRole(createData)
      ElMessage.success('角色创建成功')
    }
    
    roleDialogVisible.value = false
    loadRoleList()
  } catch (error) {
    console.error('保存角色失败:', error)
    ElMessage.error('保存角色失败')
  } finally {
    saving.value = false
  }
}

const savePermissions = async () => {
  try {
    saving.value = true
    const checkedKeys = permissionTreeRef.value?.getCheckedKeys() || []
    const halfCheckedKeys = permissionTreeRef.value?.getHalfCheckedKeys() || []
    const allPermissions = [...checkedKeys, ...halfCheckedKeys] as string[]
    
    await systemApi.updateRole({
      id: permissionForm.roleId,
      permissions: allPermissions
    })
    
    ElMessage.success('权限配置成功')
    permissionDialogVisible.value = false
    loadRoleList()
  } catch (error) {
    console.error('保存权限失败:', error)
    ElMessage.error('保存权限失败')
  } finally {
    saving.value = false
  }
}

const toggleRoleStatus = async (role: Role) => {
  const action = role.status === 'active' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}角色 "${role.name}" 吗？`,
      `${action}角色`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const newStatus = role.status === 'active' ? 'disabled' : 'active'
    await systemApi.updateRole({
      id: role.id,
      status: newStatus
    })
    
    ElMessage.success(`角色${action}成功`)
    loadRoleList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}角色失败:`, error)
      ElMessage.error(`${action}角色失败`)
    }
  }
}

// 生命周期
onMounted(() => {
  loadRoleList()
  loadPermissionTree()
})
</script>

<style scoped>
.role-management {
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

.role-table {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.permissions-section {
  margin-top: 24px;
}

.permissions-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.permission-tag {
  margin: 4px 8px 4px 0;
}

.role-info {
  font-weight: 600;
  color: #409eff;
}

.permission-tree {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
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