package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.UploadVO;
import com.lelcc.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（玩家端）。与管理端 UploadController 复用同一套 UploadService 与存储抽象，
 * 差别只在校验与鉴权：/app/** 不在放行名单里，所以这里必须带 MEMBER 令牌（匿名调用 401）。
 *
 * <p>用途是玩家自己产内容：个人主页头像、发帖封面。之所以不是"放行任意人上传"——
 * 上传是会落盘写文件的写操作，匿名放行等于给了任何人往服务器塞文件的能力。</p>
 */
@RestController
@RequestMapping("/app/upload")
public class AppUploadController {

    @Resource
    private UploadService uploadService;

    @PostMapping("/image")
    public Result<UploadVO> image(@RequestParam("file") MultipartFile file) {
        return Result.success(uploadService.uploadImage(file));
    }
}
