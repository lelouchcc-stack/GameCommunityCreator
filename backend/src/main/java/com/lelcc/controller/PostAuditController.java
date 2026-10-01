package com.lelcc.controller;

import com.lelcc.annotation.OperLog;
import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.PostAuditDTO;
import com.lelcc.dto.PostAuditVO;
import com.lelcc.service.PostAuditService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/post")
public class PostAuditController {

    @Resource
    private PostAuditService postAuditService;

    @GetMapping("/page")
    public Result<PageResult<PostAuditVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) Long boardId,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(postAuditService.page(status, boardId, keyword, pageNum, pageSize));
    }

    @OperLog(module = "帖子审核", operation = "审核帖子")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody PostAuditDTO dto) {
        postAuditService.audit(id, dto.status(), dto.reason());
        return Result.success(null);
    }
}
