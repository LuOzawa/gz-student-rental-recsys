package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源评论实体类
 */
@Data
@TableName("house_comment")
@Schema(description = "房源评论实体类")
public class HouseComment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "房屋ID")
    private Long houseId;

    @Schema(description = "评论用户ID")
    private Long userId;

    @Schema(description = "类型(1:评价,2:留言)")
    private Integer type;

    @Schema(description = "评分(1-5,留言时为null)")
    private Integer score;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "房东回复内容")
    private String replyContent;

    @Schema(description = "房东回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "房东回复者ID")
    private Long replyUserId;

    @Schema(description = "回复者类型(1:房东,2:管理员) - 已废弃，保留兼容")
    private Integer replyUserType;

    @Schema(description = "管理员回复内容")
    private String adminReplyContent;

    @Schema(description = "管理员回复时间")
    private LocalDateTime adminReplyTime;

    @Schema(description = "管理员回复者ID")
    private Long adminReplyUserId;

    @Schema(description = "状态(0:隐藏,1:显示)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "房屋信息")
    private House house;

    @TableField(exist = false)
    @Schema(description = "评论用户信息")
    private User user;

    @TableField(exist = false)
    @Schema(description = "评论用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "评论用户头像")
    private String userAvatar;

    @TableField(exist = false)
    @Schema(description = "房东回复者名称")
    private String replyUserName;

    @TableField(exist = false)
    @Schema(description = "管理员回复者名称")
    private String adminReplyUserName;
}
