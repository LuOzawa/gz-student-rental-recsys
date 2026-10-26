<template>
  <div class="message-center-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <span class="page-icon">💬</span>
      <h1 class="page-title">消息中心</h1>
      <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
    </div>

    <div class="chat-container">
      <!-- 左侧会话列表 -->
      <div class="conversation-panel card-styled">
        <div class="panel-header">
          <h3>📋 最近联系</h3>
        </div>

        <div v-if="conversationsLoading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>

        <div v-else-if="conversations.length === 0" class="empty-list">
          <span class="empty-icon">📭</span>
          <p>暂无消息</p>
        </div>

        <div v-else class="conversation-list">
          <div
            v-for="conv in conversations"
            :key="conv.messageId || conv.otherUserId"
            class="conversation-item"
            :class="{ active: currentUserId === conv.otherUserId }"
            @click="selectConversation(conv.otherUserId)"
          >
            <div class="avatar-wrapper">
              <div class="avatar">
                {{ getAvatarText(conv.otherUserName, conv.otherUserId) }}
              </div>
              <span v-if="conv.unreadCount > 0" class="unread-dot"></span>
            </div>
            <div class="conversation-content">
              <div class="conversation-top">
                <span class="user-name">{{ getConversationDisplayName(conv) }}</span>
                <span class="message-time">{{ formatTime(conv.lastTime) }}</span>
              </div>
              <div class="last-message">{{ conv.lastMessage || '暂无消息' }}</div>
            </div>
            <div v-if="conv.unreadCount > 0" class="unread-count">
              {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-panel card-styled">
        <div v-if="!currentUserId" class="no-chat-selected">
          <span class="placeholder-icon">💬</span>
          <p>选择一个会话开始聊天</p>
        </div>

        <div v-else class="chat-window">
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div class="header-avatar">
              {{ getAvatarText(getCurrentUserInfo()?.otherUserName, currentUserId) }}
            </div>
            <span class="chat-name">{{ getConversationDisplayName(getCurrentUserInfo()) }}</span>
          </div>

          <!-- 消息区域 -->
          <div ref="messagesContainer" class="messages-area">
            <div v-if="messagesLoading" class="messages-loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="messages.length === 0" class="no-messages">
              <span class="empty-icon">👋</span>
              <p>暂无消息记录，打个招呼吧~</p>
            </div>
            <div v-else class="messages-list">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="message-item"
                :class="{ 'is-self': msg.isSelf }"
              >
                <div class="avatar">
                  {{ msg.isSelf ? getAvatarText(userStore.userInfo?.name) : getAvatarText(getCurrentUserInfo()?.otherUserName, currentUserId) }}
                </div>
                <div class="message-wrapper">
                  <div class="message-bubble">
                    {{ msg.content }}
                  </div>
                  <div class="message-meta">
                    <span class="message-time">{{ formatMessageTime(msg.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <div class="input-wrapper">
              <el-input
                v-model="newMessage"
                type="textarea"
                :rows="2"
                :maxlength="500"
                show-word-limit
                placeholder="输入消息...按 Enter 发送，Shift+Enter 换行"
                @keydown.enter.exact="handleEnterKey"
                @keydown.enter.shift.exact="handleShiftEnter"
                resize="none"
                class="message-input"
              />
              <div class="input-actions">
                <span class="input-tip">按 Enter 发送</span>
                <button
                  class="btn-send"
                  :disabled="!newMessage.trim() || sending"
                  @click="sendMessage"
                >
                  {{ sending ? '发送中...' : '📤 发送' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getRecentConversations,
  getConversation,
  sendMessage as sendMsg
} from '@/api/message'

const baseAPI = import.meta.env.VITE_BASE_API || '/api'
const route = useRoute()
const userStore = useUserStore()
const conversations = ref([])
const conversationsLoading = ref(true)
const currentUserId = ref(null)
const messages = ref([])
const messagesLoading = ref(false)
const newMessage = ref('')
const sending = ref(false)
const unreadCount = ref(0)
const messagesContainer = ref(null)
// 用户名缓存，用于存储从URL参数传递的用户名
const userNameCache = ref(new Map())

const getCurrentUserInfo = () => {
  return conversations.value.find(c => c.otherUserId === currentUserId.value)
}

// 基于用户名或ID生成头像文字
const getAvatarText = (name, userId = null) => {
  if (name) {
    return name.charAt(0).toUpperCase()
  }
  // 如果没有名字，基于userId生成一致的字符
  if (userId) {
    const chars = 'U用户租客房东'
    return chars[userId % chars.length]
  }
  return 'U'
}

// 获取会话的显示名称
const getConversationDisplayName = (conv) => {
  return conv.otherUserName || '用户'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString()
}

const formatMessageTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  if (msgDate.getTime() === today.getTime()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (diff = today - msgDate, diff === 86400000) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else {
    return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  }
}

const loadConversations = async () => {
  conversationsLoading.value = true
  try {
    const res = await getRecentConversations()
    const data = Array.isArray(res) ? res : (res?.data || [])
    // 从缓存中填充缺失的用户名
    conversations.value = data.map(conv => {
      if (!conv.otherUserName && userNameCache.value.has(conv.otherUserId)) {
        return { ...conv, otherUserName: userNameCache.value.get(conv.otherUserId) }
      }
      return conv
    })
    unreadCount.value = conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
  } catch (error) {
    console.error('加载会话列表失败:', error)
    conversations.value = []
  } finally {
    conversationsLoading.value = false
  }
}

const selectConversation = async (userId) => {
  if (currentUserId.value === userId) return
  currentUserId.value = userId
  await loadMessages()
}

const loadMessages = async () => {
  if (!currentUserId.value) return

  messagesLoading.value = true
  try {
    const res = await getConversation(currentUserId.value)
    const data = Array.isArray(res) ? res : (res?.data || [])
    messages.value = data

    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = []
  } finally {
    messagesLoading.value = false
  }
}

const handleEnterKey = () => {
  sendMessage()
}

const handleShiftEnter = () => {
  // 让输入框自动换行，不做处理
}

const sendMessage = async () => {
  if (!newMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  sending.value = true
  try {
    await sendMsg({
      receiverId: currentUserId.value,
      content: newMessage.value.trim()
    })
    newMessage.value = ''
    await loadMessages()
    await loadConversations()
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

onMounted(async () => {
  await loadConversations()

  // 检查URL中是否包含userId参数（从房源详情页跳转过来）
  const targetUserId = Number(route.query.userId)
  const targetUserName = route.query.userName
  const houseId = route.query.houseId

  if (targetUserId) {
    // 如果传递了用户名，保存到缓存并更新会话列表
    if (targetUserName) {
      userNameCache.value.set(targetUserId, targetUserName)
      const convIndex = conversations.value.findIndex(c => c.otherUserId === targetUserId)
      if (convIndex !== -1) {
        conversations.value[convIndex].otherUserName = targetUserName
      } else {
        // 如果会话不存在，创建一个临时会话
        conversations.value.unshift({
          otherUserId: targetUserId,
          otherUserName: targetUserName,
          lastMessage: '',
          lastTime: null,
          unreadCount: 0
        })
      }
    }

    await selectConversation(targetUserId)

    // 如果有房源ID，可以在消息中添加房源引用
    if (houseId && !newMessage.value) {
      newMessage.value = `您好，我对房源（ID: ${houseId}）很感兴趣，想咨询一下相关信息。`
    }
  }
})
</script>

<style lang="scss" scoped>
.message-center-page {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: var(--color-bg-primary);
}

// 页面头部
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  position: relative;

  .page-icon {
    font-size: 28px;
    animation: float 3s ease-in-out infinite;
  }

  .page-title {
    font-family: 'Outfit', sans-serif;
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .unread-badge {
    margin-left: auto;
    padding: 4px 10px;
    background: var(--color-primary-gradient);
    color: white;
    border-radius: var(--radius-full);
    font-size: 14px;
    font-weight: 600;
  }
}

// 聊天容器
.chat-container {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

// 左侧会话面板
.conversation-panel {
  width: 320px;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--color-border-light);

    h3 {
      font-size: 16px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0;
    }
  }

  .loading-state,
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

  .conversation-list {
    flex: 1;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: var(--color-border);
      border-radius: 3px;
    }
  }

  .conversation-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    cursor: pointer;
    transition: all var(--transition-fast);
    position: relative;
    border-bottom: 1px solid var(--color-border-light);

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

      .user-name {
        color: var(--color-primary);
      }
    }

    .avatar-wrapper {
      position: relative;
      flex-shrink: 0;

      .avatar {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background: var(--color-secondary-gradient);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 18px;
      }

      .unread-dot {
        position: absolute;
        bottom: 0;
        right: 0;
        width: 12px;
        height: 12px;
        background: var(--color-error);
        border: 2px solid white;
        border-radius: 50%;
      }
    }

    .conversation-content {
      flex: 1;
      min-width: 0;
    }

    .conversation-top {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;
    }

    .user-name {
      font-weight: 600;
      color: var(--color-text-primary);
      font-size: 14px;
    }

    .message-time {
      font-size: 12px;
      color: var(--color-text-tertiary);
    }

    .last-message {
      font-size: 13px;
      color: var(--color-text-tertiary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .unread-count {
      margin-left: auto;
      padding: 2px 8px;
      background: var(--color-primary-gradient);
      color: white;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
    }
  }
}

// 右侧聊天面板
.chat-panel {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.no-chat-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);

  .placeholder-icon {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.4;
  }

  p {
    margin: 0;
  }
}

.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
}

// 聊天头部
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;

  .header-avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    background: var(--color-secondary-gradient);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 16px;
  }

  .chat-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
}

// 消息区域
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--color-bg-tertiary);
  min-height: 0;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: var(--color-border);
    border-radius: 3px;
  }
}

