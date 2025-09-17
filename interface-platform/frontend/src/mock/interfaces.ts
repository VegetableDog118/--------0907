import type { InterfaceInfo, InterfaceParameter, InterfaceDetailResponse } from '@/api/interface'
import { mockCategories } from './categories'

// 接口mock数据
export const mockInterfaces: InterfaceInfo[] = [
  {
    id: 'if-001',
    interfaceName: '实时电价查询',
    interfacePath: '/api/v1/power/realtime-price',
    description: '获取当前实时电价信息，支持按地区和时间段查询',
    categoryId: 'cat-001',
    categoryName: '电力交易',
    dataSourceId: 'ds-001',
    tableName: 'power_price_realtime',
    requestMethod: 'GET',
    status: 'published',
    version: '1.0.0',
    sqlTemplate: 'SELECT region, price, timestamp FROM power_price_realtime WHERE region = ? AND timestamp >= ?',
    responseFormat: { region: 'string', price: 'number', timestamp: 'datetime' },
    rateLimit: 1000,
    timeout: 5000,
    createTime: '2024-01-15T08:00:00Z',
    updateTime: '2024-01-20T10:30:00Z',
    publishTime: '2024-01-16T09:00:00Z',
    createBy: 'user-001',
    updateBy: 'user-001',
    publishBy: 'user-001'
  },
  {
    id: 'if-002',
    interfaceName: '历史交易数据',
    interfacePath: '/api/v1/power/historical-trades',
    description: '查询历史电力交易记录，支持多维度筛选',
    categoryId: 'cat-001',
    categoryName: '电力交易',
    dataSourceId: 'ds-001',
    tableName: 'power_trades_history',
    requestMethod: 'POST',
    status: 'published',
    version: '1.2.0',
    sqlTemplate: 'SELECT * FROM power_trades_history WHERE trade_date BETWEEN ? AND ? AND region = ?',
    responseFormat: { tradeId: 'string', volume: 'number', price: 'number', tradeDate: 'date' },
    rateLimit: 500,
    timeout: 10000,
    createTime: '2024-01-15T08:30:00Z',
    updateTime: '2024-01-19T14:20:00Z',
    publishTime: '2024-01-16T10:00:00Z',
    createBy: 'user-002',
    updateBy: 'user-002',
    publishBy: 'user-001'
  },
  {
    id: 'if-003',
    interfaceName: '负荷预测数据',
    interfacePath: '/api/v1/market/load-forecast',
    description: '获取电力负荷预测数据，包括短期和中长期预测',
    categoryId: 'cat-002',
    categoryName: '市场数据',
    dataSourceId: 'ds-002',
    tableName: 'load_forecast',
    requestMethod: 'GET',
    status: 'published',
    version: '2.0.0',
    sqlTemplate: 'SELECT forecast_date, predicted_load, confidence_level FROM load_forecast WHERE region = ? AND forecast_type = ?',
    responseFormat: { forecastDate: 'datetime', predictedLoad: 'number', confidenceLevel: 'number' },
    rateLimit: 200,
    timeout: 8000,
    createTime: '2024-01-15T09:00:00Z',
    updateTime: '2024-01-20T11:15:00Z',
    publishTime: '2024-01-17T08:30:00Z',
    createBy: 'user-003',
    updateBy: 'user-003',
    publishBy: 'user-001'
  },
  {
    id: 'if-004',
    interfaceName: '市场价格分析',
    interfacePath: '/api/v1/market/price-analysis',
    description: '电力市场价格趋势分析和统计数据',
    categoryId: 'cat-002',
    categoryName: '市场数据',
    dataSourceId: 'ds-002',
    tableName: 'market_price_analysis',
    requestMethod: 'GET',
    status: 'published',
    version: '1.1.0',
    sqlTemplate: 'SELECT analysis_date, avg_price, max_price, min_price, volatility FROM market_price_analysis WHERE period = ?',
    responseFormat: { analysisDate: 'date', avgPrice: 'number', maxPrice: 'number', minPrice: 'number', volatility: 'number' },
    rateLimit: 300,
    timeout: 6000,
    createTime: '2024-01-15T09:30:00Z',
    updateTime: '2024-01-18T16:45:00Z',
    publishTime: '2024-01-17T10:00:00Z',
    createBy: 'user-003',
    updateBy: 'user-003',
    publishBy: 'user-001'
  },
  {
    id: 'if-005',
    interfaceName: '设备运行状态',
    interfacePath: '/api/v1/grid/equipment-status',
    description: '查询电网设备实时运行状态和参数',
    categoryId: 'cat-003',
    categoryName: '电网运行',
    dataSourceId: 'ds-003',
    tableName: 'equipment_status',
    requestMethod: 'GET',
    status: 'published',
    version: '1.0.0',
    sqlTemplate: 'SELECT equipment_id, status, temperature, voltage, current FROM equipment_status WHERE station_id = ?',
    responseFormat: { equipmentId: 'string', status: 'string', temperature: 'number', voltage: 'number', current: 'number' },
    rateLimit: 800,
    timeout: 3000,
    createTime: '2024-01-15T10:00:00Z',
    updateTime: '2024-01-20T09:20:00Z',
    publishTime: '2024-01-17T11:00:00Z',
    createBy: 'user-004',
    updateBy: 'user-004',
    publishBy: 'user-001'
  },
  {
    id: 'if-006',
    interfaceName: '电网拓扑信息',
    interfacePath: '/api/v1/grid/topology',
    description: '获取电网拓扑结构和连接关系',
    categoryId: 'cat-003',
    categoryName: '电网运行',
    dataSourceId: 'ds-003',
    tableName: 'grid_topology',
    requestMethod: 'GET',
    status: 'unpublished',
    version: '0.9.0',
    sqlTemplate: 'SELECT node_id, node_type, connections, coordinates FROM grid_topology WHERE region = ?',
    responseFormat: { nodeId: 'string', nodeType: 'string', connections: 'array', coordinates: 'object' },
    rateLimit: 100,
    timeout: 15000,
    createTime: '2024-01-15T10:30:00Z',
    updateTime: '2024-01-20T15:30:00Z',
    createBy: 'user-004',
    updateBy: 'user-004'
  },
  {
    id: 'if-007',
    interfaceName: '能源消费统计',
    interfacePath: '/api/v1/energy/consumption-stats',
    description: '各地区能源消费统计数据，支持多时间维度查询',
    categoryId: 'cat-004',
    categoryName: '能源统计',
    dataSourceId: 'ds-004',
    tableName: 'energy_consumption',
    requestMethod: 'POST',
    status: 'published',
    version: '1.3.0',
    sqlTemplate: 'SELECT region, period, consumption, energy_type FROM energy_consumption WHERE period BETWEEN ? AND ?',
    responseFormat: { region: 'string', period: 'string', consumption: 'number', energyType: 'string' },
    rateLimit: 400,
    timeout: 7000,
    createTime: '2024-01-15T11:00:00Z',
    updateTime: '2024-01-19T13:10:00Z',
    publishTime: '2024-01-18T09:00:00Z',
    createBy: 'user-005',
    updateBy: 'user-005',
    publishBy: 'user-001'
  },
  {
    id: 'if-008',
    interfaceName: '可再生能源发电',
    interfacePath: '/api/v1/energy/renewable-generation',
    description: '可再生能源发电量统计和预测数据',
    categoryId: 'cat-004',
    categoryName: '能源统计',
    dataSourceId: 'ds-004',
    tableName: 'renewable_generation',
    requestMethod: 'GET',
    status: 'published',
    version: '2.1.0',
    sqlTemplate: 'SELECT generation_date, solar_power, wind_power, hydro_power FROM renewable_generation WHERE region = ?',
    responseFormat: { generationDate: 'date', solarPower: 'number', windPower: 'number', hydroPower: 'number' },
    rateLimit: 600,
    timeout: 5000,
    createTime: '2024-01-15T11:30:00Z',
    updateTime: '2024-01-20T14:25:00Z',
    publishTime: '2024-01-18T10:30:00Z',
    createBy: 'user-005',
    updateBy: 'user-005',
    publishBy: 'user-001'
  },
  {
    id: 'if-009',
    interfaceName: '用户权限查询',
    interfacePath: '/api/v1/user/permissions',
    description: '查询用户权限和角色信息',
    categoryId: 'cat-005',
    categoryName: '用户管理',
    dataSourceId: 'ds-005',
    tableName: 'user_permissions',
    requestMethod: 'GET',
    status: 'published',
    version: '1.0.0',
    sqlTemplate: 'SELECT user_id, role, permissions FROM user_permissions WHERE user_id = ?',
    responseFormat: { userId: 'string', role: 'string', permissions: 'array' },
    rateLimit: 1000,
    timeout: 2000,
    createTime: '2024-01-15T12:00:00Z',
    updateTime: '2024-01-20T08:15:00Z',
    publishTime: '2024-01-16T14:00:00Z',
    createBy: 'user-001',
    updateBy: 'user-001',
    publishBy: 'user-001'
  },
  {
    id: 'if-010',
    interfaceName: '系统日志查询',
    interfacePath: '/api/v1/system/logs',
    description: '系统操作日志查询接口',
    categoryId: 'cat-005',
    categoryName: '用户管理',
    dataSourceId: 'ds-005',
    tableName: 'system_logs',
    requestMethod: 'POST',
    status: 'offline',
    version: '1.0.0',
    sqlTemplate: 'SELECT log_time, user_id, action, details FROM system_logs WHERE log_time BETWEEN ? AND ?',
    responseFormat: { logTime: 'datetime', userId: 'string', action: 'string', details: 'string' },
    rateLimit: 200,
    timeout: 10000,
    createTime: '2024-01-15T12:30:00Z',
    updateTime: '2024-01-20T16:00:00Z',
    publishTime: '2024-01-17T15:00:00Z',
    offlineTime: '2024-01-20T16:00:00Z',
    createBy: 'user-001',
    updateBy: 'user-001',
    publishBy: 'user-001',
    offlineBy: 'user-001'
  }
]

