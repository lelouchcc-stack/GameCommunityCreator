package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lelcc.dto.BoardVO;
import com.lelcc.entity.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BoardMapper extends BaseMapper<Board> {

    /**
     * 板块分页列表，顺带把当前版本号查出来。
     * 用 <script> 拼条件是因为要 join，用不了 LambdaQueryWrapper 的 customSqlSegment。
     */
    @Select("""
            <script>
            SELECT b.id, b.name, b.intro, b.cover, b.sort, b.status,
                   b.current_version_id, v.version_no AS current_version_no, b.create_time
            FROM board b
            LEFT JOIN board_version v ON v.id = b.current_version_id
            <where>
                <if test="name != null and name != ''">
                    AND b.name LIKE CONCAT('%', #{name}, '%')
                </if>
                <if test="status != null">
                    AND b.status = #{status}
                </if>
            </where>
            ORDER BY b.sort, b.id
            </script>
            """)
    IPage<BoardVO> selectBoardPage(IPage<BoardVO> page,
                                   @Param("name") String name,
                                   @Param("status") Integer status);

    /**
     * 用户端板块导航用：只取开放板块，按展示顺序返回。
     * 不分页——板块数量有限，首页一次拿全。
     */
    @Select("""
            SELECT b.id, b.name, b.intro, b.cover, b.sort, b.status,
                   b.current_version_id, v.version_no AS current_version_no, b.create_time
            FROM board b
            LEFT JOIN board_version v ON v.id = b.current_version_id
            WHERE b.status = 1
            ORDER BY b.sort, b.id
            """)
    List<BoardVO> selectOpenList();
}
