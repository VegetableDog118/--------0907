<template>
  <div class="interface-catalog">
    <el-container>
      <!-- 左侧分类树 -->
      <el-aside width="300px" class="catalog-aside">
        <el-card class="category-card" v-loading="categoryLoading">
          <template #header>
            <div class="card-header">
              <span>接口分类</span>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索接口"
                :prefix-icon="Search"
                size="small"
                clearable
              />
            </div>
          </template>
          
          <!-- 分类加载错误状态 -->
          <div v-if="error && categories.length === 0" class="error-state">
            <el-empty description="加载分类失败">
              <el-button type="primary" @click="loadCategories()">重试</el-button>
            </el-empty>
          </div>
          
          <!-- 分类树 -->
          <el-tree
            v-else
            :data="categoryTree"
            :props="treeProps"
            node-key="id"
            default-expand-all
            @node-click="handleCategoryClick"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <el-icon><Folder /></el-icon>
                <span>{{ node.label }}</span>
                <span class="node-count">({{ data.count }})</span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-aside>

      <!-- 右侧接口列表 -->
      <el-main class="catalog-main">
        <el-card>
          <template #header>
            <div class="list-header">
              <div class="header-left">
                <h3>{{ currentCategory.name || '全部接口' }}</h3>
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索接口名称或描述"
                  :prefix-icon="Search"
                  size="default"
                  clearable
                  style="width: 300px; margin-left: 20px;"
                  @keyup.enter="loadInterfaceList"
                />
              </div>
              <div class="header-actions">
                <!-- 数据消费者角色显示订阅相关按钮 -->
                <template v-if="userStore.hasRole('consumer')">
                  <el-badge :value="shoppingCart.length" :hidden="shoppingCart.length === 0" class="cart-badge">
                    <el-button type="warning" @click="handleCartSubscribe" :disabled="shoppingCart.length === 0">
                      <el-icon><ShoppingCart /></el-icon>
                      购物车订阅 ({{ shoppingCart.length }})
                    </el-button>
                  </el-badge>
                  <el-button type="primary" @click="handleBatchSubscribe" :disabled="selectedInterfaces.length === 0">
                    <el-icon><Plus /></el-icon>
                    批量订阅 ({{ selectedInterfaces.length }})
                  </el-button>
                </template>
                
                <!-- 移除技术部和结算部的管理功能按钮，保持接口目录页面的纯粹性 -->
              </div>
            </div>
          </template>

          <!-- 接口列表错误状态 -->
          <div v-if="error && interfaceList.length === 0 && !loading" class="error-state">
            <el-empty description="加载接口列表失败">
              <el-button type="primary" @click="loadInterfaceList()">重试</el-button>
            </el-empty>
          </div>
          
          <!-- 接口列表空状态 -->
          <div v-else-if="!loading && interfaceList.length === 0" class="empty-state">
            <el-empty description="暂无接口数据">
              <template v-if="searchKeyword">
                <p>未找到包含 "{{ searchKeyword }}" 的接口</p>
                <el-button @click="searchKeyword = ''; loadInterfaceList()">清除搜索</el-button>
              </template>
            </el-empty>
          </div>
          
          <!-- 接口列表 -->
          <el-table
            v-else
            :data="interfaceList"
            v-loading="loading"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="interfaceName" label="接口名称" min-width="200">
              <template #default="{ row }">
                <el-link type="primary" @click="viewInterfaceDetail(row)">
                  {{ row.interfaceName }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="接口描述" min-width="300" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="分类" width="120">
              <template #default="{ row }">
                <el-tag :type="getCategoryTagType(row.categoryId)">{{ row.categoryName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="interfacePath" label="接口路径" min-width="250" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'published' ? 'success' : 'info'">
                  {{ row.status === 'published' ? '已上架' : '未上架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" :width="getActionColumnWidth()">
              <template #default="{ row }">
                <el-button size="small" @click="viewInterfaceDetail(row)">详情</el-button>
                
                <!-- 数据消费者角色可以订阅接口 -->
                <template v-if="userStore.hasRole('consumer')">
                  <el-button 
                    size="small" 
                    type="success" 
                    @click="addToCart(row)" 
                    :disabled="shoppingCart.some(item => item.id === row.id)"
                  >
                    <el-icon><ShoppingCart /></el-icon>
                    {{ shoppingCart.some(item => item.id === row.id) ? '已加入' : '购物车' }}
                  </el-button>
                  <el-button size="small" type="primary" @click="subscribeInterface(row)">订阅</el-button>
                </template>
                
                <!-- 移除技术部编辑和结算部上架下架功能，这些功能应在接口管理页面中实现 -->
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-main>
    </el-container>

    <!-- 接口详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="接口详情" width="90%" :close-on-click-modal="false">
      <InterfaceDetail 
        v-if="selectedInterface" 
        :interface-id="selectedInterface.id" 
        :is-subscribed="false"
        @subscribe="handleInterfaceSubscribed"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Folder, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInterfaceCategories, getInterfaceList, type InterfaceInfo, type InterfaceCategory } from '@/api/interface'
import { submitSubscriptionApplication, batchSubmitSubscriptionApplications } from '@/api/approval'
import { useUserStore } from '@/stores/user'
import InterfaceDetail from '@/components/InterfaceDetail.vue'

interface InterfaceItem extends InterfaceInfo {
  categoryName?: string
}

interface CategoryTreeNode {
  id: string
  label: string
  count: number
  children?: CategoryTreeNode[]
}

const userStore = useUserStore()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const categoryLoading = ref(false)
const searchKeyword = ref('')
const detailDialogVisible = ref(false)
const subscribeDialogVisible = ref(false)
const selectedInterface = ref<InterfaceItem | null>(null)
const selectedInterfaces = ref<InterfaceItem[]>([])
const currentCategory = ref({ id: '', name: '' })
const categories = ref<InterfaceCategory[]>([])
const shoppingCart = ref<InterfaceItem[]>([])
const error = ref<string>('')
const retryCount = ref(0)
const maxRetries = 3

// 分类树数据
const categoryTree = ref<CategoryTreeNode[]>([])

const treeProps = {
  children: 'children',
  label: 'label'
}

// 接口列表数据
const interfaceList = ref<InterfaceItem[]>([])

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 计算属性
const getCategoryTagType = (category: string) => {
  const typeMap: Record<string, string> = {
    day_ahead_spot: 'primary',
    forecast: 'success',
    ancillary_service: 'warning',
    grid_operation: 'danger'
  }
  return typeMap[category] || 'info'
}

const getCategoryName = (category: string) => {
  const nameMap: Record<string, string> = {
    day_ahead_spot: '日前现货',
    forecast: '预测',
    ancillary_service: '辅助服务',
    grid_operation: '电网运行'
  }
  return nameMap[category] || category
}

// 根据用户角色计算操作列宽度
const getActionColumnWidth = () => {
  if (!userStore.isLoggedIn) return 100
  
  if (userStore.hasRole('admin')) return 350
  if (userStore.hasRole('settlement')) return 250
  if (userStore.hasRole('tech')) return 200
  if (userStore.hasRole('consumer')) return 250
  
  return 100
}

// 方法
const loadCategories = async (showLoading = true) => {
  if (showLoading) categoryLoading.value = true
  error.value = ''
  
  try {
    console.log('🔄 开始加载分类数据...')
    categories.value = await getInterfaceCategories()
    console.log('✅ 分类数据加载成功:', categories.value)
    buildCategoryTree()
    retryCount.value = 0
  } catch (err: any) {
    console.error('❌ 加载分类失败:', err)
    error.value = err.message || '加载分类失败'
    
    if (retryCount.value < maxRetries) {
      retryCount.value++
      ElMessage.warning(`加载失败，正在重试 (${retryCount.value}/${maxRetries})`)
      setTimeout(() => loadCategories(false), 2000 * retryCount.value)
    } else {
      ElMessage.error('加载分类失败，请刷新页面重试')
    }
  } finally {
    if (showLoading) categoryLoading.value = false
  }
}

const buildCategoryTree = () => {
  const tree: CategoryTreeNode[] = [
    {
      id: 'all',
      label: '全部接口',
      count: 0,
      children: categories.value.map(cat => ({
        id: cat.id,
        label: cat.categoryName,
        count: cat.interfaceCount || 0
      }))
    }
  ]
  
  // 计算总数
  tree[0].count = categories.value.reduce((sum, cat) => sum + (cat.interfaceCount || 0), 0)
  categoryTree.value = tree
}

let loadInterfaceListTimer: number | null = null

const loadInterfaceList = async (showMessage = false) => {
  // 防抖处理
  if (loadInterfaceListTimer) {
    clearTimeout(loadInterfaceListTimer)
  }
  
  loadInterfaceListTimer = setTimeout(async () => {
    loading.value = true
    error.value = ''
    
    try {
      const params = {
        categoryId: currentCategory.value.id === 'all' ? undefined : currentCategory.value.id,
        keyword: searchKeyword.value || undefined,
        status: 'published',
        page: pagination.currentPage,
        size: pagination.pageSize
      }
      
      console.log('🔄 开始加载接口列表，参数:', params)
      const response = await getInterfaceList(params)
      console.log('✅ 接口列表加载成功:', response)
      interfaceList.value = response.records.map(item => ({
        ...item,
        categoryName: categories.value.find(cat => cat.id === item.categoryId)?.categoryName
      }))
      console.log('📋 处理后的接口列表:', interfaceList.value)
      pagination.total = response.total
      
      if (showMessage && response.records.length === 0) {
        ElMessage.info('暂无符合条件的接口')
      }
    } catch (err: any) {
      console.error('❌ 加载接口列表失败:', err)
      error.value = err.message || '加载接口列表失败'
      ElMessage.error('加载接口列表失败，请稍后重试')
      interfaceList.value = []
      pagination.total = 0
    } finally {
      loading.value = false
    }
  }, 300)
}

const handleCategoryClick = (data: { id: string; label: string }) => {
  currentCategory.value = { id: data.id, name: data.label }
  pagination.currentPage = 1
  loadInterfaceList()
}

const handleSelectionChange = (selection: InterfaceItem[]) => {
  selectedInterfaces.value = selection
}

const viewInterfaceDetail = (row: InterfaceItem) => {
  selectedInterface.value = row
  detailDialogVisible.value = true
}

const addToCart = (row: InterfaceItem) => {
  const exists = shoppingCart.value.find(item => item.id === row.id)
  if (exists) {
    ElMessage.warning('该接口已在购物车中')
    return
  }
  shoppingCart.value.push(row)
  ElMessage.success('已添加到购物车')
}

const removeFromCart = (interfaceId: string) => {
  const index = shoppingCart.value.findIndex(item => item.id === interfaceId)
  if (index > -1) {
    shoppingCart.value.splice(index, 1)
    ElMessage.success('已从购物车移除')
  }
}

const subscribeInterface = async (row: InterfaceItem) => {
   try {
     const { value: reason } = await ElMessageBox.prompt('请输入申请理由', '订阅申请', {
       confirmButtonText: '提交申请',
       cancelButtonText: '取消',
       inputPlaceholder: '请详细说明申请该接口的业务需求和使用场景',
       inputType: 'textarea',
       inputValidator: (value) => {
         if (!value || value.trim().length < 10) {
           return '申请理由不能少于10个字符'
         }
         return true
       }
     })
     
     await submitSubscriptionApplication({
       interfaceIds: [row.id],
       reason: reason as string
     })
     
     ElMessage.success('申请已提交，请等待审批')
     
     // 自动跳转到审批模块
     setTimeout(() => {
       ElMessageBox.confirm(
         '申请已成功提交！是否立即跳转到审批模块查看申请状态？',
         '跳转确认',
         {
           confirmButtonText: '立即跳转',
           cancelButtonText: '稍后查看',
           type: 'success'
         }
       ).then(() => {
         router.push('/application/approval')
       }).catch(() => {
         // 用户选择稍后查看，不做任何操作
       })
     }, 1000) // 延迟1秒显示跳转确认框
   } catch (error) {
     if (error !== 'cancel') {
       console.error('提交申请失败:', error)
       
       // 提取详细错误信息
       let errorMessage = '提交申请失败'
       if (error && typeof error === 'object' && error.message) {
         errorMessage = `申请失败: ${error.message}`
       } else if (error && typeof error === 'string') {
         errorMessage = `申请失败: ${error}`
       }
       
       ElMessage.error({
         message: errorMessage,
         duration: 6000, // 显示6秒，让用户有足够时间阅读错误信息
         showClose: true,
         dangerouslyUseHTMLString: false
       })
     }
   }
 }

const handleBatchSubscribe = async () => {
  if (selectedInterfaces.value.length === 0) {
    ElMessage.warning('请先选择要订阅的接口')
    return
  }
  
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入申请理由', '批量订阅申请', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '请详细说明申请这些接口的业务需求和使用场景',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim().length < 10) {
          return '申请理由不能少于10个字符'
        }
        return true
      }
    })
    
    await batchSubmitSubscriptionApplications([{
      interfaceIds: selectedInterfaces.value.map(item => item.id),
      reason: reason as string
    }])
    
    ElMessage.success('批量申请已提交，请等待审批')
    selectedInterfaces.value = []
    
    // 自动跳转到用户中心查看申请状态
    setTimeout(() => {
      ElMessageBox.confirm(
        '申请已成功提交！是否立即跳转到用户中心查看申请状态？',
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '稍后查看',
          type: 'success'
        }
      ).then(() => {
        router.push('/user/center')
        // 通过事件或其他方式通知用户中心切换到申请历史标签
        setTimeout(() => {
          // 这里可以通过全局事件或状态管理来切换到申请历史标签
          window.dispatchEvent(new CustomEvent('switchToApplications'))
        }, 100)
      }).catch(() => {
        // 用户选择稍后查看，不做任何操作
      })
    }, 1000) // 延迟1秒显示跳转确认框
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交批量申请失败:', error)
      
      // 提取详细错误信息
      let errorMessage = '提交批量申请失败'
      if (error && typeof error === 'object' && error.message) {
        errorMessage = `批量申请失败: ${error.message}`
      } else if (error && typeof error === 'string') {
        errorMessage = `批量申请失败: ${error}`
      }
      
      ElMessage.error({
        message: errorMessage,
        duration: 6000, // 显示6秒，让用户有足够时间阅读错误信息
        showClose: true,
        dangerouslyUseHTMLString: false
      })
    }
  }
}

