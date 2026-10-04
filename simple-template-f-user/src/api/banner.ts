import http from "../utils/request";

/** 与后端 BannerSlideVO 对应（用户端轮播只需要这几个字段） */
export interface BannerItem {
    id: number
    title: string | null
    subtitle: string | null
    /** objectKey 或完整地址，渲染前用 resolveAssetUrl 现拼 */
    image: string
    linkUrl: string | null
    sort: number
}

/** 展示中的轮播图：匿名可读（后端已对会员拦截器放行 /app/banner/list） */
export function listVisibleBanners() {
    return http.get<BannerItem[], BannerItem[]>("/app/banner/list");
}
