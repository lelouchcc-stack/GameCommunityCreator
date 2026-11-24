package com.gcc.community.dto;

import java.util.List;

/** 公开社区的详情，包括项目、当前页面和导航。 */
public record PublicCommunityDetail(
        PublicProject project,
        PublicPageView page,
        List<PublicPageSummary> pages,
        List<NavigationItemView> navigation,
        SiteSettingsView settings) {
}
