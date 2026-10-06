package com.lelcc.dto;

/** 当前登录玩家对某帖的互动状态与计数。 */
public record InteractionVO(boolean liked, boolean favorited,
                            int likeCount, int favoriteCount, int commentCount) {
}
