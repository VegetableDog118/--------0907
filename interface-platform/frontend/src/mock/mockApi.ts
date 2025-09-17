import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import type { 
  InterfaceListRequest, 
  InterfaceListResponse, 
  InterfaceDetailResponse,
  InterfaceManagementRequest,
  CreateInterfaceRequest,
  UpdateInterfaceRequest
} from '@/api/interface'
import type { 
  UserRegisterRequest, 
  UserRegisterResponse, 
  UserLoginRequest, 
  UserLoginResponse,
  UserInfoResponse,
  UserUpdateRequest
} from '@/api/user'
import type {
  SubscriptionApplicationRequest,
  ApplicationListRequest,
  ApplicationListResponse,
  ApprovalRequest,
  BatchApprovalRequest
} from '@/api/approval'

// 导入mock数据
import { mockCategories, getCategoryStats } from './categories'
import { 
  mockUsers, 
  mockLoginResponse, 
  getUserByUsername, 
  getUserById, 
  getUserStats 
} from './users'
import { 
  mockInterfaces, 
  mockInterfaceParameters,
  getInterfacesByCategory,
  getInterfacesByStatus,
  searchInterfaces,
  getInterfaceStats,
  getInterfaceDetail
} from './interfaces'
import { 
  mockApplications, 
  mockUserSubscriptions,
  getApprovalStats,
  getApplicationsByUser,
  getApplicationsByStatus,
  getSubscriptionsByUser,
  getApplicationDetailWithInfo
} from './applications'

// 创建Mock适配器
let mockAdapter: MockAdapter | null = null

// 分页工具函数
function paginate<T>(data: T[], page: number, size: number) {
  const start = (page - 1) * size
  const end = start + size
  const records = data.slice(start, end)
  const total = data.length
  const pages = Math.ceil(total / size)
  
  return {
    records,
    total,
    current: page,
    size,
    pages,
    hasNext: page < pages,
    hasPrevious: page > 1
  }
}

