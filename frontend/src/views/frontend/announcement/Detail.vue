<template>
  <div class="announcement-detail-page">
    <!-- 返回按钮 -->
    <div class="back-section">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
        返回
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 公告内容 -->
    <template v-else>
      <article v-if="announcement" class="announcement-card card-styled">
        <div class="card-header">
          <span class="card-icon">📢</span>
          <h1 class="card-title">{{ announcement.title }}</h1>
        </div>

        <div class="card-meta">
          <span class="meta-item">
            <span class="meta-icon">👤</span>
            {{ announcement.admin?.name || '管理员' }}
          </span>
          <span class="meta-item">
            <span class="meta-icon">📅</span>
            {{ formatDate(announcement.createTime) }}
          </span>
        </div>

        <div class="card-content">
          <p v-html="formatContent(announcement.content)"></p>
        </div>
      </article>

      <!-- 未找到 -->
      <div v-else class="not-found-state">
        <div class="not-found-icon">📭</div>
        <h3 class="not-found-title">公告不存在</h3>
        <p class="not-found-desc">该公告可能已被删除或不存在</p>
        <button class="btn-go-list" @click="goToAnnouncementList">
          📋 返回公告列表
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const announcement = ref(null)

// 初始化
onMounted(() => {
  const announcementId = route.params.id
  if (announcementId) {
    fetchAnnouncementDetail(announcementId)
  } else {
    loading.value = false
  }
})

// 获取公告详情
const fetchAnnouncementDetail = async (id) => {
  loading.value = true
  try {
    await request.get(`/announcements/${id}`, {}, {
      onSuccess: (data) => {
        announcement.value = data
      },
      onError: () => {
        announcement.value = null
      }
    })
  } catch (error) {
    console.error('获取公告详情失败:', error)
    announcement.value = null
  } finally {
    loading.value = false
  }
}

// 格式化内容（将换行符转换为<br>）
const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return DateUtils.formatDateTime(date)
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 前往公告列表
const goToAnnouncementList = () => {
  router.push('/announcements')
}
</script>

<style lang="scss" scoped>
.announcement-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  animation: fadeInUp 0.4s ease-out;
}

// 返回按钮
.back-section {
  margin-bottom: 20px;

  .back-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    background: white;
    border: 2px solid var(--color-border-light);
    border-radius: var(--radius-full);
    font-weight: 600;
    font-size: 14px;
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
      background: var(--color-bg-tertiary);
    }

    .back-icon {
      font-size: 16px;
    }
  }
}

// 加载状态
.loading-state {
  background: white;
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-md);
}

// 公告卡片
.announcement-card {
  padding: 40px;

  .card-header {
    text-align: center;
    margin-bottom: 24px;

    .card-icon {
      font-size: 56px;
      display: block;
      margin-bottom: 12px;
      animation: float 3s ease-in-out infinite;
    }

    .card-title {
      font-size: 28px;
      font-weight: 800;
      color: var(--color-text-primary);
      margin: 0;
      line-height: 1.3;
    }
  }

  .card-meta {
    display: flex;
    justify-content: center;
    gap: 32px;
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--color-border-light);

    .meta-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: var(--color-text-tertiary);

      .meta-icon {
        font-size: 18px;
      }
    }
  }

  .card-content {
    line-height: 1.8;
    font-size: 16px;
    color: var(--color-text-secondary);

    :deep(p) {
      margin-bottom: 1em;
    }
  }
}

// 未找到状态
.not-found-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);

  .not-found-icon {
    font-size: 80px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  .not-found-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
  }

  .not-found-desc {
    color: var(--color-text-tertiary);
    margin: 0 0 24px 0;
  }
}

.btn-go-list {
  padding: 12px 28px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
  }
}

// 响应式
@media (max-width: 768px) {
  .announcement-detail-page {
    padding: 15px;
  }

  .announcement-card {
    padding: 24px;

    .card-header {
      .card-icon {
        font-size: 42px;
      }

      .card-title {
        font-size: 22px;
      }
    }

    .card-meta {
      flex-direction: column;
      gap: 8px;
      align-items: center;
    }
  }

  .not-found-state {
    padding: 60px 20px;

    .not-found-icon {
      font-size: 64px;
    }
  }
}
</style>
