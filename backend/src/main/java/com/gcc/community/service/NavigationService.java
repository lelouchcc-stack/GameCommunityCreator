package com.gcc.community.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.domain.CommunityProject;
import com.gcc.community.domain.NavigationItem;
import com.gcc.community.domain.SitePage;
import com.gcc.community.dto.NavigationEnvelope;
import com.gcc.community.dto.NavigationItemInput;
import com.gcc.community.dto.NavigationItemView;
import com.gcc.community.dto.NavigationPayload;
import com.gcc.community.repository.SiteNavigationRepository;
import com.gcc.community.repository.SitePageRepository;
import com.gcc.community.validation.UrlRules;

import jakarta.annotation.Resource;

/** 站点导航的业务。 */
@Service
public class NavigationService {

    public static final int MAX_ITEMS_PER_LOCATION = 20;
    private static final int MAX_TITLE = 40;
    private static final int MAX_URL = 300;
    private static final int MAX_SORT_ORDER = 999;

    @Resource
    private ProjectService projectService;
    @Resource
    private SitePageRepository sitePageRepository;
    @Resource
    private SiteNavigationRepository siteNavigationRepository;

    public NavigationEnvelope get(long projectId) {
        projectService.requireOwnedProject(projectId);
        return new NavigationEnvelope(siteNavigationRepository.findByProjectIdOrdered(projectId).stream()
                .map(this::toView).toList());
    }

    // 整体替换：先删掉原来的，再按顺序插进去
    @Transactional
    public NavigationEnvelope save(long projectId, NavigationPayload payload) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        List<NavigationItemInput> items = payload == null || payload.items() == null
                ? List.of() : payload.items();

        Map<String, String> pageStatuses = new LinkedHashMap<>();
        for (SitePage page : sitePageRepository.findByProjectIdOrdered(project.id())) {
            pageStatuses.put(page.id(), page.status());
        }

        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Integer> locationCounts = new LinkedHashMap<>();
        List<ValidatedItem> validated = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            NavigationItemInput item = items.get(i);
            String path = "items[" + i + "]";
            if (item == null) {
                errors.put(path, "导航条目不能为空");
                continue;
            }
            String location = trim(item.location());
            if (!"header".equals(location) && !"footer".equals(location)) {
                errors.put(path + ".location", "location 只能是 header 或 footer");
            } else {
                locationCounts.merge(location, 1, Integer::sum);
            }
            String title = trim(item.title());
            if (title.length() < 1 || title.length() > MAX_TITLE) {
                errors.put(path + ".title", "标题长度必须在 1 到 40 个字符之间");
            }
            String targetType = trim(item.targetType());
            if (!"page".equals(targetType) && !"external".equals(targetType)) {
                errors.put(path + ".targetType", "targetType 只能是 page 或 external");
                continue;
            }
            String targetPageId = trimToNull(item.targetPageId());
            String url = trim(item.url());
            if ("page".equals(targetType)) {
                if (targetPageId == null) {
                    errors.put(path + ".targetPageId", "page 类型必须指定 targetPageId");
                } else {
                    String status = pageStatuses.get(targetPageId);
                    if (status == null) {
                        errors.put(path + ".targetPageId", "目标页面不存在");
                    } else if (SitePageService.STATUS_DISABLED.equals(status)) {
                        errors.put(path + ".targetPageId", "目标页面已停用");
                    }
                }
            } else {
                if (targetPageId != null) {
                    errors.put(path + ".targetPageId", "external 类型不能设置 targetPageId");
                }
                if (url.length() < 1 || url.length() > MAX_URL) {
                    errors.put(path + ".url", "URL 长度必须在 1 到 300 个字符之间");
                } else if (!UrlRules.isValidHttpUrl(url)) {
                    errors.put(path + ".url", "必须是 http/https 绝对 URL");
                }
            }
            Integer sortOrder = item.sortOrder();
            if (sortOrder == null) {
                errors.put(path + ".sortOrder", "sortOrder 不能为空");
                sortOrder = 0;
            } else if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
                errors.put(path + ".sortOrder", "sortOrder 必须在 0 到 999 之间");
            }
            Boolean visible = item.visible();
            if (visible == null) {
                errors.put(path + ".visible", "visible 不能为空");
                visible = Boolean.TRUE;
            }
            validated.add(new ValidatedItem(location, title, targetType, targetPageId,
                    "page".equals(targetType) ? "" : url, sortOrder, visible));
        }
        for (Map.Entry<String, Integer> entry : locationCounts.entrySet()) {
            if (entry.getValue() > MAX_ITEMS_PER_LOCATION) {
                errors.put("items", entry.getKey() + " 位置最多允许 20 条导航");
            }
        }
        if (!errors.isEmpty()) {
            Map.Entry<String, String> first = errors.entrySet().iterator().next();
            throw ApiException.badRequest("导航校验失败：" + first.getKey() + " " + first.getValue(),
                    new LinkedHashMap<>(errors));
        }

        siteNavigationRepository.deleteByProjectId(projectId);
        for (ValidatedItem item : validated) {
            siteNavigationRepository.insert(projectId, item.location(), item.title(), item.targetType(),
                    item.targetPageId(), item.url(), item.sortOrder(), item.visible());
        }
        return get(projectId);
    }

    // 公开端只保留可见的、目标页面也发布了的条目
    public List<NavigationItemView> filterForPublic(long projectId, java.util.Set<String> publishedPageIds) {
        List<NavigationItemView> filtered = new ArrayList<>();
        for (NavigationItem item : siteNavigationRepository.findVisibleByProjectId(projectId)) {
            if ("page".equals(item.targetType())) {
                if (item.targetPageId() == null || !publishedPageIds.contains(item.targetPageId())) {
                    continue;
                }
            } else if (!"external".equals(item.targetType())) {
                continue;
            }
            filtered.add(toView(item));
        }
        return filtered;
    }

    private NavigationItemView toView(NavigationItem item) {
        return new NavigationItemView(item.id(), item.location(), item.title(), item.targetType(),
                item.targetPageId(), item.url(), item.sortOrder(), item.visible());
    }

    private static String trim(String value) {
        return value == null ? "" : value.strip();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ValidatedItem(String location, String title, String targetType, String targetPageId,
            String url, int sortOrder, boolean visible) {
    }
}
