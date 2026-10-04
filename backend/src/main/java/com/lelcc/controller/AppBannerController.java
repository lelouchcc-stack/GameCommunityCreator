package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.BannerSlideVO;
import com.lelcc.service.BannerSlideService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端轮播图只读接口。
 * /app/banner/list 已在 WebMvcConfig 里对会员拦截器放行（首页轮播要匿名可见），
 * 因此这里只能有读接口，写操作一律挂到 /admin/banner/**。
 */
@RestController
@RequestMapping("/app/banner")
public class AppBannerController {

    @Resource
    private BannerSlideService bannerSlideService;

    /** 展示中的轮播图，按展示顺序返回；取几张由页面节点上的 limit 决定 */
    @GetMapping("/list")
    public Result<List<BannerSlideVO>> list() {
        return Result.success(bannerSlideService.listVisible());
    }
}
