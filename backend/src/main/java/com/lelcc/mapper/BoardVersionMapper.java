package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lelcc.entity.BoardVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BoardVersionMapper extends BaseMapper<BoardVersion> {

    /** 版本被帖子引用时不允许删除，只能停用 */
    @Select("SELECT COUNT(*) FROM post WHERE version_id = #{versionId}")
    long countPostsByVersionId(@Param("versionId") Long versionId);
}
