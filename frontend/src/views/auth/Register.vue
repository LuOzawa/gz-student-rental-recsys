<template>
  <Auth 
    :formData="registerForm" 
    :rules="rules" 
    :loading="loading"
    submitText="注册"
    title="创建账号"
    subtitle="填写以下信息完成注册"
    @submit="handleSubmit"
  >
    <template #form-items>
      <el-form-item prop="username">
        <el-input 
          v-model="registerForm.username"
          :prefix-icon="User"
          placeholder="请输入用户名">
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input 
          v-model="registerForm.password"
          :prefix-icon="Lock"
          type="password"
          placeholder="请输入密码">
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input 
          v-model="registerForm.confirmPassword"
          :prefix-icon="Lock"
          type="password"
          placeholder="请确认密码">
        </el-input>
      </el-form-item>
      
      <div class="form-row">
        <el-form-item prop="email" class="form-col">
          <el-input 
            v-model="registerForm.email"
            :prefix-icon="Message"
            placeholder="请输入邮箱">
          </el-input>
        </el-form-item>
        <el-form-item prop="phone" class="form-col">
          <el-input 
            v-model="registerForm.phone"
            :prefix-icon="Phone"
            placeholder="请输入手机号">
          </el-input>
        </el-form-item>
      </div>
      
      <el-form-item prop="name">
        <el-input
          v-model="registerForm.name"
          :prefix-icon="UserFilled"
          placeholder="请输入姓名">
        </el-input>
      </el-form-item>

      <el-form-item prop="roleCode">
        <template #label>
          <span class="role-label">注册类型</span>
        </template>
        <el-radio-group v-model="registerForm.roleCode" class="role-radio-group">
          <el-radio value="TENANT" size="large">
            <span class="role-option">
              <el-icon><User /></el-icon>
              <span class="role-text">
                <strong>租客</strong>
                <small>寻找房源，预约看房</small>
              </span>
            </span>
          </el-radio>
          <el-radio value="LANDLORD" size="large">
            <span class="role-option">
              <el-icon><House /></el-icon>
              <span class="role-text">
                <strong>房东</strong>
                <small>发布房源，管理出租</small>
              </span>
            </span>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 租客偏好信息 -->
      <template v-if="registerForm.roleCode === 'TENANT'">
        <el-divider content-position="left">
          <span class="preference-title">偏好设置（可选）</span>
        </el-divider>

        <el-form-item prop="campus">
          <el-select
            v-model="registerForm.campus"
            placeholder="请选择您的校区"
            clearable
            style="width: 100%"
          >
            <el-option label="大学城校区" value="大学城校区" />
            <el-option label="南校区" value="南校区" />
            <el-option label="北校区" value="北校区" />
            <el-option label="东校区" value="东校区" />
          </el-select>
        </el-form-item>

        <div class="form-row">
          <el-form-item prop="major" class="form-col">
            <el-input
              v-model="registerForm.major"
              placeholder="请输入专业（可选）">
            </el-input>
          </el-form-item>
          <el-form-item prop="enrollmentYear" class="form-col">
            <el-date-picker
              v-model="registerForm.enrollmentYear"
              type="year"
              placeholder="选择入学年份（可选）"
              value-format="YYYY"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item prop="rentBudget">
          <el-input-number
            v-model="registerForm.rentBudget"
            :min="500"
            :max="10000"
            :step="100"
            placeholder="租房预算（元/月，可选）"
            style="width: 100%"
          />
          <div class="form-tip">设置预算后，我们会为您推荐更合适的房源</div>
        </el-form-item>
      </template>
    </template>

    <template #auth-links>
      已有账号？<router-link to="/login">立即登录</router-link>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Phone, UserFilled, House } from '@element-plus/icons-vue'
import request from '@/utils/request'
import Auth from './Auth.vue'

const router = useRouter()
const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  name: '',
  roleCode: 'TENANT', // 默认注册为普通用户
  // 用户偏好字段
  campus: '',
  major: '',
  enrollmentYear: null,
  rentBudget: null
})

const validatePass2 = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  const emailRegex = /^[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!emailRegex.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3到50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  name: [
    { required: false }
  ],
  roleCode: [
    { required: true, message: '请选择注册类型', trigger: 'change' }
  ]
}

const handleSubmit = (form) => {
  formRef.value = form.value
  registerFormRef.value = form.value
  handleRegister()
}

const handleRegister = () => {
  registerFormRef.value.validate(async valid => {
    if (valid) {
      loading.value = true
      try {
        const { confirmPassword, ...registerData } = registerForm
        await request.post("/user/add", registerData, {
          successMsg: "注册成功",
          showDefaultMsg: true,
          onSuccess: () => {
            router.push('/login')
          }
        })
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.form-row {
  display: flex;
  gap: 15px;

  .form-col {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}

.role-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 8px;
  display: inline-block;
}

.role-radio-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;

  :deep(.el-radio) {
    width: 100%;
    height: auto;
    margin-right: 0;
    margin-bottom: 0;
    padding: 14px 16px;
    border: 2px solid #dcdfe6;
    border-radius: 8px;
    transition: all 0.3s;
    white-space: normal;
    display: flex;
    align-items: center;

    .el-radio__input {
      align-self: center;
    }

    .el-radio__label {
      padding-left: 8px;
      flex: 1;
    }

    &:hover {
      border-color: #409eff;
      background-color: #ecf5ff;
    }

    &.is-checked {
      border-color: #409eff;
      background-color: #ecf5ff;
    }
  }

  .role-option {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;

    .el-icon {
      font-size: 24px;
      color: #409eff;
      flex-shrink: 0;
    }

    .role-text {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      flex: 1;
      min-width: 0;

      strong {
        font-size: 15px;
        color: #303133;
        margin-bottom: 2px;
        line-height: 1.4;
      }

      small {
        font-size: 12px;
        color: #909399;
        line-height: 1.4;
      }
    }
  }
}

.preference-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.form-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
}
</style> 