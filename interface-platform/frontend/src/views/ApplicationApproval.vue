<template>
  <div class="application-approval">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>申请审批</h2>
        <p>管理接口访问申请和权限审批</p>
      </div>
      <div class="header-right">
        <el-badge :value="pendingCount" class="badge">
          <el-button type="primary">
            <el-icon><Bell /></el-icon>
            待审批
          </el-button>
        </el-badge>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon pending">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.pending }}</div>
                <div class="stat-label">待审批</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon approved">
                <el-icon><Check /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.approved }}</div>
                <div class="stat-label">已通过</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon rejected">
                <el-icon><Close /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.rejected }}</div>
                <div class="stat-label">已拒绝</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.total }}</div>
                <div class="stat-label">总申请</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 搜索和筛选 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="申请人">
          <el-input
            v-model="searchForm.applicant"
            placeholder="请输入申请人姓名"
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item label="申请状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="接口访问" value="api_access" />
            <el-option label="权限提升" value="permission_upgrade" />
            <el-option label="数据导出" value="data_export" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 240px;"
          />
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

    <!-- 申请列表 -->
    <el-card class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="申请ID" width="120" />
        
        <el-table-column prop="applicant" label="申请人" width="150">
          <template #default="{ row }">
            <div class="applicant-info">
              <el-avatar :size="32" :src="row.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="applicant-details">
                <div class="name">{{ row.applicant }}</div>
                <div class="department">{{ row.department }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="申请类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)" size="small">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="title" label="申请内容" min-width="250">
          <template #default="{ row }">
            <div class="application-content">
              <div class="title">{{ row.title }}</div>
              <div class="description">{{ row.description }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityColor(row.priority)" size="small">
              {{ getPriorityName(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="applyTime" label="申请时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.applyTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="approver" label="审批人" width="120">
          <template #default="{ row }">
            <span v-if="row.approver">{{ row.approver }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <template v-if="row.status === 'pending'">
              <el-button size="small" type="success" @click="handleApprove(row)">
                <el-icon><Check /></el-icon>
                通过
              </el-button>
              <el-button size="small" type="danger" @click="handleReject(row)">
                <el-icon><Close /></el-icon>
                拒绝
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

    <!-- 申请详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="申请详情"
      width="800px"
    >
      <div v-if="currentApplication" class="application-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请ID">{{ currentApplication.id }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentApplication.applicant }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentApplication.department }}</el-descriptions-item>
          <el-descriptions-item label="申请类型">
            <el-tag :type="getTypeColor(currentApplication.type)">
              {{ getTypeName(currentApplication.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityColor(currentApplication.priority)">
              {{ getPriorityName(currentApplication.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusColor(currentApplication.status)">
              {{ getStatusName(currentApplication.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">
            {{ formatDate(currentApplication.applyTime) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="detail-section">
          <h4>申请内容</h4>
          <div class="content-box">
            <h5>{{ currentApplication.title }}</h5>
            <p>{{ currentApplication.description }}</p>
            <div v-if="currentApplication.attachments" class="attachments">
              <h6>附件：</h6>
              <el-tag v-for="file in currentApplication.attachments" :key="file" class="attachment-tag">
                <el-icon><Paperclip /></el-icon>
                {{ file }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <div v-if="currentApplication.reason" class="detail-section">
          <h4>{{ currentApplication.status === 'approved' ? '通过原因' : '拒绝原因' }}</h4>
          <div class="content-box">
            <p>{{ currentApplication.reason }}</p>
          </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <template v-if="currentApplication?.status === 'pending'">
            <el-button type="success" @click="handleApprove(currentApplication)">
              <el-icon><Check /></el-icon>
              通过
            </el-button>
            <el-button type="danger" @click="handleReject(currentApplication)">
              <el-icon><Close /></el-icon>
              拒绝
            </el-button>
          </template>
        </span>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog
      v-model="approvalDialogVisible"
      :title="approvalAction === 'approve' ? '通过申请' : '拒绝申请'"
      width="500px"
    >
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="审批意见" required>
          <el-input
            v-model="approvalForm.reason"
            type="textarea"
            :rows="4"
            :placeholder="approvalAction === 'approve' ? '请输入通过原因（可选）' : '请输入拒绝原因'"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="approvalDialogVisible = false">取消</el-button>
          <el-button
            :type="approvalAction === 'approve' ? 'success' : 'danger'"
            @click="handleConfirmApproval"
            :loading="submitting"
          >
            确定{{ approvalAction === 'approve' ? '通过' : '拒绝' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell, Clock, Check, Close, Document, Search, View, User, Paperclip
} from '@element-plus/icons-vue'
import { getApplicationList, approveApplication, getApprovalStats } from '@/api/approval'

// 搜索表单
const searchForm = reactive({
  applicant: '',
  status: '',
  type: '',
  dateRange: []
})

// 统计数据
const stats = reactive({
  pending: 12,
  approved: 45,
  rejected: 8,
  total: 65
})

// 待审批数量
const pendingCount = computed(() => stats.pending)

// 申请数据类型定义
interface ApplicationItem {
  id: string
  applicant: string
  department: string
  avatar: string
  type: string
  title: string
  description: string
  priority: string
  status: string
  applyTime: string
  approver: string
  reason: string
  attachments: string[]
}

// 表格数据
const tableData = ref<ApplicationItem[]>([])
const loading = ref(false)

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 对话框
const detailDialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const currentApplication = ref<ApplicationItem | null>(null)
const approvalAction = ref('approve')
const submitting = ref(false)

// 审批表单
const approvalForm = reactive({
  reason: ''
})

// 获取类型颜色
const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    api_access: 'primary',
    permission_upgrade: 'warning',
    data_export: 'success'
  }
  return colors[type] || 'info'
}

// 获取类型名称
const getTypeName = (type: string) => {
  const names: Record<string, string> = {
    api_access: '接口访问',
    permission_upgrade: '权限提升',
    data_export: '数据导出'
  }
  return names[type] || type
}

// 获取优先级颜色
const getPriorityColor = (priority: string) => {
  const colors: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return colors[priority] || 'info'
}

// 获取优先级名称
const getPriorityName = (priority: string) => {
  const names: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return names[priority] || priority
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return colors[status] || 'info'
}

// 获取状态名称
const getStatusName = (status: string) => {
  const names: Record<string, string> = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return names[status] || status
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      status: searchForm.status || undefined,
      userId: undefined, // 管理员查看所有申请
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
      page: pagination.currentPage,
      size: pagination.pageSize
    }
    
    // 调用真实API
    const response = await getApplicationList(params)
    
    // 转换数据格式以适配现有的表格结构
    tableData.value = response.records.map(item => ({
      id: item.id,
      applicant: item.userId, // 这里可能需要根据userId获取用户名
      department: '未知部门', // 需要从用户信息中获取
      avatar: '',
      type: 'api_access', // 统一为接口访问类型
      title: `申请订阅接口: ${item.interfaceIds && Array.isArray(item.interfaceIds) ? item.interfaceIds.join(', ') : '未知接口'}`,
      description: item.reason || '无描述',
      priority: 'medium', // 默认中等优先级
      status: item.status,
      applyTime: item.submitTime,
      approver: item.processBy || '',
      reason: item.processComment || '',
      attachments: [] // 当前没有附件功能
    }))
    
    pagination.total = response.total
    
    // 同时加载统计数据
    await loadStats()
  } catch (error: any) {
    console.error('加载申请列表失败:', error)
    ElMessage.error(error.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const statsData = await getApprovalStats()
    Object.assign(stats, {
      pending: statsData.pendingApplications,
      approved: statsData.approvedApplications,
      rejected: statsData.rejectedApplications,
      total: statsData.totalApplications
    })
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
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
    applicant: '',
    status: '',
    type: '',
    dateRange: []
  })
  handleSearch()
}

// 查看详情
const handleView = (row: any) => {
  currentApplication.value = row
  detailDialogVisible.value = true
}

// 通过申请
const handleApprove = (row: any) => {
  currentApplication.value = row
  approvalAction.value = 'approve'
  approvalForm.reason = ''
  approvalDialogVisible.value = true
  detailDialogVisible.value = false
}

// 拒绝申请
const handleReject = (row: any) => {
  currentApplication.value = row
  approvalAction.value = 'reject'
  approvalForm.reason = ''
  approvalDialogVisible.value = true
  detailDialogVisible.value = false
}

// 确认审批
const handleConfirmApproval = async () => {
  if (approvalAction.value === 'reject' && !approvalForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  
  if (!currentApplication.value) {
    ElMessage.error('未选择申请')
    return
  }
  
  submitting.value = true
  try {
    // 调用真实的审批API
    await approveApplication({
      applicationId: currentApplication.value.id,
      action: approvalAction.value,
      comment: approvalForm.reason.trim() || undefined
    })
    
    const actionText = approvalAction.value === 'approve' ? '通过' : '拒绝'
    ElMessage.success(`申请已${actionText}`)
    
    approvalDialogVisible.value = false
    
    // 重新加载数据以获取最新状态
    await loadData()
  } catch (error: any) {
    console.error('审批操作失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
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
.application-approval {
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

.badge {
  margin-left: 10px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 20px;
  color: white;
}

.stat-icon.pending {
  background: #e6a23c;
}

.stat-icon.approved {
  background: #67c23a;
}

.stat-icon.rejected {
  background: #f56c6c;
}

.stat-icon.total {
  background: #409eff;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  border-radius: 8px;
}

.applicant-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.applicant-details .name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.applicant-details .department {
  font-size: 12px;
  color: #909399;
}

.application-content .title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.application-content .description {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.text-placeholder {
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.application-detail {
  padding: 10px 0;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
}

.content-box {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.content-box h5 {
  margin: 0 0 10px 0;
  color: #303133;
}

.content-box p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

.attachments {
  margin-top: 15px;
}

.attachments h6 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 14px;
}

.attachment-tag {
  margin-right: 8px;
  margin-bottom: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>