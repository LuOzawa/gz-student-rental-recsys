<template>
  <div class="forum-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <span class="page-icon">💬</span>
          <h1 class="page-title">广寓论坛</h1>
          <p class="page-subtitle">分享租房经验，交流生活点滴</p>
        </div>
        <div class="header-actions">
          <button
            v-if="isLoggedIn"
            class="btn-create"
            @click="handleCreatePost"
          >
            <span class="btn-icon">✍️</span>
            发布帖子
          </button>
        </div>
      </div>
      <!-- 装饰性blob -->
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <!-- 分类标签 -->
    <div class="category-section">
      <div class="category-tabs">
        <button
          :class="['category-tab', { active: selectedCategory === null }]"
          @click="selectCategory(null)"
        >
          📋 全部
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          :class="['category-tab', { active: selectedCategory === category.id }]"
          @click="selectCategory(category.id)"
        >
          {{ getCategoryEmoji(category.id) }} {{ category.name }}
        </button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索帖子标题..."
          @keyup.enter="handleSearch"
        />
        <button v-if="searchKeyword" class="clear-btn" @click="searchKeyword = ''; handleSearch()">
          ✕
        </button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="posts-section" v-loading="loading">
      <div v-if="!loading && posts.length > 0" class="posts-list">
        <div
          v-for="(post, index) in posts"
          :key="post.id"
          class="post-card card-creative"
          :style="{ animationDelay: `${index * 50}ms` }"
          @click="viewPostDetail(post.id)"
        >
          <div class="post-badges">
            <span v-if="post.isTop === 1" class="badge badge-top">📌 置顶</span>
            <span v-if="post.isEssence === 1" class="badge-badge-essence">⭐ 精华</span>
          </div>

          <h3 class="post-title">{{ post.title }}</h3>

          <p class="post-preview">{{ getContentPreview(post.content) }}</p>

          <div class="post-footer">
            <div class="author-info">
              <div class="author-avatar">
                {{ getAvatarInitial(getPostAuthorName(post), getPostAuthorId(post)) }}
              </div>
              <div class="author-details">
                <span class="author-name">{{ getPostAuthorName(post) }}</span>
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
              </div>
            </div>

            <div class="post-stats">
              <span class="stat">
                <span class="stat-icon">👁</span>
                {{ post.viewCount }}
              </span>
              <span class="stat">
                <span class="stat-icon">💬</span>
                {{ post.replyCount }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && posts.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <h3 class="empty-title">暂无帖子</h3>
        <p class="empty-desc">
          {{ searchKeyword ? '试试其他关键词吧' : '成为第一个发帖的人吧！' }}
        </p>
        <button v-if="isLoggedIn && !searchKeyword" class="btn-primary" @click="handleCreatePost">
          ✍️ 发布帖子
        </button>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="posts.length > 0" class="pagination-section">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 30]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 发布帖子对话框 -->
    <el-dialog
      v-model="postDialogVisible"
      title="发布帖子"
      width="600px"
      :close-on-click-modal="false"
      class="post-dialog"
    >
      <el-form
        ref="postFormRef"
        :model="postForm"
        :rules="postRules"
        label-position="top"
      >
        <el-form-item label="📁 分类" prop="categoryId">
          <el-select v-model="postForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="📝 标题" prop="title">
          <el-input
            v-model="postForm.title"
            placeholder="请输入帖子标题（5-200字）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="✍️ 内容" prop="content">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="10"
            placeholder="分享你的租房经验、生活技巧或任何想说的..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn-secondary" @click="postDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitPost">
            {{ submitting ? '发布中...' : '🚀 发布' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()

// 用户登录状态
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 分类数据
const categories = ref([])
const selectedCategory = ref(null)

// 分类emoji映射
const getCategoryEmoji = (id) => {
  const emojis = ['🏠', '💡', '🤝', '📖', '🎯', '💬']
  return emojis[id % emojis.length] || '📁'
}

// 帖子列表数据
const loading = ref(false)
const posts = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

// 发布帖子对话框
const postDialogVisible = ref(false)
const postFormRef = ref(null)
const submitting = ref(false)
const postForm = reactive({
  categoryId: null,
  title: '',
  content: ''
})

const postRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 200, message: '标题长度在5到200个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 10, max: 2000, message: '内容长度在10到2000个字符', trigger: 'blur' }
  ]
}

// 初始化
onMounted(() => {
  fetchCategories()
  fetchPosts()
})

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await request.get('/forum-categories')
    categories.value = res || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 获取帖子列表
