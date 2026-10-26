/*
Navicat MySQL Data Transfer

Source Server         : lzh
Source Server Version : 80100
Source Host           : localhost:3306
Source Database       : kb251018

Target Server Type    : MYSQL
Target Server Version : 80100
File Encoding         : 65001

Date: 2025-12-12 16:39:58
*/

SET FOREIGN_KEY_CHECKS=0;
DROP DATABASE IF EXISTS kb251018;
CREATE DATABASE kb251018 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE kb251018;
-- ----------------------------
-- Table structure for announcement
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告内容',
  `admin_id` bigint NOT NULL COMMENT '发布管理员ID',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:下架,1:发布)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='公告表';

-- ----------------------------
-- Records of announcement
-- ----------------------------
INSERT INTO `announcement` VALUES ('2', '五月租房优惠活动', '五月租房优惠活动开始啦！新用户首次租房可享受9折优惠，活动时间：2025年5月15日-2025年5月31日。', '1', '1', '2025-05-15 09:30:00', '2025-05-15 09:30:00');
INSERT INTO `announcement` VALUES ('5', '1111', '111111', '1', '1', '2025-11-26 11:25:04', '2025-11-26 11:25:04');

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `tenant_id` bigint NOT NULL COMMENT '租客ID',
  `landlord_id` bigint NOT NULL COMMENT '房东ID',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `appointment_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约时间段',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '留言信息',
  `status` tinyint DEFAULT '0' COMMENT '状态(0:待确认,1:已确认,2:已拒绝,3:已完成,4:已取消)',
  `reject_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '拒绝原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_house` (`house_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_landlord` (`landlord_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='预约看房表';

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES ('1', '4', '4', '3', '2025-11-26', '上午10:00-12:00', '13800138003', '我想看看这个房子，方便的话请尽快确认', '3', null, '2025-11-24 10:00:00', '2025-12-11 16:55:46');
INSERT INTO `appointment` VALUES ('2', '6', '5', '2', '2025-11-27', '下午14:00-16:00', '13800138004', '希望能实地看房，了解周边环境', '0', null, '2025-11-24 11:00:00', '2025-11-24 11:00:00');
INSERT INTO `appointment` VALUES ('3', '7', '4', '3', '2025-11-25', '上午09:00-11:00', '13800138003', '想看看房子的采光情况', '3', null, '2025-11-23 15:00:00', '2025-11-25 11:30:00');

-- ----------------------------
-- Table structure for carousel
-- ----------------------------
DROP TABLE IF EXISTS `carousel`;
CREATE TABLE `carousel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '轮播图标题',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '轮播图描述',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片URL',
  `link_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '链接URL',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `sort` int DEFAULT '0' COMMENT '排序(数值越小越靠前)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='轮播图表';

-- ----------------------------
-- Records of carousel
-- ----------------------------
INSERT INTO `carousel` VALUES ('1', '寻找理想住所，开启美好生活', '我们提供各类优质房源，满足您的不同需求', '/img/1747466278007.jpeg', '/houses', '1', '1', '2025-05-15 09:00:00', '2025-05-15 09:00:00');
INSERT INTO `carousel` VALUES ('2', '安心租房，品质保障', '全部房源实名认证，真实可靠', '/img/1747466271575.jpeg', '/houses', '1', '2', '2025-05-15 09:01:00', '2025-05-15 09:01:00');
INSERT INTO `carousel` VALUES ('3', '便捷的租房流程', '在线预约看房，足不出户轻松租房', '/img/1747466264612.jpeg', '/houses', '1', '3', '2025-05-15 09:02:00', '2025-05-15 09:02:00');

-- ----------------------------
-- Table structure for credit_record
-- ----------------------------
DROP TABLE IF EXISTS `credit_record`;
CREATE TABLE `credit_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `change_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更类型(增加/扣除)',
  `change_value` int NOT NULL COMMENT '变更数值',
  `reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更原因',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='信用记录表';

-- ----------------------------
-- Records of credit_record
-- ----------------------------
INSERT INTO `credit_record` VALUES ('1', '3', '扣除', '-10', '发布虚假房源信息', '1', '1', '2025-09-15 14:30:00');
INSERT INTO `credit_record` VALUES ('2', '5', '扣除', '-5', '在论坛发布恶意评论', '2', '1', '2025-10-20 16:00:00');
INSERT INTO `credit_record` VALUES ('3', '2', '扣除', '-3', '多次爽约看房', '3', '1', '2025-11-20 11:00:00');
INSERT INTO `credit_record` VALUES ('4', '3', '增加', '5', '房源评价优秀，获得租客好评', null, '1', '2025-10-01 10:00:00');
INSERT INTO `credit_record` VALUES ('5', '2', '增加', '10', '按时履约，获得优秀房东评价', null, '1', '2025-11-01 15:00:00');

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `user_id` bigint NOT NULL COMMENT '反馈用户ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈类型(建议/投诉/bug)',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈内容',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图片(JSON格式)',
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式',
  `status` tinyint DEFAULT '0' COMMENT '状态(0:待处理,1:处理中,2:已处理,3:已关闭)',
  `reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '管理员回复',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='反馈表';

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES ('1', '4', '功能建议', '希望增加房源收藏功能', '目前浏览过的房源找不到了，希望能添加收藏功能，方便下次快速找到感兴趣的房源。', null, '13800138003', '2', '感谢您的建议！收藏功能已经在我们的开发计划中，预计下个版本会上线。', '1', '2025-08-15 10:00:00', '2025-08-10 14:30:00', '2025-08-15 10:00:00');
INSERT INTO `feedback` VALUES ('2', '5', '投诉', '房东信息不真实', '预约看房时发现房东提供的联系方式是错误的，无法联系上，希望平台能够加强审核。', null, '13900139004', '1', null, '1', null, '2025-09-20 16:00:00', '2025-09-21 09:00:00');
INSERT INTO `feedback` VALUES ('3', '2', 'Bug反馈', '支付页面卡顿', '在支付租金时页面经常卡住，需要刷新多次才能完成支付，影响使用体验。建议优化一下支付流程。', '[\"bug_screenshot1.jpg\"]', '13700137002', '2', '我们已经定位到问题，是支付接口响应超时导致的。已在最新版本中修复，请您更新后再试。', '1', '2025-10-10 11:00:00', '2025-10-05 09:30:00', '2025-10-10 11:00:00');
INSERT INTO `feedback` VALUES ('4', '4', '功能建议', '增加地图搜索功能', '希望能在地图上直接搜索附近的房源，这样可以更直观地了解房源位置。', null, null, '0', null, null, null, '2025-11-15 20:00:00', '2025-11-15 20:00:00');

