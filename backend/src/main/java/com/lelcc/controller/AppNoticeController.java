package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.NoticeVO;
import com.lelcc.service.NoticeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端公告只读接口。
 * /app/notice/list 已在 WebMvcConfig 里对会员拦截器放行（首页公告栏要匿名可见），
 * 因此这里只能有读接口，写操作以后挂到 /admin/notice/**。
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
}
