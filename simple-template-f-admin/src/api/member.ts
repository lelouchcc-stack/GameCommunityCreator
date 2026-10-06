import http from "../utils/request";
import type { PageResult } from "../types/common";
import type { PostListItem } from "./content";

/** 与后端 MemberInfoVO 对应（管理端画布预览 builtin:profile 时需要这份形状） */
export interface MemberInfo {
    id: number
    username: string
    nickname: string | null
    avatar: string | null
    intro: string | null
}

/** 个人资料修改请求体 */
export interface MemberProfileForm {
    nickname?: string
    intro?: string
    avatar?: string
}

/** 修改昵称/简介/头像，返回更新后的信息（需登录） */
export function updateProfile(data: MemberProfileForm) {
    return http.put<MemberInfo, MemberInfo>("/app/member/profile", data);
}

/** 我的帖子：含待审核与已下架的（需登录） */
export function pageMyPosts(pageNum = 1, pageSize = 10) {
    return http.get<PageResult<PostListItem>, PageResult<PostListItem>>("/app/member/my-posts", {
        params: { pageNum, pageSize },
    });
}
