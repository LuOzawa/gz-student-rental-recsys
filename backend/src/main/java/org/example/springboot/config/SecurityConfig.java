package org.example.springboot.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security安全配置类
 * 用于配置系统的安全认证、授权等功能
 * 包括：
 * - 密码加密方式
 * - 安全过滤器链
 * - 请求授权规则
 * - CSRF防护配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 开启后 @PreAuthorize 才会生效
public class SecurityConfig {
    @Resource
    private UserDetailsService userDetailsService;

    /**
     * 密码编码器配置
     * 使用BCrypt加密算法对密码进行加密
     * BCrypt是一种安全的密码哈希函数，自动包含随机盐值
     *
     * @return PasswordEncoder BCrypt密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);  // 设置加密强度
    }

    /**
     * 安全过滤器链配置
     * 配置系统的安全规则，包括：
     * 1. 请求授权规则
     *    - 所有请求暂时允许所有访问（开发环境配置）
     * 2. CSRF防护
     *    - 当前已禁用（仅适用于开发环境）
     *
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain 配置好的安全过滤器链
     * @throws Exception 配置过程中可能发生的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers.frameOptions(frameOptionsConfig -> frameOptionsConfig.sameOrigin()))
                // 认证与授权由 JwtInterceptor + @PreAuthorize 完成，
                // 这里放行是为了让请求能进入拦截器链；真正的拦截在 WebConfig 中配置。
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // 统一返回 JSON，避免前端解析纯文本失败
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":401,\"message\":\"未登录或登录状态已失效\",\"data\":null}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":403,\"message\":\"无权访问该接口\",\"data\":null}");
                        })
                )
                .csrf(csrf -> csrf.disable());  // 无状态 JWT API，CSRF 可关闭
        return http.build();
    }
}