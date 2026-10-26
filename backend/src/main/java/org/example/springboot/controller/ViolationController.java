package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Violation;
import org.example.springboot.service.ViolationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "违规记录管理接口")
@RestController
@RequestMapping("/violations")
public class ViolationController {

    @Resource
    private ViolationService violationService;

    @Operation(summary = "用户举报房源")
    @PostMapping("/report")
    public Result<?> reportHouse(@RequestBody Violation violation) {
        return Result.success("举报成功，感谢您的反馈，我们会尽快处理", violationService.reportHouse(violation));
    }

    @Operation(summary = "创建违规记录(管理员)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> createViolation(@RequestBody Violation violation) {
        return Result.success("创建成功", violationService.createViolation(violation));
    }

    @Operation(summary = "分页查询违规记录(管理员)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getViolationsByPage(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(violationService.getViolationsByPage(userId, status, username, type, currentPage, size));
    }

    @Operation(summary = "分页查询违规记录(兼容旧接口)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getViolationsByPageCompat(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(violationService.getViolationsByPage(userId, status, username, type, currentPage, size));
    }

    @Operation(summary = "查询当前用户的违规记录")
    @GetMapping("/my")
    public Result<?> getUserViolations(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(violationService.getUserViolations(currentPage, size));
    }

    @Operation(summary = "处理违规记录(管理员)")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> handleViolation(
            @PathVariable Long id,
            @RequestParam String penalty,
            @RequestParam Integer creditDeduction,
            @RequestParam String handleRemark) {
        return Result.success("处理成功", violationService.handleViolation(id, penalty, creditDeduction, handleRemark));
    }
}
