package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.User;
import org.example.springboot.service.OrderService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "订单管理接口")
@RestController
@RequestMapping("/orders")
public class OrderController {
    @Resource
    private OrderService orderService;
    
    @Operation(summary = "创建订单")
    @PostMapping
    public Result<?> createOrder(@RequestBody Order order) {
        return Result.success(orderService.createOrder(order));
    }
    
    @Operation(summary = "支付订单")
    @PutMapping("/{id}/pay")
    public Result<?> payOrder(@PathVariable Long id, @RequestParam String paymentMethod) {
        return Result.success(orderService.payOrder(id, paymentMethod));
    }
    
    @Operation(summary = "确认订单（房东确认）")
    @PutMapping("/{id}/confirm")
    public Result<?> confirmOrder(@PathVariable Long id) {
        return Result.success(orderService.confirmOrder(id));
    }
    
    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<?> cancelOrder(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(id));
    }
    
    @Operation(summary = "申请退款")
    @PostMapping("/{id}/refund")
    public Result<?> refundOrder(@PathVariable Long id) {
        return Result.success(orderService.refundOrder(id));
    }
    
    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<?> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }
    
    @Operation(summary = "租客获取自己的订单")
    @GetMapping("/tenant")
    public Result<?> getTenantOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getTenantOrders(orderNo, status, currentPage, size));
    }
    
    @Operation(summary = "房东获取自己的订单")
    @GetMapping("/landlord")
    public Result<?> getLandlordOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getLandlordOrders(orderNo, status, currentPage, size));
    }
    
    @Operation(summary = "管理员获取所有订单")
    @GetMapping("/admin")
    public Result<?> getAllOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String tenantUsername,
            @RequestParam(required = false) String landlordUsername,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.getAllOrders(orderNo, status, tenantUsername, landlordUsername, currentPage, size));
    }

    @Operation(summary = "房东导出自己的订单到Excel")
    @GetMapping("/landlord/export/excel")
    public void exportLandlordOrdersToExcel(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        byte[] excelData = orderService.exportLandlordOrdersToExcel(orderNo, status);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "我的订单列表_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    @Operation(summary = "租客导出自己的订单到Excel")
    @GetMapping("/tenant/export/excel")
    public void exportTenantOrdersToExcel(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        byte[] excelData = orderService.exportTenantOrdersToExcel(orderNo, status);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "我的订单列表_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    @Operation(summary = "管理员导出所有订单到Excel")
    @GetMapping("/admin/export/excel")
    public void exportAllOrdersToExcel(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String tenantUsername,
            @RequestParam(required = false) String landlordUsername,
            HttpServletResponse response) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return;
        }

        byte[] excelData = orderService.exportAllOrdersToExcel(orderNo, status, tenantUsername, landlordUsername);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "所有订单列表_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    @Operation(summary = "检查租约是否可以续签")
    @GetMapping("/check-renewal/{leaseId}")
    public Result<?> checkRenewal(@PathVariable Long leaseId) {
        boolean canRenew = orderService.canRenewLease(leaseId);
        return Result.success(canRenew);
    }

    @Operation(summary = "创建续签订单")
    @PostMapping("/renewal/{leaseId}")
    public Result<?> createRenewalOrder(
            @PathVariable Long leaseId,
            @RequestBody Map<String, Object> request) {
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String paymentCycle = request.get("paymentCycle").toString();
        Order order = orderService.createRenewalOrder(leaseId, amount, paymentCycle);
        return Result.success("续签订单创建成功", order);
    }

    @Operation(summary = "确认续签订单")
    @PutMapping("/renewal/{orderId}/confirm")
    public Result<?> confirmRenewalOrder(@PathVariable Long orderId) {
        Order order = orderService.confirmRenewalOrder(orderId);
        return Result.success("续签订单确认成功", order);
    }
} 