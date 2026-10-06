package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子收藏，对应 post_favorite。唯一键 (post_id, member_id) 保证同一人只能收藏一次。 */
@Data
@TableName("post_favorite")
public class PostFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long memberId;

    private LocalDateTime createTime;
}
