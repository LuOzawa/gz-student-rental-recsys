package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛回复实体类
 */
@Data
@TableName("forum_reply")
@Schema(description = "论坛回复实体类")
public class ForumReply {
    @TableId(type = IdType.AUTO)
    @Schema(description = "回复ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "回复用户ID")
    private Long userId;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "父回复ID")
    private Long parentId;

    @Schema(description = "回复给谁(用户ID)")
    private Long replyToUserId;

    @Schema(description = "状态(0:隐藏,1:正常,2:已删除)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "回复用户信息")
    private User user;

    @TableField(exist = false)
    @Schema(description = "被回复用户信息")
    private User replyToUser;

    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "用户头像")
    private String userAvatar;

    @TableField(exist = false)
    @Schema(description = "被回复用户名")
    private String replyToUsername;
}
