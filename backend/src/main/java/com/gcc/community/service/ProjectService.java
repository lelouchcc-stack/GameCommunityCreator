package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcc.community.common.ApiException;
import com.gcc.community.domain.CommunityProject;
import com.gcc.community.domain.GameInfo;
import com.gcc.community.domain.PageConfig;
import com.gcc.community.domain.ProjectPageStats;
import com.gcc.community.domain.SitePage;
import com.gcc.community.domain.SitePageDraft;
import com.gcc.community.dto.CreateProjectRequest;
import com.gcc.community.dto.ProjectView;
import com.gcc.community.dto.UpdateProjectRequest;
import com.gcc.community.repository.AnnouncementRepository;
import com.gcc.community.repository.ChangelogRepository;
import com.gcc.community.repository.CommunityProjectRepository;
import com.gcc.community.repository.DocArticleRepository;
import com.gcc.community.repository.FriendLinkRepository;
import com.gcc.community.repository.GameInfoRepository;
import com.gcc.community.repository.IssueReportRepository;
import com.gcc.community.repository.SiteNavigationRepository;
import com.gcc.community.repository.SitePageDraftRepository;
import com.gcc.community.repository.SitePagePublicationRepository;
import com.gcc.community.repository.SitePageRepository;
import com.gcc.community.repository.SiteSettingsRepository;
import com.gcc.community.repository.TeamMemberRepository;
import com.gcc.community.security.CurrentUserProvider;
import com.gcc.community.validation.PageValidationContext;

