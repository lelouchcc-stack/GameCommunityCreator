package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.StorageConfigVO;
import com.lelcc.dto.UploadVO;
import com.lelcc.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（管理端）。玩家侧的上传接口将随头像/帖子封面模块挂到 /app/upload，复用同一套存储抽象。
 * 未加 @OperLog：multipart 参数写进操作日志没有价值，反而会把文件内容序列化进去。
 */
@RestController
@RequestMapping("/admin/upload")
public class UploadController {

    @Resource
    private UploadService uploadService;

    @PostMapping("/image")
    public Result<UploadVO> image(@RequestParam("file") MultipartFile file) {
        return Result.success(uploadService.uploadImage(file));
    }

    @GetMapping("/config")
    public Result<StorageConfigVO> config() {
        return Result.success(uploadService.storageConfig());
    }
}
