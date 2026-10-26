<template>
  <div class="verification-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <span class="page-icon">✓</span>
      <h1 class="page-title">实名认证</h1>
      <p class="page-subtitle">完成认证后可预约看房和租赁房屋</p>
    </div>

    <!-- 认证卡片 -->
    <div class="verification-card card-styled">
      <!-- 认证状态 -->
      <div class="status-section">
        <div v-if="userInfo.verifyStatus === 0" class="status-box status-pending">
          <span class="status-icon">⚠️</span>
          <div class="status-content">
            <h4 class="status-title">未认证</h4>
            <p class="status-desc">为了保障交易安全，请完成实名认证</p>
          </div>
        </div>
        <div v-else-if="userInfo.verifyStatus === 1" class="status-box status-reviewing">
          <span class="status-icon">⏳</span>
          <div class="status-content">
            <h4 class="status-title">审核中</h4>
            <p class="status-desc">您的实名认证信息正在审核中，请耐心等待</p>
          </div>
        </div>
        <div v-else-if="userInfo.verifyStatus === 2" class="status-box status-verified">
          <span class="status-icon">✅</span>
          <div class="status-content">
            <h4 class="status-title">已认证</h4>
            <p class="status-desc">您已完成实名认证，可以正常使用所有功能</p>
          </div>
        </div>
        <div v-else-if="userInfo.verifyStatus === 3" class="status-box status-rejected">
          <span class="status-icon">❌</span>
          <div class="status-content">
            <h4 class="status-title">审核不通过</h4>
            <p class="status-desc">原因：{{ userInfo.verifyRejectReason || '请认真填写信息' }}，请重新提交</p>
          </div>
        </div>
      </div>

      <!-- 认证表单 -->
      <div v-if="userInfo.verifyStatus !== 2" class="form-section">
        <h3 class="form-title">📝 认证信息</h3>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="verification-form"
        >
          <el-form-item label="👤 真实姓名" prop="name">
            <el-input
              v-model="form.name"
              placeholder="请输入真实姓名"
              :disabled="userInfo.verifyStatus === 1"
              maxlength="50"
              size="large"
            />
          </el-form-item>

          <el-form-item label="🪪 身份证号" prop="idCard">
            <el-input
              v-model="form.idCard"
              placeholder="请输入18位身份证号"
              :disabled="userInfo.verifyStatus === 1"
              maxlength="18"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <button
              class="btn-submit"
              :disabled="userInfo.verifyStatus === 1 || submitting"
              @click="submitForm"
            >
              {{ submitting ? '提交中...' : userInfo.verifyStatus === 1 ? '审核中' : '🚀 提交认证' }}
            </button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 已认证信息 -->
      <div v-else class="verified-section">
        <h3 class="form-title">📋 认证信息</h3>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">👤 真实姓名</span>
            <span class="info-value">{{ userInfo.name }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">🪪 身份证号</span>
            <span class="info-value">{{ maskIdCard(userInfo.idCard) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">📅 认证时间</span>
            <span class="info-value">{{ userInfo.updateTime }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const formRef = ref()
const submitting = ref(false)
const userInfo = ref({})

const form = reactive({
  name: '',
  idCard: ''
})

const rules = {
  name: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度在2到50个字符', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/,
      message: '请输入正确的18位身份证号', trigger: 'blur' }
  ]
}

// 获取认证状态
const fetchVerificationStatus = async () => {
  try {
    const res = await request.get('/user/verify/status')
    userInfo.value = res || {}
    if (res && res.verifyStatus !== 2) {
      form.name = res.name || ''
      form.idCard = res.idCard || ''
    }
  } catch (error) {
    console.error('获取认证状态失败:', error)
  }
}

// 提交认证
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate((valid) => {
    if (valid) {
      submitVerification()
    }
  })
}

const submitVerification = async () => {
  submitting.value = true
  try {
    await request.post('/user/verify/submit', form)
    ElMessage.success('实名认证信息已提交，请等待管理员审核')
    fetchVerificationStatus()
  } catch (error) {
    console.error('提交认证失败:', error)
  } finally {
    submitting.value = false
  }
}

// 身份证号脱敏
const maskIdCard = (idCard) => {
  if (!idCard) return ''
  return idCard.replace(/^(.{6})(?:\d+)(.{4})$/, '$1********$2')
}

onMounted(() => {
  fetchVerificationStatus()
})
</script>

<style lang="scss" scoped>
.verification-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
  animation: fadeInUp 0.4s ease-out;
}

// 页面头部
.page-header {
  text-align: center;
  margin-bottom: 32px;

  .page-icon {
    font-size: 56px;
    display: block;
    margin-bottom: 12px;
    animation: float 3s ease-in-out infinite;
  }

  .page-title {
    font-family: 'Outfit', sans-serif;
    font-size: 28px;
    font-weight: 800;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
  }

  .page-subtitle {
    color: var(--color-text-tertiary);
    margin: 0;
  }
}

// 认证卡片
.verification-card {
  padding: 32px;
}

// 状态区域
.status-section {
  margin-bottom: 32px;

  .status-box {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 20px;
    border-radius: var(--radius-lg);
    border: 2px solid;

    &.status-pending {
      background: rgba(255, 230, 109, 0.15);
      border-color: var(--color-accent);

      .status-icon {
        font-size: 32px;
      }
    }

    &.status-reviewing {
      background: rgba(78, 205, 196, 0.15);
      border-color: var(--color-secondary);

      .status-icon {
        font-size: 32px;
        animation: pulse 2s ease-in-out infinite;
      }
    }

    &.status-verified {
      background: rgba(78, 205, 196, 0.15);
      border-color: var(--color-secondary);

      .status-icon {
        font-size: 32px;
      }
    }

    &.status-rejected {
      background: rgba(255, 107, 107, 0.15);
      border-color: var(--color-error);

      .status-icon {
        font-size: 32px;
      }
    }

    .status-content {
      flex: 1;

      .status-title {
        font-size: 18px;
        font-weight: 700;
        color: var(--color-text-primary);
        margin: 0 0 6px 0;
      }

      .status-desc {
        color: var(--color-text-secondary);
        margin: 0;
        line-height: 1.5;
      }
    }
  }
}

// 表单区域
.form-section {
  .form-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0 0 20px 0;
  }

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  :deep(.el-input__wrapper) {
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);

    &:focus,
    &.is-focus {
      box-shadow: 0 0 0 2px var(--color-primary-light);
    }
  }
}

.btn-submit {
  width: 100%;
  padding: 14px 24px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

// 已认证信息
.verified-section {
  .form-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0 0 20px 0;
  }

  .info-list {
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-lg);
    padding: 20px;

    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 0;
      border-bottom: 1px solid var(--color-border-light);

      &:last-child {
        border-bottom: none;
      }

      .info-label {
        font-weight: 600;
        color: var(--color-text-secondary);
      }

      .info-value {
        color: var(--color-text-primary);
        font-weight: 500;
      }
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .verification-page {
    padding: 15px;
  }

  .verification-card {
    padding: 20px;
  }

  .page-header {
    .page-icon {
      font-size: 42px;
    }

    .page-title {
      font-size: 24px;
    }
  }

  .status-box {
    flex-direction: column;
    text-align: center;
  }
}
</style>