// 延迟函数，模拟网络请求
function delay(ms: number = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// 初始化Mock服务
export function initMockApi() {
  // 如果已经初始化过，先清理
  if (mockAdapter) {
    mockAdapter.restore()
  }

  // 创建新的Mock适配器，拦截所有axios实例
  mockAdapter = new MockAdapter(axios, { 
    delayResponse: 300,
    onNoMatch: 'passthrough'
  })
  
  console.log('🚀 正在初始化Mock API服务...')
  console.log('📊 Mock数据统计:')
  console.log('  - 分类数量:', mockCategories.length)
  console.log('  - 接口数量:', mockInterfaces.length)
  console.log('  - 用户数量:', mockUsers.length)
  console.log('  - 申请数量:', mockApplications.length)

  // ========== 接口相关API ==========
  
  // 获取接口分类列表
  mockAdapter.onGet('/api/v1/interfaces/categories').reply(async () => {
    console.log('📋 Mock API: 获取接口分类列表')
    console.log('📋 分类数据:', mockCategories)
    await delay(200)
    const response = {
      code: 200,
      data: mockCategories,
      message: '获取分类列表成功'
    }
    console.log('📋 Mock API 响应:', response)
    return [200, response]
  })

  // 获取接口列表
  mockAdapter.onGet('/api/v1/interfaces/list').reply(async (config) => {
    console.log('📝 Mock API: 获取接口列表', config.params)
    console.log('📝 原始接口数据量:', mockInterfaces.length)
    await delay(300)
    const params = config.params as InterfaceListRequest
    let filteredInterfaces = [...mockInterfaces]
    console.log('📝 初始筛选后接口数量:', filteredInterfaces.length)

    // 按分类筛选
    if (params.categoryId) {
      filteredInterfaces = getInterfacesByCategory(params.categoryId)
      console.log('📝 按分类筛选后接口数量:', filteredInterfaces.length)
    }

    // 按状态筛选
    if (params.status) {
      filteredInterfaces = filteredInterfaces.filter(iface => iface.status === params.status)
      console.log('📝 按状态筛选后接口数量:', filteredInterfaces.length)
    }

    // 关键词搜索
    if (params.keyword) {
      const keyword = params.keyword.toLowerCase()
      filteredInterfaces = filteredInterfaces.filter(iface => 
        iface.interfaceName.toLowerCase().includes(keyword) ||
        iface.description?.toLowerCase().includes(keyword) ||
        iface.interfacePath.toLowerCase().includes(keyword)
      )
      console.log('📝 关键词搜索后接口数量:', filteredInterfaces.length)
    }

    const result = paginate(filteredInterfaces, params.page, params.size)
    console.log('📝 分页结果:', result)
    const response = {
      code: 200,
      data: result,
      message: '获取接口列表成功'
    }
    console.log('📝 Mock API 响应:', response)
    return [200, response]
  })

  // 获取接口详情
  mockAdapter.onGet(/\/api\/v1\/interfaces\/(.+)/).reply(async (config) => {
    await delay(250)
    const interfaceId = config.url?.split('/').pop()
    if (!interfaceId) {
      return [404, { success: false, message: '接口ID不能为空' }]
    }

    const detail = getInterfaceDetail(interfaceId)
    if (!detail) {
      return [404, { success: false, message: '接口不存在' }]
    }

    return [200, {
      code: 200,
      data: detail,
      message: '获取接口详情成功'
    }]
  })

  // 搜索接口
  mockAdapter.onGet('/api/v1/interfaces/search').reply(async (config) => {
    await delay(400)
    const { keyword, page = 1, size = 20 } = config.params
    const results = searchInterfaces(keyword || '')
    const paginatedResults = paginate(results, page, size)
    
    return [200, {
      code: 200,
      data: paginatedResults,
      message: '搜索完成'
    }]
  })

  // 获取接口参数
  mockAdapter.onGet(/\/api\/v1\/interfaces\/(.+)\/parameters/).reply(async (config) => {
    await delay(200)
    const interfaceId = config.url?.split('/')[4]
    if (!interfaceId) {
      return [404, { success: false, message: '接口ID不能为空' }]
    }

    const parameters = mockInterfaceParameters[interfaceId] || []
    return [200, {
      code: 200,
      data: parameters,
      message: '获取接口参数成功'
    }]
  })

  // 测试接口调用
  mockAdapter.onPost(/\/api\/v1\/interfaces\/(.+)\/test/).reply(async (config) => {
    await delay(800)
    const interfaceId = config.url?.split('/')[4]
    const interfaceInfo = mockInterfaces.find(iface => iface.id === interfaceId)
    
    if (!interfaceInfo) {
      return [404, { success: false, message: '接口不存在' }]
    }

    // 模拟测试结果
    const mockResult = {
      success: true,
      data: {
        executionTime: Math.floor(Math.random() * 500) + 100,
        resultCount: Math.floor(Math.random() * 100) + 1,
        sampleData: [
          { id: 1, name: '测试数据1', value: Math.random() * 100 },
          { id: 2, name: '测试数据2', value: Math.random() * 100 }
        ]
      },
      message: '接口测试成功'
    }

    return [200, mockResult]
  })

  // 获取接口统计信息
  mockAdapter.onGet('/api/v1/interfaces/stats').reply(async () => {
    await delay(300)
    const stats = getInterfaceStats()
    return [200, {
      code: 200,
      data: stats,
      message: '获取统计信息成功'
    }]
  })

  // ========== 用户相关API ==========

  // 用户登录
  mockAdapter.onPost('/api/v1/users/login').reply(async (config) => {
    console.log('🔐 Mock API: 用户登录', JSON.parse(config.data))
    await delay(500)
    const loginData = JSON.parse(config.data) as UserLoginRequest
    
    // 简单验证
    if (loginData.account === 'admin' && loginData.password === '123456') {
      return [200, {
        code: 200,
        data: mockLoginResponse,
        message: '登录成功'
      }]
    }

    // 检查其他用户
    const user = getUserByUsername(loginData.account)
    if (user && loginData.password === '123456') {
      const loginResponse = {
        ...mockLoginResponse,
        userId: user.userId,
        username: user.username,
        companyName: user.companyName,
        role: user.role
      }
      return [200, {
        code: 200,
        data: loginResponse,
        message: '登录成功'
      }]
    }

    return [401, {
      code: 401,
      message: '用户名或密码错误'
    }]
  })

  // 用户注册
  mockAdapter.onPost('/api/v1/users/register').reply(async (config) => {
    await delay(600)
    const registerData = JSON.parse(config.data) as UserRegisterRequest
    
    // 检查用户名是否已存在
    const existingUser = getUserByUsername(registerData.username)
    if (existingUser) {
      return [400, {
        code: 400,
        message: '用户名已存在'
      }]
    }

    const response: UserRegisterResponse = {
      userId: `user-${Date.now()}`,
      status: 'pending',
      createTime: new Date().toISOString(),
      message: '注册申请已提交，请等待审核'
    }

    return [200, {
      code: 200,
      data: response,
      message: '注册成功'
    }]
  })

  // 获取当前用户信息
  mockAdapter.onGet('/api/v1/users/profile').reply(async () => {
    await delay(200)
    // 默认返回admin用户信息
    const userInfo = mockUsers[0]
    return [200, {
      code: 200,
      data: userInfo,
      message: '获取用户信息成功'
    }]
  })

  // 更新用户信息
  mockAdapter.onPut('/api/v1/users/profile').reply(async (config) => {
    await delay(400)
    const updateData = JSON.parse(config.data) as UserUpdateRequest
    
    return [200, {
      code: 200,
      data: null,
      message: '用户信息更新成功'
    }]
  })

  // 用户登出
  mockAdapter.onPost('/api/v1/users/logout').reply(async () => {
    await delay(200)
    return [200, {
      code: 200,
      data: null,
      message: '登出成功'
    }]
  })

  // ========== 申请审批相关API ==========

  // 提交订阅申请
  mockAdapter.onPost('/api/v1/applications').reply(async (config) => {
    await delay(500)
    const applicationData = JSON.parse(config.data) as SubscriptionApplicationRequest
    
    const applicationId = `app-${Date.now()}`
    return [200, {
      code: 200,
      data: applicationId,
      message: '申请提交成功'
    }]
  })

  // 获取申请列表
  mockAdapter.onGet('/api/v1/applications').reply(async (config) => {
    await delay(300)
    const params = config.params as ApplicationListRequest
    let filteredApplications = [...mockApplications]

    // 按状态筛选
    if (params.status) {
      filteredApplications = getApplicationsByStatus(params.status)
    }

    // 按用户筛选
    if (params.userId) {
      filteredApplications = getApplicationsByUser(params.userId)
    }

    // 按时间筛选
    if (params.startDate) {
      filteredApplications = filteredApplications.filter(app => app.submitTime >= params.startDate!)
    }
    if (params.endDate) {
      filteredApplications = filteredApplications.filter(app => app.submitTime <= params.endDate!)
    }

    const result = paginate(filteredApplications, params.page, params.size)
    return [200, {
      code: 200,
      data: result,
      message: '获取申请列表成功'
    }]
  })

  // 获取申请详情
  mockAdapter.onGet(/\/api\/v1\/applications\/(.+)/).reply(async (config) => {
    await delay(250)
    const applicationId = config.url?.split('/').pop()
    if (!applicationId) {
      return [404, { success: false, message: '申请ID不能为空' }]
    }

    const detail = getApplicationDetailWithInfo(applicationId)
    if (!detail) {
      return [404, { success: false, message: '申请不存在' }]
    }

    return [200, {
      code: 200,
      data: detail,
      message: '获取申请详情成功'
    }]
  })

  // 审批申请
  mockAdapter.onPost('/api/v1/applications/approve').reply(async (config) => {
    await delay(600)
    const approvalData = JSON.parse(config.data) as ApprovalRequest
    
    return [200, {
      code: 200,
      data: null,
      message: `申请已${approvalData.action === 'approve' ? '批准' : '拒绝'}`
    }]
  })

  // 批量审批
  mockAdapter.onPost('/api/v1/applications/batch-approve').reply(async (config) => {
    await delay(800)
    const batchData = JSON.parse(config.data) as BatchApprovalRequest
    
    return [200, {
      code: 200,
      data: null,
      message: `批量${batchData.action === 'approve' ? '批准' : '拒绝'}成功`
    }]
  })

  // 获取用户订阅列表
  mockAdapter.onGet('/api/v1/subscriptions').reply(async (config) => {
    await delay(300)
    const { userId, page = 1, size = 20 } = config.params
    let subscriptions = [...mockUserSubscriptions]

    if (userId) {
      subscriptions = getSubscriptionsByUser(userId)
    }

    const result = paginate(subscriptions, page, size)
    return [200, {
      success: true,
      data: result,
      message: '获取订阅列表成功'
    }]
  })

  // ========== 统计相关API ==========

  // 获取审批统计
  mockAdapter.onGet('/api/v1/statistics/approval').reply(async () => {
    await delay(300)
    const stats = getApprovalStats()
    return [200, {
      code: 200,
      data: stats,
      message: '获取审批统计成功'
    }]
  })

  // 获取用户统计
  mockAdapter.onGet('/api/v1/statistics/users').reply(async () => {
    await delay(300)
    const stats = getUserStats()
    return [200, {
      success: true,
      data: stats,
      message: '获取用户统计成功'
    }]
  })

  // 获取分类统计
  mockAdapter.onGet('/api/v1/statistics/categories').reply(async () => {
    await delay(200)
    const stats = getCategoryStats()
    return [200, {
      code: 200,
      data: stats,
      message: '获取分类统计成功'
    }]
  })

  // 添加通用拦截器，捕获所有未匹配的请求
  mockAdapter.onAny().reply((config) => {
    console.warn('⚠️ 未匹配的API请求:', config.method?.toUpperCase(), config.url)
    return [404, {
       code: 404,
       message: `API接口未找到: ${config.method?.toUpperCase()} ${config.url}`
     }]
  })

  console.log('🎭 Mock API 服务已启动')
  console.log('📊 已加载样例数据：')
  console.log(`  - 接口分类: ${mockCategories.length} 个`)
  console.log(`  - 接口数据: ${mockInterfaces.length} 个`)
  console.log(`  - 用户数据: ${mockUsers.length} 个`)
  console.log(`  - 申请数据: ${mockApplications.length} 个`)
  console.log(`  - 订阅数据: ${mockUserSubscriptions.length} 个`)
  console.log('🔍 Mock拦截器已配置，将拦截所有axios请求')
}

// 停止Mock服务
export function stopMockApi() {
  if (mockAdapter) {
    mockAdapter.restore()
    mockAdapter = null
    console.log('🛑 Mock API 服务已停止')
  }
}

// 检查Mock服务状态
export function isMockApiActive(): boolean {
  return mockAdapter !== null
}