<template>
  <div class="test-interface-catalog">
    <el-container>
      <!-- 左侧分类树 -->
      <el-aside width="300px" class="catalog-aside">
        <el-card class="category-card">
          <template #header>
            <div class="card-header">
              <span>接口分类测试</span>
            </div>
          </template>
          
          <el-tree
            :data="mockCategoryTree"
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
              </div>
              <div class="header-actions">
                <el-button type="primary">
                  <el-icon><Plus /></el-icon>
                  批量订阅
                </el-button>
                <el-button type="success">
                  <el-icon><ShoppingCart /></el-icon>
                  购物车
                </el-button>
              </div>
            </div>
          </template>

          <!-- 接口列表 -->
          <el-table :data="filteredInterfaceList" @selection-change="handleSelectionChange">
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
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="viewInterfaceDetail(row)">详情</el-button>
                <el-button size="small" type="success" @click="addToCart(row)">购物车</el-button>
                <el-button size="small" type="primary" @click="subscribeInterface(row)">订阅</el-button>
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
    <el-dialog v-model="detailDialogVisible" title="接口详情" width="80%">
      <div v-if="selectedInterface">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="接口名称">{{ selectedInterface.interfaceName }}</el-descriptions-item>
          <el-descriptions-item label="接口路径">{{ selectedInterface.interfacePath }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ selectedInterface.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="接口状态">
            <el-tag :type="selectedInterface.status === 'published' ? 'success' : 'info'">
              {{ selectedInterface.status === 'published' ? '已上架' : '未上架' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接口描述" :span="2">{{ selectedInterface.description }}</el-descriptions-item>
        </el-descriptions>
        
        <el-divider>请求参数</el-divider>
        <el-table :data="mockParameters" border>
          <el-table-column prop="paramName" label="参数名" width="150" />
          <el-table-column prop="paramType" label="类型" width="100" />
          <el-table-column prop="required" label="必填" width="80">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" show-overflow-tooltip />
          <el-table-column prop="example" label="示例" width="150" />
        </el-table>
        
        <el-divider>响应示例</el-divider>
        <el-input
          v-model="mockResponse"
          type="textarea"
          :rows="8"
          readonly
          placeholder="响应示例"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Search, Folder, Plus, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 接口类型定义
interface InterfaceItem {
  id: string
  interfaceName: string
  interfacePath: string
  description: string
  categoryId: string
  categoryName: string
  status: string
  requestMethod: string
  version: string
}

interface CategoryItem {
  id: string
  label: string
  count: number
  children?: CategoryItem[]
}

// 模拟数据
const mockCategoryTree = ref([
  {
    id: 'all',
    label: '全部接口',
    count: 4,
    children: [
      { id: 'cat_001', label: '日前现货', count: 1 },
      { id: 'cat_002', label: '预测', count: 1 },
      { id: 'cat_003', label: '辅助服务', count: 1 },
      { id: 'cat_004', label: '电网运行', count: 1 }
    ]
  }
])

const mockInterfaceList = ref([
  {
    id: 'if_001',
    interfaceName: '电力负荷预测接口',
    interfacePath: '/api/data/load-forecast',
    description: '获取未来24小时电力负荷预测数据',
    categoryId: 'cat_002',
    categoryName: '预测',
    status: 'published',
    requestMethod: 'GET',
    version: '1.0.0'
  },
  {
    id: 'if_002',
    interfaceName: '日前现货价格接口',
    interfacePath: '/api/data/day-ahead-price',
    description: '获取日前现货市场价格数据',
    categoryId: 'cat_001',
    categoryName: '日前现货',
    status: 'published',
    requestMethod: 'GET',
    version: '1.0.0'
  },
  {
    id: 'if_003',
    interfaceName: '调频服务数据接口',
    interfacePath: '/api/data/frequency-regulation',
    description: '获取调频辅助服务相关数据',
    categoryId: 'cat_003',
    categoryName: '辅助服务',
    status: 'published',
    requestMethod: 'GET',
    version: '1.0.0'
  },
  {
    id: 'if_004',
    interfaceName: '电网运行状态接口',
    interfacePath: '/api/data/grid-status',
    description: '获取电网实时运行状态数据',
    categoryId: 'cat_004',
    categoryName: '电网运行',
    status: 'published',
    requestMethod: 'GET',
    version: '1.0.0'
  }
])

const mockParameters = ref([
  { paramName: 'dataTime', paramType: 'string', required: true, description: '数据时间，格式：YYYY-MM-DD HH:mm:ss', example: '2024-01-15 10:00:00' },
  { paramName: 'appId', paramType: 'string', required: true, description: 'API密钥ID', example: 'app_123456789' },
  { paramName: 'region', paramType: 'string', required: false, description: '区域代码', example: 'BJ' },
  { paramName: 'format', paramType: 'string', required: false, description: '返回格式', example: 'json' }
])

const mockResponse = ref(`{
  "code": 200,
  "message": "success",
  "data": {
    "dataTime": "2024-01-15 10:00:00",
    "region": "BJ",
    "forecast": [
      {
        "time": "2024-01-15 11:00:00",
        "load": 12500.5,
        "unit": "MW"
      },
      {
        "time": "2024-01-15 12:00:00",
        "load": 13200.8,
        "unit": "MW"
      }
    ]
  },
  "timestamp": 1705294800000
}`)

// 响应式数据
const detailDialogVisible = ref(false)
const selectedInterface = ref<InterfaceItem | null>(null)
const selectedInterfaces = ref<InterfaceItem[]>([])
const currentCategory = ref({ id: 'all', name: '全部接口' })
const shoppingCart = ref<InterfaceItem[]>([])

const treeProps = {
  children: 'children',
  label: 'label'
}

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 4
})

// 计算属性
const filteredInterfaceList = computed(() => {
  if (currentCategory.value.id === 'all') {
    return mockInterfaceList.value
  }
  return mockInterfaceList.value.filter(item => item.categoryId === currentCategory.value.id)
})

const getCategoryTagType = (categoryId: string) => {
  const typeMap: Record<string, string> = {
    cat_001: 'primary',
    cat_002: 'success',
    cat_003: 'warning',
    cat_004: 'danger'
  }
  return typeMap[categoryId] || 'info'
}

// 方法
const handleCategoryClick = (data: { id: string; label: string }) => {
  currentCategory.value = { id: data.id, name: data.label }
  pagination.currentPage = 1
  ElMessage.success(`切换到分类：${data.label}`)
}

const handleSelectionChange = (selection: InterfaceItem[]) => {
  selectedInterfaces.value = selection
}

const viewInterfaceDetail = (row: InterfaceItem) => {
  selectedInterface.value = row
  detailDialogVisible.value = true
}

const addToCart = (row: InterfaceItem) => {
  if (!shoppingCart.value.some(item => item.id === row.id)) {
    shoppingCart.value.push(row)
    ElMessage.success(`${row.interfaceName} 已加入购物车`)
  } else {
    ElMessage.warning('该接口已在购物车中')
  }
}

const subscribeInterface = (row: InterfaceItem) => {
  ElMessage.success(`订阅接口：${row.interfaceName}`)
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
}
</script>

<style scoped>
.test-interface-catalog {
  height: 100vh;
  background-color: #f5f5f5;
}

.catalog-aside {
  background-color: #fff;
  border-right: 1px solid #e8e8e8;
}

.category-card {
  height: 100%;
  border: none;
  box-shadow: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.node-count {
  color: #999;
  font-size: 12px;
  margin-left: auto;
}

.catalog-main {
  background-color: #f5f5f5;
  padding: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.error-state,
.empty-state {
  text-align: center;
  padding: 40px;
}
</style>