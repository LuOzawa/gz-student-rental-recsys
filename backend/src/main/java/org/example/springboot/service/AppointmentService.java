package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Appointment;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AppointmentMapper;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约看房服务实现类
 */
@Service
public class AppointmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentService.class);

    @Resource
    private AppointmentMapper appointmentMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private UserBehaviorService userBehaviorService;

    /**
     * 创建预约
     */
    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 检查用户是否是租客
        if (!"TENANT".equals(currentUser.getRoleCode())) {
            throw new ServiceException("只有租客可以预约看房");
        }

        // ⭐ 新增：验证实名认证状态
        if (!userService.isUserVerified(currentUser.getId())) {
            User user = userMapper.selectById(currentUser.getId());
            if (user.getVerifyStatus() == null || user.getVerifyStatus() == 0) {
                throw new ServiceException("请先完成实名认证才能预约看房");
            } else if (user.getVerifyStatus() == 1) {
                throw new ServiceException("您的实名认证正在审核中，请耐心等待");
            } else if (user.getVerifyStatus() == 3) {
                String reason = user.getVerifyRejectReason() != null ? user.getVerifyRejectReason() : "请认真填写信息";
                throw new ServiceException("您的实名认证未通过：" + reason);
            }
        }

        // 验证房屋是否存在
        House house = houseMapper.selectById(appointment.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }

        // 检查是否是自己的房源
        if (house.getLandlordId().equals(currentUser.getId())) {
            throw new ServiceException("不能预约自己的房源");
        }

        // 检查房屋状态
        if (house.getStatus() != 1) {
            throw new ServiceException("该房屋暂不可预约");
        }

        // 检查是否已经预约过该房源(待确认或已确认状态)
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getTenantId, currentUser.getId())
                .eq(Appointment::getHouseId, appointment.getHouseId())
                .in(Appointment::getStatus, 0, 1); // 待确认或已确认
        Long count = appointmentMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ServiceException("您已预约过该房源，请勿重复预约");
        }

        // 检查预约日期是否是未来日期
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ServiceException("预约日期不能是过去的日期");
        }

        // ⭐ 新增：验证预约时间段的合理性（早上8点到晚上8点）
        if (!isValidTimeSlot(appointment.getAppointmentTime())) {
            throw new ServiceException("预约时间必须在早上8:00到晚上20:00之间");
        }

        // ⭐ 新增：检查用户在该时间段是否已有其他预约（避免用户时间冲突）
        LambdaQueryWrapper<Appointment> userConflictQuery = new LambdaQueryWrapper<>();
        userConflictQuery.eq(Appointment::getTenantId, currentUser.getId())
                .eq(Appointment::getAppointmentDate, appointment.getAppointmentDate())
                .eq(Appointment::getAppointmentTime, appointment.getAppointmentTime())
                .in(Appointment::getStatus, 0, 1); // 待确认或已确认
        Long userConflictCount = appointmentMapper.selectCount(userConflictQuery);
        if (userConflictCount > 0) {
            throw new ServiceException("您在该时间段已有其他预约，请选择其他时间");
        }

        // 检查时间段冲突 - 同一房源同一时间段只能有一个确认的预约
        LambdaQueryWrapper<Appointment> conflictQuery = new LambdaQueryWrapper<>();
        conflictQuery.eq(Appointment::getHouseId, appointment.getHouseId())
                .eq(Appointment::getAppointmentDate, appointment.getAppointmentDate())
                .eq(Appointment::getAppointmentTime, appointment.getAppointmentTime())
                .in(Appointment::getStatus, 0, 1); // 待确认或已确认
        Long conflictCount = appointmentMapper.selectCount(conflictQuery);
        if (conflictCount > 0) {
            throw new ServiceException("该时间段已被预约，请选择其他时间");
        }

        // 设置预约信息
        appointment.setTenantId(currentUser.getId());
        appointment.setLandlordId(house.getLandlordId());
        appointment.setStatus(0); // 待确认
        appointment.setCreateTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());

        appointmentMapper.insert(appointment);

        // Track appointment behavior for recommendations
        userBehaviorService.trackAppointment(appointment.getHouseId());

        LOGGER.info("用户 {} 创建预约看房，预约ID: {}", currentUser.getUsername(), appointment.getId());

        return appointment;
    }

    /**
     * 租客分页查询自己的预约
     */
    public Page<Appointment> getTenantAppointments(Integer status, String houseName, Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getTenantId, currentUser.getId());

        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }

        // 如果有房源名称筛选，先查询匹配的房源ID
        if (StringUtils.isNotBlank(houseName)) {
            LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
            houseQuery.like(House::getTitle, houseName);
            java.util.List<Long> houseIds = houseMapper.selectList(houseQuery).stream()
                    .map(House::getId).collect(java.util.stream.Collectors.toList());
            if (houseIds.isEmpty()) {
                // 没有匹配的房源，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Appointment::getHouseId, houseIds);
        }

        queryWrapper.orderByDesc(Appointment::getCreateTime);

        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充关联信息
        page.getRecords().forEach(this::fillAppointmentInfo);

        return page;
    }

    /**
     * 房东分页查询收到的预约
     */
    public Page<Appointment> getLandlordAppointments(Integer status, String houseName, String tenantName, Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getLandlordId, currentUser.getId());

        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }

        // 如果有房源名称筛选，先查询匹配的房源ID
        if (StringUtils.isNotBlank(houseName)) {
            LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
            houseQuery.like(House::getTitle, houseName);
            java.util.List<Long> houseIds = houseMapper.selectList(houseQuery).stream()
                    .map(House::getId).collect(java.util.stream.Collectors.toList());
            if (houseIds.isEmpty()) {
                // 没有匹配的房源，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Appointment::getHouseId, houseIds);
        }

        // 如果有租客姓名筛选，先查询匹配的租客ID
        if (StringUtils.isNotBlank(tenantName)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.and(wrapper -> wrapper
                    .like(User::getName, tenantName)
                    .or()
                    .like(User::getUsername, tenantName));
            java.util.List<Long> tenantIds = userMapper.selectList(userQuery).stream()
                    .map(User::getId).collect(java.util.stream.Collectors.toList());
            if (tenantIds.isEmpty()) {
                // 没有匹配的租客，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Appointment::getTenantId, tenantIds);
        }

        queryWrapper.orderByDesc(Appointment::getCreateTime);

        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充关联信息
        page.getRecords().forEach(this::fillAppointmentInfo);

        return page;
    }

    /**
     * 管理员分页查询所有预约
     */
    public Page<Appointment> getAllAppointments(Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }

        queryWrapper.orderByDesc(Appointment::getCreateTime);

        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充关联信息
        page.getRecords().forEach(this::fillAppointmentInfo);

        return page;
    }

    /**
     * 根据ID获取预约详情
     */
    public Appointment getById(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        fillAppointmentInfo(appointment);
        return appointment;
    }

    /**
     * 房东确认预约
     */
    @Transactional
    public Appointment confirmAppointment(Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        // 检查是否是房东本人
        if (!appointment.getLandlordId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限确认该预约");
        }

        // 检查预约状态
        if (appointment.getStatus() != 0) {
            throw new ServiceException("该预约已处理，不能重复确认");
        }

        appointment.setStatus(1); // 已确认
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        LOGGER.info("房东 {} 确认预约，预约ID: {}", currentUser.getUsername(), id);
        return appointment;
    }

    /**
     * 房东拒绝预约
     */
    @Transactional
    public Appointment rejectAppointment(Long id, String rejectReason) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        // 检查是否是房东本人
        if (!appointment.getLandlordId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限拒绝该预约");
        }

        // 检查预约状态
        if (appointment.getStatus() != 0) {
            throw new ServiceException("该预约已处理，不能重复拒绝");
        }

        if (StringUtils.isBlank(rejectReason)) {
            throw new ServiceException("请填写拒绝原因");
        }

        appointment.setStatus(2); // 已拒绝
        appointment.setRejectReason(rejectReason);
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        LOGGER.info("房东 {} 拒绝预约，预约ID: {}, 原因: {}", currentUser.getUsername(), id, rejectReason);
        return appointment;
    }

    /**
     * 完成预约
     */
    @Transactional
    public Appointment completeAppointment(Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        // 房东或租客都可以标记为已完成
        if (!appointment.getLandlordId().equals(currentUser.getId())
            && !appointment.getTenantId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限操作该预约");
        }

        // 检查预约状态
        if (appointment.getStatus() != 1) {
            throw new ServiceException("只有已确认的预约才能标记为已完成");
        }

        appointment.setStatus(3); // 已完成
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        LOGGER.info("用户 {} 完成预约，预约ID: {}", currentUser.getUsername(), id);
        return appointment;
    }

    /**
     * 租客取消预约
     */
    @Transactional
    public Appointment cancelAppointment(Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        // 检查是否是租客本人
        if (!appointment.getTenantId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限取消该预约");
        }

        // 检查预约状态
        if (appointment.getStatus() == 3 || appointment.getStatus() == 4) {
            throw new ServiceException("该预约已完成或已取消，不能再取消");
        }

        appointment.setStatus(4); // 已取消
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        LOGGER.info("租客 {} 取消预约，预约ID: {}", currentUser.getUsername(), id);
        return appointment;
    }

    /**
     * 租客重新安排预约
     */
    @Transactional
    public Appointment rescheduleAppointment(Long id, LocalDate newDate, String newTime) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        // 检查是否是租客本人
        if (!appointment.getTenantId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限修改该预约");
        }

        // 只有待确认的预约才能重新安排
        if (appointment.getStatus() != 0) {
            throw new ServiceException("只有待确认的预约才能重新安排");
        }

        // 检查新日期是否是未来日期
        if (newDate.isBefore(LocalDate.now())) {
            throw new ServiceException("预约日期不能是过去的日期");
        }

        // 检查新时间段是否冲突
        LambdaQueryWrapper<Appointment> conflictQuery = new LambdaQueryWrapper<>();
        conflictQuery.eq(Appointment::getHouseId, appointment.getHouseId())
                .eq(Appointment::getAppointmentDate, newDate)
                .eq(Appointment::getAppointmentTime, newTime)
                .in(Appointment::getStatus, 0, 1) // 待确认或已确认
                .ne(Appointment::getId, id); // 排除当前预约
        Long conflictCount = appointmentMapper.selectCount(conflictQuery);
        if (conflictCount > 0) {
            throw new ServiceException("该时间段已被预约，请选择其他时间");
        }

        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        LOGGER.info("租客 {} 重新安排预约，预约ID: {}", currentUser.getUsername(), id);
        return appointment;
    }

    /**
     * 自动过期处理 - 将过期的待确认预约标记为已取消
     * 此方法可以被定时任务调用
     */
    @Transactional
    public int autoExpireAppointments() {
        // 查询所有过期的待确认或已确认预约
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(Appointment::getAppointmentDate, LocalDate.now())
                .in(Appointment::getStatus, 0, 1); // 待确认或已确认

        java.util.List<Appointment> expiredAppointments = appointmentMapper.selectList(queryWrapper);

        int count = 0;
        for (Appointment appointment : expiredAppointments) {
            appointment.setStatus(4); // 设置为已取消
            appointment.setRejectReason("预约已过期，系统自动取消");
            appointment.setUpdateTime(LocalDateTime.now());
            appointmentMapper.updateById(appointment);
            count++;
        }

        if (count > 0) {
            LOGGER.info("自动过期处理: 共处理 {} 条过期预约", count);
        }

        return count;
    }

    /**
     * 获取房源的可用时间段
     * 返回指定日期该房源已被预约的时间段列表
     */
    public java.util.List<String> getBookedTimeSlots(Long houseId, LocalDate date) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getHouseId, houseId)
                .eq(Appointment::getAppointmentDate, date)
                .in(Appointment::getStatus, 0, 1); // 待确认或已确认

        java.util.List<Appointment> appointments = appointmentMapper.selectList(queryWrapper);
        return appointments.stream()
                .map(Appointment::getAppointmentTime)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 填充预约关联信息
     */
    private void fillAppointmentInfo(Appointment appointment) {
        // 填充房屋信息
        House house = houseMapper.selectById(appointment.getHouseId());
        appointment.setHouse(house);

        // 填充租客信息
        User tenant = userMapper.selectById(appointment.getTenantId());
        appointment.setTenant(tenant);

        // 填充房东信息
        User landlord = userMapper.selectById(appointment.getLandlordId());
        appointment.setLandlord(landlord);
    }

    /**
     * 房东获取预约列表
     */
    public Page<Appointment> getLandlordAppointments(Long landlordId, Integer status,
                                                      Integer currentPage, Integer size) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getLandlordId, landlordId);

        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }

        queryWrapper.orderByDesc(Appointment::getCreateTime);

        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充关联信息
        page.getRecords().forEach(this::fillAppointmentInfo);

        return page;
    }

    /**
     * 验证预约时间段是否在合理时间范围内（早上8:00 - 晚上20:00）
     * 时间格式示例："上午09:00-11:00", "下午14:00-16:00"
     */
    private boolean isValidTimeSlot(String timeSlot) {
        if (StringUtils.isBlank(timeSlot)) {
            return false;
        }

        try {
            // 提取时间段中的小时数字，例如 "上午09:00-11:00" 提取出 9 和 11
            String[] parts = timeSlot.split("-");
            if (parts.length != 2) {
                return false;
            }

            // 提取开始时间的小时（如 "上午09:00" 或 "下午14:00"）
            String startPart = parts[0].replaceAll("[^0-9]", "").substring(0, 2);
            String endPart = parts[1].replaceAll("[^0-9]", "").substring(0, 2);

            int startHour = Integer.parseInt(startPart);
            int endHour = Integer.parseInt(endPart);

            // 验证时间范围：8:00 - 20:00
            // 开始时间必须 >= 8, 结束时间必须 <= 20
            return startHour >= 8 && endHour <= 20 && startHour < endHour;
        } catch (Exception e) {
            LOGGER.warn("时间段格式验证失败: {}", timeSlot, e);
            return false;
        }
    }
}
