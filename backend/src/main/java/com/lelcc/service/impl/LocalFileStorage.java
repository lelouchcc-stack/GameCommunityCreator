package com.lelcc.service.impl;

import com.lelcc.common.BusinessException;
import com.lelcc.config.UploadProperties;
import com.lelcc.service.FileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储：开发/答辩默认实现。文件写入 ${app.upload.local.dir}/<key>，
 * 由 WebMvcConfig 把 /static/** 映射到该目录对外提供访问。
 */
@Service
@ConditionalOnProperty(name = "app.upload.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final UploadProperties properties;

    public LocalFileStorage(UploadProperties properties) {
        this.properties = properties;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new BusinessException(500, "图片保存失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new BusinessException(500, "图片删除失败：" + e.getMessage());
        }
    }

    @Override
    public String urlOf(String key) {
        return FileStorage.joinUrl(publicBaseUrl(), key);
    }

    @Override
    public String publicBaseUrl() {
        return properties.getLocal().getPublicBaseUrl();
    }

    @Override
    public String provider() {
        return "local";
    }

    /** key 由服务端生成，但仍确保解析结果落在存储根目录内，避免路径穿越 */
    private Path resolve(String key) {
        Path root = Paths.get(properties.getLocal().getDir()).toAbsolutePath().normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(400, "非法的存储路径");
        }
        return target;
    }
}
