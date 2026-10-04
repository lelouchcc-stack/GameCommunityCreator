package com.lelcc.service.impl;

import com.lelcc.dto.NoticeVO;
import com.lelcc.mapper.NoticeMapper;
import com.lelcc.service.NoticeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    /** 首页公告栏放不下太多条，上限兜一下，避免调用方传个巨大的 limit 把整表拉出来 */
    private static final int MAX_LIMIT = 20;
    private static final int DEFAULT_LIMIT = 5;

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public List<NoticeVO> listPublished(int limit) {
        return noticeMapper.selectPublished(normalize(limit));
    }

    private int normalize(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
