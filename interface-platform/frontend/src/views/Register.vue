<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>企业用户注册</h2>
        <p>请填写企业信息完成注册申请</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="120px"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <!-- 企业信息 -->
        <div class="form-section">
          <h3>企业信息</h3>
          
          <el-form-item label="企业名称" prop="companyName">
            <el-input
              v-model="registerForm.companyName"
              placeholder="请输入企业全称"
              :maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="统一社会信用代码" prop="creditCode">
            <el-input
              v-model="registerForm.creditCode"
              placeholder="请输入18位统一社会信用代码"
              :maxlength="18"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 账户信息 -->
        <div class="form-section">
          <h3>账户信息</h3>
          
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名，4-20位字符"
              :maxlength="20"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码，8-20位，包含大小写字母、数字和特殊字符"
              show-password
              :maxlength="20"
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
              :maxlength="20"
            />
          </el-form-item>
        </div>

        <!-- 联系人信息 -->
        <div class="form-section">
          <h3>联系人信息</h3>
          
          <el-form-item label="联系人姓名" prop="contactName">
            <el-input
              v-model="registerForm.contactName"
              placeholder="请输入联系人姓名"
              :maxlength="20"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="手机号码" prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入11位手机号码"
              :maxlength="11"
            />
          </el-form-item>

          <el-form-item label="邮箱地址" prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱地址"
              :maxlength="100"
            />
          </el-form-item>

          <el-form-item label="部门" prop="department">
            <el-input
              v-model="registerForm.department"
              placeholder="请输入所在部门（可选）"
              :maxlength="50"
            />
          </el-form-item>

          <el-form-item label="职位" prop="position">
            <el-input
              v-model="registerForm.position"
              placeholder="请输入职位（可选）"
              :maxlength="50"
            />
          </el-form-item>
        </div>

        <!-- 服务协议 -->
        <el-form-item prop="agreement">
          <el-checkbox v-model="registerForm.agreement">
            我已阅读并同意
            <el-link type="primary" @click="showAgreement = true">《用户服务协议》</el-link>
            和
            <el-link type="primary" @click="showPrivacy = true">《隐私政策》</el-link>
          </el-checkbox>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleRegister"
            class="register-btn"
          >
            提交注册申请
          </el-button>
          <el-button
            size="large"
            @click="handleReset"
            class="reset-btn"
          >
            重置表单
          </el-button>
        </el-form-item>

        <!-- 登录链接 -->
        <div class="login-link">
          已有账户？
          <router-link to="/login" class="link">立即登录</router-link>
        </div>
      </el-form>
    </div>

    <!-- 注册成功对话框 -->
    <el-dialog
      v-model="showSuccessDialog"
      title="注册申请提交成功"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="success-content">
        <el-icon class="success-icon" color="#67C23A" :size="60">
          <CircleCheck />
        </el-icon>
        <h3>注册申请已提交</h3>
        <p>您的注册申请已成功提交，我们将在1-2个工作日内完成审核。</p>
        <p>审核结果将通过邮件或短信通知您，请保持联系方式畅通。</p>
        <div class="user-info">
          <p><strong>用户ID：</strong>{{ registeredUserId }}</p>
          <p><strong>申请时间：</strong>{{ registeredTime }}</p>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="goToLogin">前往登录</el-button>
      </template>
    </el-dialog>

    <!-- 服务协议对话框 -->
    <el-dialog v-model="showAgreement" title="用户服务协议" width="70%" class="agreement-dialog">
      <div class="agreement-content">
        <h4>1. 服务条款的确认和接纳</h4>
        <p>本协议是您与接口平台之间关于使用接口平台服务的法律协议。</p>
        
        <h4>2. 服务内容</h4>
        <p>接口平台为企业用户提供数据接口服务，包括但不限于接口调用、数据查询等功能。</p>
        
        <h4>3. 用户义务</h4>
        <p>用户应当遵守相关法律法规，不得利用本服务从事违法违规活动。</p>
        
        <h4>4. 隐私保护</h4>
        <p>我们承诺保护用户隐私，不会泄露用户的个人信息和企业数据。</p>
        
        <h4>5. 服务变更</h4>
        <p>平台有权根据业务需要调整服务内容，会提前通知用户。</p>
      </div>
    </el-dialog>

    <!-- 隐私政策对话框 -->
    <el-dialog v-model="showPrivacy" title="隐私政策" width="70%" class="agreement-dialog">
      <div class="agreement-content">
        <h4>1. 信息收集</h4>
        <p>我们收集您提供的企业信息和联系信息，用于账户管理和服务提供。</p>
        
        <h4>2. 信息使用</h4>
        <p>收集的信息仅用于提供服务、账户管理和必要的沟通。</p>
        
        <h4>3. 信息保护</h4>
        <p>我们采用行业标准的安全措施保护您的信息安全。</p>
        
        <h4>4. 信息共享</h4>
        <p>除法律要求外，我们不会与第三方共享您的个人信息。</p>
        
        <h4>5. 权利行使</h4>
        <p>您有权查询、更正或删除您的个人信息。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { registerUser } from '@/api/user'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const showSuccessDialog = ref(false)