const handleCartSubscribe = async () => {
  if (shoppingCart.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入申请理由', '购物车订阅申请', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '请详细说明申请这些接口的业务需求和使用场景',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim().length < 10) {
          return '申请理由不能少于10个字符'
        }
        return true
      }
    })
    
    await batchSubmitSubscriptionApplications([{
      interfaceIds: shoppingCart.value.map(item => item.id),
      reason: reason as string
    }])
    
    ElMessage.success('购物车申请已提交，请等待审批')
    shoppingCart.value = []
    
    // 自动跳转到用户中心查看申请状态
    setTimeout(() => {
      ElMessageBox.confirm(
        '申请已成功提交！是否立即跳转到用户中心查看申请状态？',
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '稍后查看',
          type: 'success'
        }
      ).then(() => {
        router.push('/user/center')
        // 通过事件或其他方式通知用户中心切换到申请历史标签
        setTimeout(() => {
          // 这里可以通过全局事件或状态管理来切换到申请历史标签
          window.dispatchEvent(new CustomEvent('switchToApplications'))
        }, 100)
      }).catch(() => {
        // 用户选择稍后查看，不做任何操作
      })
    }, 1000) // 延迟1秒显示跳转确认框
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交购物车申请失败:', error)
      
      // 提取详细错误信息
      let errorMessage = '提交购物车申请失败'
      if (error && typeof error === 'object' && error.message) {
        errorMessage = `购物车申请失败: ${error.message}`
      } else if (error && typeof error === 'string') {
        errorMessage = `购物车申请失败: ${error}`
      }
      
      ElMessage.error({
        message: errorMessage,
        duration: 6000, // 显示6秒，让用户有足够时间阅读错误信息
        showClose: true,
        dangerouslyUseHTMLString: false
      })
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadInterfaceList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadInterfaceList()
}

