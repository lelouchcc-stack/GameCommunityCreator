package com.gcc.community.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gcc.community.common.ApiException;
import com.gcc.community.common.PageResult;
import com.gcc.community.domain.Announcement;
import com.gcc.community.domain.CommunityProject;
import com.gcc.community.domain.GameInfo;
import com.gcc.community.domain.PageConfig;
import com.gcc.community.domain.SitePage;
import com.gcc.community.domain.SitePagePublication;
import com.gcc.community.dto.ChangelogView;
import com.gcc.community.dto.CreateIssueRequest;
import com.gcc.community.dto.DocView;
import com.gcc.community.dto.FriendLinkView;
import com.gcc.community.dto.IssueProgressView;
import com.gcc.community.dto.IssueSubmitResult;
import com.gcc.community.dto.NavigationItemView;
import com.gcc.community.dto.PublicCommunityDetail;
import com.gcc.community.dto.PublicCommunitySummary;
import com.gcc.community.dto.PublicPageSummary;
import com.gcc.community.dto.PublicPageView;
import com.gcc.community.dto.PublicProject;
import com.gcc.community.dto.SiteSettingsView;
import com.gcc.community.dto.TeamMemberView;
import com.gcc.community.repository.AnnouncementRepository;
import com.gcc.community.repository.CommunityProjectRepository;
import com.gcc.community.repository.GameInfoRepository;
import com.gcc.community.repository.SitePagePublicationRepository;
import com.gcc.community.repository.SitePageRepository;

/** 公开端的业务，只读已发布的内容。 */
@Service
public class PublicCommunityService {

    private static final String NOT_AVAILABLE = "该社区不存在或尚未发布";
    private static final String PAGE_NOT_AVAILABLE = "该页面不存在或尚未发布";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private CommunityProjectRepository projectRepository;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private SitePagePublicationRepository sitePagePublicationRepository;
    @Autowired
    private GameInfoRepository gameInfoRepository;
    @Autowired
    private PageConfigService pageConfigService;
    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private NavigationService navigationService;
    @Autowired
    private SiteSettingsService siteSettingsService;
    @Autowired
    private ChangelogService changelogService;
    @Autowired
    private DocService docService;
    @Autowired
    private TeamMemberService teamMemberService;
    @Autowired
    private FriendLinkService friendLinkService;
    @Autowired
    private IssueService issueService;

    // 站点列表和站点信息

    // 已发布的站点列表
    public List<PublicCommunitySummary> listCommunities() {
        List<PublicCommunitySummary> summaries = new ArrayList<>();
        for (CommunityProject project : projectRepository.findAllPublished()) {
            Optional<SitePage> home = sitePageRepository.findHomePage(project.id());
            if (home.isEmpty()) {
                continue;
            }
            Optional<SitePagePublication> publication =
                    sitePagePublicationRepository.findLatestByPageId(home.get().id());
            if (publication.isEmpty()) {
                continue;
            }
            String coverUrl = gameInfoRepository.findByProjectId(project.id())
                    .map(GameInfo::coverUrl)
                    .orElse("");
            summaries.add(new PublicCommunitySummary(project.id(), project.name(), project.slug(),
                    project.gameName(), project.description(), coverUrl,
                    publication.get().configVersion(), publication.get().publishedAt()));
        }
        summaries.sort(Comparator.comparing(PublicCommunitySummary::publishedAt).reversed()
                .thenComparing(PublicCommunitySummary::id, Comparator.reverseOrder()));
        return summaries;
    }

    // 站点首页和上下文
    public PublicCommunityDetail getCommunity(String slug) {
        ResolvedSite site = resolve(slug);
        return buildDetail(site, site.homePage());
    }

