package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.ForumPost;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.entity.ForumReply;
import org.example.springboot.mapper.ForumCategoryMapper;
import org.example.springboot.mapper.ForumPostMapper;
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
public class ForumPostService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForumPostService.class);

    @Resource
    private ForumPostMapper forumPostMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ForumCategoryMapper forumCategoryMapper;

    @Resource
    private ForumReplyMapper forumReplyMapper;

    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    @Transactional
    public ForumPost createPost(ForumPost post) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        if (StringUtils.isBlank(post.getTitle())) {
            throw new ServiceException("标题不能为空");
        }

        if (StringUtils.isBlank(post.getContent())) {
            throw new ServiceException("内容不能为空");
        }

        // ⭐ 新增：敏感词检测和过滤
        Set<String> titleSensitiveWords = sensitiveWordFilter.getSensitiveWords(post.getTitle());
        Set<String> contentSensitiveWords = sensitiveWordFilter.getSensitiveWords(post.getContent());

        if (!titleSensitiveWords.isEmpty() || !contentSensitiveWords.isEmpty()) {
            LOGGER.warn("用户 {} 发布帖子包含敏感词: 标题{}, 内容{}",
                    currentUser.getUsername(), titleSensitiveWords, contentSensitiveWords);
            throw new ServiceException("标题或内容包含敏感词，请修改后重新发布");
        }

        // 自动过滤并替换敏感词（额外保护）
        post.setTitle(sensitiveWordFilter.filterSensitiveWord(post.getTitle()));
        post.setContent(sensitiveWordFilter.filterSensitiveWord(post.getContent()));

        post.setUserId(currentUser.getId());
        post.setViewCount(0);
        post.setReplyCount(0);
        post.setIsTop(0);
        post.setIsEssence(0);
        post.setStatus(1);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        forumPostMapper.insert(post);
        LOGGER.info("用户 {} 发布帖子 {}", currentUser.getUsername(), post.getId());

        return post;
    }

    public Page<ForumPost> getPostsByPage(Long categoryId, String keyword, Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<ForumPost> queryWrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            queryWrapper.eq(ForumPost::getCategoryId, categoryId);
        }

        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(ForumPost::getTitle, keyword)
                    .or()
                    .like(ForumPost::getContent, keyword));
        }

        // 如果指定了状态则按状态筛选，否则默认只显示正常状态
        if (status != null) {
            queryWrapper.eq(ForumPost::getStatus, status);
        } else {
            queryWrapper.eq(ForumPost::getStatus, 1);
        }

        queryWrapper.orderByDesc(ForumPost::getIsTop)
                .orderByDesc(ForumPost::getCreateTime);

        Page<ForumPost> page = forumPostMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        page.getRecords().forEach(this::fillPostInfo);

        return page;
    }

    @Transactional
    public ForumPost getPostById(Long id) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null || post.getStatus() != 1) {
            throw new ServiceException("帖子不存在");
        }

        // 增加浏览次数
        post.setViewCount(post.getViewCount() + 1);
        forumPostMapper.updateById(post);

        fillPostInfo(post);
        return post;
    }

    @Transactional
    public ForumPost updatePost(Long id, ForumPost post) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        ForumPost existing = forumPostMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("帖子不存在");
        }

        if (!existing.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("您没有权限编辑该帖子");
        }

        // ⭐ 新增：敏感词检测和过滤
        Set<String> titleSensitiveWords = sensitiveWordFilter.getSensitiveWords(post.getTitle());
        Set<String> contentSensitiveWords = sensitiveWordFilter.getSensitiveWords(post.getContent());

        if (!titleSensitiveWords.isEmpty() || !contentSensitiveWords.isEmpty()) {
            LOGGER.warn("用户 {} 编辑帖子包含敏感词: 标题{}, 内容{}",
                    currentUser.getUsername(), titleSensitiveWords, contentSensitiveWords);
            throw new ServiceException("标题或内容包含敏感词，请修改后重新提交");
        }

        existing.setTitle(sensitiveWordFilter.filterSensitiveWord(post.getTitle()));
        existing.setContent(sensitiveWordFilter.filterSensitiveWord(post.getContent()));
        existing.setImages(post.getImages());
        existing.setUpdateTime(LocalDateTime.now());

        forumPostMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除帖子（同时删除帖子下的所有回复）
     */
    @Transactional
    public void deletePost(Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        ForumPost post = forumPostMapper.selectById(id);
        if (post == null) {
            throw new ServiceException("帖子不存在");
        }

        if (!post.getUserId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("您没有权限删除该帖子");
        }

        // 删除帖子下的所有回复（软删除）
        int deletedReplyCount = deleteAllRepliesByPostId(id);

        // 删除帖子（软删除）
        post.setStatus(2); // 已删除
        post.setReplyCount(0); // 回复数清零
        forumPostMapper.updateById(post);

        LOGGER.info("用户 {} 删除帖子 {} 及其 {} 条回复", currentUser.getUsername(), id, deletedReplyCount);
    }

    /**
     * 删除帖子下的所有回复
     * @param postId 帖子ID
     * @return 删除的回复数量
     */
    private int deleteAllRepliesByPostId(Long postId) {
        LambdaQueryWrapper<ForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ForumReply::getPostId, postId)
                    .eq(ForumReply::getStatus, 1);

        java.util.List<ForumReply> replies = forumReplyMapper.selectList(queryWrapper);
        int count = replies.size();

        // 批量软删除所有回复
        for (ForumReply reply : replies) {
            reply.setStatus(2);
            reply.setUpdateTime(java.time.LocalDateTime.now());
            forumReplyMapper.updateById(reply);
        }

        return count;
    }

    @Transactional
    public void setTop(Long id, Integer isTop) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null) {
            throw new ServiceException("帖子不存在");
        }
        post.setIsTop(isTop);
        forumPostMapper.updateById(post);
    }

    @Transactional
    public void setEssence(Long id, Integer isEssence) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null) {
            throw new ServiceException("帖子不存在");
        }
        post.setIsEssence(isEssence);
        forumPostMapper.updateById(post);
    }

    @Transactional
    public void incrementReplyCount(Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post != null) {
            post.setReplyCount(post.getReplyCount() + 1);
            forumPostMapper.updateById(post);
        }
    }

    /**
     * 减少帖子回复数
     * @param postId 帖子ID
     * @param count 减少的数量
     */
    @Transactional
    public void decrementReplyCount(Long postId, int count) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post != null) {
            int newCount = post.getReplyCount() - count;
            post.setReplyCount(Math.max(0, newCount)); // 确保不小于0
            forumPostMapper.updateById(post);
            LOGGER.info("帖子 {} 回复数减少 {}，当前回复数: {}", postId, count, post.getReplyCount());
        }
    }

    private void fillPostInfo(ForumPost post) {
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            post.setUsername(user.getUsername());
            post.setUserAvatar(user.getAvatar());
        }

        if (post.getCategoryId() != null) {
            post.setCategory(forumCategoryMapper.selectById(post.getCategoryId()));
        }
    }
}
