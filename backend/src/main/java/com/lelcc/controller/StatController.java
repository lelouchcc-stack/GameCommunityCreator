package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.StatOverviewVO;
import com.lelcc.service.StatService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台数据统计。挂在 /admin/stat/** 下，走后台 JWT。
 * 目前只有读接口（AdminPermInterceptor 只拦写方法），URL 前缀已在它的 RULES 里登记为 stat:view，
 * 将来加"导出"之类的写接口会自动受控。
 */
@RestController
@RequestMapping("/admin/stat")
public class StatController {

    @Resource
    private StatService statService;

    /** 总览：days 为统计天数（趋势覆盖最近这么多天，收敛到 7~90，默认 30） */
    @GetMapping("/overview")
    public Result<StatOverviewVO> overview(@RequestParam(required = false) Integer days) {
        return Result.success(statService.overview(days));
    }
}
