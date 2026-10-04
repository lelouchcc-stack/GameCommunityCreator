package com.lelcc.dto;

/**
 * 存储配置的只读视图：provider 名与对外访问基址。
 * 前端拿到 publicBaseUrl 后，才能把库里存的 objectKey 现拼成可显示的图片地址。
 */
public record StorageConfigVO(String provider, String publicBaseUrl) {
}