const fetchPosts = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }

    if (selectedCategory.value) {
      params.categoryId = selectedCategory.value
    }

    const res = await request.get('/forum-posts', params)

    // 前端搜索过滤
    let records = res.records || []
    if (searchKeyword.value) {
      records = records.filter(item =>
        item.title && item.title.includes(searchKeyword.value)
      )
    }

    posts.value = records
    total.value = res.total || 0
  } catch (error) {
    console.error('获取帖子列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 选择分类
const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  currentPage.value = 1
  fetchPosts()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPosts()
}

// 查看帖子详情
const viewPostDetail = (postId) => {
  router.push(`/forum/post/${postId}`)
}

// 发布帖子
const handleCreatePost = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  postDialogVisible.value = true
}

// 提交帖子
const submitPost = () => {
  postFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.post('/forum-posts', postForm)
        ElMessage.success('发布成功')
        postDialogVisible.value = false
        resetPostForm()
        fetchPosts()
      } catch (error) {
        console.error('发布失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 重置表单
const resetPostForm = () => {
  postForm.categoryId = null
  postForm.title = ''
  postForm.content = ''
  if (postFormRef.value) {
    postFormRef.value.resetFields()
  }
}

// 获取头像首字母
const getAvatarInitial = (name, userId = null) => {
  if (name) {
    return name.charAt(0).toUpperCase()
  }
  // 如果没有名字，基于userId生成一致的字符
  if (userId) {
    const chars = '论坛租客房东用户UP'
    return chars[userId % chars.length]
  }
  return '论'
}

// 获取帖子作者名字（兼容多种数据结构）
const getPostAuthorName = (post) => {
  return post.user?.name || post.user?.username || post.userName || post.authorName || post.author || '租客'
}

// 获取帖子作者ID
const getPostAuthorId = (post) => {
  return post.user?.id || post.userId || post.authorId || post.id
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return time
}

// 获取内容预览
const getContentPreview = (content) => {
  if (!content) return ''
  return content.length > 150 ? content.substring(0, 150) + '...' : content
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchPosts()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchPosts()
}
</script>

<style lang="scss" scoped>
.forum-page {
  min-height: 100vh;
  background: var(--color-bg-primary);
  padding-bottom: 48px;
}

// 页面头部
.page-header {
  position: relative;
  padding: 48px 20px 32px;
  background: var(--color-primary-gradient);
  overflow: hidden;

  .header-content {
    position: relative;
    max-width: 1000px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    z-index: 1;
  }

  .title-section {
    .page-icon {
      font-size: 48px;
      display: block;
      margin-bottom: 8px;
      animation: float 3s ease-in-out infinite;
    }

    .page-title {
      font-family: 'Outfit', sans-serif;
      font-size: 36px;
      font-weight: 800;
      color: white;
      margin: 0 0 8px 0;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .page-subtitle {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.9);
      margin: 0;
    }
  }

  .header-actions {
    .btn-create {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 24px;
      background: white;
      color: var(--color-primary);
      border: none;
      border-radius: var(--radius-full);
      font-family: 'Outfit', sans-serif;
      font-weight: 600;
      font-size: 15px;
      cursor: pointer;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
      transition: all var(--transition-base);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 24px rgba(0, 0, 0, 0.2);
      }

      .btn-icon {
        font-size: 18px;
      }
    }
  }

  .blob {
    position: absolute;
    border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
    opacity: 0.15;
    animation: float 8s ease-in-out infinite;

    &.blob-1 {
      width: 200px;
      height: 200px;
      background: white;
      top: -50px;
      right: 10%;
      filter: blur(40px);
    }

    &.blob-2 {
      width: 150px;
      height: 150px;
      background: var(--color-accent);
      bottom: -30px;
      left: 5%;
      filter: blur(35px);
      animation-delay: -3s;
    }
  }
}

// 分类标签
.category-section {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--color-border-light);
  padding: 16px 20px;

  .category-tabs {
    max-width: 1000px;
    margin: 0 auto;
    display: flex;
    gap: 10px;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .category-tab {
      flex-shrink: 0;
      padding: 10px 18px;
      background: var(--color-bg-tertiary);
      border: 2px solid transparent;
      border-radius: var(--radius-full);
      font-size: 14px;
      font-weight: 500;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all var(--transition-base);
      white-space: nowrap;

      &:hover {
        background: var(--color-bg-secondary);
        border-color: var(--color-primary-light);
      }

      &.active {
        background: var(--color-primary-gradient);
        color: white;
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
      }
    }
  }
}

