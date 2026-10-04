package com.lelcc.config;

import com.lelcc.common.UserType;
import com.lelcc.interceptor.AuthInterceptor;
import com.lelcc.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UploadProperties uploadProperties;

    /**
     * 约定：所有业务接口必须挂在 /admin 或 /app 下才会被鉴权，
     * 新增 Controller 时不要使用这两个前缀之外的路径。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtils, UserType.ADMIN))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");

        // /app/site/config 是匿名可读的站点外观配置（站点名/Logo/主题/首页模块），
        // /app/board/list 是匿名可读的开放板块列表（首页板块导航用）。
        // 不放行的话未登录用户打开用户端会先撞 401。放行路径下只允许存在读接口。
        registry.addInterceptor(new AuthInterceptor(jwtUtils, UserType.MEMBER))
                .addPathPatterns("/app/**")
                .excludePathPatterns("/app/login", "/app/register", "/app/site/config", "/app/board/list");
    }

    /**
     * /static/** 指向本地图片存储目录（local provider 用），并保留 classpath:/static/ 兜底，
     * 这样打包进 jar 的静态资源也照常可用。provider=s3 时本地资源仍在，只是上传不落这里。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadProperties.getLocal().getDir())
                .toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/static/**")
                .addResourceLocations(location, "classpath:/static/");
    }
}
