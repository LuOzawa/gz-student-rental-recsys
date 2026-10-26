package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.*;
import org.example.springboot.enumClass.OrderStatus;
import org.example.springboot.enumClass.TransactionType;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AppointmentMapper;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.LeaseRecordMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.TransactionMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.ExcelExportUtils;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
public class OrderService {
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private HouseMapper houseMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private LeaseRecordMapper leaseRecordMapper;

    @Resource
    private LeaseRecordService leaseRecordService;

    @Resource
    private UserService userService;

    @Resource
    private AppointmentMapper appointmentMapper;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Order order) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }

        // 验证实名认证状态（租客必须完成实名认证才能租房）
        if (!userService.isUserVerified(user.getId())) {
            User userInfo = userMapper.selectById(user.getId());
            if (userInfo.getVerifyStatus() == null || userInfo.getVerifyStatus() == 0) {
                throw new ServiceException("请先完成实名认证才能租赁房屋");
            } else if (userInfo.getVerifyStatus() == 1) {
                throw new ServiceException("您的实名认证正在审核中，请耐心等待");
            } else if (userInfo.getVerifyStatus() == 3) {
                String reason = userInfo.getVerifyRejectReason() != null ? userInfo.getVerifyRejectReason() : "请认真填写信息";
                throw new ServiceException("您的实名认证未通过：" + reason);
            }
        }

        // 查询房屋信息
        House house = houseMapper.selectById(order.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }

        // 检查房屋状态是否可租
        if (house.getStatus() != 1) {
            throw new ServiceException("该房屋不可租用");
        }
        
        // 设置订单基本信息
        order.setTenantId(user.getId());
        order.setLandlordId(house.getLandlordId());
        order.setOrderNo(generateOrderNo());
        order.setAmount(house.getPrice()); // 从房屋价格设置订单金额
        order.setDeposit(house.getPrice()); // 押金默认等于月租金，可根据业务需求调整
        order.setStatus(OrderStatus.WAITING_PAYMENT.getValue());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        // 保存订单
        orderMapper.insert(order);
        
        return order;
    }
    
    /**
     * 支付订单
     */
    @Transactional
    public Order payOrder(Long orderId, String paymentMethod) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限
        if (!order.getTenantId().equals(user.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态
        if (!order.getStatus().equals(OrderStatus.WAITING_PAYMENT.getValue())) {
            throw new ServiceException("订单状态不正确，无法支付");
        }
        
        // 更新订单状态为已支付待确认
        order.setStatus(OrderStatus.PAID_WAITING_CONFIRM.getValue());
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 创建租客支出交易记录
        Transaction tenantExpense = new Transaction();
        tenantExpense.setOrderId(orderId);
        tenantExpense.setUserId(user.getId());
        tenantExpense.setType(TransactionType.EXPENSE.getValue());
        tenantExpense.setAmount(order.getAmount());
        tenantExpense.setDescription("租金支付-订单号:" + order.getOrderNo());
        tenantExpense.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tenantExpense);
        
        // 创建房东收入交易记录
        Transaction landlordIncome = new Transaction();
        landlordIncome.setOrderId(orderId);
        landlordIncome.setUserId(order.getLandlordId());
        landlordIncome.setType(TransactionType.INCOME.getValue());
        landlordIncome.setAmount(order.getAmount());
        landlordIncome.setDescription("租金收入-订单号:" + order.getOrderNo());
        landlordIncome.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(landlordIncome);
        
        return order;
    }
    
    /**
     * 确认订单
     */
    @Transactional
    public Order confirmOrder(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限，只有房东可以确认订单
        if (!order.getLandlordId().equals(user.getId())) {
            throw new ServiceException("只有房东可以确认订单");
        }
        
        // 检查订单状态，只有已支付待确认的订单可以确认
        if (!order.getStatus().equals(OrderStatus.PAID_WAITING_CONFIRM.getValue())) {
            throw new ServiceException("订单状态不正确，无法确认");
        }
        
        // 获取房屋信息
        House house = houseMapper.selectById(order.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋信息不存在");
        }
        
        // 更新房屋状态为已出租
        house.setStatus(2); // 2表示已出租
        houseMapper.updateById(house);
        
        // 更新订单状态为已确认
        order.setStatus(OrderStatus.CONFIRMED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 创建租赁记录并生成合同
        LeaseRecord leaseRecord = leaseRecordService.createLeaseRecord(order);
        log.info("创建租赁记录成功，ID：{}", leaseRecord.getId());

        return order;
    }

    /**
     * 取消订单
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限
        if (!order.getTenantId().equals(user.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有待支付的订单可以取消
        if (!order.getStatus().equals(OrderStatus.WAITING_PAYMENT.getValue())) {
            throw new ServiceException("订单状态不正确，无法取消");
        }
        
        // 更新订单状态为已取消
        order.setStatus(OrderStatus.CANCELED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        return order;
    }
    
    /**
     * 申请退款
     */
    @Transactional
    public Order refundOrder(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限，只有租客可以申请退款
        if (!order.getTenantId().equals(user.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有已支付待确认的订单可以退款
        if (!order.getStatus().equals(OrderStatus.PAID_WAITING_CONFIRM.getValue())) {
            throw new ServiceException("订单状态不正确，无法申请退款");
        }
        
        // 更新订单状态为已退款
        order.setStatus(OrderStatus.REFUNDED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 创建租客收入交易记录（退款）
        Transaction tenantIncome = new Transaction();
        tenantIncome.setOrderId(orderId);
        tenantIncome.setUserId(user.getId());
        tenantIncome.setType(TransactionType.INCOME.getValue());
        tenantIncome.setAmount(order.getAmount());
        tenantIncome.setDescription("退款收入-订单号:" + order.getOrderNo());
        tenantIncome.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tenantIncome);
        
        // 创建房东支出交易记录（退款）
        Transaction landlordExpense = new Transaction();
        landlordExpense.setOrderId(orderId);
        landlordExpense.setUserId(order.getLandlordId());
        landlordExpense.setType(TransactionType.EXPENSE.getValue());
        landlordExpense.setAmount(order.getAmount());
        landlordExpense.setDescription("退款支出-订单号:" + order.getOrderNo());
        landlordExpense.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(landlordExpense);
        
        return order;
    }
    
    /**
     * 获取订单详情
     */
    public Order getOrderDetail(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        

        
        // 获取关联信息
        order.setHouse(houseMapper.selectById(order.getHouseId()));
        order.setTenant(userMapper.selectById(order.getTenantId()));
        order.setLandlord(userMapper.selectById(order.getLandlordId()));
        
        return order;
    }
    
    /**
     * 租客获取自己的订单列表
     */
    public Page<Order> getTenantOrders(String orderNo, Integer status, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getTenantId, user.getId());
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 分页查询
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Order order : page.getRecords()) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setLandlord(userMapper.selectById(order.getLandlordId()));
        }
        
        return page;
    }
    
    /**
     * 房东获取自己的订单列表
     */
    public Page<Order> getLandlordOrders(String orderNo, Integer status, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getLandlordId, user.getId());
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 分页查询
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Order order : page.getRecords()) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(userMapper.selectById(order.getTenantId()));
        }
        
        return page;
    }
    
    /**
     * 管理员获取所有订单列表
     */
    public Page<Order> getAllOrders(String orderNo, Integer status, String tenantUsername, String landlordUsername, Integer currentPage, Integer size) {
        // 如果有用户名查询条件，先查询用户ID
        Long tenantId = null;
        Long landlordId = null;
        

        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(tenantUsername)) {
            LambdaQueryWrapper<User> tenantQuery = new LambdaQueryWrapper<>();
            tenantQuery.like(User::getUsername, tenantUsername);
            List<User> tenants = userMapper.selectList(tenantQuery);
            List<Long> tenantIds = tenants.stream().map(User::getId).toList();
            if(!tenantIds.isEmpty()){
                queryWrapper.in(Order::getTenantId, tenantIds);
            }else{
                return new Page<>(currentPage, size);
            }

        }

        if (StringUtils.isNotBlank(landlordUsername)) {
            LambdaQueryWrapper<User> landlordQuery = new LambdaQueryWrapper<>();
            landlordQuery.like(User::getUsername, landlordUsername);
            List<User> landlords = userMapper.selectList(landlordQuery);
            List<Long> landlordIds = landlords.stream().map(User::getId).toList();
            if(!landlordIds.isEmpty()){
                queryWrapper.in(Order::getLandlordId, landlordIds);
            }else {
                return new Page<>(currentPage, size);
            }


        }
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (tenantId != null) {
            queryWrapper.eq(Order::getTenantId, tenantId);
        }
        if (landlordId != null) {
            queryWrapper.eq(Order::getLandlordId, landlordId);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 分页查询
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Order order : page.getRecords()) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(userMapper.selectById(order.getTenantId()));
            order.setLandlord(userMapper.selectById(order.getLandlordId()));
        }
        
        return page;
    }
    
    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }

    /**
     * 房东导出自己的订单到Excel
     */
    public byte[] exportLandlordOrdersToExcel(String orderNo, Integer status) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getLandlordId, currentUser.getId());

        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }

        queryWrapper.orderByDesc(Order::getCreateTime);

        List<Order> orders = orderMapper.selectList(queryWrapper);

        // 填充关联信息
        for (Order order : orders) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(userMapper.selectById(order.getTenantId()));
            order.setLandlord(currentUser);
        }

        return ExcelExportUtils.exportOrdersToExcel(orders);
    }

    /**
     * 租客导出自己的订单到Excel
     */
    public byte[] exportTenantOrdersToExcel(String orderNo, Integer status) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getTenantId, currentUser.getId());

        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }

        queryWrapper.orderByDesc(Order::getCreateTime);

        List<Order> orders = orderMapper.selectList(queryWrapper);

        // 填充关联信息
        for (Order order : orders) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(currentUser);
            order.setLandlord(userMapper.selectById(order.getLandlordId()));
        }

        return ExcelExportUtils.exportOrdersToExcel(orders);
    }

    /**
     * 管理员导出所有订单到Excel
     */
    public byte[] exportAllOrdersToExcel(String orderNo, Integer status, String tenantUsername, String landlordUsername) throws IOException {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限访问");
        }

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(tenantUsername)) {
            LambdaQueryWrapper<User> tenantQuery = new LambdaQueryWrapper<>();
            tenantQuery.like(User::getUsername, tenantUsername);
            List<User> tenants = userMapper.selectList(tenantQuery);
            List<Long> tenantIds = tenants.stream().map(User::getId).toList();
            if(!tenantIds.isEmpty()){
                queryWrapper.in(Order::getTenantId, tenantIds);
            } else {
                return ExcelExportUtils.exportOrdersToExcel(List.of());
            }
        }

        if (StringUtils.isNotBlank(landlordUsername)) {
            LambdaQueryWrapper<User> landlordQuery = new LambdaQueryWrapper<>();
            landlordQuery.like(User::getUsername, landlordUsername);
            List<User> landlords = userMapper.selectList(landlordQuery);
            List<Long> landlordIds = landlords.stream().map(User::getId).toList();
            if(!landlordIds.isEmpty()){
                queryWrapper.in(Order::getLandlordId, landlordIds);
            } else {
                return ExcelExportUtils.exportOrdersToExcel(List.of());
            }
        }

        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }

        queryWrapper.orderByDesc(Order::getCreateTime);

        List<Order> orders = orderMapper.selectList(queryWrapper);

        // 填充关联信息
        for (Order order : orders) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(userMapper.selectById(order.getTenantId()));
            order.setLandlord(userMapper.selectById(order.getLandlordId()));
        }

        return ExcelExportUtils.exportOrdersToExcel(orders);
    }

    /**
     * 检查租约是否可以续签
     */
    public boolean canRenewLease(Long leaseId) {
        LeaseRecord lease = leaseRecordMapper.selectById(leaseId);
        if (lease == null) {
            return false;
        }

        // 检查租约状态，必须是租赁中
        if (!lease.getStatus().equals(1)) {
            return false;
        }

        // 检查是否在到期前7天内
        LocalDate now = LocalDate.now();
        LocalDate endDate = lease.getEndDate();
        long daysUntilEnd = java.time.temporal.ChronoUnit.DAYS.between(now, endDate);

        // 必须在到期前7天内，且未到期
        return daysUntilEnd >= 0 && daysUntilEnd <= 7;
    }

    /**
     * 创建续签订单
     */
    @Transactional
    public Order createRenewalOrder(Long leaseId, BigDecimal amount, String paymentCycle) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 检查续签条件
        if (!canRenewLease(leaseId)) {
            throw new ServiceException("当前租约不符合续签条件，续签只能在到期前7天内申请");
        }

        // 获取原租约
        LeaseRecord originalLease = leaseRecordMapper.selectById(leaseId);

        // 验证当前用户是否为租客
        if (!currentUser.getId().equals(originalLease.getTenantId())) {
            throw new ServiceException("只有租客可以申请续签");
        }

        // 检查是否已有未完成的续签订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getIsRenewal, 1)
               .eq(Order::getOriginalOrderId, originalLease.getOrderId())
               .in(Order::getStatus, 0, 1); // 待支付或待确认
        Long existingCount = orderMapper.selectCount(wrapper);
        if (existingCount > 0) {
            throw new ServiceException("已存在未完成的续签订单，请先处理");
        }

        // 获取原订单
        Order originalOrder = orderMapper.selectById(originalLease.getOrderId());

        // 创建续签订单
        Order renewalOrder = new Order();
        renewalOrder.setOrderNo(generateOrderNo());
        renewalOrder.setHouseId(originalOrder.getHouseId());
        renewalOrder.setTenantId(originalOrder.getTenantId());
        renewalOrder.setLandlordId(originalOrder.getLandlordId());
        renewalOrder.setAmount(amount);
        renewalOrder.setDeposit(BigDecimal.ZERO); // 续签不需要押金
        renewalOrder.setStatus(OrderStatus.WAITING_PAYMENT.getValue());
        renewalOrder.setIsRenewal(1);
        renewalOrder.setOriginalOrderId(originalOrder.getId());
        renewalOrder.setCreateTime(LocalDateTime.now());
        renewalOrder.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(renewalOrder);

        log.info("租客 {} 创建续签订单成功，原租约ID：{}，新订单ID：{}",
                currentUser.getUsername(), leaseId, renewalOrder.getId());

        return renewalOrder;
    }

    /**
     * 确认续签订单
     */
    @Transactional
    public Order confirmRenewalOrder(Long orderId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 验证是否为续签订单
        if (order.getIsRenewal() == null || order.getIsRenewal() != 1) {
            throw new ServiceException("非续签订单");
        }

        // 验证操作权限
        if (!order.getLandlordId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }

        // 检查订单状态
        if (!order.getStatus().equals(OrderStatus.PAID_WAITING_CONFIRM.getValue())) {
            throw new ServiceException("订单状态不正确，无法确认");
        }

        // 获取原订单和原租约
        Order originalOrder = orderMapper.selectById(order.getOriginalOrderId());
        LambdaQueryWrapper<LeaseRecord> leaseWrapper = new LambdaQueryWrapper<>();
        leaseWrapper.eq(LeaseRecord::getOrderId, originalOrder.getId())
                   .eq(LeaseRecord::getStatus, 1); // 必须是租赁中
        LeaseRecord originalLease = leaseRecordMapper.selectOne(leaseWrapper);

        if (originalLease == null) {
            throw new ServiceException("原租约不存在或已结束");
        }

        // 再次检查续签条件
        if (!canRenewLease(originalLease.getId())) {
            throw new ServiceException("原租约不符合续签条件");
        }

        // 更新订单状态为已确认
        order.setStatus(OrderStatus.CONFIRMED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 创建续签租约记录
        leaseRecordService.createRenewalLeaseRecord(order, originalLease);

        // 注意：房屋状态保持为已出租(2)，不需要修改

        log.info("房东 {} 确认续签订单成功，订单ID：{}", currentUser.getUsername(), orderId);

        return order;
    }
} 