// 接口参数mock数据
export const mockInterfaceParameters: Record<string, InterfaceParameter[]> = {
  'if-001': [
    {
      id: 'param-001-001',
      interfaceId: 'if-001',
      paramName: 'region',
      paramType: 'string',
      paramLocation: 'query',
      description: '地区代码，如：BJ（北京）、SH（上海）',
      required: true,
      defaultValue: 'BJ',
      validationRule: '^[A-Z]{2}$',
      example: 'BJ',
      sortOrder: 1,
      createTime: '2024-01-15T08:00:00Z',
      updateTime: '2024-01-15T08:00:00Z'
    },
    {
      id: 'param-001-002',
      interfaceId: 'if-001',
      paramName: 'startTime',
      paramType: 'datetime',
      paramLocation: 'query',
      description: '查询开始时间',
      required: false,
      example: '2024-01-20T00:00:00Z',
      sortOrder: 2,
      createTime: '2024-01-15T08:00:00Z',
      updateTime: '2024-01-15T08:00:00Z'
    }
  ],
  'if-002': [
    {
      id: 'param-002-001',
      interfaceId: 'if-002',
      paramName: 'startDate',
      paramType: 'date',
      paramLocation: 'body',
      description: '查询开始日期',
      required: true,
      example: '2024-01-01',
      sortOrder: 1,
      createTime: '2024-01-15T08:30:00Z',
      updateTime: '2024-01-15T08:30:00Z'
    },
    {
      id: 'param-002-002',
      interfaceId: 'if-002',
      paramName: 'endDate',
      paramType: 'date',
      paramLocation: 'body',
      description: '查询结束日期',
      required: true,
      example: '2024-01-31',
      sortOrder: 2,
      createTime: '2024-01-15T08:30:00Z',
      updateTime: '2024-01-15T08:30:00Z'
    },
    {
      id: 'param-002-003',
      interfaceId: 'if-002',
      paramName: 'region',
      paramType: 'string',
      paramLocation: 'body',
      description: '地区代码',
      required: true,
      example: 'BJ',
      sortOrder: 3,
      createTime: '2024-01-15T08:30:00Z',
      updateTime: '2024-01-15T08:30:00Z'
    }
  ]
}

