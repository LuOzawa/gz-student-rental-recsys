package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Feedback;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.FeedbackMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeedbackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackService.class);

    @Resource
    private FeedbackMapper feedbackMapper;

    @Resource
    private UserMapper userMapper;

    @Transactional
    public Feedback createFeedback(Feedback feedback) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        if (StringUtils.isBlank(feedback.getTitle())) {
            throw new ServiceException("标题不能为空");
        }

        if (StringUtils.isBlank(feedback.getContent())) {
            throw new ServiceException("内容不能为空");
        }

        feedback.setUserId(currentUser.getId());
        feedback.setStatus(0);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());

        feedbackMapper.insert(feedback);
        LOGGER.info("用户 {} 提交反馈 {}", currentUser.getUsername(), feedback.getId());

        return feedback;
    }

    public Page<Feedback> getUserFeedbacks(Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Feedback::getUserId, currentUser.getId())
                .orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillFeedbackInfo);

        return page;
    }

    public Page<Feedback> getAllFeedbacks(String type, Integer status, String username, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq(Feedback::getType, type);
        }

        if (status != null) {
            queryWrapper.eq(Feedback::getStatus, status);
        }

        // 如果有用户名筛选，先查询匹配的用户ID
        if (StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.and(wrapper -> wrapper
                    .like(User::getName, username)
                    .or()
                    .like(User::getUsername, username));
            java.util.List<Long> userIds = userMapper.selectList(userQuery).stream()
                    .map(User::getId).collect(java.util.stream.Collectors.toList());
            if (userIds.isEmpty()) {
                // 没有匹配的用户，返回空结果
                return new Page<>(currentPage, size);
            }
            queryWrapper.in(Feedback::getUserId, userIds);
        }

        queryWrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillFeedbackInfo);

        return page;
    }

    @Transactional
    public Feedback handleFeedback(Long id, String reply, Integer status) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }

        feedback.setReply(reply);
        feedback.setStatus(status);
        feedback.setHandlerId(currentUser.getId());
        feedback.setHandleTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        LOGGER.info("管理员 {} 处理反馈 {}", currentUser.getUsername(), id);
        return feedback;
    }

    private void fillFeedbackInfo(Feedback feedback) {
        User user = userMapper.selectById(feedback.getUserId());
        if (user != null) {
            feedback.setUsername(user.getUsername());
        }

        if (feedback.getHandlerId() != null) {
            User handler = userMapper.selectById(feedback.getHandlerId());
            if (handler != null) {
                feedback.setHandlerName(handler.getName());
            }
        }
    }
}
