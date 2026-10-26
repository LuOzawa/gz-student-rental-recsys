package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.House;
import org.example.springboot.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏接口")
@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping("/{houseId}")
    public Result<?> addFavorite(@PathVariable Long houseId, @RequestParam(required = false) String note) {
        try {
            favoriteService.addFavorite(houseId, note);
            return Result.success("收藏成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{houseId}")
    public Result<?> removeFavorite(@PathVariable Long houseId) {
        try {
            favoriteService.removeFavorite(houseId);
            return Result.success("取消收藏成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{houseId}")
    public Result<Boolean> checkFavorited(@PathVariable Long houseId) {
        boolean favorited = favoriteService.isFavorited(houseId);
        return Result.success(favorited);
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping("/list")
    public Result<Page<House>> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Page<House> page = favoriteService.getUserFavoriteHouses(currentPage, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取收藏数量")
    @GetMapping("/count")
    public Result<Integer> getFavoriteCount() {
        int count = favoriteService.getUserFavoriteCount();
        return Result.success(count);
    }
}
