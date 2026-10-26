package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.StatisticsDTO;
import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.example.springboot.util.JwtTokenUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsService {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private HouseMapper houseMapper;
    
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private TransactionMapper transactionMapper;
    
    @Resource
    private LeaseRecordMapper leaseRecordMapper;
    
    @Resource
    private HouseTypeMapper houseTypeMapper;
    
    /**
     * 获取仪表盘统计数据
     * 根据用户角色返回不同的数据：
     * - 管理员: 返回系统全局数据
     * - 房东: 返回自己的房源、订单、交易数据
     */
    public StatisticsDTO.DashboardStatistics getDashboardStatistics() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRoleCode());
        boolean isLandlord = currentUser != null && "LANDLORD".equals(currentUser.getRoleCode());
        Long landlordId = currentUser != null ? currentUser.getId() : null;

        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long userCount = 0;
        long tenantCount = 0;
        long landlordCount = 0;
        long houseCount;
        long availableHouseCount;
        long rentedHouseCount;
        long orderCount;
        long monthOrderCount;
        long pendingOrderCount;
        BigDecimal totalAmount;
        BigDecimal monthAmount;

        if (isAdmin) {
            // 管理员: 返回全局统计数据

            // 用户统计
            userCount = userMapper.selectCount(new LambdaQueryWrapper<>());

            // 租客统计
            LambdaQueryWrapper<User> tenantQuery = new LambdaQueryWrapper<>();
            tenantQuery.eq(User::getRoleCode, "TENANT");
            tenantCount = userMapper.selectCount(tenantQuery);

            // 房东统计
            LambdaQueryWrapper<User> landlordQuery = new LambdaQueryWrapper<>();
            landlordQuery.eq(User::getRoleCode, "LANDLORD");
            landlordCount = userMapper.selectCount(landlordQuery);

            // 房源统计
            houseCount = houseMapper.selectCount(new LambdaQueryWrapper<>());

            // 可租房源统计
            LambdaQueryWrapper<House> availableHouseQuery = new LambdaQueryWrapper<>();
            availableHouseQuery.eq(House::getStatus, 1);
            availableHouseCount = houseMapper.selectCount(availableHouseQuery);

            // 已租房源统计
            LambdaQueryWrapper<House> rentedHouseQuery = new LambdaQueryWrapper<>();
            rentedHouseQuery.eq(House::getStatus, 2);
            rentedHouseCount = houseMapper.selectCount(rentedHouseQuery);

            // 订单统计
            orderCount = orderMapper.selectCount(new LambdaQueryWrapper<>());

            // 本月订单统计
            LambdaQueryWrapper<Order> monthOrderQuery = new LambdaQueryWrapper<>();
            monthOrderQuery.ge(Order::getCreateTime, firstDayOfMonth);
            monthOrderCount = orderMapper.selectCount(monthOrderQuery);

            // 待支付订单统计
            LambdaQueryWrapper<Order> pendingOrderQuery = new LambdaQueryWrapper<>();
            pendingOrderQuery.eq(Order::getStatus, 0);
            pendingOrderCount = orderMapper.selectCount(pendingOrderQuery);

            // 交易总额
            LambdaQueryWrapper<Transaction> transactionQuery = new LambdaQueryWrapper<>();
            transactionQuery.eq(Transaction::getType, 1);
            List<Transaction> transactions = transactionMapper.selectList(transactionQuery);
            totalAmount = transactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 本月交易额
            LambdaQueryWrapper<Transaction> monthTransactionQuery = new LambdaQueryWrapper<>();
            monthTransactionQuery.eq(Transaction::getType, 1)
                    .ge(Transaction::getCreateTime, firstDayOfMonth);
            List<Transaction> monthTransactions = transactionMapper.selectList(monthTransactionQuery);
            monthAmount = monthTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } else if (isLandlord && landlordId != null) {
            // 房东: 只返回自己的数据

            // 房东的房源统计
            LambdaQueryWrapper<House> myHouseQuery = new LambdaQueryWrapper<>();
            myHouseQuery.eq(House::getLandlordId, landlordId);
            houseCount = houseMapper.selectCount(myHouseQuery);

            // 房东可租房源统计
            LambdaQueryWrapper<House> myAvailableQuery = new LambdaQueryWrapper<>();
            myAvailableQuery.eq(House::getLandlordId, landlordId).eq(House::getStatus, 1);
            availableHouseCount = houseMapper.selectCount(myAvailableQuery);

            // 房东已租房源统计
            LambdaQueryWrapper<House> myRentedQuery = new LambdaQueryWrapper<>();
            myRentedQuery.eq(House::getLandlordId, landlordId).eq(House::getStatus, 2);
            rentedHouseCount = houseMapper.selectCount(myRentedQuery);

            // 房东的订单统计
            LambdaQueryWrapper<Order> myOrderQuery = new LambdaQueryWrapper<>();
            myOrderQuery.eq(Order::getLandlordId, landlordId);
            orderCount = orderMapper.selectCount(myOrderQuery);

            // 房东本月订单统计
            LambdaQueryWrapper<Order> myMonthOrderQuery = new LambdaQueryWrapper<>();
            myMonthOrderQuery.eq(Order::getLandlordId, landlordId)
                    .ge(Order::getCreateTime, firstDayOfMonth);
            monthOrderCount = orderMapper.selectCount(myMonthOrderQuery);

            // 房东待支付订单统计
            LambdaQueryWrapper<Order> myPendingOrderQuery = new LambdaQueryWrapper<>();
            myPendingOrderQuery.eq(Order::getLandlordId, landlordId).eq(Order::getStatus, 0);
            pendingOrderCount = orderMapper.selectCount(myPendingOrderQuery);

            // 房东交易总额（收入）
            LambdaQueryWrapper<Transaction> myTransactionQuery = new LambdaQueryWrapper<>();
            myTransactionQuery.eq(Transaction::getUserId, landlordId).eq(Transaction::getType, 1);
            List<Transaction> myTransactions = transactionMapper.selectList(myTransactionQuery);
            totalAmount = myTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 房东本月交易额
            LambdaQueryWrapper<Transaction> myMonthTransactionQuery = new LambdaQueryWrapper<>();
            myMonthTransactionQuery.eq(Transaction::getUserId, landlordId)
                    .eq(Transaction::getType, 1)
                    .ge(Transaction::getCreateTime, firstDayOfMonth);
            List<Transaction> myMonthTransactions = transactionMapper.selectList(myMonthTransactionQuery);
            monthAmount = myMonthTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } else {
            // 其他用户: 返回空数据
            houseCount = 0;
            availableHouseCount = 0;
            rentedHouseCount = 0;
            orderCount = 0;
            monthOrderCount = 0;
            pendingOrderCount = 0;
            totalAmount = BigDecimal.ZERO;
            monthAmount = BigDecimal.ZERO;
        }

        return StatisticsDTO.DashboardStatistics.builder()
                .userCount(userCount)
                .tenantCount(tenantCount)
                .landlordCount(landlordCount)
                .houseCount(houseCount)
                .availableHouseCount(availableHouseCount)
                .rentedHouseCount(rentedHouseCount)
                .orderCount(orderCount)
                .monthOrderCount(monthOrderCount)
                .pendingOrderCount(pendingOrderCount)
                .totalAmount(totalAmount)
                .monthAmount(monthAmount)
                .build();
    }
    
    /**
     * 获取最近租赁记录
     * 房东只能看到自己的租赁记录
     */
    public List<StatisticsDTO.RecentLease> getRecentLeases() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean isLandlord = currentUser != null && "LANDLORD".equals(currentUser.getRoleCode());
        Long landlordId = currentUser != null ? currentUser.getId() : null;

        LambdaQueryWrapper<LeaseRecord> query = new LambdaQueryWrapper<>();

        // 房东只能查看自己的租赁记录
        if (isLandlord && landlordId != null) {
            query.eq(LeaseRecord::getLandlordId, landlordId);
        }

        query.orderByDesc(LeaseRecord::getCreateTime)
                .last("LIMIT 5"); // 只取最近5条记录

        List<LeaseRecord> leases = leaseRecordMapper.selectList(query);
        List<StatisticsDTO.RecentLease> result = new ArrayList<>();

        for (LeaseRecord lease : leases) {
            House house = houseMapper.selectById(lease.getHouseId());
            User tenant = userMapper.selectById(lease.getTenantId());
            User landlord = userMapper.selectById(lease.getLandlordId());

            if (house != null && tenant != null && landlord != null) {
                StatisticsDTO.RecentLease recentLease = StatisticsDTO.RecentLease.builder()
                        .id(lease.getId())
                        .houseId(lease.getHouseId())
                        .houseName(house.getTitle())
                        .houseImage(house.getImages())
                        .tenantId(lease.getTenantId())
                        .tenantName(tenant.getName())
                        .landlordId(lease.getLandlordId())
                        .landlordName(landlord.getName())
                        .startDate(lease.getStartDate().atStartOfDay())
                        .endDate(lease.getEndDate().atStartOfDay())
                        .amount(lease.getRentAmount())
                        .status(lease.getStatus())
                        .createTime(lease.getCreateTime())
                        .build();

                result.add(recentLease);
            }
        }

        return result;
    }
    
    /**
     * 获取月度订单统计
     * 房东只能看到自己的订单统计
     */
    public StatisticsDTO.MonthlyOrderStatistics getMonthlyOrders() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean isLandlord = currentUser != null && "LANDLORD".equals(currentUser.getRoleCode());
        Long landlordId = currentUser != null ? currentUser.getId() : null;

        List<String> months = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();

        // 获取最近6个月的月份
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthStr = month.format(formatter);
            months.add(monthStr);

            // 查询该月的订单数量
            LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = month.plusMonths(1).withDayOfMonth(1).atStartOfDay();

            LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
            query.ge(Order::getCreateTime, startOfMonth)
                    .lt(Order::getCreateTime, endOfMonth);

            // 房东只能看到自己的订单
            if (isLandlord && landlordId != null) {
                query.eq(Order::getLandlordId, landlordId);
            }

            long count = orderMapper.selectCount(query);
            orderCounts.add(count);
        }

        return StatisticsDTO.MonthlyOrderStatistics.builder()
                .months(months)
                .orderCounts(orderCounts)
                .build();
    }
    
    /**
     * 获取房源类型分布
     * 房东只能看到自己的房源类型分布
     */
    public List<StatisticsDTO.HouseTypeData> getHouseTypeDistribution() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        boolean isLandlord = currentUser != null && "LANDLORD".equals(currentUser.getRoleCode());
        Long landlordId = currentUser != null ? currentUser.getId() : null;

        // 获取所有房屋类型
        List<HouseType> houseTypes = houseTypeMapper.selectList(null);
        List<StatisticsDTO.HouseTypeData> result = new ArrayList<>();

        for (HouseType type : houseTypes) {
            // 查询该类型的房源数量
            LambdaQueryWrapper<House> query = new LambdaQueryWrapper<>();
            query.eq(House::getTypeId, type.getId());

            // 房东只能看到自己的房源
            if (isLandlord && landlordId != null) {
                query.eq(House::getLandlordId, landlordId);
            }

            long count = houseMapper.selectCount(query);

            StatisticsDTO.HouseTypeData typeData = StatisticsDTO.HouseTypeData.builder()
                    .name(type.getName())
                    .count(count)
                    .build();

            result.add(typeData);
        }

        return result;
    }
} 