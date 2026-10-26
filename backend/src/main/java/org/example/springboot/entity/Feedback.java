package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈实体类
 */
@Data
@TableName("feedback")
@Schema(description = "反馈实体类")
public class Feedback {
    @TableId(type = IdType.AUTO)
    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "反馈用户ID")
    private Long userId;

    @Schema(description = "反馈类型(建议/投诉/bug等)")
    private String type;

    @Schema(description = "反馈标题")
    private String title;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "图片(JSON格式)")
    private String images;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "状态(0:待处理,1:处理中,2:已处理,3:已关闭)")
    private Integer status;

    @Schema(description = "管理员回复")
    private String reply;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "反馈用户信息")
    private User user;

    @TableField(exist = false)
    @Schema(description = "处理人信息")
    private User handler;

    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "处理人姓名")
    private String handlerName;
}
