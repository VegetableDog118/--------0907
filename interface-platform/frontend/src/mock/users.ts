import type { UserInfoResponse, UserLoginResponse } from '@/api/user'

// 用户mock数据
export const mockUsers: UserInfoResponse[] = [
  {
    userId: 'user-001',
    username: 'admin',
    companyName: '国家电网有限公司',
    creditCode: '91110000100000001X',
    contactName: '张三',
    phone: '13800138001',
    email: 'admin@sgcc.com.cn',
    department: '信息通信部',
    position: '系统管理员',
    role: 'admin',
    status: 'active',
    createTime: '2024-01-01T08:00:00Z',
    lastLoginTime: '2024-01-20T14:30:00Z'
  },
  {
    userId: 'user-002',
    username: 'trader001',
    companyName: '华能集团有限公司',
    creditCode: '91110000200000002Y',
    contactName: '李四',
    phone: '13800138002',
    email: 'trader@huaneng.com',
    department: '电力交易部',
    position: '交易员',
    role: 'trader',
    status: 'active',
    createTime: '2024-01-02T09:00:00Z',
    lastLoginTime: '2024-01-20T10:15:00Z'
  },
  {
    userId: 'user-003',
    username: 'analyst001',
    companyName: '大唐集团有限公司',
    creditCode: '91110000300000003Z',
    contactName: '王五',
    phone: '13800138003',
    email: 'analyst@datang.com',
    department: '市场分析部',
    position: '数据分析师',
    role: 'analyst',
    status: 'active',
    createTime: '2024-01-03T10:00:00Z',
    lastLoginTime: '2024-01-20T16:45:00Z'
  },
  {
    userId: 'user-004',
    username: 'operator001',
    companyName: '华电集团有限公司',
    creditCode: '91110000400000004A',
    contactName: '赵六',
    phone: '13800138004',
    email: 'operator@chd.com.cn',
    department: '运行维护部',
    position: '运维工程师',
    role: 'operator',
    status: 'active',
    createTime: '2024-01-04T11:00:00Z',
    lastLoginTime: '2024-01-20T08:20:00Z'
  },
  {
    userId: 'user-005',
    username: 'developer001',
    companyName: '中国电力科学研究院',
    creditCode: '91110000500000005B',
    contactName: '孙七',
    phone: '13800138005',
    email: 'dev@epri.sgcc.com.cn',
    department: '信息技术部',
    position: '软件开发工程师',
    role: 'developer',
    status: 'active',
    createTime: '2024-01-05T12:00:00Z',
    lastLoginTime: '2024-01-19T17:30:00Z'
  },
  {
    userId: 'user-006',
    username: 'guest001',
    companyName: '北京电力交易中心',
    creditCode: '91110000600000006C',
    contactName: '周八',
    phone: '13800138006',
    email: 'guest@bj-pex.com',
    department: '业务部',
    position: '业务专员',
    role: 'guest',
    status: 'active',
    createTime: '2024-01-06T13:00:00Z',
    lastLoginTime: '2024-01-20T11:00:00Z'
  },
  {
    userId: 'user-007',
    username: 'manager001',
    companyName: '南方电网有限责任公司',
    creditCode: '91110000700000007D',
    contactName: '吴九',
    phone: '13800138007',
    email: 'manager@csg.cn',
    department: '市场部',
    position: '部门经理',
    role: 'manager',
    status: 'active',
    createTime: '2024-01-07T14:00:00Z',
    lastLoginTime: '2024-01-20T13:15:00Z'
  },
  {
    userId: 'user-008',
    username: 'auditor001',
    companyName: '国家能源局',
    creditCode: '91110000800000008E',
    contactName: '郑十',
    phone: '13800138008',
    email: 'auditor@nea.gov.cn',
    department: '监管部',
    position: '监管专员',
    role: 'auditor',
    status: 'active',
    createTime: '2024-01-08T15:00:00Z',
    lastLoginTime: '2024-01-20T09:45:00Z'
  },
  {
    userId: 'user-009',
    username: 'consultant001',
    companyName: '中电联咨询有限公司',
    creditCode: '91110000900000009F',
    contactName: '冯十一',
    phone: '13800138009',
    email: 'consultant@cec.org.cn',
    department: '咨询部',
    position: '高级顾问',
    role: 'consultant',
    status: 'active',
    createTime: '2024-01-09T16:00:00Z',
    lastLoginTime: '2024-01-19T15:20:00Z'
  },
  {
    userId: 'user-010',
    username: 'researcher001',
    companyName: '清华大学电机系',
    creditCode: '91110000A00000010G',
    contactName: '陈十二',
    phone: '13800138010',
    email: 'researcher@tsinghua.edu.cn',
    department: '电力系统研究所',
    position: '研究员',
    role: 'researcher',
    status: 'active',
    createTime: '2024-01-10T17:00:00Z',
    lastLoginTime: '2024-01-20T12:30:00Z'
  }
]

// 模拟登录响应数据
export const mockLoginResponse: UserLoginResponse = {
  token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiYWRtaW4iLCJpYXQiOjE3MDU3MjgwMDAsImV4cCI6MTcwNTgxNDQwMH0.mock-jwt-token',
  tokenType: 'Bearer',
  expiresIn: 86400,
  userId: 'user-001',
  username: 'admin',
  companyName: '国家电网有限公司',
  role: 'admin',
  permissions: [
    'interface:read',
    'interface:write',
    'interface:delete',
    'user:read',
    'user:write',
    'approval:read',
    'approval:write',
    'system:read',
    'system:write'
  ]
}

// 根据用户名获取用户信息
export const getUserByUsername = (username: string): UserInfoResponse | undefined => {
  return mockUsers.find(user => user.username === username)
}

// 根据用户ID获取用户信息
export const getUserById = (userId: string): UserInfoResponse | undefined => {
  return mockUsers.find(user => user.userId === userId)
}

// 获取用户统计信息
export const getUserStats = () => {
  const roleStats = mockUsers.reduce((stats, user) => {
    stats[user.role] = (stats[user.role] || 0) + 1
    return stats
  }, {} as Record<string, number>)

  return {
    totalUsers: mockUsers.length,
    activeUsers: mockUsers.filter(user => user.status === 'active').length,
    roleDistribution: roleStats
  }
}