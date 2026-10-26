package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.House;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 高德地图服务
 * 提供地址解析、周边设施查询、距离计算等功能
 */
@Slf4j
@Service
public class AmapService {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String REVERSE_GEOCODE_URL = "https://restapi.amap.com/v3/geocode/regeo";
    private static final String AROUND_SEARCH_URL = "https://restapi.amap.com/v5/place/around";
    private static final String DISTANCE_URL = "https://restapi.amap.com/v3/distance";

    @Value("${amap.api.key:3a3471f3d38b1f56d507b51ae00473b1}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HouseMapper houseMapper;

    public AmapService(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 地址转坐标（地理编码）
     */
    public Map<String, Object> geocode(String address, String city) {
        try {
            String url = GEOCODE_URL + "?key=" + apiKey + "&address=" + address;
            if (city != null && !city.isEmpty()) {
                url += "&city=" + city;
            }

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            int status = root.path("status").asInt();
            if (status == 1) {
                JsonNode geocodes = root.path("geocodes");
                if (geocodes.isArray() && geocodes.size() > 0) {
                    JsonNode location = geocodes.get(0).path("location");
                    String[] coords = location.asText().split(",");
                    return Map.of(
                        "longitude", new BigDecimal(coords[0]),
                        "latitude", new BigDecimal(coords[1]),
                        "formattedAddress", geocodes.get(0).path("formatted_address").asText()
                    );
                }
            }
            return Map.of("error", "地址解析失败");
        } catch (Exception e) {
            log.error("地址解析失败: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 坐标转地址（逆地理编码）
     */
    public Map<String, Object> reverseGeocode(BigDecimal longitude, BigDecimal latitude) {
        try {
            String url = REVERSE_GEOCODE_URL + "?key=" + apiKey
                    + "&location=" + longitude + "," + latitude;

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            int status = root.path("status").asInt();
            if (status == 1) {
                JsonNode regeocode = root.path("regeocode");
                return Map.of(
                    "formattedAddress", regeocode.path("formatted_address").asText(),
                    "province", regeocode.path("addressComponent").path("province").asText(),
                    "city", regeocode.path("addressComponent").path("city").asText(),
                    "district", regeocode.path("addressComponent").path("district").asText()
                );
            }
            return Map.of("error", "坐标解析失败");
        } catch (Exception e) {
            log.error("坐标解析失败: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 周边设施查询
     * @param keywords 关键词类型: 交通设施=150500|餐饮服务=050000|购物服务=060000|生活服务=070000|体育休闲服务=080000
     */
    public List<Map<String, Object>> searchNearbyFacilities(BigDecimal longitude, BigDecimal latitude, String keywords) {
        List<Map<String, Object>> facilities = new ArrayList<>();

        try {
            // 默认搜索地铁、公交、餐饮、购物等设施
            String[] types = keywords != null
                ? new String[]{keywords}
                : new String[]{"150500", "050000", "060000", "070000"}; // 交通、餐饮、购物、生活

            for (String type : types) {
                String url = AROUND_SEARCH_URL + "?key=" + apiKey
                        + "&location=" + longitude + "," + latitude
                        + "&keywords=" + type
                        + "&radius=1000"
                        + "&show_fields=business,distance";

                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(response);

                JsonNode pois = root.path("pois");
                if (pois.isArray()) {
                    for (JsonNode poi : pois) {
                        Map<String, Object> facility = new HashMap<>();
                        facility.put("name", poi.path("name").asText());
                        facility.put("type", poi.path("type").asText());
                        facility.put("distance", poi.path("distance").asText());
                        facility.put("address", poi.path("address").asText());
                        facility.put("location", poi.path("location").asText());

                        // 解析位置坐标
                        String location = poi.path("location").asText();
                        if (location != null && !location.isEmpty()) {
                            String[] coords = location.split(",");
                            facility.put("longitude", new BigDecimal(coords[0]));
                            facility.put("latitude", new BigDecimal(coords[1]));
                        }

                        facilities.add(facility);
                    }
                }
            }
        } catch (Exception e) {
            log.error("周边设施查询失败: {}", e.getMessage());
        }

        return facilities;
    }

    /**
     * 计算两点间距离（使用高德距离API）
     */
    public BigDecimal calculateDistance(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        try {
            String url = DISTANCE_URL + "?key=" + apiKey
                    + "&origins=" + lng1 + "," + lat1
                    + "&destination=" + lng2 + "," + lat2
                    + "&type=1"; // 直线距离

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            int status = root.path("status").asInt();
            if (status == 1) {
                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    int distance = results.get(0).path("distance").asInt();
                    return new BigDecimal(distance).divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP); // 转换为公里
                }
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("距离计算失败: {}", e.getMessage());
            // 降级使用Haversine公式计算
            return calculateDistanceByHaversine(lng1, lat1, lng2, lat2);
        }
    }

    /**
     * 使用Haversine公式计算两点间距离（降级方案）
     */
    private BigDecimal calculateDistanceByHaversine(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        final int EARTH_RADIUS = 6371; // 地球半径，单位：公里

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double lng1Rad = Math.toRadians(lng1.doubleValue());
        double lng2Rad = Math.toRadians(lng2.doubleValue());

        double dLat = lat2Rad - lat1Rad;
        double dLng = lng2Rad - lng1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return new BigDecimal(EARTH_RADIUS * c).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 地图范围搜索房源
     */
    public Page<House> searchHousesByBounds(BigDecimal minLng, BigDecimal maxLng,
                                           BigDecimal minLat, BigDecimal maxLat,
                                           Integer currentPage, Integer size) {
        Page<House> page = new Page<>(currentPage, size);

        LambdaQueryWrapper<House> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(House::getStatus, 1)
                .eq(House::getAuditStatus, 1)
                .isNotNull(House::getLatitude)
                .isNotNull(House::getLongitude)
                .between(House::getLongitude, minLng, maxLng)
                .between(House::getLatitude, minLat, maxLat)
                .orderByDesc(House::getCreateTime);

        houseMapper.selectPage(page, queryWrapper);

        // 为每个房源计算与地图中心的距离
        BigDecimal centerLng = (minLng.add(maxLng)).divide(new BigDecimal("2"), 7, RoundingMode.HALF_UP);
        BigDecimal centerLat = (minLat.add(maxLat)).divide(new BigDecimal("2"), 7, RoundingMode.HALF_UP);

        for (House house : page.getRecords()) {
            if (house.getLongitude() != null && house.getLatitude() != null) {
                BigDecimal distance = calculateDistanceByHaversine(
                    centerLng, centerLat, house.getLongitude(), house.getLatitude()
                );
                house.setDistance(distance);
            }
        }

        // 按距离排序
        page.getRecords().sort(Comparator.comparing(House::getDistance));

        return page;
    }

    /**
     * 计算房源到指定校区的距离
     */
    public BigDecimal calculateDistanceToCampus(Long houseId, String campus) {
        House house = houseMapper.selectById(houseId);
        if (house == null || house.getLongitude() == null || house.getLatitude() == null) {
            return null;
        }

        Map<String, double[]> campusCoordinates = getCampusCoordinates();
        double[] coords = campusCoordinates.get(campus);

        if (coords == null) {
            return null;
        }

        BigDecimal campusLng = new BigDecimal(coords[0]);
        BigDecimal campusLat = new BigDecimal(coords[1]);

        return calculateDistance(house.getLongitude(), house.getLatitude(), campusLng, campusLat);
    }

    /**
     * 获取校区坐标配置
     */
    public Map<String, double[]> getCampusCoordinates() {
        return Map.of(
            "大学城校区", new double[]{113.4000, 23.0500},
            "南校区", new double[]{113.3800, 23.0400},
            "北校区", new double[]{113.4200, 23.0600},
            "东校区", new double[]{113.4300, 23.0450}
        );
    }

    /**
     * 更新房源的周边设施信息（缓存到数据库）
     */
    public void updateHouseNearbyFacilities(Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null || house.getLongitude() == null || house.getLatitude() == null) {
            return;
        }

        List<Map<String, Object>> facilities = searchNearbyFacilities(
            house.getLongitude(), house.getLatitude(), null
        );

        try {
            String facilitiesJson = objectMapper.writeValueAsString(facilities);
            house.setNearbyFacilities(facilitiesJson);
            houseMapper.updateById(house);
        } catch (JsonProcessingException e) {
            log.error("周边设施JSON序列化失败: {}", e.getMessage());
        }
    }
}