-- ----------------------------
-- Table structure for forum_category
-- ----------------------------
DROP TABLE IF EXISTS `forum_category`;
CREATE TABLE `forum_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='论坛分类表';

-- ----------------------------
-- Records of forum_category
-- ----------------------------
INSERT INTO `forum_category` VALUES ('1', '租房经验', '分享租房心得和经验', 'el-icon-house', '1', '1', '2025-05-15 10:00:00', '2025-05-15 10:00:00');
INSERT INTO `forum_category` VALUES ('2', '房屋装修', '讨论房屋装修相关话题', 'el-icon-brush', '2', '1', '2025-05-15 10:00:00', '2025-05-15 10:00:00');
INSERT INTO `forum_category` VALUES ('3', '生活交流', '日常生活话题交流', 'el-icon-chat-dot-round', '3', '1', '2025-05-15 10:00:00', '2025-05-15 10:00:00');
INSERT INTO `forum_category` VALUES ('4', '投诉建议', '平台投诉和建议', 'el-icon-warning', '4', '1', '2025-05-15 10:00:00', '2025-05-15 10:00:00');

-- ----------------------------
-- Table structure for forum_post
-- ----------------------------
DROP TABLE IF EXISTS `forum_post`;
CREATE TABLE `forum_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子内容',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图片(JSON格式)',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `reply_count` int DEFAULT '0' COMMENT '回复数量',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶(0:否,1:是)',
  `is_essence` tinyint DEFAULT '0' COMMENT '是否精华(0:否,1:是)',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:隐藏,1:正常,2:已删除)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category` (`category_id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='论坛帖子表';

-- ----------------------------
-- Records of forum_post
-- ----------------------------
INSERT INTO `forum_post` VALUES ('1', '1', '4', '新手租房必看！避坑指南', '作为一个租房多年的老租客，想分享一些租房的经验...\n1. 一定要实地看房\n2. 仔细检查房屋设施\n3. 签合同前确认所有细节\n4. 保留好所有凭证', null, '159', '8', '0', '1', '1', '2025-05-20 10:00:00', '2025-05-20 10:00:00');
INSERT INTO `forum_post` VALUES ('2', '2', '5', '分享我的出租房改造经验', '刚租的房子有点老旧，花了一个周末时间简单改造了一下，效果还不错！分享给大家一些小技巧...', null, '89', '5', '0', '1', '1', '2025-06-01 15:30:00', '2025-06-01 15:30:00');
INSERT INTO `forum_post` VALUES ('3', '3', '4', '有没有在朝阳区租房的小伙伴？', '刚搬到朝阳区，想认识一些附近的朋友，可以一起吃饭逛街~', null, '45', '12', '0', '0', '1', '2025-10-05 20:00:00', '2025-10-05 20:00:00');
INSERT INTO `forum_post` VALUES ('4', '4', '5', '建议增加房源筛选功能', '希望平台能增加更多的筛选条件，比如是否允许养宠物、有无独立卫浴等', null, '32', '3', '0', '0', '1', '2025-10-15 09:00:00', '2025-10-15 09:00:00');

