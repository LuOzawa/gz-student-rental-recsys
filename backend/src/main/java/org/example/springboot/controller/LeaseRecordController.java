package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.entity.LeaseRecord;
import org.example.springboot.entity.User;
import org.example.springboot.service.LeaseRecordService;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/lease-records")
@Tag(name = "租赁记录管理接口")
public class LeaseRecordController {

    @Resource
    private LeaseRecordService leaseRecordService;

    private static final String CONTRACT_BASE_PATH = "files/contract/";

    @Operation(summary = "根据ID获取租赁记录")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        LeaseRecord leaseRecord = leaseRecordService.getById(id);
        return Result.success(leaseRecord);
    }

    @Operation(summary = "根据订单ID获取租赁记录")
    @GetMapping("/order/{orderId}")
    public Result<?> getByOrderId(@PathVariable Long orderId) {
        LeaseRecord leaseRecord = leaseRecordService.getByOrderId(orderId);
        return Result.success(leaseRecord);
    }

    @Operation(summary = "管理员分页查询租赁记录")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getLeaseRecordsByPage(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<LeaseRecord> page = leaseRecordService.getLeaseRecordsByPage(currentPage, size, status);
        return Result.success(page);
    }

    @Operation(summary = "房东分页查询租赁记录")
    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD')")
    public Result<?> getLandlordLeaseRecordsByPage(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<LeaseRecord> page = leaseRecordService.getLandlordLeaseRecordsByPage(currentPage, size, status);
        return Result.success(page);
    }

    @Operation(summary = "租客分页查询租赁记录")
    @GetMapping("/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> getTenantLeaseRecordsByPage(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<LeaseRecord> page = leaseRecordService.getTenantLeaseRecordsByPage(currentPage, size, status);
        return Result.success(page);
    }

    @Operation(summary = "提交租赁评价", description = "租客对租赁进行评价，包括房屋评分和房东信用评分")
    @PostMapping("/{id}/evaluation")
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> submitEvaluation(
            @PathVariable Long id,
            @RequestParam Integer houseScore,
            @RequestParam Integer landlordCreditScore,
            @RequestParam(required = false) String content) {
        if (houseScore < 1 || houseScore > 5) {
            return Result.error("房屋评分应在1-5之间");
        }
        if (landlordCreditScore < 1 || landlordCreditScore > 5) {
            return Result.error("房东信用评分应在1-5之间");
        }
        LeaseRecord leaseRecord = leaseRecordService.submitEvaluation(id, houseScore, landlordCreditScore, content);
        return Result.success(leaseRecord);
    }

    @Operation(summary = "下载租赁合同")
    @GetMapping("/{id}/download-contract")
    public void downloadContract(@PathVariable Long id, HttpServletResponse response) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        // 获取租赁记录
        LeaseRecord leaseRecord = leaseRecordService.getById(id);
        if (leaseRecord == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // 检查权限（只有管理员、房东和对应的租户可以下载合同）
        String roleCode = currentUser.getRoleCode();
        if (!"ADMIN".equals(roleCode) 
            && !leaseRecord.getLandlordId().equals(currentUser.getId())
            && !leaseRecord.getTenantId().equals(currentUser.getId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        // 检查合同是否存在
        if (StringUtils.isEmpty(leaseRecord.getContractUrl())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // 获取文件路径
        String contractPath = "files" + leaseRecord.getContractUrl();
        File file = new File(contractPath);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // 设置响应头
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=contract_" + id + ".pdf");
        
        try {
            // 将文件内容写入响应流
            Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载合同文件失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "生成租赁合同", description = "根据租赁记录ID生成租赁合同")
    @PostMapping("/{id}/generate-contract")
    public Result<?> generateContract(@PathVariable Long id) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        // 获取租赁记录
        LeaseRecord leaseRecord = leaseRecordService.getById(id);
        if (leaseRecord == null) {
            return Result.error("租赁记录不存在");
        }
        
        // 检查权限（只有管理员和房东可以生成合同）
        String roleCode = currentUser.getRoleCode();
        if (!"ADMIN".equals(roleCode) && !leaseRecord.getLandlordId().equals(currentUser.getId())) {
            return Result.error("您没有权限生成该合同");
        }
        
        // 生成合同
        String contractUrl = leaseRecordService.generateContract(id);
        if (StringUtils.isEmpty(contractUrl)) {
            return Result.error("合同生成失败");
        }

        return Result.success("合同生成成功", contractUrl);
    }

    @Operation(summary = "续签合同", description = "根据租赁记录ID续签合同")
    @PostMapping("/{id}/renew")
    public Result<?> renewLease(@PathVariable Long id, @RequestParam Integer months) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }

        // 续签合同
        LeaseRecord newLease = leaseRecordService.renewLease(id, months);
        if (newLease == null) {
            return Result.error("续签失败");
        }

        return Result.success("续签成功", newLease);
    }

    @Operation(summary = "自动结束过期租赁(管理员)", description = "自动结束所有过期的租赁记录")
    @PostMapping("/admin/auto-end-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> autoEndExpiredLeases() {
        int count = leaseRecordService.autoEndExpiredLeases();
        return Result.success("已处理 " + count + " 条过期租赁记录");
    }

    @Operation(summary = "获取租客即将到期的租赁", description = "获取租客即将到期的租赁记录")
    @GetMapping("/tenant/expiring-soon")
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> getTenantExpiringSoonLeases(@RequestParam(defaultValue = "30") int days) {
        return Result.success(leaseRecordService.getTenantExpiringSoonLeases(days));
    }

    @Operation(summary = "获取房东即将到期的租赁", description = "获取房东即将到期的租赁记录")
    @GetMapping("/landlord/expiring-soon")
    @PreAuthorize("hasRole('LANDLORD')")
    public Result<?> getLandlordExpiringSoonLeases(@RequestParam(defaultValue = "30") int days) {
        return Result.success(leaseRecordService.getLandlordExpiringSoonLeases(days));
    }

    @Operation(summary = "管理员获取即将到期的租赁", description = "获取所有即将到期的租赁记录")
    @GetMapping("/admin/expiring-soon")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getExpiringSoonLeases(@RequestParam(defaultValue = "30") int days) {
        return Result.success(leaseRecordService.getExpiringSoonLeases(days));
    }

    @Operation(summary = "检查租赁是否可以续签", description = "检查指定租赁记录是否可以续签")
    @GetMapping("/{id}/can-renew")
    public Result<?> canRenewLease(@PathVariable Long id) {
        boolean canRenew = leaseRecordService.canRenewLease(id);
        return Result.success(canRenew);
    }

    @Operation(summary = "获取租赁剩余天数", description = "获取指定租赁记录的剩余天数")
    @GetMapping("/{id}/days-remaining")
    public Result<?> getDaysRemaining(@PathVariable Long id) {
        long days = leaseRecordService.getDaysRemaining(id);
        return Result.success(days);
    }

    @Operation(summary = "提前结束租赁(退租)", description = "租客或房东可以提前结束租赁")
    @PostMapping("/{id}/terminate")
    public Result<?> terminateLease(@PathVariable Long id, @RequestParam(required = false) String reason) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }

        LeaseRecord lease = leaseRecordService.terminateLease(id, reason);
        return Result.success("已成功退租", lease);
    }
} 