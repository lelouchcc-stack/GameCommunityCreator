package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.TagVO;
import com.lelcc.service.TagService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端内容分类（标签）只读接口。
 * /app/tag/* 已在 WebMvcConfig 里对会员拦截器放行——攻略/资料页要匿名可见，
 * 因此这里只能有读接口，写操作一律挂到 /admin/tag/**。
 */
@RestController
@RequestMapping("/app/tag")
public class AppTagController {

    @Resource
    private TagService tagService;

    /** 启用的内容分类：全局标签 + 指定板块的标签（boardId 可空） */
    @GetMapping("/list")
    public Result<List<TagVO>> list(@RequestParam(required = false) Long boardId) {
        return Result.success(tagService.listOpen(boardId));
    }
}
