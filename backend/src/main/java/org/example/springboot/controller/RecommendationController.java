package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.House;
import org.example.springboot.service.CollaborativeFilteringService;
import org.example.springboot.service.UserBehaviorService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "推荐接口")
@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Resource
    private UserBehaviorService userBehaviorService;

    @Resource
    private CollaborativeFilteringService collaborativeFilteringService;

    @Operation(summary = "获取个性化推荐房源")
    @GetMapping("/personalized")
    public Result<List<House>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<House> houses = userBehaviorService.getPersonalizedRecommendations(limit);
        return Result.success(houses);
    }

    @Operation(summary = "获取相似房源")
    @GetMapping("/similar/{houseId}")
    public Result<List<House>> getSimilarHouses(
            @PathVariable Long houseId,
            @RequestParam(defaultValue = "6") Integer limit) {
        List<House> houses = userBehaviorService.getSimilarHouses(houseId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "获取热门房源")
    @GetMapping("/hot")
    public Result<List<House>> getHotHouses(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<House> houses = userBehaviorService.getHotHouses(limit);
        return Result.success(houses);
    }

    @Operation(summary = "获取新用户推荐(冷启动)")
    @GetMapping("/cold-start")
    public Result<List<House>> getColdStartRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<House> houses = userBehaviorService.getColdStartRecommendations(limit);
        return Result.success(houses);
    }

    @Operation(summary = "获取最近搜索关键词")
    @GetMapping("/search-keywords")
    public Result<List<String>> getRecentSearchKeywords(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<String> keywords = userBehaviorService.getRecentSearchKeywords(limit);
        return Result.success(keywords);
    }

    @Operation(summary = "基于用户偏好获取推荐房源")
    @GetMapping("/preference-based")
    public Result<List<House>> getPreferenceBasedRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getPreferenceBasedRecommendations(userId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "智能混合推荐(动态权重)")
    @GetMapping("/smart-hybrid")
    public Result<List<House>> getSmartHybridRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getSmartHybridRecommendations(userId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "Item-Based协同过滤推荐")
    @GetMapping("/item-cf")
    public Result<List<House>> getItemBasedCFRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getItemBasedCFRecommendations(userId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "User-Based协同过滤推荐")
    @GetMapping("/user-cf")
    public Result<List<House>> getUserBasedCFRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getUserBasedCFRecommendations(userId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "基于内容的推荐")
    @GetMapping("/content-based")
    public Result<List<House>> getContentBasedRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getContentBasedRecommendations(userId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "混合推荐(User-CF + Item-CF)")
    @GetMapping("/hybrid")
    public Result<List<House>> getHybridRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getHybridRecommendations(userId, limit);
        return Result.success(houses);
    }

    @Operation(summary = "获取推荐解释")
    @GetMapping("/explanation/{houseId}")
    public Result<Map<String, Object>> getRecommendationExplanation(
            @PathVariable Long houseId) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        House house = collaborativeFilteringService.getHouseById(houseId);
        if (house == null) {
            return Result.error("房源不存在");
        }

        String explanation = collaborativeFilteringService.generateRecommendationExplanation(userId, house);

        // 计算多维度评分
        double multiDimScore = collaborativeFilteringService.calculateMultiDimensionalScore(
            userId, house, java.time.LocalDateTime.now());

        Map<String, Object> result = new HashMap<>();
        result.put("houseId", houseId);
        result.put("explanation", explanation);
        result.put("score", Math.round(multiDimScore * 1000) / 1000.0);

        return Result.success(result);
    }

    @Operation(summary = "获取推荐房源列表(含推荐理由)")
    @GetMapping("/with-reasons")
    public Result<List<Map<String, Object>>> getRecommendationsWithReasons(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = JwtTokenUtils.getCurrentUserId();
        List<House> houses = collaborativeFilteringService.getSmartHybridRecommendations(userId, limit);

        List<Map<String, Object>> result = houses.stream()
            .map(house -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", house.getId());
                item.put("title", house.getTitle());
                item.put("price", house.getPrice());
                item.put("area", house.getArea());
                item.put("address", house.getAddress());
                item.put("image", house.getImages());
                item.put("explanation", collaborativeFilteringService.generateRecommendationExplanation(userId, house));
                item.put("score", collaborativeFilteringService.calculateMultiDimensionalScore(
                    userId, house, java.time.LocalDateTime.now()));
                return item;
            })
            .limit(limit)
            .toList();

        return Result.success(result);
    }
}
