package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.ForumCategory;
import org.example.springboot.service.ForumCategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "论坛分类管理接口")
@RestController
@RequestMapping("/forum-categories")
public class ForumCategoryController {

    @Resource
    private ForumCategoryService forumCategoryService;

    @Operation(summary = "获取所有分类")
    @GetMapping
    public Result<?> getAllCategories() {
        return Result.success(forumCategoryService.getAllCategories());
    }

    @Operation(summary = "创建分类(管理员)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> createCategory(@RequestBody ForumCategory category) {
        return Result.success("创建成功", forumCategoryService.createCategory(category));
    }

    @Operation(summary = "更新分类(管理员)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateCategory(@PathVariable Long id, @RequestBody ForumCategory category) {
        return Result.success("更新成功", forumCategoryService.updateCategory(id, category));
    }

    @Operation(summary = "删除分类(管理员)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteCategory(@PathVariable Long id) {
        forumCategoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}
