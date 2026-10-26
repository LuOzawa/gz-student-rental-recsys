package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springboot.entity.Favorite;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * Check if user has favorited a house
     */
    boolean isFavorited(@Param("userId") Long userId, @Param("houseId") Long houseId);

    /**
     * Get user's favorite house IDs
     */
    List<Long> getUserFavoriteHouseIds(@Param("userId") Long userId);
}
