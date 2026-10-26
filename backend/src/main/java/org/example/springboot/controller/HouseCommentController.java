package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.HouseComment;
import org.example.springboot.service.HouseCommentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 房源评论控制器
 */
@Tag(name = "房源评论管理接口")
@RestController
@RequestMapping("/house-comments")
public class HouseCommentController {

    @Resource
    private HouseCommentService houseCommentService;

    @Operation(summary = "创建评论")
    @PostMapping
    public Result<?> createComment(@RequestBody HouseComment comment) {
        return Result.success("评论成功", houseCommentService.createComment(comment));
    }

    @Operation(summary = "根据房源ID分页查询评论")
    @GetMapping("/house/{houseId}")
    public Result<?> getCommentsByHouseId(
            @PathVariable Long houseId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(houseCommentService.getCommentsByHouseId(houseId, currentPage, size));
    }

    @Operation(summary = "分页查询所有评论(管理员)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAllComments(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(houseCommentService.getAllComments(houseId, type, status, currentPage, size));
    }

    @Operation(summary = "分页查询房东房源的评论(房东)")
    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD')")
    public Result<?> getLandlordComments(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(houseCommentService.getLandlordComments(houseId, type, currentPage, size));
    }

    @Operation(summary = "房东/管理员回复评论")
    @PutMapping("/{id}/reply")
    public Result<?> replyComment(@PathVariable Long id, @RequestParam String replyContent) {
        return Result.success("回复成功", houseCommentService.replyComment(id, replyContent));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<?> deleteComment(@PathVariable Long id) {
        houseCommentService.deleteComment(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "隐藏/显示评论(管理员)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateCommentStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success("状态更新成功", houseCommentService.updateCommentStatus(id, status));
    }
}
