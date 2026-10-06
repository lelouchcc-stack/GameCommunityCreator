package com.lelcc.service;

import com.lelcc.dto.InteractionVO;

/** 帖子互动：点赞、收藏（点击即切换），计数与明细同事务维护。 */
public interface PostInteractionService {

    /** 当前登录玩家对该帖的互动状态与计数 */
    InteractionVO get(Long postId);

    /** 点赞/取消点赞 */
    InteractionVO toggleLike(Long postId);

    /** 收藏/取消收藏 */
    InteractionVO toggleFavorite(Long postId);
}
