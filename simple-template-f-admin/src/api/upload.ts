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

/**
 * 玩家端图片上传（同源业务组件 builtin:post_form / builtin:profile 引用它，管理端必须能编过）。
 *
 * 管理端画布里这些组件的 interactive 分支不取数、不调接口，所以这个函数在后台永远不会真被执行；
 * 放在这里只是为了让共享组件的 import 在两个应用里都能解析。真正执行的是用户端的那份副本
 * （simple-template-f-user/src/api/upload.ts，同样叫 uploadMemberImage）。
 */
export function uploadMemberImage(file: File) {
    const form = new FormData();
    form.append("file", file);
    return http.post<UploadResult, UploadResult>("/app/upload/image", form, { timeout: 30000 });
}
