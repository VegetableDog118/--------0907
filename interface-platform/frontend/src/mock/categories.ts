import type { InterfaceCategory } from '@/api/interface'

// 接口分类mock数据
export const mockCategories: InterfaceCategory[] = [
  {
    id: 'cat-001',
    categoryCode: 'POWER_TRADING',
    categoryName: '电力交易',
    description: '电力交易相关接口，包括交易数据查询、价格信息等',
    color: '#1890ff',
    sortOrder: 1,
    status: 1,
    createTime: '2024-01-15T08:00:00Z',
    updateTime: '2024-01-15T08:00:00Z',
    interfaceCount: 8
  },
  {
    id: 'cat-002',
    categoryCode: 'MARKET_DATA',
    categoryName: '市场数据',
    description: '电力市场数据接口，包括负荷预测、价格分析等',
    color: '#52c41a',
    sortOrder: 2,
    status: 1,
    createTime: '2024-01-15T08:30:00Z',
    updateTime: '2024-01-15T08:30:00Z',
    interfaceCount: 6
  },
  {
    id: 'cat-003',
    categoryCode: 'GRID_OPERATION',
    categoryName: '电网运行',
    description: '电网运行监控相关接口，包括设备状态、运行参数等',
    color: '#faad14',
    sortOrder: 3,
    status: 1,
    createTime: '2024-01-15T09:00:00Z',
    updateTime: '2024-01-15T09:00:00Z',
    interfaceCount: 5
  },
  {
    id: 'cat-004',
    categoryCode: 'ENERGY_STATS',
    categoryName: '能源统计',
    description: '能源消费和生产统计数据接口',
    color: '#f5222d',
    sortOrder: 4,
    status: 1,
    createTime: '2024-01-15T09:30:00Z',
    updateTime: '2024-01-15T09:30:00Z',
    interfaceCount: 4
  },
  {
    id: 'cat-005',
    categoryCode: 'USER_MANAGEMENT',
    categoryName: '用户管理',
    description: '用户账户管理相关接口',
    color: '#722ed1',
    sortOrder: 5,
    status: 1,
    createTime: '2024-01-15T10:00:00Z',
    updateTime: '2024-01-15T10:00:00Z',
    interfaceCount: 3
  }
]

// 获取分类统计信息
export const getCategoryStats = () => {
  return {
    totalCategories: mockCategories.length,
    activeCategories: mockCategories.filter(cat => cat.status === 1).length,
    totalInterfaces: mockCategories.reduce((sum, cat) => sum + (cat.interfaceCount || 0), 0)
  }
}