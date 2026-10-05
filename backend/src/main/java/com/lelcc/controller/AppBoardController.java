package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.AppBoardDetailVO;
import com.lelcc.dto.BoardVO;
import com.lelcc.service.AppBoardService;
import com.lelcc.service.BoardService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端板块只读接口。
 * /app/board/** 已在 WebMvcConfig 里对会员拦截器放行——首页板块导航与板块详情要匿名可见，
 * 因此这里只能有读接口，任何写操作一律挂到 /admin/board/**。
 */
@RestController
@RequestMapping("/app/board")
public class AppBoardController {

    @Resource
    private BoardService boardService;

    @Resource
    private AppBoardService appBoardService;

    /** 开放板块列表（板块导航用），按展示顺序返回 */
    @GetMapping("/list")
    public Result<List<BoardVO>> list() {
        return Result.success(boardService.listOpen());
    }

    /** 板块详情：板块本体 + 启用中的游戏版本与进度阶段 */
    @GetMapping("/{boardId}")
    public Result<AppBoardDetailVO> detail(@PathVariable Long boardId) {
        return Result.success(appBoardService.detail(boardId));
    }
}
