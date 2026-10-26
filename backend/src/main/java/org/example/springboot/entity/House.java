package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 房屋信息实体类
 */
@Data
@TableName("house")
@Schema(description = "房屋实体类")
public class House {
    @TableId(type = IdType.AUTO)
    @Schema(description = "房屋ID")
    private Long id;

    @Schema(description = "房屋标题")
    private String title;

    @Schema(description = "房屋描述")
    private String description;

    @Schema(description = "面积(平方米)")
    private BigDecimal area;

    @Schema(description = "价格(元/月)")
    private BigDecimal price;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "房屋类型ID")
    private Long typeId;

    @Schema(description = "房东ID")
    private Long landlordId;

    @Schema(description = "状态(0:下架,1:待出租,2:已出租)")
    private Integer status;

    @Schema(description = "审核状态(0:待审核,1:审核通过,2:审核拒绝)")
    private Integer auditStatus;

    @Schema(description = "审核拒绝原因")
    private String auditReason;

    @Schema(description = "平均评分(0-5)")
    private BigDecimal avgScore;

    @Schema(description = "评论数量")
    private Integer commentCount;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "收藏次数")
    private Integer favoriteCount;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "小区名称")
    private String communityName;

    @Schema(description = "装修情况(0:毛坯,1:简装,2:精装,3:豪装)")
    private Integer decoration;

    @Schema(description = "楼层")
    private Integer floor;

    @Schema(description = "总楼层数")
    private Integer totalFloors;

    @Schema(description = "朝向(0:东,1:南,2:西,3:北,4:东南,5:西南,6:东北,7:西北,8:南北通透)")
    private Integer orientation;

    @Schema(description = "建筑年代")
    private Integer buildYear;

    @Schema(description = "房屋类型(0:普通住宅,1:公寓,2:别墅,3:loft,4:复式)")
    private Integer buildingType;

    @Schema(description = "押付方式(0:押一付一,1:押一付三,2:押三付一,3:押一付六,4:半年付,5:年付,6:无押金)")
    private Integer paymentMethod;

    @Schema(description = "最短租期(月)")
    private Integer minLeaseTerm;

    @Schema(description = "入住时间")
    private LocalDate checkInTime;

    @Schema(description = "服务费(元/月)")
    private BigDecimal serviceFee;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @TableField(exist = false)
    @Schema(description = "周边设施(JSON格式)")
    private String nearbyFacilities;

    @Schema(description = "是否推荐(0:否,1:是)")
    private Integer isRecommended;

    @Schema(description = "推荐排序值")
    private Integer recommendSort;

    @Schema(description = "房屋图片(JSON格式)")
    private String images;

    @Schema(description = "配套设施(JSON格式)")
    private String facilities;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "房屋类型名称")
    private String typeName;

    @TableField(exist = false)
    @Schema(description = "房东姓名")
    private String landlordName;
    @TableField(exist = false)
    @Schema(description = "房东头像")
    private String landlordImg;
    @TableField(exist = false)
    @Schema(description = "房东信用分")
    private Integer landlordCreditScore;

    @TableField(exist = false)
    @Schema(description = "距离(公里,用于地图搜索)")
    private BigDecimal distance;

    @TableField(exist = false)
    @Schema(description = "装修情况名称")
    private String decorationName;

    @TableField(exist = false)
    @Schema(description = "朝向名称")
    private String orientationName;

    @TableField(exist = false)
    @Schema(description = "周边设施数据(解析后的对象)")
    private Map<String, Object> nearbyFacilitiesData;

} 