<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <img src="@/assets/logo.svg" alt="Logo" class="logo-img" />
        </div>
        <h2>接口平台登录</h2>
        <p>欢迎使用企业数据接口服务平台</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名/手机号/邮箱"
            size="large"
            :prefix-icon="User"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 验证码（可选） -->
        <el-form-item v-if="showCaptcha" prop="captcha">
          <div class="captcha-container">
            <el-input
              v-model="loginForm.captcha"
              placeholder="请输入验证码"
              size="large"
              :prefix-icon="Picture"
              clearable
              @keyup.enter="handleLogin"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img :src="captchaUrl" alt="验证码" />
              <div class="refresh-hint">点击刷新</div>
            </div>
          </div>
        </el-form-item>

        <!-- 记住登录状态 -->
        <el-form-item>
          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住登录状态</el-checkbox>
            <el-link type="primary" @click="showForgotPassword = true">
              忘记密码？
            </el-link>
          </div>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loginLoading"
            @click="handleLogin"
            class="login-btn"
          >
            {{ loginLoading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        
        <!-- 临时登录绕过按钮 -->
        <el-form-item>
          <el-button
            type="success"
            class="login-btn"
            @click="handleTempLogin"
            style="margin-top: 10px;"
          >
            临时登录（演示模式）
          </el-button>
        </el-form-item>

        <!-- 注册链接 -->
        <div class="register-link">
          还没有账户？
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </el-form>

      <!-- 登录失败提示 -->
      <div v-if="loginError" class="error-message">
        <el-alert
          :title="loginError"
          type="error"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 登录帮助 -->
      <div class="login-help">
        <el-divider>登录帮助</el-divider>
        <div class="help-content">
          <p><strong>支持的登录方式：</strong></p>
          <ul>
            <li>用户名登录</li>
            <li>手机号登录</li>
            <li>邮箱登录</li>
          </ul>
          <p><strong>遇到问题？</strong></p>
          <p>请联系技术支持：400-123-4567</p>
        </div>
      </div>
    </div>

    <!-- 忘记密码对话框 -->
    <el-dialog
      v-model="showForgotPassword"
      title="找回密码"
      width="400px"
    >
      <el-form
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-width="80px"
      >
        <el-form-item label="账户" prop="account">
          <el-input
            v-model="forgotForm.account"
            placeholder="请输入用户名/手机号/邮箱"
          />
        </el-form-item>
        
        <el-form-item label="验证码" prop="verifyCode">
          <div class="verify-code-container">
            <el-input
              v-model="forgotForm.verifyCode"
              placeholder="请输入验证码"
            />
            <el-button
              :disabled="sendCodeDisabled"
              @click="sendVerifyCode"
              class="send-code-btn"
            >
              {{ sendCodeText }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showForgotPassword = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Picture } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
// import { loginUser } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const forgotFormRef = ref<FormInstance>()
const loginLoading = ref(false)
const resetLoading = ref(false)
const showCaptcha = ref(false) // 默认不显示验证码
const showForgotPassword = ref(false)
const rememberMe = ref(false)
const loginError = ref('')
const captchaUrl = ref('')
const sendCodeDisabled = ref(false)
const sendCodeCountdown = ref(0)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

// 忘记密码表单数据
const forgotForm = reactive({
  account: '',
  verifyCode: '',
  newPassword: ''
})

// 登录表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名/手机号/邮箱', trigger: 'blur' },
    { min: 2, max: 50, message: '账户长度为2-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 忘记密码表单验证规则
const forgotRules: FormRules = {
  account: [
    { required: true, message: '请输入账户', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度为8-20个字符', trigger: 'blur' },
    { 
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/, 
      message: '密码必须包含大小写字母、数字和特殊字符', 
      trigger: 'blur' 
    }
  ]
}

// 发送验证码按钮文本
const sendCodeText = computed(() => {
  return sendCodeCountdown.value > 0 ? `${sendCodeCountdown.value}s后重发` : '发送验证码'
})

// 验证验证码（临时实现）
const validateCaptcha = () => {
  // 临时返回true，跳过验证码验证
  return true
}

// 临时登录方法（绕过后端验证）
const handleTempLogin = async () => {
  try {
    // 创建临时用户数据
    const tempUserData = {
      token: 'temp-token-' + Date.now(),
      user: {
        userId: 'admin001',
        username: 'admin',
        realName: '系统管理员',
        email: 'admin@powertrading.com',
        phone: '13800000000',
        role: 'admin' as const,
        status: 'active' as const,
        companyName: '电力交易中心',
        department: '系统管理部',
        position: '系统管理员',
        createTime: new Date().toISOString(),
        lastLoginTime: new Date().toISOString()
      },
      permissions: ['*'], // 所有权限
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString() // 24小时后过期
    }
    
    // 直接设置用户登录状态
    userStore.setUserInfo(tempUserData.user)
    userStore.setToken(tempUserData.token)
    userStore.setPermissions(tempUserData.permissions)
    
    // 保存到localStorage
    localStorage.setItem('token', tempUserData.token)
    localStorage.setItem('userInfo', JSON.stringify(tempUserData.user))
    localStorage.setItem('permissions', JSON.stringify(tempUserData.permissions))
    
    ElMessage.success('临时登录成功！当前为演示模式')
    
    // 跳转到主页
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
    
  } catch (error) {
    console.error('临时登录失败:', error)
    ElMessage.error('临时登录失败，请重试')
  }
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    // 验证表单
    await loginFormRef.value.validate()
    
    // 验证验证码（如果显示）
    if (showCaptcha.value && !validateCaptcha()) {
      ElMessage.error('验证码错误')
      refreshCaptcha()
      return
    }
    
    loginLoading.value = true
    loginError.value = ''
    
    // 调用登录API
    const result = await userStore.login({
      account: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha
    })
    
    if (result.success) {
      ElMessage.success('登录成功')
      
      // 跳转到目标页面
      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    } else {
      throw new Error(result.message || '登录失败')
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    loginError.value = error.message || '登录失败，请稍后重试'
    
    // 暂时不显示验证码
    // showCaptcha.value = true
    // refreshCaptcha()
    
    ElMessage.error(loginError.value)
  } finally {
    loginLoading.value = false
  }
}

// 刷新验证码
const refreshCaptcha = () => {
  // 生成验证码URL（实际项目中应该调用后端接口）
  captchaUrl.value = `/api/captcha?t=${Date.now()}`
}

// 发送验证码
const sendVerifyCode = async () => {
  if (!forgotForm.account) {
    ElMessage.warning('请先输入账户')
    return
  }
  
  try {
    // TODO: 调用发送验证码API
    // await sendResetPasswordCode(forgotForm.account)
    
    ElMessage.success('验证码已发送，请查收')
    
    // 开始倒计时
    sendCodeDisabled.value = true
    sendCodeCountdown.value = 60
    
    const timer = setInterval(() => {
      sendCodeCountdown.value--
      if (sendCodeCountdown.value <= 0) {
        clearInterval(timer)
        sendCodeDisabled.value = false
      }
    }, 1000)
    
  } catch {
    ElMessage.error('发送验证码失败')
  }
}

// 重置密码
const handleResetPassword = async () => {
  if (!forgotFormRef.value) return
  
  try {
    const valid = await forgotFormRef.value.validate()
    if (!valid) return
    
    resetLoading.value = true
    
    // TODO: 调用重置密码API
    // await resetPassword(forgotForm)
    
    ElMessage.success('密码重置成功，请使用新密码登录')
    showForgotPassword.value = false
    
    // 清空忘记密码表单
    forgotFormRef.value.resetFields()
    
  } catch {
    ElMessage.error('密码重置失败')
  } finally {
    resetLoading.value = false
  }
}

// 初始化
onMounted(() => {
  // 检查是否记住登录状态
  const remembered = localStorage.getItem('rememberMe')
  if (remembered) {
    rememberMe.value = true
    // 可以在这里恢复用户名等信息
  }
  
  // 如果已经登录，直接跳转
  if (userStore.isLoggedIn) {
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  }
})
</script>

<style scoped>
/* 登录页面样式 */
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 50%, var(--primary-darker) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: float 20s ease-in-out infinite;
  pointer-events: none;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

.login-card {
  background: var(--bg-white);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-dark);
  padding: var(--spacing-xxxl);
  width: 100%;
  max-width: 480px;
  position: relative;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: var(--transition-base);
}

.login-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: var(--spacing-xxxl);
  position: relative;
}

.logo {
  margin-bottom: var(--spacing-xl);
  position: relative;
}

.logo-img {
  width: 72px;
  height: 72px;
  border-radius: var(--border-radius-circle);
  box-shadow: var(--shadow-base);
  transition: var(--transition-base);
}

.logo-img:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-light);
}

