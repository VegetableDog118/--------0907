import axios, { type AxiosResponse } from 'axios'

// 使用真实的后端API
const interfaceApi = axios.create({
  baseURL: import.meta.env.VITE_INTERFACE_API_BASE_URL || 'http://localhost:8083',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
interfaceApi.interceptors.request.use(
  (config) => {
    // 添加认证token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
interfaceApi.interceptors.response.use(
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
export interface InterfaceCategory {
  id: string
  categoryCode: string
  categoryName: string
  description?: string
  color?: string
  sortOrder: number
  status: number
  createTime: string
  updateTime: string
  interfaceCount?: number
}

export interface InterfaceInfo {
  id: string
  interfaceName: string
  interfacePath: string
  description?: string
  categoryId: string
  categoryName?: string
  dataSourceId: string
  tableName: string
  requestMethod: 'GET' | 'POST' | 'PUT' | 'DELETE'
  status: 'unpublished' | 'published' | 'offline'
  version: string
  sqlTemplate?: string
  responseFormat?: any
  rateLimit?: number
  timeout: number
  createTime: string
  updateTime: string
  publishTime?: string
  offlineTime?: string
  createBy: string
  updateBy?: string
  publishBy?: string
  offlineBy?: string
}

export interface InterfaceParameter {
  id: string
  interfaceId: string
  paramName: string
  paramType: 'string' | 'integer' | 'number' | 'boolean' | 'date' | 'datetime'
  paramLocation: 'query' | 'body' | 'header' | 'path'
  description?: string
  required: boolean
  defaultValue?: string
  validationRule?: string
  example?: string
  sortOrder: number
  createTime: string
  updateTime: string
}

export interface InterfaceListRequest {
  categoryId?: string
  keyword?: string
  status?: string
  page: number
  size: number
}

export interface InterfaceListResponse {
  records: InterfaceInfo[]
  total: number
  current: number
  size: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface InterfaceDetailResponse {
  interface: InterfaceInfo
  parameters: InterfaceParameter[]
  category: InterfaceCategory
}

// API方法

/**
 * 获取接口分类列表
 */
export const getInterfaceCategories = async (): Promise<InterfaceCategory[]> => {
  return interfaceApi.get('/api/v1/interfaces/categories')
}

/**
 * 获取接口列表
 */
export const getInterfaceList = async (params: InterfaceListRequest): Promise<InterfaceListResponse> => {
  return interfaceApi.get('/api/v1/interfaces/list', { params })
}

/**
 * 获取接口详情
 */
export const getInterfaceDetail = async (interfaceId: string): Promise<InterfaceDetailResponse> => {
  return interfaceApi.get(`/api/v1/interfaces/${interfaceId}`)
}

/**
 * 搜索接口
 */
export const searchInterfaces = async (keyword: string, page: number = 1, size: number = 20): Promise<InterfaceListResponse> => {
  return interfaceApi.get('/api/v1/interfaces/search', {
    params: { keyword, page, size }
  })
}

/**
 * 获取接口参数
 */
export const getInterfaceParameters = async (interfaceId: string): Promise<InterfaceParameter[]> => {
  return interfaceApi.get(`/api/v1/interfaces/${interfaceId}/parameters`)
}

/**
 * 测试接口调用
 */
export const testInterfaceCall = async (interfaceId: string, params: any): Promise<any> => {
  return interfaceApi.post(`/api/v1/interfaces/${interfaceId}/test`, params)
}

/**
 * 获取接口统计信息
 */
export const getInterfaceStats = async (): Promise<any> => {
  return interfaceApi.get('/api/v1/interfaces/stats')
}

// ========== 接口状态管理API ==========

/**
 * 上架接口
 */
export const publishInterface = async (interfaceId: string): Promise<void> => {
  return interfaceApi.post(`/api/v1/status/${interfaceId}/publish`)
}

/**
 * 下架接口
 */
export const unpublishInterface = async (interfaceId: string, reason: string): Promise<void> => {
  return interfaceApi.post(`/api/v1/status/${interfaceId}/offline`, {
    offlineReason: reason
  })
}

/**
 * 重新上架接口
 */
export const republishInterface = async (interfaceId: string): Promise<void> => {
  return interfaceApi.post(`/api/v1/status/${interfaceId}/republish`)
}

/**
 * 批量上架接口
 */
export const batchPublishInterfaces = async (interfaceIds: string[]): Promise<any> => {
  return interfaceApi.post('/api/v1/status/batch/publish', {
    interfaceIds
  })
}

/**
 * 批量下架接口
 */
export const batchUnpublishInterfaces = async (interfaceIds: string[], reason: string): Promise<any> => {
  return interfaceApi.post('/api/v1/status/batch/offline', {
    interfaceIds,
    offlineReason: reason
  })
}

/**
 * 获取接口状态统计
 */
export const getInterfaceStatusStats = async (): Promise<any> => {
  return interfaceApi.get('/api/v1/status/status/statistics')
}

// ========== 接口管理相关API ==========

/**
 * 接口管理列表查询参数
 */
export interface InterfaceManagementRequest {
  status?: string
  category?: string
  creator?: string
  page: number
  size: number
}

/**
 * 创建接口请求参数
 */
export interface CreateInterfaceRequest {
  interfaceName: string
  interfacePath: string
  description?: string
  categoryId: string
  dataSourceId: string
  tableName: string
  requestMethod: 'GET' | 'POST' | 'PUT' | 'DELETE'
  rateLimit?: number
  timeout: number
  sqlTemplate: string
  parameters: {
    paramName: string
    paramType: string
    paramLocation: string
    required: boolean
    description?: string
    defaultValue?: string
  }[]
}

/**
 * 更新接口请求参数
 */
export interface UpdateInterfaceRequest extends Partial<CreateInterfaceRequest> {
  id: string
}

/**
 * 数据源信息
 */
export interface DataSource {
  id: string
  sourceName: string
  sourceType: string
  host: string
  port: number
  databaseName: string
  status: number
}

/**
 * 数据表信息
 */
export interface DataTable {
  name: string
  comment?: string
}

/**
 * 表字段信息
 */
export interface TableColumn {
  name: string
  type: string
  comment?: string
  nullable: boolean
}

/**
 * 获取接口管理列表
 */
export const getInterfaceManagementList = async (params: InterfaceManagementRequest): Promise<InterfaceListResponse> => {
  return interfaceApi.get('/api/v1/interfaces/list', { params })
}

/**
 * 创建接口
 */
export const createInterface = async (data: CreateInterfaceRequest): Promise<InterfaceInfo> => {
  return interfaceApi.post('/api/v1/management/interfaces', data)
}

/**
 * 更新接口
 */
export const updateInterface = async (data: UpdateInterfaceRequest): Promise<InterfaceInfo> => {
  return interfaceApi.put(`/api/v1/management/interfaces/${data.id}`, data)
}

/**
 * 删除接口
 */
export const deleteInterface = async (interfaceId: string): Promise<void> => {
  return interfaceApi.delete(`/api/v1/management/interfaces/${interfaceId}`)
}

/**
 * 批量删除接口
 */
export const batchDeleteInterfaces = async (interfaceIds: string[]): Promise<void> => {
  return interfaceApi.delete('/api/v1/management/interfaces/batch', {
    data: { interfaceIds }
  })
}

// 移除重复的接口状态管理API定义，使用前面已定义的版本

/**
 * 获取数据源列表
 */
export const getDataSources = async (): Promise<DataSource[]> => {
  return interfaceApi.get('/api/v1/management/datasources')
}

/**
 * 获取数据源的表列表
 */
export const getDataSourceTables = async (dataSourceId: string): Promise<DataTable[]> => {
  return interfaceApi.get(`/api/v1/management/datasources/${dataSourceId}/tables`)
}

/**
 * 获取表字段信息
 */
export const getTableColumns = async (dataSourceId: string, tableName: string): Promise<TableColumn[]> => {
  return interfaceApi.get(`/api/v1/management/datasources/${dataSourceId}/tables/${tableName}/columns`)
}

/**
 * 预览SQL执行结果
 */
export const previewSqlResult = async (dataSourceId: string, sql: string, params?: any): Promise<any> => {
  return interfaceApi.post(`/api/v1/management/datasources/${dataSourceId}/preview`, {
    sql,
    params
  })
}

/**
 * 获取接口调用统计
 */
export const getInterfaceCallStats = async (interfaceId: string, startDate?: string, endDate?: string): Promise<any> => {
  return interfaceApi.get(`/api/v1/management/interfaces/${interfaceId}/stats`, {
    params: { startDate, endDate }
  })
}

/**
 * 获取接口监控数据
 */
export const getInterfaceMonitorData = async (interfaceId: string): Promise<any> => {
  return interfaceApi.get(`/api/v1/management/interfaces/${interfaceId}/monitor`)
}

// ========== 接口生成四步骤向导API ==========

/**
 * 接口生成配置
 */
export interface InterfaceGenerationConfig {
  interfaceName: string
  interfacePath: string
  description?: string
  categoryId: string
  requestMethod: 'GET' | 'POST'
  rateLimit?: number
  timeout?: number
  dataSourceId: string
  tableName: string
  parameters: {
    paramName: string
    paramType: string
    paramLocation: 'query' | 'body' | 'path'
    required: boolean
    description?: string
    defaultValue?: string
  }[]
}

/**
 * 接口生成请求
 */
export interface InterfaceGenerationRequest {
  config: InterfaceGenerationConfig
}

/**
 * 步骤1：获取数据源列表
 */
export const getGenerationDataSources = async (): Promise<DataSource[]> => {
  return interfaceApi.get('/api/v1/interfaces/generation/step1/datasources')
}

/**
 * 步骤1：获取数据源的表列表
 */
export const getGenerationTables = async (dataSourceId: string): Promise<DataTable[]> => {
  return interfaceApi.get(`/api/v1/interfaces/generation/step1/datasources/${dataSourceId}/tables`)
}

/**
 * 步骤1：获取表字段信息
 */
export const getGenerationTableColumns = async (dataSourceId: string, tableName: string): Promise<TableColumn[]> => {
  return interfaceApi.get(`/api/v1/interfaces/generation/step1/datasources/${dataSourceId}/tables/${tableName}/columns`)
}

/**
 * 步骤2：验证接口配置
 */
export const validateInterfaceConfig = async (config: any): Promise<any> => {
  return interfaceApi.post('/api/v1/interfaces/generation/step2/validate', config)
}

/**
 * 步骤3：生成参数模板
 */
export const generateParameterTemplate = async (config: InterfaceGenerationConfig): Promise<any> => {
  return interfaceApi.post('/api/v1/interfaces/generation/step3/parameters', { config })
}

/**
 * 步骤4：预览接口配置
 */
export const previewInterfaceConfig = async (request: InterfaceGenerationRequest): Promise<any> => {
  return interfaceApi.post('/api/v1/interfaces/generation/step4/preview', request)
}

/**
 * 步骤4：生成接口
 */
export const generateInterface = async (request: any): Promise<string> => {
  return interfaceApi.post('/api/v1/interfaces/generation/step4/generate', request)
}

// ========== MySQL表选择相关API ==========

/**
 * MySQL表信息
 */
export interface MysqlTableInfo {
  tableName: string
  tableComment: string
  tableType: string
  recordCount: number
}

/**
 * 表结构信息
 */
export interface TableStructureInfo {
  tableName: string
  columns: ColumnInfo[]
}

/**
 * 字段信息
 */
export interface ColumnInfo {
  columnName: string
  dataType: string
  columnComment: string
  isPrimaryKey: boolean
  isNullable: boolean
}

/**
 * 获取MySQL表列表
 */
export const getMysqlTables = async (): Promise<MysqlTableInfo[]> => {
  return interfaceApi.get('/api/v1/mysql/tables')
}

/**
 * 获取表结构信息
 */
export const getTableStructure = async (tableName: string): Promise<TableStructureInfo> => {
  return interfaceApi.get(`/api/v1/mysql/table-structure/${tableName}`)
}

export default interfaceApi