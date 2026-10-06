package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.UserContext;
import com.lelcc.dto.InteractionVO;
import com.lelcc.entity.Post;
import com.lelcc.entity.PostFavorite;
import com.lelcc.entity.PostLike;
import com.lelcc.mapper.PostFavoriteMapper;
import com.lelcc.mapper.PostLikeMapper;
import com.lelcc.mapper.PostMapper;
import com.lelcc.service.PostInteractionService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostInteractionServiceImpl implements PostInteractionService {

    private static final int STATUS_PUBLISHED = 1;

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostLikeMapper postLikeMapper;

    @Resource
    private PostFavoriteMapper postFavoriteMapper;

    @Override
    public InteractionVO get(Long postId) {
        requirePublished(postId);
        return current(postId, UserContext.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InteractionVO toggleLike(Long postId) {
        requirePublished(postId);
        Long memberId = UserContext.getId();
        if (likes(postId, memberId)) {
            postLikeMapper.delete(Wrappers.<PostLike>lambdaQuery()
                    .eq(PostLike::getPostId, postId).eq(PostLike::getMemberId, memberId));
            postMapper.adjustLikeCount(postId, -1);
        } else {
            try {
                PostLike like = new PostLike();
                like.setPostId(postId);
                like.setMemberId(memberId);
                postLikeMapper.insert(like);
                postMapper.adjustLikeCount(postId, 1);
            } catch (DuplicateKeyException ignored) {
                // 并发下已被自己赞过：按已点赞处理，不再重复加计数
            }
        }
        return current(postId, memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InteractionVO toggleFavorite(Long postId) {
        requirePublished(postId);
        Long memberId = UserContext.getId();
        if (favorites(postId, memberId)) {
            postFavoriteMapper.delete(Wrappers.<PostFavorite>lambdaQuery()
                    .eq(PostFavorite::getPostId, postId).eq(PostFavorite::getMemberId, memberId));
            postMapper.adjustFavoriteCount(postId, -1);
        } else {
            try {
                PostFavorite favorite = new PostFavorite();
                favorite.setPostId(postId);
                favorite.setMemberId(memberId);
                postFavoriteMapper.insert(favorite);
                postMapper.adjustFavoriteCount(postId, 1);
            } catch (DuplicateKeyException ignored) {
                // 并发下已收藏：按已收藏处理
            }
        }
        return current(postId, memberId);
    }

    /** 回读最新计数与当前用户的互动状态，保证返回值与库内一致 */
    private InteractionVO current(Long postId, Long memberId) {
        Post fresh = postMapper.selectById(postId);
        return new InteractionVO(likes(postId, memberId), favorites(postId, memberId),
                fresh.getLikeCount(), fresh.getFavoriteCount(), fresh.getCommentCount());
    }

    private boolean likes(Long postId, Long memberId) {
        return memberId != null && postLikeMapper.exists(Wrappers.<PostLike>lambdaQuery()
                .eq(PostLike::getPostId, postId).eq(PostLike::getMemberId, memberId));
    }

    private boolean favorites(Long postId, Long memberId) {
        return memberId != null && postFavoriteMapper.exists(Wrappers.<PostFavorite>lambdaQuery()
                .eq(PostFavorite::getPostId, postId).eq(PostFavorite::getMemberId, memberId));
    }

    private Post requirePublished(Long postId) {
        Post post = postId == null ? null : postMapper.selectById(postId);
        if (post == null || !Integer.valueOf(STATUS_PUBLISHED).equals(post.getStatus())) {
            throw new BusinessException(404, "帖子不存在或不可互动");
        }
        return post;
    }
}
