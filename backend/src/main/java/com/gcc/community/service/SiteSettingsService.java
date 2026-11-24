package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.domain.SiteSettingsRecord;
import com.gcc.community.dto.SiteSettingsView;
import com.gcc.community.dto.UpdateSettingsRequest;
import com.gcc.community.repository.SiteSettingsRepository;

/** 站点设置的业务。 */
@Service
public class SiteSettingsService {

    public static final String DEFAULT_THEME_MODE = "DARK";

    @Autowired
    private ProjectService projectService;
    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    public SiteSettingsView get(long projectId) {
        projectService.requireOwnedProject(projectId);
        return siteSettingsRepository.findByProjectId(projectId)
                .map(row -> new SiteSettingsView(row.themeMode()))
                .orElseGet(() -> new SiteSettingsView(DEFAULT_THEME_MODE));
    }

    @Transactional
    public SiteSettingsView save(long projectId, UpdateSettingsRequest request) {
        projectService.requireOwnedProject(projectId);
        LocalDateTime now = LocalDateTime.now();
        String themeMode = request.themeMode().strip();
        if (siteSettingsRepository.update(projectId, themeMode, now) == 0) {
            siteSettingsRepository.insert(projectId, themeMode, now);
        }
        return new SiteSettingsView(themeMode);
    }

    // 公开端读主题，没有就用默认的 DARK
    public String themeModeForPublic(long projectId) {
        return siteSettingsRepository.findByProjectId(projectId)
                .map(SiteSettingsRecord::themeMode)
                .orElse(DEFAULT_THEME_MODE);
    }

    // 判断主题模式合不合法
    public static boolean isValidThemeMode(String themeMode) {
        return Set.of("DARK", "LIGHT", "SYSTEM").contains(themeMode);
    }

    // 支持的主题模式
    public static List<String> supportedThemeModes() {
        return List.of("DARK", "LIGHT", "SYSTEM");
    }
}