    // 按 slug 取页面
    public PublicCommunityDetail getPage(String slug, String pageSlug) {
        ResolvedSite site = resolve(slug);
        SitePage page = site.publishedPages().stream()
                .filter(candidate -> candidate.slug().equals(pageSlug))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound(PAGE_NOT_AVAILABLE));
        return buildDetail(site, page);
    }

    public GameInfo getGameInfo(String slug) {
        ResolvedSite site = resolve(slug);
        return gameInfoRepository.findByProjectId(site.project().id())
                .orElseThrow(() -> ApiException.notFound("游戏信息不存在"));
    }

    public PageResult<Announcement> listAnnouncements(String slug, int pageNum, int pageSize) {
        ResolvedSite site = resolve(slug);
        return announcementService.list(site.project().id(), pageNum, pageSize);
    }

    // 内容模块

    // 更新日志
    public PageResult<ChangelogView> listChangelogs(String slug, int pageNum, int pageSize) {
        ResolvedSite site = resolve(slug);
        return changelogService.publicList(site.project().id(), pageNum, pageSize);
    }

    // 更新日志详情
    public ChangelogView getChangelog(String slug, long entryId) {
        ResolvedSite site = resolve(slug);
        return changelogService.publicDetail(site.project().id(), entryId);
    }

    // 文档列表，可以按分类过滤
    public PageResult<DocView> listDocs(String slug, int pageNum, int pageSize, String category) {
        ResolvedSite site = resolve(slug);
        return docService.publicList(site.project().id(), pageNum, pageSize, category);
    }

    // 文档详情
    public DocView getDoc(String slug, String docSlug) {
        ResolvedSite site = resolve(slug);
        return docService.publicDetail(site.project().id(), docSlug);
    }

    public List<TeamMemberView> listTeam(String slug) {
        ResolvedSite site = resolve(slug);
        return teamMemberService.publicList(site.project().id());
    }

    // 友情链接，只要启用的
    public List<FriendLinkView> listLinks(String slug) {
        ResolvedSite site = resolve(slug);
        return friendLinkService.publicList(site.project().id());
    }

    // 公开提交问题
    public IssueSubmitResult submitIssue(String slug, CreateIssueRequest request) {
        ResolvedSite site = resolve(slug);
        return issueService.submit(site.project().id(), request);
    }

    // 问题处理进展
    public List<IssueProgressView> listIssueProgress(String slug) {
        ResolvedSite site = resolve(slug);
        return issueService.publicProgress(site.project().id());
    }

    // 公告详情
    public Announcement getNews(String slug, long announcementId) {
        ResolvedSite site = resolve(slug);
        Long projectId = announcementRepository.findProjectIdById(announcementId)
                .orElseThrow(() -> ApiException.notFound("该资讯不存在"));
        if (projectId != site.project().id()) {
            throw ApiException.notFound("该资讯不存在");
        }
        return announcementRepository.findById(announcementId)
                .orElseThrow(() -> ApiException.notFound("该资讯不存在"));
    }

    // 解析与组装

    // 找到公开的站点，找不到统一 404
    private ResolvedSite resolve(String slug) {
        CommunityProject project = projectRepository.findBySlug(slug)
                .filter(candidate -> STATUS_PUBLISHED.equals(candidate.status()))
                .orElseThrow(() -> ApiException.notFound(NOT_AVAILABLE));

        List<SitePage> allPages = sitePageRepository.findByProjectIdOrdered(project.id());
        Map<String, SitePagePublication> latestPublications = new LinkedHashMap<>();
        for (SitePagePublication publication : sitePagePublicationRepository.findAllByProjectId(project.id())) {
            latestPublications.putIfAbsent(publication.pageId(), publication);
        }
        List<SitePage> publishedPages = allPages.stream()
                .filter(page -> STATUS_PUBLISHED.equals(page.status())
                        && latestPublications.containsKey(page.id()))
                .toList();
        SitePage homePage = publishedPages.stream()
                .filter(page -> PageTypes.HOME.equals(page.pageType()))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound(NOT_AVAILABLE));
        return new ResolvedSite(project, allPages, publishedPages, homePage, latestPublications);
    }

    private PublicCommunityDetail buildDetail(ResolvedSite site, SitePage selected) {
        CommunityProject project = site.project();
        SitePagePublication publication = site.latestPublications().get(selected.id());
        if (publication == null) {
            throw ApiException.notFound(PAGE_NOT_AVAILABLE);
        }
        PageConfig config = pageConfigService.parsePublishedConfig(publication);
        PublicProject publicProject = new PublicProject(project.id(), project.name(), project.slug(),
                project.gameName(), project.description());
        PublicPageView page = new PublicPageView(selected.id(), selected.name(), selected.slug(),
                selected.pageType(), publication.configVersion(), publication.publishedAt(), config);

        List<PublicPageSummary> pageSummaries = site.publishedPages().stream()
                .map(row -> new PublicPageSummary(row.id(), row.name(), row.slug(), row.pageType()))
                .toList();

        Map<String, String> publishedStatuses = new LinkedHashMap<>();
        for (SitePage row : site.publishedPages()) {
            publishedStatuses.put(row.id(), STATUS_PUBLISHED);
        }
        List<NavigationItemView> navigation = navigationService
                .filterForPublic(project.id(), publishedStatuses.keySet());

        SiteSettingsView settings = new SiteSettingsView(
                siteSettingsService.themeModeForPublic(project.id()));
        return new PublicCommunityDetail(publicProject, page, pageSummaries, navigation, settings);
    }

    // 解析结果
    private record ResolvedSite(
            CommunityProject project,
            List<SitePage> allPages,
            List<SitePage> publishedPages,
            SitePage homePage,
            Map<String, SitePagePublication> latestPublications) {
    }
}
