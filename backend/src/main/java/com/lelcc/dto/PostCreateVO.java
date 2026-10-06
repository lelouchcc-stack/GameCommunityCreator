package com.lelcc.dto;

/**
 * 发帖结果：新帖 id 与落库状态。
 * 玩家端据此提示"已提交，等待审核"，并给出去「我的帖子」的入口——
 * 帖子在审核通过前不在任何公开列表里，所以必须让作者知道自己那条去哪了。
 */
public record PostCreateVO(Long id, Integer status) {
}
