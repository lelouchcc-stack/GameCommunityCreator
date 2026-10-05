package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lelcc.entity.ProgressStage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProgressStageMapper extends BaseMapper<ProgressStage> {

    /** 阶段被帖子引用时不允许删除 */
    @Select("SELECT COUNT(*) FROM post WHERE stage_id = #{stageId}")
    long countPostsByStageId(@Param("stageId") Long stageId);

    /** 阶段被玩家的游戏进度引用时不允许删除 */
    @Select("SELECT COUNT(*) FROM member_game_progress WHERE stage_id = #{stageId}")
    long countMemberProgressByStageId(@Param("stageId") Long stageId);
}
