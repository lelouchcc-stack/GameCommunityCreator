package com.lelcc.controller;

import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.NoticeVO;
import com.lelcc.service.NoticeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端公告只读接口。
 * /app/notice/* 已在 WebMvcConfig 里对会员拦截器放行（公告要匿名可见），
 * 因此这里只能有读接口，写操作以后挂到 /admin/notice/**（多段路径不会被误放行）。
 */
@RestController
@RequestMapping("/app/notice")
public class AppNoticeController {

    @Resource
    private NoticeService noticeService;

    /** 已发布公告，最多 limit 条（默认 5） */
    @GetMapping("/list")
    public Result<List<NoticeVO>> list(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(noticeService.listPublished(limit));
    }

    /** 公告列表页：已发布公告分页 */
    @GetMapping("/page")
    public Result<PageResult<NoticeVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(noticeService.pagePublished(pageNum, pageSize));
    }

    /** 公告详情：草稿对外不可见 */
    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.success(noticeService.detail(id));
    }
}
