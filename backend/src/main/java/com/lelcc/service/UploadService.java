package com.lelcc.service;

import com.lelcc.dto.StorageConfigVO;
import com.lelcc.dto.UploadVO;
import org.springframework.web.multipart.MultipartFile;

/** 图片上传：校验 + 生成 objectKey + 交给当前 {@link FileStorage} 落盘 */
public interface UploadService {

    UploadVO uploadImage(MultipartFile file);

    /** 当前存储后端的只读配置，供前端解析 objectKey */
    StorageConfigVO storageConfig();
}
