package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ForumPost;
import org.example.springboot.service.ForumPostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "论坛帖子管理接口")
@RestController
@RequestMapping("/forum-posts")
public class ForumPostController {

    @Resource
    private ForumPostService forumPostService;

    @Operation(summary = "发布帖子")
    @PostMapping
    public Result<?> createPost(@RequestBody ForumPost post) {
        return Result.success("发布成功", forumPostService.createPost(post));
    }

    @Operation(summary = "分页查询帖子")
    @GetMapping
    public Result<?> getPostsByPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(forumPostService.getPostsByPage(categoryId, keyword, status, currentPage, size));
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id}")
    public Result<?> getPostById(@PathVariable Long id) {
        return Result.success(forumPostService.getPostById(id));
    }

    @Operation(summary = "更新帖子")
    @PutMapping("/{id}")
    public Result<?> updatePost(@PathVariable Long id, @RequestBody ForumPost post) {
        return Result.success("更新成功", forumPostService.updatePost(id, post));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public Result<?> deletePost(@PathVariable Long id) {
        forumPostService.deletePost(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "置顶帖子(管理员)")
    @PutMapping("/{id}/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> setTop(@PathVariable Long id, @RequestParam Integer isTop) {
        forumPostService.setTop(id, isTop);
        return Result.success("操作成功");
    }

    @Operation(summary = "设置精华(管理员)")
    @PutMapping("/{id}/essence")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> setEssence(@PathVariable Long id, @RequestParam Integer isEssence) {
        forumPostService.setEssence(id, isEssence);
        return Result.success("操作成功");
    }
}
