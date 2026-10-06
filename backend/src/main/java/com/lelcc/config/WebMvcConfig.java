package com.lelcc.config;

import com.lelcc.common.UserType;
import com.lelcc.interceptor.AdminPermInterceptor;
import com.lelcc.interceptor.AuthInterceptor;
import com.lelcc.mapper.SysMenuMapper;
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
    private SysMenuMapper sysMenuMapper;

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

        // 后台写操作再查一次权限标识（sys_menu.perms）：只挡非 GET 的请求，
        // 否则只有部分角色的账号会因为读不到站点配置而进不了后台。细则见 AdminPermInterceptor。
        registry.addInterceptor(new AdminPermInterceptor(sysMenuMapper))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");

        // 以下 /app 路径匿名可读，供未登录用户浏览社区：站点外观配置（站点名/游戏名/Logo/主题/导航）、
        // 板块列表与板块详情、轮播图、公告列表与详情、内容分类（标签）、热门帖子与帖子列表/详情。
        // 不放行的话未登录用户打开用户端会先撞 401。
        // 注意：放行路径下只允许存在读接口——写接口一律用多段路径（如 /app/post/{id}/like），
        // 才不会被 /app/post/* 这种单段通配误放行。
        // 例外 /app/member/**：个人主页是自己的数据，不在此列，必须登录。
        registry.addInterceptor(new AuthInterceptor(jwtUtils, UserType.MEMBER))
                .addPathPatterns("/app/**")
                .excludePathPatterns("/app/login", "/app/register", "/app/site/config",
                        "/app/banner/list", "/app/notice/*", "/app/tag/*",
                        "/app/board/*", "/app/post/*", "/app/comment/page");
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
