package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.LogicNodeVO;
import com.lelcc.service.UiLogicNodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 原子库：逻辑图上能用的节点类型，由代码/种子提供，管理端只读。 */
@RestController
@RequestMapping("/admin/site/logic-nodes")
public class UiLogicNodeController {

    @Resource
    private UiLogicNodeService logicNodeService;

    @GetMapping
    public Result<List<LogicNodeVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(logicNodeService.list(status));
    }
}
