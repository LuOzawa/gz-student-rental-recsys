package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.CreditRecord;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.CreditRecordMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CreditRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreditRecordService.class);

    @Resource
    private CreditRecordMapper creditRecordMapper;

    @Resource
    private UserMapper userMapper;

    @Transactional
    public void addCredit(Long userId, Integer value, String reason, Long relatedId, Long operatorId) {
        if (value <= 0) {
            throw new ServiceException("增加值必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 创建信用记录
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeType("增加");
        record.setChangeValue(value);
        record.setReason(reason);
        record.setRelatedId(relatedId);
        record.setOperatorId(operatorId);
        record.setCreateTime(LocalDateTime.now());
        creditRecordMapper.insert(record);

        // 更新用户信用分
        user.setCreditScore(user.getCreditScore() + value);
        userMapper.updateById(user);

        LOGGER.info("用户 {} 信用分增加 {}, 原因: {}", user.getUsername(), value, reason);
    }

    @Transactional
    public void deductCredit(Long userId, Integer value, String reason, Long relatedId, Long operatorId) {
        if (value <= 0) {
            throw new ServiceException("扣除值必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 创建信用记录
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeType("扣除");
        record.setChangeValue(-value);
        record.setReason(reason);
        record.setRelatedId(relatedId);
        record.setOperatorId(operatorId);
        record.setCreateTime(LocalDateTime.now());
        creditRecordMapper.insert(record);

        // 更新用户信用分
        int newScore = user.getCreditScore() - value;
        user.setCreditScore(Math.max(0, newScore)); // 信用分不能为负
        userMapper.updateById(user);

        LOGGER.info("用户 {} 信用分扣除 {}, 原因: {}", user.getUsername(), value, reason);
    }

    public Page<CreditRecord> getCreditRecordsByUserId(Long userId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<CreditRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CreditRecord::getUserId, userId)
                .orderByDesc(CreditRecord::getCreateTime);

        Page<CreditRecord> page = creditRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillRecordInfo);

        return page;
    }

    public Page<CreditRecord> getAllCreditRecords(String username, String changeType, Integer currentPage, Integer size) {
        LambdaQueryWrapper<CreditRecord> queryWrapper = new LambdaQueryWrapper<>();

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
            queryWrapper.in(CreditRecord::getUserId, userIds);
        }

        // 变更类型筛选
        if (changeType != null && !changeType.isEmpty()) {
            queryWrapper.eq(CreditRecord::getChangeType, changeType);
        }

        queryWrapper.orderByDesc(CreditRecord::getCreateTime);

        Page<CreditRecord> page = creditRecordMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillRecordInfo);

        return page;
    }

    private void fillRecordInfo(CreditRecord record) {
        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            record.setUsername(user.getUsername());
        }

        if (record.getOperatorId() != null) {
            User operator = userMapper.selectById(record.getOperatorId());
            if (operator != null) {
                record.setOperatorName(operator.getName());
            }
        }
    }
}
