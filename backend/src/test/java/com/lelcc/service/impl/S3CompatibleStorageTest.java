package com.lelcc.service.impl;

import com.lelcc.config.S3StorageConfig;
import com.lelcc.config.UploadProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 兼容存储的协议级测试：用一个本地假的 S3 HTTP 端点（JDK HttpServer）承接请求，
 * 断言 SDK 真的按 S3 协议把对象写到了 {endpoint}/{bucket}/{key}，无需任何云账号或 Docker。
 */
class S3CompatibleStorageTest {

    private static HttpServer server;
    private static int port;
    /** 记录收到的请求行，形如 "PUT /test-bucket/upload/a.png" */
    private static final List<String> received = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void startFakeS3() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readAllBytes();
            }
            received.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            // ETag 必须是内容的 MD5：SDK 会校验它，并拒绝不匹配的响应
            exchange.getResponseHeaders().add("ETag", "\"" + md5Hex(body) + "\"");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    private static String md5Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    static void stopFakeS3() {
        server.stop(0);
    }

    @BeforeEach
    void reset() {
        received.clear();
    }

    private static UploadProperties.S3 baseConfig() {
        UploadProperties.S3 config = new UploadProperties.S3();
        config.setEndpoint("http://127.0.0.1:" + port);
        config.setRegion("us-east-1");
        config.setBucket("test-bucket");
        config.setAccessKey("test-ak");
        config.setSecretKey("test-sk");
        config.setPathStyle(true);
        return config;
    }

    private static S3CompatibleStorage storageOf(UploadProperties.S3 config) {
        UploadProperties properties = new UploadProperties();
        properties.setS3(config);
        return new S3CompatibleStorage(properties, S3StorageConfig.buildClient(config));
    }

    @Test
    void putSendsObjectToBucketAndKey() {
        storageOf(baseConfig()).put("upload/2026/10/04/a.png", "hello".getBytes(StandardCharsets.UTF_8), "image/png");

        assertThat(received).contains("PUT /test-bucket/upload/2026/10/04/a.png");
    }

    @Test
    void deleteTargetsSameBucketAndKey() {
        storageOf(baseConfig()).delete("upload/2026/10/04/a.png");

        assertThat(received).contains("DELETE /test-bucket/upload/2026/10/04/a.png");
    }

    @Test
    void urlOfPrefersConfiguredPublicBaseUrl() {
        UploadProperties.S3 config = baseConfig();
        config.setPublicBaseUrl("https://cdn.example.com/");

        S3CompatibleStorage storage = storageOf(config);

        assertThat(storage.urlOf("upload/x.png")).isEqualTo("https://cdn.example.com/upload/x.png");
        assertThat(storage.publicBaseUrl()).isEqualTo("https://cdn.example.com/");
    }

    @Test
    void urlOfFallsBackToEndpointAndBucket() {
        S3CompatibleStorage storage = storageOf(baseConfig());

        assertThat(storage.urlOf("upload/x.png"))
                .isEqualTo("http://127.0.0.1:" + port + "/test-bucket/upload/x.png");
    }

    @Test
    void providerNameIsS3() {
        assertThat(storageOf(baseConfig()).provider()).isEqualTo("s3");
    }
}
