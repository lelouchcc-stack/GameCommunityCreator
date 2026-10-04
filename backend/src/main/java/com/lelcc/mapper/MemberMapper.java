package com.lelcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lelcc.entity.Member;
import org.apache.ibatis.annotations.Mapper;

/** 玩家账号。目前只需基础 CRUD，条件查询用 Wrappers 拼即可。 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
