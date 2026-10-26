package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Favorite;
import org.example.springboot.entity.House;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.FavoriteMapper;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private HouseMapper houseMapper;

    @Resource
    private UserBehaviorService userBehaviorService;

    /**
     * Add to favorite
     */
    public void addFavorite(Long houseId, String note) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // Check if already favorited
        if (favoriteMapper.isFavorited(currentUser.getId(), houseId)) {
            throw new RuntimeException("已经收藏过了");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(currentUser.getId());
        favorite.setHouseId(houseId);
        favorite.setNote(note);
        favoriteMapper.insert(favorite);

        // Track favorite behavior for recommendations
        userBehaviorService.trackFavorite(houseId);

        // Update house favorite count
        House house = houseMapper.selectById(houseId);
        if (house != null) {
            Integer currentCount = house.getFavoriteCount();
            house.setFavoriteCount(currentCount != null ? currentCount + 1 : 1);
            houseMapper.updateById(house);
        }
    }

    /**
     * Remove from favorite
     */
    public void removeFavorite(Long houseId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", currentUser.getId()).eq("house_id", houseId);
        favoriteMapper.delete(wrapper);

        // Update house favorite count
        House house = houseMapper.selectById(houseId);
        if (house != null) {
            Integer currentCount = house.getFavoriteCount();
            house.setFavoriteCount(currentCount != null && currentCount > 0 ? currentCount - 1 : 0);
            houseMapper.updateById(house);
        }
    }

    /**
     * Check if house is favorited by current user
     */
    public boolean isFavorited(Long houseId) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return favoriteMapper.isFavorited(currentUser.getId(), houseId);
    }

    /**
     * Get user's favorites with house details (paginated)
     */
    public Page<House> getUserFavoriteHouses(int currentPage, int pageSize) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // Get favorite house IDs
        List<Long> houseIds = favoriteMapper.getUserFavoriteHouseIds(currentUser.getId());
        if (houseIds.isEmpty()) {
            return new Page<>(currentPage, pageSize);
        }

        // Paginate the results
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, houseIds.size());
        List<Long> pageHouseIds = houseIds.subList(start, end);

        // Fetch house details
        List<House> houses = pageHouseIds.stream()
                .map(houseMapper::selectById)
                .collect(Collectors.toList());

        Page<House> page = new Page<>(currentPage, pageSize, houseIds.size());
        page.setRecords(houses);
        return page;
    }

    /**
     * Get user's favorite count
     */
    public int getUserFavoriteCount() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        return Math.toIntExact(favoriteMapper.getUserFavoriteHouseIds(currentUser.getId()).size());
    }
}