.messages-loading,
.no-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-tertiary);

  .empty-icon {
    font-size: 56px;
    margin-bottom: 12px;
    opacity: 0.5;
  }

  p {
    margin: 0;
  }
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 70%;

  .avatar {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    background: var(--color-secondary-gradient);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 14px;
    flex-shrink: 0;
  }

  .message-wrapper {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .message-bubble {
    padding: 12px 16px;
    border-radius: 16px;
    background: white;
    word-break: break-word;
    font-size: 14px;
    line-height: 1.5;
    color: var(--color-text-primary);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }

  .message-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 11px;
    color: var(--color-text-tertiary);
  }

  &.is-self {
    align-self: flex-end;
    flex-direction: row-reverse;

    .avatar {
      background: var(--color-primary-gradient);
    }

    .message-bubble {
      background: var(--color-primary-gradient);
      color: white;
      border-bottom-right-radius: 4px;
    }

    .message-meta {
      justify-content: flex-end;
    }
  }

  &:not(.is-self) .message-bubble {
    border-bottom-left-radius: 4px;
  }
}

// 输入区域
.input-area {
  flex-shrink: 0;
  padding: 16px 20px;
  background: white;
  border-top: 1px solid var(--color-border-light);
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    border: 2px solid var(--color-border);
    resize: none;
    font-size: 14px;

    &:focus {
      border-color: var(--color-primary);
    }
  }
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-tip {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.btn-send {
  padding: 10px 20px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
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

// 响应式
@media (max-width: 768px) {
  .message-center-page {
    padding: 10px;
  }

  .chat-container {
    flex-direction: column;
  }

  .conversation-panel {
    width: 100%;
    max-height: 200px;
  }

  .message-item {
    max-width: 85%;
  }
}
</style>
