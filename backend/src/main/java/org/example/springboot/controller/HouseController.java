package org.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.service.AmapService;
import org.example.springboot.service.HouseService;
import org.example.springboot.service.UserBehaviorService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name="房屋管理接口")
@RestController
@RequestMapping("/houses")
public class HouseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HouseController.class);

    @Resource
    private HouseService houseService;

    @Resource
    private UserBehaviorService userBehaviorService;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private AmapService amapService;

    @Operation(summary = "分页查询房屋信息（支持多维度检索）")
    @GetMapping("/page")
    public Result<?> getHousesByPage(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String address,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) BigDecimal minScore,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long landLordId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "") String facilities,
            @RequestParam(required = false) Integer minCreditScore,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer decoration,
            @RequestParam(required = false) Integer minFloor,
            @RequestParam(required = false) Integer maxFloor,
            @RequestParam(required = false) Integer orientation,
            @RequestParam(required = false) Integer buildingType,
            @RequestParam(required = false) Integer paymentMethod,
            @RequestParam(required = false) Integer minBuildYear,
            @RequestParam(required = false) Integer maxBuildYear,
            @RequestParam(required = false) Integer isRecommended,
            @RequestParam(required = false) String communityName,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy) {
        // Track search behavior
        if (!title.isEmpty() || !address.isEmpty()) {
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("minPrice", minPrice);
            searchParams.put("maxPrice", maxPrice);
            searchParams.put("typeId", typeId);
            searchParams.put("city", city);
            searchParams.put("district", district);
            searchParams.put("decoration", decoration);
            searchParams.put("orientation", orientation);
            searchParams.put("buildingType", buildingType);
            searchParams.put("paymentMethod", paymentMethod);
            userBehaviorService.trackSearch(title.isEmpty() ? address : title, searchParams);
        }

        Page<House> page = houseService.getHousesByPage(
                title, address, landLordId, minPrice, maxPrice,
                minArea, maxArea, minScore, maxScore, typeId, status, facilities, minCreditScore,
                city, district, decoration, minFloor, maxFloor, orientation, buildingType,
                paymentMethod, minBuildYear, maxBuildYear, isRecommended, communityName, tags,
                currentPage, size, sortBy);
        return Result.success(page);
    }
    
    @Operation(summary = "房东获取自己的房屋列表")
    @GetMapping("/landlord")
    public Result<?> getLandlordHouses(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        // 检查当前用户是否登录以及是否是房东
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        
        Page<House> page = houseService.getLandlordHouses(title, status, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "根据ID获取房屋详情")
    @GetMapping("/{id}")
    public Result<?> getHouseById(@PathVariable Long id) {
        House house = houseService.getHouseById(id);
        // Track house view (async, don't block response)
        try {
            userBehaviorService.trackHouseView(id);
        } catch (Exception e) {
            LOGGER.warn("Failed to track house view: {}", e.getMessage());
        }
        return Result.success(house);
    }
    
    @Operation(summary = "发布新房屋")
    @PostMapping
    public Result<?> createHouse(@RequestBody House house) {
        // 检查当前用户是否登录以及角色权限
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        
        // 检查用户是否为房东或管理员
        if (!"LANDLORD".equals(currentUser.getRoleCode()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("只有房东或管理员可以发布房屋");
        }
        
        houseService.createHouse(house);
        return Result.success("发布成功");
    }
    
    @Operation(summary = "更新房屋信息")
    @PutMapping("/{id}")
    public Result<?> updateHouse(@PathVariable Long id, @RequestBody House house) {
        // 检查当前用户是否登录
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        
        houseService.updateHouse(id, house);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "修改房屋状态")
    @PutMapping("/{id}/status")
    public Result<?> updateHouseStatus(
            @PathVariable Long id, 
            @RequestParam Integer status) {
        // 检查当前用户是否登录
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        
        houseService.updateHouseStatus(id, status);
        return Result.success("状态更新成功");
    }
    
    @Operation(summary = "删除房屋")
    @DeleteMapping("/{id}")
    public Result<?> deleteHouse(@PathVariable Long id) {
        // 检查当前用户是否登录
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }

        houseService.deleteHouse(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "管理员分页查询房源(用于审核)")
    @GetMapping("/admin/audit")
    public Result<?> getHousesForAudit(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限访问");
        }

        Page<House> page = houseService.getHousesForAudit(title, auditStatus, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "审核通过房源")
    @PutMapping("/{id}/approve")
    public Result<?> approveHouse(@PathVariable Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限操作");
        }

        houseService.approveHouse(id);
        return Result.success("审核通过");
    }

    @Operation(summary = "审核拒绝房源")
    @PutMapping("/{id}/reject")
    public Result<?> rejectHouse(@PathVariable Long id, @RequestParam String reason) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限操作");
        }

        houseService.rejectHouse(id, reason);
        return Result.success("审核拒绝");
    }

    @Operation(summary = "获取房源评分排行榜")
    @GetMapping("/top-rated")
    public Result<?> getTopRatedHouses(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(houseService.getTopRatedHouses(limit));
    }

    @Operation(summary = "房东导出自己的房源列表到Excel")
    @GetMapping("/landlord/export/excel")
    public void exportLandlordHousesToExcel(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long typeId,
            HttpServletResponse response) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        byte[] excelData = houseService.exportLandlordHousesToExcel(title, status, minPrice, maxPrice, typeId);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "我的房源列表_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    @Operation(summary = "管理员导出所有房源到Excel")
    @GetMapping("/admin/export/excel")
    public void exportAllHousesToExcel(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long typeId,
            HttpServletResponse response) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return;
        }

        byte[] excelData = houseService.exportAllHousesToExcel(title, status, auditStatus, minPrice, maxPrice, typeId);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = "所有房源列表_" + System.currentTimeMillis() + ".xlsx";
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    // ==================== 地图相关接口 ====================

    @Operation(summary = "地址转坐标（地理编码）")
    @GetMapping("/geocode")
    public Result<?> geocode(@RequestParam String address, @RequestParam(required = false) String city) {
        return Result.success(amapService.geocode(address, city));
    }

    @Operation(summary = "坐标转地址（逆地理编码）")
    @GetMapping("/reverse-geocode")
    public Result<?> reverseGeocode(@RequestParam BigDecimal longitude, @RequestParam BigDecimal latitude) {
        return Result.success(amapService.reverseGeocode(longitude, latitude));
    }

    @Operation(summary = "获取房源周边设施")
    @GetMapping("/{id}/nearby-facilities")
    public Result<?> getNearbyFacilities(@PathVariable Long id) {
        House house = houseService.getHouseById(id);
        if (house == null || house.getLongitude() == null || house.getLatitude() == null) {
            return Result.error("房源信息不完整或未设置坐标");
        }
        return Result.success(amapService.searchNearbyFacilities(
            house.getLongitude(), house.getLatitude(), null
        ));
    }

    @Operation(summary = "地图范围搜索房源")
    @GetMapping("/map-search")
    public Result<?> searchByBounds(
            @RequestParam BigDecimal minLng,
            @RequestParam BigDecimal maxLng,
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLat,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(amapService.searchHousesByBounds(
            minLng, maxLng, minLat, maxLat, currentPage, size
        ));
    }

    @Operation(summary = "计算房源到指定位置的距离")
    @GetMapping("/{id}/distance")
    public Result<?> calculateDistance(
            @PathVariable Long id,
            @RequestParam BigDecimal targetLng,
            @RequestParam BigDecimal targetLat) {
        House house = houseService.getHouseById(id);
        if (house == null || house.getLongitude() == null || house.getLatitude() == null) {
            return Result.error("房源信息不完整或未设置坐标");
        }
        BigDecimal distance = amapService.calculateDistance(
            house.getLongitude(), house.getLatitude(), targetLng, targetLat
        );
        return Result.success(Map.of("distance", distance, "unit", "公里"));
    }

    @Operation(summary = "计算房源到校区的距离")
    @GetMapping("/{id}/campus-distance")
    public Result<?> calculateDistanceToCampus(@PathVariable Long id, @RequestParam String campus) {
        BigDecimal distance = amapService.calculateDistanceToCampus(id, campus);
        if (distance == null) {
            return Result.error("无法计算距离，请检查房源坐标和校区参数");
        }
        return Result.success(Map.of("campus", campus, "distance", distance, "unit", "公里"));
    }

    @Operation(summary = "获取所有校区坐标")
    @GetMapping("/campus-coordinates")
    public Result<?> getCampusCoordinates() {
        return Result.success(amapService.getCampusCoordinates());
    }

    @Operation(summary = "更新房源周边设施缓存")
    @PostMapping("/{id}/update-nearby")
    public Result<?> updateNearbyFacilities(@PathVariable Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }

        House house = houseMapper.selectById(id);
        if (house == null) {
            return Result.error("房源不存在");
        }

        // 检查权限：房东只能更新自己的房源
        if (!"ADMIN".equals(currentUser.getRoleCode()) &&
            !house.getLandlordId().equals(currentUser.getId())) {
            return Result.error("无权限操作");
        }

        amapService.updateHouseNearbyFacilities(id);
        return Result.success("周边设施更新成功");
    }
}