package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.UserBehavior;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * Get user's viewed house IDs with view counts
     */
    List<Map<String, Object>> getUserViewedHouses(@Param("userId") Long userId);

    /**
     * Get user's favorite house IDs
     */
    List<Long> getUserFavoriteHouseIds(@Param("userId") Long userId);

    /**
     * Get user's search keywords
     */
    List<String> getUserSearchKeywords(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Get houses similar to a house based on user behavior
     */
    List<Long> getSimilarHousesByBehavior(@Param("houseId") Long houseId);

    /**
     * Get hot houses (most viewed in recent days)
     */
    List<Long> getHotHouses(@Param("days") int days, @Param("limit") int limit);

    /**
     * Get user's appointment house IDs
     */
    List<Long> getUserAppointmentHouseIds(@Param("userId") Long userId);

    /**
     * Get active user IDs in recent days
     */
    List<Long> getActiveUserIds(@Param("days") int days);
}
