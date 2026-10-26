package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.service.CreditRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "信用记录管理接口")
@RestController
@RequestMapping("/credit-records")
public class CreditRecordController {

    @Resource
    private CreditRecordService creditRecordService;

    @Operation(summary = "增加信用分(管理员)")
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> addCredit(
            @RequestParam Long userId,
            @RequestParam Integer value,
            @RequestParam String reason,
            @RequestParam(required = false) Long relatedId) {
        creditRecordService.addCredit(userId, value, reason, relatedId, null);
        return Result.success("操作成功");
    }

    @Operation(summary = "扣除信用分(管理员)")
    @PostMapping("/deduct")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deductCredit(
            @RequestParam Long userId,
            @RequestParam Integer value,
            @RequestParam String reason,
            @RequestParam(required = false) Long relatedId) {
        creditRecordService.deductCredit(userId, value, reason, relatedId, null);
        return Result.success("操作成功");
    }

    @Operation(summary = "根据用户ID查询信用记录")
    @GetMapping("/user/{userId}")
    public Result<?> getCreditRecordsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(creditRecordService.getCreditRecordsByUserId(userId, currentPage, size));
    }

    @Operation(summary = "查询所有信用记录(管理员)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAllCreditRecords(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(creditRecordService.getAllCreditRecords(username, changeType, currentPage, size));
    }

    @Operation(summary = "查询所有信用记录(兼容旧接口)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAllCreditRecordsCompat(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(creditRecordService.getAllCreditRecords(username, changeType, currentPage, size));
    }
}
