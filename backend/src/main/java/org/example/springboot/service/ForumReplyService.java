package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.ForumReply;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ForumReplyMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.SensitiveWordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ForumReplyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForumReplyService.class);

    @Resource
    private ForumReplyMapper forumReplyMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ForumPostService forumPostService;

    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    @Transactional
    public ForumReply createReply(ForumReply reply) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        if (StringUtils.isBlank(reply.getContent())) {
            throw new ServiceException("回复内容不能为空");
        }

        // ⭐ 新增：敏感词检测和过滤
        Set<String> sensitiveWords = sensitiveWordFilter.getSensitiveWords(reply.getContent());

        if (!sensitiveWords.isEmpty()) {
            LOGGER.warn("用户 {} 回复包含敏感词: {}", currentUser.getUsername(), sensitiveWords);
            throw new ServiceException("回复内容包含敏感词，请修改后重新发布");
        }

        // 自动过滤并替换敏感词（额外保护）
        reply.setContent(sensitiveWordFilter.filterSensitiveWord(reply.getContent()));

        reply.setUserId(currentUser.getId());
        reply.setStatus(1);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());

        forumReplyMapper.insert(reply);

        // 增加帖子回复数量
        forumPostService.incrementReplyCount(reply.getPostId());

        LOGGER.info("用户 {} 回复帖子 {}", currentUser.getUsername(), reply.getPostId());
        return reply;
    }

    public Page<ForumReply> getRepliesByPostId(Long postId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<ForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ForumReply::getPostId, postId)
                .eq(ForumReply::getStatus, 1)
                .orderByAsc(ForumReply::getCreateTime);

        Page<ForumReply> page = forumReplyMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillReplyInfo);

        return page;
    }

    /**
     * 删除回复（级联删除所有子孙回复，并更新帖子回复数）
     */
    @Transactional
    public void deleteReply(Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        ForumReply reply = forumReplyMapper.selectById(id);
        if (reply == null) {
            throw new ServiceException("回复不存在");
        }

        if (!reply.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("您没有权限删除该回复");
        }

        Long postId = reply.getPostId();

        // 递归删除所有子孙回复，并统计删除的总数
        int deletedCount = deleteReplyAndDescendants(id);

        // 更新帖子的回复数
        forumPostService.decrementReplyCount(postId, deletedCount);

        LOGGER.info("用户 {} 删除回复 {} 及其 {} 条子孙回复", currentUser.getUsername(), id, deletedCount - 1);
    }

    /**
     * 递归删除回复及其所有子孙回复
     * @param replyId 回复ID
     * @return 删除的回复总数（包含自身）
     */
    private int deleteReplyAndDescendants(Long replyId) {
        int count = 0;

        // 查找所有直接子回复
        LambdaQueryWrapper<ForumReply> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(ForumReply::getParentId, replyId)
                  .eq(ForumReply::getStatus, 1); // 只处理未删除的回复

        java.util.List<ForumReply> children = forumReplyMapper.selectList(childQuery);

        // 递归删除所有子回复
        for (ForumReply child : children) {
            count += deleteReplyAndDescendants(child.getId());
        }

        // 删除当前回复（软删除）
        ForumReply reply = forumReplyMapper.selectById(replyId);
        if (reply != null && reply.getStatus() == 1) {
            reply.setStatus(2); // 已删除
            reply.setUpdateTime(LocalDateTime.now());
            forumReplyMapper.updateById(reply);
            count++;
        }

        return count;
    }

    /**
     * 根据帖子ID删除所有回复（用于删除帖子时调用）
     * @param postId 帖子ID
     * @return 删除的回复数量
     */
    @Transactional
    public int deleteRepliesByPostId(Long postId) {
        LambdaQueryWrapper<ForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ForumReply::getPostId, postId)
                    .eq(ForumReply::getStatus, 1);

        java.util.List<ForumReply> replies = forumReplyMapper.selectList(queryWrapper);
        int count = replies.size();

        // 批量软删除所有回复
        for (ForumReply reply : replies) {
            reply.setStatus(2);
            reply.setUpdateTime(LocalDateTime.now());
            forumReplyMapper.updateById(reply);
        }

        LOGGER.info("删除帖子 {} 的所有回复，共 {} 条", postId, count);
        return count;
    }

    private void fillReplyInfo(ForumReply reply) {
        User user = userMapper.selectById(reply.getUserId());
        if (user != null) {
            reply.setUsername(user.getUsername());
            reply.setUserAvatar(user.getAvatar());
        }

        if (reply.getReplyToUserId() != null) {
            User replyToUser = userMapper.selectById(reply.getReplyToUserId());
            if (replyToUser != null) {
                reply.setReplyToUsername(replyToUser.getUsername());
            }
        }
    }
}
