package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.SiteBundleVO;
import com.lelcc.service.SiteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端站点配置只读接口。
 * 该路径已在 WebMvcConfig 里对会员拦截器放行——匿名用户要能看到站点外观，
 * 因此这里只能有读接口，任何写操作一律挂到 /admin/site/**。
 */
@RestController
@RequestMapping("/app/site")
public class AppSiteController {

    @Resource
    private SiteService siteService;

    @GetMapping("/config")
    public Result<SiteBundleVO> config() {
        return Result.success(siteService.userBundle());
    }
}
