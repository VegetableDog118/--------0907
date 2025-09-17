<template>
  <div class="user-center">
    <el-container>
      <!-- 左侧导航菜单 -->
      <el-aside width="250px" class="user-aside">
        <el-menu
          :default-active="activeMenu"
          class="user-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
          <el-menu-item index="api-keys">
            <el-icon><Key /></el-icon>
            <span>API密钥</span>
          </el-menu-item>
          <el-menu-item index="subscriptions">
            <el-icon><List /></el-icon>
            <span>我的订阅</span>
          </el-menu-item>
          <el-menu-item index="applications">
            <el-icon><Document /></el-icon>
            <span>申请历史</span>
          </el-menu-item>
          <el-menu-item index="statistics">
            <el-icon><TrendCharts /></el-icon>
            <span>使用统计</span>
          </el-menu-item>
          <el-menu-item index="settings">
            <el-icon><Setting /></el-icon>
            <span>账户设置</span>
          </el-menu-item>
          <el-menu-item index="help">
            <el-icon><QuestionFilled /></el-icon>
            <span>使用帮助</span>
          </el-menu-item>
          <el-menu-item index="permissions">
            <el-icon><Lock /></el-icon>
            <span>权限信息</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 右侧内容区域 -->
      <el-main class="user-main">
        <!-- 个人信息 -->
        <el-card v-if="activeMenu === 'profile'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>个人信息</h3>
              <el-button type="primary" @click="editProfile">编辑信息</el-button>
            </div>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">{{ userInfo?.userId }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="企业名称">{{ userInfo?.companyName }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码">{{ profileData?.creditCode }}</el-descriptions-item>
            <el-descriptions-item label="联系人姓名">{{ profileData?.contactName }}</el-descriptions-item>
            <el-descriptions-item label="手机号码">{{ profileData?.phone }}</el-descriptions-item>
            <el-descriptions-item label="邮箱地址">{{ profileData?.email }}</el-descriptions-item>
            <el-descriptions-item label="部门信息">{{ profileData?.department || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="职位信息">{{ profileData?.position || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="用户角色">
              <el-tag :type="getRoleTagType(profileData?.role)">{{ getRoleName(profileData?.role) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="账户状态">
              <el-tag :type="getStatusTagType(profileData?.status)">{{ getStatusName(profileData?.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDateTime(profileData?.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ formatDateTime(profileData?.lastLoginTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 修改密码 -->
        <el-card v-if="activeMenu === 'password'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>修改密码</h3>
            </div>
          </template>

          <div class="password-form-container">
            <el-alert
              title="密码安全提示"
              description="为了您的账户安全，请定期更换密码。新密码必须包含大小写字母、数字和特殊字符，长度8-20位。"
              type="info"
              :closable="false"
              class="security-alert"
            />

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="120px"
              class="password-form"
            >
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入当前密码"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                  修改密码
                </el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- API密钥管理 -->
        <el-card v-if="activeMenu === 'api-keys'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>API密钥管理</h3>
              <div class="header-actions">
                <el-button type="primary" @click="refreshApiKey" :loading="apiKeyLoading">
                  重置密钥
                </el-button>
                <el-button @click="loadApiKey">刷新</el-button>
              </div>
            </div>
          </template>

          <div class="api-key-section">
            <el-alert
              title="安全提示"
              description="请妥善保管您的API密钥，不要在公开场合泄露。如发现密钥泄露，请立即重置密钥。重置后原密钥将立即失效。"
              type="warning"
              :closable="false"
              class="security-alert"
            />

            <el-descriptions :column="1" border class="api-key-info" v-loading="apiKeyLoading">
              <el-descriptions-item label="AppId">
                <div class="key-display">
                  <el-input 
                    :value="apiKeyData?.appId" 
                    readonly 
                    class="key-input"
                  >
                    <template #append>
                      <el-button @click="copyToClipboard(apiKeyData?.appId, 'AppId')">复制</el-button>
                    </template>
                  </el-input>
                </div>
              </el-descriptions-item>
              
              <el-descriptions-item label="AppSecret">
                <div class="key-display">
                  <el-input 
                    :value="showSecret ? apiKeyData?.appSecret : maskSecret(apiKeyData?.appSecret)" 
                    readonly 
                    class="key-input"
                  >
                    <template #append>
                      <el-button @click="toggleSecretVisibility">{{ showSecret ? '隐藏' : '显示' }}</el-button>
                      <el-button @click="copyToClipboard(apiKeyData?.appSecret, 'AppSecret')">复制</el-button>
                    </template>
                  </el-input>
                </div>
              </el-descriptions-item>
              
              <el-descriptions-item label="密钥状态">
                <el-tag :type="getApiKeyStatusTagType(apiKeyData?.status)">
                  {{ getApiKeyStatusName(apiKeyData?.status) }}
                </el-tag>
              </el-descriptions-item>
              
              <el-descriptions-item label="创建时间">
                {{ formatDateTime(apiKeyData?.createTime) }}
              </el-descriptions-item>
              
              <el-descriptions-item label="最后使用时间">
                {{ formatDateTime(apiKeyData?.lastUsedTime) || '未使用' }}
              </el-descriptions-item>
              
              <el-descriptions-item label="权限范围">
                <div class="permissions-container">
                  <el-tag 
                    v-for="permission in apiKeyData?.permissions" 
                    :key="permission" 
                    class="permission-tag"
                    type="info"
                  >
                    {{ permission }}
                  </el-tag>
                  <span v-if="!apiKeyData?.permissions?.length" class="no-permissions">
                    暂无权限
                  </span>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <!-- 权限信息 -->
        <el-card v-if="activeMenu === 'permissions'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>权限信息</h3>
              <el-button @click="loadPermissions">刷新</el-button>
            </div>
          </template>

          <div class="permissions-section" v-loading="permissionsLoading">
            <el-alert
              title="权限说明"
              description="以下是您当前拥有的系统权限，如需申请更多权限，请联系系统管理员。"
              type="info"
              :closable="false"
              class="security-alert"
            />

            <div class="permissions-list">
              <h4>当前权限列表</h4>
              <div class="permission-items">
                <el-tag 
                  v-for="permission in userPermissions" 
                  :key="permission" 
                  class="permission-item"
                  type="success"
                  size="large"
                >
                  {{ getPermissionName(permission) }}
                </el-tag>
                <div v-if="!userPermissions?.length" class="no-permissions">
                  暂无权限信息
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 我的订阅 -->
        <el-card v-if="activeMenu === 'subscriptions'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>我的订阅</h3>
              <div class="header-actions">
                <el-button @click="loadSubscriptions">刷新</el-button>
              </div>
            </div>
          </template>

          <div class="subscriptions-section" v-loading="subscriptionsLoading">
            <el-alert
              title="订阅说明"
              description="以下是您已订阅的接口列表，您可以使用API密钥调用这些接口获取数据。"
              type="info"
              :closable="false"
              class="security-alert"
            />

            <div class="subscription-filters">
              <el-row :gutter="16">
                <el-col :span="6">
                  <el-select v-model="subscriptionFilters.status" placeholder="订阅状态" clearable @change="loadSubscriptions">
                    <el-option label="全部" value="" />
                    <el-option label="正常" value="active" />
                    <el-option label="已过期" value="expired" />
                    <el-option label="已取消" value="cancelled" />
                  </el-select>
                </el-col>
                <el-col :span="8">
                  <el-input
                    v-model="subscriptionFilters.keyword"
                    placeholder="搜索接口名称"
                    clearable
                    @keyup.enter="loadSubscriptions"
                  >
                    <template #append>
                      <el-button @click="loadSubscriptions">
                        <el-icon><Search /></el-icon>
                      </el-button>
                    </template>
                  </el-input>
                </el-col>
              </el-row>
            </div>

            <el-table :data="subscriptionList" stripe class="subscription-table">
              <el-table-column prop="interfaceId" label="接口ID" width="120" />
              <el-table-column prop="interfaceName" label="接口名称" min-width="200">
                <template #default="{ row }">
                  <div class="interface-info">
                    <div class="interface-name">{{ row.interfaceName || row.interfaceId }}</div>
                    <div class="interface-path" v-if="row.interfacePath">{{ row.interfacePath }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="订阅状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getSubscriptionStatusTagType(row.status)">
                    {{ getSubscriptionStatusName(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="subscribeTime" label="订阅时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.subscribeTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="expireTime" label="到期时间" width="180">
                <template #default="{ row }">
                  {{ row.expireTime ? formatDateTime(row.expireTime) : '永久有效' }}
                </template>
              </el-table-column>
              <el-table-column prop="callCount" label="调用次数" width="100" />
              <el-table-column prop="lastCallTime" label="最后调用" width="180">
                <template #default="{ row }">
                  {{ row.lastCallTime ? formatDateTime(row.lastCallTime) : '未调用' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'active'"
                    type="danger"
                    size="small"
                    @click="handleCancelSubscription(row)"
                  >
                    取消订阅
                  </el-button>
                  <el-button
                    type="primary"
                    size="small"
                    @click="viewInterfaceDetail(row.interfaceId)"
                  >
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="!subscriptionList.length && !subscriptionsLoading" class="empty-state">
              <el-empty description="暂无订阅记录" />
            </div>

            <el-pagination
              v-if="subscriptionPagination.total > 0"
              v-model:current-page="subscriptionPagination.current"
              v-model:page-size="subscriptionPagination.size"
              :total="subscriptionPagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              class="pagination"
              @size-change="loadSubscriptions"
              @current-change="loadSubscriptions"
            />
          </div>
         </el-card>

        <!-- 申请历史 -->
        <el-card v-if="activeMenu === 'applications'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>申请历史</h3>
              <div class="header-actions">
                <el-button @click="loadApplications">刷新</el-button>
              </div>
            </div>
          </template>

          <div class="applications-section" v-loading="applicationsLoading">
            <el-alert
              title="申请说明"
              description="以下是您提交的所有订阅申请记录，包括待审批、已通过和已拒绝的申请。"
              type="info"
              :closable="false"
              class="security-alert"
            />

            <div class="application-filters">
              <el-row :gutter="16">
                <el-col :span="6">
                  <el-select v-model="applicationFilters.status" placeholder="申请状态" clearable @change="loadApplications">
                    <el-option label="全部" value="" />
                    <el-option label="待审批" value="pending" />
                    <el-option label="已通过" value="approved" />
                    <el-option label="已拒绝" value="rejected" />
                  </el-select>
                </el-col>
                <el-col :span="6">
                  <el-date-picker
                    v-model="applicationFilters.dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="loadApplications"
                  />
                </el-col>
                <el-col :span="8">
                  <el-input
                    v-model="applicationFilters.keyword"
                    placeholder="搜索申请原因"
                    clearable
                    @keyup.enter="loadApplications"
                  >
                    <template #append>
                      <el-button @click="loadApplications">
                        <el-icon><Search /></el-icon>
                      </el-button>
                    </template>
                  </el-input>
                </el-col>
              </el-row>
            </div>

            <el-table :data="applicationList" stripe class="application-table">
              <el-table-column prop="id" label="申请ID" width="120" />
              <el-table-column prop="interfaceIds" label="申请接口" min-width="200">
                <template #default="{ row }">
                  <div class="interface-list">
                    <el-tag
                      v-for="interfaceId in row.interfaceIds.slice(0, 3)"
                      :key="interfaceId"
                      size="small"
                      class="interface-tag"
                    >
                      {{ interfaceId }}
                    </el-tag>
                    <el-tag v-if="row.interfaceIds.length > 3" size="small" type="info">
                      +{{ row.interfaceIds.length - 3 }}个
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="申请原因" min-width="150">
                <template #default="{ row }">
                  <el-tooltip :content="row.reason" placement="top">
                    <div class="reason-text">{{ row.reason }}</div>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="申请状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getApplicationStatusTagType(row.status)">
                    {{ getApplicationStatusName(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="submitTime" label="提交时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.submitTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="processTime" label="处理时间" width="180">
                <template #default="{ row }">
                  {{ row.processTime ? formatDateTime(row.processTime) : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    size="small"
                    @click="viewApplicationDetail(row)"
                  >
                    查看详情
                  </el-button>
                  <el-button
                    v-if="row.status === 'pending'"
                    type="danger"
                    size="small"
                    @click="withdrawApplication(row)"
                  >
                    撤回
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="!applicationList.length && !applicationsLoading" class="empty-state">
              <el-empty description="暂无申请记录" />
            </div>

            <el-pagination
              v-if="applicationPagination.total > 0"
              v-model:current-page="applicationPagination.current"
              v-model:page-size="applicationPagination.size"
              :total="applicationPagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              class="pagination"
              @size-change="loadApplications"
              @current-change="loadApplications"
            />
          </div>
         </el-card>

        <!-- 使用统计 -->
        <el-card v-if="activeMenu === 'statistics'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>使用统计</h3>
              <div class="header-actions">
                <el-date-picker
                  v-model="statisticsDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="loadStatistics"
                  size="small"
                />
                <el-button @click="loadStatistics">刷新</el-button>
              </div>
            </div>
          </template>

          <div class="statistics-section" v-loading="statisticsLoading">
            <!-- 统计概览 -->
            <div class="stats-overview">
              <el-row :gutter="16">
                <el-col :span="6">
                  <el-card class="stats-card">
                    <div class="stats-item">
                      <div class="stats-value">{{ statisticsData.totalCalls || 0 }}</div>
                      <div class="stats-label">总调用次数</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card class="stats-card">
                    <div class="stats-item">
                      <div class="stats-value">{{ statisticsData.successRate || '0%' }}</div>
                      <div class="stats-label">成功率</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card class="stats-card">
                    <div class="stats-item">
                      <div class="stats-value">{{ statisticsData.avgResponseTime || '0ms' }}</div>
                      <div class="stats-label">平均响应时间</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="6">
                  <el-card class="stats-card">
                    <div class="stats-item">
                      <div class="stats-value">{{ statisticsData.activeInterfaces || 0 }}</div>
                      <div class="stats-label">活跃接口数</div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>

            <!-- 调用趋势图表 -->
            <el-card class="chart-card">
              <template #header>
                <h4>调用趋势</h4>
              </template>
              <div class="chart-container">
                <div v-if="!statisticsData.trendData?.length" class="empty-chart">
                  <el-empty description="暂无趋势数据" />
                </div>
                <div v-else class="trend-chart">
                  <!-- 这里可以集成图表库如ECharts -->
                  <div class="simple-chart">
                    <div v-for="(item, index) in statisticsData.trendData" :key="index" class="chart-bar">
                      <div class="bar-value">{{ item.calls }}</div>
                      <div class="bar" :style="{ height: `${(item.calls / Math.max(...statisticsData.trendData.map(d => d.calls))) * 100}px` }"></div>
                      <div class="bar-label">{{ item.date }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 热门接口排行 -->
            <el-card class="ranking-card">
              <template #header>
                <h4>热门接口排行</h4>
              </template>
              <div class="ranking-list">
                <div v-if="!statisticsData.topInterfaces?.length" class="empty-ranking">
                  <el-empty description="暂无排行数据" />
                </div>
                <div v-else>
                  <div v-for="(item, index) in statisticsData.topInterfaces" :key="item.interfaceId" class="ranking-item">
                    <div class="ranking-number">
                      <el-tag :type="getRankingTagType(index + 1)" size="small">
                        {{ index + 1 }}
                      </el-tag>
                    </div>
                    <div class="ranking-info">
                      <div class="interface-name">{{ item.interfaceName || item.interfaceId }}</div>
                      <div class="interface-path">{{ item.interfacePath }}</div>
                    </div>
                    <div class="ranking-stats">
                      <div class="call-count">{{ item.callCount }}次</div>
                      <div class="success-rate">成功率: {{ item.successRate }}%</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 错误统计 -->
            <el-card class="error-card">
              <template #header>
                <h4>错误统计</h4>
              </template>
              <div class="error-stats">
                <div v-if="!statisticsData.errorStats?.length" class="empty-errors">
                  <el-empty description="暂无错误数据" />
                </div>
                <el-table v-else :data="statisticsData.errorStats" size="small">
                  <el-table-column prop="errorCode" label="错误码" width="100" />
                  <el-table-column prop="errorMessage" label="错误信息" min-width="200" />
                  <el-table-column prop="count" label="出现次数" width="100" />
                  <el-table-column prop="lastOccurred" label="最后出现时间" width="180">
                    <template #default="{ row }">
                      {{ formatDateTime(row.lastOccurred) }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </div>
         </el-card>

        <!-- 安全中心 -->
        <el-card v-if="activeMenu === 'settings'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>安全中心</h3>
              <div class="header-actions">
                <el-button @click="loadSecurityData">刷新</el-button>
              </div>
            </div>
          </template>

          <div class="security-section" v-loading="securityLoading">
            <el-tabs v-model="activeSecurityTab" class="security-tabs">
              <!-- 登录日志 -->
              <el-tab-pane label="登录日志" name="loginLogs">
                <div class="login-logs-section">
                  <div class="logs-filters">
                    <el-row :gutter="16">
                      <el-col :span="8">
                        <el-date-picker
                          v-model="securityFilters.loginDateRange"
                          type="daterange"
                          range-separator="至"
                          start-placeholder="开始日期"
                          end-placeholder="结束日期"
                          format="YYYY-MM-DD"
                          value-format="YYYY-MM-DD"
                          @change="loadLoginLogs"
                          size="small"
                        />
                      </el-col>
                      <el-col :span="6">
                        <el-select v-model="securityFilters.loginStatus" placeholder="登录状态" clearable @change="loadLoginLogs" size="small">
                          <el-option label="全部" value="" />
                          <el-option label="成功" value="success" />
                          <el-option label="失败" value="failed" />
                        </el-select>
                      </el-col>
                    </el-row>
                  </div>

                  <el-table :data="securityData.loginLogs" stripe class="logs-table">
                    <el-table-column prop="loginTime" label="登录时间" width="180">
                      <template #default="{ row }">
                        {{ formatDateTime(row.loginTime) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="ipAddress" label="IP地址" width="150" />
                    <el-table-column prop="location" label="登录地点" width="200" />
                    <el-table-column prop="device" label="设备信息" min-width="200" />
                    <el-table-column prop="browser" label="浏览器" width="150" />
                    <el-table-column prop="status" label="状态" width="100">
                      <template #default="{ row }">
                        <el-tag :type="row.status === 'success' ? 'success' : 'danger'">
                          {{ row.status === 'success' ? '成功' : '失败' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>

                  <div v-if="!securityData.loginLogs.length" class="empty-state">
                    <el-empty description="暂无登录记录" />
                  </div>
                </div>
              </el-tab-pane>

              <!-- 异常告警 -->
              <el-tab-pane label="异常告警" name="alerts">
                <div class="alerts-section">
                  <div class="alerts-filters">
                    <el-row :gutter="16">
                      <el-col :span="6">
                        <el-select v-model="securityFilters.alertLevel" placeholder="告警级别" clearable @change="loadAlerts" size="small">
                          <el-option label="全部" value="" />
                          <el-option label="高危" value="high" />
                          <el-option label="中危" value="medium" />
                          <el-option label="低危" value="low" />
                        </el-select>
                      </el-col>
                      <el-col :span="6">
                        <el-select v-model="securityFilters.alertStatus" placeholder="处理状态" clearable @change="loadAlerts" size="small">
                          <el-option label="全部" value="" />
                          <el-option label="未处理" value="pending" />
                          <el-option label="已处理" value="resolved" />
                          <el-option label="已忽略" value="ignored" />
                        </el-select>
                      </el-col>
                    </el-row>
                  </div>

                  <div class="alerts-list">
                    <div v-for="alert in securityData.alerts" :key="alert.id" class="alert-item">
                      <div class="alert-header">
                        <div class="alert-title">
                          <el-tag :type="getAlertLevelTagType(alert.level)" size="small">
                            {{ getAlertLevelName(alert.level) }}
                          </el-tag>
                          <span class="alert-message">{{ alert.message }}</span>
                        </div>
                        <div class="alert-time">{{ formatDateTime(alert.createTime) }}</div>
                      </div>
                      <div class="alert-content">
                        <p class="alert-description">{{ alert.description }}</p>
                        <div class="alert-details">
                          <span><strong>IP地址：</strong>{{ alert.ipAddress }}</span>
                          <span><strong>用户代理：</strong>{{ alert.userAgent }}</span>
                        </div>
                      </div>
                      <div class="alert-actions" v-if="alert.status === 'pending'">
                        <el-button size="small" @click="resolveAlert(alert.id)">标记已处理</el-button>
                        <el-button size="small" type="info" @click="ignoreAlert(alert.id)">忽略</el-button>
                      </div>
                    </div>
                    
                    <div v-if="!securityData.alerts.length" class="empty-state">
                      <el-empty description="暂无异常告警" />
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <!-- 设备管理 -->
              <el-tab-pane label="设备管理" name="devices">
                <div class="devices-section">
                  <el-alert
                    title="设备管理说明"
                    description="以下是您最近登录的设备列表，如发现异常设备请及时移除。"
                    type="info"
                    :closable="false"
                    class="security-alert"
                  />

                  <div class="devices-list">
                    <div v-for="device in securityData.devices" :key="device.id" class="device-item">
                      <div class="device-info">
                        <div class="device-icon">
                          <el-icon size="24">
                            <Monitor v-if="device.deviceType === 'desktop'" />
                            <Cellphone v-else />
                          </el-icon>
                        </div>
                        <div class="device-details">
                          <div class="device-name">{{ device.deviceName }}</div>
                          <div class="device-meta">
                            <span>{{ device.browser }} · {{ device.os }}</span>
                            <span class="device-location">{{ device.location }}</span>
                          </div>
                          <div class="device-time">
                            <span v-if="device.isCurrent" class="current-device">当前设备</span>
                            <span v-else>最后活跃：{{ formatDateTime(device.lastActiveTime) }}</span>
                          </div>
                        </div>
                      </div>
                      <div class="device-actions">
                        <el-button
                          v-if="!device.isCurrent"
                          type="danger"
                          size="small"
                          @click="removeDevice(device.id)"
                        >
                          移除设备
                        </el-button>
                      </div>
                    </div>
                    
                    <div v-if="!securityData.devices.length" class="empty-state">
                      <el-empty description="暂无设备记录" />
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
         </el-card>

        <!-- 使用帮助 -->
        <el-card v-if="activeMenu === 'help'" class="content-card">
          <template #header>
            <div class="card-header">
              <h3>使用帮助</h3>
            </div>
          </template>

          <div class="help-section">
            <el-tabs v-model="activeHelpTab" class="help-tabs">
              <!-- API使用指南 -->
              <el-tab-pane label="API使用指南" name="apiGuide">
                <div class="api-guide-section">
                  <el-collapse v-model="activeGuideItems" class="guide-collapse">
                    <el-collapse-item title="1. 快速开始" name="quickStart">
                      <div class="guide-content">
                        <h4>获取API密钥</h4>
                        <p>在用户中心的"API密钥"页面可以查看和管理您的API密钥。</p>
                        <el-code-block language="bash" :code="`# 您的API密钥信息
AppId: ${apiKeyData?.appId || 'your-app-id'}
AppSecret: ${apiKeyData?.appSecret ? '***' : 'your-app-secret'}`" />
                        
                        <h4>基础调用示例</h4>
                        <p>使用HTTP请求调用接口：</p>
                        <el-code-block language="javascript" :code="quickStartCode" />
                      </div>
                    </el-collapse-item>
                    
                    <el-collapse-item title="2. 认证方式" name="authentication">
                      <div class="guide-content">
                        <h4>请求头认证</h4>
                        <p>在请求头中添加以下参数：</p>
                        <el-code-block language="http" :code="authenticationCode" />
                        
                        <h4>参数说明</h4>
                        <ul>
                          <li><strong>X-App-Id</strong>: 您的应用ID</li>
                          <li><strong>X-App-Secret</strong>: 您的应用密钥</li>
                          <li><strong>X-Timestamp</strong>: 当前时间戳（可选）</li>
                        </ul>
                      </div>
                    </el-collapse-item>
                    
                    <el-collapse-item title="3. 响应格式" name="responseFormat">
                      <div class="guide-content">
                        <h4>标准响应格式</h4>
                        <p>所有API接口都遵循统一的响应格式：</p>
                        <el-code-block language="json" :code="responseFormatCode" />
                        
                        <h4>状态码说明</h4>
                        <ul>
                          <li><strong>200</strong>: 请求成功</li>
                          <li><strong>400</strong>: 请求参数错误</li>
                          <li><strong>401</strong>: 认证失败</li>
                          <li><strong>403</strong>: 权限不足</li>
                          <li><strong>500</strong>: 服务器内部错误</li>
                        </ul>
                      </div>
                    </el-collapse-item>
                    
                    <el-collapse-item title="4. 错误处理" name="errorHandling">
                      <div class="guide-content">
                        <h4>错误响应示例</h4>
                        <el-code-block language="json" :code="errorHandlingCode" />
                        
                        <h4>常见错误及解决方案</h4>
                        <el-table :data="commonErrors" size="small">
                          <el-table-column prop="code" label="错误码" width="100" />
                          <el-table-column prop="message" label="错误信息" width="200" />
                          <el-table-column prop="solution" label="解决方案" />
                        </el-table>
                      </div>
                    </el-collapse-item>
                    
                    <el-collapse-item title="5. 最佳实践" name="bestPractices">
                      <div class="guide-content">
                        <h4>性能优化建议</h4>
                        <ul>
                          <li>合理设置请求超时时间</li>
                          <li>实现请求重试机制</li>
                          <li>使用连接池复用连接</li>
                          <li>避免频繁的短连接</li>
                        </ul>
                        
                        <h4>安全建议</h4>
                        <ul>
                          <li>妥善保管API密钥，不要在客户端代码中暴露</li>
                          <li>定期更换API密钥</li>
                          <li>监控API调用异常</li>
                          <li>实现访问频率限制</li>
                        </ul>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-tab-pane>
              
              <!-- SDK文档 -->
              <el-tab-pane label="SDK文档" name="sdkDoc">
                <div class="sdk-doc-section">
                  <el-alert
                    title="SDK开发中"
                    description="我们正在开发各种语言的SDK，敬请期待。目前请使用HTTP请求方式调用接口。"
                    type="info"
                    :closable="false"
                    class="sdk-alert"
                  />
                  
                  <div class="sdk-languages">
                    <div class="language-card" v-for="lang in sdkLanguages" :key="lang.name">
                      <div class="language-icon">
                        <el-icon size="32">
                          <Document />
                        </el-icon>
                      </div>
                      <div class="language-info">
                        <h4>{{ lang.name }}</h4>
                        <p>{{ lang.description }}</p>
                        <el-tag :type="lang.status === 'available' ? 'success' : 'info'" size="small">
                          {{ lang.status === 'available' ? '可用' : '开发中' }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              
              <!-- 常见问题 -->
              <el-tab-pane label="常见问题" name="faq">
                <div class="faq-section">
                  <el-collapse v-model="activeFaqItems" class="faq-collapse">
                    <el-collapse-item v-for="faq in faqList" :key="faq.id" :title="faq.question" :name="faq.id">
                      <div class="faq-answer" v-html="faq.answer"></div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-tab-pane>
              
              <!-- 联系支持 -->
              <el-tab-pane label="联系支持" name="support">
                <div class="support-section">
                  <el-row :gutter="24">
                    <el-col :span="12">
                      <el-card class="support-card">
                        <template #header>
                          <h4>技术支持</h4>
                        </template>
                        <div class="support-content">
                          <p><strong>邮箱：</strong>support@powertrading.com</p>
                          <p><strong>电话：</strong>400-123-4567</p>
                          <p><strong>工作时间：</strong>周一至周五 9:00-18:00</p>
                          <el-button type="primary" @click="contactSupport">发送邮件</el-button>
                        </div>
                      </el-card>
                    </el-col>
                    <el-col :span="12">
                      <el-card class="support-card">
                        <template #header>
                          <h4>在线文档</h4>
                        </template>
                        <div class="support-content">
                          <p>访问我们的在线文档获取更多详细信息：</p>
                          <ul>
                            <li><a href="#" target="_blank">API参考文档</a></li>
                            <li><a href="#" target="_blank">开发者指南</a></li>
                            <li><a href="#" target="_blank">更新日志</a></li>
                          </ul>
                          <el-button @click="openDocumentation">访问文档</el-button>
                        </div>
                      </el-card>
                    </el-col>
                  </el-row>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
          </el-main>
        </el-container>

    <!-- 编辑个人信息弹窗 -->
    <el-dialog v-model="profileDialogVisible" title="编辑个人信息" width="600px">
      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="120px"
      >
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input
            v-model="profileForm.contactName"
            placeholder="请输入联系人姓名"
            :maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="手机号码" prop="phone">
          <el-input
            v-model="profileForm.phone"
            placeholder="请输入手机号码"
            :maxlength="11"
          />
        </el-form-item>

        <el-form-item label="邮箱地址" prop="email">
          <el-input
            v-model="profileForm.email"
            placeholder="请输入邮箱地址"
            :maxlength="100"
          />
        </el-form-item>

        <el-form-item label="部门" prop="department">
          <el-input
            v-model="profileForm.department"
            placeholder="请输入所在部门"
            :maxlength="50"
          />
        </el-form-item>

        <el-form-item label="职位" prop="position">
          <el-input
            v-model="profileForm.position"
            placeholder="请输入职位"
            :maxlength="50"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Key, List, Document, TrendCharts, Setting, QuestionFilled, Search, Monitor, Cellphone } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { 
  getCurrentUserInfo, 
  updateUserInfo, 
  changePassword, 
  getApiKey, 
  resetApiKey,
  getUserPermissions,
  type UserInfoResponse,
  type UserUpdateRequest,
  type ChangePasswordRequest,
  type ApiKeyResponse
} from '@/api/user'
import {
  getUserSubscriptions,
  cancelSubscription,
  getApplicationList,
  type UserInterfaceSubscription,
  type SubscriptionApplication,
  type ApplicationListRequest
} from '@/api/approval'
import {
  getInterfaceDetail,
  type InterfaceDetailResponse
} from '@/api/interface'

const userStore = useUserStore()
const activeMenu = ref('profile')
const profileDialogVisible = ref(false)
const passwordLoading = ref(false)
const profileLoading = ref(false)
const apiKeyLoading = ref(false)
const permissionsLoading = ref(false)
const subscriptionsLoading = ref(false)
const applicationsLoading = ref(false)
const statisticsLoading = ref(false)
const securityLoading = ref(false)
const showSecret = ref(false)

// 安全中心相关数据
const activeSecurityTab = ref('loginLogs')
const securityFilters = reactive({
  loginDateRange: null as [string, string] | null,
  loginStatus: '',
  alertLevel: '',
  alertStatus: ''
})
const securityData = reactive({
  loginLogs: [] as {
    id: string;
    loginTime: string;
    ipAddress: string;
    location: string;
    device: string;
    browser: string;
    status: 'success' | 'failed';
  }[],
  alerts: [] as {
    id: string;
    level: 'high' | 'medium' | 'low';
    message: string;
    description: string;
    ipAddress: string;
    userAgent: string;
    status: 'pending' | 'resolved' | 'ignored';
    createTime: string;
  }[],
  devices: [] as {
    id: string;
    deviceName: string;
    deviceType: 'desktop' | 'mobile';
    browser: string;
    os: string;
    location: string;
    isCurrent: boolean;
    lastActiveTime: string;
  }[]
 })

// 使用帮助相关数据
const activeHelpTab = ref('apiGuide')
const activeGuideItems = ref(['quickStart'])
const activeFaqItems = ref([])

// 代码示例
const quickStartCode = `// 使用fetch调用接口
fetch('https://api.powertrading.com/v1/your-interface', {
  method: 'GET',
  headers: {
    'X-App-Id': 'your-app-id',
    'X-App-Secret': 'your-app-secret',
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));`

const authenticationCode = `GET /api/v1/your-interface HTTP/1.1
Host: api.powertrading.com
X-App-Id: your-app-id
X-App-Secret: your-app-secret
X-Timestamp: 1640995200
Content-Type: application/json`

const responseFormatCode = `{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": {
    // 具体的业务数据
  },
  "timestamp": 1640995200
}`

const errorHandlingCode = `{
  "success": false,
  "code": 400,
  "message": "请求参数错误",
  "error": {
    "field": "userId",
    "reason": "用户ID不能为空"
  },
  "timestamp": 1640995200
}`

// 常见错误列表
const commonErrors = [
  {
    code: '400',
    message: '请求参数错误',
    solution: '检查请求参数格式和必填字段'
  },
  {
    code: '401',
    message: '认证失败',
    solution: '检查API密钥是否正确'
  },
  {
    code: '403',
    message: '权限不足',
    solution: '确认已订阅该接口'
  },
  {
    code: '429',
    message: '请求频率超限',
    solution: '降低请求频率或联系客服提升限额'
  },
  {
    code: '500',
    message: '服务器内部错误',
    solution: '稍后重试或联系技术支持'
  }
]

// SDK语言列表
const sdkLanguages = [
  {
    name: 'JavaScript/Node.js',
    description: 'JavaScript和Node.js SDK',
    status: 'developing'
  },
  {
    name: 'Python',
    description: 'Python SDK',
    status: 'developing'
  },
  {
    name: 'Java',
    description: 'Java SDK',
    status: 'developing'
  },
  {
    name: 'PHP',
    description: 'PHP SDK',
    status: 'developing'
  },
  {
    name: 'Go',
    description: 'Go SDK',
    status: 'developing'
  },
  {
    name: 'C#',
    description: '.NET SDK',
    status: 'developing'
  }
]

// 常见问题列表
const faqList = [
  {
    id: 'faq1',
    question: '如何获取API密钥？',
    answer: '登录用户中心，在"API密钥"页面可以查看您的AppId和AppSecret。请妥善保管这些信息。'
  },
  {
    id: 'faq2',
    question: '接口调用频率有限制吗？',
    answer: '是的，不同的接口有不同的调用频率限制。具体限制请查看接口详情页面，或联系客服了解更多信息。'
  },
  {
    id: 'faq3',
    question: '如何处理接口返回的错误？',
    answer: '接口返回的错误信息包含错误码和错误描述。请根据错误码进行相应的处理，常见错误码及解决方案请参考"错误处理"章节。'
  },
  {
    id: 'faq4',
    question: '可以在前端直接调用接口吗？',
    answer: '不建议在前端直接调用接口，因为这会暴露您的API密钥。建议通过后端服务调用接口，前端通过您的后端获取数据。'
  },
  {
    id: 'faq5',
    question: '接口数据更新频率是多少？',
    answer: '不同接口的数据更新频率不同，实时接口会立即返回最新数据，批量接口通常每日更新。具体更新频率请查看接口详情。'
  }
]

// 订阅管理相关数据
const subscriptionList = ref<(UserInterfaceSubscription & { interfaceName?: string; interfacePath?: string })[]>([])
const subscriptionPagination = reactive({
  current: 1,
  size: 20,
  total: 0
})
const subscriptionFilters = reactive({
  status: '',
  keyword: ''
})

// 申请历史相关数据
const applicationList = ref<SubscriptionApplication[]>([])
const applicationPagination = reactive({
  current: 1,
  size: 20,
  total: 0
})
const applicationFilters = reactive({
  status: '',
  keyword: '',
  dateRange: null as [string, string] | null
})

// 使用统计相关数据
const statisticsDateRange = ref<[string, string] | null>(null)
const statisticsData = reactive({
  totalCalls: 0,
  successRate: '0%',
  avgResponseTime: '0ms',
  activeInterfaces: 0,
  trendData: [] as { date: string; calls: number }[],
  topInterfaces: [] as {
    interfaceId: string;
    interfaceName?: string;
    interfacePath?: string;
    callCount: number;
    successRate: number;
  }[],
  errorStats: [] as {
    errorCode: string;
    errorMessage: string;
    count: number;
    lastOccurred: string;
  }[]
})

// 表单引用
const passwordFormRef = ref<FormInstance>()
const profileFormRef = ref<FormInstance>()

// 用户信息
const userInfo = computed(() => userStore.userInfo)
const profileData = ref<UserInfoResponse | null>(null)
const apiKeyData = ref<ApiKeyResponse | null>(null)
const userPermissions = ref<string[]>([])

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 编辑个人信息表单
const profileForm = reactive({
  contactName: '',
  phone: '',
  email: '',
  department: '',
  position: ''
})

// 密码表单验证规则
const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度为8-20个字符', trigger: 'blur' },
    { 
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/, 
      message: '密码必须包含大小写字母、数字和特殊字符', 
      trigger: 'blur' 
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 个人信息表单验证规则
const profileRules: FormRules = {
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '联系人姓名长度为2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 菜单选择处理
const handleMenuSelect = (key: string) => {
  activeMenu.value = key
  
  // 根据选择的菜单加载对应数据
  switch (key) {
    case 'profile':
      loadUserProfile()
      break
    case 'api-keys':
      loadApiKey()
      break
    case 'subscriptions':
      loadSubscriptions()
      break
    case 'applications':
      loadApplications()
      break
    case 'statistics':
      loadStatistics()
      break
    case 'settings':
      loadSecurityData()
      break
    case 'help':
      // 使用帮助页面不需要加载数据
      break
    case 'permissions':
      loadPermissions()
      break
  }
}

// 加载用户详细信息
const loadUserProfile = async () => {
  try {
    // 检查用户是否已登录
    if (!userStore.isLoggedIn) {
      ElMessage.error('请先登录')
      return
    }
    
    profileData.value = await getCurrentUserInfo()
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    
    // 显示具体的错误信息
    if (error?.message) {
      ElMessage.error(`加载用户信息失败: ${error.message}`)
    } else {
      ElMessage.error('加载用户信息失败，请检查网络连接或重新登录')
    }
    
    // 如果是认证错误，跳转到登录页
    if (error?.response?.status === 401) {
      userStore.performLogout()
      window.location.href = '/login'
    }
  }
}

// 编辑个人信息
const editProfile = () => {
  if (profileData.value) {
    profileForm.contactName = profileData.value.contactName
    profileForm.phone = profileData.value.phone
    profileForm.email = profileData.value.email
    profileForm.department = profileData.value.department || ''
    profileForm.position = profileData.value.position || ''
  }
  profileDialogVisible.value = true
}

// 更新个人信息
const handleUpdateProfile = async () => {
  if (!profileFormRef.value) return
  
  try {
    const valid = await profileFormRef.value.validate()
    if (!valid) return
    
    profileLoading.value = true
    
    const updateData: UserUpdateRequest = {
      contactName: profileForm.contactName,
      phone: profileForm.phone,
      email: profileForm.email,
      department: profileForm.department || undefined,
      position: profileForm.position || undefined
    }
    
    await updateUserInfo(updateData)
    
    // 更新本地用户信息
    await userStore.fetchUserInfo()
    
    // 重新加载用户详细信息
    await loadUserProfile()
    
    ElMessage.success('个人信息更新成功')
    profileDialogVisible.value = false
    
  } catch {
    ElMessage.error('更新个人信息失败')
  } finally {
    profileLoading.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  try {
    const valid = await passwordFormRef.value.validate()
    if (!valid) return
    
    passwordLoading.value = true
    
    const changePasswordData: ChangePasswordRequest = {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    }
    
    await changePassword(changePasswordData)
    
    ElMessage.success('密码修改成功，请重新登录')
    
    // 清空表单
    resetPasswordForm()
    
    // 延迟登出
    setTimeout(() => {
      userStore.performLogout()
      window.location.href = '/login'
    }, 2000)
    
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}

// 加载API密钥
const loadApiKey = async () => {
  try {
    apiKeyLoading.value = true
    apiKeyData.value = await getApiKey()
  } catch {
    ElMessage.error('获取API密钥失败')
  } finally {
    apiKeyLoading.value = false
  }
}

// 重置API密钥
const refreshApiKey = async () => {
  try {
    await ElMessageBox.confirm(
      '重置API密钥后，原密钥将立即失效，请确保已更新所有使用该密钥的应用。是否继续？',
      '确认重置API密钥',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    apiKeyLoading.value = true
    apiKeyData.value = await resetApiKey()
    
    ElMessage.success('API密钥重置成功')
    showSecret.value = true // 重置后显示新密钥
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('API密钥重置失败')
    }
  } finally {
    apiKeyLoading.value = false
  }
}

// 加载用户权限
const loadPermissions = async () => {
  try {
    permissionsLoading.value = true
    userPermissions.value = await getUserPermissions()
  } catch {
    ElMessage.error('获取权限信息失败')
  } finally {
    permissionsLoading.value = false
  }
}

// 加载订阅列表
const loadSubscriptions = async () => {
  try {
    subscriptionsLoading.value = true
    const response = await getUserSubscriptions(
      userStore.userInfo?.userId,
      subscriptionPagination.current,
      subscriptionPagination.size
    )
    
    // 获取接口详情信息
    const subscriptionsWithDetails = await Promise.all(
      response.records.map(async (subscription) => {
        try {
          const interfaceDetail = await getInterfaceDetail(subscription.interfaceId)
          return {
            ...subscription,
            interfaceName: interfaceDetail.interface.interfaceName,
            interfacePath: interfaceDetail.interface.interfacePath
          }
        } catch {
          return {
            ...subscription,
            interfaceName: `接口-${subscription.interfaceId}`,
            interfacePath: ''
          }
        }
      })
    )
    
    subscriptionList.value = subscriptionsWithDetails
    subscriptionPagination.total = response.total
    subscriptionPagination.current = response.current
    subscriptionPagination.size = response.size
    
  } catch {
    ElMessage.error('获取订阅列表失败')
  } finally {
    subscriptionsLoading.value = false
  }
}

// 取消订阅
const handleCancelSubscription = async (subscription: UserInterfaceSubscription) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消订阅接口"${subscription.interfaceId}"吗？取消后将无法继续调用该接口。`,
      '确认取消订阅',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await cancelSubscription(subscription.id)
    ElMessage.success('订阅已取消')
    
    // 重新加载订阅列表
    await loadSubscriptions()
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('取消订阅失败')
    }
  }
}

// 查看接口详情
const viewInterfaceDetail = (interfaceId: string) => {
  // 这里可以跳转到接口详情页面或打开详情弹窗
  window.open(`/interface-catalog?id=${interfaceId}`, '_blank')
}

// 加载申请历史
const loadApplications = async () => {
  try {
    applicationsLoading.value = true
    
    const params: ApplicationListRequest = {
      page: applicationPagination.current,
      size: applicationPagination.size,
      userId: userStore.userInfo?.userId
    }
    
    if (applicationFilters.status) {
      params.status = applicationFilters.status
    }
    
    if (applicationFilters.dateRange && applicationFilters.dateRange.length === 2) {
      params.startDate = applicationFilters.dateRange[0] + ' 00:00:00'
      params.endDate = applicationFilters.dateRange[1] + ' 23:59:59'
    }
    
    const response = await getApplicationList(params)
    
    // 如果有关键词过滤，在前端进行过滤
    let filteredRecords = response.records
    if (applicationFilters.keyword) {
      filteredRecords = response.records.filter(app => 
        app.reason.toLowerCase().includes(applicationFilters.keyword.toLowerCase()) ||
        app.businessScenario?.toLowerCase().includes(applicationFilters.keyword.toLowerCase())
      )
    }
    
    applicationList.value = filteredRecords
    applicationPagination.total = response.total
    applicationPagination.current = response.current
    applicationPagination.size = response.size
    
  } catch {
    ElMessage.error('获取申请历史失败')
  } finally {
    applicationsLoading.value = false
  }
}

// 查看申请详情
const viewApplicationDetail = (application: SubscriptionApplication) => {
  ElMessageBox.alert(
    `<div style="text-align: left;">
      <p><strong>申请ID：</strong>${application.id}</p>
      <p><strong>申请接口：</strong>${application.interfaceIds.join(', ')}</p>
      <p><strong>申请原因：</strong>${application.reason}</p>
      <p><strong>业务场景：</strong>${application.businessScenario || '未填写'}</p>
      <p><strong>预估调用量：</strong>${application.estimatedCalls || '未填写'}</p>
      <p><strong>申请状态：</strong>${getApplicationStatusName(application.status)}</p>
      <p><strong>提交时间：</strong>${formatDateTime(application.submitTime)}</p>
      ${application.processTime ? `<p><strong>处理时间：</strong>${formatDateTime(application.processTime)}</p>` : ''}
      ${application.processComment ? `<p><strong>处理意见：</strong>${application.processComment}</p>` : ''}
    </div>`,
    '申请详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

// 撤回申请
const withdrawApplication = async (application: SubscriptionApplication) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤回申请"${application.id}"吗？撤回后该申请将被取消。`,
      '确认撤回申请',
      {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 这里需要调用撤回申请的API
    // await withdrawApplication(application.id)
    ElMessage.success('申请已撤回')
    
    // 重新加载申请列表
    await loadApplications()
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('撤回申请失败')
    }
  }
}

// 加载使用统计
const loadStatistics = async () => {
  try {
    statisticsLoading.value = true
    
    // 模拟统计数据，实际应该调用后端API
    // 这里可以根据日期范围获取统计数据
    const mockData = {
      totalCalls: 15420,
      successRate: '98.5%',
      avgResponseTime: '245ms',
      activeInterfaces: 12,
      trendData: [
        { date: '01-01', calls: 1200 },
        { date: '01-02', calls: 1350 },
        { date: '01-03', calls: 1100 },
        { date: '01-04', calls: 1450 },
        { date: '01-05', calls: 1600 },
        { date: '01-06', calls: 1380 },
        { date: '01-07', calls: 1520 }
      ],
      topInterfaces: [
        {
          interfaceId: 'api-001',
          interfaceName: '用户信息查询',
          interfacePath: '/api/v1/user/info',
          callCount: 5420,
          successRate: 99.2
        },
        {
          interfaceId: 'api-002',
          interfaceName: '订单数据查询',
          interfacePath: '/api/v1/order/list',
          callCount: 3280,
          successRate: 98.8
        },
        {
          interfaceId: 'api-003',
          interfaceName: '商品信息查询',
          interfacePath: '/api/v1/product/detail',
          callCount: 2150,
          successRate: 97.5
        }
      ],
      errorStats: [
        {
          errorCode: '400',
          errorMessage: '请求参数错误',
          count: 45,
          lastOccurred: new Date().toISOString()
        },
        {
          errorCode: '500',
          errorMessage: '服务器内部错误',
          count: 12,
          lastOccurred: new Date(Date.now() - 3600000).toISOString()
        }
      ]
    }
    
    // 更新统计数据
    Object.assign(statisticsData, mockData)
    
    // 实际实现时应该调用真实的API
    // const response = await getUsageStatistics({
    //   userId: userStore.userInfo?.userId,
    //   startDate: statisticsDateRange.value?.[0],
    //   endDate: statisticsDateRange.value?.[1]
    // })
    // Object.assign(statisticsData, response)
    
  } catch {
    ElMessage.error('获取使用统计失败')
  } finally {
    statisticsLoading.value = false
  }
}

// 获取排行标签类型
const getRankingTagType = (rank: number) => {
  if (rank === 1) return 'danger'
  if (rank === 2) return 'warning'
  if (rank === 3) return 'success'
  return 'info'
}

// 加载安全中心数据
const loadSecurityData = async () => {
  try {
    securityLoading.value = true
    
    // 根据当前标签页加载对应数据
    switch (activeSecurityTab.value) {
      case 'loginLogs':
        await loadLoginLogs()
        break
      case 'alerts':
        await loadAlerts()
        break
      case 'devices':
        await loadDevices()
        break
    }
    
  } catch {
    ElMessage.error('获取安全数据失败')
  } finally {
    securityLoading.value = false
  }
}

// 加载登录日志
const loadLoginLogs = async () => {
  try {
    // 模拟登录日志数据
    const mockLogs = [
      {
        id: '1',
        loginTime: new Date().toISOString(),
        ipAddress: '192.168.1.100',
        location: '北京市朝阳区',
        device: 'Windows 10 PC',
        browser: 'Chrome 120.0',
        status: 'success' as const
      },
      {
        id: '2',
        loginTime: new Date(Date.now() - 3600000).toISOString(),
        ipAddress: '192.168.1.101',
        location: '上海市浦东新区',
        device: 'iPhone 15 Pro',
        browser: 'Safari 17.0',
        status: 'success' as const
      },
      {
        id: '3',
        loginTime: new Date(Date.now() - 7200000).toISOString(),
        ipAddress: '10.0.0.1',
        location: '广州市天河区',
        device: 'MacBook Pro',
        browser: 'Chrome 119.0',
        status: 'failed' as const
      }
    ]
    
    securityData.loginLogs = mockLogs
    
  } catch {
    ElMessage.error('获取登录日志失败')
  }
}

// 加载异常告警
const loadAlerts = async () => {
  try {
    // 模拟告警数据
    const mockAlerts = [
      {
        id: '1',
        level: 'high' as const,
        message: '检测到异常登录尝试',
        description: '在短时间内检测到多次登录失败，可能存在暴力破解风险',
        ipAddress: '203.0.113.1',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        status: 'pending' as const,
        createTime: new Date(Date.now() - 1800000).toISOString()
      },
      {
        id: '2',
        level: 'medium' as const,
        message: '新设备登录',
        description: '检测到从未见过的设备登录您的账户',
        ipAddress: '198.51.100.1',
        userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)',
        status: 'resolved' as const,
        createTime: new Date(Date.now() - 86400000).toISOString()
      }
    ]
    
    securityData.alerts = mockAlerts
    
  } catch {
    ElMessage.error('获取异常告警失败')
  }
}

// 加载设备列表
const loadDevices = async () => {
  try {
    // 模拟设备数据
    const mockDevices = [
      {
        id: '1',
        deviceName: 'Windows PC - Chrome',
        deviceType: 'desktop' as const,
        browser: 'Chrome 120.0',
        os: 'Windows 10',
        location: '北京市朝阳区',
        isCurrent: true,
        lastActiveTime: new Date().toISOString()
      },
      {
        id: '2',
        deviceName: 'iPhone 15 Pro - Safari',
        deviceType: 'mobile' as const,
        browser: 'Safari 17.0',
        os: 'iOS 17.0',
        location: '上海市浦东新区',
        isCurrent: false,
        lastActiveTime: new Date(Date.now() - 3600000).toISOString()
      },
      {
        id: '3',
        deviceName: 'MacBook Pro - Chrome',
        deviceType: 'desktop' as const,
        browser: 'Chrome 119.0',
        os: 'macOS 14.0',
        location: '深圳市南山区',
        isCurrent: false,
        lastActiveTime: new Date(Date.now() - 86400000).toISOString()
      }
    ]
    
    securityData.devices = mockDevices
    
  } catch {
    ElMessage.error('获取设备列表失败')
  }
}

// 处理告警
const resolveAlert = async (alertId: string) => {
  try {
    // 这里应该调用后端API
    const alert = securityData.alerts.find(a => a.id === alertId)
    if (alert) {
      alert.status = 'resolved'
      ElMessage.success('告警已标记为已处理')
    }
  } catch {
    ElMessage.error('处理告警失败')
  }
}

// 忽略告警
const ignoreAlert = async (alertId: string) => {
  try {
    // 这里应该调用后端API
    const alert = securityData.alerts.find(a => a.id === alertId)
    if (alert) {
      alert.status = 'ignored'
      ElMessage.success('告警已忽略')
    }
  } catch {
    ElMessage.error('忽略告警失败')
  }
}

// 移除设备
const removeDevice = async (deviceId: string) => {
  try {
    await ElMessageBox.confirm(
      '确定要移除该设备吗？移除后该设备将无法继续访问您的账户。',
      '确认移除设备',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 这里应该调用后端API
    const index = securityData.devices.findIndex(d => d.id === deviceId)
    if (index > -1) {
      securityData.devices.splice(index, 1)
      ElMessage.success('设备已移除')
    }
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('移除设备失败')
    }
  }
}

// 获取告警级别标签类型
const getAlertLevelTagType = (level: string) => {
  const typeMap: Record<string, string> = {
    'high': 'danger',
    'medium': 'warning',
    'low': 'info'
  }
  return typeMap[level] || 'info'
}

// 获取告警级别名称
const getAlertLevelName = (level: string) => {
  const nameMap: Record<string, string> = {
    'high': '高危',
    'medium': '中危',
    'low': '低危'
  }
  return nameMap[level] || '未知'
}

// 联系技术支持
const contactSupport = () => {
  const subject = encodeURIComponent('API技术支持咨询')
  const body = encodeURIComponent(`您好，\n\n我在使用API时遇到了问题，希望得到技术支持。\n\n问题描述：\n\n\n用户信息：\n- 用户ID: ${userStore.userInfo?.userId}\n- AppId: ${apiKeyData.value?.appId}\n\n谢谢！`)
  window.open(`mailto:support@powertrading.com?subject=${subject}&body=${body}`)
}

// 打开在线文档
const openDocumentation = () => {
  window.open('https://docs.powertrading.com', '_blank')
}

// 切换密钥显示状态
const toggleSecretVisibility = () => {
  showSecret.value = !showSecret.value
}

// 复制到剪贴板
const copyToClipboard = async (text: string | undefined, label: string) => {
  if (!text) {
    ElMessage.warning(`${label}为空，无法复制`)
    return
  }
  
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`${label}已复制到剪贴板`)
  } catch (error) {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = text
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success(`${label}已复制到剪贴板`)
  }
}

// 掩码显示密钥
const maskSecret = (secret: string | undefined) => {
  if (!secret) return ''
  if (secret.length <= 8) return '****'
  return secret.substring(0, 4) + '****' + secret.substring(secret.length - 4)
}

// 格式化日期时间
const formatDateTime = (dateTime: string | undefined) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 获取角色标签类型
const getRoleTagType = (role: string | undefined) => {
  const typeMap: Record<string, string> = {
    'ADMIN': 'danger',
    'USER': 'primary'
  }
  return typeMap[role || ''] || 'info'
}

// 获取角色名称
const getRoleName = (role: string | undefined) => {
  const nameMap: Record<string, string> = {
    'ADMIN': '系统管理员',
    'USER': '普通用户'
  }
  return nameMap[role || ''] || '未知角色'
}

// 获取状态标签类型
const getStatusTagType = (status: string | undefined) => {
  const typeMap: Record<string, string> = {
    'ACTIVE': 'success',
    'PENDING': 'warning',
    'LOCKED': 'danger',
    'REJECTED': 'info'
  }
  return typeMap[status || ''] || 'info'
}

// 获取状态名称
const getStatusName = (status: string | undefined) => {
  const nameMap: Record<string, string> = {
    'ACTIVE': '正常',
    'PENDING': '待审核',
    'LOCKED': '已锁定',
    'REJECTED': '已拒绝'
  }
  return nameMap[status || ''] || '未知状态'
}

// 获取API密钥状态标签类型
const getApiKeyStatusTagType = (status: string | undefined) => {
  const typeMap: Record<string, string> = {
    'ACTIVE': 'success',
    'INACTIVE': 'danger'
  }
  return typeMap[status || ''] || 'info'
}

// 获取API密钥状态名称
const getApiKeyStatusName = (status: string | undefined) => {
  const nameMap: Record<string, string> = {
    'ACTIVE': '启用',
    'INACTIVE': '禁用'
  }
  return nameMap[status || ''] || '未知状态'
}

// 获取权限名称
const getPermissionName = (permission: string) => {
  const nameMap: Record<string, string> = {
    'user:read': '查看用户',
    'user:write': '编辑用户',
    'user:delete': '删除用户',
    'user:read:self': '查看个人信息',
    'user:write:self': '编辑个人信息',
    'interface:read': '查看接口',
    'interface:write': '编辑接口',
    'interface:delete': '删除接口',
    'interface:call': '调用接口',
    'datasource:read': '查看数据源',
    'datasource:write': '编辑数据源',
    'datasource:delete': '删除数据源',
    'system:read': '查看系统信息',
    'system:write': '系统配置管理'
  }
  return nameMap[permission] || permission
}

// 获取订阅状态标签类型
const getSubscriptionStatusTagType = (status: string | undefined) => {
  const typeMap: Record<string, string> = {
    'active': 'success',
    'inactive': 'info',
    'expired': 'warning',
    'cancelled': 'danger'
  }
  return typeMap[status || ''] || 'info'
}

// 获取订阅状态名称
const getSubscriptionStatusName = (status: string | undefined) => {
  const nameMap: Record<string, string> = {
    'active': '正常',
    'inactive': '未激活',
    'expired': '已过期',
    'cancelled': '已取消'
  }
  return nameMap[status || ''] || '未知状态'
}

// 获取申请状态标签类型
const getApplicationStatusTagType = (status: string | undefined) => {
  const typeMap: Record<string, string> = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  }
  return typeMap[status || ''] || 'info'
}

// 获取申请状态名称
const getApplicationStatusName = (status: string | undefined) => {
  const nameMap: Record<string, string> = {
    'pending': '待审批',
    'approved': '已通过',
    'rejected': '已拒绝'
  }
  return nameMap[status || ''] || '未知状态'
}

// 处理来自接口目录的跳转事件
const handleSwitchToApplications = () => {
  activeMenu.value = 'applications'
  loadApplications()
}

// 初始化
onMounted(() => {
  loadUserProfile()
  
  // 监听来自接口目录的跳转事件
  window.addEventListener('switchToApplications', handleSwitchToApplications)
})

// 清理事件监听器
onUnmounted(() => {
  window.removeEventListener('switchToApplications', handleSwitchToApplications)
})
</script>

<style scoped>
.user-center {
  height: 100vh;
  background-color: #f5f7fa;
}

.user-aside {
  background-color: white;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.user-menu {
  border-right: none;
  height: 100%;
}

.user-menu .el-menu-item {
  height: 56px;
  line-height: 56px;
  font-size: 16px;
}

.user-main {
  padding: 20px;
}

.content-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.security-alert {
  margin-bottom: 20px;
}

.password-form-container {
  max-width: 600px;
}

.password-form {
  margin-top: 20px;
}

.api-key-section {
  margin-top: 20px;
}

.api-key-info {
  margin-top: 20px;
}

.key-display {
  width: 100%;
}

.key-input {
  width: 100%;
}

.permissions-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.permissions-section {
  margin-top: 20px;
}

.permissions-list h4 {
  color: #303133;
  font-size: 16px;
  margin-bottom: 15px;
}

.permission-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.permission-item {
  margin-bottom: 8px;
}

.no-permissions {
  color: #909399;
  font-style: italic;
}

.subscriptions-section {
  margin-top: 20px;
}

.subscription-filters {
  margin-bottom: 20px;
}

.subscription-table {
  margin-top: 20px;
}

.interface-info {
  display: flex;
  flex-direction: column;
}

.interface-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.interface-path {
  font-size: 12px;
  color: #909399;
  font-family: 'Courier New', monospace;
}

.empty-state {
  margin: 40px 0;
  text-align: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.applications-section {
  margin-top: 20px;
}

.application-filters {
  margin-bottom: 20px;
}

.application-table {
  margin-top: 20px;
}

.interface-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.interface-tag {
  margin-bottom: 4px;
}

.reason-text {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.statistics-section {
  margin-top: 20px;
}

.stats-overview {
  margin-bottom: 20px;
}

.stats-card {
  text-align: center;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stats-item {
  padding: 10px;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 8px;
}

.stats-label {
  font-size: 14px;
  color: #666;
}

.chart-card,
.ranking-card,
.error-card {
  margin-top: 20px;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-chart,
.empty-ranking,
.empty-errors {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.simple-chart {
  display: flex;
  align-items: end;
  justify-content: space-around;
  height: 200px;
  width: 100%;
  padding: 20px;
}

.chart-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar-value {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.bar {
  width: 30px;
  background: linear-gradient(to top, #1890ff, #40a9ff);
  border-radius: 2px;
  min-height: 10px;
  margin-bottom: 5px;
}

.bar-label {
  font-size: 12px;
  color: #666;
  transform: rotate(-45deg);
  margin-top: 10px;
}

.ranking-list {
  max-height: 400px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-number {
  width: 50px;
  text-align: center;
}

.ranking-info {
  flex: 1;
  margin-left: 15px;
}

.ranking-info .interface-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.ranking-info .interface-path {
  font-size: 12px;
  color: #909399;
  font-family: 'Courier New', monospace;
}

.ranking-stats {
  text-align: right;
  min-width: 120px;
}

.call-count {
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 4px;
}

.success-rate {
  font-size: 12px;
  color: #67c23a;
}

.security-section {
  margin-top: 20px;
}

.security-tabs {
  margin-top: 10px;
}

.logs-filters,
.alerts-filters {
  margin-bottom: 20px;
}

.logs-table {
  margin-top: 15px;
}

.alerts-list {
  max-height: 600px;
  overflow-y: auto;
}

.alert-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alert-message {
  font-weight: 600;
  color: #303133;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

.alert-content {
  margin-bottom: 12px;
}

.alert-description {
  color: #606266;
  margin-bottom: 8px;
}

.alert-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.alert-actions {
  display: flex;
  gap: 8px;
}

.devices-list {
  margin-top: 20px;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fff;
}

.device-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.device-icon {
  margin-right: 16px;
  color: #606266;
}

.device-details {
  flex: 1;
}

.device-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.device-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.device-location {
  color: #909399;
}

.device-time {
  font-size: 12px;
  color: #909399;
}

.current-device {
  color: #67c23a;
  font-weight: 600;
}

.device-actions {
  margin-left: 16px;
}

.help-section {
  margin-top: 20px;
}

.help-tabs {
  margin-top: 10px;
}

.guide-collapse,
.faq-collapse {
  margin-top: 20px;
}

.guide-content {
  padding: 16px 0;
}

.guide-content h4 {
  color: #303133;
  margin-bottom: 12px;
  font-size: 16px;
}

.guide-content p {
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.6;
}

.guide-content ul {
  margin: 12px 0;
  padding-left: 20px;
}

.guide-content li {
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.6;
}

.sdk-doc-section {
  margin-top: 20px;
}

.sdk-alert {
  margin-bottom: 24px;
}

.sdk-languages {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.language-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  transition: all 0.3s;
}

.language-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.language-icon {
  margin-right: 16px;
  color: #1890ff;
}

.language-info h4 {
  color: #303133;
  margin-bottom: 8px;
  font-size: 16px;
}

.language-info p {
  color: #606266;
  margin-bottom: 8px;
  font-size: 14px;
}

.faq-section {
  margin-top: 20px;
}

.faq-answer {
  color: #606266;
  line-height: 1.6;
  padding: 8px 0;
}

.support-section {
  margin-top: 20px;
}

.support-card {
  height: 100%;
}

.support-content {
  padding: 16px 0;
}

.support-content p {
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.6;
}

.support-content ul {
  margin: 12px 0;
  padding-left: 20px;
}

.support-content li {
  margin-bottom: 8px;
}

.support-content a {
  color: #1890ff;
  text-decoration: none;
}

.support-content a:hover {
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-center {
    height: auto;
  }
  
  .user-aside {
    width: 200px !important;
  }
  
  .user-main {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .password-form-container {
    max-width: 100%;
  }
  
  /* 订阅管理响应式 */
  .subscription-filters .el-row {
    flex-direction: column;
  }
  
  .subscription-filters .el-col {
    width: 100% !important;
    margin-bottom: 10px;
  }
  
  .subscription-table {
    font-size: 12px;
  }
  
  .interface-info {
    min-width: 120px;
  }
  
  /* 申请历史响应式 */
  .application-filters .el-row {
    flex-direction: column;
  }
  
  .application-filters .el-col {
    width: 100% !important;
    margin-bottom: 10px;
  }
  
  .application-table {
    font-size: 12px;
  }
  
  .interface-list {
    flex-direction: column;
    gap: 2px;
  }
  
  .reason-text {
    max-width: 100px;
  }
  
  /* 使用统计响应式 */
  .stats-overview .el-row {
    flex-direction: column;
  }
  
  .stats-overview .el-col {
    width: 100% !important;
    margin-bottom: 10px;
  }
  
  .stats-card {
    margin-bottom: 10px;
  }
  
  .simple-chart {
    padding: 10px;
    height: 150px;
  }
  
  .chart-bar {
    margin: 0 2px;
  }
  
  .bar {
    width: 20px;
  }
  
  .ranking-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .ranking-stats {
    text-align: left;
    min-width: auto;
  }
  
  /* 安全中心响应式 */
  .logs-filters .el-row,
  .alerts-filters .el-row {
    flex-direction: column;
  }
  
  .logs-filters .el-col,
  .alerts-filters .el-col {
    width: 100% !important;
    margin-bottom: 10px;
  }
  
  .logs-table {
    font-size: 12px;
  }
  
  .alert-item {
    padding: 12px;
  }
  
  .alert-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .alert-details {
    font-size: 11px;
  }
  
  .device-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .device-info {
    width: 100%;
  }
  
  .device-actions {
    margin-left: 0;
    width: 100%;
  }
  
  .device-meta {
    flex-direction: column;
    gap: 4px;
  }
  
  /* 使用帮助响应式 */
  .sdk-languages {
    grid-template-columns: 1fr;
  }
  
  .language-card {
    padding: 16px;
  }
  
  .support-section .el-row {
    flex-direction: column;
  }
  
  .support-section .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }
  
  .guide-content {
    padding: 12px 0;
  }
  
  .guide-content h4 {
    font-size: 14px;
  }
  
  .guide-content p,
  .guide-content li {
    font-size: 13px;
  }
}

/* 平板设备响应式 */
@media (max-width: 1024px) and (min-width: 769px) {
  .user-aside {
    width: 220px !important;
  }
  
  .stats-overview .el-col {
    width: 50% !important;
  }
  
  .sdk-languages {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .subscription-filters .el-col:first-child,
  .application-filters .el-col:first-child {
    width: 33.33% !important;
  }
  
  .subscription-filters .el-col:nth-child(2),
  .application-filters .el-col:nth-child(2) {
    width: 33.33% !important;
  }
  
  .subscription-filters .el-col:last-child,
  .application-filters .el-col:last-child {
    width: 33.33% !important;
  }
}

/* 小屏幕设备优化 */
@media (max-width: 480px) {
  .user-center {
    font-size: 14px;
  }
  
  .user-aside {
    width: 180px !important;
  }
  
  .user-menu .el-menu-item {
    height: 48px;
    line-height: 48px;
    font-size: 14px;
    padding: 0 12px;
  }
  
  .user-main {
    padding: 8px;
  }
  
  .content-card {
    margin-bottom: 16px;
  }
  
  .card-header h3 {
    font-size: 16px;
  }
  
  .stats-value {
    font-size: 24px;
  }
  
  .stats-label {
    font-size: 12px;
  }
  
  .simple-chart {
    height: 120px;
    padding: 8px;
  }
  
  .bar {
    width: 16px;
  }
  
  .bar-value,
  .bar-label {
    font-size: 10px;
  }
  
  .pagination {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .el-pagination {
    font-size: 12px;
  }
  
  .alert-item,
  .device-item {
    padding: 10px;
  }
  
  .language-card {
    padding: 12px;
  }
  
  .language-info h4 {
    font-size: 14px;
  }
  
  .language-info p {
    font-size: 12px;
  }
}
</style>