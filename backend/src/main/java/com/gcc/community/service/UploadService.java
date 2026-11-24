package com.gcc.community.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gcc.community.common.ApiException;
import com.gcc.community.dto.UploadView;

/** 图片上传和读取的业务。 */
@Service
public class UploadService {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    // 允许的图片类型对应的后缀
    private static final Map<String, String> EXT_BY_CONTENT_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif");

    // 后缀对应的 Content-Type
    private static final Map<String, String> CONTENT_TYPE_BY_EXT = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "gif", "image/gif");

    private static final Pattern STORED_NAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{1,64}\\.(png|jpg|jpeg|webp|gif)$");

    private final Path rootDir;

    public UploadService(@Value("${app.upload.dir}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // 保存图片
    public UploadView storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("上传文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw ApiException.badRequest("图片大小不能超过 5MB");
        }
        String contentType = resolveContentType(file);
        String extension = EXT_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw ApiException.badRequest("不支持的图片类型，仅支持 png / jpeg / webp / gif");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT)
                + extension;
        Path target = rootDir.resolve(storedName).normalize();
        if (!target.startsWith(rootDir)) {
            // 理论不可达（文件名由服务端生成），保留防御
            throw ApiException.badRequest("文件名不合法");
        }
        try {
            Files.createDirectories(rootDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("保存上传图片失败", ex);
        }
        String originalName = safeOriginalName(file.getOriginalFilename());
        return new UploadView("/api/public/uploads/" + storedName, originalName,
                file.getSize(), contentType);
    }

    // 读取图片
    public LoadedImage loadImage(String storedName) {
        if (storedName == null || !STORED_NAME_PATTERN.matcher(storedName).matches()) {
            throw ApiException.badRequest("文件名不合法");
        }
        Path target = rootDir.resolve(storedName).normalize();
        if (!target.startsWith(rootDir) || !Files.isRegularFile(target)) {
            throw ApiException.notFound("图片不存在");
        }
        String extension = storedName.substring(storedName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPE_BY_EXT.getOrDefault(extension, "application/octet-stream");
        long size;
        try {
            size = Files.size(target);
        } catch (IOException ex) {
            throw new IllegalStateException("读取上传图片失败", ex);
        }
        Resource resource = new FileSystemResource(target);
        return new LoadedImage(resource, MediaType.parseMediaType(contentType), size, storedName);
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT).strip();
        }
        // 部分客户端不传 Content-Type：按扩展名兜底推导
        String name = safeOriginalName(file.getOriginalFilename());
        int index = name.lastIndexOf('.');
        if (index >= 0) {
            String ext = name.substring(index + 1).toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : CONTENT_TYPE_BY_EXT.entrySet()) {
                String mappedExt = EXT_BY_CONTENT_TYPE.get(entry.getValue());
                if (ext.equals(entry.getKey()) && mappedExt != null) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    // 原始文件名只用来展示
    private static String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        String normalized = originalName.replace('\\', '/');
        String base = normalized.substring(normalized.lastIndexOf('/') + 1).strip();
        return base.isEmpty() ? "image" : base;
    }

    // 读取结果，响应头在 controller 里写
    public record LoadedImage(Resource resource, MediaType contentType, long size, String filename) {
    }
}
