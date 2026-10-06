/**
 * 业务组件注册表（管理端与用户端同源副本）：render_kind 以 builtin: 开头时按这里查组件。
 * 未注册的 builtin:* 由 NodeRenderer 回落到占位，不会白屏。
 */
import type { Component } from "vue";
import BannerCarousel from "./builtin/BannerCarousel.vue";
import BoardNav from "./builtin/BoardNav.vue";
import BoardDetail from "./builtin/BoardDetail.vue";
import HotPostList from "./builtin/HotPostList.vue";
import NoticeBar from "./builtin/NoticeBar.vue";
import NoticeList from "./builtin/NoticeList.vue";
import NoticeDetail from "./builtin/NoticeDetail.vue";
import PostList from "./builtin/PostList.vue";
import PostDetail from "./builtin/PostDetail.vue";
import PostActions from "./builtin/PostActions.vue";
import CommentList from "./builtin/CommentList.vue";
import GameIntro from "./builtin/GameIntro.vue";
import GameVersions from "./builtin/GameVersions.vue";
import Profile from "./builtin/Profile.vue";
import PostForm from "./builtin/PostForm.vue";

export const BUILTIN_WIDGETS: Record<string, Component> = {
    "builtin:banner": BannerCarousel,
    "builtin:notice": NoticeBar,
    "builtin:hot_post": HotPostList,
    "builtin:board_nav": BoardNav,
    "builtin:board_detail": BoardDetail,
    "builtin:post_list": PostList,
    "builtin:post_detail": PostDetail,
    "builtin:post_actions": PostActions,
    "builtin:comment_list": CommentList,
    // 默认社区模板新增：游戏简介 / 版本更新 / 公告列表与详情 / 个人主页
    "builtin:game_intro": GameIntro,
    "builtin:game_versions": GameVersions,
    "builtin:notice_list": NoticeList,
    "builtin:notice_detail": NoticeDetail,
    "builtin:profile": Profile,
    // 默认社区模板：发帖表单（板块→版本→阶段→标签联动 + 封面上传 + 提交待审）
    "builtin:post_form": PostForm,
};

/** 取出 renderKind 对应的业务组件；不是业务组件或尚未接入时返回 null */
export function resolveBuiltin(renderKind?: string | null): Component | null {
    if (!renderKind || !renderKind.startsWith("builtin:")) {
        return null;
    }
    return BUILTIN_WIDGETS[renderKind] ?? null;
}
