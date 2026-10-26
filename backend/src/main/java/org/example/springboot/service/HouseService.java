package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.House;
import org.example.springboot.entity.HouseType;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.HouseTypeMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.ExcelExportUtils;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房屋服务实现类
 */
@Service
public class HouseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HouseService.class);
    
    @Resource
    private HouseMapper houseMapper;

    @Resource
    private HouseTypeMapper houseTypeMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;
    
    /**
     * 分页查询房屋信息
     * @param title 房屋标题
     * @param address 地址关键字
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param minArea 最小面积
     * @param maxArea 最大面积
     * @param minScore 最低评分
     * @param maxScore 最高评分
     * @param typeId 房屋类型ID
     * @param status 房屋状态
     * @param facilities 设施列表(逗号分隔)
     * @param minCreditScore 房东最低信用分
     * @param decoration 装修情况(0:毛坯,1:简装,2:精装,3:豪装)
     * @param minFloor 最低楼层
     * @param maxFloor 最高楼层
     * @param orientation 朝向
     * @param buildingType 房屋类型
     * @param paymentMethod 付款方式
     * @param minBuildYear 最早建筑年代
     * @param maxBuildYear 最晚建筑年代
     * @param isRecommended 是否推荐
     * @param communityName 小区名称
     * @param tags 房源标签
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页数据
     */
    public Page<House> getHousesByPage(String title, String address, Long landLordId, BigDecimal minPrice, BigDecimal maxPrice,
                                       BigDecimal minArea, BigDecimal maxArea, BigDecimal minScore, BigDecimal maxScore,
                                       Long typeId, Integer status, String facilities, Integer minCreditScore,
                                       String city, String district, Integer decoration, Integer minFloor, Integer maxFloor,
                                       Integer orientation, Integer buildingType, Integer paymentMethod,
                                       Integer minBuildYear, Integer maxBuildYear, Integer isRecommended,
                                       String communityName, String tags, Integer currentPage, Integer size, String sortBy) {
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        // 关键词搜索：同时匹配标题和地址（OR条件）
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.and(wrapper -> wrapper
                .like(House::getTitle, title)
                .or()
                .like(House::getAddress, title)
                .or()
                .like(House::getCommunityName, title));
        }

        // 单独的地址搜索（如果同时传了title和address参数）
        if (StringUtils.isNotBlank(address) && !address.equals(title)) {
            queryWrapper.like(House::getAddress, address);
        }

        // 价格区间
        if (minPrice != null) {
            queryWrapper.ge(House::getPrice, minPrice);
        }
        if (maxPrice != null) {
            queryWrapper.le(House::getPrice, maxPrice);
        }

        // 面积区间
        if (minArea != null) {
            queryWrapper.ge(House::getArea, minArea);
        }
        if (maxArea != null) {
            queryWrapper.le(House::getArea, maxArea);
        }

        // 评分区间
        if (minScore != null) {
            queryWrapper.ge(House::getAvgScore, minScore);
        }
        if (maxScore != null) {
            queryWrapper.le(House::getAvgScore, maxScore);
        }

        // 房屋类型
        if (typeId != null) {
            queryWrapper.eq(House::getTypeId, typeId);
        }

        // 状态
        if (status != null) {
            queryWrapper.eq(House::getStatus, status);
        }

        // 房东筛选或只显示审核通过的房源
        if(landLordId != null) {
            queryWrapper.eq(House::getLandlordId, landLordId);
        } else {
            queryWrapper.eq(House::getAuditStatus, 1);
        }

        // 城市过滤
        if (StringUtils.isNotBlank(city)) {
            queryWrapper.eq(House::getCity, city);
        }

        // 区域过滤
        if (StringUtils.isNotBlank(district)) {
            queryWrapper.eq(House::getDistrict, district);
        }

        // 装修情况
        if (decoration != null && decoration >= 0) {
            queryWrapper.eq(House::getDecoration, decoration);
        }

        // 楼层区间
        if (minFloor != null) {
            queryWrapper.ge(House::getFloor, minFloor);
        }
        if (maxFloor != null) {
            queryWrapper.le(House::getFloor, maxFloor);
        }

        // 朝向
        if (orientation != null && orientation >= 0) {
            queryWrapper.eq(House::getOrientation, orientation);
        }

        // 房屋类型（建筑类型）
        if (buildingType != null && buildingType >= 0) {
            queryWrapper.eq(House::getBuildingType, buildingType);
        }

        // 付款方式
        if (paymentMethod != null && paymentMethod >= 0) {
            queryWrapper.eq(House::getPaymentMethod, paymentMethod);
        }

        // 建筑年代区间
        if (minBuildYear != null) {
            queryWrapper.ge(House::getBuildYear, minBuildYear);
        }
        if (maxBuildYear != null) {
            queryWrapper.le(House::getBuildYear, maxBuildYear);
        }

        // 是否推荐
        if (isRecommended != null && isRecommended > 0) {
            queryWrapper.eq(House::getIsRecommended, 1)
                    .orderByAsc(House::getRecommendSort);
        }

        // 小区名称模糊搜索
        if (StringUtils.isNotBlank(communityName)) {
            queryWrapper.like(House::getCommunityName, communityName);
        }

        // 设施过滤：检查facilities JSON字段是否包含所需设施
        if (StringUtils.isNotBlank(facilities)) {
            String[] facilityArray = facilities.split(",");
            for (String facility : facilityArray) {
                queryWrapper.like(House::getFacilities, "\"" + facility.trim() + "\":true");
            }
        }

        // 排序：支持按评分、价格、创建时间排序
        if ("score".equalsIgnoreCase(sortBy)) {
            queryWrapper.orderByDesc(House::getAvgScore);
        } else if ("price_asc".equalsIgnoreCase(sortBy)) {
            queryWrapper.orderByAsc(House::getPrice);
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            queryWrapper.orderByDesc(House::getPrice);
        } else if ("recommended".equalsIgnoreCase(sortBy)) {
            queryWrapper.orderByDesc(House::getIsRecommended)
                    .orderByAsc(House::getRecommendSort)
                    .orderByDesc(House::getCreateTime);
        } else {
            queryWrapper.orderByDesc(House::getCreateTime);
        }

        Page<House> page = houseMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充房屋类型名称和房东姓名
        page.getRecords().forEach(this::fillHouseInfo);

        // 如果有房东信用分筛选，需要过滤结果
        if (minCreditScore != null && minCreditScore > 0) {
            List<House> filteredRecords = page.getRecords().stream()
                .filter(house -> {
                    User landlord = userMapper.selectById(house.getLandlordId());
                    return landlord != null && landlord.getCreditScore() != null
                           && landlord.getCreditScore() >= minCreditScore;
                })
                .collect(java.util.stream.Collectors.toList());
            page.setRecords(filteredRecords);
            page.setTotal(filteredRecords.size());
        }

        return page;
    }
    
    /**
     * 房东查询自己的房屋
     * @param title 房屋标题
     * @param status 房屋状态
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页数据
     */
    public Page<House> getLandlordHouses(String title, Integer status, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(House::getLandlordId, currentUser.getId());
        
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(House::getTitle, title);
        }
        
        if (status != null) {
            queryWrapper.eq(House::getStatus, status);
        }
        
        queryWrapper.orderByDesc(House::getCreateTime);
        
        Page<House> page = houseMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充房屋类型名称和房东姓名
        page.getRecords().forEach(this::fillHouseInfo);
        
        return page;
    }
    
    /**
     * 根据ID获取房屋详情（带缓存）
     * @param id 房屋ID
     * @return 房屋信息
     */
    @Cacheable(value = "houses", key = "#id", unless = "#result == null")
    public House getHouseById(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }

        // 填充房屋类型名称和房东姓名
        fillHouseInfo(house);

        return house;
    }
    
    /**
     * 创建房屋（清除缓存）
     * @param house 房屋信息
     */
    @CacheEvict(value = "houses", allEntries = true)
    @Transactional
    public void createHouse(House house) {
        // 获取当前登录用户作为房东
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }

        // 检查房东是否完成实名认证（管理员除外）
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            if (!userService.isUserVerified(currentUser.getId())) {
                User userInfo = userMapper.selectById(currentUser.getId());
                if (userInfo.getVerifyStatus() == null || userInfo.getVerifyStatus() == 0) {
                    throw new ServiceException("请先完成实名认证才能发布房源");
                } else if (userInfo.getVerifyStatus() == 1) {
                    throw new ServiceException("您的实名认证正在审核中，请耐心等待");
                } else if (userInfo.getVerifyStatus() == 3) {
                    String reason = userInfo.getVerifyRejectReason() != null ? userInfo.getVerifyRejectReason() : "请认真填写信息";
                    throw new ServiceException("您的实名认证未通过：" + reason);
                }
            }
        }

        // 检查房屋类型是否存在
        HouseType houseType = houseTypeMapper.selectById(house.getTypeId());
        if (houseType == null) {
            throw new ServiceException("所选房屋类型不存在");
        }
        
        // 设置房东ID和初始状态
        house.setLandlordId(currentUser.getId());
        house.setStatus(0); // 初始设置为下架状态，审核通过后自动设置为待出租
        house.setAuditStatus(0); // ⭐ 初始审核状态为待审核
        house.setAvgScore(BigDecimal.ZERO); // 初始化平均评分
        house.setCommentCount(0); // 初始化评论数量

        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        house.setCreateTime(now);
        house.setUpdateTime(now);

        int result = houseMapper.insert(house);
        if (result <= 0) {
            throw new ServiceException("创建房屋失败");
        }

        LOGGER.info("房东 {} 发布新房源，房源ID：{}，等待管理员审核", currentUser.getUsername(), house.getId());
    }
    
    /**
     * 更新房屋信息（清除缓存）
     * @param id 房屋ID
     * @param house 房屋信息
     */
    @CacheEvict(value = "houses", key = "#id")
    @Transactional
    public void updateHouse(Long id, House house) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋是否存在
        House existingHouse = houseMapper.selectById(id);
        if (existingHouse == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 校验房屋类型是否存在
        if (house.getTypeId() != null && !house.getTypeId().equals(existingHouse.getTypeId())) {
            HouseType houseType = houseTypeMapper.selectById(house.getTypeId());
            if (houseType == null) {
                throw new ServiceException("所选房屋类型不存在");
            }
        }
        
        // 检查权限：只有管理员或房东可以修改
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !currentUser.getId().equals(existingHouse.getLandlordId())) {
            throw new ServiceException("无权修改该房屋信息");
        }
        
        // 设置ID和更新时间，保留创建时间和房东ID
        house.setId(id);
        house.setUpdateTime(LocalDateTime.now());
        house.setCreateTime(existingHouse.getCreateTime());
        house.setLandlordId(existingHouse.getLandlordId());
        
        int result = houseMapper.updateById(house);
        if (result <= 0) {
            throw new ServiceException("更新房屋信息失败");
        }
    }
    
    /**
     * 修改房屋状态（清除缓存）
     * @param id 房屋ID
     * @param status 状态(0:下架,1:待出租,2:已出租)
     */
    @CacheEvict(value = "houses", key = "#id")
    @Transactional
    public void updateHouseStatus(Long id, Integer status) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋是否存在
        House existingHouse = houseMapper.selectById(id);
        if (existingHouse == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 检查权限：只有管理员或房东可以修改
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !currentUser.getId().equals(existingHouse.getLandlordId())) {
            throw new ServiceException("无权修改该房屋状态");
        }
        
        // 检查状态值是否有效
        if (status < 0 || status > 2) {
            throw new ServiceException("无效的房屋状态值");
        }
        
        House house = new House();
        house.setId(id);
        house.setStatus(status);
        house.setUpdateTime(LocalDateTime.now());
        
        int result = houseMapper.updateById(house);
        if (result <= 0) {
            throw new ServiceException("更新房屋状态失败");
        }
    }
    
    /**
     * 删除房屋（清除缓存）
     * @param id 房屋ID
     */
    @CacheEvict(value = "houses", key = "#id")
    @Transactional
    public void deleteHouse(Long id) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查房屋是否存在
        House existingHouse = houseMapper.selectById(id);
        if (existingHouse == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 检查权限：只有管理员或房东可以删除
        if (!"ADMIN".equals(currentUser.getRoleCode()) && !currentUser.getId().equals(existingHouse.getLandlordId())) {
            throw new ServiceException("无权删除该房屋");
        }
        
        // 已出租状态的房屋不能删除
        if (existingHouse.getStatus() == 2) {
            throw new ServiceException("已出租的房屋不能删除");
        }
        
        int result = houseMapper.deleteById(id);
        if (result <= 0) {
            throw new ServiceException("删除房屋失败");
        }
    }
    
    /**
     * 填充房屋类型名称、房东姓名、装修和朝向名称
     * @param house 房屋信息
     */
    private void fillHouseInfo(House house) {
        if (house == null) {
            return;
        }

        // 填充房屋类型名称
        if (house.getTypeId() != null) {
            HouseType houseType = houseTypeMapper.selectById(house.getTypeId());
            if (houseType != null) {
                house.setTypeName(houseType.getName());
            }
        }

        // 填充房东姓名和信用分
        if (house.getLandlordId() != null) {
            User landlord = userMapper.selectById(house.getLandlordId());
            if (landlord != null) {
                house.setLandlordName(landlord.getName());
                house.setLandlordImg(landlord.getAvatar());
                house.setLandlordCreditScore(landlord.getCreditScore());
            }
        }

        // 填充装修情况名称
        if (house.getDecoration() != null) {
            String[] decorationNames = {"毛坯", "简装", "精装", "豪装"};
            if (house.getDecoration() >= 0 && house.getDecoration() < decorationNames.length) {
                house.setDecorationName(decorationNames[house.getDecoration()]);
            }
        }

        // 填充朝向名称
        if (house.getOrientation() != null) {
            String[] orientationNames = {"东", "南", "西", "北", "东南", "西南", "东北", "西北", "南北通透"};
            if (house.getOrientation() >= 0 && house.getOrientation() < orientationNames.length) {
                house.setOrientationName(orientationNames[house.getOrientation()]);
            }
        }
    }

    /**
     * 审核通过房源（清除缓存）
     * @param id 房源ID
     */
    @CacheEvict(value = "houses", key = "#id")
    @Transactional
    public void approveHouse(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new ServiceException("房源不存在");
        }

        if (house.getAuditStatus() != 0) {
            throw new ServiceException("该房源已审核");
        }

        house.setAuditStatus(1); // 审核通过
        house.setAuditReason(null);
        house.setStatus(1); // 自动设置为待出租状态
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);

        LOGGER.info("房源审核通过，房源ID：{}", id);
    }

    /**
     * 审核拒绝房源（清除缓存）
     * @param id 房源ID
     * @param reason 拒绝原因
     */
    @CacheEvict(value = "houses", key = "#id")
    @Transactional
    public void rejectHouse(Long id, String reason) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new ServiceException("房源不存在");
        }

        if (house.getAuditStatus() != 0) {
            throw new ServiceException("该房源已审核");
        }

        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("拒绝原因不能为空");
        }

        house.setAuditStatus(2); // 审核拒绝
        house.setAuditReason(reason);
        house.setStatus(0); // 自动下架
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);

        LOGGER.info("房源审核拒绝，房源ID：{}，原因：{}", id, reason);
    }

    /**
     * 管理员分页查询房源(用于审核)
     * @param title 房屋标题
     * @param auditStatus 审核状态
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页数据
     */
    public Page<House> getHousesForAudit(String title, Integer auditStatus, Integer currentPage, Integer size) {
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(House::getTitle, title);
        }

        if (auditStatus != null) {
            queryWrapper.eq(House::getAuditStatus, auditStatus);
        }

        queryWrapper.orderByDesc(House::getCreateTime);

        Page<House> page = houseMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充房屋类型名称和房东姓名
        page.getRecords().forEach(this::fillHouseInfo);

        return page;
    }

    /**
     * 获取房源评分排行榜
     * @param limit 返回数量
     * @return 房源列表
     */
    public List<House> getTopRatedHouses(Integer limit) {
        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(House::getStatus, 1) // 只查询待出租的房源
                    .eq(House::getAuditStatus, 1) // 只查询审核通过的房源
                    .gt(House::getCommentCount, 0) // 有评论的房源
                    .orderByDesc(House::getAvgScore)
                    .orderByDesc(House::getCommentCount)
                    .last("LIMIT " + (limit != null && limit > 0 ? limit : 10));

        List<House> houses = houseMapper.selectList(queryWrapper);
        houses.forEach(this::fillHouseInfo);

        return houses;
    }

    /**
     * 导出房东的房源列表到Excel
     */
    public byte[] exportLandlordHousesToExcel(String title, Integer status,
            BigDecimal minPrice, BigDecimal maxPrice, Long typeId) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(House::getLandlordId, currentUser.getId());

        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(House::getTitle, title);
        }

        if (status != null) {
            queryWrapper.eq(House::getStatus, status);
        }

        if (minPrice != null) {
            queryWrapper.ge(House::getPrice, minPrice);
        }

        if (maxPrice != null) {
            queryWrapper.le(House::getPrice, maxPrice);
        }

        if (typeId != null) {
            queryWrapper.eq(House::getTypeId, typeId);
        }

        queryWrapper.orderByDesc(House::getCreateTime);

        List<House> houses = houseMapper.selectList(queryWrapper);

        // 填充房屋类型名称和房东姓名
        houses.forEach(this::fillHouseInfo);

        return ExcelExportUtils.exportHousesToExcel(houses);
    }

    /**
     * 管理员导出所有房源到Excel
     */
    public byte[] exportAllHousesToExcel(String title, Integer status, Integer auditStatus,
            BigDecimal minPrice, BigDecimal maxPrice, Long typeId) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限访问");
        }

        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(House::getTitle, title);
        }

        if (status != null) {
            queryWrapper.eq(House::getStatus, status);
        }

        if (auditStatus != null) {
            queryWrapper.eq(House::getAuditStatus, auditStatus);
        }

        if (minPrice != null) {
            queryWrapper.ge(House::getPrice, minPrice);
        }

        if (maxPrice != null) {
            queryWrapper.le(House::getPrice, maxPrice);
        }

        if (typeId != null) {
            queryWrapper.eq(House::getTypeId, typeId);
        }

        queryWrapper.orderByDesc(House::getCreateTime);

        List<House> houses = houseMapper.selectList(queryWrapper);

        // 填充房屋类型名称和房东姓名
        houses.forEach(this::fillHouseInfo);

        return ExcelExportUtils.exportHousesToExcel(houses);
    }
} 