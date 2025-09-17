import axios from 'axios'
import type { AxiosResponse } from 'axios'

// 使用真实的后端API - 直接连接到approval-service
const approvalApi = axios.create({
  baseURL: import.meta.env.VITE_APPROVAL_API_BASE_URL || 'http://localhost:8085/approval',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
approvalApi.interceptors.request.use(
  (config) => {
    // 添加认证token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 添加用户ID头部
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        const user = JSON.parse(userInfo)
        if (user.userId) {
          config.headers['X-User-Id'] = user.userId
        }
      } catch (error) {
        console.error('解析用户信息失败:', error)
      }
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
approvalApi.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.success) {
      return data.data
    } else {
      throw new Error(data.message || '请求失败')
    }
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
          throw new Error('登录已过期，请重新登录')
        case 403:
          throw new Error('权限不足，无法访问该资源')
        case 404:
          throw new Error('请求的资源不存在')
        case 500:
          throw new Error('服务器内部错误')
        default:
          throw new Error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      throw new Error('请求超时，请稍后重试')
    } else {
      throw new Error('网络错误，请检查网络连接')
    }
  }
)

// 类型定义
export interface SubscriptionApplication {
  id: string
  userId: string
  interfaceIds: string[]
  reason: string
  businessScenario?: string
  estimatedCalls?: number
  status: 'pending' | 'approved' | 'rejected'
  submitTime: string
  processTime?: string
  processBy?: string
  processComment?: string
  createTime: string
  updateTime: string
}

export interface SubscriptionApplicationRequest {
  interfaceIds: string[]
  reason: string
  businessScenario?: string
  estimatedCalls?: number
}

export interface ApprovalRequest {
  applicationId: string
  action: 'approve' | 'reject'
  comment?: string
}

export interface BatchApprovalRequest {
  applicationIds: string[]
  action: 'approve' | 'reject'
  comment?: string
}

export interface ApplicationListRequest {
  status?: string
  userId?: string
  startDate?: string
  endDate?: string
  page: number
  size: number
}

export interface ApplicationListResponse {
  records: SubscriptionApplication[]
  total: number
  current: number
  size: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface UserInterfaceSubscription {
  id: string
  userId: string
  interfaceId: string
  applicationId: string
  status: 'active' | 'inactive' | 'expired' | 'cancelled'
  subscribeTime: string
  expireTime?: string
  cancelTime?: string
  callCount: number
  lastCallTime?: string
  createTime: string
  updateTime: string
}

export interface ApprovalStats {
  totalApplications: number
  pendingApplications: number
  approvedApplications: number
  rejectedApplications: number
  todayApplications: number
  weekApplications: number
  monthApplications: number
}

// API方法

/**
 * 提交订阅申请
 */
export const submitSubscriptionApplication = async (data: SubscriptionApplicationRequest): Promise<string> => {
  return approvalApi.post('/api/approval/applications', data)
}

/**
 * 批量提交订阅申请
 */
export const batchSubmitSubscriptionApplications = async (data: SubscriptionApplicationRequest[]): Promise<string[]> => {
  return approvalApi.post('/api/approval/applications/batch', data)
}

/**
 * 获取申请列表
 */
export const getApplicationList = async (params: ApplicationListRequest): Promise<ApplicationListResponse> => {
  return approvalApi.get('/api/approval/applications', { params })
}

/**
 * 获取申请详情
 */
export const getApplicationDetail = async (applicationId: string): Promise<SubscriptionApplication> => {
  return approvalApi.get(`/api/v1/applications/${applicationId}`)
}

/**
 * 审批申请
 */
export const approveApplication = async (data: ApprovalRequest): Promise<void> => {
  // 转换数据格式以匹配后端API
  const processRequest = {
    applicationIds: [data.applicationId],
    action: data.action === 'approve' ? 'approved' : 'rejected',
    comment: data.comment
  }
  return approvalApi.post('/api/approval/applications/process', processRequest)
}

/**
 * 批量审批
 */
export const batchApproveApplications = async (data: BatchApprovalRequest): Promise<void> => {
  return approvalApi.post('/api/v1/applications/batch-approve', data)
}

/**
 * 获取用户的订阅列表
 */
export const getUserSubscriptions = async (userId?: string, page: number = 1, size: number = 20): Promise<{
  records: UserInterfaceSubscription[]
  total: number
  current: number
  size: number
}> => {
  return approvalApi.get('/api/v1/subscriptions', {
    params: { userId, page, size }
  })
}

/**
 * 取消订阅
 */
export const cancelSubscription = async (subscriptionId: string): Promise<void> => {
  return approvalApi.delete(`/api/v1/subscriptions/${subscriptionId}`)
}

/**
 * 获取审批统计
 */
export const getApprovalStats = async (): Promise<ApprovalStats> => {
  const response = await approvalApi.get('/api/approval/statistics')
  // 转换后端返回的数据格式以匹配前端期望的格式
  // 后端返回字段：total, pending, approved, rejected
  return {
    totalApplications: response.total || 0,
    pendingApplications: response.pending || 0,
    approvedApplications: response.approved || 0,
    rejectedApplications: response.rejected || 0,
    todayApplications: 0, // 后端暂未提供此数据
    weekApplications: 0,  // 后端暂未提供此数据
    monthApplications: 0  // 后端暂未提供此数据
  }
}

/**
 * 获取审批历史
 */
export const getApprovalHistory = async (applicationId: string): Promise<any[]> => {
  return approvalApi.get(`/api/v1/applications/${applicationId}/history`)
}

/**
 * 撤回申请
 */
export const withdrawApplication = async (applicationId: string): Promise<void> => {
  return approvalApi.post(`/api/v1/applications/${applicationId}/withdraw`)
}

export default approvalApi