const handleInterfaceSubscribed = (interfaceId: string) => {
  ElMessage.success('接口订阅申请已提交')
  detailDialogVisible.value = false
}

// 移除了权限控制相关的管理方法，这些功能已迁移到接口管理页面

// 搜索监听
watch(searchKeyword, (newVal) => {
  if (newVal.trim() === '') {
    loadInterfaceList()
  }
})

// 生命周期
onMounted(async () => {
  await loadCategories()
  await loadInterfaceList()
})
</script>

<style scoped>
/* 接口目录页面样式 */
.interface-catalog {
  height: 100vh;
  padding: var(--spacing-xl);
  background: var(--bg-extra-light);
  overflow: hidden;
}

/* 侧边栏样式 */
.catalog-aside {
  margin-right: var(--spacing-xl);
}

.category-card {
  height: calc(100vh - var(--spacing-xxxl));
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-base);
  border: 1px solid var(--border-extra-light);
  transition: var(--transition-base);
  overflow: hidden;
}

.category-card:hover {
  box-shadow: var(--shadow-light);
  transform: translateY(-2px);
}

.category-card .el-card__header {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--bg-extra-light) 100%);
  border-bottom: 1px solid var(--border-extra-light);
  padding: var(--spacing-xl);
}

.category-card .el-card__body {
  padding: 0;
  height: calc(100% - 80px);
  overflow-y: auto;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.card-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: var(--font-size-large);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.card-header .el-icon {
  color: var(--primary-color);
  font-size: var(--font-size-medium);
}

/* 分类树样式增强 */
.el-tree {
  background: transparent;
  padding: var(--spacing-md);
}

.el-tree-node {
  margin-bottom: var(--spacing-xs);
}

.el-tree-node__content {
  height: 40px;
  border-radius: var(--border-radius-base);
  margin-bottom: var(--spacing-xs);
  transition: var(--transition-base);
  padding: 0 var(--spacing-md);
}

.el-tree-node__content:hover {
  background: var(--primary-extra-light);
  transform: translateX(4px);
}

.el-tree-node.is-current > .el-tree-node__content {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  font-weight: 600;
  box-shadow: var(--shadow-base);
}

.el-tree-node.is-current > .el-tree-node__content .node-count {
  color: rgba(255, 255, 255, 0.9);
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  width: 100%;
  font-size: var(--font-size-base);
  font-weight: 500;
}

.custom-tree-node .el-icon {
  color: var(--primary-color);
  font-size: var(--font-size-medium);
}

.node-count {
  color: var(--text-placeholder);
  font-size: var(--font-size-small);
  font-weight: 600;
  margin-left: auto;
  background: var(--bg-light);
  padding: 2px var(--spacing-sm);
  border-radius: var(--border-radius-small);
  min-width: 24px;
  text-align: center;
  transition: var(--transition-base);
}

/* 主内容区域 */
.catalog-main {
  padding: 0;
}

.catalog-main .el-card {
  height: calc(100vh - var(--spacing-xxxl));
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-base);
  border: 1px solid var(--border-extra-light);
  overflow: hidden;
}

