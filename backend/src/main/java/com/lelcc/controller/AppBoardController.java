package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.BoardVO;
import com.lelcc.service.BoardService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端板块只读接口。
 * /app/board/list 已在 WebMvcConfig 里对会员拦截器放行——首页板块导航要匿名可见，
 * 因此这里只能有读接口，任何写操作一律挂到 /admin/board/**。
 */
@RestController
@RequestMapping("/app/board")
public class AppBoardController {

    @Resource
    private BoardService boardService;

    /** 开放板块列表（板块导航用），按展示顺序返回 */
    @GetMapping("/list")
    public Result<List<BoardVO>> list() {
        return Result.success(boardService.listOpen());
    }
}