/** 社区项目的业务，包括项目增删改查和创建时的默认数据。 */
@Service
public class    ProjectService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String FORBIDDEN_MESSAGE = "无权访问该资源";

    // 新建站点时自动建的 6 个默认页面
    private static final List<PageSpec> DEFAULT_PAGES = List.of(
            new PageSpec("首页", PageTypes.HOME),
            new PageSpec("更新日志", PageTypes.CHANGELOG),
            new PageSpec("公开文档", PageTypes.DOCS),
            new PageSpec("制作团队", PageTypes.TEAM),
            new PageSpec("问题反馈", PageTypes.ISSUE_REPORT),
            new PageSpec("友情链接", PageTypes.LINKS));

    @Autowired
    private CommunityProjectRepository projectRepository;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private SitePageDraftRepository sitePageDraftRepository;
    @Autowired
    private SitePagePublicationRepository sitePagePublicationRepository;
    @Autowired
    private SiteSettingsRepository siteSettingsRepository;
    @Autowired
    private SiteNavigationRepository siteNavigationRepository;
    @Autowired
    private GameInfoRepository gameInfoRepository;
    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private DocArticleRepository docArticleRepository;
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    @Autowired
    private IssueReportRepository issueReportRepository;
    @Autowired
    private FriendLinkRepository friendLinkRepository;
    @Autowired
    private DefaultPageTemplateService templateService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CurrentUserProvider currentUserProvider;

    // 只返回当前用户自己的项目
    public List<ProjectView> list() {
        long ownerUserId = currentUserProvider.requireUserId();
        return projectRepository.findAllWithPageStatsByOwner(ownerUserId).stream()
                .map(this::toView).toList();
    }

    public ProjectView get(long id) {
        return toView(requireOwnedWithStats(id));
    }

    // 创建项目。默认页面、导航、设置和游戏信息都在同一个事务里建好
    @Transactional
    public ProjectView create(CreateProjectRequest request) {
        long ownerUserId = currentUserProvider.requireUserId();
        String slug = request.slug();
        if (projectRepository.existsBySlug(slug)) {
            throw slugConflict();
        }
        String gameName = request.gameName();
        String description = normalize(request.description());
        LocalDateTime now = LocalDateTime.now();
        CommunityProject project = new CommunityProject(0L, request.name(), slug, gameName,
                description, STATUS_DRAFT, ownerUserId, now, now);
        long projectId;
        try {
            projectId = projectRepository.insert(project);
        } catch (DuplicateKeyException ex) {
            // 并发创建同一 slug 时由唯一键兜底
            throw slugConflict();
        }

        // 默认页面
        Map<String, String> statuses = new LinkedHashMap<>();
        Map<String, SitePage> pagesByType = new LinkedHashMap<>();
        for (int i = 0; i < DEFAULT_PAGES.size(); i++) {
            PageSpec spec = DEFAULT_PAGES.get(i);
            String pageId = PageTypes.HOME.equals(spec.pageType())
                    ? PageTypes.homePageId(projectId)
                    : PageTypes.modulePageId(spec.pageType(), projectId);
            String pageSlug = PageTypes.FIXED_SLUG_BY_TYPE.get(spec.pageType());
            String templateKey = DefaultPageTemplateService.templateKeyForPageType(spec.pageType());
            SitePage page = new SitePage(pageId, projectId, spec.name(), pageSlug, spec.pageType(),
                    templateKey, STATUS_DRAFT, i, 1, null, now, now);
            sitePageRepository.insert(page);
            pagesByType.put(spec.pageType(), page);
            statuses.put(pageId, STATUS_DRAFT);
        }
        for (PageSpec spec : DEFAULT_PAGES) {
            SitePage page = pagesByType.get(spec.pageType());
            String pageTitle = PageTypes.defaultPageTitle(spec.pageType(), gameName, spec.name());
            PageValidationContext ctx = new PageValidationContext(slug, page.id(), statuses);
            PageConfig config = templateService.render(page.templateKey(),
                    new DefaultPageTemplateService.Placeholders(page.id(), gameName, slug, pageTitle), ctx);
            sitePageDraftRepository.insert(
                    new SitePageDraft(page.id(), serialize(config), 1, now));
        }

        // 默认导航
        int headerSort = 0;
        for (PageSpec spec : DEFAULT_PAGES) {
            SitePage page = pagesByType.get(spec.pageType());
            siteNavigationRepository.insert(projectId, "header", spec.name(), "page",
                    page.id(), "", headerSort++, true);
        }
        int footerSort = 0;
        for (String pageType : List.of(PageTypes.HOME, PageTypes.ISSUE_REPORT)) {
            SitePage page = pagesByType.get(pageType);
            siteNavigationRepository.insert(projectId, "footer", page.name(), "page",
                    page.id(), "", footerSort++, true);
        }

        // 默认设置
        siteSettingsRepository.insert(projectId, "DARK", now);

        // 游戏信息
        gameInfoRepository.insert(new GameInfo(projectId, gameName, "", description, "", "", ""));

        return get(projectId);
    }

    @Transactional
    public ProjectView update(long id, UpdateProjectRequest request) {
        requireOwnedProject(id);
        LocalDateTime now = LocalDateTime.now();
        projectRepository.update(id, request.name(), request.gameName(), normalize(request.description()), now);
        return get(id);
    }

    // 删除项目，顺便把关联的数据都清掉
    @Transactional
    public void delete(long id) {
        requireOwnedProject(id);
        sitePageDraftRepository.deleteByProjectId(id);
        sitePagePublicationRepository.deleteByProjectId(id);
        sitePageRepository.deleteByProjectId(id);
        siteNavigationRepository.deleteByProjectId(id);
        siteSettingsRepository.deleteByProjectId(id);
        gameInfoRepository.deleteByProjectId(id);
        announcementRepository.deleteByProjectId(id);
        changelogRepository.deleteByProjectId(id);
        docArticleRepository.deleteByProjectId(id);
        teamMemberRepository.deleteByProjectId(id);
        issueReportRepository.deleteByProjectId(id);
        friendLinkRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }

    // 内部用，不检查所有权
    public CommunityProject requireProject(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("社区项目不存在"));
    }

    // 管理端用：项目必须是自己的，不是就 403
    public CommunityProject requireOwnedProject(long id) {
        CommunityProject project = requireProject(id);
        if (!Objects.equals(project.ownerUserId(), currentUserProvider.requireUserId())) {
            throw new ApiException(403, FORBIDDEN_MESSAGE);
        }
        return project;
    }

    // 带页面统计，不检查所有权
    public ProjectPageStats requireWithStats(long id) {
        return projectRepository.findWithPageStatsById(id)
                .orElseThrow(() -> ApiException.notFound("社区项目不存在"));
    }

    public ProjectPageStats requireOwnedWithStats(long id) {
        ProjectPageStats stats = requireWithStats(id);
        if (!Objects.equals(stats.project().ownerUserId(), currentUserProvider.requireUserId())) {
            throw new ApiException(403, FORBIDDEN_MESSAGE);
        }
        return stats;
    }

    // 页面 ID 到状态的映射，校验导航跳转用
    public PageValidationContext validationContext(CommunityProject project, String pageId) {
        Map<String, String> statuses = new LinkedHashMap<>();
        for (SitePage page : sitePageRepository.findByProjectIdOrdered(project.id())) {
            statuses.put(page.id(), page.status());
        }
        return new PageValidationContext(project.slug(), pageId, statuses);
    }

    public String serialize(PageConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("页面配置序列化失败", ex);
        }
    }

    private ProjectView toView(ProjectPageStats row) {
        CommunityProject project = row.project();
        return new ProjectView(project.id(), project.name(), project.slug(), project.gameName(),
                project.description(), project.status(), row.homePageId(), row.draftVersion(),
                row.publishedVersion(), row.pageCount(), row.publishedPageCount(),
                project.createdAt(), project.updatedAt());
    }

    private ApiException slugConflict() {
        return new ApiException(409, "项目标识已被占用", Map.of("slug", "项目标识已被占用"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    // 默认页面的名字和类型
    private record PageSpec(String name, String pageType) {
    }
}
