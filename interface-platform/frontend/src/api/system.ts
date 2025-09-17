import request from '@/utils/request'

// 系统统计数据接口
export interface SystemStats {
  userCount: number
  roleCount: number
  configCount: number
  datasourceCount: number
  systemStatus: string
  todayLogCount: number
}

// 用户管理相关接口
export interface User {
  id: string
  username: string
  email: string
  phone: string
  realName: string
  role: string
  status: string
  createTime: string
  updateTime: string
  lastLoginTime?: string
}

export interface CreateUserRequest {
  username: string
  email: string
  phone: string
  realName: string
  role: string
  password: string
}

export interface UpdateUserRequest {
  id: string
  email?: string
  phone?: string
  realName?: string
  role?: string
  status?: string
}

// 角色管理相关接口
export interface Role {
  id: string
  name: string
  code: string
  description: string
  permissions: string[]
  status: string
  createTime: string
  updateTime: string
}

export interface CreateRoleRequest {
  name: string
  code: string
  description: string
  permissions: string[]
}

export interface UpdateRoleRequest {
  id: string
  name?: string
  description?: string
  permissions?: string[]
  status?: string
}

// 系统配置相关接口
export interface SystemConfig {
  id: string
  key: string
  value: string
  description: string
  type: string
  category: string
  updateTime: string
  updateBy: string
}

export interface UpdateConfigRequest {
  id: string
  value: string
}

// 数据源管理相关接口
export interface Datasource {
  id: string
  name: string
  type: string
  host: string
  port: number
  database: string
  username: string
  status: string
  createTime: string
  updateTime: string
}

export interface CreateDatasourceRequest {
  name: string
  type: string
  host: string
  port: number
  database: string
  username: string
  password: string
}

export interface UpdateDatasourceRequest {
  id: string
  name?: string
  host?: string
  port?: number
  database?: string
  username?: string
  password?: string
  status?: string
}

// 操作日志相关接口
export interface OperationLog {
  id: string
  userId: string
  username: string
  operation: string
  module: string
  method: string
  params: string
  ip: string
  userAgent: string
  executeTime: number
  status: string
  errorMsg?: string
  createTime: string
}

// 系统监控相关接口
export interface SystemMonitorData {
  cpu: {
    usage: number
    cores: number
  }
  memory: {
    total: number
    used: number
    free: number
    usage: number
  }
  disk: {
    total: number
    used: number
    free: number
    usage: number
  }
  network: {
    upload: number
    download: number
  }
  jvm: {
    heapUsed: number
    heapMax: number
    heapUsage: number
    nonHeapUsed: number
    nonHeapMax: number
  }
}

// API 方法

// 获取系统统计数据
export const getSystemStats = (): Promise<SystemStats> => {
  return request({
    url: '/system/stats',
    method: 'get'
  })
}

// 用户管理 API
export const getUserList = (params: {
  page?: number
  size?: number
  username?: string
  role?: string
  status?: string
}): Promise<{
  records: User[]
  total: number
  current: number
  size: number
}> => {
  return request({
    url: '/system/users',
    method: 'get',
    params
  })
}

export const createUser = (data: CreateUserRequest): Promise<void> => {
  return request({
    url: '/system/users',
    method: 'post',
    data
  })
}

export const updateUser = (data: UpdateUserRequest): Promise<void> => {
  return request({
    url: `/system/users/${data.id}`,
    method: 'put',
    data
  })
}

export const deleteUser = (id: string): Promise<void> => {
  return request({
    url: `/system/users/${id}`,
    method: 'delete'
  })
}

export const resetUserPassword = (id: string): Promise<{ password: string }> => {
  return request({
    url: `/system/users/${id}/reset-password`,
    method: 'post'
  })
}

// 角色管理 API
export const getRoleList = (params: {
  page?: number
  size?: number
  name?: string
  status?: string
}): Promise<{
  records: Role[]
  total: number
  current: number
  size: number
}> => {
  return request({
    url: '/system/roles',
    method: 'get',
    params
  })
}

export const getAllRoles = (): Promise<Role[]> => {
  return request({
    url: '/system/roles/all',
    method: 'get'
  })
}

export const createRole = (data: CreateRoleRequest): Promise<void> => {
  return request({
    url: '/system/roles',
    method: 'post',
    data
  })
}

export const updateRole = (data: UpdateRoleRequest): Promise<void> => {
  return request({
    url: `/system/roles/${data.id}`,
    method: 'put',
    data
  })
}

export const deleteRole = (id: string): Promise<void> => {
  return request({
    url: `/system/roles/${id}`,
    method: 'delete'
  })
}

export const getAllPermissions = (): Promise<{
  id: string
  name: string
  code: string
  type: string
  children?: any[]
}[]> => {
  return request({
    url: '/system/permissions',
    method: 'get'
  })
}

// 系统配置 API
export const getConfigList = (params: {
  page?: number
  size?: number
  key?: string
  category?: string
}): Promise<{
  records: SystemConfig[]
  total: number
  current: number
  size: number
}> => {
  return request({
    url: '/system/configs',
    method: 'get',
    params
  })
}

export const updateConfig = (data: UpdateConfigRequest): Promise<void> => {
  return request({
    url: `/system/configs/${data.id}`,
    method: 'put',
    data
  })
}

// 数据源管理 API
export const getDatasourceList = (params: {
  page?: number
  size?: number
  name?: string
  type?: string
  status?: string
}): Promise<{
  records: Datasource[]
  total: number
  current: number
  size: number
}> => {
  return request({
    url: '/system/datasources',
    method: 'get',
    params
  })
}

export const createDatasource = (data: CreateDatasourceRequest): Promise<void> => {
  return request({
    url: '/system/datasources',
    method: 'post',
    data
  })
}

export const updateDatasource = (data: UpdateDatasourceRequest): Promise<void> => {
  return request({
    url: `/system/datasources/${data.id}`,
    method: 'put',
    data
  })
}

export const deleteDatasource = (id: string): Promise<void> => {
  return request({
    url: `/system/datasources/${id}`,
    method: 'delete'
  })
}

export const testDatasourceConnection = (id: string): Promise<{
  success: boolean
  message: string
  responseTime: number
}> => {
  return request({
    url: `/system/datasources/${id}/test`,
    method: 'post'
  })
}

// 操作日志 API
export const getOperationLogList = (params: {
  page?: number
  size?: number
  username?: string
  module?: string
  operation?: string
  startTime?: string
  endTime?: string
}): Promise<{
  records: OperationLog[]
  total: number
  current: number
  size: number
}> => {
  return request({
    url: '/system/logs',
    method: 'get',
    params
  })
}

export const clearOperationLogs = (beforeDate: string): Promise<void> => {
  return request({
    url: '/system/logs/clear',
    method: 'delete',
    params: { beforeDate }
  })
}

// 系统监控 API
export const getSystemMonitorData = (): Promise<SystemMonitorData> => {
  return request({
    url: '/system/monitor',
    method: 'get'
  })
}

export const getSystemHealth = (): Promise<{
  status: string
  components: Record<string, {
    status: string
    details?: any
  }>
}> => {
  return request({
    url: '/system/health',
    method: 'get'
  })
}