package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.TemplateApplyDTO;
import com.lelcc.dto.TemplateApplyVO;
import com.lelcc.dto.TemplateVO;
import com.lelcc.service.TemplateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端「创建社区」：列出可用模板并按模板初始化整站。
 * 挂在 /admin/** 下，走后台 JWT 鉴权；这是建站入口，普通玩家端没有对应接口。
 */
@RestController
@RequestMapping("/admin/site/template")
public class AdminTemplateController {

    @Resource
    private TemplateService templateService;

    /** 可用模板清单（含各自的输入项声明），管理端据此动态渲染表单 */
    @GetMapping("/list")
    public Result<List<TemplateVO>> list() {
        return Result.success(templateService.list());
    }

    /** 应用模板建站：返回建了什么/跳过什么的完整报告 */
    @OperLog(module = "站点配置", operation = "应用社区模板")
    @PostMapping("/apply")
    public Result<TemplateApplyVO> apply(@RequestBody TemplateApplyDTO dto) {
        return Result.success(templateService.apply(dto));
    }
}
