package org.example.springboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.UserBehaviorMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐服务
 * 实现 User-Based CF 和 Item-Based CF 算法
 */
@Service
public class CollaborativeFilteringService {

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private UserMapper userMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 校区坐标配置（与AmapService保持一致）
    private static final Map<String, double[]> CAMPUS_COORDINATES = Map.of(
        "大学城校区", new double[]{113.4000, 23.0500},
        "南校区", new double[]{113.3800, 23.0400},
        "北校区", new double[]{113.4200, 23.0600},
        "东校区", new double[]{113.4300, 23.0450}
    );

    /**
     * ==================== Item-Based CF 实现 ====================
     * 基于物品的协同过滤：找出与用户喜欢的房源相似的其他房源
     */

    /**
     * 使用 Item-Based CF 获取个性化推荐
     * 算法思路：
     * 1. 获取用户浏览/收藏/预约过的房源
     * 2. 计算这些房源与其他房源的相似度（使用余弦相似度）
     * 3. 推荐相似度高且用户未交互过的房源
     */
    public List<House> getItemBasedCFRecommendations(Long userId, int limit) {
        // 处理未登录用户
        if (userId == null) {
            return getColdStartRecommendations(limit);
        }

        // 1. 获取用户交互过的房源及其权重
        Map<Long, Double> userItemWeights = getUserItemWeights(userId);

        if (userItemWeights.isEmpty()) {
            // 新用户或无行为数据，返回热门房源
            return fetchHotHouses(limit);
        }

        // 2. 计算候选房源的推荐分数
        Map<Long, Double> candidateScores = new HashMap<>();
        Set<Long> interactedItemIds = userItemWeights.keySet();

        // 对用户交互过的每个房源，找到相似的房源
        for (Map.Entry<Long, Double> entry : userItemWeights.entrySet()) {
            Long itemId = entry.getKey();
            Double userWeight = entry.getValue();

            // 获取与该房源相似的其他房源（排除已交互的）
            List<Map.Entry<Long, Double>> similarItems = getSimilarItemsByCosine(
                itemId, interactedItemIds, 20
            );

            // 累加推荐分数：相似度 × 用户对该房源的权重
            for (Map.Entry<Long, Double> similarItem : similarItems) {
                Long candidateId = similarItem.getKey();
                Double similarity = similarItem.getValue();
                candidateScores.merge(candidateId, similarity * userWeight, Double::sum);
            }
        }

        // 3. 按推荐分数排序，返回 top N
        List<Long> recommendedIds = candidateScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 4. 获取房源详情
        List<House> result = getHousesByIds(recommendedIds);

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = fetchHotHouses(limit - result.size());
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());
            for (House hotHouse : hotHouses) {
                if (!existingIds.contains(hotHouse.getId())) {
                    result.add(hotHouse);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 使用余弦相似度计算与指定房源相似的房源
     * 算法思路：
     * 1. 构建房源特征向量（价格、面积、户型等）
     * 2. 计算两个向量之间的余弦相似度
     * 3. 返回相似度最高的房源
     */
    private List<Map.Entry<Long, Double>> getSimilarItemsByCosine(
            Long targetHouseId, Set<Long> excludeIds, int limit) {

        House targetHouse = houseMapper.selectById(targetHouseId);
        if (targetHouse == null) {
            return Collections.emptyList();
        }

        // 构建目标房源的特征向量
        double[] targetVector = buildHouseFeatureVector(targetHouse);

        // 获取所有可用房源
        List<House> allHouses = houseMapper.selectList(null);

        // 计算每个房源与目标房源的余弦相似度
        Map<Long, Double> similarityMap = new HashMap<>();

        for (House house : allHouses) {
            if (house.getId().equals(targetHouseId)) {
                continue; // 跳过自己
            }
            if (excludeIds.contains(house.getId())) {
                continue; // 跳过已交互的房源
            }
            if (house.getStatus() != 1 || house.getAuditStatus() != 1) {
                continue; // 跳过不可用/未审核的房源
            }

            // 计算余弦相似度
            double[] candidateVector = buildHouseFeatureVector(house);
            double similarity = cosineSimilarity(targetVector, candidateVector);

            // 只有相似度大于阈值的才考虑
            if (similarity > 0.3) {
                similarityMap.put(house.getId(), similarity);
            }
        }

        // 返回相似度最高的房源
        return similarityMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * ==================== User-Based CF 实现 ====================
     * 基于用户的协同过滤：找出相似用户，推荐他们喜欢的房源
     */

    /**
     * 使用 User-Based CF 获取个性化推荐
     * 算法思路：
     * 1. 计算当前用户与其他用户的相似度（基于共同浏览/收藏的房源）
     * 2. 找出最相似的 K 个用户
     * 3. 推荐这些相似用户喜欢但当前用户未交互过的房源
     */
    public List<House> getUserBasedCFRecommendations(Long userId, int limit) {
        // 处理未登录用户
        if (userId == null) {
            return getColdStartRecommendations(limit);
        }

        // 1. 获取当前用户交互过的房源
        Set<Long> userInteractedItems = getUserInteractedItems(userId);

        if (userInteractedItems.isEmpty()) {
            return fetchHotHouses(limit);
        }

        // 2. 计算用户相似度
        Map<Long, Double> userSimilarities = calculateUserSimilarities(userId, userInteractedItems);

        if (userSimilarities.isEmpty()) {
            return fetchHotHouses(limit);
        }

        // 3. 获取相似用户喜欢的房源
        Map<Long, Double> candidateScores = new HashMap<>();

        // 只考虑相似度 > 0 的用户
        for (Map.Entry<Long, Double> similarityEntry : userSimilarities.entrySet()) {
            Long similarUserId = similarityEntry.getKey();
            Double similarity = similarityEntry.getValue();

            if (similarity <= 0.1) {
                continue; // 相似度太低，跳过
            }

            // 获取该相似用户交互过的房源
            Map<Long, Double> similarUserItems = getUserItemWeights(similarUserId);

            for (Map.Entry<Long, Double> itemEntry : similarUserItems.entrySet()) {
                Long itemId = itemEntry.getKey();
                Double itemWeight = itemEntry.getValue();

                // 跳过当前用户已交互过的房源
                if (userInteractedItems.contains(itemId)) {
                    continue;
                }

                // 推荐分数 = 用户相似度 × 相似用户对该房源的权重
                candidateScores.merge(itemId, similarity * itemWeight, Double::sum);
            }
        }

        // 4. 按分数排序返回
        List<Long> recommendedIds = candidateScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<House> result = getHousesByIds(recommendedIds);

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = fetchHotHouses(limit - result.size());
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());
            for (House hotHouse : hotHouses) {
                if (!existingIds.contains(hotHouse.getId())) {
                    result.add(hotHouse);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 计算用户之间的相似度（基于余弦相似度）
     */
    private Map<Long, Double> calculateUserSimilarities(Long userId, Set<Long> userInteractedItems) {
        Map<Long, Double> similarities = new HashMap<>();

        // 获取所有有交互行为的用户及其房源权重
        Map<Long, Map<Long, Double>> allUserItems = getAllUserItemWeights();

        // 构建当前用户的向量（key为房源ID，value为权重）
        Map<Long, Double> currentUserVector = allUserItems.getOrDefault(userId, new HashMap<>());

        if (currentUserVector.isEmpty()) {
            return similarities;
        }

        // 计算与其他用户的相似度
        for (Map.Entry<Long, Map<Long, Double>> entry : allUserItems.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(userId)) {
                continue; // 跳过自己
            }

            Map<Long, Double> otherUserVector = entry.getValue();

            // 计算余弦相似度
            Double similarity = calculateUserCosineSimilarity(currentUserVector, otherUserVector);
            if (similarity != null && similarity > 0) {
                similarities.put(otherUserId, similarity);
            }
        }

        return similarities;
    }

    /**
     * ==================== 混合推荐 ====================
     * 结合 User-Based CF 和 Item-Based CF 的结果
     */

    /**
     * 混合推荐：结合 User-CF 和 Item-CF
     * 权重：Item-CF 70%, User-CF 30%
     */
    public List<House> getHybridRecommendations(Long userId, int limit) {
        // 处理未登录用户
        if (userId == null) {
            return getColdStartRecommendations(limit);
        }

        // 获取 Item-CF 推荐
        List<House> itemCFRecs = getItemBasedCFRecommendations(userId, limit);

        // 获取 User-CF 推荐
        List<House> userCFRecs = getUserBasedCFRecommendations(userId, limit);

        // 合并结果，计算综合分数
        Map<Long, Double> hybridScores = new HashMap<>();

        // Item-CF 权重 0.7
        for (int i = 0; i < itemCFRecs.size(); i++) {
            House house = itemCFRecs.get(i);
            double score = (itemCFRecs.size() - i) * 0.7; // 排名越前分数越高
            hybridScores.merge(house.getId(), score, Double::sum);
        }

        // User-CF 权重 0.3
        for (int i = 0; i < userCFRecs.size(); i++) {
            House house = userCFRecs.get(i);
            double score = (userCFRecs.size() - i) * 0.3;
            hybridScores.merge(house.getId(), score, Double::sum);
        }

        // 按综合分数排序
        List<Long> recommendedIds = hybridScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<House> result = getHousesByIds(recommendedIds);

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = fetchHotHouses(limit - result.size());
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());
            for (House hotHouse : hotHouses) {
                if (!existingIds.contains(hotHouse.getId())) {
                    result.add(hotHouse);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * ==================== 内容推荐（基于用户偏好特征）====================
     */

    /**
     * 基于用户浏览历史的偏好分析，推荐相似房源
     * 分析用户喜欢的房源特征，找出符合这些特征的其他房源
     */
    public List<House> getContentBasedRecommendations(Long userId, int limit) {
        // 处理未登录用户
        if (userId == null) {
            return getColdStartRecommendations(limit);
        }

        // 获取用户交互过的房源及权重
        Map<Long, Double> userItemWeights = getUserItemWeights(userId);

        if (userItemWeights.isEmpty()) {
            return fetchHotHouses(limit);
        }

        // 计算用户的平均偏好特征向量
        double[] userPreferenceVector = buildUserPreferenceVector(userItemWeights);

        // 获取所有可用房源，计算与用户偏好的相似度
        List<House> allHouses = houseMapper.selectList(null);
        Map<Long, Double> candidateScores = new HashMap<>();

        for (House house : allHouses) {
            if (userItemWeights.containsKey(house.getId())) {
                continue; // 跳过已交互的房源
            }
            if (house.getStatus() != 1 || house.getAuditStatus() != 1) {
                continue;
            }

            // 计算房源特征与用户偏好的匹配度
            double[] houseVector = buildHouseFeatureVector(house);
            double matchScore = cosineSimilarity(userPreferenceVector, houseVector);

            if (matchScore > 0.6) { // 只保留匹配度 > 0.6 的房源
                candidateScores.put(house.getId(), matchScore);
            }
        }

        // 按匹配度排序
        List<Long> recommendedIds = candidateScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<House> result = getHousesByIds(recommendedIds);

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = fetchHotHouses(limit - result.size());
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());
            for (House hotHouse : hotHouses) {
                if (!existingIds.contains(hotHouse.getId())) {
                    result.add(hotHouse);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 构建用户偏好特征向量
     * 基于用户交互过的房源特征，加权平均得到用户偏好
     */
    private double[] buildUserPreferenceVector(Map<Long, Double> userItemWeights) {
        double[] preferenceVector = new double[12]; // 12个特征维度
        double totalWeight = 0.0;

        for (Map.Entry<Long, Double> entry : userItemWeights.entrySet()) {
            Long houseId = entry.getKey();
            Double weight = entry.getValue();

            House house = houseMapper.selectById(houseId);
            if (house == null) continue;

            double[] houseVector = buildHouseFeatureVector(house);

            // 累加加权特征
            for (int i = 0; i < houseVector.length; i++) {
                preferenceVector[i] += houseVector[i] * weight;
            }
            totalWeight += weight;
        }

        // 计算加权平均
        if (totalWeight > 0) {
            for (int i = 0; i < preferenceVector.length; i++) {
                preferenceVector[i] /= totalWeight;
            }
        }

        return preferenceVector;
    }

    /**
     * ==================== 相似房源推荐（基于余弦相似度）====================
     */

    /**
     * 获取与指定房源相似的房源（使用余弦相似度）
     */
    public List<House> getSimilarHousesByCosine(Long houseId, int limit) {
        House targetHouse = houseMapper.selectById(houseId);
        if (targetHouse == null) {
            return Collections.emptyList();
        }

        // 构建目标房源的特征向量
        double[] targetVector = buildHouseFeatureVector(targetHouse);

        // 获取所有可用房源
        List<House> allHouses = houseMapper.selectList(null);

        // 计算相似度并排序
        Map<Long, Double> similarityMap = new HashMap<>();

        for (House house : allHouses) {
            if (house.getId().equals(houseId)) {
                continue;
            }
            if (house.getStatus() != 1 || house.getAuditStatus() != 1) {
                continue;
            }

            double[] candidateVector = buildHouseFeatureVector(house);
            double similarity = cosineSimilarity(targetVector, candidateVector);

            if (similarity > 0.5) { // 只保留相似度 > 0.5 的房源
                similarityMap.put(house.getId(), similarity);
            }
        }

        // 按相似度排序
        List<Long> similarIds = similarityMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return getHousesByIds(similarIds);
    }

    /**
     * 获取冷启动推荐（新用户策略）
     * 结合热门房源和基于内容的推荐
     */
    public List<House> getColdStartRecommendations(int limit) {
        try {
            // 50% 热门房源 + 50% 高质量推荐房源
            int hotLimit = limit / 2;
            int qualityLimit = limit - hotLimit;

            List<House> result = new ArrayList<>();

            // 获取热门房源
            List<House> hotHouses = fetchHotHouses(hotLimit);
            if (hotHouses != null) {
                result.addAll(hotHouses);
            }

            // 获取高质量推荐房源（基于新字段特征）
            List<House> qualityHouses = fetchQualityHouses(qualityLimit);
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());

            if (qualityHouses != null) {
                for (House house : qualityHouses) {
                    if (!existingIds.contains(house.getId())) {
                        result.add(house);
                        if (result.size() >= limit) {
                            break;
                        }
                    }
                }
            }

            // 如果仍然不足，用普通房源补充
            if (result.size() < limit) {
                List<House> backupHouses = fetchBackupHouses(limit - result.size());
                if (backupHouses != null) {
                    Set<Long> currentIds = result.stream()
                        .map(House::getId)
                        .collect(Collectors.toSet());
                    for (House house : backupHouses) {
                        if (!currentIds.contains(house.getId())) {
                            result.add(house);
                            if (result.size() >= limit) {
                                break;
                            }
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            // 发生错误时返回基本的可用房源列表
            try {
                return houseMapper.selectList(null).stream()
                    .filter(h -> h.getStatus() == 1 && h.getAuditStatus() == 1)
                    .limit(limit)
                    .collect(Collectors.toList());
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }
    }

    /**
     * 获取高质量房源（基于新字段特征）
     * 筛选条件：精装/豪装、评分高、推荐标识、建筑年代新
     */
    private List<House> fetchQualityHouses(int limit) {
        List<House> allHouses = houseMapper.selectList(null);

        // 计算每个房源的综合质量分数
        Map<Long, Double> qualityScores = new HashMap<>();

        for (House house : allHouses) {
            if (house.getStatus() != 1 || house.getAuditStatus() != 1) {
                continue;
            }

            double score = 0.0;

            // 装修情况加分 (30%)
            if (house.getDecoration() != null) {
                score += house.getDecoration() * 0.3; // 豪装=3*0.3=0.9
            }

            // 评分加分 (25%)
            if (house.getAvgScore() != null) {
                score += (house.getAvgScore().doubleValue() / 5.0) * 0.25;
            }

            // 推荐标识加分 (20%)
            if (house.getIsRecommended() != null && house.getIsRecommended() == 1) {
                score += 0.2;
            }

            // 建筑年代加分 (15%)
            if (house.getBuildYear() != null) {
                int age = java.time.Year.now().getValue() - house.getBuildYear();
                score += Math.max(0, (1.0 - age / 50.0) * 0.15);
            }

            // 朝向加分 (10%)
            if (house.getOrientation() != null) {
                score += getOrientationPreference(house.getOrientation()) * 0.1;
            }

            qualityScores.put(house.getId(), score);
        }

        // 按质量分数排序
        List<Long> topIds = qualityScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit * 2) // 多取一些，后续过滤
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return getHousesByIds(topIds).stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取备用房源（当其他推荐不足时使用）
     */
    private List<House> fetchBackupHouses(int limit) {
        return houseMapper.selectList(null).stream()
            .filter(h -> h.getStatus() == 1 && h.getAuditStatus() == 1)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * ==================== 辅助方法 ====================
     */

    /**
     * 构建房源特征向量
     * 向量维度（基于新增的房源详细字段）：
     * [0] 价格（标准化）
     * [1] 面积（标准化）
     * [2] 户型类型（1=整租, 2=合租）
     * [3] 装修情况（0=毛坯, 1=简装, 2=精装, 3=豪装）
     * [4] 朝向（0=东, 1=南, 2=西, 3=北, 4=东南, 5=西南, 6=东北, 7=西北, 8=南北通透）
     * [5] 房屋类型（0=普通住宅, 1=公寓, 2=别墅, 3=loft, 4=复式）
     * [6] 付款方式（0=押一付三, 1=押一付一, 2=半年付, 3=年付）
     * [7] 楼层（标准化）
     * [8] 评分（标准化）
     * [9] 浏览量（标准化）
     * [10] 是否推荐（0=否, 1=是）
     * [11] 建筑年代（标准化）
     */
    private double[] buildHouseFeatureVector(House house) {
        // 使用全局最大最小值进行标准化
        final double MAX_PRICE = 20000.0;  // 最大价格
        final double MIN_PRICE = 500.0;    // 最小价格
        final double MAX_AREA = 300.0;     // 最大面积
        final double MIN_AREA = 10.0;      // 最小面积
        final double MAX_SCORE = 5.0;      // 最大评分
        final double MAX_VIEW_COUNT = 10000.0; // 最大浏览量
        final double MAX_FLOOR = 50;       // 最大楼层
        final int CURRENT_YEAR = java.time.Year.now().getValue();
        final int MIN_BUILD_YEAR = CURRENT_YEAR - 50; // 最早建筑年代

        // 扩展特征向量到12个维度
        double[] vector = new double[12];

        // [0] 价格标准化
        double price = house.getPrice() != null ? house.getPrice().doubleValue() : 0;
        vector[0] = normalize(price, MIN_PRICE, MAX_PRICE);

        // [1] 面积标准化
        double area = house.getArea() != null ? house.getArea().doubleValue() : 0;
        vector[1] = normalize(area, MIN_AREA, MAX_AREA);

        // [2] 户型类型（标准化）
        int typeId = house.getTypeId() != null ? house.getTypeId().intValue() : 1;
        vector[2] = typeId == 1 ? 1.0 : 0.5; // 整租=1, 合租=0.5

        // [3] 装修情况（使用新字段）
        Integer decoration = house.getDecoration();
        if (decoration != null) {
            vector[3] = decoration / 3.0; // 标准化到 0-1 (0=毛坯, 1=简装, 2=精装, 3=豪装)
        } else {
            // 兼容旧数据，从设施推断
            vector[3] = getDecorationLevel(house) / 3.0;
        }

        // [4] 朝向（使用新字段）
        Integer orientation = house.getOrientation();
        if (orientation != null) {
            // 将朝向转换为偏好值：南=1.0, 南北通透=0.95, 东南/西南=0.8, 东=0.6, 西=0.4, 北=0.3, 东北/西北=0.5
            vector[4] = getOrientationPreference(orientation);
        } else {
            vector[4] = 0.5; // 默认值
        }

        // [5] 房屋类型（使用新字段）
        Integer buildingType = house.getBuildingType();
        if (buildingType != null) {
            vector[5] = buildingType / 4.0; // 标准化到 0-1
        } else {
            vector[5] = 0.0; // 默认普通住宅
        }

        // [6] 付款方式（使用新字段）
        Integer paymentMethod = house.getPaymentMethod();
        if (paymentMethod != null) {
            vector[6] = paymentMethod / 3.0; // 标准化到 0-1
        } else {
            vector[6] = 0.0; // 默认押一付三
        }

        // [7] 楼层（使用新字段）
        Integer floor = house.getFloor();
        Integer totalFloors = house.getTotalFloors();
        if (floor != null && totalFloors != null && totalFloors > 0) {
            // 使用楼层比例（低层偏好更高）
            double floorRatio = (double) floor / totalFloors;
            // 中间楼层（3-7层）偏好最高
            if (floor >= 3 && floor <= 7) {
                vector[7] = 1.0;
            } else if (floor < 3) {
                vector[7] = 0.6 + floorRatio * 0.2;
            } else {
                vector[7] = Math.max(0.4, 1.0 - (floor - 7) * 0.1);
            }
        } else {
            vector[7] = 0.5; // 默认值
        }

        // [8] 评分标准化
        double avgScore = house.getAvgScore() != null ? house.getAvgScore().doubleValue() : 0;
        vector[8] = avgScore / MAX_SCORE;

        // [9] 浏览量标准化
        Integer viewCountInt = house.getViewCount();
        if (viewCountInt != null) {
            vector[9] = Math.min(viewCountInt / MAX_VIEW_COUNT, 1.0);
        } else {
            vector[9] = 0.0;
        }

        // [10] 是否推荐（使用新字段）
        Integer isRecommended = house.getIsRecommended();
        vector[10] = (isRecommended != null && isRecommended == 1) ? 1.0 : 0.0;

        // [11] 建筑年代（越新越好）
        Integer buildYear = house.getBuildYear();
        if (buildYear != null) {
            // 标准化：新房子=1, 50年老房=0
            int age = CURRENT_YEAR - buildYear;
            vector[11] = Math.max(0, 1.0 - (double) age / 50.0);
        } else {
            vector[11] = 0.5; // 默认值
        }

        return vector;
    }

    /**
     * 将朝向编码转换为偏好值
     * 南向和南北通透最受欢迎
     */
    private double getOrientationPreference(Integer orientation) {
        return switch (orientation) {
            case 1 -> 1.0;      // 南
            case 8 -> 0.95;     // 南北通透
            case 4 -> 0.8;      // 东南
            case 5 -> 0.8;      // 西南
            case 0 -> 0.6;      // 东
            case 6 -> 0.5;      // 东北
            case 7 -> 0.5;      // 西北
            case 2 -> 0.4;      // 西
            case 3 -> 0.3;      // 北
            default -> 0.5;
        };
    }

    /**
     * 计算两个向量的余弦相似度
     * cos(θ) = (A · B) / (||A|| × ||B||)
     */
    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Min-Max 标准化
     */
    private double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.5;
        }
        return (value - min) / (max - min);
    }

    /**
     * 获取用户对房源的权重
     * 浏览=1, 收藏=3, 预约=5
     */
    private Map<Long, Double> getUserItemWeights(Long userId) {
        Map<Long, Double> weights = new HashMap<>();

        try {
            // 获取浏览记录（计数）
            List<Map<String, Object>> viewedHouses = userBehaviorMapper.getUserViewedHouses(userId);
            if (viewedHouses != null) {
                for (Map<String, Object> record : viewedHouses) {
                    try {
                        Object houseIdObj = record.get("houseId");
                        Object viewCountObj = record.get("viewCount");
                        if (houseIdObj != null && viewCountObj != null) {
                            Long houseId = ((Number) houseIdObj).longValue();
                            Long viewCount = ((Number) viewCountObj).longValue();
                            weights.merge(houseId, viewCount * 1.0, Double::sum);
                        }
                    } catch (Exception e) {
                        // Skip invalid records
                    }
                }
            }
        } catch (Exception e) {
            // Continue if query fails
        }

        try {
            // 获取收藏记录
            List<Long> favoriteIds = userBehaviorMapper.getUserFavoriteHouseIds(userId);
            if (favoriteIds != null) {
                for (Long houseId : favoriteIds) {
                    if (houseId != null) {
                        weights.merge(houseId, 3.0, Double::sum);
                    }
                }
            }
        } catch (Exception e) {
            // Continue if query fails
        }

        try {
            // 获取预约记录
            List<Long> appointmentIds = userBehaviorMapper.getUserAppointmentHouseIds(userId);
            if (appointmentIds != null) {
                for (Long houseId : appointmentIds) {
                    if (houseId != null) {
                        weights.merge(houseId, 5.0, Double::sum);
                    }
                }
            }
        } catch (Exception e) {
            // Continue if query fails
        }

        return weights;
    }

    /**
     * 获取所有用户的房源权重
     */
    private Map<Long, Map<Long, Double>> getAllUserItemWeights() {
        // 这里简化实现，只返回当前用户和有相似行为的用户
        // 实际生产环境应该使用缓存或预计算
        Map<Long, Map<Long, Double>> allWeights = new HashMap<>();

        try {
            // 获取最近有行为的用户
            List<Long> activeUserIds = userBehaviorMapper.getActiveUserIds(30);
            if (activeUserIds != null) {
                for (Long uid : activeUserIds) {
                    if (uid != null) {
                        allWeights.put(uid, getUserItemWeights(uid));
                    }
                }
            }
        } catch (Exception e) {
            // Return empty map if query fails
        }

        return allWeights;
    }

    /**
     * 计算用户之间的余弦相似度
     */
    private Double calculateUserCosineSimilarity(Map<Long, Double> userA, Map<Long, Double> userB) {
        // 找出共同交互的房源
        Set<Long> commonItems = new HashSet<>(userA.keySet());
        commonItems.retainAll(userB.keySet());

        if (commonItems.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Long itemId : commonItems) {
            double weightA = userA.getOrDefault(itemId, 0.0);
            double weightB = userB.getOrDefault(itemId, 0.0);
            dotProduct += weightA * weightB;
        }

        for (Double weight : userA.values()) {
            normA += weight * weight;
        }

        for (Double weight : userB.values()) {
            normB += weight * weight;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 获取用户交互过的房源ID集合
     */
    private Set<Long> getUserInteractedItems(Long userId) {
        Set<Long> interactedItems = new HashSet<>();

        // 浏览记录
        List<Map<String, Object>> viewedHouses = userBehaviorMapper.getUserViewedHouses(userId);
        for (Map<String, Object> record : viewedHouses) {
            interactedItems.add(((Number) record.get("houseId")).longValue());
        }

        // 收藏记录
        interactedItems.addAll(userBehaviorMapper.getUserFavoriteHouseIds(userId));

        // 预约记录
        interactedItems.addAll(userBehaviorMapper.getUserAppointmentHouseIds(userId));

        return interactedItems;
    }

    /**
     * 根据ID列表获取房源
     */
    private List<House> getHousesByIds(List<Long> houseIds) {
        List<House> houses = new ArrayList<>();
        for (Long id : houseIds) {
            House house = houseMapper.selectById(id);
            if (house != null && house.getStatus() == 1 && house.getAuditStatus() == 1) {
                houses.add(house);
            }
        }
        return houses;
    }

    /**
     * 获取热门房源
     */
    private List<House> fetchHotHouses(int limit) {
        try {
            List<Long> hotHouseIds = userBehaviorMapper.getHotHouses(30, limit * 2);
            List<House> houses = new ArrayList<>();
            if (hotHouseIds != null) {
                for (Long houseId : hotHouseIds) {
                    if (houses.size() >= limit) break;
                    if (houseId == null) continue;
                    House house = houseMapper.selectById(houseId);
                    if (house != null && house.getStatus() == 1 && house.getAuditStatus() == 1) {
                        houses.add(house);
                    }
                }
            }
            return houses;
        } catch (Exception e) {
            // Return empty list if query fails
            return new ArrayList<>();
        }
    }

    /**
     * 根据ID获取房源
     */
    public House getHouseById(Long houseId) {
        return houseMapper.selectById(houseId);
    }

    /**
     * 获取装修等级
     */
    private int getDecorationLevel(House house) {
        // 可以从设施或其他字段推断，这里简化处理
        return 2; // 默认精装
    }

    /**
     * ==================== 基于用户偏好的推荐 ====================
     * 结合用户设置的校区、预算等偏好信息进行推荐
     */

    /**
     * 基于用户偏好获取个性化推荐
     * 主要考虑：校区距离、预算匹配、房源质量
     */
    public List<House> getPreferenceBasedRecommendations(Long userId, int limit) {
        // 处理未登录用户
        if (userId == null) {
            return getColdStartRecommendations(limit);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return fetchHotHouses(limit);
        }

        // 获取所有可用房源
        List<House> allHouses = houseMapper.selectList(null);
        Map<Long, Double> preferenceScores = new HashMap<>();

        for (House house : allHouses) {
            if (house.getStatus() != 1 || house.getAuditStatus() != 1) {
                continue;
            }

            double score = calculatePreferenceScore(user, house);
            if (score > 0) {
                preferenceScores.put(house.getId(), score);
            }
        }

        // 按分数排序返回
        List<Long> recommendedIds = preferenceScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<House> result = getHousesByIds(recommendedIds);

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = fetchHotHouses(limit - result.size());
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());
            for (House hotHouse : hotHouses) {
                if (!existingIds.contains(hotHouse.getId())) {
                    result.add(hotHouse);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 计算房源与用户偏好的匹配分数
     * 总分 1.0 = 预算匹配(0.3) + 位置匹配(0.4) + 房源质量(0.3)
     */
    private double calculatePreferenceScore(User user, House house) {
        double score = 0.0;

        // 1. 预算匹配度 (30%)
        if (user.getRentBudget() != null && house.getPrice() != null) {
            double budgetRatio = house.getPrice().doubleValue() / user.getRentBudget().doubleValue();
            if (budgetRatio <= 0.8) {
                score += 0.30; // 房租远低于预算，最推荐
            } else if (budgetRatio <= 1.0) {
                score += 0.25; // 房租在预算内
            } else if (budgetRatio <= 1.1) {
                score += 0.15; // 稍微超预算
            }
            // 超过预算10%不推荐
        } else {
            // 用户未设置预算，给予中等分数
            score += 0.10;
        }

        // 2. 位置匹配度 (40%) - 基于校区距离
        if (user.getCampus() != null && house.getLatitude() != null && house.getLongitude() != null) {
            double distance = getDistanceFromCampus(user.getCampus(),
                house.getLongitude(), house.getLatitude());

            if (distance <= 2.0) {
                score += 0.40; // 2公里内，最推荐
            } else if (distance <= 5.0) {
                score += 0.30; // 5公里内
            } else if (distance <= 10.0) {
                score += 0.20; // 10公里内
            } else if (distance <= 15.0) {
                score += 0.10; // 15公里内
            }
            // 超过15公里不加分
        } else {
            // 用户未设置校区或房源无坐标，给予较低分数
            score += 0.05;
        }

        // 3. 房源质量 (30%)
        // 评分 (20%)
        if (house.getAvgScore() != null) {
            score += (house.getAvgScore().doubleValue() / 5.0) * 0.20;
        } else {
            score += 0.10; // 无评分给予默认分
        }

        // 装修情况 (10%)
        if (house.getDecoration() != null) {
            score += (house.getDecoration() / 3.0) * 0.10;
        } else {
            score += 0.05;
        }

        return score;
    }

    /**
     * 计算房源到校区的距离（使用Haversine公式）
     */
    private double getDistanceFromCampus(String campus, BigDecimal houseLng, BigDecimal houseLat) {
        double[] campusCoords = CAMPUS_COORDINATES.get(campus);
        if (campusCoords == null) {
            return Double.MAX_VALUE;
        }

        double campusLng = campusCoords[0];
        double campusLat = campusCoords[1];

        return calculateDistanceByHaversine(houseLng.doubleValue(), houseLat.doubleValue(),
            campusLng, campusLat);
    }

    /**
     * 使用Haversine公式计算两点间距离（公里）
     */
    private double calculateDistanceByHaversine(double lng1, double lat1, double lng2, double lat2) {
        final int EARTH_RADIUS = 6371; // 地球半径，单位：公里

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double lng1Rad = Math.toRadians(lng1);
        double lng2Rad = Math.toRadians(lng2);

        double dLat = lat2Rad - lat1Rad;
        double dLng = lng2Rad - lng1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * ==================== 高级推荐算法优化 ====================
     * 包含：时间衰减、皮尔逊相关系数、智能混合推荐、多维度评分
     */

    /**
     * 计算时间衰减因子
     * 最近的行为权重更高，使用指数衰减
     * @param behaviorTime 行为发生时间
     * @param halfLife 半衰期（天数）
     * @return 衰减因子 0-1
     */
    private double calculateTimeDecay(LocalDateTime behaviorTime, int halfLife) {
        if (behaviorTime == null) {
            return 0.5; // 默认中等权重
        }
        long daysBetween = ChronoUnit.DAYS.between(behaviorTime, LocalDateTime.now());
        return Math.pow(0.5, (double) daysBetween / halfLife);
    }

    /**
     * 计算皮尔逊相关系数
     * 适用于处理用户评分偏差的情况，比余弦相似度更准确
     * @param userA 用户A的房源评分向量
     * @param userB 用户B的房源评分向量
     * @return 皮尔逊相关系数 (-1 到 1)
     */
    private Double calculatePearsonCorrelation(Map<Long, Double> userA, Map<Long, Double> userB) {
        // 找出共同交互的房源
        Set<Long> commonItems = new HashSet<>(userA.keySet());
        commonItems.retainAll(userB.keySet());

        if (commonItems.size() < 2) {
            return 0.0; // 至少需要2个共同项才能计算
        }

        int n = commonItems.size();

        // 计算均值
        double sumA = 0.0, sumB = 0.0;
        for (Long itemId : commonItems) {
            sumA += userA.get(itemId);
            sumB += userB.get(itemId);
        }
        double meanA = sumA / n;
        double meanB = sumB / n;

        // 计算皮尔逊相关系数
        double numerator = 0.0;
        double denominatorA = 0.0;
        double denominatorB = 0.0;

        for (Long itemId : commonItems) {
            double diffA = userA.get(itemId) - meanA;
            double diffB = userB.get(itemId) - meanB;
            numerator += diffA * diffB;
            denominatorA += diffA * diffA;
            denominatorB += diffB * diffB;
        }

        if (denominatorA == 0.0 || denominatorB == 0.0) {
            return 0.0;
        }

        return numerator / Math.sqrt(denominatorA * denominatorB);
    }

    /**
     * 智能混合推荐 - 动态权重分配
     * 根据用户行为数据量和房源特征动态调整权重
     * 算法策略：
     * 1. 新用户(行为<5): 80% 热门 + 20% 内容推荐
     * 2. 少量行为(5-20): 50% 内容 + 30% Item-CF + 20% 热门
     * 3. 中等行为(20-50): 35% 内容 + 40% Item-CF + 25% User-CF
     * 4. 大量行为(>50): 25% 内容 + 45% Item-CF + 30% User-CF
     *
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐房源列表
     */
    @Cacheable(value = "recommendations", key = "#userId + '_smart_' + #limit")
    public List<House> getSmartHybridRecommendations(Long userId, int limit) {
        // 处理未登录用户，返回冷启动推荐
        if (userId == null) {
            return getColdStartRecommendations(limit);
        }

        // 获取用户行为统计
        Map<Long, Double> userItemWeights = getUserItemWeights(userId);
        int behaviorCount = userItemWeights.size();

        // 根据行为量动态分配权重
        double contentWeight, itemCFWeight, userCFWeight, hotWeight;

        if (behaviorCount < 5) {
            // 新用户策略
            contentWeight = 0.2;
            itemCFWeight = 0.0;
            userCFWeight = 0.0;
            hotWeight = 0.8;
        } else if (behaviorCount < 20) {
            // 少量行为策略
            contentWeight = 0.5;
            itemCFWeight = 0.3;
            userCFWeight = 0.0;
            hotWeight = 0.2;
        } else if (behaviorCount < 50) {
            // 中等行为策略
            contentWeight = 0.35;
            itemCFWeight = 0.40;
            userCFWeight = 0.25;
            hotWeight = 0.0;
        } else {
            // 大量行为策略
            contentWeight = 0.25;
            itemCFWeight = 0.45;
            userCFWeight = 0.30;
            hotWeight = 0.0;
        }

        // 获取各类型推荐
        Map<Long, Double> hybridScores = new HashMap<>();
        Set<Long> interactedItems = userItemWeights.keySet();

        // 1. 基于内容的推荐（考虑用户偏好）
        User user = userMapper.selectById(userId);
        List<House> allHouses = houseMapper.selectList(null);

        for (House house : allHouses) {
            if (house.getStatus() != 1 || house.getAuditStatus() != 1) {
                continue;
            }
            if (interactedItems.contains(house.getId())) {
                continue;
            }

            double contentScore = 0.0;

            // 基于用户偏好评分
            if (user != null) {
                contentScore = calculatePreferenceScore(user, house);
            }

            // 基于用户历史偏好的相似度
            if (behaviorCount >= 5) {
                double[] userPreferenceVector = buildUserPreferenceVector(userItemWeights);
                double[] houseVector = buildHouseFeatureVector(house);
                double similarity = cosineSimilarity(userPreferenceVector, houseVector);
                contentScore = (contentScore + similarity) / 2.0;
            }

            if (contentScore > 0) {
                hybridScores.merge(house.getId(), contentScore * contentWeight, Double::sum);
            }
        }

        // 2. Item-CF推荐
        if (itemCFWeight > 0) {
            List<House> itemCFRecs = getItemBasedCFRecommendations(userId, limit * 2);
            for (int i = 0; i < itemCFRecs.size(); i++) {
                double score = (itemCFRecs.size() - i) / (double) itemCFRecs.size();
                hybridScores.merge(itemCFRecs.get(i).getId(), score * itemCFWeight, Double::sum);
            }
        }

        // 3. User-CF推荐
        if (userCFWeight > 0) {
            List<House> userCFRecs = getUserBasedCFRecommendations(userId, limit * 2);
            for (int i = 0; i < userCFRecs.size(); i++) {
                double score = (userCFRecs.size() - i) / (double) userCFRecs.size();
                hybridScores.merge(userCFRecs.get(i).getId(), score * userCFWeight, Double::sum);
            }
        }

        // 4. 热门房源
        if (hotWeight > 0) {
            List<House> hotHouses = fetchHotHouses(limit * 2);
            for (int i = 0; i < hotHouses.size(); i++) {
                double score = (hotHouses.size() - i) / (double) hotHouses.size();
                hybridScores.merge(hotHouses.get(i).getId(), score * hotWeight, Double::sum);
            }
        }

        // 按综合分数排序
        List<Long> recommendedIds = hybridScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<House> result = getHousesByIds(recommendedIds);

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = fetchHotHouses(limit - result.size());
            Set<Long> existingIds = result.stream()
                .map(House::getId)
                .collect(Collectors.toSet());
            for (House hotHouse : hotHouses) {
                if (!existingIds.contains(hotHouse.getId())) {
                    result.add(hotHouse);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 多维度综合评分
     * 整合位置、价格、用户偏好、时间衰减、流行度等多维度因素
     *
     * @param userId 用户ID
     * @param house 房源
     * @param behaviorTime 行为时间（用于时间衰减）
     * @return 综合评分 0-1
     */
    public double calculateMultiDimensionalScore(Long userId, House house, LocalDateTime behaviorTime) {
        // 处理未登录用户
        if (userId == null) {
            return 0.5; // 返回中等分数
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return 0.0;
        }

        double score = 0.0;

        // 1. 时间衰减因子 (15%)
        double timeDecay = calculateTimeDecay(behaviorTime, 30); // 30天半衰期
        score += timeDecay * 0.15;

        // 2. 位置匹配度 (25%)
        double locationScore = calculateLocationScore(user, house);
        score += locationScore * 0.25;

        // 3. 价格敏感度 (20%)
        double priceScore = calculatePriceScore(user, house);
        score += priceScore * 0.20;

        // 4. 用户偏好匹配 (20%)
        double preferenceScore = calculateContentPreferenceMatch(user, house);
        score += preferenceScore * 0.20;

        // 5. 房源质量 (10%)
        double qualityScore = calculateHouseQualityScore(house);
        score += qualityScore * 0.10;

        // 6. 流行度 (10%)
        double popularityScore = calculatePopularityScore(house);
        score += popularityScore * 0.10;

        return Math.min(score, 1.0);
    }

    /**
     * 计算位置匹配分数
     * 考虑校区距离、交通便利度
     */
    private double calculateLocationScore(User user, House house) {
        if (user.getCampus() == null || house.getLatitude() == null || house.getLongitude() == null) {
            return 0.5; // 无位置信息时给中等分
        }

        double distance = getDistanceFromCampus(user.getCampus(),
            house.getLongitude(), house.getLatitude());

        // 距离越近分数越高
        if (distance <= 1.0) return 1.0;
        if (distance <= 2.0) return 0.9;
        if (distance <= 3.0) return 0.8;
        if (distance <= 5.0) return 0.6;
        if (distance <= 10.0) return 0.4;
        if (distance <= 15.0) return 0.2;
        return 0.1;
    }

    /**
     * 计算价格匹配分数
     * 考虑用户预算与房源价格的匹配度
     */
    private double calculatePriceScore(User user, House house) {
        if (user.getRentBudget() == null || house.getPrice() == null) {
            return 0.5; // 无预算信息时给中等分
        }

        double budget = user.getRentBudget().doubleValue();
        double price = house.getPrice().doubleValue();
        double ratio = price / budget;

        // 价格比例越接近0.7-0.9分数越高（性价比区间）
        if (ratio <= 0.5) return 0.7; // 太便宜可能有问题
        if (ratio <= 0.7) return 0.9; // 性价比高
        if (ratio <= 0.9) return 1.0; // 最佳区间
        if (ratio <= 1.0) return 0.95; // 在预算内
        if (ratio <= 1.1) return 0.7; // 稍微超预算
        if (ratio <= 1.2) return 0.4; // 超预算较多
        return 0.1; // 严重超预算
    }

    /**
     * 计算内容偏好匹配分数
     * 基于用户历史行为偏好分析
     */
    private double calculateContentPreferenceMatch(User user, House house) {
        // 获取用户行为历史构建偏好向量
        Map<Long, Double> userItemWeights = getUserItemWeights(user.getId());
        if (userItemWeights.isEmpty()) {
            return 0.5;
        }

        double[] userPreferenceVector = buildUserPreferenceVector(userItemWeights);
        double[] houseVector = buildHouseFeatureVector(house);
        return cosineSimilarity(userPreferenceVector, houseVector);
    }

    /**
     * 计算房源质量分数
     * 基于评分、装修、建筑年代等
     */
    private double calculateHouseQualityScore(House house) {
        double score = 0.0;

        // 评分 (40%)
        if (house.getAvgScore() != null) {
            score += (house.getAvgScore().doubleValue() / 5.0) * 0.4;
        } else {
            score += 0.2; // 默认分
        }

        // 装修情况 (30%)
        if (house.getDecoration() != null) {
            score += (house.getDecoration() / 3.0) * 0.3;
        } else {
            score += 0.15;
        }

        // 推荐标识 (15%)
        if (house.getIsRecommended() != null && house.getIsRecommended() == 1) {
            score += 0.15;
        }

        // 建筑年代 (15%)
        if (house.getBuildYear() != null) {
            int age = java.time.Year.now().getValue() - house.getBuildYear();
            score += Math.max(0, (1.0 - age / 50.0) * 0.15);
        } else {
            score += 0.075;
        }

        return score;
    }

    /**
     * 计算房源流行度分数
     * 基于浏览量、收藏量、预约量
     */
    private double calculatePopularityScore(House house) {
        double score = 0.0;

        // 浏览量 (40%)
        Integer viewCount = house.getViewCount();
        if (viewCount != null) {
            score += Math.min(viewCount / 1000.0, 1.0) * 0.4;
        }

        // 收藏量 (30%)
        Integer favoriteCount = house.getFavoriteCount();
        if (favoriteCount != null) {
            score += Math.min(favoriteCount / 100.0, 1.0) * 0.3;
        }

        // 评分 (30%)
        if (house.getAvgScore() != null) {
            score += (house.getAvgScore().doubleValue() / 5.0) * 0.3;
        }

        return score;
    }

    /**
     * 生成推荐解释
     * 说明为什么推荐这套房源给用户
     *
     * @param userId 用户ID
     * @param house 房源
     * @return 推荐原因文本
     */
    public String generateRecommendationExplanation(Long userId, House house) {
        // 处理未登录用户
        if (userId == null) {
            return "热门房源推荐";
        }

        User user = userMapper.selectById(userId);
        if (user == null || house == null) {
            return "基于热门房源推荐";
        }

        List<String> reasons = new ArrayList<>();

        // 1. 检查位置匹配
        if (user.getCampus() != null && house.getLatitude() != null && house.getLongitude() != null) {
            double distance = getDistanceFromCampus(user.getCampus(),
                house.getLongitude(), house.getLatitude());
            if (distance <= 2.0) {
                reasons.add("距离您所在的" + user.getCampus() + "仅" + String.format("%.1f", distance) + "公里");
            }
        }

        // 2. 检查价格匹配
        if (user.getRentBudget() != null && house.getPrice() != null) {
            double ratio = house.getPrice().doubleValue() / user.getRentBudget().doubleValue();
            if (ratio <= 0.8) {
                reasons.add("价格低于您预算" + String.format("%.0f", (1 - ratio) * 100) + "%，性价比高");
            } else if (ratio <= 1.0) {
                reasons.add("价格在您的预算范围内");
            }
        }

        // 3. 检查房源质量
        if (house.getAvgScore() != null && house.getAvgScore().compareTo(BigDecimal.valueOf(4.5)) >= 0) {
            reasons.add("用户评分高达" + house.getAvgScore() + "分");
        }
        if (house.getIsRecommended() != null && house.getIsRecommended() == 1) {
            reasons.add("平台精选优质房源");
        }
        if (house.getDecoration() != null && house.getDecoration() >= 2) {
            String[] decorationTypes = {"", "毛坯", "简装", "精装", "豪装"};
            if (house.getDecoration() <= 3) {
                reasons.add(decorationTypes[house.getDecoration()] + "修，居住舒适");
            }
        }

        // 4. 检查流行度
        Integer viewCount = house.getViewCount();
        if (viewCount != null && viewCount > 500) {
            reasons.add("已有" + viewCount + "人浏览，热门房源");
        }

        // 5. 检查用户行为相似度
        Map<Long, Double> userItemWeights = getUserItemWeights(userId);
        if (!userItemWeights.isEmpty()) {
            double[] userPreferenceVector = buildUserPreferenceVector(userItemWeights);
            double[] houseVector = buildHouseFeatureVector(house);
            double similarity = cosineSimilarity(userPreferenceVector, houseVector);
            if (similarity > 0.8) {
                reasons.add("符合您的浏览偏好");
            }
        }

        if (reasons.isEmpty()) {
            return "基于您可能感兴趣的内容推荐";
        }

        return String.join("；", reasons.subList(0, Math.min(3, reasons.size())));
    }

    /**
     * 清除推荐缓存
     * 当用户行为更新时调用
     */
    @CacheEvict(value = "recommendations", allEntries = false)
    public void evictUserRecommendationCache(Long userId) {
        // 缓存清除由Spring自动处理
    }

    /**
     * 清除所有推荐缓存
     * 当房源信息更新时调用
     */
    @CacheEvict(value = "recommendations", allEntries = true)
    public void evictAllRecommendationCache() {
        // 缓存清除由Spring自动处理
    }
}