-- ----------------------------
-- Table structure for forum_reply
-- ----------------------------
DROP TABLE IF EXISTS `forum_reply`;
CREATE TABLE `forum_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回复ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '回复用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回复内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父回复ID(用于回复的回复)',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复给谁(用户ID)',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:隐藏,1:正常,2:已删除)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_post` (`post_id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='论坛回复表';

-- ----------------------------
-- Records of forum_reply
-- ----------------------------
INSERT INTO `forum_reply` VALUES ('1', '1', '5', '感谢分享！这些建议太实用了', null, null, '1', '2025-05-20 11:00:00', '2025-05-20 11:00:00');
INSERT INTO `forum_reply` VALUES ('2', '1', '2', '确实，实地看房非常重要', null, null, '1', '2025-05-20 12:30:00', '2025-05-20 12:30:00');
INSERT INTO `forum_reply` VALUES ('3', '1', '4', '@landlord1 感谢房东支持！', '2', '2', '1', '2025-05-20 13:00:00', '2025-05-20 13:00:00');
INSERT INTO `forum_reply` VALUES ('4', '2', '4', '改造前后对比图可以分享一下吗？', null, null, '1', '2025-06-01 16:00:00', '2025-06-01 16:00:00');
INSERT INTO `forum_reply` VALUES ('5', '2', '5', '@tenant1 我稍后整理一下发上来', '4', '4', '1', '2025-06-01 16:30:00', '2025-06-01 16:30:00');
INSERT INTO `forum_reply` VALUES ('6', '3', '5', '我也在朝阳区！可以加个微信', null, null, '1', '2025-10-05 20:30:00', '2025-10-05 20:30:00');
INSERT INTO `forum_reply` VALUES ('7', '3', '2', '朝阳区的小伙伴们可以组个群', null, null, '1', '2025-10-05 21:00:00', '2025-10-05 21:00:00');
INSERT INTO `forum_reply` VALUES ('8', '4', '1', '您的建议很好，我们会考虑添加', null, null, '1', '2025-10-15 10:00:00', '2025-10-15 10:00:00');
INSERT INTO `forum_reply` VALUES ('9', '4', '4', '支持！养宠物的筛选真的很需要', null, null, '1', '2025-10-15 11:00:00', '2025-10-15 11:00:00');

-- ----------------------------
-- Table structure for house
-- ----------------------------
DROP TABLE IF EXISTS `house`;
CREATE TABLE `house` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房屋ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房屋标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '房屋描述',
  `area` decimal(10,2) NOT NULL COMMENT '面积(平方米)',
  `price` decimal(10,2) NOT NULL COMMENT '价格(元/月)',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '地址',
  `type_id` bigint NOT NULL COMMENT '房屋类型ID',
  `landlord_id` bigint NOT NULL COMMENT '房东ID',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:下架,1:待出租,2:已出租)',
  `audit_status` tinyint DEFAULT '0' COMMENT '审核状态(0:待审核,1:审核通过,2:审核拒绝)',
  `audit_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审核拒绝原因',
  `avg_score` decimal(3,2) DEFAULT '0.00' COMMENT '平均评分(0-5)',
  `comment_count` int DEFAULT '0' COMMENT '评论数量',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `favorite_count` int DEFAULT '0' COMMENT '收藏次数',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '城市',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区县',
  `community_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '小区名称',
  `decoration` tinyint DEFAULT '1' COMMENT '装修情况(0:毛坯,1:简装,2:精装,3:豪装)',
  `floor` int DEFAULT '1' COMMENT '楼层',
  `total_floors` int DEFAULT '1' COMMENT '总楼层数',
  `orientation` tinyint DEFAULT NULL COMMENT '朝向(0:东,1:南,2:西,3:北,4:东南,5:西南,6:东北,7:西北,8:南北通透)',
  `build_year` int DEFAULT NULL COMMENT '建筑年代',
  `building_type` tinyint DEFAULT '0' COMMENT '房屋类型(0:普通住宅,1:公寓,2:别墅,3:loft,4:复式)',
  `payment_method` tinyint DEFAULT '0' COMMENT '押付方式(0:押一付一,1:押一付三,2:押三付一,3:押一付六,4:半年付,5:年付,6:无押金)',
  `min_lease_term` int DEFAULT '1' COMMENT '最短租期(月)',
  `check_in_time` date DEFAULT NULL COMMENT '入住时间',
  `service_fee` decimal(10,2) DEFAULT '0.00' COMMENT '服务费(元/月)',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `is_recommended` tinyint DEFAULT '0' COMMENT '是否推荐(0:否,1:是)',
  `recommend_sort` int DEFAULT '0' COMMENT '推荐排序值',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '房屋图片(JSON格式)',
  `facilities` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '配套设施(JSON格式)',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_landlord` (`landlord_id`) USING BTREE,
  KEY `idx_type` (`type_id`) USING BTREE,
  KEY `idx_city_district` (`city`, `district`) USING BTREE,
  KEY `idx_price` (`price`) USING BTREE,
  KEY `idx_payment_method` (`payment_method`) USING BTREE,
  KEY `idx_status_audit` (`status`, `audit_status`) USING BTREE,
  KEY `idx_latlng` (`latitude`, `longitude`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='房屋信息表';

-- ----------------------------
-- Records of house
-- ----------------------------
INSERT INTO `house` (`id`, `title`, `description`, `area`, `price`, `address`, `type_id`, `landlord_id`, `status`, `audit_status`, `avg_score`, `comment_count`, `view_count`, `favorite_count`, `city`, `district`, `community_name`, `decoration`, `floor`, `total_floors`, `orientation`, `build_year`, `building_type`, `payment_method`, `min_lease_term`, `check_in_time`, `service_fee`, `contact_phone`, `is_recommended`, `recommend_sort`, `images`, `facilities`, `latitude`, `longitude`, `create_time`, `update_time`) VALUES
('1', '大学城精装两室一厅', '位于广州大学城中心的精装两室一厅，交通便利，周边配套齐全，拎包入住。近地铁4号线大学城站。', '85.50', '3500.00', '广州市番禺区大学城中环东路8号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '大学城北', 2, 5, 12, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/1747462284644.jpg\",\"/img/1747462286822.jpg\",\"/img/1747462294409.jpg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0458, 113.3875, '2025-05-15 10:20:00', '2025-05-17 14:11:36'),
('2', '地铁口温馨单间', '紧邻地铁4号线大学城南站口的温馨单间，家具齐全，适合单身人士居住。', '30.00', '1800.00', '广州市番禺区大学城中环西路12号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '大学城南', 2, 3, 6, 8, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/1747462267665.jpg\",\"/img/1747462268824.jpg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0324, 113.3956, '2025-05-15 10:21:00', '2025-05-17 14:11:10'),
('3', '大学城花园三室两厅', '位于广州大学城花园小区的三室两厅，环境优美，适合家庭居住。近广工校区。', '120.00', '5000.00', '广州市番禺区大学城外环东路6号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广工小区', 2, 1, 6, 8, 2017, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/1747462229576.jpg\",\"/img/1747462233802.jpg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0489, 113.4012, '2025-05-15 10:22:00', '2025-05-17 14:10:38'),
('4', '中山大学旁合租单间', '位于中山大学旁的合租单间，安静舒适，适合学生。步行10分钟到校区。', '15.00', '1200.00', '广州市番禺区大学城中环东路6号', '2', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中大小区', 1, 2, 5, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/1747462213715.jpg\",\"/img/1747462215787.jpg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0387, 113.3892, '2025-05-15 10:23:00', '2025-05-17 14:10:17'),
('5', '大学城精装一居室', '位于广州大学城商业区的精装一居室，生活便利，拎包入住。近GOGO新天地。', '55.00', '2800.00', '广州市番禺区大学城中环西路10号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '贝岗村', 2, 8, 15, 1, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/1747462198453.jpg\",\"/img/1747462201040.jpg\",\"/img/1747462203472.jpg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":false,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0356, 113.3967, '2025-05-15 10:24:00', '2025-05-17 14:10:05'),
('6', '大学城温馨小户型', '适合单身或情侣，生活便利。近广大商业区。', '35.00', '2000.00', '广州市番禺区大学城中二路5号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广大小区', 1, 2, 6, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0423, 113.3934, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('7', '华工精装三居室', '宽敞明亮，适合家庭居住。近华工校区。', '110.00', '4800.00', '广州市番禺区大学城东二路8号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '华工小区', 2, 3, 10, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0512, 113.4056, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('8', '地铁旁一居室', '交通便利，拎包入住。近地铁7号线。', '45.00', '2600.00', '广州市番禺区大学城内环西路2号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '穗石村', 1, 1, 5, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0298, 113.3823, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('9', '南亭阳光两室一厅', '南北通透，采光好。近南亭村。', '80.00', '3500.00', '广州市番禺区大学城南二路20号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '南亭村', 2, 4, 8, 8, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0267, 113.3901, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('10', '北亭经济合租房', '价格实惠，适合上班族。近北亭村。', '20.00', '1200.00', '广州市番禺区大学城北二路5号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '北亭村', 1, 3, 6, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0489, 113.3812, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('11', '高层景观房', '视野开阔，环境优美。近大学城中心湖。', '90.00', '4000.00', '广州市番禺区大学城中心一街3号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中心湖', 2, 10, 18, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0389, 113.3923, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('12', '贝岗温馨合租单间', '合租单间，安静舒适。近贝岗市场。', '18.00', '1100.00', '广州市番禺区大学城中环东路9号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '贝岗村', 1, 2, 5, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0345, 113.3956, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('13', '精致一居室', '精致装修，拎包入住。近大学城体育中心。', '40.00', '2300.00', '广州市番禺区大学城中环路6号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中心区', 2, 5, 12, 1, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0378, 113.3934, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('14', '穗石地铁口合租房', '交通便利，生活方便。近地铁站。', '22.00', '1300.00', '广州市番禺区大学城外环西路7号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '穗石村', 1, 3, 6, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0298, 113.3834, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('15', '谷围岛舒适两居室', '适合小家庭，环境安静。近谷围新村。', '70.00', '3200.00', '广州市番禺区大学城东二路5号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '谷围岛', 2, 2, 6, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0467, 113.4023, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('16', '广药单间', '适合学生，安静舒适。近广东药学院校区。', '16.00', '1000.00', '广州市番禺区大学城中环东路30号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广药小区', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0434, 113.3956, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('17', '豪华四居室', '高档小区，适合大家庭。近大学城别墅区。', '150.00', '6500.00', '广州市番禺区大学城内环东路8号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '别墅区', 3, 8, 20, 8, 2017, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0423, 113.3967, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('18', '便捷单身公寓', '地铁沿线，上班方便。近大学城北站。', '28.00', '1800.00', '广州市番禺区大学城中二路12号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '北亭公寓', 2, 6, 12, 1, 2019, 1, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0498, 113.3845, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('19', '阳光三居室', '南北通透，采光极好。近华师校区。', '120.00', '5200.00', '广州市番禺区大学城西二路5号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '华师小区', 2, 5, 15, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0367, 113.3901, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('20', '晓岸经济合租房', '价格实惠，适合打工族。近晓岸兰庭。', '15.00', '900.00', '广州市番禺区大学城外环西路11号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '晓岸兰庭', 1, 1, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":false,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0289, 113.3878, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('21', 'GOGO新天地精装两居', '商业区中心，生活便利。近GOGO新天地广场。', '85.00', '3800.00', '广州市番禺区大学城中环西路22号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '贝岗商业区', 2, 4, 12, 8, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0345, 113.3978, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('22', '南亭花园单间', '环境优美，安静舒适。近南亭花园。', '20.00', '1300.00', '广州市番禺区大学城南一路2号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '南亭村', 1, 1, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0256, 113.3889, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('23', '高档一居室', '精装修，拎包入住。近大学城体育馆。', '50.00', '2800.00', '广州市番禺区大学城中环西二路9号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '体育中心', 2, 9, 18, 1, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0323, 113.3923, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('24', '大学城北两居室', '交通便利，适合上班族。近大学城北站。', '75.00', '3300.00', '广州市番禺区大学城中二路6号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '北亭区', 1, 3, 8, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0478, 113.3834, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('25', '北亭经济单间', '价格实惠，基础设施齐全。近北亭广场。', '18.00', '1100.00', '广州市番禺区大学城北一路3号', '2', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '北亭村', 1, 5, 6, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":false,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0498, 113.3812, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('26', '广外两居室', '适合教职工或研究生，环境安静。近广外校区。', '80.00', '3500.00', '广州市番禺区大学城外环东路6号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广外小区', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0456, 113.4023, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('27', '商务区单身公寓', '商务区中心，适合商务人士。近大学城商务中心。', '45.00', '2600.00', '广州市番禺区大学城中环路1号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '商务中心', 2, 8, 20, 1, 2019, 1, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0378, 113.3945, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('28', '岭南印象园合租房', '价格实惠，适合年轻人。近岭南印象园。', '16.00', '950.00', '广州市番禺区大学城外环西路7号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '岭南印象', 1, 1, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":false,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0289, 113.3856, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('29', '豪华三居室', '高档小区，环境优美。近大学城湖畔。', '130.00', '5800.00', '广州市番禺区大学城内环西路15号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '湖畔', 3, 8, 18, 1, 2017, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0367, 113.3912, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('30', '南亭地铁旁单间', '交通便利，上班方便。近南亭地铁站。', '20.00', '1200.00', '广州市番禺区大学城南二路10号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '南亭村', 1, 1, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0267, 113.3891, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('31', '中心区精装两居室', '精致装修，拎包入住。近大学城中心。', '85.00', '3800.00', '广州市番禺区大学城中环东路3号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中心区', 2, 4, 12, 8, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0389, 113.3934, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('32', '星海合租房', '适合学生，价格实惠。近星海学院校区。', '18.00', '1000.00', '广州市番禺区大学城中环西路68号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '星海学院', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0312, 113.3945, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('33', '中心湖阳光一居室', '采光好，温馨舒适。近中心湖公园。', '48.00', '2500.00', '广州市番禺区大学城中心二路15号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中心湖', 2, 1, 6, NULL, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0378, 113.3923, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('34', '广州大道路段单间', '价格实惠，基础设施齐全。近广州大道。', '15.00', '900.00', '广州市番禺区大学城外环东路2号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '外环东区', 1, 1, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":false,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0467, 113.4034, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('35', '大学城舒适三居室', '适合家庭，环境安静。近广大校区。', '110.00', '4600.00', '广州市番禺区大学城中环西路4号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广大小区', 2, 2, 16, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0412, 113.3956, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('36', '市中心单身公寓', '市中心地段，生活便利。近大学城商业中心。', '40.00', '2400.00', '广州市番禺区大学城中环路8号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '商业中心', 2, 1, 8, NULL, 2019, 1, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0367, 113.3945, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('37', '贝岗花园合租', '环境优美，安静舒适。近贝岗花园。', '22.00', '1300.00', '广州市番禺区大学城中环东路5号', '2', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '贝岗村', 2, 3, 12, 4, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0345, 113.3967, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('38', '精装两室一厅', '精致装修，拎包入住。近广工校区。', '75.00', '3400.00', '广州市番禺区大学城外环东路12号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广工小区', 2, 2, 8, 8, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0489, 113.4001, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('39', '南亭经济合租房', '价格实惠，适合打工族。近南亭村。', '16.00', '950.00', '广州市番禺区大学城南一路3号', '2', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '南亭村', 1, 1, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":false,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0256, 113.3878, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('40', '豪华四室两厅', '高档小区，适合大家庭。近大学城别墅区。', '160.00', '6800.00', '广州市番禺区大学城内环东路2号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '别墅区', 3, 5, 20, 1, 2017, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0423, 113.3978, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('41', '地铁旁小户型', '交通便利，上班方便。近大学城南站。', '35.00', '2100.00', '广州市番禺区大学城中环西路10号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '地铁南站', 2, 3, 8, NULL, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0312, 113.3934, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('42', '大学城单人间', '适合学生，价格实惠。近广美校区。', '12.00', '800.00', '广州市番禺区大学城外环西路5号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '广美小区', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":false,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0298, 113.3845, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('43', '阳光三室两厅', '南北通透，采光极好。近大学城湖景。', '130.00', '5500.00', '广州市番禺区大学城内环东路8号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '湖景', 2, 6, 12, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0398, 113.3934, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('44', '经济型小户型', '价格实惠，基础设施齐全。近北亭广场。', '30.00', '1800.00', '广州市番禺区大学城北二路8号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '北亭广场', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0489, 113.3823, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('45', '舒适两室一厅', '适合小家庭，环境安静。近谷围岛。', '80.00', '3600.00', '广州市番禺区大学城东二路9号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '谷围岛', 2, 4, 8, 1, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0478, 113.4012, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('46', '高层景观两居', '视野开阔，环境优美。近大学城制高点。', '85.00', '3900.00', '广州市番禺区大学城中环东路2号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中心区', 2, 5, 10, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0398, 113.3923, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('47', '商务区精装单间', '商务区中心，适合商务人士。近商务中心。', '25.00', '1600.00', '广州市番禺区大学城中环路8号', '2', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '商务中心', 2, 3, 12, NULL, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0367, 113.3945, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('48', '温馨一室一厅', '温馨舒适，拎包入住。近华工校区。', '50.00', '2700.00', '广州市番禺区大学城东二路16号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '华工小区', 2, 2, 8, NULL, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0501, 113.4045, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('49', '经济合租单间', '价格实惠，适合年轻人。近穗石村。', '18.00', '1100.00', '广州市番禺区大学城内环西路12号', '2', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '穗石村', 1, 2, 5, NULL, 2021, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":false,\"television\":false,\"waterHeater\":true}', 23.0298, 113.3834, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('50', '豪华复式公寓', '复式结构，空间宽敞。近大学城高端区。', '120.00', '5800.00', '广州市番禺区大学城内环东路2号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '高端区', 2, 2, 8, NULL, 2019, 4, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0412, 113.3967, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('51', '地铁口小户型', '交通便利，上班方便。近大学城南站。', '40.00', '2300.00', '广州市番禺区大学城中环西路15号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '地铁南站', 2, 5, 15, 1, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0323, 113.3945, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('52', '大学附近合租', '适合学生，安静舒适。近中大校区。', '20.00', '1200.00', '广州市番禺区大学城中环东路25号', '2', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中大小区', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":false,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":false,\"waterHeater\":true}', 23.0389, 113.3891, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('53', '阳光四居室', '南北通透，适合大家庭。近大学城湖景。', '150.00', '6200.00', '广州市番禺区大学城内环西路10号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '湖景', 2, 7, 25, 8, 2018, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0367, 113.3923, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('54', '经济型两居室', '价格实惠，基础设施齐全。近岭南印象园。', '70.00', '3000.00', '广州市番禺区大学城外环西路2号', '1', '2', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '岭南印象', 1, 2, 6, NULL, 2020, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0278, 113.3845, '2025-06-17 12:35:42', '2025-06-17 12:35:42'),
('55', '舒适三室一厅', '适合家庭，环境安静。近中心湖公园。', '100.00', '4200.00', '广州市番禺区大学城中心一路8号', '1', '3', '1', 1, '0.00', 0, 0, 0, '广州市', '番禺区', '中心湖', 2, 3, 6, 8, 2019, 0, 0, 1, NULL, '0.00', NULL, 0, 0, '[\"/img/default.jpeg\"]', '{\"wifi\":true,\"elevator\":true,\"airConditioner\":true,\"washer\":true,\"refrigerator\":true,\"television\":true,\"waterHeater\":true}', 23.0378, 113.3912, '2025-06-17 12:35:42', '2025-06-17 12:35:42');

-- ----------------------------
-- Table structure for house_comment
-- ----------------------------
DROP TABLE IF EXISTS `house_comment`;
CREATE TABLE `house_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '类型(1:评价,2:留言)',
  `score` int DEFAULT NULL COMMENT '评分(1-5,留言时为NULL)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '评论内容',
  `reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '房东回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '房东回复时间',
  `reply_user_id` bigint DEFAULT NULL COMMENT '房东回复者ID',
  `reply_user_type` tinyint DEFAULT NULL COMMENT '回复者类型(1:房东,2:管理员)-已废弃保留兼容',
  `admin_reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '管理员回复内容',
  `admin_reply_time` datetime DEFAULT NULL COMMENT '管理员回复时间',
  `admin_reply_user_id` bigint DEFAULT NULL COMMENT '管理员回复者ID',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:隐藏,1:显示)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_house` (`house_id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_reply_user` (`reply_user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='房源评论表(包含评价和留言)';

-- ----------------------------
-- Records of house_comment
-- ----------------------------
INSERT INTO `house_comment` VALUES ('1', '1', '4', '1', '5', '房子位置很好，交通便利，房东人也很好！', '谢谢您的好评，祝您住得愉快！', '2025-05-18 10:00:00', null, null, null, null, null, '1', '2025-05-17 20:00:00', '2025-05-18 10:00:00');
INSERT INTO `house_comment` VALUES ('2', '2', '5', '1', '4', '房间干净整洁，设施齐全，性价比不错。', null, null, null, null, null, null, null, '1', '2025-05-18 15:00:00', '2025-05-18 15:00:00');
INSERT INTO `house_comment` VALUES ('3', '3', '4', '1', '5', '环境很好，小区安静，适合家庭居住', '感谢支持，有任何问题随时联系', '2025-06-18 09:00:00', null, null, null, null, null, '1', '2025-06-17 18:00:00', '2025-06-18 09:00:00');
INSERT INTO `house_comment` VALUES ('4', '1', '5', '1', '4', '采光不错，就是楼层有点高', null, null, null, null, null, null, null, '1', '2025-10-10 14:00:00', '2025-10-10 14:00:00');
INSERT INTO `house_comment` VALUES ('5', '5', '4', '1', '5', '很满意，会推荐给朋友！', '感谢推荐！', '2025-10-12 10:00:00', null, null, null, null, null, '1', '2025-10-11 16:00:00', '2025-11-26 11:15:58');

-- ----------------------------
-- Table structure for house_type
-- ----------------------------
DROP TABLE IF EXISTS `house_type`;
CREATE TABLE `house_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型名称(合租/整租)',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '类型描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='房屋类型表';

-- ----------------------------
-- Records of house_type
-- ----------------------------
INSERT INTO `house_type` VALUES ('1', '整租', '整套房屋出租，独立使用所有空间', '2025-05-15 10:10:00', '2025-05-15 10:10:00');
INSERT INTO `house_type` VALUES ('2', '合租', '与他人合租房屋，共享部分公共空间', '2025-05-15 10:11:00', '2025-05-15 10:11:00');

-- ----------------------------
-- Table structure for lease_record
-- ----------------------------
DROP TABLE IF EXISTS `lease_record`;
CREATE TABLE `lease_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `tenant_id` bigint NOT NULL COMMENT '租客ID',
  `landlord_id` bigint NOT NULL COMMENT '房东ID',
  `start_date` date NOT NULL COMMENT '租期开始日期',
  `end_date` date NOT NULL COMMENT '租期结束日期',
  `rent_amount` decimal(10,2) NOT NULL COMMENT '租金金额',
  `payment_cycle` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'MONTHLY' COMMENT '支付周期(MONTHLY/QUARTERLY/YEARLY)',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:已取消,1:租赁中,2:已结租,3:已退租)',
  `actual_end_date` date DEFAULT NULL COMMENT '实际结束日期',
  `contract_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '合同文件URL',
  `evaluation_score` int DEFAULT NULL COMMENT '房屋评价分数(1-5)',
  `landlord_credit_score` int DEFAULT NULL COMMENT '房东信用评分(1-5)',
  `evaluation_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '评价内容',
  `is_renewal` tinyint DEFAULT '0' COMMENT '是否续签(0:否,1:是)',
  `original_lease_id` bigint DEFAULT NULL COMMENT '原租约ID(续签时使用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order` (`order_id`) USING BTREE,
  KEY `idx_house` (`house_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_landlord` (`landlord_id`) USING BTREE,
  KEY `idx_original_lease_id` (`original_lease_id`) USING BTREE,
  KEY `idx_lease_renewal_status` (`original_lease_id`,`is_renewal`,`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='租赁记录表';

-- ----------------------------
-- Records of lease_record
-- ----------------------------
INSERT INTO `lease_record` VALUES ('1', '1', '5', '4', '2', '2025-06-01', '2026-05-31', '2800.00', 'MONTHLY', '1', null, '/contract/contract_36d5224f84f048acbb4adb29a8e5fd7f.pdf', '5', '5', '不错！', '0', null, '2025-05-15 11:35:00', '2025-05-15 11:35:00');
INSERT INTO `lease_record` VALUES ('2', '2', '2', '5', '2', '2025-05-17', '2026-05-17', '1800.00', 'MONTHLY', '1', null, '/contract/contract_f8eefa59a1a04619b4d7d22cb59de5b0.pdf', null, null, null, '0', null, '2025-05-17 15:25:31', '2025-05-17 15:25:31');
INSERT INTO `lease_record` VALUES ('3', '6', '3', '8', '3', '2025-06-17', '2026-06-17', '5000.00', 'MONTHLY', '1', null, '/contract/contract_3e623bdc0e9c47f58558da2cc9765356.pdf', null, null, null, '0', null, '2025-06-17 12:02:25', '2025-06-17 12:02:24');
INSERT INTO `lease_record` VALUES ('4', '7', '9', '4', '3', '2025-10-07', '2026-10-07', '3500.00', 'MONTHLY', '1', null, '/contract/contract_53e278208d0e454eac066d5f504bca52.pdf', null, null, null, '0', null, '2025-10-07 15:59:48', '2025-10-07 15:59:49');
INSERT INTO `lease_record` VALUES ('5', '8', '1', '5', '2', '2024-12-01', '2025-12-01', '3500.00', 'MONTHLY', '1', null, '/contract/contract_renewal_test_1.pdf', null, null, null, '0', null, '2024-11-01 10:05:00', '2024-11-01 10:05:00');
INSERT INTO `lease_record` VALUES ('6', '9', '4', '4', '3', '2024-01-15', '2025-01-15', '1200.00', 'MONTHLY', '2', '2025-01-15', '/contract/contract_renewal_test_2.pdf', '4', '4', '房子不错，准备续约', '0', null, '2024-01-15 14:05:00', '2025-01-15 00:00:00');
INSERT INTO `lease_record` VALUES ('7', '10', '4', '4', '3', '2025-01-15', '2026-01-15', '1300.00', 'MONTHLY', '2', '2026-01-15', '/contract/contract_renewal_test_3.pdf', '5', '5', '续签一年，房东服务很好', '1', '6', '2025-01-10 11:05:00', '2026-01-15 00:00:00');
INSERT INTO `lease_record` VALUES ('8', '11', '4', '4', '3', '2026-01-15', '2027-01-15', '1400.00', 'MONTHLY', '1', null, '/contract/contract_renewal_test_4.pdf', null, null, null, '1', '7', '2026-01-05 15:05:00', '2026-01-05 15:05:00');

-- ----------------------------
-- Table structure for `order`
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `tenant_id` bigint NOT NULL COMMENT '租客ID',
  `landlord_id` bigint NOT NULL COMMENT '房东ID',
  `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
  `deposit` decimal(10,2) DEFAULT '0.00' COMMENT '押金金额',
  `status` tinyint DEFAULT '0' COMMENT '状态(0:待支付,1:已支付待确认,2:已确认,3:已取消,4:已退款)',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `payment_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付方式',
  `is_renewal` int DEFAULT '0' COMMENT '是否续签订单(0:否,1:是)',
  `original_order_id` bigint DEFAULT NULL COMMENT '原订单ID(续签时使用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  KEY `idx_house` (`house_id`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`) USING BTREE,
  KEY `idx_landlord` (`landlord_id`) USING BTREE,
  KEY `idx_original_order_id` (`original_order_id`) USING BTREE,
  KEY `idx_order_renewal_status` (`original_order_id`,`is_renewal`,`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单表';

-- ----------------------------
-- Records of `order`
-- ----------------------------
INSERT INTO `order` VALUES ('1', 'ORD202505150001', '5', '4', '2', '33600.00', '2800.00', '2', '2025-05-15 11:30:00', '支付平台', '0', null, '2025-05-15 11:00:00', '2025-05-15 11:35:00');
INSERT INTO `order` VALUES ('2', 'ORD202505150002', '2', '5', '2', '1800.00', '1800.00', '2', '2025-05-15 14:30:00', '微信支付', '0', null, '2025-05-15 14:00:00', '2025-05-17 15:25:31');
INSERT INTO `order` VALUES ('3', 'ORD202505150003', '1', '4', '2', '3500.00', '3500.00', '2', '2025-05-17 00:34:37', '支付平台', '0', null, '2025-05-15 16:00:00', '2025-05-17 00:37:11');
INSERT INTO `order` VALUES ('4', 'ORD174741285103394f1', '4', '4', '3', '1200.00', '1200.00', '4', '2025-05-17 00:34:30', '支付平台', '0', null, '2025-05-17 00:27:31', '2025-05-17 00:34:34');
INSERT INTO `order` VALUES ('5', 'ORD1747466667711aecd', '4', '4', '3', '1200.00', '1200.00', '1', '2025-05-17 15:24:37', '支付平台', '0', null, '2025-05-17 15:24:28', '2025-05-17 15:24:37');
INSERT INTO `order` VALUES ('6', 'ORD17501328914781657', '3', '8', '3', '5000.00', '5000.00', '2', '2025-06-17 12:01:36', '微信支付', '0', null, '2025-06-17 12:01:31', '2025-06-17 12:02:24');
INSERT INTO `order` VALUES ('7', 'ORD1759823919332eaf4', '9', '4', '3', '10500.00', '3500.00', '2', '2025-10-07 15:58:44', '微信支付', '0', null, '2025-10-07 15:58:39', '2025-10-07 15:59:49');
INSERT INTO `order` VALUES ('8', 'ORD202511010001', '1', '5', '2', '3500.00', '3500.00', '2', '2024-11-01 10:00:00', '支付平台', '0', null, '2024-11-01 09:30:00', '2024-11-01 10:05:00');
INSERT INTO `order` VALUES ('9', 'ORD202401150001', '4', '4', '3', '14400.00', '1200.00', '2', '2024-01-15 14:00:00', '微信支付', '0', null, '2024-01-15 13:30:00', '2024-01-15 14:05:00');
INSERT INTO `order` VALUES ('10', 'ORD202501100001', '4', '4', '3', '15600.00', '0.00', '2', '2025-01-10 11:00:00', '支付平台', '1', '9', '2025-01-10 10:30:00', '2025-01-10 11:05:00');
INSERT INTO `order` VALUES ('11', 'ORD202601050001', '4', '4', '3', '16800.00', '0.00', '2', '2026-01-05 15:00:00', '微信支付', '1', '10', '2026-01-05 14:30:00', '2026-01-05 15:05:00');

-- ----------------------------
-- Table structure for transaction
-- ----------------------------
DROP TABLE IF EXISTS `transaction`;
CREATE TABLE `transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` tinyint NOT NULL COMMENT '类型(1:收入,2:支出)',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '交易描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order` (`order_id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='交易记录表';

-- ----------------------------
-- Records of transaction
-- ----------------------------
INSERT INTO `transaction` VALUES ('1', '1', '4', '2', '33600.00', '租金支付-商圈精装一居室-年付', '2025-05-15 11:30:00');
INSERT INTO `transaction` VALUES ('2', '1', '2', '1', '33600.00', '租金收入-商圈精装一居室-年付', '2025-05-15 11:30:00');
INSERT INTO `transaction` VALUES ('3', '2', '5', '2', '1800.00', '租金支付-地铁口温馨单间-月付', '2025-05-15 14:30:00');
INSERT INTO `transaction` VALUES ('4', '2', '2', '1', '1800.00', '租金收入-地铁口温馨单间-月付', '2025-05-15 14:30:00');
INSERT INTO `transaction` VALUES ('5', '4', '4', '2', '1200.00', '租金支付-订单号ORD174741285103394f1', '2025-05-17 00:34:30');
INSERT INTO `transaction` VALUES ('6', '4', '3', '1', '1200.00', '租金收入-订单号ORD174741285103394f1', '2025-05-17 00:34:30');
INSERT INTO `transaction` VALUES ('7', '4', '4', '1', '1200.00', '退款收入-订单号ORD174741285103394f1', '2025-05-17 00:34:34');
INSERT INTO `transaction` VALUES ('8', '4', '3', '2', '1200.00', '退款支出-订单号ORD174741285103394f1', '2025-05-17 00:34:34');
INSERT INTO `transaction` VALUES ('9', '3', '4', '2', '3500.00', '租金支付-订单号ORD202505150003', '2025-05-17 00:34:37');
INSERT INTO `transaction` VALUES ('10', '3', '2', '1', '3500.00', '租金收入-订单号ORD202505150003', '2025-05-17 00:34:37');
INSERT INTO `transaction` VALUES ('11', '5', '4', '2', '1200.00', '租金支付-订单号ORD1747466667711aecd', '2025-05-17 15:24:37');
INSERT INTO `transaction` VALUES ('12', '5', '3', '1', '1200.00', '租金收入-订单号ORD1747466667711aecd', '2025-05-17 15:24:37');
INSERT INTO `transaction` VALUES ('13', '6', '8', '2', '5000.00', '租金支付-订单号ORD17501328914781657', '2025-06-17 12:01:36');
INSERT INTO `transaction` VALUES ('14', '6', '3', '1', '5000.00', '租金收入-订单号ORD17501328914781657', '2025-06-17 12:01:36');
INSERT INTO `transaction` VALUES ('15', '7', '4', '2', '10500.00', '租金支付-订单号ORD1759823919332eaf4', '2025-10-07 15:58:44');
INSERT INTO `transaction` VALUES ('16', '7', '3', '1', '10500.00', '租金收入-订单号ORD1759823919332eaf4', '2025-10-07 15:58:44');
INSERT INTO `transaction` VALUES ('17', '8', '5', '2', '3500.00', '租金支付-阳光花园精装两居-月付', '2024-11-01 10:00:00');
INSERT INTO `transaction` VALUES ('18', '8', '2', '1', '3500.00', '租金收入-阳光花园精装两居-月付', '2024-11-01 10:00:00');
INSERT INTO `transaction` VALUES ('19', '9', '4', '2', '14400.00', '租金支付-公园旁舒适一居-年付', '2024-01-15 14:00:00');
INSERT INTO `transaction` VALUES ('20', '9', '3', '1', '14400.00', '租金收入-公园旁舒适一居-年付', '2024-01-15 14:00:00');
INSERT INTO `transaction` VALUES ('21', '10', '4', '2', '15600.00', '续签租金支付-公园旁舒适一居-年付', '2025-01-10 11:00:00');
INSERT INTO `transaction` VALUES ('22', '10', '3', '1', '15600.00', '续签租金收入-公园旁舒适一居-年付', '2025-01-10 11:00:00');
INSERT INTO `transaction` VALUES ('23', '11', '4', '2', '16800.00', '续签租金支付-公园旁舒适一居-年付(第二次续约)', '2026-01-05 15:00:00');
INSERT INTO `transaction` VALUES ('24', '11', '3', '1', '16800.00', '续签租金收入-公园旁舒适一居-年付(第二次续约)', '2026-01-05 15:00:00');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(加密存储)',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '角色code(ADMIN/LANDLORD/TENANT)',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `status` tinyint DEFAULT '1' COMMENT '状态(0:禁用,1:正常)',
  `audit_status` tinyint DEFAULT '0' COMMENT '审核状态(0:待审核,1:审核通过,2:审核拒绝)',
  `audit_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审核拒绝原因',
  `verify_status` tinyint DEFAULT '0' COMMENT '实名认证状态(0:未认证,1:认证中,2:已认证,3:认证失败)',
  `verify_reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '实名认证拒绝原因',
  `credit_score` int DEFAULT '100' COMMENT '信用分(仅房东,初始100)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '身份证号码',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户信息表';

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('1', 'admin', '$2a$10$Cc6gX7Jel5UtKreBdrY8SeRiqwqEuccdySNafjQRMtz30KE92sPaS', 'admin@example.com', '13800138000', 'ADMIN', '管理员', '/img/1747460779427.png', '1', '1', null, '2', null, '100', '2025-05-15 10:00:00', '2025-05-15 10:00:00', '男', '320851200504063951');
INSERT INTO `user` VALUES ('2', 'landlord1', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', 'landlord1@example.com', '13800138001', 'LANDLORD', '张房东', '/img/1747463391377.jpg', '1', '1', null, '2', null, '98', '2025-05-15 10:01:00', '2025-05-15 10:01:00', '男', '420851200504063952');
INSERT INTO `user` VALUES ('3', 'landlord2', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', 'landlord2@example.com', '13800138002', 'LANDLORD', '李房东', '/img/1747463396621.jpg', '1', '1', null, '2', null, '100', '2025-05-15 10:02:00', '2025-05-15 10:02:00', '男', '322851200204062251');
INSERT INTO `user` VALUES ('4', 'tenant1', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', 'tenant1@example.com', '13800138003', 'TENANT', '王租客', '/img/1747463366220.jpg', '1', '1', null, '2', null, '100', '2025-05-15 10:03:00', '2025-05-17 14:29:26', '男', '320850200104063551');
INSERT INTO `user` VALUES ('5', 'tenant2', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', 'tenant2@example.com', '13800138004', 'TENANT', '赵租客', '/img/1747463406478.jpg', '1', '1', null, '2', null, '100', '2025-05-15 10:04:00', '2025-05-15 10:04:00', '女', '320851199704063951');
INSERT INTO `user` VALUES ('7', 'test', '$2a$10$85eQCoElUepP2aj4IBU4YePjB9WvfX3C16O1JPJzknuu6K5BFd0ZC', '123456789@qq.com', '13456789987', 'TENANT', 'test', null, '1', '1', null, '0', null, '100', '2025-06-11 09:53:22', '2025-06-11 09:53:22', null, null);
INSERT INTO `user` VALUES ('8', 'user1', '$2a$10$z/7iYO6YsUZRLSNvcYvwsuAeinCJW3SBhtTRti78v7eQrgVYVourm', '115151@qq.com', '13123456789', 'TENANT', 'test', null, '1', '1', null, '0', null, '100', '2025-06-17 11:52:19', '2025-06-17 11:52:19', null, null);
INSERT INTO `user` VALUES ('9', 'test000', '$2a$10$sthL52kleYzziLe6k2EK7OXuVe465pnQXTmG66Ui2Pju/Gkf1qese', '156165156@qq.com', '13123456789', 'TENANT', '111', null, '1', '1', null, '0', null, '100', '2025-08-11 14:11:54', '2025-08-11 14:11:54', null, null);

-- ----------------------------
-- Table structure for violation
-- ----------------------------
DROP TABLE IF EXISTS `violation`;
CREATE TABLE `violation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '违规ID',
  `user_id` bigint NOT NULL COMMENT '违规用户ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '违规类型',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '违规描述',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID(房源/帖子ID)',
  `related_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联类型(house/post)',
  `penalty` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处罚措施',
  `credit_deduction` int DEFAULT '0' COMMENT '扣除信用分',
  `status` tinyint DEFAULT '0' COMMENT '状态(0:待处理,1:已处理,2:已忽略)',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='违规记录表';

-- ----------------------------
-- Records of violation
-- ----------------------------
INSERT INTO `violation` VALUES ('1', '3', '虚假房源信息', '用户发布的房源照片与实际不符，存在欺诈行为', '4', 'house', '扣除信用分10分，隐藏该房源', '10', '1', '1', '2025-09-15 14:30:00', '已核实，照片确实与实际房源不符，已扣除信用分并隐藏房源', '2025-09-10 10:00:00', '2025-09-15 14:30:00');
INSERT INTO `violation` VALUES ('2', '5', '恶意评论', '在论坛发布人身攻击性言论', '3', 'post', '删除帖子，扣除信用分5分', '5', '1', '1', '2025-10-20 16:00:00', '已删除违规帖子，并对用户进行警告', '2025-10-18 09:00:00', '2025-10-20 16:00:00');
INSERT INTO `violation` VALUES ('3', '2', '多次爽约看房', '租客预约看房后多次无故不到，影响房东正常出租', null, 'appointment', '扣除信用分3分，警告处理', '3', '0', null, null, null, '2025-11-20 11:00:00', '2025-11-20 11:00:00');

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `house_id` bigint NOT NULL COMMENT '房源ID',
  `note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '收藏备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_house` (`user_id`, `house_id`) USING BTREE COMMENT '用户对同一房源只能收藏一次',
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_house` (`house_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='房源收藏表';

-- ----------------------------
-- Table structure for user_behavior
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior`;
CREATE TABLE `user_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '行为ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `behavior_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为类型(view:浏览,favorite:收藏,search:搜索,appointment:预约)',
  `target_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标类型(house:房源)',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID(房源ID)',
  `behavior_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '行为数据(JSON格式,存储搜索关键词等)',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户代理',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_behavior_type` (`behavior_type`) USING BTREE,
  KEY `idx_target` (`target_type`, `target_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户行为记录表(用于推荐系统)';

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `related_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联类型(house:房源,order:订单)',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读(0:未读,1:已读)',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `is_deleted_by_sender` tinyint DEFAULT '0' COMMENT '发送者是否删除(0:未删除,1:已删除)',
  `is_deleted_by_receiver` tinyint DEFAULT '0' COMMENT '接收者是否删除(0:未删除,1:已删除)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sender` (`sender_id`) USING BTREE,
  KEY `idx_receiver` (`receiver_id`) USING BTREE,
  KEY `idx_is_read` (`receiver_id`, `is_read`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='站内消息表';

SET FOREIGN_KEY_CHECKS=1;

-- =============================
-- 功能完善：添加地图相关字段和用户偏好字段
-- =============================


-- 添加位置索引以支持地图范围搜索
ALTER TABLE `house` ADD INDEX `idx_location` (`latitude`, `longitude`);

-- User表添加用户偏好字段
ALTER TABLE `user`
ADD COLUMN `campus` VARCHAR(50) DEFAULT NULL COMMENT '所属校区' AFTER `id_card`,
ADD COLUMN `major` VARCHAR(100) DEFAULT NULL COMMENT '专业' AFTER `campus`,
ADD COLUMN `enrollment_year` INT DEFAULT NULL COMMENT '入学年份' AFTER `major`,
ADD COLUMN `rent_budget` DECIMAL(10,2) DEFAULT NULL COMMENT '租房预算(月)' AFTER `enrollment_year`;

-- 添加校区索引以支持按校区推荐
ALTER TABLE `user` ADD INDEX `idx_campus` (`campus`);
