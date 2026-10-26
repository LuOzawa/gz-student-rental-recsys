package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Appointment;
import org.example.springboot.service.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 预约看房控制器
 */
@Tag(name = "预约看房管理接口")
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Resource
    private AppointmentService appointmentService;

    @Operation(summary = "创建预约")
    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> createAppointment(@RequestBody Appointment appointment) {
        return Result.success("预约成功，请等待房东确认", appointmentService.createAppointment(appointment));
    }

    @Operation(summary = "租客查询自己的预约")
    @GetMapping("/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> getTenantAppointments(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String houseName,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(appointmentService.getTenantAppointments(status, houseName, currentPage, size));
    }

    @Operation(summary = "房东查询收到的预约")
    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD')")
    public Result<?> getLandlordAppointments(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String houseName,
            @RequestParam(required = false) String tenantName,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(appointmentService.getLandlordAppointments(status, houseName, tenantName, currentPage, size));
    }

    @Operation(summary = "管理员查询所有预约")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAllAppointments(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(appointmentService.getAllAppointments(status, currentPage, size));
    }

    @Operation(summary = "根据ID获取预约详情")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(appointmentService.getById(id));
    }

    @Operation(summary = "房东确认预约")
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('LANDLORD')")
    public Result<?> confirmAppointment(@PathVariable Long id) {
        return Result.success("预约已确认", appointmentService.confirmAppointment(id));
    }

    @Operation(summary = "房东拒绝预约")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public Result<?> rejectAppointment(@PathVariable Long id, @RequestParam String rejectReason) {
        return Result.success("已拒绝预约", appointmentService.rejectAppointment(id, rejectReason));
    }

    @Operation(summary = "完成预约")
    @PutMapping("/{id}/complete")
    public Result<?> completeAppointment(@PathVariable Long id) {
        return Result.success("预约已完成", appointmentService.completeAppointment(id));
    }

    @Operation(summary = "租客取消预约")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> cancelAppointment(@PathVariable Long id) {
        return Result.success("预约已取消", appointmentService.cancelAppointment(id));
    }

    @Operation(summary = "租客重新安排预约")
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('TENANT')")
    public Result<?> rescheduleAppointment(
            @PathVariable Long id,
            @RequestParam String newDate,
            @RequestParam String newTime) {
        LocalDate date = LocalDate.parse(newDate);
        return Result.success("预约已重新安排", appointmentService.rescheduleAppointment(id, date, newTime));
    }

    @Operation(summary = "获取房源已预约的时间段")
    @GetMapping("/booked-slots")
    public Result<?> getBookedTimeSlots(
            @RequestParam Long houseId,
            @RequestParam String date) {
        LocalDate appointmentDate = LocalDate.parse(date);
        return Result.success(appointmentService.getBookedTimeSlots(houseId, appointmentDate));
    }

    @Operation(summary = "自动过期处理(管理员)")
    @PostMapping("/admin/auto-expire")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> autoExpireAppointments() {
        int count = appointmentService.autoExpireAppointments();
        return Result.success("已处理 " + count + " 条过期预约");
    }
}
