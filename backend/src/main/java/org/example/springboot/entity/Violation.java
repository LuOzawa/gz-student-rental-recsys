package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 违规记录实体类
 */
@Data
@TableName("violation")
@Schema(description = "违规记录实体类")
public class Violation {
    @TableId(type = IdType.AUTO)
    @Schema(description = "违规ID")
    private Long id;

    @Schema(description = "违规用户ID")
    private Long userId;

    @Schema(description = "违规类型")
    private String type;

    @Schema(description = "违规描述")
    private String description;

    @Schema(description = "关联ID(房源/帖子等)")
    private Long relatedId;

    @Schema(description = "关联类型(house/post等)")
    private String relatedType;

    @Schema(description = "处罚措施")
    private String penalty;

    @Schema(description = "扣除信用分")
    private Integer creditDeduction;

    @Schema(description = "状态(0:待处理,1:已处理,2:已忽略)")
    private Integer status;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "违规用户信息")
    private User user;

    @TableField(exist = false)
    @Schema(description = "处理人信息")
    private User handler;

    @TableField(exist = false)
    @Schema(description = "违规用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "处理人姓名")
    private String handlerName;
}
