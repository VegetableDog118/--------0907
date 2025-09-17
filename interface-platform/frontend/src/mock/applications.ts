import type { SubscriptionApplication, UserInterfaceSubscription, ApprovalStats } from '@/api/approval'
import { mockUsers } from './users'
import { mockInterfaces } from './interfaces'

// 申请mock数据
export const mockApplications: SubscriptionApplication[] = [
  {
    id: 'app-001',
    userId: 'user-002',
    interfaceIds: ['if-001', 'if-002'],
    reason: '需要获取实时电价和历史交易数据用于交易决策分析',
    businessScenario: '电力交易业务',
    estimatedCalls: 10000,
    status: 'approved',
    submitTime: '2024-01-18T09:00:00Z',
    processTime: '2024-01-18T14:30:00Z',
    processBy: 'user-001',
    processComment: '申请理由充分，业务需求明确，批准使用',
    createTime: '2024-01-18T09:00:00Z',
    updateTime: '2024-01-18T14:30:00Z'
  },
  {
    id: 'app-002',
    userId: 'user-003',
    interfaceIds: ['if-003', 'if-004'],
    reason: '用于电力市场分析和负荷预测研究',
    businessScenario: '市场分析研究',
    estimatedCalls: 5000,
    status: 'approved',
    submitTime: '2024-01-19T10:15:00Z',
    processTime: '2024-01-19T16:20:00Z',
    processBy: 'user-001',
    processComment: '研究目的明确，数据使用合规，同意申请',
    createTime: '2024-01-19T10:15:00Z',
    updateTime: '2024-01-19T16:20:00Z'
  },
  {
    id: 'app-003',
    userId: 'user-004',
    interfaceIds: ['if-005'],
    reason: '运维监控需要实时获取设备状态信息',
    businessScenario: '设备运维监控',
    estimatedCalls: 20000,
    status: 'approved',
    submitTime: '2024-01-19T14:00:00Z',
    processTime: '2024-01-20T09:15:00Z',
    processBy: 'user-001',
    processComment: '运维需求合理，调用频次适中，批准',
    createTime: '2024-01-19T14:00:00Z',
    updateTime: '2024-01-20T09:15:00Z'
  },
  {
    id: 'app-004',
    userId: 'user-005',
    interfaceIds: ['if-007', 'if-008'],
    reason: '开发能源统计分析系统，需要历史数据支持',
    businessScenario: '系统开发',
    estimatedCalls: 8000,
    status: 'pending',
    submitTime: '2024-01-20T11:30:00Z',
    createTime: '2024-01-20T11:30:00Z',
    updateTime: '2024-01-20T11:30:00Z'
  },
  {
    id: 'app-005',
    userId: 'user-006',
    interfaceIds: ['if-001', 'if-003'],
    reason: '业务分析需要电价和负荷预测数据',
    businessScenario: '业务分析',
    estimatedCalls: 3000,
    status: 'pending',
    submitTime: '2024-01-20T13:45:00Z',
    createTime: '2024-01-20T13:45:00Z',
    updateTime: '2024-01-20T13:45:00Z'
  },
  {
    id: 'app-006',
    userId: 'user-007',
    interfaceIds: ['if-004', 'if-007'],
    reason: '市场部门需要价格分析和消费统计数据制定策略',
    businessScenario: '战略规划',
    estimatedCalls: 6000,
    status: 'pending',
    submitTime: '2024-01-20T15:20:00Z',
    createTime: '2024-01-20T15:20:00Z',
    updateTime: '2024-01-20T15:20:00Z'
  },
  {
    id: 'app-007',
    userId: 'user-008',
    interfaceIds: ['if-009'],
    reason: '监管工作需要查询用户权限信息',
    businessScenario: '监管审计',
    estimatedCalls: 1000,
    status: 'rejected',
    submitTime: '2024-01-19T16:00:00Z',
    processTime: '2024-01-20T10:30:00Z',
    processBy: 'user-001',
    processComment: '权限接口涉及敏感信息，需要更高级别审批',
    createTime: '2024-01-19T16:00:00Z',
    updateTime: '2024-01-20T10:30:00Z'
  },
  {
    id: 'app-008',
    userId: 'user-009',
    interfaceIds: ['if-002', 'if-004', 'if-008'],
    reason: '咨询项目需要综合分析电力市场数据',
    businessScenario: '咨询服务',
    estimatedCalls: 12000,
    status: 'approved',
    submitTime: '2024-01-18T08:30:00Z',
    processTime: '2024-01-18T17:45:00Z',
    processBy: 'user-001',
    processComment: '咨询业务需求合理，数据用途明确，同意',
    createTime: '2024-01-18T08:30:00Z',
    updateTime: '2024-01-18T17:45:00Z'
  },
  {
    id: 'app-009',
    userId: 'user-010',
    interfaceIds: ['if-003', 'if-007', 'if-008'],
    reason: '学术研究需要负荷预测和能源统计数据',
    businessScenario: '学术研究',
    estimatedCalls: 4000,
    status: 'approved',
    submitTime: '2024-01-19T09:20:00Z',
    processTime: '2024-01-19T15:10:00Z',
    processBy: 'user-001',
    processComment: '学术研究目的明确，支持科研工作',
    createTime: '2024-01-19T09:20:00Z',
    updateTime: '2024-01-19T15:10:00Z'
  },
  {
    id: 'app-010',
    userId: 'user-002',
    interfaceIds: ['if-005', 'if-006'],
    reason: '交易系统升级需要电网运行数据支持',
    businessScenario: '系统升级',
    estimatedCalls: 15000,
    status: 'pending',
    submitTime: '2024-01-20T16:30:00Z',
    createTime: '2024-01-20T16:30:00Z',
    updateTime: '2024-01-20T16:30:00Z'
  },
  {
    id: 'app-011',
    userId: 'user-003',
    interfaceIds: ['if-001'],
    reason: '实时监控系统需要电价数据',
    businessScenario: '实时监控',
    estimatedCalls: 50000,
    status: 'rejected',
    submitTime: '2024-01-20T08:15:00Z',
    processTime: '2024-01-20T12:20:00Z',
    processBy: 'user-001',
    processComment: '调用频次过高，可能影响系统性能，建议降低频次后重新申请',
    createTime: '2024-01-20T08:15:00Z',
    updateTime: '2024-01-20T12:20:00Z'
  },
  {
    id: 'app-012',
    userId: 'user-004',
    interfaceIds: ['if-007'],
    reason: '运维报告需要能源消费统计数据',
    businessScenario: '运维报告',
    estimatedCalls: 2000,
    status: 'pending',
    submitTime: '2024-01-20T17:00:00Z',
    createTime: '2024-01-20T17:00:00Z',
    updateTime: '2024-01-20T17:00:00Z'
  },
  {
    id: 'app-013',
    userId: 'user-005',
    interfaceIds: ['if-009'],
    reason: '系统集成需要用户权限验证接口',
    businessScenario: '系统集成',
    estimatedCalls: 5000,
    status: 'pending',
    submitTime: '2024-01-20T18:15:00Z',
    createTime: '2024-01-20T18:15:00Z',
    updateTime: '2024-01-20T18:15:00Z'
  },
  {
    id: 'app-014',
    userId: 'user-006',
    interfaceIds: ['if-008'],
    reason: '可再生能源项目评估需要发电数据',
    businessScenario: '项目评估',
    estimatedCalls: 3500,
    status: 'pending',
    submitTime: '2024-01-20T19:30:00Z',
    createTime: '2024-01-20T19:30:00Z',
    updateTime: '2024-01-20T19:30:00Z'
  },
  {
    id: 'app-015',
    userId: 'user-007',
    interfaceIds: ['if-002', 'if-003'],
    reason: '市场策略制定需要历史交易和预测数据',
    businessScenario: '策略制定',
    estimatedCalls: 7000,
    status: 'pending',
    submitTime: '2024-01-20T20:45:00Z',
    createTime: '2024-01-20T20:45:00Z',
    updateTime: '2024-01-20T20:45:00Z'
  }
]

