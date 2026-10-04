package com.lelcc.service.impl;

import com.lelcc.dto.HotPostVO;
import com.lelcc.mapper.PostMapper;
import com.lelcc.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_LIMIT = 10;

    @Resource
    private PostMapper postMapper;

    @Override
    public List<HotPostVO> listHot(int limit) {
        return postMapper.selectHotList(normalize(limit));
    }

    private int normalize(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