.login-header h2 {
  color: var(--text-primary);
  font-size: clamp(var(--font-size-large), 5vw, 2rem);
  font-weight: 700;
  margin-bottom: var(--spacing-md);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p {
  color: var(--text-secondary);
  font-size: var(--font-size-medium);
  font-weight: 400;
  margin: 0;
  opacity: 0.9;
}

.login-form {
  margin-top: var(--spacing-xl);
}

/* 表单项样式增强 */
.login-form .el-form-item {
  margin-bottom: var(--spacing-xl);
}

.login-form .el-input {
  height: 48px;
}

.login-form .el-input__wrapper {
  border-radius: var(--border-radius-base);
  border: 2px solid var(--border-lighter);
  transition: var(--transition-base);
  background: var(--bg-white);
}

.login-form .el-input__wrapper:hover {
  border-color: var(--primary-lighter);
  box-shadow: 0 0 0 2px var(--primary-extra-light);
}

.login-form .el-input__wrapper.is-focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--primary-extra-light);
}

.login-form .el-input__inner {
  font-size: var(--font-size-base);
  color: var(--text-primary);
  font-weight: 500;
}

.login-form .el-input__inner::placeholder {
  color: var(--text-placeholder);
  font-weight: 400;
}

/* 验证码容器 */
.captcha-container {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.captcha-image {
  position: relative;
  cursor: pointer;
  border: 2px solid var(--border-lighter);
  border-radius: var(--border-radius-base);
  overflow: hidden;
  transition: var(--transition-base);
  background: var(--bg-light);
}

.captcha-image:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-base);
}