.catalog-main .el-card__header {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--bg-extra-light) 100%);
  border-bottom: 1px solid var(--border-extra-light);
  padding: var(--spacing-xl);
}

.catalog-main .el-card__body {
  padding: var(--spacing-xl);
  height: calc(100% - 80px);
  overflow-y: auto;
}

/* 列表头部样式 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
  padding: var(--spacing-lg);
  background: var(--bg-white);
  border-radius: var(--border-radius-base);
  border: 1px solid var(--border-extra-light);
  box-shadow: var(--shadow-subtle);
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  gap: var(--spacing-lg);
}

.header-left h3 {
  margin: 0;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: var(--font-size-large);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.header-left h3 .el-icon {
  color: var(--primary-color);
}

.header-left .el-input {
  width: 300px;
  transition: var(--transition-base);
}

.header-left .el-input .el-input__wrapper {
  border-radius: var(--border-radius-base);
  border: 2px solid var(--border-lighter);
  transition: var(--transition-base);
}

.header-left .el-input .el-input__wrapper:hover {
  border-color: var(--primary-lighter);
  box-shadow: 0 0 0 2px var(--primary-extra-light);
}

.header-left .el-input .el-input__wrapper.is-focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-extra-light);
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.header-actions .el-button {
  border-radius: var(--border-radius-base);
  font-weight: 500;
  transition: var(--transition-base);
  position: relative;
  overflow: hidden;
}

.header-actions .el-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: var(--transition-base);
}

.header-actions .el-button:hover::before {
  left: 100%;
}

.header-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-base);
}

/* 购物车徽章样式 */
.cart-badge {
  margin-right: var(--spacing-sm);
}

