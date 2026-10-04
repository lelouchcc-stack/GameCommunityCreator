package com.lelcc.service;

/**
 * 图片对象存储抽象——唯一与「存储后端」相关的边界。
 *
 * <p>业务层只依赖这个接口：本地磁盘与云对象存储（S3 兼容协议）各是一个实现，
 * 由 app.upload.provider 选择。换存储后端不改业务代码，也不引入任何云厂商专有类型。</p>
 *
 * <p>库里存的是 objectKey（如 upload/2026/10/04/xxx.png），对外 URL 由 {@link #urlOf(String)}
 * 在访问时现拼——换域名/换 CDN 不必迁移历史数据。</p>
 */
public interface FileStorage {

    /** 写入一个对象。key 由调用方生成（见 UploadService），实现只负责持久化 */
    void put(String key, byte[] content, String contentType);

    /** 删除对象；不存在时静默 */
    void delete(String key);

    /** 由 objectKey 现拼对外可访问 URL */
    String urlOf(String key);

    /** 对外访问基址，下发给前端用于把 key 解析成可显示地址 */
    String publicBaseUrl();

    /** provider 名：local / s3 */
    String provider();

    /** 基址 + objectKey 拼成对外地址；容忍基址结尾多余的斜杠 */
    static String joinUrl(String baseUrl, String key) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return key;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/" + key;
    }
}
