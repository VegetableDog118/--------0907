<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="section-header">
          <h4>用户管理</h4>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              新增用户
            </el-button>
            <el-button @click="loadUserList">
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
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="filterForm.role" placeholder="选择角色" clearable style="width: 150px">
              <el-option label="管理员" value="admin" />
              <el-option label="结算部" value="settlement" />
              <el-option label="技术部" value="tech" />
              <el-option label="消费者" value="consumer" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="选择状态" clearable style="width: 120px">
              <el-option label="正常" value="active" />
              <el-option label="禁用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadUserList">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 用户列表 -->
      <el-table
        :data="userList"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        empty-text="暂无用户数据"
        class="user-table"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewUser(row)">详情</el-button>
            <el-button size="small" type="primary" @click="editUser(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button 
              size="small" 
              :type="row.status === 'active' ? 'danger' : 'success'"
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="userList.length > 0">
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

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog 
      v-model="userDialogVisible" 
      :title="isEdit ? '编辑用户' : '新增用户'" 
      width="600px"
      @close="resetUserForm"
    >
      <el-form 
        ref="userFormRef" 
        :model="userForm" 
        :rules="userFormRules" 
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="userForm.username" 
            placeholder="请输入用户名" 
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="结算部" value="settlement" />
            <el-option label="技术部" value="tech" />
            <el-option label="消费者" value="consumer" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input 
            v-model="userForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="selectedUser">
        <el-descriptions-item label="用户名">{{ selectedUser.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ selectedUser.realName }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ selectedUser.email }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ selectedUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="getRoleTagType(selectedUser.role)">{{ getRoleName(selectedUser.role) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedUser.status === 'active' ? 'success' : 'danger'">
            {{ selectedUser.status === 'active' ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ selectedUser.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ selectedUser.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="最后登录" :span="2">{{ selectedUser.lastLoginTime || '从未登录' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import * as systemApi from '@/api/system'
import type { User, CreateUserRequest, UpdateUserRequest } from '@/api/system'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const userDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const isEdit = ref(false)
const selectedUser = ref<User | null>(null)
const selectedUsers = ref<User[]>([])
const userFormRef = ref<FormInstance>()

// 筛选表单
const filterForm = reactive({
  username: '',
  role: '',
  status: ''
})

// 用户表单
const userForm = reactive({
  id: '',
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: '',
  password: '',
  status: 'active'
})

// 表单验证规则
const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 用户列表数据
const userList = ref<User[]>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 方法
const getRoleTagType = (role: string) => {
  const typeMap: Record<string, string> = {
    admin: 'danger',
    settlement: 'warning',
    tech: 'primary',
    consumer: 'success'
  }
  return typeMap[role] || 'info'
}

const getRoleName = (role: string) => {
  const nameMap: Record<string, string> = {
    admin: '管理员',
    settlement: '结算部',
    tech: '技术部',
    consumer: '消费者'
  }
  return nameMap[role] || role
}

const loadUserList = async () => {
  loading.value = true
  try {
    const response = await systemApi.getUserList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      username: filterForm.username,
      role: filterForm.role,
      status: filterForm.status
    })
    
    userList.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
    
    // 使用模拟数据
    userList.value = [
      {
        id: '1',
        username: 'admin',
        realName: '系统管理员',
        email: 'admin@example.com',
        phone: '13800138000',
        role: 'admin',
        status: 'active',
        createTime: '2024-01-01 00:00:00',
        updateTime: '2024-01-01 00:00:00',
        lastLoginTime: '2024-01-15 10:30:00'
      },
      {
        id: '2',
        username: 'settlement_user',
        realName: '结算部用户',
        email: 'settlement@example.com',
        phone: '13800138001',
        role: 'settlement',
        status: 'active',
        createTime: '2024-01-02 00:00:00',
        updateTime: '2024-01-02 00:00:00',
        lastLoginTime: '2024-01-15 09:15:00'
      },
      {
        id: '3',
        username: 'tech_user',
        realName: '技术部用户',
        email: 'tech@example.com',
        phone: '13800138002',
        role: 'tech',
        status: 'active',
        createTime: '2024-01-03 00:00:00',
        updateTime: '2024-01-03 00:00:00',
        lastLoginTime: '2024-01-15 08:45:00'
      }
    ]
    pagination.total = userList.value.length
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.username = ''
  filterForm.role = ''
  filterForm.status = ''
  loadUserList()
}

const handleSelectionChange = (selection: User[]) => {
  selectedUsers.value = selection
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadUserList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadUserList()
}

const showCreateDialog = () => {
  isEdit.value = false
  userDialogVisible.value = true
}

const viewUser = (user: User) => {
  selectedUser.value = user
  detailDialogVisible.value = true
}

const editUser = (user: User) => {
  isEdit.value = true
  Object.assign(userForm, {
    id: user.id,
    username: user.username,
    realName: user.realName,
    email: user.email,
    phone: user.phone,
    role: user.role,
    status: user.status,
    password: ''
  })
  userDialogVisible.value = true
}

const resetUserForm = () => {
  userFormRef.value?.resetFields()
  Object.assign(userForm, {
    id: '',
    username: '',
    realName: '',
    email: '',
    phone: '',
    role: '',
    password: '',
    status: 'active'
  })
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    saving.value = true
    
    if (isEdit.value) {
      const updateData: UpdateUserRequest = {
        id: userForm.id,
        email: userForm.email,
        phone: userForm.phone,
        realName: userForm.realName,
        role: userForm.role,
        status: userForm.status
      }
      await systemApi.updateUser(updateData)
      ElMessage.success('用户更新成功')
    } else {
      const createData: CreateUserRequest = {
        username: userForm.username,
        email: userForm.email,
        phone: userForm.phone,
        realName: userForm.realName,
        role: userForm.role,
        password: userForm.password
      }
      await systemApi.createUser(createData)
      ElMessage.success('用户创建成功')
    }
    
    userDialogVisible.value = false
    loadUserList()
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存用户失败')
  } finally {
    saving.value = false
  }
}

const resetPassword = async (user: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户 "${user.realName}" 的密码吗？`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await systemApi.resetUserPassword(user.id)
    ElMessageBox.alert(
      `新密码为：${response.password}\n请及时通知用户修改密码`,
      '密码重置成功',
      {
        confirmButtonText: '确定',
        type: 'success'
      }
    )
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

const toggleUserStatus = async (user: User) => {
  const action = user.status === 'active' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${user.realName}" 吗？`,
      `${action}用户`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const newStatus = user.status === 'active' ? 'disabled' : 'active'
    await systemApi.updateUser({
      id: user.id,
      status: newStatus
    })
    
    ElMessage.success(`用户${action}成功`)
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}用户失败:`, error)
      ElMessage.error(`${action}用户失败`)
    }
  }
}

// 生命周期
onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-management {
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

.user-table {
  margin-bottom: 20px;
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
}
</style>