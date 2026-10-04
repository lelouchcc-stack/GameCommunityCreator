package com.lelcc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片存储配置。
 *
 * <p>provider 决定注入哪个 {@link com.lelcc.service.FileStorage} 实现：
 * local＝本地磁盘（开发/答辩默认），s3＝任意 S3 兼容对象存储（生产云 OSS，部署方自选厂商）。
 * 这里只有 S3 协议相关的通用项，没有任何云厂商专有配置。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** local | s3 */
    private String provider = "local";

    private Local local = new Local();

    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        /** 本地存储根目录（相对后端运行目录） */
        private String dir = "./uploads";
        /** 对外访问前缀，与 WebMvcConfig 的 /static/** 映射配套 */
        private String publicBaseUrl = "/static";
    }

    @Getter
    @Setter
    public static class S3 {
        private String endpoint = "";
        private String region = "";
        private String bucket = "";
        private String accessKey = "";
        private String secretKey = "";
        /** 对外访问基址（CDN / 自定义域名 / 桶公网地址）；留空则用 endpoint/bucket 兜底 */
        private String publicBaseUrl = "";
        /** MinIO 等自建网关常需 path-style（{endpoint}/{bucket}/{key}） */
        private boolean pathStyle = false;
    }
}
