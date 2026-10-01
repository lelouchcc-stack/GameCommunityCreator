package com.lelcc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.PostAuditVO;
import com.lelcc.entity.Post;
import com.lelcc.mapper.PostMapper;
import com.lelcc.service.PostAuditService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostAuditServiceImpl implements PostAuditService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    @Resource
    private PostMapper postMapper;

    @Override
    public PageResult<PostAuditVO> page(Integer status, Long boardId, String keyword,
                                        long pageNum, long pageSize) {
        Page<PostAuditVO> page = new Page<>(pageNum, pageSize);
        return PageResult.of(postMapper.selectAuditPage(page, status, boardId, keyword));
    }

    @Override
    public void audit(Long id, Integer status, String reason) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }

        if (status != null && status == STATUS_PUBLISHED) {
            // 只有待审的帖子能被"通过"，否则说明这篇已被别的管理员处理过
            if (!Integer.valueOf(STATUS_PENDING).equals(post.getStatus())) {
                throw new BusinessException(400, "该帖子已被处理，请刷新后重试");
            }
            Post update = new Post();
            update.setId(post.getId());
            update.setStatus(STATUS_PUBLISHED);
            postMapper.updateById(update);
            return;
        }

        if (status != null && status == STATUS_OFFLINE) {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(400, "下架帖子必须填写下架原因");
            }
            if (Integer.valueOf(STATUS_OFFLINE).equals(post.getStatus())) {
                throw new BusinessException(400, "该帖子已被下架，请刷新后重试");
            }
            Post update = new Post();
            update.setId(post.getId());
            update.setStatus(STATUS_OFFLINE);
            update.setAuditRemark(reason);
            postMapper.updateById(update);
            return;
        }

        throw new BusinessException(400, "审核结果只能是通过(1)或下架(2)");
    }
}
