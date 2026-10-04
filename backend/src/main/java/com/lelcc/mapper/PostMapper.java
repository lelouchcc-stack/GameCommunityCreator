package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.HotPostVO;
import com.lelcc.dto.PostAuditVO;
import com.lelcc.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT COUNT(*) FROM post WHERE board_id = #{boardId}")
    long countByBoardId(@Param("boardId") Long boardId);

    /**
     * 帖子审核列表。板块、作者、关联版本与进度阶段都靠左联接带出来，
     * 让管理员在列表里就能看清"这篇帖挂在哪个版本、作者是谁"。
     */
    @Select("""
            <script>
            SELECT p.id, p.title, p.content, p.status, p.audit_remark, p.create_time,
                   b.name AS board_name,
                   m.nickname AS author_name,
                   v.version_no,
                   s.stage_name
            FROM post p
            LEFT JOIN board b ON b.id = p.board_id
            LEFT JOIN member m ON m.id = p.member_id
            LEFT JOIN board_version v ON v.id = p.version_id
            LEFT JOIN progress_stage s ON s.id = p.stage_id
            <where>
                <if test="status != null">
                    AND p.status = #{status}
                </if>
                <if test="boardId != null">
                    AND p.board_id = #{boardId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND p.title LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            ORDER BY p.id DESC
            </script>
            """)
    IPage<PostAuditVO> selectAuditPage(IPage<PostAuditVO> page,
                                       @Param("status") Integer status,
                                       @Param("boardId") Long boardId,
                                       @Param("keyword") String keyword);

    /**
     * 用户端首页热门帖子：只要已发布的，置顶优先，再按点赞、浏览降序。
     * 板块关闭后它的帖子也不再上首页（b.status = 1）。
     */
    @Select("""
            SELECT p.id, p.title, p.cover, p.board_id, b.name AS board_name,
                   p.like_count, p.view_count, p.comment_count, p.is_top, p.create_time,
                   m.nickname AS author_name, m.avatar AS author_avatar
            FROM post p
            LEFT JOIN board b ON b.id = p.board_id
            LEFT JOIN member m ON m.id = p.member_id
            WHERE p.status = 1 AND b.status = 1
            ORDER BY p.is_top DESC, p.like_count DESC, p.view_count DESC, p.id DESC
            LIMIT #{limit}
            """)
    List<HotPostVO> selectHotList(@Param("limit") int limit);
}
