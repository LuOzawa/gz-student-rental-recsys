package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ForumReply;
import org.example.springboot.service.ForumReplyService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "论坛回复管理接口")
@RestController
@RequestMapping("/forum-replies")
public class ForumReplyController {

    @Resource
    private ForumReplyService forumReplyService;

    @Operation(summary = "创建回复")
    @PostMapping
    public Result<?> createReply(@RequestBody ForumReply reply) {
        return Result.success("回复成功", forumReplyService.createReply(reply));
    }

    @Operation(summary = "根据帖子ID分页查询回复")
    @GetMapping("/post/{postId}")
    public Result<?> getRepliesByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(forumReplyService.getRepliesByPostId(postId, currentPage, size));
    }

    @Operation(summary = "删除回复")
    @DeleteMapping("/{id}")
    public Result<?> deleteReply(@PathVariable Long id) {
        forumReplyService.deleteReply(id);
        return Result.success("删除成功");
    }
}
