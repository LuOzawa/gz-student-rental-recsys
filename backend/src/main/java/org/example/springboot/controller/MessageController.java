package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "消息接口")
@RestController
@RequestMapping("/messages")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<?> sendMessage(
            @RequestParam Long receiverId,
            @RequestParam String content,
            @RequestParam(required = false) String relatedType,
            @RequestParam(required = false) Long relatedId) {
        try {
            messageService.sendMessage(receiverId, content, relatedType, relatedId);
            return Result.success("发送成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取与某用户的对话")
    @GetMapping("/conversation/{userId}")
    public Result<List<Map<String, Object>>> getConversation(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> conversation = messageService.getConversation(userId);
            return Result.success(conversation);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取最近会话列表")
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getRecentConversations() {
        try {
            List<Map<String, Object>> conversations = messageService.getRecentConversations();
            return Result.success(conversations);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        int count = messageService.getUnreadCount();
        return Result.success(count);
    }

    @Operation(summary = "标记消息为已读")
    @PutMapping("/read/{userId}")
    public Result<?> markAsRead(@PathVariable Long userId) {
        messageService.markAsRead(userId);
        return Result.success("标记成功");
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{messageId}")
    public Result<?> deleteMessage(@PathVariable Long messageId) {
        try {
            messageService.deleteMessage(messageId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
