package com.lelcc.mapper;

import com.lelcc.dto.StatBoardVO;
import com.lelcc.dto.StatTrendVO;
import com.lelcc.dto.StatVersionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据统计的聚合查询。
 *
 * <p>刻意不复用 PostMapper 等业务 Mapper：统计是只读的聚合，和业务查询的索引/形状都不一样，
 * 混在一起会让业务 Mapper 越来越难读。这里的每条 SQL 都带 from 参数（可空）——
 * 传了就统计"范围内新增/发生"，不传就是累计快照。</p>
 *
 * <p>注意：统计口径与用户端一致——只算已发布（post.status=1）与正常评论（comment.status=1），
 * 否则后台看到的活跃度会比玩家实际看到的高。</p>
 *
 * <p>写法提醒：`&lt;![CDATA[&gt;=]]&gt;` 只在 `<script>` 包裹的注解里才会被 MyBatis 解析；
 * 普通 `@Select`（没有 `<script>`）是纯文本，CDATA 会被原样发给数据库、直接报 SQL 语法错。
 * 这里的比较只用到 `&gt;=`（单个 `&gt;` 在 XML 里合法），所以一律不写 CDATA。</p>
 */
@Mapper
public interface StatMapper {

    /** 玩家数：from 为空取累计，否则取该日期之后注册的 */
    @Select("""
            <script>
            SELECT COUNT(*) FROM member
            <where>
                <if test="from != null">
                    AND create_time >= #{from}
                </if>
            </where>
            </script>
            """)
    long countMembers(@Param("from") LocalDate from);

    /** 帖子数：status 可空（空=全部状态），from 可空 */
    @Select("""
            <script>
            SELECT COUNT(*) FROM post
            <where>
                <if test="status != null">
                    AND status = #{status}
                </if>
                <if test="from != null">
                    AND create_time >= #{from}
                </if>
            </where>
            </script>
            """)
    long countPosts(@Param("status") Integer status, @Param("from") LocalDate from);

    /** 评论数：只算正常状态（status=1），from 可空 */
    @Select("""
            <script>
            SELECT COUNT(*) FROM comment
            WHERE status = 1
            <if test="from != null">
                AND create_time >= #{from}
            </if>
            </script>
            """)
    long countComments(@Param("from") LocalDate from);

    @Select("SELECT COUNT(*) FROM board WHERE status = 1")
    long countOpenBoards();

    @Select("SELECT COUNT(*) FROM notice WHERE status = 1")
    long countPublishedNotices();

    @Select("SELECT COUNT(*) FROM report WHERE status = 0")
    long countPendingReports();

    /** 每日新增帖子（已发布），仅返回有数据的天，补零交给 service */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS total
            FROM post
            WHERE status = 1 AND create_time >= #{from}
            GROUP BY date
            """)
    List<StatTrendVO> selectDailyPosts(@Param("from") LocalDate from);

    /** 每日新增评论（正常状态） */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS total
            FROM comment
            WHERE status = 1 AND create_time >= #{from}
            GROUP BY date
            """)
    List<StatTrendVO> selectDailyComments(@Param("from") LocalDate from);

    /** 帖子数最多的板块（含 0 帖的板块，用 LEFT JOIN 保住"板块本身存在"这个事实） */
    @Select("""
            SELECT b.id AS board_id, b.name AS board_name, COUNT(p.id) AS total
            FROM board b
            LEFT JOIN post p ON p.board_id = b.id AND p.status = 1
            WHERE b.status = 1
            GROUP BY b.id, b.name, b.sort
            ORDER BY total DESC, b.sort, b.id
            LIMIT #{limit}
            """)
    List<StatBoardVO> selectTopBoards(@Param("limit") int limit);

    /**
     * 版本分布：各版本的已发布帖子数，is_current 标记它是不是所属板块的当前版本。
     * 末尾用 UNION ALL 补一行"通用帖"（没挂版本的帖子），否则这类帖子在分布里会凭空消失。
     */
    @Select("""
            SELECT * FROM (
                SELECT v.version_no AS version_no, COUNT(p.id) AS total,
                       CASE WHEN b.current_version_id = v.id THEN 1 ELSE 0 END AS is_current
                FROM board_version v
                JOIN board b ON b.id = v.board_id
                LEFT JOIN post p ON p.version_id = v.id AND p.status = 1
                WHERE v.status = 1 AND b.status = 1
                GROUP BY v.id, v.version_no, b.current_version_id
                UNION ALL
                SELECT '（通用帖）' AS version_no, COUNT(*) AS total, 0 AS is_current
                FROM post WHERE version_id IS NULL AND status = 1
            ) t
            WHERE t.total > 0 OR t.version_no <> '（通用帖）'
            ORDER BY t.total DESC, t.version_no
            """)
    List<StatVersionVO> selectVersionDistribution();
}

