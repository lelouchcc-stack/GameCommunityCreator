package com.lelcc.service.impl;

import com.lelcc.common.BusinessException;
import com.lelcc.config.UploadProperties;
import com.lelcc.service.FileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 云对象存储实现（app.upload.provider=s3），走 S3 兼容协议。
 *
 * <p>部署方把 endpoint/桶/密钥配成任意提供 S3 兼容端点的对象存储即可，代码不认厂商；
 * 若某家云不支持 S3 协议，按 {@link FileStorage} 另写一个实现类即可（EXT：需要写代码）。</p>
 */
@Service
@ConditionalOnProperty(name = "app.upload.provider", havingValue = "s3")
public class S3CompatibleStorage implements FileStorage {

    private final UploadProperties properties;
    private final S3Client client;

    public S3CompatibleStorage(UploadProperties properties, S3Client client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            client.putObject(
                    request -> request.bucket(bucket()).key(key).contentType(contentType),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new BusinessException(500, "图片上传到对象存储失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(request -> request.bucket(bucket()).key(key));
        } catch (SdkException e) {
            throw new BusinessException(500, "对象存储删除失败：" + e.getMessage());
        }
    }

    @Override
    public String urlOf(String key) {
        return FileStorage.joinUrl(publicBaseUrl(), key);
    }

    @Override
    public String publicBaseUrl() {
        String configured = properties.getS3().getPublicBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        // 未配对外域名时按 path-style 兜底：{endpoint}/{bucket}
        return FileStorage.joinUrl(properties.getS3().getEndpoint(), bucket());
    }

    @Override
    public String provider() {
        return "s3";
    }

    private String bucket() {
        return properties.getS3().getBucket();
    }
}
