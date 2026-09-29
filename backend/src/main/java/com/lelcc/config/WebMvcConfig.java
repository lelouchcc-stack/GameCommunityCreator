package com.lelcc.config;

import com.lelcc.common.UserType;
import com.lelcc.interceptor.AuthInterceptor;
import com.lelcc.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtUtils jwtUtils;

    /**
     * 约定：所有业务接口必须挂在 /admin 或 /app 下才会被鉴权，
     * 新增 Controller 时不要使用这两个前缀之外的路径。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtils, UserType.ADMIN))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");

        registry.addInterceptor(new AuthInterceptor(jwtUtils, UserType.MEMBER))
                .addPathPatterns("/app/**")
                .excludePathPatterns("/app/login", "/app/register");
    }
}
