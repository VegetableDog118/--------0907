import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import HomeView from '../views/HomeView.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import InterfaceCatalog from '../views/InterfaceCatalog.vue'
import TestInterfaceCatalog from '../views/TestInterfaceCatalog.vue'
import InterfaceManagement from '../views/InterfaceManagement.vue'
import ApplicationApproval from '../views/ApplicationApproval.vue'
import UserCenter from '../views/UserCenter.vue'
import SystemManagement from '../views/SystemManagement.vue'
import DataStatistics from '../views/DataStatistics.vue'
import InterfaceDetail from '../views/InterfaceDetail.vue'
import NotFound from '../views/404.vue'
import Forbidden from '../views/403.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/Register.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/interface',
      name: 'interface',
      redirect: '/interface/catalog',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'catalog',
          name: 'interface-catalog',
          component: () => import('../views/InterfaceCatalog.vue'),
          meta: { 
            requiresAuth: true,
            title: '接口目录',
            description: '浏览和订阅已上架的接口'
          }
        },
        {
          path: 'management',
          name: 'interface-management',
          component: () => import('../views/InterfaceManagement.vue'),
          meta: { 
            requiresAuth: true,
            requiresRole: ['tech', 'settlement', 'admin'],
            title: '接口管理',
            description: '管理接口生命周期：生成、编辑、上架、下架'
          }
        },
        {
          path: 'detail/:id',
          name: 'interface-detail',
          component: () => import('../views/InterfaceDetail.vue'),
          meta: { 
            requiresAuth: true,
            title: '接口详情'
          }
        },
        {
          path: 'test-catalog',
          name: 'test-interface-catalog',
          component: () => import('../views/TestInterfaceCatalog.vue'),
          meta: { 
            requiresAuth: false,
            title: '接口目录测试'
          }
        }
      ]
    },
    {
      path: '/application',
      name: 'application',
      redirect: '/application/approval',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'approval',
          name: 'application-approval',
          component: () => import('../views/ApplicationApproval.vue'),
          meta: { 
            requiresAuth: true,
            requiresRole: ['settlement', 'admin'],
            title: '申请审批'
          }
        }
      ]
    },
    {
      path: '/user',
      name: 'user',
      redirect: '/user/center',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'center',
          name: 'user-center',
          component: () => import('../views/UserCenter.vue'),
          meta: { 
            requiresAuth: true,
            title: '用户中心'
          }
        }
      ]
    },
    {
      path: '/system',
      name: 'system',
      redirect: '/system/management',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'management',
          name: 'system-management',
          component: () => import('../views/SystemManagement.vue'),
          meta: { 
            requiresAuth: true,
            requiresRole: ['admin'],
            title: '系统管理'
          }
        }
      ]
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: () => import('../views/DataStatistics.vue'),
      meta: { 
        requiresAuth: true,
        requiresRole: ['admin', 'settlement'],
        title: '数据统计'
      }
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('../views/403.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/404.vue'),
      meta: { requiresAuth: false }
    }
  ],
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 防止重定向循环
  if (to.name === 'login' && from.name === 'login') {
    next(false)
    return
  }
  
  // 如果是登录页面，直接允许访问
  if (to.name === 'login') {
    // 已登录用户访问登录页，重定向到首页
    if (userStore.isLoggedIn) {
      const redirectPath = (to.query.redirect as string) || '/'
      next(redirectPath)
      return
    }
    next()
    return
  }
  
  // 检查是否需要登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 防止重定向循环，检查是否已经在重定向过程中
    if (from.name === 'login' || to.query.redirect) {
      next({ name: 'login', replace: true })
    } else {
      next({ name: 'login', query: { redirect: to.fullPath }, replace: true })
    }
    return
  }
  
  // 检查角色权限
  if (to.meta.requiresRole && userStore.isLoggedIn) {
    const requiredRoles = to.meta.requiresRole as string[]
    const hasRequiredRole = requiredRoles.some(role => userStore.hasRole(role))
    
    if (!hasRequiredRole) {
      next({ name: 'forbidden', replace: true })
      return
    }
  }
  
  next()
})

export default router
