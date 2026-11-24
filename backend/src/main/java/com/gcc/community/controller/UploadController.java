package com.gcc.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gcc.community.common.ApiResponse;
import com.gcc.community.dto.UploadView;
import com.gcc.community.service.UploadService;

/** 图片上传和读取的接口。 */
@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    // 上传图片，返回图片地址
    @PostMapping("/uploads/images")
    public ResponseEntity<ApiResponse<UploadView>> upload(
            @RequestParam("file") MultipartFile file) {
        UploadView view = uploadService.storeImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(view));
    }

    // 公开读取图片，浏览器可以直接打开
    @GetMapping("/public/uploads/{name}")
    public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable String name) {
        UploadService.LoadedImage image = uploadService.loadImage(name);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .contentLength(image.size())
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + image.filename() + "\"")
                .body(image.resource());
    }
}
