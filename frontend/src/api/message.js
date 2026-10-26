import request from '@/utils/request'

// 发送消息
export function sendMessage(data) {
  return request.post('/messages/send', {}, { params: data })
}

// 获取与某用户的对话
export function getConversation(userId) {
  return request.get(`/messages/conversation/${userId}`)
}

// 获取最近会话列表
export function getRecentConversations() {
  return request.get('/messages/conversations')
}

// 获取未读消息数
export function getUnreadCount() {
  return request.get('/messages/unread-count')
}

// 标记消息为已读
export function markAsRead(userId) {
  return request.put(`/messages/read/${userId}`)
}

// 删除消息
export function deleteMessage(messageId) {
  return request.delete(`/messages/${messageId}`)
}
