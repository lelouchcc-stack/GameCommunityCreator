import http from "../utils/request";

/** 与后端 UploadVO 对应：库里存 key，url 是后端按当前存储 provider 现拼出的可访问地址 */
export interface UploadResult {
    key: string
    url: string
    name: string
    size: number
}

/**
 * 玩家端图片上传（个人主页头像、帖子封面）。
 *
 * 与管理端 /admin/upload/image 复用后端同一套 UploadService 与存储抽象，
 * 差别只在鉴权与目录：/app/** 不在放行名单里，必须带 MEMBER 令牌。
 * 单独放宽超时——请求拦截器的 5 秒默认值对上传来说太短。
 */
export function uploadMemberImage(file: File) {
    const form = new FormData();
    form.append("file", file);
    return http.post<UploadResult, UploadResult>("/app/upload/image", form, { timeout: 30000 });
}
