package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.*;
import org.example.springboot.enumClass.TransactionType;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CreditRecordMapper;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.LeaseRecordMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.TransactionMapper;
import org.example.springboot.mapper.UserMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.example.springboot.util.ContractGenerator;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class LeaseRecordService {

    @Resource
    private LeaseRecordMapper leaseRecordMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private CreditRecordMapper creditRecordMapper;

    @Resource
    private ContractGenerator contractGenerator;

    @Value("${contract.default.duration:12}")
    private int defaultContractDuration; // 默认合同期限（月）

    /**
     * 根据订单ID查询租赁记录
     */
    public LeaseRecord getByOrderId(Long orderId) {
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getOrderId, orderId);
        LeaseRecord leaseRecord = leaseRecordMapper.selectOne(queryWrapper);
        
        if (leaseRecord != null) {
            // 加载关联信息
            loadAssociatedInfo(leaseRecord);
        }
        
        return leaseRecord;
    }

    /**
     * 根据ID查询租赁记录
     */
    public LeaseRecord getById(Long id) {
        LeaseRecord leaseRecord = leaseRecordMapper.selectById(id);
        if (leaseRecord != null) {
            // 加载关联信息
            loadAssociatedInfo(leaseRecord);
        }
        return leaseRecord;
    }

    /**
     * 当订单确认后，创建租赁记录并生成合同
     */
    @Transactional
    public LeaseRecord createLeaseRecord(Order order) {
        // 查询相关信息
        House house = houseMapper.selectById(order.getHouseId());
        User tenant = userMapper.selectById(order.getTenantId());
        User landlord = userMapper.selectById(order.getLandlordId());
        
        if (house == null || tenant == null || landlord == null) {
            throw new ServiceException("创建租赁记录失败：缺少必要信息");
        }
        
        // 创建租赁记录
        LeaseRecord leaseRecord = new LeaseRecord();
        leaseRecord.setOrderId(order.getId());
        leaseRecord.setHouseId(house.getId());
        leaseRecord.setTenantId(tenant.getId());
        leaseRecord.setLandlordId(landlord.getId());
        
        // 设置租期
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(defaultContractDuration);
        leaseRecord.setStartDate(startDate);
        leaseRecord.setEndDate(endDate);
        
        // 设置租金和支付周期
        leaseRecord.setRentAmount(house.getPrice());
        leaseRecord.setPaymentCycle("MONTHLY"); // 默认月付
        
        // 设置初始状态为租赁中
        leaseRecord.setStatus(1);
        
        // 生成合同
        String contractUrl = contractGenerator.generateContract(leaseRecord, order, house, tenant, landlord);
        leaseRecord.setContractUrl(contractUrl);
        
        // 保存租赁记录
        leaseRecordMapper.insert(leaseRecord);
        
        return leaseRecord;
    }

    /**
     * 分页查询租赁记录（管理员）
     */
    public Page<LeaseRecord> getLeaseRecordsByPage(Integer currentPage, Integer size, Integer status) {
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            queryWrapper.eq(LeaseRecord::getStatus, status);
        }

        // 按创建时间降序排序
        queryWrapper.orderByDesc(LeaseRecord::getCreateTime);

        Page<LeaseRecord> page = leaseRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 加载关联信息
        for (LeaseRecord record : page.getRecords()) {
            loadAssociatedInfo(record);
        }
        
        return page;
    }

    /**
     * 分页查询租赁记录（房东）
     */
    public Page<LeaseRecord> getLandlordLeaseRecordsByPage(Integer currentPage, Integer size, Integer status) {
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        if (userId == null) {
            throw new ServiceException("获取当前用户信息失败");
        }

        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getLandlordId, userId);

        if (status != null) {
            queryWrapper.eq(LeaseRecord::getStatus, status);
        }

        // 按创建时间降序排序
        queryWrapper.orderByDesc(LeaseRecord::getCreateTime);

        Page<LeaseRecord> page = leaseRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 加载关联信息
        for (LeaseRecord record : page.getRecords()) {
            loadAssociatedInfo(record);
        }
        
        return page;
    }

    /**
     * 分页查询租赁记录（租客）
     */
    public Page<LeaseRecord> getTenantLeaseRecordsByPage(Integer currentPage, Integer size, Integer status) {
        Long userId = JwtTokenUtils.getCurrentUser().getId();
        if (userId == null) {
            throw new ServiceException("获取当前用户信息失败");
        }

        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getTenantId, userId);

        if (status != null) {
            queryWrapper.eq(LeaseRecord::getStatus, status);
        }

        // 按创建时间降序排序，确保最新的租赁记录显示在前面
        queryWrapper.orderByDesc(LeaseRecord::getCreateTime);

        Page<LeaseRecord> page = leaseRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 加载关联信息
        for (LeaseRecord record : page.getRecords()) {
            loadAssociatedInfo(record);
        }
        
        return page;
    }

    /**
     * 提交租赁评价
     * @param id 租赁记录ID
     * @param houseScore 房屋评分(1-5)
     * @param landlordCreditScore 房东信用评分(1-5)
     * @param content 评价内容
     */
    @Transactional
    public LeaseRecord submitEvaluation(Long id, Integer houseScore, Integer landlordCreditScore, String content) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }

        LeaseRecord leaseRecord = leaseRecordMapper.selectById(id);
        if (leaseRecord == null) {
            throw new ServiceException("租赁记录不存在");
        }

        // 验证是否是该租赁记录的租客
        if (!leaseRecord.getTenantId().equals(currentUser.getId())) {
            throw new ServiceException("您无权评价此租赁记录");
        }

        // 检查是否已评价
        if (leaseRecord.getEvaluationScore() != null) {
            throw new ServiceException("您已评价过此租赁记录");
        }

        // 验证评分范围
        if (houseScore == null || houseScore < 1 || houseScore > 5) {
            throw new ServiceException("房屋评分必须在1-5之间");
        }
        if (landlordCreditScore == null || landlordCreditScore < 1 || landlordCreditScore > 5) {
            throw new ServiceException("房东信用评分必须在1-5之间");
        }

        // 更新评价信息
        leaseRecord.setEvaluationScore(houseScore);
        leaseRecord.setLandlordCreditScore(landlordCreditScore);
        leaseRecord.setEvaluationContent(content);
        leaseRecord.setUpdateTime(LocalDateTime.now());
        leaseRecordMapper.updateById(leaseRecord);

        // 更新房屋平均评分
        updateHouseAvgScore(leaseRecord.getHouseId());

        // 更新房东信用分
        updateLandlordCreditScore(leaseRecord.getLandlordId(), landlordCreditScore, leaseRecord.getId(), currentUser.getId());

        log.info("租客 {} 评价租赁记录 {}，房屋评分: {}，房东信用评分: {}",
                currentUser.getUsername(), id, houseScore, landlordCreditScore);

        return leaseRecord;
    }

    /**
     * 更新房屋平均评分
     * 基于所有租赁评价的 evaluationScore 计算
     */
    private void updateHouseAvgScore(Long houseId) {
        // 查询该房屋所有有评分的租赁记录
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getHouseId, houseId)
                .isNotNull(LeaseRecord::getEvaluationScore);

        List<LeaseRecord> leaseRecords = leaseRecordMapper.selectList(queryWrapper);

        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return;
        }

        if (leaseRecords.isEmpty()) {
            house.setAvgScore(BigDecimal.ZERO);
            house.setCommentCount(0);
        } else {
            // 计算平均评分
            double avgScore = leaseRecords.stream()
                    .mapToInt(LeaseRecord::getEvaluationScore)
                    .average()
                    .orElse(0.0);

            house.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
            house.setCommentCount(leaseRecords.size());
        }

        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);

        log.info("更新房屋 {} 平均评分: {}, 评价数: {}", houseId, house.getAvgScore(), house.getCommentCount());
    }

    /**
     * 更新房东信用分
     * 计算逻辑：基于所有租客对该房东的信用评分，计算加权平均值
     * 信用分 = 基础分(100) + (平均信用评分 - 3) * 权重系数(10)
     * 例如：平均评分5分 -> 100 + (5-3)*10 = 120
     *      平均评分3分 -> 100 + (3-3)*10 = 100
     *      平均评分1分 -> 100 + (1-3)*10 = 80
     * 信用分范围：60-120
     */
    private void updateLandlordCreditScore(Long landlordId, Integer newCreditScore, Long leaseId, Long operatorId) {
        // 查询该房东所有有信用评分的租赁记录
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getLandlordId, landlordId)
                .isNotNull(LeaseRecord::getLandlordCreditScore);

        List<LeaseRecord> leaseRecords = leaseRecordMapper.selectList(queryWrapper);

        User landlord = userMapper.selectById(landlordId);
        if (landlord == null || !"LANDLORD".equals(landlord.getRoleCode())) {
            return;
        }

        int oldCreditScore = landlord.getCreditScore() != null ? landlord.getCreditScore() : 100;

        if (leaseRecords.isEmpty()) {
            landlord.setCreditScore(100); // 无评价时保持基础分
        } else {
            // 计算平均信用评分
            double avgCreditRating = leaseRecords.stream()
                    .mapToInt(LeaseRecord::getLandlordCreditScore)
                    .average()
                    .orElse(3.0);

            // 计算信用分：基础分100 + (平均评分-3) * 10
            int calculatedScore = (int) Math.round(100 + (avgCreditRating - 3) * 10);

            // 限制信用分范围在60-120之间
            calculatedScore = Math.max(60, Math.min(120, calculatedScore));

            landlord.setCreditScore(calculatedScore);
        }

        landlord.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(landlord);

        // 记录信用变更
        int changeValue = landlord.getCreditScore() - oldCreditScore;
        if (changeValue != 0) {
            CreditRecord creditRecord = new CreditRecord();
            creditRecord.setUserId(landlordId);
            creditRecord.setChangeType(changeValue > 0 ? "增加" : "扣除");
            creditRecord.setChangeValue(changeValue);
            creditRecord.setReason("租客评价，信用评分: " + newCreditScore + " 分");
            creditRecord.setRelatedId(leaseId);
            creditRecord.setOperatorId(operatorId);
            creditRecord.setCreateTime(LocalDateTime.now());
            creditRecordMapper.insert(creditRecord);
        }

        log.info("更新房东 {} 信用分: {} -> {}", landlordId, oldCreditScore, landlord.getCreditScore());
    }

    /**
     * 加载租赁记录关联信息
     */
    private void loadAssociatedInfo(LeaseRecord leaseRecord) {
        // 加载房屋信息
        House house = houseMapper.selectById(leaseRecord.getHouseId());
        leaseRecord.setHouse(house);

        // 加载租客信息
        User tenant = userMapper.selectById(leaseRecord.getTenantId());
        leaseRecord.setTenant(tenant);

        // 加载房东信息
        User landlord = userMapper.selectById(leaseRecord.getLandlordId());
        leaseRecord.setLandlord(landlord);

        // 加载订单信息
        Order order = orderMapper.selectById(leaseRecord.getOrderId());
        leaseRecord.setOrder(order);

        // 检查是否已续签（是否存在以当前租约为原租约的续签记录）
        LambdaQueryWrapper<LeaseRecord> renewalQuery = new LambdaQueryWrapper<>();
        renewalQuery.eq(LeaseRecord::getOriginalLeaseId, leaseRecord.getId());
        Long renewalCount = leaseRecordMapper.selectCount(renewalQuery);
        leaseRecord.setHasRenewal(renewalCount > 0);
    }

    /**
     * 为租赁记录手动生成合同
     */
    @Transactional
    public LeaseRecord generateContractForLeaseRecord(Long id) {
        LeaseRecord leaseRecord = leaseRecordMapper.selectById(id);
        if (leaseRecord == null) {
            throw new ServiceException("租赁记录不存在");
        }
        
        // 查询相关信息
        House house = houseMapper.selectById(leaseRecord.getHouseId());
        User tenant = userMapper.selectById(leaseRecord.getTenantId());
        User landlord = userMapper.selectById(leaseRecord.getLandlordId());
        Order order = orderMapper.selectById(leaseRecord.getOrderId());
        
        if (house == null || tenant == null || landlord == null || order == null) {
            throw new ServiceException("生成合同失败：缺少必要信息");
        }
        
        // 生成合同
        String contractUrl = contractGenerator.generateContract(leaseRecord, order, house, tenant, landlord);
        if (contractUrl == null) {
            throw new ServiceException("合同生成失败，请稍后重试");
        }
        
        // 更新租赁记录的合同URL
        leaseRecord.setContractUrl(contractUrl);
        leaseRecordMapper.updateById(leaseRecord);
        
        log.info("手动生成合同成功，租赁记录ID：{}，合同URL：{}", id, contractUrl);
        
        return leaseRecord;
    }

    /**
     * 生成租赁合同并返回URL
     */
    public String generateContract(Long id) {
        LeaseRecord leaseRecord = generateContractForLeaseRecord(id);
        return leaseRecord != null ? leaseRecord.getContractUrl() : null;
    }

    /**
     * 续签合同
     */
    @Transactional
    public LeaseRecord renewLease(Long id, Integer months) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 获取原租赁记录
        LeaseRecord originalLease = leaseRecordMapper.selectById(id);
        if (originalLease == null) {
            throw new ServiceException("租赁记录不存在");
        }

        // 检查权限（租客或房东都可以发起续签）
        if (!originalLease.getTenantId().equals(currentUser.getId())
            && !originalLease.getLandlordId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限续签该合同");
        }

        // 检查租赁状态
        if (originalLease.getStatus() != 1) {
            throw new ServiceException("只有租赁中的合同才能续签");
        }

        // 检查是否已经续签过
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getOriginalLeaseId, id);
        Long count = leaseRecordMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("该合同已经续签过");
        }

        if (months == null || months <= 0) {
            throw new ServiceException("续签月数必须大于0");
        }

        // 创建新的租赁记录
        LeaseRecord newLease = new LeaseRecord();
        newLease.setOrderId(originalLease.getOrderId());
        newLease.setHouseId(originalLease.getHouseId());
        newLease.setTenantId(originalLease.getTenantId());
        newLease.setLandlordId(originalLease.getLandlordId());
        newLease.setStartDate(originalLease.getEndDate().plusDays(1)); // 从原合同结束后的第二天开始
        newLease.setEndDate(originalLease.getEndDate().plusMonths(months));
        newLease.setRentAmount(originalLease.getRentAmount());
        newLease.setPaymentCycle(originalLease.getPaymentCycle());
        newLease.setStatus(1); // 租赁中
        newLease.setIsRenewal(1); // 标记为续签
        newLease.setOriginalLeaseId(id); // 记录原租约ID

        leaseRecordMapper.insert(newLease);

        // 生成新的合同
        try {
            generateContractForLeaseRecord(newLease.getId());
        } catch (Exception e) {
            log.error("续签合同生成失败", e);
        }

        log.info("用户 {} 续签合同成功，原租赁记录ID：{}，新租赁记录ID：{}",
                currentUser.getUsername(), id, newLease.getId());

        return newLease;
    }

    /**
     * 自动结束过期的租赁记录
     * 此方法可以被定时任务调用
     */
    @Transactional
    public int autoEndExpiredLeases() {
        // 查询所有过期的租赁中的记录
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(LeaseRecord::getEndDate, LocalDate.now())
                .eq(LeaseRecord::getStatus, 1); // 租赁中

        List<LeaseRecord> expiredLeases = leaseRecordMapper.selectList(queryWrapper);

        int count = 0;
        for (LeaseRecord lease : expiredLeases) {
            lease.setStatus(2); // 设置为已结束
            lease.setActualEndDate(lease.getEndDate());
            lease.setUpdateTime(LocalDateTime.now());
            leaseRecordMapper.updateById(lease);

            // 将房屋状态改为待出租
            House house = houseMapper.selectById(lease.getHouseId());
            if (house != null && house.getStatus() == 2) { // 已出租
                house.setStatus(1); // 待出租
                houseMapper.updateById(house);
            }

            count++;
        }

        if (count > 0) {
            log.info("自动结束过期租赁: 共处理 {} 条过期租赁记录", count);
        }

        return count;
    }

    /**
     * 获取即将到期的租赁记录（用于提醒）
     * @param days 提前多少天提醒
     */
    public List<LeaseRecord> getExpiringSoonLeases(int days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);

        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getStatus, 1) // 租赁中
                .le(LeaseRecord::getEndDate, targetDate)
                .ge(LeaseRecord::getEndDate, LocalDate.now());

        List<LeaseRecord> leases = leaseRecordMapper.selectList(queryWrapper);

        // 加载关联信息
        for (LeaseRecord lease : leases) {
            loadAssociatedInfo(lease);
        }

        return leases;
    }

    /**
     * 获取租客即将到期的租赁记录
     */
    public List<LeaseRecord> getTenantExpiringSoonLeases(int days) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LocalDate targetDate = LocalDate.now().plusDays(days);

        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getTenantId, currentUser.getId())
                .eq(LeaseRecord::getStatus, 1) // 租赁中
                .le(LeaseRecord::getEndDate, targetDate)
                .ge(LeaseRecord::getEndDate, LocalDate.now());

        List<LeaseRecord> leases = leaseRecordMapper.selectList(queryWrapper);

        // 加载关联信息
        for (LeaseRecord lease : leases) {
            loadAssociatedInfo(lease);
        }

        return leases;
    }

    /**
     * 获取房东即将到期的租赁记录
     */
    public List<LeaseRecord> getLandlordExpiringSoonLeases(int days) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LocalDate targetDate = LocalDate.now().plusDays(days);

        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getLandlordId, currentUser.getId())
                .eq(LeaseRecord::getStatus, 1) // 租赁中
                .le(LeaseRecord::getEndDate, targetDate)
                .ge(LeaseRecord::getEndDate, LocalDate.now());

        List<LeaseRecord> leases = leaseRecordMapper.selectList(queryWrapper);

        // 加载关联信息
        for (LeaseRecord lease : leases) {
            loadAssociatedInfo(lease);
        }

        return leases;
    }

    /**
     * 检查租赁记录是否可以续签
     */
    public boolean canRenewLease(Long id) {
        LeaseRecord lease = leaseRecordMapper.selectById(id);
        if (lease == null) {
            return false;
        }

        // 必须是租赁中状态
        if (lease.getStatus() != 1) {
            return false;
        }

        // 检查是否已经续签过
        LambdaQueryWrapper<LeaseRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LeaseRecord::getOriginalLeaseId, id);
        Long count = leaseRecordMapper.selectCount(queryWrapper);

        return count == 0;
    }

    /**
     * 获取租赁记录的剩余天数
     */
    public long getDaysRemaining(Long id) {
        LeaseRecord lease = leaseRecordMapper.selectById(id);
        if (lease == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = lease.getEndDate();

        if (endDate.isBefore(today)) {
            return 0; // 已过期
        }

        return ChronoUnit.DAYS.between(today, endDate);
    }

    /**
     * 提前结束租赁（退租）
     */
    @Transactional
    public LeaseRecord terminateLease(Long id, String reason) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LeaseRecord lease = leaseRecordMapper.selectById(id);
        if (lease == null) {
            throw new ServiceException("租赁记录不存在");
        }

        // 检查权限（租客或房东都可以发起退租）
        if (!lease.getTenantId().equals(currentUser.getId())
            && !lease.getLandlordId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限操作该租赁记录");
        }

        // 检查租赁状态
        if (lease.getStatus() != 1) {
            throw new ServiceException("只有租赁中的记录才能退租");
        }

        lease.setStatus(3); // 已退租
        lease.setActualEndDate(LocalDate.now());
        lease.setUpdateTime(LocalDateTime.now());
        leaseRecordMapper.updateById(lease);

        // 将房屋状态改为待出租
        House house = houseMapper.selectById(lease.getHouseId());
        if (house != null && house.getStatus() == 2) { // 已出租
            house.setStatus(1); // 待出租
            houseMapper.updateById(house);
        }

        log.info("用户 {} 提前结束租赁，租赁记录ID：{}，原因：{}", currentUser.getUsername(), id, reason);

        return lease;
    }

    /**
     * 创建续签租约记录
     */
    @Transactional
    public LeaseRecord createRenewalLeaseRecord(Order renewalOrder, LeaseRecord originalLease) {
        // 验证原租约状态
        if (originalLease.getStatus() != 1) {
            throw new ServiceException("原租约状态异常");
        }

        // 获取房屋和租客信息
        House house = houseMapper.selectById(renewalOrder.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }

        User tenant = userMapper.selectById(renewalOrder.getTenantId());
        if (tenant == null) {
            throw new ServiceException("租客不存在");
        }

        User landlord = userMapper.selectById(renewalOrder.getLandlordId());
        if (landlord == null) {
            throw new ServiceException("房东不存在");
        }

        // 创建续签租约
        LeaseRecord renewalLease = new LeaseRecord();
        renewalLease.setOrderId(renewalOrder.getId());
        renewalLease.setHouseId(renewalOrder.getHouseId());
        renewalLease.setTenantId(renewalOrder.getTenantId());
        renewalLease.setLandlordId(renewalOrder.getLandlordId());

        // 续签租约从原租约结束日期的第二天开始
        renewalLease.setStartDate(originalLease.getEndDate().plusDays(1));

        // 计算结束日期（根据支付周期）
        LocalDate endDate = calculateEndDate(renewalLease.getStartDate(), originalLease.getPaymentCycle());
        renewalLease.setEndDate(endDate);

        renewalLease.setRentAmount(renewalOrder.getAmount());
        renewalLease.setPaymentCycle(originalLease.getPaymentCycle());
        renewalLease.setStatus(1); // 租赁中（待生效）
        renewalLease.setIsRenewal(1);
        renewalLease.setOriginalLeaseId(originalLease.getId());
        renewalLease.setCreateTime(LocalDateTime.now());
        renewalLease.setUpdateTime(LocalDateTime.now());

        // 插入续签租约记录
        leaseRecordMapper.insert(renewalLease);

        // 生成续签合同
        String contractUrl = contractGenerator.generateContract(renewalLease, renewalOrder, house, tenant, landlord);
        renewalLease.setContractUrl(contractUrl);
        leaseRecordMapper.updateById(renewalLease);

        // 创建交易记录
        createTransactionsForLease(renewalOrder, renewalLease);

        // 注意：房屋状态保持为已出租(2)，不需要修改

        log.info("创建续签租约成功，租约ID：{}，原租约ID：{}，开始日期：{}，结束日期：{}",
                renewalLease.getId(), originalLease.getId(),
                renewalLease.getStartDate(), renewalLease.getEndDate());

        return renewalLease;
    }

    /**
     * 根据支付周期计算结束日期
     */
    private LocalDate calculateEndDate(LocalDate startDate, String paymentCycle) {
        if ("MONTHLY".equalsIgnoreCase(paymentCycle)) {
            return startDate.plusMonths(1).minusDays(1);
        } else if ("QUARTERLY".equalsIgnoreCase(paymentCycle)) {
            return startDate.plusMonths(3).minusDays(1);
        } else if ("YEARLY".equalsIgnoreCase(paymentCycle)) {
            return startDate.plusYears(1).minusDays(1);
        }
        // 默认按月计算
        return startDate.plusMonths(1).minusDays(1);
    }

    /**
     * 为租约创建交易记录
     */
    private void createTransactionsForLease(Order order, LeaseRecord leaseRecord) {
        // 创建租客支出交易记录
        Transaction tenantExpense = new Transaction();
        tenantExpense.setOrderId(order.getId());
        tenantExpense.setUserId(order.getTenantId());
        tenantExpense.setType(TransactionType.EXPENSE.getValue());
        tenantExpense.setAmount(order.getAmount());
        tenantExpense.setDescription("租金支付-订单号:" + order.getOrderNo());
        tenantExpense.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tenantExpense);

        // 创建房东收入交易记录
        Transaction landlordIncome = new Transaction();
        landlordIncome.setOrderId(order.getId());
        landlordIncome.setUserId(order.getLandlordId());
        landlordIncome.setType(TransactionType.INCOME.getValue());
        landlordIncome.setAmount(order.getAmount());
        landlordIncome.setDescription("租金收入-订单号:" + order.getOrderNo());
        landlordIncome.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(landlordIncome);

        log.info("为租约ID {} 创建交易记录成功", leaseRecord.getId());
    }
} 