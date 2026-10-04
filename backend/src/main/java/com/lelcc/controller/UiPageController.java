package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.Result;
import com.lelcc.dto.NodeDTO;
import com.lelcc.dto.NodeVO;
import com.lelcc.dto.PageDTO;
import com.lelcc.dto.PageVO;
import com.lelcc.service.UiPageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 用户端页面与页面节点（组件树）管理。 */
@RestController
@RequestMapping("/admin/site/pages")
public class UiPageController {

    @Resource
    private UiPageService pageService;

    @GetMapping
    public Result<List<PageVO>> list() {
        return Result.success(pageService.list());
    }

    @OperLog(module = "页面搭建", operation = "新增页面")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PageDTO dto) {
        pageService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "页面搭建", operation = "修改页面")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PageDTO dto) {
        pageService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "页面搭建", operation = "删除页面")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pageService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/{id}/nodes")
    public Result<List<NodeVO>> nodes(@PathVariable Long id) {
        return Result.success(pageService.listNodes(id));
    }

    /**
     * 整棵树保存。新增节点用负数临时 id，子节点的 parentId 指向父节点的临时 id，
     * 后端会按父子顺序落库并回填真实 id。
     */
    @OperLog(module = "页面搭建", operation = "保存页面节点")
    @PutMapping("/{id}/nodes")
    public Result<List<NodeVO>> saveNodes(@PathVariable Long id, @RequestBody List<NodeDTO> nodes) {
        return Result.success(pageService.saveNodes(id, nodes));
    }
}
