package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;

import org.example.springboot.entity.User;
import org.example.springboot.DTO.UserPasswordUpdateDTO;
import org.example.springboot.enumClass.AccountStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
    

    
    @Value("${user.defaultPassword}")
    private String DEFAULT_PWD;

    @Resource
    private PasswordEncoder bCryptPasswordEncoder;

    public User getByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        return user;
    }

    public User login(User user) {
        User dbUser = getByUsername(user.getUsername());
        // 用户存在性检查已经在 getByUsername 中处理

        // 检查账号状态（仅检查是否因违规被禁用）
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new ServiceException("账号已被禁用，请联系管理员");
        }

        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }

        String token = JwtTokenUtils.genToken(String.valueOf(dbUser.getId()), dbUser.getPassword());

        dbUser.setToken(token);
        return dbUser;
    }

    public List<User> getUserByRole(String roleCode) {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, roleCode)
        );
        if (users.isEmpty()) {
            throw new ServiceException("未找到该角色的用户");
        }
        return users;
    }

    public void createUser(User user) {
        // 检查用户名是否存在
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername())
            ) != null) {
            throw new ServiceException("用户名已存在");
        }

        // 检查邮箱是否被使用
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, user.getEmail())
            ) != null) {
            throw new ServiceException("邮箱已被使用");
        }


        user.setPassword(StringUtils.isNotBlank(user.getPassword()) ? user.getPassword() : DEFAULT_PWD);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));

        // 用户注册后直接可用，无需审核
        user.setAuditStatus(1);  // 审核通过
        user.setStatus(1);       // 启用状态
        user.setVerifyStatus(0); // 未实名认证

        // 房东初始信用分设置为100
        if ("LANDLORD".equals(user.getRoleCode())) {
            user.setCreditScore(100);
        }

        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("用户创建失败");
        }
    }

    public void updateUser(Long id, User user) {
        // 检查用户是否存在
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new ServiceException("要更新的用户不存在");
        }

        // 如果用户已通过实名认证，禁止任何人（包括管理员）修改姓名和身份证号
        if (existingUser.getVerifyStatus() != null && existingUser.getVerifyStatus() == 2) {
            // 检查是否尝试修改姓名
            if (user.getName() != null && !user.getName().equals(existingUser.getName())) {
                throw new ServiceException("该用户已通过实名认证，无法修改姓名");
            }
            // 检查是否尝试修改身份证号
            if (user.getIdCard() != null && !user.getIdCard().equals(existingUser.getIdCard())) {
                throw new ServiceException("该用户已通过实名认证，无法修改身份证号码");
            }
            // 清除这些字段，防止被意外修改
            user.setName(null);
            user.setIdCard(null);
        }

        // 检查新用户名是否与其他用户重复
        if (user.getUsername() != null) {
            User checkUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername())
            );
            if (checkUser != null && !checkUser.getId().equals(id)) {
                throw new ServiceException("新用户名已被使用");
            }
        }

        user.setId(id);
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("用户更新失败");
        }
    }

    public User getByUsername(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public void deleteBatch(List<Integer> ids) {
        if (userMapper.deleteByIds(ids) <= 0) {
            throw new ServiceException("批量删除失败");
        }
    }

    public List<User> getUserList() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        if (users.isEmpty()) {
            throw new ServiceException("未找到任何用户");
        }
        return users;
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public Page<User> getUsersByPage(String username,  String name, String roleCode, Integer auditStatus, Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }

        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(User::getName, name);
        }
        if (StringUtils.isNotBlank(roleCode)) {
            queryWrapper.eq(User::getRoleCode, roleCode);
        }
        if (auditStatus != null) {
            queryWrapper.eq(User::getAuditStatus, auditStatus);
        }

        return userMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    public void updatePassword(Long id, UserPasswordUpdateDTO update) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 验证旧密码
        if (!bCryptPasswordEncoder.matches(update.getOldPassword(), user.getPassword())) {
            throw new ServiceException("原密码错误");
        }
        
        // 更新新密码
        user.setPassword(bCryptPasswordEncoder.encode(update.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码修改失败");
        }
    }

    public void forgetPassword(String email, String newPassword) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
        );
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }
    }

    public void deleteUserById(Long id) {
        if (userMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除失败");
        }
    }

    /**
     * 审核通过用户/房东
     */
    public void approveUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (user.getAuditStatus() != 0) {
            throw new ServiceException("该用户已审核");
        }

        user.setAuditStatus(1); // 审核通过
        user.setAuditReason(null);
        user.setStatus(1); // 状态设置为正常
        userMapper.updateById(user);
    }

    /**
     * 审核拒绝用户/房东
     */
    public void rejectUser(Long id, String reason) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (user.getAuditStatus() != 0) {
            throw new ServiceException("该用户已审核");
        }

        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("拒绝原因不能为空");
        }

        user.setAuditStatus(2); // 审核拒绝
        user.setAuditReason(reason);
        user.setStatus(0); // 状态设置为禁用
        userMapper.updateById(user);
    }

    // ==================== 实名认证相关方法 ====================

    /**
     * 提交实名认证
     */
    public void submitVerification(Long userId, String name, String idCard) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 验证姓名
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("真实姓名不能为空");
        }

        // 验证身份证号格式（18位）
        if (StringUtils.isBlank(idCard) || !idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$")) {
            throw new ServiceException("身份证号格式不正确");
        }

        // 检查身份证号是否已被其他用户使用
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getIdCard, idCard)
                    .ne(User::getId, userId);
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new ServiceException("该身份证号已被使用");
        }

        // 检查当前认证状态
        if (user.getVerifyStatus() != null && user.getVerifyStatus() == 1) {
            throw new ServiceException("您的实名认证正在审核中，请耐心等待");
        }

        if (user.getVerifyStatus() != null && user.getVerifyStatus() == 2) {
            throw new ServiceException("您已完成实名认证");
        }

        // 更新用户信息
        user.setName(name);
        user.setIdCard(idCard);
        user.setVerifyStatus(1); // 待审核
        user.setVerifyRejectReason(null); // 清空之前的拒绝原因
        userMapper.updateById(user);
    }

    /**
     * 管理员审核实名认证-通过
     */
    public void approveVerification(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (user.getVerifyStatus() == null || user.getVerifyStatus() != 1) {
            throw new ServiceException("该用户没有待审核的实名认证");
        }

        user.setVerifyStatus(2); // 已认证
        user.setVerifyRejectReason(null);
        userMapper.updateById(user);
    }

    /**
     * 管理员审核实名认证-拒绝
     */
    public void rejectVerification(Long userId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (user.getVerifyStatus() == null || user.getVerifyStatus() != 1) {
            throw new ServiceException("该用户没有待审核的实名认证");
        }

        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("拒绝原因不能为空");
        }

        user.setVerifyStatus(3); // 审核拒绝
        user.setVerifyRejectReason(reason);
        userMapper.updateById(user);
    }

    /**
     * 分页查询待审核的实名认证
     */
    public Page<User> getPendingVerifications(Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getVerifyStatus, 1) // 待审核
                    .orderByAsc(User::getCreateTime);

        return userMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }

    /**
     * 检查用户是否已实名认证
     */
    public boolean isUserVerified(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getVerifyStatus() != null && user.getVerifyStatus() == 2;
    }

    // ==================== 用户偏好设置相关方法 ====================

    /**
     * 更新用户偏好设置
     */
    public void updatePreferences(Long userId, Map<String, Object> preferences) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 更新校区
        if (preferences.containsKey("campus")) {
            String campus = (String) preferences.get("campus");
            if (campus != null && !campus.isEmpty()) {
                user.setCampus(campus);
            }
        }

        // 更新专业
        if (preferences.containsKey("major")) {
            String major = (String) preferences.get("major");
            if (major != null && !major.isEmpty()) {
                user.setMajor(major);
            }
        }

        // 更新入学年份
        if (preferences.containsKey("enrollmentYear")) {
            Object yearObj = preferences.get("enrollmentYear");
            if (yearObj != null) {
                Integer year = null;
                if (yearObj instanceof Integer) {
                    year = (Integer) yearObj;
                } else if (yearObj instanceof String) {
                    try {
                        year = Integer.parseInt((String) yearObj);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (year != null && year > 2000 && year <= 2030) {
                    user.setEnrollmentYear(year);
                }
            }
        }

        // 更新租房预算
        if (preferences.containsKey("rentBudget")) {
            Object budgetObj = preferences.get("rentBudget");
            if (budgetObj != null) {
                BigDecimal budget = null;
                if (budgetObj instanceof BigDecimal) {
                    budget = (BigDecimal) budgetObj;
                } else if (budgetObj instanceof Integer) {
                    budget = new BigDecimal((Integer) budgetObj);
                } else if (budgetObj instanceof Double) {
                    budget = new BigDecimal((Double) budgetObj);
                } else if (budgetObj instanceof String) {
                    try {
                        budget = new BigDecimal((String) budgetObj);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
                    user.setRentBudget(budget);
                }
            }
        }

        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("偏好设置更新失败");
        }
    }

    /**
     * 获取用户偏好设置
     */
    public Map<String, Object> getPreferences(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("campus", user.getCampus());
        preferences.put("major", user.getMajor());
        preferences.put("enrollmentYear", user.getEnrollmentYear());
        preferences.put("rentBudget", user.getRentBudget());

        return preferences;
    }
}
