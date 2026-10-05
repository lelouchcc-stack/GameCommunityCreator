package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.AppPostDetailVO;
import com.lelcc.dto.AppPostItemVO;
import com.lelcc.dto.HotPostVO;
import com.lelcc.dto.PostAuditVO;
import com.lelcc.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 用户端帖子检索：只要已发布、且所属板块开放的帖子。
     * tag 过滤用 EXISTS 子查询，避免 join post_tag 造成一行帖子被标签放大成多行。
     * 版本是否过期不在 SQL 里算，交给服务层按板块当前版本动态判定。
     */
    @Select("""
            <script>
            SELECT p.id, p.title, p.cover, p.board_id, b.name AS board_name,
                   p.version_id, v.version_no, p.stage_id, s.stage_name,
                   m.nickname AS author_name, m.avatar AS author_avatar,
                   p.like_count, p.view_count, p.comment_count, p.is_top, p.create_time,
                   b.current_version_id AS current_version_id
            FROM post p
            JOIN board b ON b.id = p.board_id
            LEFT JOIN member m ON m.id = p.member_id
            LEFT JOIN board_version v ON v.id = p.version_id
            LEFT JOIN progress_stage s ON s.id = p.stage_id
            WHERE p.status = 1 AND b.status = 1
            <if test="boardId != null">
                AND p.board_id = #{boardId}
            </if>
            <if test="versionId != null">
                AND p.version_id = #{versionId}
            </if>
            <if test="stageId != null">
                AND p.stage_id = #{stageId}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="tagId != null">
                AND EXISTS (SELECT 1 FROM post_tag pt WHERE pt.post_id = p.id AND pt.tag_id = #{tagId})
            </if>
            <choose>
                <when test="sort == 'hot'">
                    ORDER BY p.is_top DESC, p.like_count DESC, p.view_count DESC, p.id DESC
                </when>
                <otherwise>
                    ORDER BY p.is_top DESC, p.create_time DESC, p.id DESC
                </otherwise>
            </choose>
            </script>
            """)
    IPage<AppPostItemVO> selectAppPostPage(IPage<AppPostItemVO> page,
                                           @Param("boardId") Long boardId,
                                           @Param("versionId") Long versionId,
                                           @Param("stageId") Long stageId,
                                           @Param("tagId") Long tagId,
                                           @Param("keyword") String keyword,
                                           @Param("sort") String sort);

    /** 用户端帖子详情（含作者、板块、版本、阶段与板块当前版本，供过期判定） */
    @Select("""
            SELECT p.id, p.title, p.content, p.cover, p.image_urls AS image_urls_json,
                   p.board_id, b.name AS board_name,
                   p.version_id, v.version_no, p.stage_id, s.stage_name,
                   p.member_id AS author_id, m.nickname AS author_name, m.avatar AS author_avatar,
                   p.like_count, p.favorite_count, p.comment_count, p.view_count, p.is_top,
                   p.status, p.create_time,
                   b.current_version_id AS current_version_id
            FROM post p
            JOIN board b ON b.id = p.board_id
            LEFT JOIN member m ON m.id = p.member_id
            LEFT JOIN board_version v ON v.id = p.version_id
            LEFT JOIN progress_stage s ON s.id = p.stage_id
            WHERE p.id = #{postId}
            """)
    AppPostDetailVO selectAppDetail(@Param("postId") Long postId);

    /** 帖子标签名列表 */
    @Select("""
            SELECT t.tag_name
            FROM post_tag pt
            JOIN tag t ON t.id = pt.tag_id
            WHERE pt.post_id = #{postId}
            ORDER BY t.sort, t.id
            """)
    List<String> selectTagNames(@Param("postId") Long postId);

    /** 浏览量 +1 */
    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);
}
