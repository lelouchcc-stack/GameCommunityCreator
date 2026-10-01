package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.ReportVO;
import com.lelcc.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /** 举报列表：带出被举报帖子的标题/内容/状态、举报人昵称、处理人账号 */
    @Select("""
            <script>
            SELECT r.id, r.post_id, r.reason, r.description, r.status,
                   r.handle_remark, r.handle_time, r.create_time,
                   p.title   AS post_title,
                   p.content AS post_content,
                   p.status  AS post_status,
                   m.nickname AS reporter_name,
                   u.username AS handle_user_name
            FROM report r
            LEFT JOIN post p ON p.id = r.post_id
            LEFT JOIN member m ON m.id = r.member_id
            LEFT JOIN sys_user u ON u.id = r.handle_user_id
            <where>
                <if test="status != null">
                    AND r.status = #{status}
                </if>
            </where>
            ORDER BY r.id DESC
            </script>
            """)
    IPage<ReportVO> selectReportPage(IPage<ReportVO> page, @Param("status") Integer status);
}