// 根据分类ID获取接口列表
export const getInterfacesByCategory = (categoryId: string): InterfaceInfo[] => {
  return mockInterfaces.filter(iface => iface.categoryId === categoryId)
}

// 根据状态获取接口列表
export const getInterfacesByStatus = (status: string): InterfaceInfo[] => {
  return mockInterfaces.filter(iface => iface.status === status)
}

// 搜索接口
export const searchInterfaces = (keyword: string): InterfaceInfo[] => {
  const lowerKeyword = keyword.toLowerCase()
  return mockInterfaces.filter(iface => 
    iface.interfaceName.toLowerCase().includes(lowerKeyword) ||
    iface.description?.toLowerCase().includes(lowerKeyword) ||
    iface.interfacePath.toLowerCase().includes(lowerKeyword)
  )
}

// 获取接口统计信息
export const getInterfaceStats = () => {
  const statusStats = mockInterfaces.reduce((stats, iface) => {
    stats[iface.status] = (stats[iface.status] || 0) + 1
    return stats
  }, {} as Record<string, number>)

  return {
    totalInterfaces: mockInterfaces.length,
    publishedInterfaces: statusStats.published || 0,
    unpublishedInterfaces: statusStats.unpublished || 0,
    offlineInterfaces: statusStats.offline || 0,
    statusDistribution: statusStats
  }
}

// 获取接口详情
export const getInterfaceDetail = (interfaceId: string): InterfaceDetailResponse | undefined => {
  const interfaceInfo = mockInterfaces.find(iface => iface.id === interfaceId)
  if (!interfaceInfo) return undefined

  const category = mockCategories.find(cat => cat.id === interfaceInfo.categoryId)
  const parameters = mockInterfaceParameters[interfaceId] || []

  return {
    interface: interfaceInfo,
    parameters,
    category: category!
  }
}