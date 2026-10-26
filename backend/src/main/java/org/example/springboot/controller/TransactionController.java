package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Transaction;
import org.example.springboot.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "交易记录管理接口")
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    @Resource
    private TransactionService transactionService;
    
    @Operation(summary = "获取当前用户的交易记录")
    @GetMapping
    public Result<?> getUserTransactions(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(transactionService.getUserTransactions(type, currentPage, size));
    }
    
    @Operation(summary = "管理员获取所有交易记录")
    @GetMapping("/admin")
    public Result<?> getAllTransactions(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(transactionService.getAllTransactions(username, type, currentPage, size));
    }

    @Operation(summary = "获取当前用户的交易统计")
    @GetMapping("/statistics")
    public Result<?> getUserStatistics(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) Integer type) {
        return Result.success(transactionService.getUserStatistics(period, type));
    }

    @Operation(summary = "管理员获取所有交易统计")
    @GetMapping("/admin/statistics")
    public Result<?> getAdminStatistics(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String username) {
        return Result.success(transactionService.getAdminStatistics(period, type, username));
    }

    @Operation(summary = "导出当前用户交易记录到Excel")
    @GetMapping("/export/excel")
    public void exportUserTransactionsToExcel(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "month") String period,
            HttpServletResponse response) throws IOException {
        byte[] excelData = transactionService.exportUserTransactionsToExcel(type, period);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "交易记录_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    @Operation(summary = "管理员导出所有交易记录到Excel")
    @GetMapping("/admin/export/excel")
    public void exportAllTransactionsToExcel(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "month") String period,
            HttpServletResponse response) throws IOException {
        byte[] excelData = transactionService.exportAllTransactionsToExcel(username, type, period);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "所有交易记录_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }
} 