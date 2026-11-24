package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcc.community.common.ApiException;
import com.gcc.community.domain.CommunityProject;
import com.gcc.community.domain.PageConfig;
import com.gcc.community.domain.SitePage;
import com.gcc.community.domain.SitePageDraft;
import com.gcc.community.domain.SitePagePublication;
import com.gcc.community.dto.PageEnvelope;
import com.gcc.community.dto.PageEnvelopeV2;
import com.gcc.community.dto.PublicationView;
import com.gcc.community.dto.PublishResult;
import com.gcc.community.dto.SavePageRequest;
import com.gcc.community.repository.SitePageDraftRepository;
import com.gcc.community.repository.SitePagePublicationRepository;
import com.gcc.community.repository.SitePageRepository;
import com.gcc.community.validation.PageConfigValidator;
import com.gcc.community.validation.PageValidationContext;

/** 页面草稿的读取、保存、重置和发布相关业务。 */
@Service
public class PageConfigService {

    public static final String CONFLICT_MESSAGE = "页面配置已被其他操作修改，请刷新后重试";

    @Autowired
    private ProjectService projectService;
    @Autowired
    private SitePageService sitePageService;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private SitePageDraftRepository sitePageDraftRepository;
    @Autowired
    private SitePagePublicationRepository sitePagePublicationRepository;
    @Autowired
    private DefaultPageTemplateService templateService;
    @Autowired
    private PageConfigValidator validator;
    @Autowired
    private ObjectMapper objectMapper;

    // 页面草稿

    // 读取页面草稿
    public PageEnvelopeV2 getPageDraft(long projectId, String pageId) {
        SitePage page = sitePageService.requirePage(projectId, pageId);
        SitePageDraft draft = requireDraft(pageId);
        return toEnvelopeV2(page, draft, parseConfig(draft.configJson()));
    }

    // 保存草稿，版本对不上就 409
    @Transactional
    public PageEnvelopeV2 savePageDraft(long projectId, String pageId, SavePageRequest request) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage page = sitePageService.requirePage(projectId, pageId);
        SitePageDraft draft = requireDraft(pageId);

        int expectedVersion = request.expectedVersion();
        if (draft.configVersion() != expectedVersion) {
            throw ApiException.conflict(CONFLICT_MESSAGE);
        }
        PageValidationContext ctx = projectService.validationContext(project, pageId);
        PageConfig config = validator.validateForSave(request.config(), ctx);
        String configJson = projectService.serialize(config);

        int newVersion = draft.configVersion() + 1;
        LocalDateTime now = LocalDateTime.now();
        int updated = sitePageDraftRepository.updateWithVersionCheck(
                pageId, configJson, newVersion, now, expectedVersion);
        if (updated == 0) {
            // 校验与更新之间被其他请求抢先更新
            throw ApiException.conflict(CONFLICT_MESSAGE);
        }
        sitePageRepository.updateDraftVersion(pageId, newVersion, now);
        return toEnvelopeV2(page, draft.configVersion() + 1, now, config);
    }

    // 按模板重置草稿
    @Transactional
    public PageEnvelopeV2 resetPageDraft(long projectId, String pageId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage page = sitePageService.requirePage(projectId, pageId);
        SitePageDraft existing = sitePageDraftRepository.findByPageId(pageId).orElse(null);
        int currentVersion = existing == null ? 0 : existing.configVersion();
        int newVersion = currentVersion + 1;

        String pageTitle = PageTypes.defaultPageTitle(page.pageType(), project.gameName(), page.name());
        PageValidationContext ctx = projectService.validationContext(project, pageId);
        PageConfig config = templateService.render(page.templateKey(),
                new DefaultPageTemplateService.Placeholders(page.id(), project.gameName(), project.slug(),
                        pageTitle), ctx);
        String configJson = projectService.serialize(config);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            sitePageDraftRepository.insert(new SitePageDraft(pageId, configJson, newVersion, now));
        } else {
            sitePageDraftRepository.update(pageId, configJson, newVersion, now);
        }
        sitePageRepository.updateDraftVersion(pageId, newVersion, now);
        return toEnvelopeV2(page, newVersion, now, config);
    }

    public List<PublicationView> listPagePublications(long projectId, String pageId) {
        return sitePageService.listPublications(projectId, pageId);
    }

    // 解析已发布快照里的配置
    public PageConfig parsePublishedConfig(SitePagePublication publication) {
        try {
            return objectMapper.readValue(publication.configJson(), PageConfig.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("已发布页面配置无法解析（page_id="
                    + publication.pageId() + "）", ex);
        }
    }

    // 旧版单页接口，都指向 home 页面

    // 读 home 页草稿
    public PageEnvelope getHomeDraft(long projectId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage home = sitePageService.requireHomePage(project);
        SitePageDraft draft = requireDraft(home.id());
        return new PageEnvelope(projectId, draft.configVersion(), draft.updatedAt(),
                parseConfig(draft.configJson()));
    }

    // 保存 home 页草稿
    @Transactional
    public PageEnvelope saveHomeDraft(long projectId, SavePageRequest request) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage home = sitePageService.requireHomePage(project);
        PageEnvelopeV2 envelope = savePageDraft(projectId, home.id(), request);
        return new PageEnvelope(projectId, envelope.configVersion(), envelope.updatedAt(), envelope.config());
    }

    // 重置 home 页草稿
    @Transactional
    public PageEnvelope resetHomeDraft(long projectId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage home = sitePageService.requireHomePage(project);
        PageEnvelopeV2 envelope = resetPageDraft(projectId, home.id());
        return new PageEnvelope(projectId, envelope.configVersion(), envelope.updatedAt(), envelope.config());
    }

    // 发布 home 页
    @Transactional
    public PublishResult publishHome(long projectId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage home = sitePageService.requireHomePage(project);
        var result = sitePageService.publishPage(project.id(), home.id());
        return new PublishResult(result.publicationId(), result.version(), result.publishedAt(),
                result.publicPath());
    }

    // home 页发布历史
    public List<PublicationView> listHomePublications(long projectId) {
        CommunityProject project = projectService.requireOwnedProject(projectId);
        SitePage home = sitePageService.requireHomePage(project);
        return sitePageService.listPublications(projectId, home.id());
    }

    // 内部工具

    private SitePageDraft requireDraft(String pageId) {
        return sitePageDraftRepository.findByPageId(pageId)
                .orElseThrow(() -> ApiException.notFound("页面配置不存在"));
    }

    private PageConfig parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, PageConfig.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("页面草稿配置无法解析", ex);
        }
    }

    private PageEnvelopeV2 toEnvelopeV2(SitePage page, SitePageDraft draft, PageConfig config) {
        return toEnvelopeV2(page, draft.configVersion(), draft.updatedAt(), config);
    }

    private PageEnvelopeV2 toEnvelopeV2(SitePage page, int configVersion, LocalDateTime updatedAt,
            PageConfig config) {
        return new PageEnvelopeV2(page.id(), page.projectId(), page.slug(), page.pageType(),
                configVersion, updatedAt, config);
    }
}
