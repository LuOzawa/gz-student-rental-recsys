package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;
import org.example.springboot.entity.UserBehavior;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.UserBehaviorMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserBehaviorService {

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private CollaborativeFilteringService collaborativeFilteringService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Record user behavior
     */
    public void recordBehavior(String behaviorType, String targetType, Long targetId, Map<String, Object> data) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return; // Don't record behavior for anonymous users
        }

        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(currentUser.getId());
            behavior.setBehaviorType(behaviorType);
            behavior.setTargetType(targetType);
            behavior.setTargetId(targetId);

            if (data != null && !data.isEmpty()) {
                behavior.setBehaviorData(objectMapper.writeValueAsString(data));
            }

            // Get request info
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                behavior.setIpAddress(getClientIpAddress(request));
                behavior.setUserAgent(request.getHeader("User-Agent"));
            }

            userBehaviorMapper.insert(behavior);
        } catch (Exception e) {
            // Don't throw exception for behavior recording failures
            e.printStackTrace();
        }
    }

    /**
     * Track house view
     */
    public void trackHouseView(Long houseId) {
        recordBehavior("view", "house", houseId, null);

        // Also increment house view count
        House house = houseMapper.selectById(houseId);
        if (house != null) {
            house.setViewCount((house.getViewCount() != null ? house.getViewCount() : 0) + 1);
            houseMapper.updateById(house);
        }
    }

    /**
     * Track search
     */
    public void trackSearch(String keyword, Map<String, Object> searchParams) {
        Map<String, Object> data = new HashMap<>();
        data.put("keyword", keyword);
        if (searchParams != null) {
            data.putAll(searchParams);
        }
        recordBehavior("search", null, null, data);
    }

    /**
     * Track favorite
     */
    public void trackFavorite(Long houseId) {
        recordBehavior("favorite", "house", houseId, null);
    }

    /**
     * Track appointment
     */
    public void trackAppointment(Long houseId) {
        recordBehavior("appointment", "house", houseId, null);
    }

    /**
     * Get personalized recommendations based on user behavior
     * 使用混合推荐算法：
     * - Content-Based CF (40%): 基于用户偏好特征
     * - Item-Based CF (35%): 基于房源相似度
     * - User-Based CF (25%): 基于用户相似度
     */
    public List<House> getPersonalizedRecommendations(int limit) {
        try {
            User currentUser = JwtTokenUtils.getCurrentUser();
            if (currentUser == null) {
                // 未登录用户返回热门房源（冷启动策略）
                return collaborativeFilteringService.getColdStartRecommendations(limit);
            }

            Long userId = currentUser.getId();

            // 检查用户是否有足够的行为数据
            Set<Long> interactedItems = getUserInteractedItems(userId);
            if (interactedItems == null || interactedItems.size() < 3) {
                // 行为数据不足，使用冷启动策略（热门+高质量）
                return collaborativeFilteringService.getColdStartRecommendations(limit);
            }

            // 使用混合推荐算法
            // Content-Based CF (40%) + Item-Based CF (35%) + User-Based CF (25%)
            List<House> contentBasedRecs = safeGetRecommendations(() ->
                collaborativeFilteringService.getContentBasedRecommendations(userId, limit), new ArrayList<>());
            List<House> itemCFRecs = safeGetRecommendations(() ->
                collaborativeFilteringService.getItemBasedCFRecommendations(userId, limit), new ArrayList<>());
            List<House> userCFRecs = safeGetRecommendations(() ->
                collaborativeFilteringService.getUserBasedCFRecommendations(userId, limit), new ArrayList<>());

        // 合并结果，计算综合分数
        Map<Long, Double> hybridScores = new HashMap<>();

        // Content-Based CF 权重 0.40
        for (int i = 0; i < contentBasedRecs.size(); i++) {
            House house = contentBasedRecs.get(i);
            double score = (contentBasedRecs.size() - i) * 0.40;
            hybridScores.merge(house.getId(), score, Double::sum);
        }

        // Item-Based CF 权重 0.35
        for (int i = 0; i < itemCFRecs.size(); i++) {
            House house = itemCFRecs.get(i);
            double score = (itemCFRecs.size() - i) * 0.35;
            hybridScores.merge(house.getId(), score, Double::sum);
        }

        // User-Based CF 权重 0.25
        for (int i = 0; i < userCFRecs.size(); i++) {
            House house = userCFRecs.get(i);
            double score = (userCFRecs.size() - i) * 0.25;
            hybridScores.merge(house.getId(), score, Double::sum);
        }

        // 按综合分数排序
        List<Long> recommendedIds = hybridScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<House> result = new ArrayList<>();
        for (Long id : recommendedIds) {
            House house = collaborativeFilteringService.getHouseById(id);
            if (house != null && house.getStatus() == 1 && house.getAuditStatus() == 1) {
                result.add(house);
            }
            if (result.size() >= limit) {
                break;
            }
        }

        // 如果结果不足，用热门房源补充
        if (result.size() < limit) {
            List<House> hotHouses = safeGetRecommendations(() ->
                collaborativeFilteringService.getColdStartRecommendations(limit - result.size()), new ArrayList<>());
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
        } catch (Exception e) {
            // 发生错误时返回冷启动推荐
            try {
                return collaborativeFilteringService.getColdStartRecommendations(limit);
            } catch (Exception ex) {
                // 如果冷启动也失败，返回空列表
                return new ArrayList<>();
            }
        }
    }

    /**
     * 安全获取推荐列表，捕获异常并返回默认值
     */
    private List<House> safeGetRecommendations(java.util.function.Supplier<List<House>> supplier, List<House> defaultValue) {
        try {
            List<House> result = supplier.get();
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取用户交互过的房源ID集合（内部方法）
     */
    private Set<Long> getUserInteractedItems(Long userId) {
        Set<Long> interactedItems = new HashSet<>();

        try {
            // 浏览记录
            List<Map<String, Object>> viewedHouses = userBehaviorMapper.getUserViewedHouses(userId);
            for (Map<String, Object> record : viewedHouses) {
                interactedItems.add(((Number) record.get("houseId")).longValue());
            }

            // 收藏记录
            interactedItems.addAll(userBehaviorMapper.getUserFavoriteHouseIds(userId));

            // 预约记录
            interactedItems.addAll(userBehaviorMapper.getUserAppointmentHouseIds(userId));
        } catch (Exception e) {
            // 忽略错误，返回空集合
        }

        return interactedItems;
    }

    /**
     * Get similar houses based on house features
     * 使用余弦相似度计算相似房源
     */
    public List<House> getSimilarHouses(Long houseId, int limit) {
        try {
            // 使用余弦相似度算法计算相似房源
            return collaborativeFilteringService.getSimilarHousesByCosine(houseId, limit);
        } catch (Exception e) {
            // 发生错误时返回基于内容的相似房源
            try {
                return collaborativeFilteringService.getColdStartRecommendations(limit);
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }
    }

    /**
     * Get hot houses (most viewed)
     */
    public List<House> getHotHouses(int limit) {
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

            // 如果没有热门房源，返回推荐的房源
            if (houses.isEmpty()) {
                return collaborativeFilteringService.getColdStartRecommendations(limit);
            }
            return houses;
        } catch (Exception e) {
            // 发生错误时返回冷启动推荐
            return collaborativeFilteringService.getColdStartRecommendations(limit);
        }
    }

    /**
     * Get cold start recommendations for new users
     * 冷启动策略：返回热门房源
     */
    public List<House> getColdStartRecommendations(int limit) {
        try {
            return collaborativeFilteringService.getColdStartRecommendations(limit);
        } catch (Exception e) {
            // 返回基本的可用房源列表
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
     * Get user's recent search keywords
     */
    public List<String> getRecentSearchKeywords(int limit) {
        try {
            User currentUser = JwtTokenUtils.getCurrentUser();
            if (currentUser == null) {
                return Collections.emptyList();
            }

            List<String> keywords = userBehaviorMapper.getUserSearchKeywords(currentUser.getId(), limit);
            return keywords != null ? keywords : Collections.emptyList();
        } catch (Exception e) {
            // 发生错误时返回空列表
            return Collections.emptyList();
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
