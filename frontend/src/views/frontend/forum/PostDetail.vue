<template>
  <div class="post-detail-page">
    <!-- 返回按钮 -->
    <div class="back-section">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
        返回论坛
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- 帖子不存在 -->
    <div v-else-if="!post" class="not-found-state">
      <div class="not-found-icon">📭</div>
      <h3 class="not-found-title">帖子不存在</h3>
      <p class="not-found-desc">该帖子可能已被删除或不存在</p>
    </div>

    <!-- 帖子内容 -->
    <template v-else>
      <!-- 帖子卡片 -->
      <article class="post-card card-creative">
        <div class="post-badges">
          <span v-if="post.isTop === 1" class="badge badge-top">📌 置顶</span>
          <span v-if="post.isEssence === 1" class="badge badge-essence">⭐ 精华</span>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>

        <div class="post-meta">
          <div class="author-avatar">
            {{ getAvatarInitial(getPostAuthorName(post), getPostAuthorId(post)) }}
          </div>
          <div class="author-info">
            <span class="author-name">{{ getPostAuthorName(post) }}</span>
            <span class="post-time">{{ post.createTime }}</span>
          </div>
        </div>

        <div class="post-content">
          {{ post.content }}
        </div>

        <div class="post-stats">
          <span class="stat">
            <span class="stat-icon">👁</span>
            {{ post.viewCount }} 浏览
          </span>
          <span class="stat">
            <span class="stat-icon">💬</span>
            {{ replies.length }} 回复
          </span>
        </div>
      </article>

      <!-- 回复区域 -->
      <section class="replies-section card-styled">
        <div class="replies-header">
          <h3 class="replies-title">💬 全部回复 ({{ replies.length }})</h3>
        </div>

        <!-- 发表回复表单 -->
        <div v-if="isLoggedIn" class="reply-form-box">
          <div class="reply-form-header">
            <span class="reply-form-icon">✍️</span>
            <span class="reply-form-title">发表回复</span>
          </div>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            placeholder="分享你的看法..."
            maxlength="500"
            show-word-limit
            class="reply-textarea"
          />
          <div class="reply-form-actions">
            <button
              class="btn-submit-reply"
              :disabled="!replyContent.trim() || submittingReply"
              @click="submitReply"
            >
              {{ submittingReply ? '发表中...' : '🚀 发表回复' }}
            </button>
          </div>
        </div>

        <div v-else class="login-prompt">
          <span class="login-icon">🔐</span>
          <p class="login-text">登录后可以参与讨论</p>
          <button class="btn-login-prompt" @click="goToLogin">立即登录</button>
        </div>

        <!-- 分隔线 -->
        <div v-if="isLoggedIn && replies.length > 0" class="divider"></div>

        <!-- 回复列表 -->
        <div v-if="topLevelReplies.length > 0" class="replies-list">
          <div
            v-for="reply in topLevelReplies"
            :key="reply.id"
            class="reply-item"
          >
            <div class="reply-avatar">
              {{ getAvatarInitial(getReplyAuthorName(reply), getReplyAuthorId(reply)) }}
            </div>
            <div class="reply-body">
              <div class="reply-header">
                <span class="reply-author">{{ getReplyAuthorName(reply) }}</span>
                <span class="reply-time">{{ reply.createTime }}</span>
              </div>
              <div class="reply-text">{{ reply.content }}</div>
              <div v-if="isLoggedIn" class="reply-actions">
                <button class="reply-action-btn" @click="handleReplyToReply(reply)">
                  💬 回复
                </button>
                <button
                  v-if="canDeleteReply(reply)"
                  class="reply-action-btn reply-action-btn-danger"
                  @click="handleDeleteReply(reply)"
                >
                  🗑️ 删除
                </button>
              </div>

              <!-- 嵌套回复表单 -->
              <div v-if="replyingToId === reply.id" class="nested-reply-form">
                <el-input
                  v-model="nestedReplyContent"
                  type="textarea"
                  :rows="3"
                  :placeholder="`回复 ${getReplyAuthorName(reply)}...`"
                  maxlength="500"
                  show-word-limit
                />
                <div class="nested-reply-actions">
                  <button class="btn-cancel" @click="cancelNestedReply">取消</button>
                  <button
                    class="btn-submit-nested"
                    :disabled="!nestedReplyContent.trim() || submittingNestedReply"
                    @click="submitNestedReply(reply)"
                  >
                    {{ submittingNestedReply ? '发表中...' : '发送' }}
                  </button>
                </div>
              </div>

              <!-- 子回复列表 -->
              <div v-if="getChildReplies(reply.id).length > 0" class="child-replies">
                <div
                  v-for="childReply in getChildReplies(reply.id)"
                  :key="childReply.id"
                  class="child-reply-item"
                >
                  <div class="reply-avatar reply-avatar-sm">
                    {{ getAvatarInitial(getReplyAuthorName(childReply), getReplyAuthorId(childReply)) }}
                  </div>
                  <div class="reply-body">
                    <div class="reply-header">
                      <span class="reply-author">{{ getReplyAuthorName(childReply) }}</span>
                      <span v-if="childReply.replyToUsername" class="reply-target">
                        回复 @{{ childReply.replyToUsername }}
                      </span>
                      <span class="reply-time">{{ childReply.createTime }}</span>
                    </div>
                    <div class="reply-text">{{ childReply.content }}</div>
                    <div v-if="isLoggedIn" class="reply-actions">
                      <button
                        class="reply-action-btn"
                        @click="handleReplyToReply(childReply, reply.id)"
                      >
                        💬 回复
                      </button>
                      <button
                        v-if="canDeleteReply(childReply)"
                        class="reply-action-btn reply-action-btn-danger"
                        @click="handleDeleteReply(childReply)"
                      >
                        🗑️ 删除
                      </button>
                    </div>

                    <!-- 子回复的回复表单 -->
                    <div v-if="replyingToId === childReply.id" class="nested-reply-form">
                      <el-input
                        v-model="nestedReplyContent"
                        type="textarea"
                        :rows="3"
                        :placeholder="`回复 ${getReplyAuthorName(childReply)}...`"
                        maxlength="500"
                        show-word-limit
                      />
                      <div class="nested-reply-actions">
                        <button class="btn-cancel" @click="cancelNestedReply">取消</button>
                        <button
                          class="btn-submit-nested"
                          :disabled="!nestedReplyContent.trim() || submittingNestedReply"
                          @click="submitNestedReply(childReply, reply.id)"
                        >
                          {{ submittingNestedReply ? '发表中...' : '发送' }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-replies">
          <div class="empty-icon">💭</div>
          <h4 class="empty-title">暂无回复</h4>
          <p class="empty-desc">快来抢沙发吧~</p>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const postId = route.params.id

// 用户登录状态
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 帖子数据
const loading = ref(true)
const post = ref(null)

// 回复数据
const replies = ref([])
const replyContent = ref('')
const submittingReply = ref(false)

// 嵌套回复
const replyingToId = ref(null)
const nestedReplyContent = ref('')
const submittingNestedReply = ref(false)
const currentParentId = ref(null)

// 获取顶级回复
const topLevelReplies = computed(() => {
  return replies.value.filter(reply => !reply.parentId)
})

// 获取子回复
const getChildReplies = (parentId) => {
  return replies.value.filter(reply => reply.parentId === parentId)
}

// 初始化
onMounted(() => {
  fetchPostDetail()
  fetchReplies()
})

// 获取帖子详情
const fetchPostDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/forum-posts/${postId}`)
    post.value = res
  } catch (error) {
    console.error('获取帖子详情失败:', error)
    post.value = null
  } finally {
    loading.value = false
  }
}

// 获取回复列表
const fetchReplies = async () => {
  try {
    const res = await request.get(`/forum-replies/post/${postId}`, {
      currentPage: 1,
      size: 100
    })
    replies.value = res?.records || res || []
  } catch (error) {
    console.error('获取回复列表失败:', error)
    replies.value = []
  }
}

// 提交回复
const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submittingReply.value = true
  try {
    await request.post('/forum-replies', {
      postId: postId,
      content: replyContent.value
    })
    ElMessage.success('回复成功')
    replyContent.value = ''
    fetchReplies()
    fetchPostDetail()
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    submittingReply.value = false
  }
}

// 回复某条回复
const handleReplyToReply = (reply, parentId = null) => {
  replyingToId.value = reply.id
  currentParentId.value = parentId || reply.id
  nestedReplyContent.value = ''
}

// 取消嵌套回复
const cancelNestedReply = () => {
  replyingToId.value = null
  currentParentId.value = null
  nestedReplyContent.value = ''
}

// 提交嵌套回复
const submitNestedReply = async (reply, topLevelParentId = null) => {
  if (!nestedReplyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submittingNestedReply.value = true
  try {
    const parentId = topLevelParentId || reply.id

    await request.post('/forum-replies', {
      postId: postId,
      content: nestedReplyContent.value,
      parentId: parentId,
      replyToUserId: reply.userId
    })
    ElMessage.success('回复成功')
    nestedReplyContent.value = ''
    replyingToId.value = null
    currentParentId.value = null
    fetchReplies()
    fetchPostDetail()
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    submittingNestedReply.value = false
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

// 获取回复者名字（兼容多种数据结构）
const getReplyAuthorName = (reply) => {
  return reply.username || reply.userName || reply.authorName || reply.author || reply.user?.name || reply.user?.username || '租客'
}

// 获取回复者ID
const getReplyAuthorId = (reply) => {
  return reply.userId || reply.user?.id || reply.authorId || reply.id
}

// 判断是否可以删除回复
const canDeleteReply = (reply) => {
  if (!isLoggedIn.value) return false
  const currentUser = userStore.userInfo
  return currentUser?.roleCode === 'ADMIN' || reply.userId === currentUser?.id
}

// 删除回复
const handleDeleteReply = async (reply) => {
  const childCount = getChildReplies(reply.id).length
  const cascadeWarning = childCount > 0
    ? `\n\n该回复有 ${childCount} 条子回复，删除后将一并删除！`
    : ''

  try {
    await ElMessageBox.confirm(
      `确定要删除该回复吗？删除后无法恢复！${cascadeWarning}`,
      '警告',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )

    await request.delete(`/forum-replies/${reply.id}`)
    ElMessage.success('删除成功')
    fetchReplies()
    fetchPostDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除回复失败:', error)
    }
  }
}

// 返回
const goBack = () => {
  router.push('/forum')
}

// 前往登录
const goToLogin = () => {
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.post-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
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
  padding: 24px;
  box-shadow: var(--shadow-md);
}

// 未找到状态
.not-found-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);

  .not-found-icon {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.6;
  }

  .not-found-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
  }

  .not-found-desc {
    color: var(--color-text-tertiary);
    margin: 0;
  }
}

// 帖子卡片
.post-card {
  padding: 32px;
  margin-bottom: 20px;
  animation: fadeInUp 0.4s ease-out;

  .post-badges {
    display: flex;
    gap: 10px;
    margin-bottom: 16px;

    .badge {
      padding: 6px 14px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 600;

      &.badge-top {
        background: var(--color-primary-gradient);
        color: white;
      }

      &.badge-essence {
        background: linear-gradient(135deg, #FFE66D 0%, #FFC107 100%);
        color: var(--color-text-primary);
      }
    }
  }

  .post-title {
    font-size: 28px;
    font-weight: 800;
    color: var(--color-text-primary);
    margin: 0 0 20px 0;
    line-height: 1.3;
  }

  .post-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--color-border-light);
    margin-bottom: 20px;

    .author-avatar {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: var(--color-secondary-gradient);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 18px;
      flex-shrink: 0;
    }

    .author-info {
      display: flex;
      flex-direction: column;

      .author-name {
        font-weight: 600;
        font-size: 16px;
        color: var(--color-text-primary);
      }

      .post-time {
        font-size: 13px;
        color: var(--color-text-tertiary);
      }
    }
  }

  .post-content {
    color: var(--color-text-secondary);
    line-height: 1.8;
    font-size: 16px;
    white-space: pre-line;
    margin-bottom: 20px;
    min-height: 100px;
  }

  .post-stats {
    display: flex;
    gap: 20px;
    padding-top: 16px;
    border-top: 1px solid var(--color-border-light);

    .stat {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: var(--color-text-tertiary);

      .stat-icon {
        font-size: 16px;
      }
    }
  }
}

// 回复区域
.replies-section {
  padding: 24px;

  .replies-header {
    margin-bottom: 20px;

    .replies-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0;
    }
  }

  // 回复表单
  .reply-form-box {
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-lg);
    padding: 20px;
    margin-bottom: 20px;

    .reply-form-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 14px;

      .reply-form-icon {
        font-size: 20px;
      }

      .reply-form-title {
        font-weight: 600;
        color: var(--color-text-primary);
      }
    }

    :deep(.el-textarea) {
      .el-textarea__inner {
        border-radius: var(--radius-md);
        border: 2px solid var(--color-border);
        background: white;

        &:focus {
          border-color: var(--color-primary);
        }
      }
    }

    .reply-form-actions {
      margin-top: 12px;
      display: flex;
      justify-content: flex-end;
    }
  }

  // 登录提示
  .login-prompt {
    text-align: center;
    padding: 40px 20px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-lg);
    margin-bottom: 20px;

    .login-icon {
      font-size: 48px;
      display: block;
      margin-bottom: 12px;
    }

    .login-text {
      color: var(--color-text-secondary);
      margin: 0 0 16px 0;
    }
  }

  .divider {
    height: 1px;
    background: var(--color-border-light);
    margin: 20px 0;
  }

  // 回复列表
  .replies-list {
    .reply-item {
      display: flex;
      gap: 12px;
      padding: 20px 0;
      border-bottom: 1px solid var(--color-border-light);

      &:last-child {
        border-bottom: none;
      }

      .reply-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: var(--color-secondary-gradient);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 16px;
        flex-shrink: 0;
      }

      .reply-body {
        flex: 1;
        min-width: 0;

        .reply-header {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;
          flex-wrap: wrap;

          .reply-author {
            font-weight: 600;
            font-size: 15px;
            color: var(--color-text-primary);
          }

          .reply-target {
            color: var(--color-primary);
            font-size: 13px;
            background: rgba(255, 107, 107, 0.1);
            padding: 2px 8px;
            border-radius: var(--radius-sm);
          }

          .reply-time {
            font-size: 13px;
            color: var(--color-text-tertiary);
          }
        }

        .reply-text {
          color: var(--color-text-secondary);
          line-height: 1.6;
          white-space: pre-line;
        }

        .reply-actions {
          margin-top: 10px;
          display: flex;
          gap: 16px;
        }

        .nested-reply-form {
          margin-top: 12px;
          padding: 14px;
          background: var(--color-bg-tertiary);
          border-radius: var(--radius-md);

          :deep(.el-textarea) {
            .el-textarea__inner {
              border: 2px solid var(--color-border);
              background: white;

              &:focus {
                border-color: var(--color-primary);
              }
            }
          }
        }

        // 子回复
        .child-replies {
          margin-top: 16px;
          padding-left: 14px;
          border-left: 2px solid var(--color-border);

          .child-reply-item {
            display: flex;
            gap: 10px;
            padding: 14px 0;
            border-bottom: 1px solid var(--color-border-light);

            &:last-child {
              border-bottom: none;
            }

            .reply-avatar-sm {
              width: 32px;
              height: 32px;
              font-size: 14px;
            }
          }
        }
      }
    }
  }

  // 空状态
  .empty-replies {
    text-align: center;
    padding: 60px 20px;

    .empty-icon {
      font-size: 56px;
      margin-bottom: 12px;
      opacity: 0.5;
    }

    .empty-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0 0 6px 0;
    }

    .empty-desc {
      color: var(--color-text-tertiary);
      margin: 0;
    }
  }
}

// 按钮样式
.btn-submit-reply {
  padding: 10px 24px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

.btn-login-prompt {
  padding: 10px 24px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
  }
}

.reply-action-btn {
  padding: 6px 12px;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-bg-tertiary);
    color: var(--color-text-secondary);
  }

  &.reply-action-btn-danger:hover {
    background: rgba(255, 107, 107, 0.1);
    color: var(--color-error);
  }
}

.btn-cancel {
  padding: 8px 16px;
  background: transparent;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--color-text-secondary);
  }
}

.btn-submit-nested {
  padding: 8px 20px;
  background: var(--color-primary-gradient);
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  font-size: 13px;
  color: white;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// 响应式
@media (max-width: 768px) {
  .post-detail-page {
    padding: 15px;
  }

  .post-card {
    padding: 20px;

    .post-title {
      font-size: 22px;
    }
  }

  .replies-section {
    padding: 16px;

    .reply-form-box,
    .login-prompt {
      padding: 16px;
    }
  }
}
</style>