// 用户订阅mock数据
export const mockUserSubscriptions: UserInterfaceSubscription[] = [
  {
    id: 'sub-001',
    userId: 'user-002',
    interfaceId: 'if-001',
    applicationId: 'app-001',
    status: 'active',
    subscribeTime: '2024-01-18T14:30:00Z',
    callCount: 2580,
    lastCallTime: '2024-01-20T16:45:00Z',
    createTime: '2024-01-18T14:30:00Z',
    updateTime: '2024-01-20T16:45:00Z'
  },
  {
    id: 'sub-002',
    userId: 'user-002',
    interfaceId: 'if-002',
    applicationId: 'app-001',
    status: 'active',
    subscribeTime: '2024-01-18T14:30:00Z',
    callCount: 1240,
    lastCallTime: '2024-01-20T15:20:00Z',
    createTime: '2024-01-18T14:30:00Z',
    updateTime: '2024-01-20T15:20:00Z'
  },
  {
    id: 'sub-003',
    userId: 'user-003',
    interfaceId: 'if-003',
    applicationId: 'app-002',
    status: 'active',
    subscribeTime: '2024-01-19T16:20:00Z',
    callCount: 890,
    lastCallTime: '2024-01-20T14:10:00Z',
    createTime: '2024-01-19T16:20:00Z',
    updateTime: '2024-01-20T14:10:00Z'
  },
  {
    id: 'sub-004',
    userId: 'user-003',
    interfaceId: 'if-004',
    applicationId: 'app-002',
    status: 'active',
    subscribeTime: '2024-01-19T16:20:00Z',
    callCount: 650,
    lastCallTime: '2024-01-20T13:30:00Z',
    createTime: '2024-01-19T16:20:00Z',
    updateTime: '2024-01-20T13:30:00Z'
  },
  {
    id: 'sub-005',
    userId: 'user-004',
    interfaceId: 'if-005',
    applicationId: 'app-003',
    status: 'active',
    subscribeTime: '2024-01-20T09:15:00Z',
    callCount: 1850,
    lastCallTime: '2024-01-20T18:00:00Z',
    createTime: '2024-01-20T09:15:00Z',
    updateTime: '2024-01-20T18:00:00Z'
  },
  {
    id: 'sub-006',
    userId: 'user-009',
    interfaceId: 'if-002',
    applicationId: 'app-008',
    status: 'active',
    subscribeTime: '2024-01-18T17:45:00Z',
    callCount: 3200,
    lastCallTime: '2024-01-20T17:30:00Z',
    createTime: '2024-01-18T17:45:00Z',
    updateTime: '2024-01-20T17:30:00Z'
  },
  {
    id: 'sub-007',
    userId: 'user-009',
    interfaceId: 'if-004',
    applicationId: 'app-008',
    status: 'active',
    subscribeTime: '2024-01-18T17:45:00Z',
    callCount: 1980,
    lastCallTime: '2024-01-20T16:15:00Z',
    createTime: '2024-01-18T17:45:00Z',
    updateTime: '2024-01-20T16:15:00Z'
  },
  {
    id: 'sub-008',
    userId: 'user-009',
    interfaceId: 'if-008',
    applicationId: 'app-008',
    status: 'active',
    subscribeTime: '2024-01-18T17:45:00Z',
    callCount: 1450,
    lastCallTime: '2024-01-20T15:45:00Z',
    createTime: '2024-01-18T17:45:00Z',
    updateTime: '2024-01-20T15:45:00Z'
  },
  {
    id: 'sub-009',
    userId: 'user-010',
    interfaceId: 'if-003',
    applicationId: 'app-009',
    status: 'active',
    subscribeTime: '2024-01-19T15:10:00Z',
    callCount: 720,
    lastCallTime: '2024-01-20T12:20:00Z',
    createTime: '2024-01-19T15:10:00Z',
    updateTime: '2024-01-20T12:20:00Z'
  },
  {
    id: 'sub-010',
    userId: 'user-010',
    interfaceId: 'if-007',
    applicationId: 'app-009',
    status: 'active',
    subscribeTime: '2024-01-19T15:10:00Z',
    callCount: 580,
    lastCallTime: '2024-01-20T11:40:00Z',
    createTime: '2024-01-19T15:10:00Z',
    updateTime: '2024-01-20T11:40:00Z'
  },
  {
    id: 'sub-011',
    userId: 'user-010',
    interfaceId: 'if-008',
    applicationId: 'app-009',
    status: 'active',
    subscribeTime: '2024-01-19T15:10:00Z',
    callCount: 420,
    lastCallTime: '2024-01-20T10:30:00Z',
    createTime: '2024-01-19T15:10:00Z',
    updateTime: '2024-01-20T10:30:00Z'
  }
]

