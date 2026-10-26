package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约看房实体类
 */
@Data
@TableName("appointment")
@Schema(description = "预约看房实体类")
public class Appointment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "预约ID")
    private Long id;

    @Schema(description = "房屋ID")
    private Long houseId;

    @Schema(description = "租客ID")
    private Long tenantId;

    @Schema(description = "房东ID")
    private Long landlordId;

    @Schema(description = "预约日期")
    private LocalDate appointmentDate;

    @Schema(description = "预约时间段")
    private String appointmentTime;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "留言信息")
    private String message;

    @Schema(description = "状态(0:待确认,1:已确认,2:已拒绝,3:已完成,4:已取消)")
    private Integer status;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "房屋信息")
    private House house;

    @TableField(exist = false)
    @Schema(description = "租客信息")
    private User tenant;

    @TableField(exist = false)
    @Schema(description = "房东信息")
    private User landlord;
}
