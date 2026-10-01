package com.lelcc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 帖子表目前只有"统计某板块下有多少帖子"这一个需求（板块删除前的校验），
 * 所以先不建 Post 实体和 BaseMapper，等帖子模块再补。
 */
@Mapper
public interface PostMapper {

    @Select("SELECT COUNT(*) FROM post WHERE board_id = #{boardId}")
    long countByBoardId(@Param("boardId") Long boardId);
}
