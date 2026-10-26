package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name="用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    @Resource
    private UserService userService;
    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "根据id获取用户信息")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        // 如果用户不存在会抛出异常，由全局异常处理器处理
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "根据username获取用户信息")
    @GetMapping("/username/{username}")
    public Result<?> getUserByUsername(@PathVariable String username) {
        // 不存在的用户会抛出异常
        User user = userService.getByUsername(username);
        return Result.success(user);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {
        User loginUser = userService.login(user);
        return Result.success(loginUser);
    }

    @Operation(summary = "密码修改")
    @PutMapping("/password/{id}")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO userPasswordUpdate) {
        // 密码修改失败会抛出异常
        userService.updatePassword(id, userPasswordUpdate);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "忘记密码")
    @GetMapping("/forget")
    public Result<?> forgetPassword(@RequestParam String email, @RequestParam String newPassword) {
        // 密码重置失败会抛出异常
        userService.forgetPassword(email, newPassword);
        return Result.success("密码重置成功");
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<?> getUsersByPage(
            @RequestParam(defaultValue = "") String username,

            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String roleCode,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> page = userService.getUsersByPage(username,  name, roleCode, auditStatus, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "根据角色获取用户列表")
    @GetMapping("/role/{roleCode}")
    public Result<?> getUserByRole(@PathVariable String roleCode) {
        List<User> users = userService.getUserByRole(roleCode);
        return Result.success(users);
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam List<Integer> ids) {
        userService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/all")
    public Result<?> getUserList() {
        List<User> list = userService.getUserList();
        return Result.success(list);
    }

    @Operation(summary = "创建新用户")
    @PostMapping("/add")
    public Result<?> createUser(@RequestBody  User user) {
        userService.createUser(user);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        // 更新失败会抛出具体原因的异常
        userService.updateUser(id, user);
        return Result.success("更新成功");
    }

    @Operation(summary = "根据id删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteUserById(@PathVariable Long id) {
        // 删除失败会抛出异常
        userService.deleteUserById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<?> getCurrentUser() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        return Result.success(currentUser);
    }
    @Operation(summary = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateStatus(@PathVariable Long userId, @RequestParam Integer status) {
        User user = userMapper.selectById(userId);
        user.setStatus(status);
        userService.updateUser(userId,user);
        return Result.success();
    }

    @Operation(summary = "审核通过用户/房东")
    @PutMapping("/approve/{id}")
    public Result<?> approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return Result.success("审核通过");
    }

    @Operation(summary = "审核拒绝用户/房东")
    @PutMapping("/reject/{id}")
    public Result<?> rejectUser(@PathVariable Long id, @RequestParam String reason) {
        userService.rejectUser(id, reason);
        return Result.success("审核已拒绝");
    }

    // ==================== 实名认证相关接口 ====================

    @Operation(summary = "提交实名认证")
    @PostMapping("/verify/submit")
    public Result<?> submitVerification(@RequestBody User user) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        userService.submitVerification(currentUser.getId(), user.getName(), user.getIdCard());
        return Result.success("实名认证信息已提交，请等待管理员审核");
    }

    @Operation(summary = "获取实名认证状态")
    @GetMapping("/verify/status")
    public Result<?> getVerificationStatus() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        User user = userService.getUserById(currentUser.getId());
        return Result.success(user);
    }

    @Operation(summary = "管理员审核实名认证-通过")
    @PutMapping("/verify/approve/{id}")
    public Result<?> approveVerification(@PathVariable Long id) {
        userService.approveVerification(id);
        return Result.success("实名认证审核通过");
    }

    @Operation(summary = "管理员审核实名认证-拒绝")
    @PutMapping("/verify/reject/{id}")
    public Result<?> rejectVerification(@PathVariable Long id, @RequestParam String reason) {
        userService.rejectVerification(id, reason);
        return Result.success("实名认证审核已拒绝");
    }

    @Operation(summary = "分页查询待审核的实名认证")
    @GetMapping("/verify/pending")
    public Result<?> getPendingVerifications(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> page = userService.getPendingVerifications(currentPage, size);
        return Result.success(page);
    }

    // ==================== 用户偏好设置相关接口 ====================

    @Operation(summary = "更新用户偏好设置")
    @PutMapping("/{id}/preferences")
    public Result<?> updatePreferences(@PathVariable Long id, @RequestBody Map<String, Object> preferences) {
        // 权限检查：只能修改自己的偏好或管理员可修改任何人的偏好
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }

        if (!currentUser.getId().equals(id) && !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权限修改其他用户的偏好设置");
        }

        userService.updatePreferences(id, preferences);
        return Result.success("偏好设置更新成功");
    }

    @Operation(summary = "获取用户偏好设置")
    @GetMapping("/{id}/preferences")
    public Result<?> getPreferences(@PathVariable Long id) {
        return Result.success(userService.getPreferences(id));
    }

    @Operation(summary = "获取当前用户偏好设置")
    @GetMapping("/preferences")
    public Result<?> getCurrentUserPreferences() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        return Result.success(userService.getPreferences(currentUser.getId()));
    }
}