.cart-badge .el-badge__content {
  background: linear-gradient(135deg, var(--danger-color), #ff4757);
  border: 2px solid white;
  box-shadow: var(--shadow-base);
  font-weight: 600;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

/* 表格样式增强 */
.el-table {
  border-radius: var(--border-radius-base);
  overflow: hidden;
  box-shadow: var(--shadow-subtle);
  border: 1px solid var(--border-extra-light);
}

.el-table .el-table__header {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--bg-extra-light) 100%);
}

.el-table .el-table__header th {
  background: transparent;
  color: var(--text-primary);
  font-weight: 600;
  border-bottom: 2px solid var(--border-light);
  padding: var(--spacing-lg) var(--spacing-md);
}

.el-table .el-table__body tr {
  transition: var(--transition-base);
}

.el-table .el-table__body tr:hover {
  background: var(--primary-extra-light) !important;
  transform: scale(1.01);
}

.el-table .el-table__body td {
  padding: var(--spacing-lg) var(--spacing-md);
  border-bottom: 1px solid var(--border-extra-light);
}

/* 标签样式增强 */
.el-tag {
  border-radius: var(--border-radius-base);
  font-weight: 500;
  padding: var(--spacing-xs) var(--spacing-sm);
  border: none;
  box-shadow: var(--shadow-subtle);
}

