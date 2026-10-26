package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.House;
import org.example.springboot.entity.HouseComment;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseCommentMapper;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源评论服务实现类
 */
@Service
public class HouseCommentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HouseCommentService.class);

    @Resource
    private HouseCommentMapper houseCommentMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private org.example.springboot.mapper.LeaseRecordMapper leaseRecordMapper;

    /**
     * 创建评论
     * 区分留言和评价：
     * - type=2 (留言)：所有用户可发，不能带评分
     * - type=1 (评价)：仅在租或已到期的租客可发，必须带评分
     */
    @Transactional
    public HouseComment createComment(HouseComment comment) {
        // 获取当前登录用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 验证房屋是否存在
        House house = houseMapper.selectById(comment.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }

        // 判断是评价还是留言
        Integer type = comment.getType();
        if (type == null) {
            // 根据是否有评分自动判断类型
            type = (comment.getScore() != null) ? 1 : 2;
            comment.setType(type);
        }

        if (type == 1) {
            // 评价：需要租赁记录，必须带评分
            if (comment.getScore() == null) {
                throw new ServiceException("评价必须包含评分");
            }

            // 验证评分范围
            if (comment.getScore() < 1 || comment.getScore() > 5) {
                throw new ServiceException("评分必须在1-5之间");
            }

            // 检查用户角色是否为租客
            if (!"TENANT".equals(currentUser.getRoleCode())) {
                throw new ServiceException("只有租客才能对房源进行评分评价");
            }

            // 检查是否有该房源的租赁记录（租赁中或已结束）
            boolean hasLeaseRecord = leaseRecordMapper.selectCount(
                new LambdaQueryWrapper<org.example.springboot.entity.LeaseRecord>()
                    .eq(org.example.springboot.entity.LeaseRecord::getTenantId, currentUser.getId())
                    .eq(org.example.springboot.entity.LeaseRecord::getHouseId, comment.getHouseId())
                    .in(org.example.springboot.entity.LeaseRecord::getStatus, 1, 2) // 1:租赁中, 2:已结束
            ) > 0;

            if (!hasLeaseRecord) {
                throw new ServiceException("只有在租或已到期的租客才能进行评价");
            }

            // 检查是否已经评价过
            LambdaQueryWrapper<HouseComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HouseComment::getUserId, currentUser.getId())
                    .eq(HouseComment::getHouseId, comment.getHouseId())
                    .eq(HouseComment::getType, 1); // 评价类型
            Long count = houseCommentMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new ServiceException("您已评价过该房源");
            }
        } else if (type == 2) {
            // 留言：所有用户可发，不能带评分
            if (comment.getScore() != null) {
                throw new ServiceException("留言不能包含评分");
            }

            // 检查是否已经留言过
            LambdaQueryWrapper<HouseComment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HouseComment::getUserId, currentUser.getId())
                    .eq(HouseComment::getHouseId, comment.getHouseId())
                    .eq(HouseComment::getType, 2); // 留言类型
            Long count = houseCommentMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new ServiceException("您已留言过该房源");
            }
        } else {
            throw new ServiceException("无效的评论类型");
        }

        // 设置评论信息
        comment.setUserId(currentUser.getId());
        comment.setStatus(1); // 显示
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        houseCommentMapper.insert(comment);

        // 仅当为评价时才更新房源的平均评分
        if (type == 1) {
            updateHouseScore(comment.getHouseId());
            LOGGER.info("租客 {} 评价房源 {}, 评分: {}", currentUser.getUsername(), comment.getHouseId(), comment.getScore());
        } else {
            LOGGER.info("用户 {} 留言房源 {}", currentUser.getUsername(), comment.getHouseId());
        }

        return comment;
    }

    /**
     * 根据房源ID分页查询评论
     */
    public Page<HouseComment> getCommentsByHouseId(Long houseId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<HouseComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HouseComment::getHouseId, houseId)
                .eq(HouseComment::getStatus, 1) // 只显示状态为1的评论
                .orderByDesc(HouseComment::getCreateTime);

        Page<HouseComment> page = houseCommentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充用户信息
        page.getRecords().forEach(this::fillCommentInfo);

        return page;
    }

    /**
     * 分页查询所有评论(管理员)
     */
    public Page<HouseComment> getAllComments(Long houseId, Integer type, Integer status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<HouseComment> queryWrapper = new LambdaQueryWrapper<>();

        if (houseId != null) {
            queryWrapper.eq(HouseComment::getHouseId, houseId);
        }

        if (type != null) {
            queryWrapper.eq(HouseComment::getType, type);
        }

        if (status != null) {
            queryWrapper.eq(HouseComment::getStatus, status);
        }

        queryWrapper.orderByDesc(HouseComment::getCreateTime);

        Page<HouseComment> page = houseCommentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充关联信息
        page.getRecords().forEach(this::fillCommentInfo);

        return page;
    }

    /**
     * 分页查询房东房源的评论(房东)
     */
    public Page<HouseComment> getLandlordComments(Long houseId, Integer type, Integer currentPage, Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 获取房东的所有房源ID
        LambdaQueryWrapper<House> houseQuery = new LambdaQueryWrapper<>();
        houseQuery.eq(House::getLandlordId, currentUser.getId())
                .select(House::getId);
        List<House> houses = houseMapper.selectList(houseQuery);
        List<Long> houseIds = houses.stream().map(House::getId).toList();

        if (houseIds.isEmpty()) {
            return new Page<>(currentPage, size);
        }

        LambdaQueryWrapper<HouseComment> queryWrapper = new LambdaQueryWrapper<>();

        if (houseId != null) {
            // 如果指定了房源ID，确保该房源属于当前房东
            if (!houseIds.contains(houseId)) {
                throw new ServiceException("您没有权限查看该房源的评论");
            }
            queryWrapper.eq(HouseComment::getHouseId, houseId);
        } else {
            // 查询房东所有房源的评论
            queryWrapper.in(HouseComment::getHouseId, houseIds);
        }

        // 添加类型筛选
        if (type != null) {
            queryWrapper.eq(HouseComment::getType, type);
        }

        // 只显示状态为显示的评论
        queryWrapper.eq(HouseComment::getStatus, 1);
        queryWrapper.orderByDesc(HouseComment::getCreateTime);

        Page<HouseComment> page = houseCommentMapper.selectPage(new Page<>(currentPage, size), queryWrapper);

        // 填充关联信息
        page.getRecords().forEach(this::fillCommentInfo);

        return page;
    }

    /**
     * 房东回复评论
     * - 房东只能回复自己房源的评论
     */
    @Transactional
    public HouseComment landlordReplyComment(Long id, String replyContent) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        HouseComment comment = houseCommentMapper.selectById(id);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        // 获取房屋信息
        House house = houseMapper.selectById(comment.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }

        // 权限检查：房东只能回复自己房源的评论
        if (!house.getLandlordId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限回复该评论");
        }

        if (StringUtils.isBlank(replyContent)) {
            throw new ServiceException("回复内容不能为空");
        }

        // 房东回复保存到 replyContent 字段
        comment.setReplyContent(replyContent);
        comment.setReplyTime(LocalDateTime.now());
        comment.setReplyUserId(currentUser.getId());
        comment.setReplyUserType(1); // 1:房东
        comment.setUpdateTime(LocalDateTime.now());
        houseCommentMapper.updateById(comment);

        LOGGER.info("房东 {} 回复评论 {}", currentUser.getUsername(), id);

        return comment;
    }

    /**
     * 管理员回复评论
     * - 管理员可以回复所有评论
     */
    @Transactional
    public HouseComment adminReplyComment(Long id, String replyContent) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        // 权限检查：只有管理员可以调用此方法
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("您没有权限执行此操作");
        }

        HouseComment comment = houseCommentMapper.selectById(id);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        if (StringUtils.isBlank(replyContent)) {
            throw new ServiceException("回复内容不能为空");
        }

        // 管理员回复保存到 adminReplyContent 字段
        comment.setAdminReplyContent(replyContent);
        comment.setAdminReplyTime(LocalDateTime.now());
        comment.setAdminReplyUserId(currentUser.getId());
        comment.setUpdateTime(LocalDateTime.now());
        houseCommentMapper.updateById(comment);

        LOGGER.info("管理员 {} 回复评论 {}", currentUser.getUsername(), id);

        return comment;
    }

    /**
     * 房东/管理员回复评论（兼容旧接口）
     * - 管理员回复保存到 adminReplyContent
     * - 房东回复保存到 replyContent
     */
    @Transactional
    public HouseComment replyComment(Long id, String replyContent) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        boolean isAdmin = "ADMIN".equals(currentUser.getRoleCode());

        if (isAdmin) {
            return adminReplyComment(id, replyContent);
        } else {
            return landlordReplyComment(id, replyContent);
        }
    }

    /**
     * 删除评论(管理员或评论者本人)
     */
    @Transactional
    public void deleteComment(Long id) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        HouseComment comment = houseCommentMapper.selectById(id);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        // 只有管理员或评论者本人可以删除
        if (!"ADMIN".equals(currentUser.getRoleCode())
            && !comment.getUserId().equals(currentUser.getId())) {
            throw new ServiceException("您没有权限删除该评论");
        }

        Long houseId = comment.getHouseId();
        houseCommentMapper.deleteById(id);

        // 更新房源的平均评分和评论数量
        updateHouseScore(houseId);

        LOGGER.info("用户 {} 删除评论 {}", currentUser.getUsername(), id);
    }

    /**
     * 隐藏/显示评论(管理员)
     */
    @Transactional
    public HouseComment updateCommentStatus(Long id, Integer status) {
        HouseComment comment = houseCommentMapper.selectById(id);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        comment.setStatus(status);
        comment.setUpdateTime(LocalDateTime.now());
        houseCommentMapper.updateById(comment);

        LOGGER.info("管理员修改评论 {} 状态为 {}", id, status);

        return comment;
    }

    /**
     * 更新房源的平均评分和评论数量
     * 只计算评价类型(type=1)的评论
     */
    private void updateHouseScore(Long houseId) {
        LambdaQueryWrapper<HouseComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HouseComment::getHouseId, houseId)
                .eq(HouseComment::getStatus, 1) // 只统计显示的评论
                .eq(HouseComment::getType, 1) // 只统计评价类型
                .isNotNull(HouseComment::getScore); // 确保有评分

        List<HouseComment> reviews = houseCommentMapper.selectList(queryWrapper);

        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return;
        }

        // 计算平均评分
        if (reviews.isEmpty()) {
            house.setAvgScore(BigDecimal.ZERO);
            house.setCommentCount(0);
        } else {
            double avgScore = reviews.stream()
                    .mapToInt(HouseComment::getScore)
                    .average()
                    .orElse(0.0);

            house.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
            house.setCommentCount(reviews.size());
        }

        houseMapper.updateById(house);
        LOGGER.info("更新房源 {} 的平均评分: {}, 评价数: {}", houseId, house.getAvgScore(), house.getCommentCount());
    }

    /**
     * 填充评论关联信息
     */
    private void fillCommentInfo(HouseComment comment) {
        // 填充用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setUserAvatar(user.getAvatar());
        }

        // 填充房屋信息
        House house = houseMapper.selectById(comment.getHouseId());
        comment.setHouse(house);

        // 填充房东回复者名称
        if (comment.getReplyUserId() != null) {
            User replyUser = userMapper.selectById(comment.getReplyUserId());
            if (replyUser != null) {
                comment.setReplyUserName(replyUser.getName() != null ? replyUser.getName() : replyUser.getUsername());
            }
        }

        // 填充管理员回复者名称
        if (comment.getAdminReplyUserId() != null) {
            User adminReplyUser = userMapper.selectById(comment.getAdminReplyUserId());
            if (adminReplyUser != null) {
                comment.setAdminReplyUserName(adminReplyUser.getName() != null ? adminReplyUser.getName() : adminReplyUser.getUsername());
            }
        }
    }
}
