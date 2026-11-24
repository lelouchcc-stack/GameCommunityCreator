package com.gcc.community.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.domain.FriendLink;
import com.gcc.community.dto.FriendLinkRequest;
import com.gcc.community.dto.FriendLinkView;
import com.gcc.community.repository.FriendLinkRepository;
import com.gcc.community.validation.UrlRules;

/** 友情链接的业务。 */
@Service
public class FriendLinkService {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private FriendLinkRepository friendLinkRepository;

    public List<FriendLinkView> list(long projectId) {
        projectService.requireOwnedProject(projectId);
        return friendLinkRepository.findByProjectIdOrdered(projectId).stream().map(this::toView).toList();
    }

    @Transactional
    public FriendLinkView create(long projectId, FriendLinkRequest request) {
        projectService.requireOwnedProject(projectId);
        Map<String, String> errors = new LinkedHashMap<>();
        String name = request.name();
        String url = request.url();
        String description = request.description() == null ? "" : request.description();
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        boolean enabled = request.enabled() == null || request.enabled();

        ContentValidation.requiredText(errors, "name", name, 60);
        validateUrl(errors, url);
        ContentValidation.optionalText(errors, "description", description, 200);
        ContentValidation.requirePositiveOrZero(errors, "sortOrder", request.sortOrder());
        ContentValidation.throwIfAny("友情链接参数校验失败", errors);

        long id = friendLinkRepository.insert(projectId, name, url, description, sortOrder, enabled);
        return toView(new FriendLink(id, projectId, name, url, description, sortOrder, enabled));
    }

    @Transactional
    public FriendLinkView update(long linkId, FriendLinkRequest request) {
        FriendLink existing = requireLink(linkId);
        projectService.requireOwnedProject(existing.projectId());
        Map<String, String> errors = new LinkedHashMap<>();
        String name = request.name() == null ? existing.name() : request.name();
        String url = request.url() == null ? existing.url() : request.url();
        String description = request.description() == null ? existing.description() : request.description();
        int sortOrder = request.sortOrder() == null ? existing.sortOrder() : request.sortOrder();
        boolean enabled = request.enabled() == null ? existing.enabled() : request.enabled();

        ContentValidation.requiredText(errors, "name", name, 60);
        validateUrl(errors, url);
        ContentValidation.optionalText(errors, "description", description, 200);
        ContentValidation.requirePositiveOrZero(errors, "sortOrder", request.sortOrder());
        ContentValidation.throwIfAny("友情链接参数校验失败", errors);

        friendLinkRepository.update(linkId, name, url, description, sortOrder, enabled);
        return toView(new FriendLink(linkId, existing.projectId(), name, url, description, sortOrder, enabled));
    }

    @Transactional
    public void delete(long linkId) {
        FriendLink existing = requireLink(linkId);
        projectService.requireOwnedProject(existing.projectId());
        friendLinkRepository.deleteById(linkId);
    }

    // 公开端只返回启用的链接
    public List<FriendLinkView> publicList(long projectId) {
        return friendLinkRepository.findEnabledByProjectId(projectId).stream().map(this::toView).toList();
    }

    private FriendLink requireLink(long linkId) {
        return friendLinkRepository.findById(linkId)
                .orElseThrow(() -> ApiException.notFound("友情链接不存在"));
    }

    private void validateUrl(Map<String, String> errors, String url) {
        if (url == null || url.isBlank()) {
            errors.put("url", "不能为空");
        } else if (url.length() > UrlRules.MAX_URL_LENGTH) {
            errors.put("url", "长度不能超过 300 个字符");
        } else if (!UrlRules.isValidHttpUrl(url)) {
            errors.put("url", "必须是 http/https 绝对 URL");
        }
    }

    private FriendLinkView toView(FriendLink link) {
        return new FriendLinkView(link.id(), link.name(), link.url(), link.description(),
                link.sortOrder(), link.enabled());
    }
}
