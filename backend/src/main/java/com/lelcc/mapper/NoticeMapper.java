package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
