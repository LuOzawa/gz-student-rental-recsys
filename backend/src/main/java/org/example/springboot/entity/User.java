package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 */
@Data
@TableName("user")
@Schema(description = "用户实体类")
public class User {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间")
    private String username;
    
    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    private String password;
    
    @Schema(description = "邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Schema(description = "角色编码")
    private String roleCode;
    
    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "所属校区")
    private String campus;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "入学年份")
    private Integer enrollmentYear;

    @Schema(description = "租房预算(月)")
    private BigDecimal rentBudget;

    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "状态(0:禁用,1:正常)")
    private Integer status;

    @Schema(description = "审核状态(0:待审核,1:审核通过,2:审核拒绝)")
    private Integer auditStatus;

    @Schema(description = "审核拒绝原因")
    private String auditReason;

    @Schema(description = "实名认证状态(0:未认证,1:待审核,2:已认证,3:审核拒绝)")
    private Integer verifyStatus;

    @Schema(description = "实名认证拒绝原因")
    private String verifyRejectReason;

    @Schema(description = "信用分(仅房东,初始100分)")
    private Integer creditScore;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String token;

}