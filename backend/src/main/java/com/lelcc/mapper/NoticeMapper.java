package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.NoticeVO;
import com.lelcc.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 用户端首页公告栏：只取已发布的，按 sort 展示顺序。
     * limit 由首页节点上的「条数」决定，在 service 里已收敛过范围。
     */
    @Select("""
            SELECT id, title, content, type, create_time
            FROM notice
            WHERE status = 1
            ORDER BY sort, id
            LIMIT #{limit}
            """)
    List<NoticeVO> selectPublished(@Param("limit") int limit);

    /** 用户端公告列表页：已发布公告分页，管理端置顶（sort 小的在前）优先 */
    @Select("""
            SELECT id, title, content, type, create_time
            FROM notice
            WHERE status = 1
            ORDER BY sort, id DESC
            """)
    IPage<NoticeVO> selectPublishedPage(IPage<NoticeVO> page);

    /** 公告详情：只取已发布的，草稿对外不可见 */
    @Select("""
            SELECT id, title, content, type, create_time
            FROM notice
            WHERE id = #{id} AND status = 1
            """)
    NoticeVO selectPublishedById(@Param("id") Long id);

    /**
     * 后台公告分页：全部状态都返回（草稿要在列表里可见），
     * 顺带带出 sort/status/update_time 供后台表格与编辑弹窗用。
     */
    @Select("""
            <script>
            SELECT id, title, content, type, sort, status, create_time, update_time
            FROM notice
            <where>
                <if test="status != null">
                    AND status = #{status}
                </if>
                <if test="type != null">
                    AND type = #{type}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))
                </if>
            </where>
            ORDER BY sort, id DESC
            </script>
            """)
    IPage<NoticeVO> selectAdminPage(IPage<NoticeVO> page,
                                    @Param("status") Integer status,
                                    @Param("type") Integer type,
                                    @Param("keyword") String keyword);
}
