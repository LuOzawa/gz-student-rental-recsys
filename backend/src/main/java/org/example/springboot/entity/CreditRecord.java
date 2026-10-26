package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用记录实体类
 */
@Data
@TableName("credit_record")
@Schema(description = "信用记录实体类")
public class CreditRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "变更类型(增加/扣除)")
    private String changeType;

    @Schema(description = "变更值")
    private Integer changeValue;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "关联ID")
    private Long relatedId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // 关联查询字段
    @TableField(exist = false)
    @Schema(description = "用户信息")
    private User user;

    @TableField(exist = false)
    @Schema(description = "操作人信息")
    private User operator;

    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "操作人姓名")
    private String operatorName;
}
