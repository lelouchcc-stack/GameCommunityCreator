package com.lelcc.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.AppPostDetailVO;
import com.lelcc.dto.AppPostItemVO;
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
    private static final int STATUS_PUBLISHED = 1;

    @Resource
    private PostMapper postMapper;

    @Override
    public List<HotPostVO> listHot(int limit) {
        return postMapper.selectHotList(normalize(limit));
    }

    @Override
    public PageResult<AppPostItemVO> pagePublished(Long boardId, Long versionId, Long stageId, Long tagId,
                                                   String keyword, String sort, long pageNum, long pageSize) {
        Page<AppPostItemVO> page = new Page<>(pageNum, pageSize);
        PageResult<AppPostItemVO> result = PageResult.of(
                postMapper.selectAppPostPage(page, boardId, versionId, stageId, tagId, keyword, sort));
        result.list().forEach(this::markExpired);
        return result;
    }

    @Override
    public AppPostDetailVO detail(Long postId) {
        AppPostDetailVO detail = postMapper.selectAppDetail(postId);
        if (detail == null || !Integer.valueOf(STATUS_PUBLISHED).equals(detail.getStatus())) {
            throw new BusinessException(404, "内容不可见或已下架");
        }
        postMapper.incrementViewCount(postId);
        // 返回值与刚写入的浏览数保持一致，前端无需再拉一次
        detail.setViewCount((detail.getViewCount() == null ? 0 : detail.getViewCount()) + 1);
        detail.setTags(postMapper.selectTagNames(postId));
        detail.setImageUrls(parseImageUrls(detail.getImageUrlsJson()));
        detail.setImageUrlsJson(null);
        markExpired(detail);
        return detail;
    }

    /** 帖子版本非空且与板块当前版本不一致时算过期；通用帖（版本为空）不算 */
    private void markExpired(AppPostItemVO item) {
        Long current = item.getCurrentVersionId();
        Long version = item.getVersionId();
        item.setExpired(current != null && version != null && !version.equals(current));
    }

    private void markExpired(AppPostDetailVO detail) {
        Long current = detail.getCurrentVersionId();
        Long version = detail.getVersionId();
        detail.setExpired(current != null && version != null && !version.equals(current));
    }

    /** image_urls 存的是 JSON 数组字符串，解析失败按空处理，不影响详情展示 */
    private List<String> parseImageUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private int normalize(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
