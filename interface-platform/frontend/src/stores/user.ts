import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  userId: string
  username: string
  realName: string
  companyName: string
  phone: string
  email: string
  department: string
  position: string
  role: 'admin' | 'settlement' | 'tech' | 'consumer'
  status: 'active' | 'inactive' | 'pending'
  createTime: string
  lastLoginTime?: string
}

export interface LoginRequest {
  account: string
  password: string
  captcha?: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
  permissions: string[]
  expireTime: number
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])
  const isLoggedIn = ref<boolean>(false)

  // 计算属性
  const hasPermission = computed(() => {
    return (permission: string) => {
      return permissions.value.includes(permission)
    }
  })

  const hasRole = computed(() => {
    return (role: string) => {
      if (!userInfo.value) return false
      
      const roleHierarchy: Record<string, string[]> = {
        'admin': ['admin', 'settlement', 'tech', 'consumer'],
        'settlement': ['settlement', 'tech', 'consumer'],
        'tech': ['tech', 'consumer'],
        'consumer': ['consumer']
      }
      
      return roleHierarchy[userInfo.value.role]?.includes(role) || false
    }
  })

  const userDisplayName = computed(() => {
    return userInfo.value?.realName || userInfo.value?.username || '未知用户'
  })

  const roleDisplayName = computed(() => {
    if (!userInfo.value) return ''
    
    const roleNames: Record<string, string> = {
      admin: '系统管理员',
      settlement: '结算部',
      tech: '技术部',
      consumer: '数据消费者'
    }
    
    return roleNames[userInfo.value.role] || userInfo.value.role
  })

  // 方法
  const login = async (loginData: LoginRequest): Promise<any> => {
    try {
      // 调用真正的登录API
      const { loginUser } = await import('@/api/user')
      const responseData = await loginUser({
        account: loginData.account,
        password: loginData.password,
        captcha: loginData.captcha
      })
      
      // 处理API响应数据结构
      if (responseData && responseData.token) {
        setLoginState({
          token: responseData.token,
          userInfo: responseData.userInfo || {
            userId: responseData.userId,
            username: responseData.username,
            realName: responseData.username,
            companyName: responseData.companyName,
            phone: '',
            email: '',
            department: '',
            position: '',
            role: responseData.role,
            status: 'active',
            createTime: new Date().toISOString()
          },
          permissions: responseData.permissions || []
        })
        return { success: true }
      } else {
        throw new Error('登录响应数据无效')
      }
    } catch (error: any) {
      console.error('Login failed:', error)
      return { 
        success: false, 
        message: error.message || '登录失败' 
      }
    }
  }

  const logout = () => {
    // 清除登录状态
    token.value = ''
    userInfo.value = null
    permissions.value = []
    isLoggedIn.value = false
    
    // 清除本地存储
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('permissions')
  }

  const setLoginState = (loginResponse: any) => {
    token.value = loginResponse.token
    userInfo.value = loginResponse.userInfo
    permissions.value = loginResponse.permissions || []
    isLoggedIn.value = true
    
    // 保存到本地存储
    localStorage.setItem('token', loginResponse.token)
    localStorage.setItem('userInfo', JSON.stringify(loginResponse.userInfo))
    localStorage.setItem('permissions', JSON.stringify(loginResponse.permissions || []))
  }

  // 单独设置用户信息
  const setUserInfo = (user: UserInfo) => {
    userInfo.value = user
    isLoggedIn.value = true
  }

  // 单独设置token
  const setToken = (newToken: string) => {
    token.value = newToken
  }

  // 单独设置权限
  const setPermissions = (newPermissions: string[]) => {
    permissions.value = newPermissions
  }

  const initFromStorage = () => {
    // 从本地存储恢复登录状态
    const storedToken = localStorage.getItem('token')
    const storedUserInfo = localStorage.getItem('userInfo')
    const storedPermissions = localStorage.getItem('permissions')
    
    if (storedToken && storedUserInfo) {
      try {
        const parsedUserInfo = JSON.parse(storedUserInfo)
        const parsedPermissions = storedPermissions ? JSON.parse(storedPermissions) : []
        
        // 验证数据完整性
        if (parsedUserInfo && parsedUserInfo.userId && parsedUserInfo.username) {
          token.value = storedToken
          userInfo.value = parsedUserInfo
          permissions.value = parsedPermissions
          isLoggedIn.value = true
          console.log('✅ 用户登录状态已恢复:', parsedUserInfo.username)
        } else {
          console.warn('⚠️ 存储的用户信息不完整，清除登录状态')
          logout()
        }
      } catch (error) {
        console.error('❌ 恢复登录状态失败:', error)
        logout()
      }
    } else {
      console.log('ℹ️ 未找到有效的登录状态')
      isLoggedIn.value = false
    }
  }

  const updateUserInfo = async (updateData: Partial<UserInfo>) => {
    try {
      // 调用更新用户信息API
      const { updateUserInfo: updateUserInfoApi } = await import('@/api/user')
      await updateUserInfoApi({
        contactName: updateData.realName,
        phone: updateData.phone,
        email: updateData.email,
        department: updateData.department,
        position: updateData.position
      })
      
      // 更新本地用户信息
      if (userInfo.value) {
        Object.assign(userInfo.value, updateData)
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      }
      
      return true
    } catch (error: any) {
      throw new Error(error.message || '更新用户信息失败')
    }
  }

  const refreshToken = async () => {
    try {
      // 调用验证token API
      const { validateToken } = await import('@/api/user')
      const tokenInfo = await validateToken(token.value)
      
      // 如果token有效，更新过期时间
      const newExpireTime = new Date(tokenInfo.expiresAt).getTime()
      if (newExpireTime > Date.now()) {
        return token.value
      } else {
        throw new Error('Token已过期')
      }
    } catch (error) {
      // 刷新失败，清除登录状态
      logout()
      throw new Error('Token刷新失败，请重新登录')
    }
  }

  const checkTokenExpiry = async () => {
    if (!token.value) return false
    
    try {
      const { validateToken } = await import('@/api/user')
      const tokenInfo = await validateToken(token.value)
      const expireTime = new Date(tokenInfo.expiresAt).getTime()
      return expireTime > Date.now()
    } catch (error) {
      return false
    }
  }

  // 获取完整用户信息
  const fetchUserInfo = async () => {
    try {
      const { getCurrentUserInfo } = await import('@/api/user')
      const userInfoResponse = await getCurrentUserInfo()
      
      if (userInfo.value) {
        userInfo.value.realName = userInfoResponse.contactName
        userInfo.value.phone = userInfoResponse.phone
        userInfo.value.email = userInfoResponse.email
        userInfo.value.department = userInfoResponse.department
        userInfo.value.position = userInfoResponse.position
        userInfo.value.status = mapStatus(userInfoResponse.status)
        userInfo.value.createTime = userInfoResponse.createTime
        userInfo.value.lastLoginTime = userInfoResponse.lastLoginTime
        
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      }
    } catch (error) {
      console.warn('获取用户详细信息失败:', error)
    }
  }
  
  // 角色映射
  const mapRole = (backendRole: string): 'admin' | 'settlement' | 'tech' | 'consumer' => {
    const roleMap: Record<string, 'admin' | 'settlement' | 'tech' | 'consumer'> = {
      'ADMIN': 'admin',
      'USER': 'consumer'
    }
    return roleMap[backendRole] || 'consumer'
  }
  
  // 状态映射
  const mapStatus = (backendStatus: string): 'active' | 'inactive' | 'pending' => {
    const statusMap: Record<string, 'active' | 'inactive' | 'pending'> = {
      'ACTIVE': 'active',
      'LOCKED': 'inactive',
      'PENDING': 'pending',
      'REJECTED': 'inactive'
    }
    return statusMap[backendStatus] || 'pending'
  }
  
  // 登出
  const performLogout = async () => {
    try {
      const { logoutUser } = await import('@/api/user')
      await logoutUser()
    } catch {
      console.warn('服务端登出失败')
    } finally {
      logout()
    }
  }

  // 初始化
  initFromStorage()

  return {
    // 状态
    token,
    userInfo,
    permissions,
    isLoggedIn,
    
    // 计算属性
    hasPermission,
    hasRole,
    userDisplayName,
    roleDisplayName,
    
    // 方法
    login,
    logout,
    performLogout,
    setLoginState,
    setUserInfo,
    setToken,
    setPermissions,
    initFromStorage,
    updateUserInfo,
    fetchUserInfo,
    refreshToken,
    checkTokenExpiry
  }
})