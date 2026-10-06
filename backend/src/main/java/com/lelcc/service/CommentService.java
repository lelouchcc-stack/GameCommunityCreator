package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.CommentDTO;
import com.lelcc.dto.CommentVO;

/** 评论读取与发表：两级评论（一级评论分页，回复挂在其下）。 */
public interface CommentService {

    /** 某帖的一级评论分页，每条带该一级评论下的全部回复 */
    PageResult<CommentVO> page(Long postId, long pageNum, long pageSize);

    /** 发表评论/回复，并同步更新帖子评论计数 */
    CommentVO create(CommentDTO dto);
}