.el-tag.el-tag--primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
}

.el-tag.el-tag--success {
  background: linear-gradient(135deg, var(--success-color), #52c41a);
  color: white;
}

.el-tag.el-tag--warning {
  background: linear-gradient(135deg, var(--warning-color), #faad14);
  color: white;
}

.el-tag.el-tag--danger {
  background: linear-gradient(135deg, var(--danger-color), #ff4757);
  color: white;
}

.el-tag.el-tag--info {
  background: linear-gradient(135deg, var(--info-color), #74b9ff);
  color: white;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
  padding: var(--spacing-lg);
  background: var(--bg-white);
  border-radius: var(--border-radius-base);
  border: 1px solid var(--border-extra-light);
  box-shadow: var(--shadow-subtle);
}

.el-pagination {
  --el-pagination-button-color: var(--text-regular);
  --el-pagination-hover-color: var(--primary-color);
  --el-pagination-button-bg-color: var(--bg-white);
  --el-pagination-button-disabled-color: var(--text-placeholder);
}

.el-pagination .el-pager li {
  border-radius: var(--border-radius-base);
  margin: 0 2px;
  transition: var(--transition-base);
}

.el-pagination .el-pager li:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-base);
}

.el-pagination .el-pager li.is-active {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  font-weight: 600;
}

/* 错误和空状态样式 */
.error-state,
.empty-state {
  padding: var(--spacing-xxxl) var(--spacing-xl);
  text-align: center;
  background: var(--bg-white);
  border-radius: var(--border-radius-large);
  border: 1px solid var(--border-extra-light);
  margin: var(--spacing-xl) 0;
}

.error-state .el-empty,
.empty-state .el-empty {
  padding: var(--spacing-xl);
}

.empty-state p {
  color: var(--text-secondary);
  margin: var(--spacing-md) 0;
  font-size: var(--font-size-base);
  line-height: var(--line-height-large);
}

.empty-state .el-button {
  margin-top: var(--spacing-lg);
  border-radius: var(--border-radius-base);
  font-weight: 500;
}

/* 加载状态优化 */
.el-loading-mask {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

.el-loading-spinner {
  color: var(--primary-color);
}

/* 对话框样式增强 */
.el-dialog {
  border-radius: var(--border-radius-large);
  overflow: hidden;
  box-shadow: var(--shadow-dark);
}

.el-dialog__header {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--bg-extra-light) 100%);
  border-bottom: 1px solid var(--border-extra-light);
  padding: var(--spacing-xl);
}

.el-dialog__title {
  font-weight: 600;
  color: var(--text-primary);
  font-size: var(--font-size-large);
}

.el-dialog__body {
  padding: var(--spacing-xl);
  background: var(--bg-white);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .interface-catalog {
    padding: var(--spacing-lg);
  }
  
  .catalog-aside {
    margin-right: var(--spacing-lg);
  }
  
  .header-left .el-input {
    width: 250px;
  }
}

@media (max-width: 768px) {
  .interface-catalog {
    padding: var(--spacing-md);
    height: auto;
    min-height: 100vh;
  }
  
  .category-card,
  .catalog-main .el-card {
    height: auto;
    min-height: 400px;
  }
  
  .list-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--spacing-md);
  }
  
  .header-left {
    flex-direction: column;
    align-items: stretch;
    gap: var(--spacing-md);
  }
  
  .header-left .el-input {
    width: 100% !important;
  }
  
  .header-actions {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .el-table {
    font-size: var(--font-size-small);
  }
  
  .el-table .el-table__header th,
  .el-table .el-table__body td {
    padding: var(--spacing-md) var(--spacing-sm);
  }
}

