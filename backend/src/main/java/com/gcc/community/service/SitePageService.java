package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gcc.community.common.ApiException;
import com.gcc.community.domain.CommunityProject;
import com.gcc.community.domain.PageConfig;
import com.gcc.community.domain.PagePublicationSummary;
import com.gcc.community.domain.SitePage;
import com.gcc.community.domain.SitePageDraft;
import com.gcc.community.dto.CreatePageRequest;
import com.gcc.community.dto.PageEnvelope;
import com.gcc.community.dto.PublicationView;
import com.gcc.community.dto.PublishPageResult;
import com.gcc.community.dto.SitePageView;
import com.gcc.community.dto.SitePublishResult;
import com.gcc.community.dto.SitePublishSkipped;
import com.gcc.community.dto.UpdatePageRequest;
import com.gcc.community.repository.CommunityProjectRepository;
import com.gcc.community.repository.SiteNavigationRepository;
import com.gcc.community.repository.SitePageDraftRepository;
import com.gcc.community.repository.SitePagePublicationRepository;
import com.gcc.community.repository.SitePageRepository;
import com.gcc.community.validation.PageConfigValidator;
import com.gcc.community.validation.PageValidationContext;

/** 页面管理的业务。 */
@Service
public class SitePageService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_DISABLED = "DISABLED";

    public static final String CONFLICT_MESSAGE = "页面配置已被其他操作修改，请刷新后重试";

    private static final Pattern CUSTOM_SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");

    @Autowired
    private ProjectService projectService;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private SitePageDraftRepository sitePageDraftRepository;
    @Autowired
    private SitePagePublicationRepository sitePagePublicationRepository;
    @Autowired
    private SiteNavigationRepository siteNavigationRepository;
    @Autowired
    private CommunityProjectRepository projectRepository;
    @Autowired
    private DefaultPageTemplateService templateService;
    @Autowired
    private PageConfigValidator validator;
    @Autowired
    private ObjectMapper objectMapper;

    // 查询

    public List<SitePageView> list(long projectId) {
        projectService.requireOwnedProject(projectId);
        return sitePageRepository.findByProjectIdOrdered(projectId).stream().map(this::toView).toList();
    }

    public SitePageView get(long projectId, String pageId) {
        return toView(requirePage(projectId, pageId));
    }

    // 页面必须属于这个项目
    public SitePage requirePage(long projectId, String pageId) {
        projectService.requireOwnedProject(projectId);
        SitePage page = sitePageRepository.findById(pageId)
                .orElseThrow(() -> ApiException.notFound("页面不存在"));
        if (page.projectId() != projectId) {
            throw ApiException.notFound("页面不存在");
        }
        return page;
    }

    public SitePage requireHomePage(CommunityProject project) {
        return sitePageRepository.findHomePage(project.id())
                .orElseThrow(() -> ApiException.notFound("页面配置不存在"));
    }

    // 增删改查和复制

    @Transactional
    public SitePageView create(long projectId, CreatePageRequest request) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        Map<String, String> errors = new LinkedHashMap<>();

        String name = request.name() == null ? "" : request.name().strip();
        if (name.length() < 1 || name.length() > 40) {
            errors.put("name", "页面名称长度必须在 1 到 40 个字符之间");
        }
        String pageType = request.pageType() == null ? "" : request.pageType().strip();
        if (!PageTypes.isValidPageType(pageType)) {
            errors.put("pageType", "不支持的页面类型：" + request.pageType());
        }
        if (!errors.isEmpty()) {
            throw validationError("页面参数校验失败", errors);
        }
        if (PageTypes.HOME.equals(pageType)) {
            throw ApiException.conflict("首页已存在，不能重复创建");
        }
        if (PageTypes.isModuleType(pageType) && sitePageRepository.existsByProjectIdAndPageType(projectId, pageType)) {
            throw new ApiException(409, "该类型的页面已存在，每个站点最多一个", Map.of("pageType", "该类型的页面已存在"));
        }

        // 模块页 slug 固定，自定义页由用户填
        String slug;
        if (PageTypes.isModuleType(pageType)) {
            slug = PageTypes.FIXED_SLUG_BY_TYPE.get(pageType);
            if (sitePageRepository.findByProjectIdAndSlug(projectId, slug).isPresent()) {
                throw slugConflict();
            }
        } else {
            String customSlug = request.slug() == null ? "" : request.slug().strip();
            if (customSlug.isEmpty()) {
                errors.put("slug", "自定义页面必须指定页面标识");
            } else if (!CUSTOM_SLUG_PATTERN.matcher(customSlug).matches()) {
                errors.put("slug", "页面标识只能由小写字母、数字和中划线组成，长度 1–63 且以字母或数字开头");
            } else if (PageTypes.RESERVED_CUSTOM_SLUGS.contains(customSlug)) {
                errors.put("slug", "该页面标识为系统保留字，不能使用");
            }
            if (!errors.isEmpty()) {
                throw validationError("页面参数校验失败", errors);
            }
            slug = customSlug;
            if (sitePageRepository.findByProjectIdAndSlug(projectId, slug).isPresent()) {
                throw slugConflict();
            }
        }

        String templateKey = resolveTemplateKey(pageType, request.templateKey(), errors);
        if (!errors.isEmpty()) {
            throw validationError("页面参数校验失败", errors);
        }

        String pageId = generatePageId(pageType, projectId);
        int sortOrder = sitePageRepository.maxSortOrder(projectId) + 1;
        LocalDateTime now = LocalDateTime.now();
        SitePage page = new SitePage(pageId, projectId, name, slug, pageType, templateKey,
                STATUS_DRAFT, sortOrder, 1, null, now, now);
        try {
            sitePageRepository.insert(page);
        } catch (DuplicateKeyException ex) {
            // 并发创建同一 slug 时由唯一键兜底
            throw slugConflict();
        }
        initDraft(project, page, now);
        return toView(requirePage(projectId, pageId));
    }

    @Transactional
    public SitePageView update(long projectId, String pageId, UpdatePageRequest request) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage page = requirePage(projectId, pageId);
        if (request.name() == null && request.slug() == null && request.status() == null
                && request.templateKey() == null) {
            throw ApiException.badRequest("至少需要修改一个字段");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        boolean isHome = PageTypes.HOME.equals(page.pageType());

        String name = page.name();
        if (request.name() != null) {
            name = request.name().strip();
            if (name.length() < 1 || name.length() > 40) {
                errors.put("name", "页面名称长度必须在 1 到 40 个字符之间");
            }
        }

        String slug = page.slug();
        if (request.slug() != null && !request.slug().strip().equals(page.slug())) {
            if (isHome) {
                errors.put("slug", "首页 slug 不可修改");
            } else if (PageTypes.isModuleType(page.pageType())) {
                errors.put("slug", "模块页面 slug 不可修改");
            } else {
                String candidate = request.slug().strip();
                if (!CUSTOM_SLUG_PATTERN.matcher(candidate).matches()) {
                    errors.put("slug", "页面标识只能由小写字母、数字和中划线组成，长度 1–63 且以字母或数字开头");
                } else if (PageTypes.RESERVED_CUSTOM_SLUGS.contains(candidate)) {
                    errors.put("slug", "该页面标识为系统保留字，不能使用");
                } else if (sitePageRepository.findByProjectIdAndSlug(projectId, candidate).isPresent()) {
                    throw slugConflict();
                } else {
                    slug = candidate;
                }
            }
        }

        String status = page.status();
        if (request.status() != null) {
            String candidate = request.status().strip();
            if (!PageTypes.PAGE_STATUSES.contains(candidate)) {
                errors.put("status", "status 只能是 DRAFT、PUBLISHED 或 DISABLED");
            } else if (STATUS_PUBLISHED.equals(candidate) && page.publishedVersion() == null) {
                errors.put("status", "请先发布该页面");
            } else if (STATUS_DISABLED.equals(candidate) && isHome) {
                errors.put("status", "首页不可停用");
            } else {
                status = candidate;
            }
        }

        String templateKey = page.templateKey();
        if (request.templateKey() != null) {
            String allowed = DefaultPageTemplateService.templateKeyForPageType(page.pageType());
            if (!allowed.equals(request.templateKey().strip())) {
                errors.put("templateKey", "该页面类型只允许使用模板 " + allowed);
            } else {
                templateKey = allowed;
            }
        }
        if (!errors.isEmpty()) {
            throw validationError("页面参数校验失败", errors);
        }
        sitePageRepository.updateMeta(pageId, name, slug, status, templateKey, LocalDateTime.now());
        return get(projectId, pageId);
    }

    @Transactional
    public void delete(long projectId, String pageId) {
        SitePage page = requirePage(projectId, pageId);
        if (PageTypes.HOME.equals(page.pageType())) {
            throw ApiException.badRequest("首页不可删除");
        }
        sitePageDraftRepository.deleteByPageId(pageId);
        sitePagePublicationRepository.deleteByPageId(pageId);
        siteNavigationRepository.deleteByTargetPageId(projectId, pageId);
        sitePageRepository.deleteById(pageId);
    }

    // 复制页面，草稿也复制，但发布记录不复制
    @Transactional
    public SitePageView copy(long projectId, String pageId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage source = requirePage(projectId, pageId);
        SitePageDraft sourceDraft = sitePageDraftRepository.findByPageId(pageId)
                .orElseThrow(() -> ApiException.notFound("页面配置不存在"));

        String newPageId = generateCustomPageId();
        String newSlug = generateCopySlug(projectId, source.slug());
        String newName = truncate(source.name() + "（副本）", 40);
        int sortOrder = sitePageRepository.maxSortOrder(projectId) + 1;
        LocalDateTime now = LocalDateTime.now();
        SitePage copy = new SitePage(newPageId, projectId, newName, newSlug, PageTypes.CUSTOM, "blank",
                STATUS_DRAFT, sortOrder, 1, null, now, now);
        sitePageRepository.insert(copy);

        // 把草稿里的 pageId 换成新的
        JsonNode configNode = parseJson(sourceDraft.configJson());
        ((ObjectNode) configNode).put("pageId", newPageId);
        Map<String, String> statuses = new LinkedHashMap<>();
        for (SitePage page : sitePageRepository.findByProjectIdOrdered(projectId)) {
            statuses.put(page.id(), page.status());
        }
        PageConfig config = validator.validateForSave(configNode,
                new PageValidationContext(project.slug(), newPageId, statuses));
        sitePageDraftRepository.insert(new SitePageDraft(newPageId, projectService.serialize(config), 1, now));
        return toView(requirePage(projectId, newPageId));
    }

    // 发布

    // 发布单个页面
    @Transactional
    public PublishPageResult publishPage(long projectId, String pageId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage page = requirePage(projectId, pageId);
        return doPublish(project, page);
    }

    // 整站发布，逐页发布有改动的页面
    @Transactional
    public SitePublishResult publishSite(long projectId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        List<PublishPageResult> published = new ArrayList<>();
        List<SitePublishSkipped> skipped = new ArrayList<>();
        for (SitePage page : sitePageRepository.findByProjectIdOrdered(projectId)) {
            if (STATUS_DISABLED.equals(page.status())) {
                skipped.add(new SitePublishSkipped(page.id(), "页面已停用"));
                continue;
            }
            if (page.publishedVersion() != null && page.publishedVersion() == page.draftVersion()) {
                skipped.add(new SitePublishSkipped(page.id(), "该版本已发布"));
                continue;
            }
            published.add(doPublish(project, page));
        }
        projectRepository.updateStatus(projectId, STATUS_PUBLISHED, LocalDateTime.now());
        return new SitePublishResult(published, skipped);
    }

    public List<PublicationView> listPublications(long projectId, String pageId) {
        requirePage(projectId, pageId);
        return sitePagePublicationRepository.findSummariesByPageId(pageId).stream()
                .map(this::toPublicationView)
                .toList();
    }

    private PublishPageResult doPublish(CommunityProject project, SitePage page) {
        SitePageDraft draft = sitePageDraftRepository.findByPageId(page.id())
                .orElseThrow(() -> ApiException.notFound("页面配置不存在"));
        PageValidationContext ctx = projectService.validationContext(project, page.id());
        PageConfig config = validator.validateForSave(parseJson(draft.configJson()), ctx);
        String configJson = projectService.serialize(config);

        if (sitePagePublicationRepository.existsByPageIdAndConfigVersion(page.id(), draft.configVersion())) {
            throw ApiException.conflict("该版本已发布");
        }
        LocalDateTime now = LocalDateTime.now();
        long publicationId;
        try {
            publicationId = sitePagePublicationRepository.insert(
                    page.id(), project.id(), draft.configVersion(), configJson, now);
        } catch (DuplicateKeyException ex) {
            // 并发发布同一版本，由 (page_id, config_version) 唯一键兜底
            throw ApiException.conflict("该版本已发布");
        }
        sitePageRepository.updatePublishState(page.id(), STATUS_PUBLISHED, draft.configVersion(), now);
        projectRepository.updateStatus(project.id(), STATUS_PUBLISHED, now);
        return new PublishPageResult(page.id(), page.slug(), publicationId, draft.configVersion(), now,
                PageTypes.publicPath(project.slug(), page));
    }

    // 内部工具

    private void initDraft(CommunityProject project, SitePage page, LocalDateTime now) {
        Map<String, String> statuses = new LinkedHashMap<>();
        for (SitePage existing : sitePageRepository.findByProjectIdOrdered(project.id())) {
            statuses.put(existing.id(), existing.status());
        }
        String pageTitle = PageTypes.defaultPageTitle(page.pageType(), project.gameName(), page.name());
        PageConfig config = templateService.render(page.templateKey(),
                new DefaultPageTemplateService.Placeholders(page.id(), project.gameName(), project.slug(),
                        pageTitle),
                new PageValidationContext(project.slug(), page.id(), statuses));
        sitePageDraftRepository.insert(new SitePageDraft(page.id(), projectService.serialize(config), 1, now));
    }

    private String resolveTemplateKey(String pageType, String requested, Map<String, String> errors) {
        String allowed = DefaultPageTemplateService.templateKeyForPageType(pageType);
        if (requested == null || requested.isBlank()) {
            return allowed;
        }
        if (!allowed.equals(requested.strip())) {
            errors.put("templateKey", "该页面类型只允许使用模板 " + allowed);
            return allowed;
        }
        return allowed;
    }

    private String generatePageId(String pageType, long projectId) {
        if (PageTypes.isModuleType(pageType)) {
            return PageTypes.modulePageId(pageType, projectId);
        }
        return generateCustomPageId();
    }

    private String generateCustomPageId() {
        for (int i = 0; i < 20; i++) {
            String candidate = "page_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            if (sitePageRepository.findById(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("无法生成唯一的页面 ID");
    }

    private String generateCopySlug(long projectId, String sourceSlug) {
        for (int i = 0; i < 20; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            String candidate = truncate(sourceSlug + "-copy-" + suffix, 63);
            if (sitePageRepository.findByProjectIdAndSlug(projectId, candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("无法生成唯一的页面标识");
    }

    private SitePageView toView(SitePage page) {
        return new SitePageView(page.id(), page.projectId(), page.name(), page.slug(), page.pageType(),
                page.templateKey(), page.status(), page.draftVersion(), page.publishedVersion(),
                PageTypes.HOME.equals(page.pageType()), page.sortOrder(), page.updatedAt());
    }

    private PublicationView toPublicationView(PagePublicationSummary summary) {
        return new PublicationView(summary.publicationId(), summary.configVersion(), summary.publishedAt());
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw ApiException.badRequest("页面配置不是合法的 JSON");
        }
    }

    private ApiException validationError(String message, Map<String, String> errors) {
        Map.Entry<String, String> first = errors.entrySet().iterator().next();
        return ApiException.badRequest(message + "：" + first.getKey() + " " + first.getValue(),
                new LinkedHashMap<>(errors));
    }

    private ApiException slugConflict() {
        return new ApiException(409, "页面标识已被占用", Map.of("slug", "页面标识已被占用"));
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    // 发布历史映射
    public List<PublicationView> mapPublications(List<PagePublicationSummary> summaries) {
        return summaries.stream().map(this::toPublicationView).toList();
    }

    // 已发布页面的 id 集合，公开端过滤导航用
    public Set<String> publishedPageIds(long projectId) {
        Set<String> ids = new LinkedHashSet<>();
        for (SitePage page : sitePageRepository.findByProjectIdOrdered(projectId)) {
            if (STATUS_PUBLISHED.equals(page.status()) && page.publishedVersion() != null) {
                ids.add(page.id());
            }
        }
        return ids;
    }
}
