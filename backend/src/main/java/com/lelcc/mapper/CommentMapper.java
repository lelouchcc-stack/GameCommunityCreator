package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.CommentVO;
import com.lelcc.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /** 一级评论分页（parent_id=0），作者信息左联带出，最新在前 */
    @Select("""
            SELECT c.id, c.post_id, c.member_id, m.nickname AS author_name, m.avatar AS author_avatar,
                   c.parent_id, c.root_id, c.content, c.like_count, c.create_time
            FROM comment c
            LEFT JOIN member m ON m.id = c.member_id
            WHERE c.post_id = #{postId} AND c.status = 1 AND c.parent_id = 0
            ORDER BY c.create_time DESC, c.id DESC
            """)
    IPage<CommentVO> selectTopPage(IPage<CommentVO> page, @Param("postId") Long postId);

    /** 某个一级评论下的全部回复，按时间正序 */
    @Select("""
            SELECT c.id, c.post_id, c.member_id, m.nickname AS author_name, m.avatar AS author_avatar,
                   c.parent_id, c.root_id, c.content, c.like_count, c.create_time
            FROM comment c
            LEFT JOIN member m ON m.id = c.member_id
            WHERE c.status = 1 AND c.root_id = #{rootId}
            ORDER BY c.create_time ASC, c.id ASC
            """)
    List<CommentVO> selectReplies(@Param("rootId") Long rootId);
}
