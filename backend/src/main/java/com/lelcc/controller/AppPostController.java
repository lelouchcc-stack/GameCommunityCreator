package com.lelcc.controller;

import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.AppPostDetailVO;
import com.lelcc.dto.AppPostItemVO;
import com.lelcc.dto.HotPostVO;
import com.lelcc.dto.InteractionVO;
import com.lelcc.dto.PostCreateDTO;
import com.lelcc.dto.PostCreateVO;
import com.lelcc.service.PostInteractionService;
import com.lelcc.service.PostService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端帖子只读接口（热门榜 + 列表检索 + 详情）。
 * /app/post/*（单段路径）已在 WebMvcConfig 里对会员拦截器放行，匿名可浏览；
 * 写接口（发帖、点赞、评论等）必须放在 /app/post/** 的多段路径下，避免被误放行。
 */
@RestController
@RequestMapping("/app/post")
public class AppPostController {

    @Resource
    private PostService postService;

    @Resource
    private PostInteractionService postInteractionService;

    /** 热门帖子：置顶优先，再按点赞、浏览降序，最多 limit 条（默认 10） */
    @GetMapping("/hot")
    public Result<List<HotPostVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(postService.listHot(limit));
    }

    /** 帖子检索：按板块/版本/阶段/标签/关键字筛选，sort = latest | hot */
    @GetMapping("/page")
    public Result<PageResult<AppPostItemVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize,
                                                  @RequestParam(required = false) Long boardId,
                                                  @RequestParam(required = false) Long versionId,
                                                  @RequestParam(required = false) Long stageId,
                                                  @RequestParam(required = false) Long tagId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "latest") String sort) {
        return Result.success(postService.pagePublished(
                boardId, versionId, stageId, tagId, keyword, sort, pageNum, pageSize));
    }

    /** 帖子详情：累加浏览量 */
    @GetMapping("/{postId}")
    public Result<AppPostDetailVO> detail(@PathVariable Long postId) {
        return Result.success(postService.detail(postId));
    }

    /* -------- 以下为多段路径的写接口，不会被 /app/post/* 放行，需登录 -------- */

    /**
     * 发帖。注意路径是 /app/post（**没有第二段**），不在 "/app/post/*" 的放行范围内，
     * 因此匿名调用会先被会员拦截器挡下返回 401——这是有意的：写操作绝不能被匿名放行。
     */
    @PostMapping
    public Result<PostCreateVO> create(@Valid @RequestBody PostCreateDTO dto) {
        return Result.success(postService.create(dto));
    }

    /** 当前登录玩家对该帖的互动状态与计数 */
    @GetMapping("/{postId}/interaction")
    public Result<InteractionVO> interaction(@PathVariable Long postId) {
        return Result.success(postInteractionService.get(postId));
    }

    /** 点赞 / 取消点赞 */
    @PostMapping("/{postId}/like")
    public Result<InteractionVO> like(@PathVariable Long postId) {
        return Result.success(postInteractionService.toggleLike(postId));
    }

    /** 收藏 / 取消收藏 */
    @PostMapping("/{postId}/favorite")
    public Result<InteractionVO> favorite(@PathVariable Long postId) {
        return Result.success(postInteractionService.toggleFavorite(postId));
    }
}
