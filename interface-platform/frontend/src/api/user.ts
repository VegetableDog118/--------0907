import axios, { type AxiosResponse } from 'axios'

// 使用真实的后端API
const userApi = axios.create({
  baseURL: import.meta.env.VITE_USER_API_BASE_URL || 'http://localhost:8087/user-service',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
userApi.interceptors.request.use(
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
userApi.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.success) {
      return data.data  // 返回实际的数据部分
    } else {
      throw new Error(data.message || '请求失败')
    }
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          // 未认证，清除token并跳转到登录页
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          localStorage.removeItem('permissions')
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
export interface UserRegisterRequest {
  username: string
  password: string
  companyName: string
  creditCode: string
  contactName: string
  phone: string
  email: string
  department?: string
  position?: string
}

export interface UserRegisterResponse {
  userId: string
  status: string
  createTime: string
  message: string
}

export interface UserLoginRequest {
  account: string
  password: string
  captcha?: string
}

export interface UserLoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userId: string
  username: string
  companyName: string
  role: string
  permissions: string[]
}

export interface UserInfoResponse {
  userId: string
  username: string
  companyName: string
  creditCode: string
  contactName: string
  phone: string
  email: string
  department: string
  position: string
  role: string
  status: string
  createTime: string
  lastLoginTime?: string
}

export interface UserUpdateRequest {
  contactName?: string
  phone?: string
  email?: string
  department?: string
  position?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ApiKeyResponse {
  appId: string
  appSecret: string
  status: string
  createTime: string
  lastUsedTime?: string
  permissions: string[]
}

export interface TokenInfo {
  userId: string
  username: string
  companyName: string
  roles: string[]
  permissions: string[]
  issuedAt: string
  expiresAt: string
  tokenType: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

// API方法

/**
 * 用户注册
 */
export const registerUser = async (data: UserRegisterRequest): Promise<UserRegisterResponse> => {
  return userApi.post('/api/v1/users/register', data)
}

/**
 * 用户登录
 */
export const loginUser = async (data: UserLoginRequest): Promise<UserLoginResponse> => {
  return userApi.post('/api/v1/users/login', data)
}

/**
 * 用户登出
 */
export const logoutUser = async (): Promise<void> => {
  return userApi.post('/api/v1/users/logout')
}

/**
 * 获取当前用户信息
 */
export const getCurrentUserInfo = async (): Promise<UserInfoResponse> => {
  return userApi.get('/api/v1/users/profile')
}

/**
 * 更新用户信息
 */
export const updateUserInfo = async (data: UserUpdateRequest): Promise<void> => {
  return userApi.put('/api/v1/users/profile', data)
}

/**
 * 修改密码
 */
export const changePassword = async (data: ChangePasswordRequest): Promise<void> => {
  return userApi.put('/api/v1/users/password', data)
}

/**
 * 获取API密钥
 */
export const getApiKey = async (): Promise<ApiKeyResponse> => {
  return userApi.get('/api/v1/users/api-key')
}

/**
 * 重置API密钥
 */
export const resetApiKey = async (): Promise<ApiKeyResponse> => {
  return userApi.post('/api/v1/users/api-key/reset')
}

/**
 * 获取用户权限列表
 */
export const getUserPermissions = async (): Promise<string[]> => {
  return userApi.get('/api/v1/users/permissions')
}

/**
 * 验证Token
 */
export const validateToken = async (token: string): Promise<TokenInfo> => {
  return userApi.post('/api/v1/users/validate-token', null, {
    params: { token }
  })
}

// 管理员接口

/**
 * 获取用户列表（管理员）
 */
export const getUserList = async (
  page: number = 1,
  size: number = 10,
  status?: string,
  keyword?: string
): Promise<PageResponse<UserInfoResponse>> => {
  return userApi.get('/api/v1/users/list', {
    params: { page, size, status, keyword }
  })
}

/**
 * 获取指定用户信息（管理员）
 */
export const getUserById = async (userId: string): Promise<UserInfoResponse> => {
  return userApi.get(`/api/v1/users/${userId}`)
}

/**
 * 审核通过用户（管理员）
 */
export const approveUser = async (userId: string): Promise<void> => {
  return userApi.post(`/api/v1/users/${userId}/approve`)
}

/**
 * 审核拒绝用户（管理员）
 */
export const rejectUser = async (userId: string, reason: string): Promise<void> => {
  return userApi.post(`/api/v1/users/${userId}/reject`, null, {
    params: { reason }
  })
}

/**
 * 锁定用户（管理员）
 */
export const lockUser = async (userId: string, reason: string): Promise<void> => {
  return userApi.post(`/api/v1/users/${userId}/lock`, null, {
    params: { reason }
  })
}

/**
 * 解锁用户（管理员）
 */
export const unlockUser = async (userId: string): Promise<void> => {
  return userApi.post(`/api/v1/users/${userId}/unlock`)
}

export default userApi