.captcha-image img {
  width: 120px;
  height: 48px;
  display: block;
  object-fit: cover;
}

.refresh-hint {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  font-size: var(--font-size-small);
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition-fade);
  backdrop-filter: blur(2px);
}

.captcha-image:hover .refresh-hint {
  opacity: 1;
}

/* 登录选项 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin: var(--spacing-lg) 0;
}

.login-options .el-checkbox {
  font-weight: 500;
  color: var(--text-regular);
}

.login-options .el-link {
  font-weight: 500;
  transition: var(--transition-base);
}

.login-options .el-link:hover {
  transform: translateX(2px);
}

/* 按钮样式增强 */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: var(--font-size-medium);
  font-weight: 600;
  border-radius: var(--border-radius-base);
  transition: var(--transition-base);
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: var(--transition-base);
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-light);
}

.login-btn:active {
  transform: translateY(0);
}

/* 临时登录按钮特殊样式 */
.login-btn.el-button--success {
  background: linear-gradient(135deg, var(--success-color), #52c41a);
  border: none;
}

.login-btn.el-button--success:hover {
  background: linear-gradient(135deg, #52c41a, var(--success-color));
}

/* 注册链接 */
.register-link {
  text-align: center;
  margin-top: var(--spacing-xl);
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  padding: var(--spacing-md);
  border-radius: var(--border-radius-base);
  background: var(--bg-extra-light);
  transition: var(--transition-base);
}

.register-link:hover {
  background: var(--primary-extra-light);
}

.register-link .link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 600;
  transition: var(--transition-base);
}

.register-link .link:hover {
  color: var(--primary-light);
  text-decoration: underline;
  text-underline-offset: 4px;
}

/* 错误消息样式 */
.error-message {
  margin-top: var(--spacing-xl);
}

.error-message .el-alert {
  border-radius: var(--border-radius-base);
  border: 1px solid var(--danger-color);
  background: linear-gradient(135deg, #fee2e2 0%, #fef2f2 100%);
}

/* 登录帮助 */
.login-help {
  margin-top: var(--spacing-xxxl);
  padding: var(--spacing-xl);
  background: var(--bg-extra-light);
  border-radius: var(--border-radius-base);
  border: 1px solid var(--border-extra-light);
}

.login-help .el-divider {
  margin: 0 0 var(--spacing-lg) 0;
}

.login-help .el-divider__text {
  font-weight: 600;
  color: var(--text-primary);
}

.help-content {
  font-size: var(--font-size-small);
  color: var(--text-regular);
  line-height: var(--line-height-large);
}

.help-content p {
  margin: var(--spacing-sm) 0;
}

.help-content strong {
  color: var(--text-primary);
  font-weight: 600;
}

.help-content ul {
  margin: var(--spacing-md) 0;
  padding-left: var(--spacing-xl);
}

.help-content li {
  margin: var(--spacing-xs) 0;
  position: relative;
}

.help-content li::before {
  content: '•';
  color: var(--primary-color);
  font-weight: bold;
  position: absolute;
  left: -var(--spacing-lg);
}

/* 验证码发送容器 */
.verify-code-container {
  display: flex;
  gap: var(--spacing-md);
}

.verify-code-container .el-input {
  flex: 1;
}

.send-code-btn {
  white-space: nowrap;
  min-width: 120px;
  font-weight: 500;
}

/* 对话框样式增强 */
.el-dialog {
  border-radius: var(--border-radius-large);
  overflow: hidden;
}

.el-dialog__header {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--bg-extra-light) 100%);
  border-bottom: 1px solid var(--border-extra-light);
  padding: var(--spacing-xl);
}