@media (max-width: 480px) {
  .interface-catalog {
    padding: var(--spacing-sm);
  }
  
  .category-card .el-card__header,
  .catalog-main .el-card__header {
    padding: var(--spacing-lg);
  }
  
  .category-card .el-card__body,
  .catalog-main .el-card__body {
    padding: var(--spacing-lg);
  }
  
  .list-header {
    padding: var(--spacing-md);
  }
  
  .header-left h3 {
    font-size: var(--font-size-medium);
  }
  
  .header-actions .el-button {
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: var(--font-size-small);
  }
}

/* 打印样式 */
@media print {
  .interface-catalog {
    background: white;
    padding: 0;
  }
  
  .category-card,
  .catalog-main .el-card {
    box-shadow: none;
    border: 1px solid #ddd;
  }
  
  .header-actions {
    display: none;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .category-card,
  .catalog-main .el-card,
  .el-table {
    border-width: 2px;
  }
  
  .el-tree-node.is-current > .el-tree-node__content {
    background: var(--text-primary);
    color: var(--bg-white);
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .category-card,
  .catalog-main .el-card,
  .el-tree-node__content,
  .el-table .el-table__body tr,
  .header-actions .el-button,
  .el-pagination .el-pager li {
    transition: none;
  }
  
  .category-card:hover,
  .el-tree-node__content:hover,
  .el-table .el-table__body tr:hover,
  .header-actions .el-button:hover,
  .el-pagination .el-pager li:hover {
    transform: none;
  }
  
  .cart-badge .el-badge__content {
    animation: none;
  }
}
</style>