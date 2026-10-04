package com.lelcc.service;

import com.lelcc.dto.NoticeVO;

import java.util.List;

/** 公告读取：用户端首页公告栏用。维护端（后台公告 CRUD）尚未做，等运营域模块再补。 */
public interface NoticeService {

    /** 已发布的公告，按展示顺序，最多 limit 条（limit 由调用方给出，内部会收敛范围） */
    List<NoticeVO> listPublished(int limit);
}
