import http from "../utils/request";
import type { StorageConfig, UploadResult } from "../types/upload";

/**
 * 上传图片。表单字段里存返回的 key，显示时由 resolveAssetUrl 用存储基址现拼地址。
 * 单独放宽超时：请求拦截器的 3 秒默认值对上传来说太短。
 */
export function uploadImage(file: File) {
    const form = new FormData();
    form.append("file", file);
    return http.post<UploadResult, UploadResult>("/admin/upload/image", form, { timeout: 30000 });
}

/** 当前存储 provider 与对外基址，用于把已存在的 key 解析成可预览的地址 */
export function getUploadConfig() {
    return http.get<StorageConfig, StorageConfig>("/admin/upload/config");
}