// 获取申请统计信息
export const getApprovalStats = (): ApprovalStats => {
  const today = new Date().toISOString().split('T')[0]
  const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

  return {
    totalApplications: mockApplications.length,
    pendingApplications: mockApplications.filter(app => app.status === 'pending').length,
    approvedApplications: mockApplications.filter(app => app.status === 'approved').length,
    rejectedApplications: mockApplications.filter(app => app.status === 'rejected').length,
    todayApplications: mockApplications.filter(app => app.submitTime.startsWith(today)).length,
    weekApplications: mockApplications.filter(app => app.submitTime >= weekAgo).length,
    monthApplications: mockApplications.filter(app => app.submitTime >= monthAgo).length
  }
}

// 根据用户ID获取申请列表
export const getApplicationsByUser = (userId: string): SubscriptionApplication[] => {
  return mockApplications.filter(app => app.userId === userId)
}

// 根据状态获取申请列表
export const getApplicationsByStatus = (status: string): SubscriptionApplication[] => {
  return mockApplications.filter(app => app.status === status)
}

// 根据用户ID获取订阅列表
export const getSubscriptionsByUser = (userId: string): UserInterfaceSubscription[] => {
  return mockUserSubscriptions.filter(sub => sub.userId === userId)
}

// 获取申请详情（包含用户和接口信息）
export const getApplicationDetailWithInfo = (applicationId: string) => {
  const application = mockApplications.find(app => app.id === applicationId)
  if (!application) return undefined

  const user = mockUsers.find(u => u.userId === application.userId)
  const interfaces = mockInterfaces.filter(iface => application.interfaceIds.includes(iface.id))
  const processUser = application.processBy ? mockUsers.find(u => u.userId === application.processBy) : undefined

  return {
    ...application,
    userInfo: user,
    interfaceList: interfaces,
    processUserInfo: processUser
  }
}