const showAgreement = ref(false)
const showPrivacy = ref(false)
const registeredUserId = ref('')
const registeredTime = ref('')

// 注册表单数据
const registerForm = reactive({
  companyName: '',
  creditCode: '',
  username: '',
  password: '',
  confirmPassword: '',
  contactName: '',
  phone: '',
  email: '',
  department: '',
  position: '',
  agreement: false
})

// 表单验证规则
const registerRules: FormRules = {
  companyName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' },
    { min: 2, max: 100, message: '企业名称长度为2-100个字符', trigger: 'blur' }
  ],
  creditCode: [
    { required: true, message: '请输入统一社会信用代码', trigger: 'blur' },
    { pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/, message: '请输入正确的18位统一社会信用代码', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度为8-20个字符', trigger: 'blur' },
    { 
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/, 
      message: '密码必须包含大小写字母、数字和特殊字符', 
      trigger: 'blur' 
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
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
  ],
  agreement: [
    { 
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请阅读并同意服务协议和隐私政策'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    const valid = await registerFormRef.value.validate()
    if (!valid) return
    
    loading.value = true
    
    // 调用注册API
    const response = await registerUser({
      username: registerForm.username,
      password: registerForm.password,
      companyName: registerForm.companyName,
      creditCode: registerForm.creditCode,
      contactName: registerForm.contactName,
      phone: registerForm.phone,
      email: registerForm.email,
      department: registerForm.department || undefined,
      position: registerForm.position || undefined
    })
    
    // 注册成功
    registeredUserId.value = response.userId
    registeredTime.value = response.createTime
    showSuccessDialog.value = true
    
  } catch {
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 重置表单
const handleReset = () => {
  ElMessageBox.confirm('确定要重置表单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    registerFormRef.value?.resetFields()
    ElMessage.success('表单已重置')
  }).catch(() => {})
}

// 前往登录
const goToLogin = () => {
  showSuccessDialog.value = false
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.register-header h2 {
  color: #303133;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 10px;
}

.register-header p {
  color: #909399;
  font-size: 16px;
}

.register-form {
  max-width: 100%;
}

.form-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.form-section h3 {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  margin-top: 0;
}

.register-btn {
  width: 200px;
  height: 44px;
  font-size: 16px;
  margin-right: 20px;
}

.reset-btn {
  width: 120px;
  height: 44px;
  font-size: 16px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #909399;
}

.login-link .link {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.login-link .link:hover {
  text-decoration: underline;
}

.success-content {
  text-align: center;
  padding: 20px;
}

.success-icon {
  margin-bottom: 20px;
}

.success-content h3 {
  color: #303133;
  font-size: 20px;
  margin-bottom: 15px;
}

.success-content p {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.user-info {
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  padding: 15px;
  margin-top: 20px;
  text-align: left;
}

.user-info p {
  margin: 5px 0;
  color: #303133;
}

.agreement-dialog .agreement-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 0 10px;
}

.agreement-content h4 {
  color: #303133;
  font-size: 16px;
  margin: 20px 0 10px 0;
}

.agreement-content p {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 15px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-card {
    padding: 20px;
    margin: 10px;
  }
  
  .register-header h2 {
    font-size: 24px;
  }
  
  .form-section {
    padding: 15px;
  }
  
  .register-btn,
  .reset-btn {
    width: 100%;
    margin-bottom: 10px;
    margin-right: 0;
  }
}
</style>