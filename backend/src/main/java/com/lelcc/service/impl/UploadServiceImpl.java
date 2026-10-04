package com.lelcc.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.lelcc.common.BusinessException;
import com.lelcc.dto.StorageConfigVO;
import com.lelcc.dto.UploadVO;
import com.lelcc.service.FileStorage;
import com.lelcc.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@Service
public class UploadServiceImpl implements UploadService {

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    /**
     * 允许的图片扩展名。刻意不含 svg：SVG 可内嵌脚本，与用户内容同源渲染时有 XSS 风险。
     */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    /** objectKey 的固定前缀，本地 provider 的 /static/** 与云桶内目录共用同一命名空间 */
    private static final String KEY_PREFIX = "upload";

    @Resource
    private FileStorage fileStorage;

    @Override
    public UploadVO uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "图片大小不能超过 5MB");
        }
        String ext = resolveExtension(file);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(500, "读取上传文件失败：" + e.getMessage());
        }
        String key = newKey(ext);
        fileStorage.put(key, content, contentType(file, ext));
        return new UploadVO(key, fileStorage.urlOf(key), file.getOriginalFilename(), file.getSize());
    }

    @Override
    public StorageConfigVO storageConfig() {
        return new StorageConfigVO(fileStorage.provider(), fileStorage.publicBaseUrl());
    }

    /** 扩展名白名单 + content-type 双重校验，两者都过才放行 */
    private String resolveExtension(MultipartFile file) {
        String ext = FileUtil.extName(file.getOriginalFilename());
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(400, "仅支持 jpg/jpeg/png/gif/webp/bmp 格式的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(400, "上传的文件不是图片");
        }
        return ext;
    }

    /** key 用日期目录 + uuid，不含原始文件名，规避路径穿越与中文/特殊字符问题 */
    private String newKey(String ext) {
        return KEY_PREFIX + "/" + LocalDate.now().format(DATE_DIR) + "/" + IdUtil.fastSimpleUUID() + "." + ext;
    }

    private String contentType(MultipartFile file, String ext) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? "image/" + ext : contentType;
    }
}
