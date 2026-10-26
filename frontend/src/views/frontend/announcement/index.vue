<template>
  <div class="announcements-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <span class="page-icon">📢</span>
      <h1 class="page-title">新闻公告</h1>
      <p class="page-subtitle">了解平台动态，获取最新资讯</p>
    </div>

    <div class="content-layout">
      <!-- 左侧公告列表 -->
      <div class="announcement-list-panel card-styled">
        <div class="list-header">
          <h3>📋 公告列表</h3>
        </div>

        <div v-loading="loading" class="announcement-list">
          <div
            v-for="item in announcementList"
            :key="item.id"
            class="announcement-item"
            :class="{ active: selectedAnnouncement?.id === item.id }"
            @click="handleSelectAnnouncement(item)"
          >
            <div class="item-title">{{ item.title }}</div>
            <div class="item-time">{{ formatDate(item.createTime) }}</div>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && announcementList.length === 0" class="empty-list">
            <span class="empty-icon">📭</span>
            <p>暂无公告</p>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-container">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="total"
            @current-change="handleCurrentChange"
            small
          />
        </div>
      </div>

      <!-- 右侧公告详情 -->
      <div class="announcement-detail-panel card-styled">
        <div v-if="selectedAnnouncement" class="announcement-detail">
          <h2 class="detail-title">{{ selectedAnnouncement.title }}</h2>
          <div class="detail-meta">
            <span class="meta-item">
              <span class="meta-icon">👤</span>
              {{ selectedAnnouncement.admin?.name || '管理员' }}
            </span>
            <span class="meta-item">
              <span class="meta-icon">📅</span>
              {{ formatDate(selectedAnnouncement.createTime) }}
            </span>
          </div>
          <div class="detail-content">
            <p v-html="formatContent(selectedAnnouncement.content)"></p>
          </div>
        </div>
        <div v-else class="detail-placeholder">
          <span class="placeholder-icon">📄</span>
          <p>请选择要查看的公告</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'

// 表格数据
const loading = ref(false)
const announcementList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedAnnouncement = ref(null)

// 初始化
onMounted(() => {
  fetchAnnouncementList()
})

// 获取公告列表
const fetchAnnouncementList = async () => {
  loading.value = true
  try {
    await request.get('/announcements', {
      status: 1, // 只获取已发布的公告
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        announcementList.value = res.records
        total.value = res.total

        // 如果有数据且未选中任何公告，则默认选中第一条
        if (res.records.length > 0 && !selectedAnnouncement.value) {
          selectedAnnouncement.value = res.records[0]
        }
      }
    })
  } catch (error) {
    console.error('获取公告列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchAnnouncementList()
}

// 选择公告
const handleSelectAnnouncement = (announcement) => {
  selectedAnnouncement.value = announcement
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return DateUtils.format(date, 'YYYY-MM-DD HH:mm')
}

// 格式化内容（将换行符转换为<br>）
const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}
</script>

<style lang="scss" scoped>
.announcements-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  animation: fadeInUp 0.4s ease-out;
}

// 页面头部
.page-header {
  text-align: center;
  margin-bottom: 32px;

  .page-icon {
    font-size: 48px;
    display: block;
    margin-bottom: 8px;
    animation: float 3s ease-in-out infinite;
  }

  .page-title {
    font-family: 'Outfit', sans-serif;
    font-size: 32px;
    font-weight: 800;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
  }

  .page-subtitle {
    color: var(--color-text-tertiary);
    margin: 0;
  }
}

// 内容布局
.content-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  align-items: start;
}

// 左侧列表
.announcement-list-panel {
  padding: 0;
  max-height: 600px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .list-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--color-border-light);

    h3 {
      font-size: 16px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0;
    }
  }

  .announcement-list {
    flex: 1;
    overflow-y: auto;
  }

  .announcement-item {
    padding: 16px 20px;
    border-bottom: 1px solid var(--color-border-light);
    cursor: pointer;
    transition: all var(--transition-fast);
    position: relative;

    &:before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--color-primary);
      opacity: 0;
      transition: opacity var(--transition-fast);
    }

    &:hover {
      background: var(--color-bg-tertiary);
    }

    &.active {
      background: rgba(255, 107, 107, 0.08);

      &:before {
        opacity: 1;
      }

      .item-title {
        color: var(--color-primary);
      }
    }

    .item-title {
      font-weight: 600;
      font-size: 14px;
      color: var(--color-text-primary);
      margin-bottom: 6px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-time {
      font-size: 12px;
      color: var(--color-text-tertiary);
    }
  }

  .empty-list {
    padding: 40px 20px;
    text-align: center;

    .empty-icon {
      font-size: 48px;
      display: block;
      margin-bottom: 12px;
      opacity: 0.5;
    }

    p {
      color: var(--color-text-tertiary);
      margin: 0;
    }
  }

  .pagination-container {
    padding: 12px 20px;
    border-top: 1px solid var(--color-border-light);
    display: flex;
    justify-content: center;

    :deep(.el-pagination) {
      .btn-prev,
      .btn-next,
      .el-pager li {
        border-radius: var(--radius-sm);
        font-weight: 600;

        &.is-active {
          background: var(--color-primary-gradient);
          color: white;
        }
      }
    }
  }
}

// 右侧详情
.announcement-detail-panel {
  min-height: 500px;

  .announcement-detail {
    padding: 32px;
  }

  .detail-title {
    font-size: 24px;
    font-weight: 800;
    color: var(--color-text-primary);
    margin: 0 0 16px 0;
    text-align: center;
  }

  .detail-meta {
    display: flex;
    justify-content: center;
    gap: 24px;
    margin-bottom: 24px;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--color-border-light);

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: var(--color-text-tertiary);

      .meta-icon {
        font-size: 16px;
      }
    }
  }

  .detail-content {
    line-height: 1.8;
    font-size: 16px;
    color: var(--color-text-secondary);

    :deep(p) {
      margin-bottom: 1em;
    }
  }

  .detail-placeholder {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 400px;

    .placeholder-icon {
      font-size: 64px;
      margin-bottom: 16px;
      opacity: 0.4;
    }

    p {
      color: var(--color-text-tertiary);
      margin: 0;
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .announcements-page {
    padding: 15px;
  }

  .page-header {
    .page-icon {
      font-size: 36px;
    }

    .page-title {
      font-size: 24px;
    }
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .announcement-list-panel {
    max-height: 300px;
  }

  .announcement-detail-panel {
    min-height: 300px;

    .announcement-detail {
      padding: 20px;
    }

    .detail-title {
      font-size: 20px;
    }

    .detail-meta {
      flex-direction: column;
      gap: 8px;
      align-items: center;
    }
  }
}
</style>
