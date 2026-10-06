package com.lelcc.controller;

import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.CommentDTO;
import com.lelcc.dto.CommentVO;
import com.lelcc.service.CommentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口。
 * GET /app/comment/page 在 WebMvcConfig 里放行（匿名可看评论）；
 * POST /app/comment 是需要登录的写接口，不能放行。
 */
@RestController
@RequestMapping("/app/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    @GetMapping("/page")
    public Result<PageResult<CommentVO>> page(@RequestParam Long postId,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(commentService.page(postId, pageNum, pageSize));
    }

    @PostMapping
    public Result<CommentVO> create(@Valid @RequestBody CommentDTO dto) {
        return Result.success(commentService.create(dto));
    }
}
