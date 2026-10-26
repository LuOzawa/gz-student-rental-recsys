package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Feedback;
import org.example.springboot.service.FeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "反馈管理接口")
@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    @Resource
    private FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping
    public Result<?> createFeedback(@RequestBody Feedback feedback) {
        return Result.success("提交成功", feedbackService.createFeedback(feedback));
    }

    @Operation(summary = "查询当前用户的反馈")
    @GetMapping("/my")
    public Result<?> getUserFeedbacks(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(feedbackService.getUserFeedbacks(currentPage, size));
    }

    @Operation(summary = "分页查询所有反馈(管理员)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAllFeedbacks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(feedbackService.getAllFeedbacks(type, status, username, currentPage, size));
    }

    @Operation(summary = "分页查询所有反馈(兼容旧接口)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAllFeedbacksCompat(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(feedbackService.getAllFeedbacks(type, status, username, currentPage, size));
    }

    @Operation(summary = "处理反馈(管理员)")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> handleFeedback(
            @PathVariable Long id,
            @RequestParam String reply,
            @RequestParam Integer status) {
        return Result.success("处理成功", feedbackService.handleFeedback(id, reply, status));
    }
}
