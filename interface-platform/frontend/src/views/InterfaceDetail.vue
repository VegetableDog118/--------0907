<template>
  <div class="interface-detail">
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <el-button @click="$router.go(-1)" type="text" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <h2>{{ interfaceInfo.name || '接口详情' }}</h2>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="interfaceInfo.id" class="detail-content">
        <!-- 基本信息 -->
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="接口名称">{{ interfaceInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="接口类型">{{ interfaceInfo.type }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ interfaceInfo.method }}</el-descriptions-item>
          <el-descriptions-item label="接口地址">{{ interfaceInfo.url }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(interfaceInfo.status)">{{ interfaceInfo.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(interfaceInfo.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 接口描述 -->
        <div class="section">
          <h3>接口描述</h3>
          <p>{{ interfaceInfo.description || '暂无描述' }}</p>
        </div>

        <!-- 请求参数 -->
        <div class="section">
          <h3>请求参数</h3>
          <el-table :data="interfaceInfo.requestParams" border>
            <el-table-column prop="name" label="参数名" width="150" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="required" label="必填" width="80">
              <template #default="{ row }">
                <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                  {{ row.required ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" />
          </el-table>
        </div>

        <!-- 响应参数 -->
        <div class="section">
          <h3>响应参数</h3>
          <el-table :data="interfaceInfo.responseParams" border>
            <el-table-column prop="name" label="参数名" width="150" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="description" label="说明" />
          </el-table>
        </div>

        <!-- 示例代码 -->
        <div class="section">
          <h3>示例代码</h3>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="cURL" name="curl">
              <pre class="code-block">{{ curlExample }}</pre>
            </el-tab-pane>
            <el-tab-pane label="JavaScript" name="javascript">
              <pre class="code-block">{{ jsExample }}</pre>
            </el-tab-pane>
            <el-tab-pane label="Python" name="python">
              <pre class="code-block">{{ pythonExample }}</pre>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 操作按钮 -->
        <div class="actions">
          <el-button type="primary" @click="testInterface">测试接口</el-button>
          <el-button @click="applyAccess">申请访问</el-button>
        </div>
      </div>

      <div v-else class="empty-state">
        <el-empty description="接口不存在或已被删除" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

interface RequestParam {
  name: string
  type: string
  required: boolean
  description: string
}

interface ResponseParam {
  name: string
  type: string
  description: string
}

interface InterfaceInfo {
  id: string
  name: string
  type: string
  method: string
  url: string
  status: string
  description: string
  createdAt: string
  requestParams: RequestParam[]
  responseParams: ResponseParam[]
}

const route = useRoute()

const loading = ref(true)
const activeTab = ref('curl')
const interfaceInfo = ref<InterfaceInfo>({
  id: '',
  name: '',
  type: '',
  method: '',
  url: '',
  status: '',
  description: '',
  createdAt: '',
  requestParams: [],
  responseParams: []
})

// 模拟数据
const mockData: InterfaceInfo = {
  id: route.params.id as string,
  name: '用户信息查询接口',
  type: 'REST API',
  method: 'GET',
  url: '/api/user/info',
  status: '正常',
  description: '根据用户ID查询用户详细信息，包括基本信息、权限信息等',
  createdAt: '2024-01-15 10:30:00',
  requestParams: [
    { name: 'userId', type: 'string', required: true, description: '用户ID' },
    { name: 'includePermissions', type: 'boolean', required: false, description: '是否包含权限信息' }
  ],
  responseParams: [
    { name: 'code', type: 'number', description: '响应状态码' },
    { name: 'message', type: 'string', description: '响应消息' },
    { name: 'data', type: 'object', description: '用户信息对象' },
    { name: 'data.id', type: 'string', description: '用户ID' },
    { name: 'data.username', type: 'string', description: '用户名' },
    { name: 'data.email', type: 'string', description: '邮箱' },
    { name: 'data.role', type: 'string', description: '用户角色' }
  ]
}

const curlExample = computed(() => {
  return `curl -X ${interfaceInfo.value.method} \
  "${window.location.origin}${interfaceInfo.value.url}?userId=123&includePermissions=true" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json"`
})

const jsExample = computed(() => {
  return `fetch('${window.location.origin}${interfaceInfo.value.url}?userId=123&includePermissions=true', {
  method: '${interfaceInfo.value.method}',
  headers: {
    'Authorization': 'Bearer YOUR_API_KEY',
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));`
})

const pythonExample = computed(() => {
  return `import requests

url = "${window.location.origin}${interfaceInfo.value.url}"
params = {
    "userId": "123",
    "includePermissions": True
}
headers = {
    "Authorization": "Bearer YOUR_API_KEY",
    "Content-Type": "application/json"
}

response = requests.${interfaceInfo.value.method.toLowerCase()}(url, params=params, headers=headers)
print(response.json())`
})

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    '正常': 'success',
    '维护': 'warning',
    '停用': 'danger'
  }
  return statusMap[status] || 'info'
}

const formatDate = (dateStr: string) => {
  return dateStr || '-'
}

const loadInterfaceDetail = async () => {
  try {
    loading.value = true
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    interfaceInfo.value = mockData
  } catch {
    ElMessage.error('加载接口详情失败')
  } finally {
    loading.value = false
  }
}

const testInterface = () => {
  ElMessage.info('接口测试功能开发中...')
}

const applyAccess = () => {
  ElMessage.info('申请访问功能开发中...')
}

onMounted(() => {
  loadInterfaceDetail()
})
</script>

<style scoped>
.interface-detail {
  padding: 20px;
}

.detail-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  font-size: 14px;
}

.back-btn:hover {
  color: var(--el-color-primary);
}

.loading-container {
  padding: 20px;
}

.detail-content {
  padding: 20px 0;
}

.section {
  margin: 30px 0;
}

.section h3 {
  margin-bottom: 15px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.code-block {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 15px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.actions {
  margin-top: 30px;
  text-align: center;
}

.actions .el-button {
  margin: 0 10px;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}
</style>