.el-dialog__title {
  font-weight: 600;
  color: var(--text-primary);
}

.el-dialog__body {
  padding: var(--spacing-xl);
}

.el-dialog__footer {
  background: var(--bg-extra-light);
  border-top: 1px solid var(--border-extra-light);
  padding: var(--spacing-lg) var(--spacing-xl);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    padding: var(--spacing-lg);
  }
  
  .login-card {
    padding: var(--spacing-xl);
    margin: var(--spacing-md);
    max-width: 100%;
  }
  
  .login-header h2 {
    font-size: var(--font-size-large);
  }
  
  .login-header p {
    font-size: var(--font-size-base);
  }
  
  .logo-img {
    width: 60px;
    height: 60px;
  }
  
  .captcha-container {
    flex-direction: column;
    align-items: stretch;
    gap: var(--spacing-md);
  }
  
  .captcha-image {
    align-self: center;
  }
  
  .captcha-image img {
    width: 100px;
    height: 40px;
  }
  
  .login-options {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }
  
  .verify-code-container {
    flex-direction: column;
  }
  
  .send-code-btn {
    min-width: auto;
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: var(--spacing-md);
  }
  
  .login-card {
    padding: var(--spacing-lg);
    margin: 0;
  }
  
  .login-header {
    margin-bottom: var(--spacing-xl);
  }
  
  .login-header h2 {
    font-size: var(--font-size-medium);
  }
  
  .logo-img {
    width: 48px;
    height: 48px;
  }
  
  .login-btn {
    height: 44px;
    font-size: var(--font-size-base);
  }
  
  .help-content {
    font-size: var(--font-size-extra-small);
  }
}

/* 打印样式 */
@media print {
  .login-container {
    background: white;
  }
  
  .login-card {
    box-shadow: none;
    border: 1px solid #ddd;
  }
  
  .login-header h2 {
    color: black;
    -webkit-text-fill-color: black;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .login-card {
    border: 2px solid var(--text-primary);
  }
  
  .login-header h2 {
    -webkit-text-fill-color: var(--text-primary);
  }
  
  .login-form .el-input__wrapper {
    border-width: 2px;
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .login-container::before {
    animation: none;
  }
  
  .login-card,
  .logo-img,
  .login-btn,
  .captcha-image,
  .refresh-hint,
  .register-link,
  .login-options .el-link {
    transition: none;
  }
  
  .login-card:hover,
  .login-btn:hover {
    transform: none;
  }
}
</style>