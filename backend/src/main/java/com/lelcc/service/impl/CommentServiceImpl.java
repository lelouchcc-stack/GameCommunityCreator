package com.lelcc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.common.UserContext;
import com.lelcc.dto.CommentDTO;
import com.lelcc.dto.CommentVO;
import com.lelcc.entity.Comment;
import com.lelcc.entity.Member;
import com.lelcc.entity.Post;
import com.lelcc.mapper.CommentMapper;
import com.lelcc.mapper.MemberMapper;
import com.lelcc.mapper.PostMapper;
import com.lelcc.service.CommentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_PUBLISHED = 1;
    private static final long TOP_LEVEL = 0L;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private MemberMapper memberMapper;

    @Override
    public PageResult<CommentVO> page(Long postId, long pageNum, long pageSize) {
        requirePublished(postId);
        Page<CommentVO> page = new Page<>(pageNum, pageSize);
        PageResult<CommentVO> result = PageResult.of(commentMapper.selectTopPage(page, postId));
        for (CommentVO top : result.list()) {
            List<CommentVO> replies = commentMapper.selectReplies(top.getId());
            top.setReplies(replies);
        }
        return result;
    }

    @Override
    public CommentVO create(CommentDTO dto) {
        Post post = requirePublished(dto.postId());
        Long memberId = UserContext.getId();

        long parentId = TOP_LEVEL;
        long rootId = TOP_LEVEL;
        if (dto.parentId() != null && dto.parentId() != TOP_LEVEL) {
            Comment parent = commentMapper.selectById(dto.parentId());
            if (parent == null || !parent.getPostId().equals(post.getId())
                    || !Integer.valueOf(STATUS_NORMAL).equals(parent.getStatus())) {
                throw new BusinessException(400, "该评论不存在");
            }
            parentId = parent.getId();
            // 回复的回复统一挂到同一条一级评论下
            rootId = parent.getRootId() == TOP_LEVEL ? parent.getId() : parent.getRootId();
        }

        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setMemberId(memberId);
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setContent(dto.content());
        comment.setLikeCount(0);
        comment.setStatus(STATUS_NORMAL);
        commentMapper.insert(comment);

        postMapper.incrementCommentCount(post.getId());
        return toVo(comment, memberMapper.selectById(memberId));
    }

    private Post requirePublished(Long postId) {
        Post post = postId == null ? null : postMapper.selectById(postId);
        if (post == null || !Integer.valueOf(STATUS_PUBLISHED).equals(post.getStatus())) {
            throw new BusinessException(404, "帖子不存在或不可评论");
        }
        return post;
    }

    private CommentVO toVo(Comment comment, Member member) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setMemberId(comment.getMemberId());
        vo.setAuthorName(member == null ? null : member.getNickname());
        vo.setAuthorAvatar(member == null ? null : member.getAvatar());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
