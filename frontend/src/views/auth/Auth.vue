<template>
  <div class="auth-container">
    <div class="auth-box">
      <!-- 左侧 - 插画与品牌区域 -->
      <div class="auth-left">
        <!-- 动态漂浮元素 -->
        <div class="floating-elements">
          <span class="float-item">🏠</span>
          <span class="float-item">✨</span>
          <span class="float-item">💫</span>
          <span class="float-item">🔑</span>
          <span class="float-item">💝</span>
        </div>

        <img src="@/assets/房地产.svg" alt="房屋租赁" class="auth-illustration" />
        <div class="auth-brand">
          <h2 class="brand-title">广寓租房</h2>
          <p class="brand-subtitle">为广州大学生打造的智能租房平台</p>

          <div class="brand-feature">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Search /></el-icon>
              </div>
              <span class="feature-text">智能搜索</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Select /></el-icon>
              </div>
              <span class="feature-text">精准推荐</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Lock /></el-icon>
              </div>
              <span class="feature-text">安全可靠</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧 - 表单区域 -->
      <div class="auth-right">
        <div class="auth-header" v-if="showHeader">
          <h1 class="title">{{ title }}</h1>
          <div class="subtitle">{{ subtitle }}</div>
        </div>

        <el-form :model="formData" :rules="rules" ref="formRef" class="auth-form">
          <slot name="form-items"></slot>

          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleSubmit" class="auth-button">
              {{ submitText }}
            </el-button>
          </el-form-item>

          <div class="auth-links">
            <slot name="auth-links"></slot>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Select, Lock } from '@element-plus/icons-vue'

const props = defineProps({
  formData: {
    type: Object,
    required: true
  },
  rules: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitText: {
    type: String,
    default: '提交'
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  title: {
    type: String,
    default: '欢迎使用'
  },
  subtitle: {
    type: String,
    default: '请填写以下信息'
  }
})

const formRef = ref(null)

const emit = defineEmits(['submit'])

const handleSubmit = () => {
  formRef.value.validate(valid => {
    if (valid) {
      emit('submit', formRef)
    }
  })
}

defineExpose({
  formRef
})
</script>

<style lang="scss" scoped>
@import '@/styles/auth.scss';

/* 额外的组件特定样式 */
.feature-icon {
  .el-icon {
    font-size: 20px;
  }
}
</style>
