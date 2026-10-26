package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子实体类
 */
@Data
@TableName("forum_post")
@Schema(description = "论坛帖子实体类")
public class ForumPost {
    @TableId(type = IdType.AUTO)
    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "发布用户ID")
    private Long userId;

    @Schema(description = "帖子标题")
    private String title;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "图片(JSON格式)")
    private String images;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "回复数量")
    private Integer replyCount;

    @Schema(description = "是否置顶(0:否,1:是)")
    private Integer isTop;

    @Schema(description = "是否精华(0:否,1:是)")
    private Integer isEssence;

    @Schema(description = "状态(0:隐藏,1:正常,2:已删除)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "分类信息")
    private ForumCategory category;

    @TableField(exist = false)
    @Schema(description = "发布用户信息")
    private User user;

    @TableField(exist = false)
    @Schema(description = "分类名称")
    private String categoryName;

    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "用户头像")
    private String userAvatar;
}
