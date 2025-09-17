import request from '@/utils/request'

const statisticsApi = request

// ========== 数据统计相关接口 ==========

/**
 * 概览数据接口
 */
export interface OverviewData {
  totalInterfaces: number
  activeUsers: number
  totalCalls: number
  successRate: number
  interfaceTrend: number
  userTrend: number
  callTrend: number
  successTrend: number
}

/**
 * 接口统计数据
 */
export interface InterfaceStatData {
  interfaceId: string
  interfaceName: string
  category: string
  totalCalls: number
  successCalls: number
  successRate: number
  avgResponseTime: number
  subscriberCount: number
  lastCallTime: string
}

/**
 * 用户统计数据
 */
export interface UserStatData {
  userId: string
  username: string
  companyName: string
  totalCalls: number
  subscriptionCount: number
  lastLoginTime: string
  status: string
}

/**
 * 审批统计数据
 */
export interface ApprovalStatData {
  date: string
  totalApplications: number
  pendingApplications: number
  approvedApplications: number
  rejectedApplications: number
  approvalRate: number
  avgProcessTime: number
}

/**
 * 统计查询参数
 */
export interface StatisticsQuery {
  startDate?: string
  endDate?: string
  timeRange?: 'today' | 'week' | 'month' | 'year' | 'custom'
  page?: number
  size?: number
  keyword?: string
  category?: string
  status?: string
}

/**
 * 图表数据接口
 */
export interface ChartData {
  labels: string[]
  datasets: {
    name: string
    data: number[]
    type?: 'line' | 'bar' | 'pie'
  }[]
}

/**
 * 导出参数
 */
export interface ExportParams {
  type: 'excel' | 'pdf'
  dataType: 'interface' | 'user' | 'approval' | 'overview'
  startDate?: string
  endDate?: string
  filters?: Record<string, any>
}

// ========== API 方法 ==========

/**
 * 获取概览数据
 */
export const getOverviewData = async (params?: StatisticsQuery): Promise<OverviewData> => {
  return statisticsApi.get('/api/v1/statistics/overview', { params })
}

/**
 * 获取接口调用趋势图表数据
 */
export const getCallTrendChart = async (params?: StatisticsQuery): Promise<ChartData> => {
  return statisticsApi.get('/api/v1/statistics/charts/call-trend', { params })
}

/**
 * 获取接口成功率分布图表数据
 */
export const getSuccessRateChart = async (params?: StatisticsQuery): Promise<ChartData> => {
  return statisticsApi.get('/api/v1/statistics/charts/success-rate', { params })
}

/**
 * 获取用户活跃度图表数据
 */
export const getUserActivityChart = async (params?: StatisticsQuery): Promise<ChartData> => {
  return statisticsApi.get('/api/v1/statistics/charts/user-activity', { params })
}

/**
 * 获取审批流程统计图表数据
 */
export const getApprovalChart = async (params?: StatisticsQuery): Promise<ChartData> => {
  return statisticsApi.get('/api/v1/statistics/charts/approval', { params })
}

/**
 * 获取系统性能指标图表数据
 */
export const getPerformanceChart = async (params?: StatisticsQuery): Promise<ChartData> => {
  return statisticsApi.get('/api/v1/statistics/charts/performance', { params })
}

/**
 * 获取接口统计列表
 */
export const getInterfaceStatsList = async (params?: StatisticsQuery): Promise<{
  list: InterfaceStatData[]
  total: number
  current: number
  size: number
}> => {
  return statisticsApi.get('/api/v1/statistics/interfaces', { params })
}

/**
 * 获取用户统计列表
 */
export const getUserStatsList = async (params?: StatisticsQuery): Promise<{
  list: UserStatData[]
  total: number
  current: number
  size: number
}> => {
  return statisticsApi.get('/api/v1/statistics/users', { params })
}

/**
 * 获取审批统计列表
 */
export const getApprovalStatsList = async (params?: StatisticsQuery): Promise<{
  list: ApprovalStatData[]
  total: number
  current: number
  size: number
}> => {
  return statisticsApi.get('/api/v1/statistics/approvals', { params })
}

/**
 * 获取接口详细统计信息
 */
export const getInterfaceDetailStats = async (interfaceId: string, params?: StatisticsQuery): Promise<any> => {
  return statisticsApi.get(`/api/v1/statistics/interfaces/${interfaceId}`, { params })
}

/**
 * 获取用户详细统计信息
 */
export const getUserDetailStats = async (userId: string, params?: StatisticsQuery): Promise<any> => {
  return statisticsApi.get(`/api/v1/statistics/users/${userId}`, { params })
}

/**
 * 导出统计数据
 */
export const exportStatisticsData = async (params: ExportParams): Promise<Blob> => {
  const response = await statisticsApi.post('/api/v1/statistics/export', params, {
    responseType: 'blob'
  })
  return response.data
}

/**
 * 获取实时统计数据
 */
export const getRealTimeStats = async (): Promise<{
  currentOnlineUsers: number
  currentCalls: number
  systemLoad: number
  responseTime: number
}> => {
  return statisticsApi.get('/api/v1/statistics/realtime')
}

/**
 * 获取热门接口排行
 */
export const getPopularInterfaces = async (params?: {
  limit?: number
  timeRange?: string
}): Promise<InterfaceStatData[]> => {
  return statisticsApi.get('/api/v1/statistics/popular-interfaces', { params })
}

/**
 * 获取系统性能指标
 */
export const getSystemPerformance = async (params?: StatisticsQuery): Promise<{
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  networkIO: number
  avgResponseTime: number
  concurrentUsers: number
}> => {
  return statisticsApi.get('/api/v1/statistics/system-performance', { params })
}

/**
 * 获取错误统计
 */
export const getErrorStats = async (params?: StatisticsQuery): Promise<{
  totalErrors: number
  errorRate: number
  errorTypes: { type: string; count: number }[]
  errorTrend: ChartData
}> => {
  return statisticsApi.get('/api/v1/statistics/errors', { params })
}

export default {
  getOverviewData,
  getCallTrendChart,
  getSuccessRateChart,
  getUserActivityChart,
  getApprovalChart,
  getPerformanceChart,
  getInterfaceStatsList,
  getUserStatsList,
  getApprovalStatsList,
  getInterfaceDetailStats,
  getUserDetailStats,
  exportStatisticsData,
  getRealTimeStats,
  getPopularInterfaces,
  getSystemPerformance,
  getErrorStats
}