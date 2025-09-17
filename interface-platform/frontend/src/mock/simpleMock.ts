// 简单的Mock API实现
import { mockCategories } from './categories'
import { mockInterfaces } from './interfaces'
import { mockUsers } from './users'
import { mockApplications } from './applications'

// 模拟延迟
const delay = (ms: number = 300) => new Promise(resolve => setTimeout(resolve, ms))

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

// Mock API 函数
export const mockApi = {
  // 获取接口分类列表
  async getInterfaceCategories() {
    console.log('🔄 Mock API: 获取接口分类列表')
    await delay(200)
    const response = {
      code: 200,
      data: mockCategories,
      message: '获取分类列表成功'
    }
    console.log('✅ Mock API 响应:', response)
    return response.data
  },

  // 获取接口列表
  async getInterfaceList(params: any) {
    console.log('🔄 Mock API: 获取接口列表', params)
    await delay(300)
    
    let filteredInterfaces = [...mockInterfaces]
    console.log('📝 原始接口数据量:', filteredInterfaces.length)

    // 按分类筛选
    if (params.categoryId && params.categoryId !== 'all') {
      filteredInterfaces = filteredInterfaces.filter(iface => iface.categoryId === params.categoryId)
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

    const result = paginate(filteredInterfaces, params.page || 1, params.size || 20)
    console.log('📝 分页结果:', result)
    
    return result
  },

  // 获取接口详情
  async getInterfaceDetail(interfaceId: string) {
    console.log('🔄 Mock API: 获取接口详情', interfaceId)
    await delay(250)
    
    const interfaceInfo = mockInterfaces.find(iface => iface.id === interfaceId)
    if (!interfaceInfo) {
      throw new Error('接口不存在')
    }

    const category = mockCategories.find(cat => cat.id === interfaceInfo.categoryId)
    
    const detail = {
      interface: interfaceInfo,
      parameters: [], // 可以根据需要添加参数数据
      category: category
    }
    
    console.log('✅ Mock API 接口详情响应:', detail)
    return detail
  },

  // 用户登录
  async userLogin(loginData: any) {
    console.log('🔄 Mock API: 用户登录', loginData)
    await delay(500)
    
    if (loginData.account === 'admin' && loginData.password === '123456') {
      const response = {
        userId: 'user-001',
        username: 'admin',
        companyName: '电力交易中心',
        role: 'admin',
        token: 'mock-token-' + Date.now(),
        permissions: ['read', 'write', 'admin']
      }
      console.log('✅ Mock API 登录成功:', response)
      return response
    }
    
    throw new Error('用户名或密码错误')
  },

  // 获取用户信息
  async getUserProfile() {
    console.log('🔄 Mock API: 获取用户信息')
    await delay(200)
    
    const userInfo = mockUsers[0]
    console.log('✅ Mock API 用户信息:', userInfo)
    return userInfo
  },

  // 获取接口管理列表
  async getInterfaceManagementList(params: any) {
    console.log('🔄 Mock API: 获取接口管理列表', params)
    await delay(300)
    
    let filteredInterfaces = [...mockInterfaces]
    console.log('📝 原始接口数据量:', filteredInterfaces.length)

    // 按状态筛选
    if (params.status) {
      filteredInterfaces = filteredInterfaces.filter(iface => {
        // 将接口状态映射到管理页面的状态
        const managementStatus = iface.status === 'published' ? 'active' : 'inactive'
        return managementStatus === params.status
      })
      console.log('📝 按状态筛选后接口数量:', filteredInterfaces.length)
    }

    // 按分类筛选
    if (params.category) {
      filteredInterfaces = filteredInterfaces.filter(iface => iface.categoryId === params.category)
      console.log('📝 按分类筛选后接口数量:', filteredInterfaces.length)
    }

    // 按创建者筛选
    if (params.creator) {
      filteredInterfaces = filteredInterfaces.filter(iface => iface.createBy === params.creator)
      console.log('📝 按创建者筛选后接口数量:', filteredInterfaces.length)
    }

    const result = paginate(filteredInterfaces, params.page || 1, params.size || 20)
    console.log('📝 接口管理列表分页结果:', result)
    
    return result
  },

  // ========== 接口生成相关API ==========
  
  // 获取MySQL表列表
  async getMysqlTables() {
    console.log('🔄 Mock API: 获取MySQL表列表')
    await delay(400)
    
    const tables = [
      { tableName: 'px_market_member', tableComment: '市场成员表', tableType: '统计表', recordCount: 1250 },
      { tableName: 'px_trading_data_24h', tableComment: '24小时交易数据表', tableType: '24小时表', recordCount: 8760 },
      { tableName: 'px_forecast_data_288', tableComment: '288点预测数据表', tableType: '288点表', recordCount: 105120 },
      { tableName: 'px_device_info', tableComment: '设备信息表', tableType: '设备表', recordCount: 500 },
      { tableName: 'px_power_generation', tableComment: '发电数据表', tableType: '24小时表', recordCount: 2400 },
      { tableName: 'px_load_forecast', tableComment: '负荷预测表', tableType: '288点表', recordCount: 52560 }
    ]
    
    console.log('✅ Mock API MySQL表列表:', tables)
    return tables
  },

  // 获取表结构
  async getTableStructure(tableName: string) {
    console.log('🔄 Mock API: 获取表结构', tableName)
    await delay(300)
    
    // 根据表名返回不同的表结构
    const tableStructures: Record<string, any[]> = {
      'px_market_member': [
        { columnName: 'id', dataType: 'varchar(32)', columnComment: '成员ID', isPrimaryKey: true, isNullable: false },
        { columnName: 'member_name', dataType: 'varchar(100)', columnComment: '成员名称', isPrimaryKey: false, isNullable: false },
        { columnName: 'member_type', dataType: 'varchar(20)', columnComment: '成员类型', isPrimaryKey: false, isNullable: false },
        { columnName: 'data_time', dataType: 'datetime', columnComment: '数据时间', isPrimaryKey: false, isNullable: false },
        { columnName: 'create_time', dataType: 'datetime', columnComment: '创建时间', isPrimaryKey: false, isNullable: false }
      ],
      'px_trading_data_24h': [
        { columnName: 'id', dataType: 'varchar(32)', columnComment: 'ID', isPrimaryKey: true, isNullable: false },
        { columnName: 'data_time', dataType: 'datetime', columnComment: '数据时间', isPrimaryKey: false, isNullable: false },
        { columnName: 'trading_volume', dataType: 'decimal(10,2)', columnComment: '交易电量', isPrimaryKey: false, isNullable: false },
        { columnName: 'trading_price', dataType: 'decimal(8,4)', columnComment: '交易价格', isPrimaryKey: false, isNullable: false },
        { columnName: 'market_type', dataType: 'varchar(20)', columnComment: '市场类型', isPrimaryKey: false, isNullable: false }
      ],
      'px_forecast_data_288': [
        { columnName: 'id', dataType: 'varchar(32)', columnComment: 'ID', isPrimaryKey: true, isNullable: false },
        { columnName: 'data_time', dataType: 'datetime', columnComment: '数据时间', isPrimaryKey: false, isNullable: false },
        { columnName: 'point_index', dataType: 'int', columnComment: '时间点索引', isPrimaryKey: false, isNullable: false },
        { columnName: 'forecast_value', dataType: 'decimal(10,2)', columnComment: '预测值', isPrimaryKey: false, isNullable: false },
        { columnName: 'forecast_type', dataType: 'varchar(20)', columnComment: '预测类型', isPrimaryKey: false, isNullable: false }
      ]
    }
    
    const structure = tableStructures[tableName] || [
      { columnName: 'id', dataType: 'varchar(32)', columnComment: 'ID', isPrimaryKey: true, isNullable: false },
      { columnName: 'data_time', dataType: 'datetime', columnComment: '数据时间', isPrimaryKey: false, isNullable: false },
      { columnName: 'value', dataType: 'decimal(10,2)', columnComment: '数值', isPrimaryKey: false, isNullable: false }
    ]
    
    console.log('✅ Mock API 表结构:', structure)
    return structure
  },

  // 验证接口配置
  async validateInterfaceConfig(config: any) {
    console.log('🔄 Mock API: 验证接口配置', config)
    await delay(500)
    
    // 模拟配置验证逻辑
    const errors = []
    
    if (!config.interfaceName) {
      errors.push('接口名称不能为空')
    }
    
    if (!config.interfacePath) {
      errors.push('接口路径不能为空')
    }
    
    if (!config.categoryId) {
      errors.push('业务分类不能为空')
    }
    
    if (!config.tableName) {
      errors.push('数据表不能为空')
    }
    
    if (!config.parameters || config.parameters.length === 0) {
      errors.push('接口参数不能为空')
    }
    
    // 检查接口名称是否已存在
    const existingInterface = mockInterfaces.find(iface => 
      iface.interfaceName === config.interfaceName
    )
    if (existingInterface) {
      errors.push('接口名称已存在')
    }
    
    // 检查接口路径是否已存在
    const existingPath = mockInterfaces.find(iface => 
      iface.interfacePath === config.interfacePath
    )
    if (existingPath) {
      errors.push('接口路径已存在')
    }
    
    const result = {
      valid: errors.length === 0,
      errors: errors
    }
    
    console.log('✅ Mock API 配置验证结果:', result)
    
    if (!result.valid) {
      throw new Error(`接口配置验证失败: ${errors.join(', ')}`)
    }
    
    return result
  },

  // 生成接口
  async generateInterface(request: any) {
    console.log('🔄 Mock API: 生成接口', request)
    await delay(800)
    
    const config = request.config
    
    // 先验证配置
    await this.validateInterfaceConfig(config)
    
    // 生成新的接口ID
    const newInterfaceId = `interface-${Date.now()}`
    
    // 创建新接口对象
    const newInterface = {
      id: newInterfaceId,
      interfaceName: config.interfaceName,
      interfacePath: config.interfacePath,
      description: config.description || '',
      categoryId: config.categoryId,
      requestMethod: config.requestMethod,
      status: 'unpublished' as const, // 新生成的接口状态为未发布
      version: '1.0.0', // 添加版本字段
      rateLimit: config.rateLimit || 1000,
      timeout: config.timeout || 30,
      dataSourceId: config.dataSourceId,
      tableName: config.tableName,
      parameters: config.parameters,
      createBy: 'admin',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    
    // 添加到Mock数据中
    mockInterfaces.push(newInterface)
    
    console.log('✅ Mock API 接口生成成功:', newInterfaceId)
    return newInterfaceId
  }
}

// 初始化简单Mock服务
export function initSimpleMock() {
  console.log('🚀 初始化简单Mock API服务...')
  console.log('📊 Mock数据统计:')
  console.log('  - 分类数量:', mockCategories.length)
  console.log('  - 接口数量:', mockInterfaces.length)
  console.log('  - 用户数量:', mockUsers.length)
  console.log('  - 申请数量:', mockApplications.length)
  console.log('✅ 简单Mock API服务初始化完成')
}