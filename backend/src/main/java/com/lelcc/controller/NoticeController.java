package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.NoticeDTO;
import com.lelcc.dto.NoticeVO;
import com.lelcc.service.NoticeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台公告管理。挂在 /admin/notice/** 下，走后台 JWT + AdminPermInterceptor 的写权限校验
 * （URL 前缀 → notice:list，见该拦截器的 RULES）。
 *
 * 用户端只读接口在 AppNoticeController（/app/notice/**），两边共用 NoticeService：
 * 编辑保存后用户端刷新即可见，不需要额外的发布动作。
 */
@RestController
@RequestMapping("/admin/notice")
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    /** 公告分页：含草稿，可按状态/类型/关键字筛选 */
    @GetMapping("/page")
    public Result<PageResult<NoticeVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Integer type,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(noticeService.page(status, type, keyword, pageNum, pageSize));
    }

    /** 公告详情（含草稿）：编辑弹窗回填用 */
    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.success(noticeService.adminDetail(id));
    }

    @OperLog(module = "公告管理", operation = "新增公告")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody NoticeDTO dto) {
        noticeService.create(dto);
        return Result.success(null);
    }

    @OperLog(module = "公告管理", operation = "修改公告")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeDTO dto) {
        noticeService.update(id, dto);
        return Result.success(null);
    }

    @OperLog(module = "公告管理", operation = "发布/下架公告")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        noticeService.changeStatus(id, status);
        return Result.success(null);
    }

    @OperLog(module = "公告管理", operation = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success(null);
    }
}
