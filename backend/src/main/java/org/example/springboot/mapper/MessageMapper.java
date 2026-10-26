package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.Message;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * Get conversation between two users
     */
    List<Message> getConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Get unread message count
     */
    int getUnreadCount(@Param("userId") Long userId);

    /**
     * Get recent conversations
     */
    List<Message> getRecentConversations(@Param("userId") Long userId);

    /**
     * Mark messages as read
     */
    int markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
