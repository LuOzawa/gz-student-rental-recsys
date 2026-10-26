package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Transaction;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.TransactionMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.ExcelExportUtils;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {
    @Resource
    private TransactionMapper transactionMapper;
    
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 获取当前用户的交易记录
     */
    public Page<Transaction> getUserTransactions(Integer type, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transaction::getUserId, user.getId());
        
        // 添加类型查询条件
        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Transaction::getCreateTime);
        
        // 分页查询
        Page<Transaction> page = transactionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Transaction transaction : page.getRecords()) {
            Order order = orderMapper.selectById(transaction.getOrderId());
            transaction.setOrder(order);
        }
        
        return page;
    }
    
    /**
     * 管理员获取所有交易记录
     */
    public Page<Transaction> getAllTransactions(String username, Integer type, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果有用户名查询条件，先查询用户ID
        if (org.apache.commons.lang3.StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, username);
            List<User> users = userMapper.selectList(userQuery);
            List<Long> userIds = users.stream().map(User::getId).toList();
            if (!userIds.isEmpty()) {
                queryWrapper.in(Transaction::getUserId, userIds);
            } else {
                // 如果没有找到匹配的用户，返回空结果
                return new Page<>(currentPage, size);
            }
        }
        
        // 添加类型查询条件
        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Transaction::getCreateTime);
        
        // 分页查询
        Page<Transaction> page = transactionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Transaction transaction : page.getRecords()) {
            transaction.setOrder(orderMapper.selectById(transaction.getOrderId()));
            transaction.setUser(userMapper.selectById(transaction.getUserId()));
        }
        
        return page;
    }
    
    /**
     * 创建交易记录
     */
    public Transaction createTransaction(Transaction transaction) {
        transaction.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(transaction);
        return transaction;
    }

    /**
     * 获取当前用户的交易统计
     * @param period 统计周期: month(本月), year(本年), all(全部)
     * @param type 交易类型筛选: 1-收入, 2-支出, null-全部
     */
    public Map<String, Object> getUserStatistics(String period, Integer type) {
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }

        return calculateStatistics(user.getId(), period, type, null);
    }

    /**
     * 管理员获取所有交易统计
     * @param period 统计周期: month(本月), year(本年), all(全部)
     * @param type 交易类型筛选: 1-收入, 2-支出, null-全部
     * @param username 用户名筛选
     */
    public Map<String, Object> getAdminStatistics(String period, Integer type, String username) {
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null || !"ADMIN".equals(user.getRoleCode())) {
            throw new ServiceException("无权限访问");
        }

        return calculateStatistics(null, period, type, username);
    }

    /**
     * 计算交易统计数据
     * @param userId 用户ID，如果为null则统计所有用户
     * @param period 统计周期
     * @param type 交易类型筛选
     * @param username 用户名筛选（仅管理员使用）
     */
    private Map<String, Object> calculateStatistics(Long userId, String period, Integer type, String username) {
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();

        // 如果指定了用户ID，则只查询该用户的交易记录
        if (userId != null) {
            queryWrapper.eq(Transaction::getUserId, userId);
        }

        // 用户名筛选（管理员）
        if (org.apache.commons.lang3.StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, username);
            List<User> users = userMapper.selectList(userQuery);
            List<Long> userIds = users.stream().map(User::getId).toList();
            if (!userIds.isEmpty()) {
                queryWrapper.in(Transaction::getUserId, userIds);
            } else {
                // 如果没有找到匹配的用户，返回空统计
                Map<String, Object> emptyStats = new HashMap<>();
                emptyStats.put("totalIncome", BigDecimal.ZERO);
                emptyStats.put("totalExpense", BigDecimal.ZERO);
                emptyStats.put("balance", BigDecimal.ZERO);
                emptyStats.put("totalCount", 0);
                emptyStats.put("incomeCount", 0);
                emptyStats.put("expenseCount", 0);
                emptyStats.put("period", period);
                return emptyStats;
            }
        }

        // 交易类型筛选
        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }

        // 根据周期添加时间条件
        LocalDateTime startTime = getStartTimeByPeriod(period);
        if (startTime != null) {
            queryWrapper.ge(Transaction::getCreateTime, startTime);
        }

        // 查询所有交易记录
        List<Transaction> transactions = transactionMapper.selectList(queryWrapper);

        // 计算统计数据
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int incomeCount = 0;
        int expenseCount = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == 1) {
                // 收入
                totalIncome = totalIncome.add(transaction.getAmount());
                incomeCount++;
            } else if (transaction.getType() == 2) {
                // 支出
                totalExpense = totalExpense.add(transaction.getAmount());
                expenseCount++;
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalIncome", totalIncome);
        statistics.put("totalExpense", totalExpense);
        statistics.put("balance", balance);
        statistics.put("totalCount", transactions.size());
        statistics.put("incomeCount", incomeCount);
        statistics.put("expenseCount", expenseCount);
        statistics.put("period", period);

        return statistics;
    }

    /**
     * 根据周期获取开始时间
     */
    private LocalDateTime getStartTimeByPeriod(String period) {
        if (period == null || "all".equals(period)) {
            return null; // 全部时间
        }

        LocalDateTime now = LocalDateTime.now();

        if ("month".equals(period)) {
            // 本月第一天
            return now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else if ("year".equals(period)) {
            // 本年第一天
            return now.with(TemporalAdjusters.firstDayOfYear()).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }

        return null;
    }

    /**
     * 导出当前用户交易记录到Excel
     */
    public byte[] exportUserTransactionsToExcel(Integer type, String period) throws IOException {
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }

        // 获取交易记录
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transaction::getUserId, user.getId());

        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }

        LocalDateTime startTime = getStartTimeByPeriod(period);
        if (startTime != null) {
            queryWrapper.ge(Transaction::getCreateTime, startTime);
        }

        queryWrapper.orderByDesc(Transaction::getCreateTime);

        List<Transaction> transactions = transactionMapper.selectList(queryWrapper);

        // 填充关联信息
        for (Transaction transaction : transactions) {
            transaction.setOrder(orderMapper.selectById(transaction.getOrderId()));
            transaction.setUser(user);
        }

        // 根据筛选后的数据计算统计
        Map<String, Object> statistics = calculateStatisticsFromList(transactions, period, type);

        // 导出Excel
        return ExcelExportUtils.exportTransactionsToExcel(transactions, statistics);
    }

    /**
     * 管理员导出所有交易记录到Excel
     */
    public byte[] exportAllTransactionsToExcel(String username, Integer type, String period) throws IOException {
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null || !"ADMIN".equals(user.getRoleCode())) {
            throw new ServiceException("无权限访问");
        }

        // 获取交易记录
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();

        if (org.apache.commons.lang3.StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, username);
            List<User> users = userMapper.selectList(userQuery);
            List<Long> userIds = users.stream().map(User::getId).toList();
            if (!userIds.isEmpty()) {
                queryWrapper.in(Transaction::getUserId, userIds);
            } else {
                // 如果没有找到匹配的用户，返回空Excel
                Map<String, Object> emptyStats = new HashMap<>();
                emptyStats.put("totalIncome", BigDecimal.ZERO);
                emptyStats.put("totalExpense", BigDecimal.ZERO);
                emptyStats.put("balance", BigDecimal.ZERO);
                emptyStats.put("totalCount", 0);
                emptyStats.put("incomeCount", 0);
                emptyStats.put("expenseCount", 0);
                emptyStats.put("period", period);
                emptyStats.put("filterType", type);
                emptyStats.put("filterUsername", username);
                return ExcelExportUtils.exportTransactionsToExcel(List.of(), emptyStats);
            }
        }

        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }

        LocalDateTime startTime = getStartTimeByPeriod(period);
        if (startTime != null) {
            queryWrapper.ge(Transaction::getCreateTime, startTime);
        }

        queryWrapper.orderByDesc(Transaction::getCreateTime);

        List<Transaction> transactions = transactionMapper.selectList(queryWrapper);

        // 填充关联信息
        for (Transaction transaction : transactions) {
            transaction.setOrder(orderMapper.selectById(transaction.getOrderId()));
            transaction.setUser(userMapper.selectById(transaction.getUserId()));
        }

        // 根据筛选后的数据计算统计
        Map<String, Object> statistics = calculateStatisticsFromList(transactions, period, type);
        statistics.put("filterUsername", username);

        // 导出Excel
        return ExcelExportUtils.exportTransactionsToExcel(transactions, statistics);
    }

    /**
     * 根据交易列表计算统计数据
     * @param transactions 交易列表
     * @param period 统计周期
     * @param filterType 筛选的交易类型
     */
    private Map<String, Object> calculateStatisticsFromList(List<Transaction> transactions, String period, Integer filterType) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int incomeCount = 0;
        int expenseCount = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == 1) {
                // 收入
                totalIncome = totalIncome.add(transaction.getAmount());
                incomeCount++;
            } else if (transaction.getType() == 2) {
                // 支出
                totalExpense = totalExpense.add(transaction.getAmount());
                expenseCount++;
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpense);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalIncome", totalIncome);
        statistics.put("totalExpense", totalExpense);
        statistics.put("balance", balance);
        statistics.put("totalCount", transactions.size());
        statistics.put("incomeCount", incomeCount);
        statistics.put("expenseCount", expenseCount);
        statistics.put("period", period);
        statistics.put("filterType", filterType);

        return statistics;
    }
} 