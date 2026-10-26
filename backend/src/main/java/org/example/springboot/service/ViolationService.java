package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;
import org.example.springboot.entity.Violation;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.mapper.ViolationMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ViolationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViolationService.class);

    @Resource
    private ViolationMapper violationMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CreditRecordService creditRecordService;

    @Resource
    private HouseMapper houseMapper;

    /**
     * 用户举报房源
     * userId 设置为被举报的房东ID
     */
    @Transactional
    public Violation reportHouse(Violation violation) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 根据房源ID获取房东ID
        if (violation.getRelatedId() == null) {
            throw new ServiceException("房源ID不能为空");
        }
        House house = houseMapper.selectById(violation.getRelatedId());
        if (house == null) {
            throw new ServiceException("房源不存在");
        }

        // userId 设置为被举报的房东ID（违规用户）
        violation.setUserId(house.getLandlordId());
        violation.setType("房源举报");
        violation.setRelatedType("house");
        violation.setStatus(0); // 待处理
        violation.setCreateTime(LocalDateTime.now());
        violation.setUpdateTime(LocalDateTime.now());

        violationMapper.insert(violation);
        LOGGER.info("用户 {} 举报房东 {} 的房源 {}", currentUser.getUsername(), house.getLandlordId(), violation.getRelatedId());
        return violation;
    }

    @Transactional
    public Violation createViolation(Violation violation) {
        violation.setStatus(0);
        violation.setCreateTime(LocalDateTime.now());
        violation.setUpdateTime(LocalDateTime.now());
        violationMapper.insert(violation);
        return violation;
    }

    public Page<Violation> getViolationsByPage(Long userId, Integer status, String username, String type, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Violation> queryWrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            queryWrapper.eq(Violation::getUserId, userId);
        }

        if (status != null) {
            queryWrapper.eq(Violation::getStatus, status);
        }

        // 如果有用户名筛选，先查询匹配的用户ID
        if (username != null && !username.isEmpty()) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.and(wrapper -> wrapper
                    .like(User::getName, username)
                    .or()
                    .like(User::getUsername, username));
            java.util.List<Long> userIds = userMapper.selectList(userQuery).stream()
                    .map(User::getId).collect(java.util.stream.Collectors.toList());
            if (userIds.isEmpty()) {
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Violation::getUserId, userIds);
        }

        // 违规类型筛选
        if (type != null && !type.isEmpty()) {
            queryWrapper.like(Violation::getType, type);
        }

        queryWrapper.orderByDesc(Violation::getCreateTime);

        Page<Violation> page = violationMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillViolationInfo);

        return page;
    }

    public Page<Violation> getUserViolations(Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        return getViolationsByPage(currentUser.getId(), null, null, null, currentPage, size);
    }

    @Transactional
    public Violation handleViolation(Long id, String penalty, Integer creditDeduction, String handleRemark) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Violation violation = violationMapper.selectById(id);
        if (violation == null) {
            throw new ServiceException("违规记录不存在");
        }

        violation.setPenalty(penalty);
        violation.setCreditDeduction(creditDeduction);
        violation.setHandleRemark(handleRemark);
        violation.setHandlerId(currentUser.getId());
        violation.setHandleTime(LocalDateTime.now());
        violation.setStatus(1);
        violation.setUpdateTime(LocalDateTime.now());
        violationMapper.updateById(violation);

        // 扣除信用分
        if (creditDeduction != null && creditDeduction > 0) {
            creditRecordService.deductCredit(violation.getUserId(), creditDeduction,
                    "违规扣分：" + violation.getDescription(), id, currentUser.getId());
        }

        LOGGER.info("管理员 {} 处理违规记录 {}", currentUser.getUsername(), id);
        return violation;
    }

    private void fillViolationInfo(Violation violation) {
        User user = userMapper.selectById(violation.getUserId());
        if (user != null) {
            violation.setUser(user);
            violation.setUsername(user.getUsername());
        }

        if (violation.getHandlerId() != null) {
            User handler = userMapper.selectById(violation.getHandlerId());
            if (handler != null) {
                violation.setHandler(handler);
                violation.setHandlerName(handler.getName());
            }
        }
    }
}
