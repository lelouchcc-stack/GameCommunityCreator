package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.HotPostVO;
import com.lelcc.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端帖子只读接口（目前只有首页热门榜）。
 * /app/post/hot 已在 WebMvcConfig 里对会员拦截器放行（首页要匿名可见），
 * 帖子详情等需要登录态的接口以后另起路径。
 */
@RestController
@RequestMapping("/app/post")
public class AppPostController {

    @Resource
    private PostService postService;

    /** 热门帖子：置顶优先，再按点赞、浏览降序，最多 limit 条（默认 10） */
    @GetMapping("/hot")
    public Result<List<HotPostVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(postService.listHot(limit));
    }
}
