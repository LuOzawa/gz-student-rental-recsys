package org.example.springboot.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.entity.User;
import org.example.springboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;


@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtInterceptor.class);

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StringUtils.isBlank(token)) {
            // 公开浏览页面（房源列表/详情、资讯、论坛列表）允许匿名只读
            if (isPublicGet(request)) {
                return true;
            }
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录，请先登录");
        }

        User user;
        try {
            String userId = JWT.decode(token).getAudience().get(0);
            user = userService.getUserById(Long.valueOf(userId));
        } catch (Exception e) {
            // 不要把 token 明文写进日志
            LOGGER.warn("token 解析失败: {}", e.getClass().getSimpleName());
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "登录状态已失效，请重新登录");
        }
        if (user == null) {
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "用户不存在");
        }
        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "登录状态已失效，请重新登录");
        }

        // 关键：把用户身份与角色写入 Spring Security 上下文，
        // @PreAuthorize("hasRole('ADMIN')") 才能取到权限。
        // roleCode 取值 ADMIN / TENANT / LANDLORD，需加 ROLE_ 前缀。
        String roleCode = StringUtils.isBlank(user.getRoleCode())
                ? "TENANT"
                : user.getRoleCode().trim().toUpperCase();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                String.valueOf(user.getId()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + roleCode)));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LOGGER.debug("JWT 校验通过，userId={}, role={}", user.getId(), roleCode);
        return true;
    }

    /**
     * 匿名可读的 GET 接口。只放行公开浏览页面实际用到的只读接口，
     * 写操作与房东/管理端接口仍然要求登录。
     */
    private boolean isPublicGet(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();

        if ("/api/houses/page".equals(path)
                || "/api/houses/map-search".equals(path)
                || "/api/houses/top-rated".equals(path)
                || "/api/recommendations/hot".equals(path)) {
            return true;
        }

        // 房源详情：/api/houses/{数字id} 及其附近的配套设施
        if (path.startsWith("/api/houses/")) {
            String rest = path.substring("/api/houses/".length());
            if (isNumeric(rest)) {
                return true;
            }
            int slash = rest.indexOf('/');
            if (slash > 0 && isNumeric(rest.substring(0, slash))
                    && "/nearby-facilities".equals(rest.substring(slash))) {
                return true;
            }
            return false;
        }

        return path.startsWith("/api/house-types")
                || path.startsWith("/api/house-comments/house/")
                || path.startsWith("/api/forum-categories")
                || path.startsWith("/api/forum-posts")
                || path.startsWith("/api/announcements");
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean writeError(HttpServletResponse response, int status, int code, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