// 搜索栏
.search-section {
  max-width: 1000px;
  margin: 24px auto 0;
  padding: 0 20px;

  .search-box {
    position: relative;
    display: flex;
    align-items: center;
    background: white;
    border-radius: var(--radius-full);
    box-shadow: var(--shadow-md);
    padding: 4px;
    border: 2px solid var(--color-border-light);
    transition: all var(--transition-base);

    &:focus-within {
      border-color: var(--color-primary);
      box-shadow: 0 4px 20px rgba(255, 107, 107, 0.15);
    }

    .search-icon {
      padding: 0 16px;
      font-size: 18px;
    }

    .search-input {
      flex: 1;
      border: none;
      background: transparent;
      padding: 12px 0;
      font-size: 15px;
      color: var(--color-text-primary);
      outline: none;

      &::placeholder {
        color: var(--color-text-tertiary);
      }
    }

    .clear-btn {
      width: 32px;
      height: 32px;
      margin-right: 4px;
      border: none;
      background: var(--color-bg-tertiary);
      border-radius: 50%;
      cursor: pointer;
      font-size: 14px;
      color: var(--color-text-tertiary);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--transition-fast);

      &:hover {
        background: var(--color-error);
        color: white;
      }
    }
  }
}

// 帖子列表
.posts-section {
  max-width: 1000px;
  margin: 24px auto 0;
  padding: 0 20px;
  min-height: 400px;

  .posts-list {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .post-card {
      padding: 20px;
      cursor: pointer;
      animation: fadeInUp 0.4s ease-out backwards;

      .post-badges {
        display: flex;
        gap: 8px;
        margin-bottom: 12px;

        .badge {
          padding: 4px 10px;
          border-radius: var(--radius-full);
          font-size: 12px;
          font-weight: 600;

          &.badge-top {
            background: linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%);
            color: white;
          }

          &.badge-badge-essence {
            background: linear-gradient(135deg, #FFE66D 0%, #FFC107 100%);
            color: var(--color-text-primary);
          }
        }
      }

      .post-title {
        font-size: 18px;
        font-weight: 700;
        color: var(--color-text-primary);
        margin: 0 0 10px 0;
        line-height: 1.4;
      }

      .post-preview {
        color: var(--color-text-secondary);
        line-height: 1.6;
        margin: 0 0 16px 0;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .post-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-top: 14px;
        border-top: 1px solid var(--color-border-light);

        .author-info {
          display: flex;
          align-items: center;
          gap: 10px;

          .author-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: var(--color-secondary-gradient);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 600;
            font-size: 14px;
          }

          .author-details {
            display: flex;
            flex-direction: column;

            .author-name {
              font-size: 14px;
              font-weight: 600;
              color: var(--color-text-primary);
            }

            .post-time {
              font-size: 12px;
              color: var(--color-text-tertiary);
            }
          }
        }

        .post-stats {
          display: flex;
          gap: 16px;

          .stat {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 14px;
            color: var(--color-text-tertiary);

            .stat-icon {
              font-size: 16px;
            }
          }
        }
      }
    }
  }

  // 空状态
  .empty-state {
    text-align: center;
    padding: 80px 20px;

    .empty-icon {
      font-size: 80px;
      margin-bottom: 16px;
      opacity: 0.6;
    }

    .empty-title {
      font-size: 20px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0 0 8px 0;
    }

    .empty-desc {
      color: var(--color-text-tertiary);
      margin: 0 0 24px 0;
    }
  }
}

// 分页
.pagination-section {
  max-width: 1000px;
  margin: 32px auto 0;
  padding: 0 20px;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    .btn-prev,
    .btn-next,
    .el-pager li {
      border-radius: var(--radius-md);
      font-weight: 600;

      &.is-active {
        background: var(--color-primary-gradient);
        color: white;
      }
    }
  }
}

// 对话框样式
:deep(.post-dialog) {
  .el-dialog__header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);

    .el-dialog__title {
      font-family: 'Outfit', sans-serif;
      font-weight: 700;
      font-size: 18px;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    border-top: 1px solid var(--color-border-light);
  }

  .el-form-item__label {
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .el-input__wrapper,
  .el-textarea__inner {
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);

    &:focus,
    &:focus-within {
      box-shadow: 0 0 0 2px var(--color-primary-light);
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-primary {
  padding: 12px 24px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
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
  }
}

.btn-secondary {
  padding: 12px 24px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-text-secondary);
    background: var(--color-bg-tertiary);
  }
}

// 响应式
@media (max-width: 768px) {
  .page-header {
    padding: 32px 20px 24px;

    .header-content {
      flex-direction: column;
      text-align: center;
      gap: 20px;
    }

    .title-section {
      .page-icon {
        font-size: 36px;
      }

      .page-title {
        font-size: 28px;
      }
    }
  }

  .post-card {
    .post-footer {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;

      .post-stats {
        width: 100%;
        justify-content: flex-start;
      }
    }
  }
}
</style>
