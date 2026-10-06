package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.TagVO;
import com.lelcc.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** 标签分页列表，左联板块名便于后台表格展示。 */
    @Select("""
            <script>
            SELECT t.id, t.board_id, b.name AS board_name, t.tag_name, t.tag_type,
                   t.sort, t.status, t.create_time
            FROM tag t
            LEFT JOIN board b ON b.id = t.board_id
            <where>
                <if test="boardId != null">
                    AND t.board_id = #{boardId}
                </if>
                <if test="tagType != null">
                    AND t.tag_type = #{tagType}
                </if>
                <if test="status != null">
                    AND t.status = #{status}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND t.tag_name LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            ORDER BY t.sort, t.id
            </script>
            """)
    IPage<TagVO> selectTagPage(IPage<TagVO> page,
                               @Param("boardId") Long boardId,
                               @Param("tagType") Integer tagType,
                               @Param("status") Integer status,
                               @Param("keyword") String keyword);

    /** 标签被帖子引用时不允许删除 */
    @Select("SELECT COUNT(*) FROM post_tag WHERE tag_id = #{tagId}")
    long countPostRefs(@Param("tagId") Long tagId);

    /**
     * 用户端内容分类：启用的全局标签 + 指定板块的标签。
     * boardId 为空时只给全局标签（首页/攻略入口在没有板块上下文时用）。
     */
    @Select("""
            <script>
            SELECT t.id, t.board_id, t.tag_name, t.tag_type, t.sort
            FROM tag t
            LEFT JOIN board b ON b.id = t.board_id
            WHERE t.status = 1
              AND (t.board_id IS NULL
              <if test="boardId != null">
                   OR (t.board_id = #{boardId} AND b.status = 1)
              </if>
              )
            ORDER BY t.sort, t.id
            </script>
            """)
    List<TagVO> selectOpenTags(@Param("boardId") Long boardId);
}
