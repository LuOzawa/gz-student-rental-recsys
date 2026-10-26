package org.example.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                // ==================== 安全响应头配置 ====================

                // 防止点击劫持攻击
                response.setHeader("X-Frame-Options", "SAMEORIGIN");

                // 内容安全策略
                response.setHeader("Content-Security-Policy", "frame-ancestors 'self'");

                // 防止 MIME 类型嗅探
                response.setHeader("X-Content-Type-Options", "nosniff");

                // 启用浏览器内置的 XSS 过滤器
                response.setHeader("X-XSS-Protection", "1; mode=block");

                // Referrer 策略
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

                // 权限策略（限制浏览器功能）
                response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

                return true;
            }
        });
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 原实现为 allowedOrigins("*")，任意站点均可跨域读取响应。
        // 改为可配置白名单，默认仅本地开发端口。
        String origins = System.getProperty("app.cors.allowed-origins",
                System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS",
                        "http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000"));
        registry.addMapping("/**")
                .allowedOriginPatterns(java.util.Arrays.stream(origins.split(","))
                        .map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录的绝对路径
        String projectPath = System.getProperty("user.dir");

        // 配置图片资源处理器
        registry.addResourceHandler("/img/**")
                .addResourceLocations("file:" + projectPath + "/files/img/");

        // 配置合同文件资源处理器
        registry.addResourceHandler("/contract/**")
                .addResourceLocations("file:" + projectPath + "/files/contract/");
    }
} 