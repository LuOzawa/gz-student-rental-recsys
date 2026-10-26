package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Message;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.MessageMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * Send message
     */
    public Message sendMessage(Long receiverId, String content, String relatedType, Long relatedId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        if (currentUser.getId().equals(receiverId)) {
            throw new RuntimeException("不能给自己发送消息");
        }

        Message message = new Message();
        message.setSenderId(currentUser.getId());
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setRelatedType(relatedType);
        message.setRelatedId(relatedId);
        message.setIsRead(0);
        message.setIsDeletedBySender(0);
        message.setIsDeletedByReceiver(0);
        messageMapper.insert(message);

        return message;
    }

    /**
     * Get conversation between current user and another user
     */
    public List<Map<String, Object>> getConversation(Long otherUserId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        List<Message> messages = messageMapper.getConversation(currentUser.getId(), otherUserId);

        // Mark messages as read
        messageMapper.markAsRead(otherUserId, currentUser.getId());

        // Enrich with user info
        return messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("senderId", msg.getSenderId());
            map.put("receiverId", msg.getReceiverId());
            map.put("content", msg.getContent());
            map.put("relatedType", msg.getRelatedType());
            map.put("relatedId", msg.getRelatedId());
            map.put("isRead", msg.getIsRead());
            map.put("createTime", msg.getCreateTime());
            map.put("isSelf", msg.getSenderId().equals(currentUser.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Get recent conversations
     */
    public List<Map<String, Object>> getRecentConversations() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        List<Message> messages = messageMapper.getRecentConversations(currentUser.getId());

        return messages.stream().map(msg -> {
            Long otherUserId = msg.getSenderId().equals(currentUser.getId()) ? msg.getReceiverId() : msg.getSenderId();
            User otherUser = userMapper.selectById(otherUserId);

            Map<String, Object> map = new HashMap<>();
            map.put("messageId", msg.getId());
            map.put("otherUserId", otherUserId);
            map.put("otherUserName", otherUser != null ? otherUser.getName() : "未知用户");
            map.put("otherUserAvatar", otherUser != null ? otherUser.getAvatar() : null);
            map.put("lastMessage", msg.getContent());
            map.put("lastTime", msg.getCreateTime());
            map.put("unreadCount", getUnreadCountWithUser(otherUserId));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Get unread message count
     */
    public int getUnreadCount() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        return messageMapper.getUnreadCount(currentUser.getId());
    }

    /**
     * Get unread count with specific user
     */
    private int getUnreadCountWithUser(Long otherUserId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("sender_id", otherUserId)
                .eq("receiver_id", currentUser.getId())
                .eq("is_read", 0);
        return Math.toIntExact(messageMapper.selectCount(wrapper));
    }

    /**
     * Mark messages as read
     */
    public void markAsRead(Long otherUserId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        messageMapper.markAsRead(otherUserId, currentUser.getId());
    }

    /**
     * Delete message for current user
     */
    public void deleteMessage(Long messageId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        UpdateWrapper<Message> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", messageId);

        if (message.getSenderId().equals(currentUser.getId())) {
            wrapper.set("is_deleted_by_sender", 1);
        } else if (message.getReceiverId().equals(currentUser.getId())) {
            wrapper.set("is_deleted_by_receiver", 1);
        } else {
            throw new RuntimeException("无权删除此消息");
        }

        messageMapper.update(null, wrapper);
    }
}
