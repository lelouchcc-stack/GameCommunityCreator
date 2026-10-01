package com.lelcc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.common.UserContext;
import com.lelcc.dto.ReportVO;
import com.lelcc.entity.Post;
import com.lelcc.entity.Report;
import com.lelcc.mapper.PostMapper;
import com.lelcc.mapper.ReportMapper;
import com.lelcc.service.ReportService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ReportServiceImpl implements ReportService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_HANDLED = 1;
    private static final int STATUS_REJECTED = 2;

    private static final int POST_STATUS_OFFLINE = 2;

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private PostMapper postMapper;

    @Override
    public PageResult<ReportVO> page(Integer status, long pageNum, long pageSize) {
        Page<ReportVO> page = new Page<>(pageNum, pageSize);
        return PageResult.of(reportMapper.selectReportPage(page, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Integer status, String remark, Boolean offlinePost) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "举报记录不存在");
        }
        // 已经被别的管理员处理过就不要再覆盖一遍
        if (!Integer.valueOf(STATUS_PENDING).equals(report.getStatus())) {
            throw new BusinessException(400, "该举报已被处理，请刷新后重试");
        }
        if (status == null || (status != STATUS_HANDLED && status != STATUS_REJECTED)) {
            throw new BusinessException(400, "处理结果只能是已处理(1)或已驳回(2)");
        }
        if (status == STATUS_REJECTED && !StringUtils.hasText(remark)) {
            throw new BusinessException(400, "驳回举报必须填写驳回说明");
        }

        Report update = new Report();
        update.setId(report.getId());
        update.setStatus(status);
        update.setHandleRemark(remark);
        update.setHandleUserId(UserContext.getId());
        update.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(update);

        if (Boolean.TRUE.equals(offlinePost) && status == STATUS_HANDLED) {
            if (!StringUtils.hasText(remark)) {
                throw new BusinessException(400, "同时下架帖子时必须填写处理说明");
            }
            Post post = postMapper.selectById(report.getPostId());
            if (post != null) {
                Post postUpdate = new Post();
                postUpdate.setId(post.getId());
                postUpdate.setStatus(POST_STATUS_OFFLINE);
                postUpdate.setAuditRemark(remark);
                postMapper.updateById(postUpdate);
            }
        }
    }
}
