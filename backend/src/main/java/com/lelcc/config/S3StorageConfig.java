package com.lelcc.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * 仅在 app.upload.provider=s3 时装配 S3 兼容客户端。
 *
 * <p>endpoint 可覆盖，因此同一份代码能对接任意厂商的 S3 兼容地址（含 MinIO 等自建网关）；
 * 这里出现的类型都属于 S3 协议本身，没有任何云厂商专有 SDK。</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.upload.provider", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(UploadProperties properties) {
        return buildClient(properties.getS3());
    }

    /** 独立成静态方法，便于测试在不启动 Spring 的情况下构建同一份客户端 */
    public static S3Client buildClient(UploadProperties.S3 config) {
        var builder = S3Client.builder()
                .region(Region.of(StrUtil.blankToDefault(config.getRegion(), "us-east-1")))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(config.isPathStyle())
                        // 兼容非 AWS 网关：关掉分块编码，让请求带明确的 content-length
                        .chunkedEncodingEnabled(false)
                        .build())
                .httpClientBuilder(ApacheHttpClient.builder()
                        // 100-continue 在部分兼容实现上握手不等价，关掉更稳
                        .expectContinueEnabled(false));
        if (StrUtil.isNotBlank(config.getEndpoint())) {
            builder.endpointOverride(URI.create(config.getEndpoint()));
        }
        return builder.build();
    }
}
