import http from "../utils/request";
import type { BannerSlide, BannerSlideForm } from "../types/banner";

/** 轮播图列表：数量有限，不分页，一次取全 */
export function listBanners() {
    return http.get<BannerSlide[], BannerSlide[]>("/admin/banner/list");
}

export function createBanner(data: BannerSlideForm) {
    return http.post<void, void>("/admin/banner", data);
}

export function updateBanner(id: number, data: BannerSlideForm) {
    return http.put<void, void>(`/admin/banner/${id}`, data);
}

/** 设为展示(1)或隐藏(0) */
export function changeBannerStatus(id: number, status: number) {
    return http.put<void, void>(`/admin/banner/${id}/status`, null, { params: { status } });
}

/** 整组重排序：按传入的 id 顺序重编 sort */
export function reorderBanners(ids: number[]) {
    return http.put<void, void>("/admin/banner/sort", ids);
}

export function deleteBanner(id: number) {
    return http.delete<void, void>(`/admin/banner/${id}`);
}

/** 用户端轮播用的展示中轮播图：匿名可读（后端已放行 /app/banner/list） */
export function listVisibleBanners() {
    return http.get<BannerSlide[], BannerSlide[]>("/app/banner/list");
}
