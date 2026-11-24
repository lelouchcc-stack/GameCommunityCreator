-- =====================================================================
-- GameCommunityCreator-MVP 演示数据（v3：多页面 + 认证所有权 + Markdown 正文）
-- 用法：MYSQL_PWD=123456 mysql -u root --default-character-set=utf8mb4 gcc_mvp < database/seed.sql
-- 说明：
--   1. 先清空全部演示表，再插入固定 ID 数据；
--      重复执行结果一致，可直接用于演示 / 联调 / 验收。
--   2. 固定 2 个站点：
--      id=1 stellar-drift「星汐边境官方社区」已发布：6 个页面全部 PUBLISHED 且有发布快照，
--        theme_mode=DARK，含更新日志 4 / 公开文档 4 / 团队成员 4 / 友情链接 4 / 问题反馈 3 条演示数据；
--      id=2 echo-garden「回响花园」草稿站点：6 个页面全部 DRAFT、无发布快照，theme_mode=LIGHT。
--   3. 每页草稿 JSON 均由 backend/src/main/resources/templates/pages/*.json 渲染占位符生成，
--      已补 schemaVersion=2 / pageId，可通过 PAGE_SCHEMA v2 全部校验；home 为 v3.2 的 7 组件顺序
--      （hero / about / notice_list / issue_progress / changelog_list / docs_list / footer）。
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 清空演示表（无物理外键，顺序不影响正确性；先子表后父表）
DELETE FROM friend_link;
DELETE FROM issue_report;
DELETE FROM team_member;
DELETE FROM doc_article;
DELETE FROM changelog;
DELETE FROM site_navigation;
DELETE FROM site_settings;
DELETE FROM site_page_publication;
DELETE FROM site_page_draft;
DELETE FROM site_page;
DELETE FROM announcement;
DELETE FROM game_info;
DELETE FROM community_project;

-- 2. 重置自增（会自愈为当前最大 ID + 1）
ALTER TABLE site_page_publication AUTO_INCREMENT = 1;
ALTER TABLE site_navigation AUTO_INCREMENT = 1;
ALTER TABLE changelog AUTO_INCREMENT = 1;
ALTER TABLE doc_article AUTO_INCREMENT = 1;
ALTER TABLE team_member AUTO_INCREMENT = 1;
ALTER TABLE issue_report AUTO_INCREMENT = 1;
ALTER TABLE friend_link AUTO_INCREMENT = 1;
ALTER TABLE announcement AUTO_INCREMENT = 1;
ALTER TABLE community_project AUTO_INCREMENT = 1;

-- 3. 站点项目
--    v3：owner_user_id 留空（NULL），由后端 BootstrapAccountInitializer 用 APP_BOOTSTRAP_* 认领给初始开发者。
INSERT INTO community_project (id, name, slug, game_name, description, status, created_at, updated_at) VALUES (1, '星汐边境官方社区', 'stellar-drift', '星汐边境', '面向独立游戏《星汐边境》的玩家社区，分享边境站建造、星域探索与剧情讨论。', 'PUBLISHED', '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO community_project (id, name, slug, game_name, description, status, created_at, updated_at) VALUES (2, '回响花园', 'echo-garden', '回响花园', '独立叙事解谜《回响花园》的官方玩家社区，交流记忆碎片、花园谜题与故事解读。', 'DRAFT', '2025-05-10 14:20:00', '2025-05-12 16:45:00');

-- 4. 游戏信息
INSERT INTO game_info (project_id, name, tagline, description, cover_url, platforms, release_status) VALUES (1, '星汐边境', '在星海尽头，建造属于你的边境站', '《星汐边境》是一款以星海拓荒为主题的建造生存游戏。你将驾驶自己的「漂流号」穿越未命名的星域，在潮汐风暴与废弃遗迹之间搭建边境站，用采集到的星尘点亮归航的信标。每一次停泊，都是一段新故事的开端。', '', 'PC / Nintendo Switch', 'EA 抢先体验');
INSERT INTO game_info (project_id, name, tagline, description, cover_url, platforms, release_status) VALUES (2, '回响花园', '把记忆种进花园，让它在回响里开花', '《回响花园》是一款以「声音与记忆」为核心的叙事解谜游戏。在一座会回应你的花园里，用收集到的回响唤醒沉睡的植物，拼出属于你的故事结局。', '', 'PC / macOS', '开发中');

-- 5. 站点页面（id 为稳定 ID，按钮 targetPageId 引用它；home 不可删除）
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_home_1', 1, '首页', 'home', 'home', 'default-home', 'PUBLISHED', 0, 1, 1, '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_changelog_1', 1, '更新日志', 'changelog', 'changelog', 'default-changelog', 'PUBLISHED', 1, 1, 1, '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_docs_1', 1, '公开文档', 'docs', 'docs', 'default-docs', 'PUBLISHED', 2, 1, 1, '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_team_1', 1, '制作团队', 'team', 'team', 'default-team', 'PUBLISHED', 3, 1, 1, '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_report_1', 1, '问题反馈', 'report', 'issue-report', 'default-issue-report', 'PUBLISHED', 4, 1, 1, '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_links_1', 1, '友情链接', 'links', 'links', 'default-links', 'PUBLISHED', 5, 1, 1, '2025-05-01 10:00:00', '2025-05-06 09:30:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_home_2', 2, '首页', 'home', 'home', 'default-home', 'DRAFT', 0, 1, NULL, '2025-05-10 14:20:00', '2025-05-12 16:45:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_changelog_2', 2, '更新日志', 'changelog', 'changelog', 'default-changelog', 'DRAFT', 1, 1, NULL, '2025-05-10 14:20:00', '2025-05-12 16:45:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_docs_2', 2, '公开文档', 'docs', 'docs', 'default-docs', 'DRAFT', 2, 1, NULL, '2025-05-10 14:20:00', '2025-05-12 16:45:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_team_2', 2, '制作团队', 'team', 'team', 'default-team', 'DRAFT', 3, 1, NULL, '2025-05-10 14:20:00', '2025-05-12 16:45:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_report_2', 2, '问题反馈', 'report', 'issue-report', 'default-issue-report', 'DRAFT', 4, 1, NULL, '2025-05-10 14:20:00', '2025-05-12 16:45:00');
INSERT INTO site_page (id, project_id, name, slug, page_type, template_key, status, sort_order, draft_version, published_version, created_at, updated_at) VALUES ('page_links_2', 2, '友情链接', 'links', 'links', 'default-links', 'DRAFT', 5, 1, NULL, '2025-05-10 14:20:00', '2025-05-12 16:45:00');

-- 6. 页面草稿（每页一条；占位符已替换，schemaVersion=2 + pageId 已就位）
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_home_1', '{
  "schemaVersion": 2,
  "pageId": "page_home_1",
  "pageTitle": "星汐边境 官方社区",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_hero",
      "type": "hero",
      "title": "游戏介绍横幅",
      "visible": true,
      "dataSource": "game_info",
      "props": {
        "headline": "",
        "subheadline": "欢迎来到 星汐边境 玩家社区",
        "backgroundImage": "",
        "showGameName": true,
        "align": "center"
      }
    },
    {
      "id": "cmp_default_about",
      "type": "about",
      "title": "游戏简介",
      "visible": true,
      "dataSource": "game_info",
      "props": {
        "body": "",
        "showTagline": true
      }
    },
    {
      "id": "cmp_default_notices",
      "type": "notice_list",
      "title": "社区公告",
      "visible": true,
      "dataSource": "announcements",
      "props": {
        "limit": 3,
        "showPinned": true
      }
    },
    {
      "id": "cmp_default_issue_progress",
      "type": "issue_progress",
      "title": "问题处理进展",
      "visible": true,
      "dataSource": "issues_progress",
      "props": {
        "limit": 5,
        "emptyText": "暂无已公开的问题处理进展"
      }
    },
    {
      "id": "cmp_default_changelogs",
      "type": "changelog_list",
      "title": "更新日志",
      "visible": true,
      "dataSource": "changelogs",
      "props": {
        "limit": 3,
        "showVersion": true
      }
    },
    {
      "id": "cmp_default_docs",
      "type": "docs_list",
      "title": "公开文档",
      "visible": true,
      "dataSource": "docs",
      "props": {
        "limit": 3,
        "category": ""
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-06 09:30:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_changelog_1', '{
  "schemaVersion": 2,
  "pageId": "page_changelog_1",
  "pageTitle": "更新日志",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_changelog",
      "type": "changelog_list",
      "title": "更新日志",
      "visible": true,
      "dataSource": "changelogs",
      "props": {
        "limit": 6,
        "showVersion": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-06 09:30:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_docs_1', '{
  "schemaVersion": 2,
  "pageId": "page_docs_1",
  "pageTitle": "公开文档",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_docs",
      "type": "docs_list",
      "title": "公开文档",
      "visible": true,
      "dataSource": "docs",
      "props": {
        "limit": 8,
        "category": ""
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-06 09:30:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_team_1', '{
  "schemaVersion": 2,
  "pageId": "page_team_1",
  "pageTitle": "制作团队",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_team",
      "type": "team_grid",
      "title": "制作团队",
      "visible": true,
      "dataSource": "team",
      "props": {
        "columns": 3,
        "showRole": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-06 09:30:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_report_1', '{
  "schemaVersion": 2,
  "pageId": "page_report_1",
  "pageTitle": "问题反馈",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_issue_form",
      "type": "issue_form",
      "title": "问题汇报",
      "visible": true,
      "dataSource": null,
      "props": {
        "intro": "",
        "categories": "Bug 反馈,体验建议,其他",
        "successMessage": "感谢反馈！我们会尽快处理。"
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-06 09:30:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_links_1', '{
  "schemaVersion": 2,
  "pageId": "page_links_1",
  "pageTitle": "友情链接",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_links",
      "type": "links_grid",
      "title": "友情链接",
      "visible": true,
      "dataSource": "links",
      "props": {
        "columns": 3,
        "showDescription": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-06 09:30:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_home_2', '{
  "schemaVersion": 2,
  "pageId": "page_home_2",
  "pageTitle": "回响花园 官方社区",
  "projectSlug": "echo-garden",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_hero",
      "type": "hero",
      "title": "游戏介绍横幅",
      "visible": true,
      "dataSource": "game_info",
      "props": {
        "headline": "",
        "subheadline": "欢迎来到 回响花园 玩家社区",
        "backgroundImage": "",
        "showGameName": true,
        "align": "center"
      }
    },
    {
      "id": "cmp_default_about",
      "type": "about",
      "title": "游戏简介",
      "visible": true,
      "dataSource": "game_info",
      "props": {
        "body": "",
        "showTagline": true
      }
    },
    {
      "id": "cmp_default_notices",
      "type": "notice_list",
      "title": "社区公告",
      "visible": true,
      "dataSource": "announcements",
      "props": {
        "limit": 3,
        "showPinned": true
      }
    },
    {
      "id": "cmp_default_issue_progress",
      "type": "issue_progress",
      "title": "问题处理进展",
      "visible": true,
      "dataSource": "issues_progress",
      "props": {
        "limit": 5,
        "emptyText": "暂无已公开的问题处理进展"
      }
    },
    {
      "id": "cmp_default_changelogs",
      "type": "changelog_list",
      "title": "更新日志",
      "visible": true,
      "dataSource": "changelogs",
      "props": {
        "limit": 3,
        "showVersion": true
      }
    },
    {
      "id": "cmp_default_docs",
      "type": "docs_list",
      "title": "公开文档",
      "visible": true,
      "dataSource": "docs",
      "props": {
        "limit": 3,
        "category": ""
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 回响花园 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-12 16:45:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_changelog_2', '{
  "schemaVersion": 2,
  "pageId": "page_changelog_2",
  "pageTitle": "更新日志",
  "projectSlug": "echo-garden",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_changelog",
      "type": "changelog_list",
      "title": "更新日志",
      "visible": true,
      "dataSource": "changelogs",
      "props": {
        "limit": 6,
        "showVersion": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 回响花园 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-12 16:45:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_docs_2', '{
  "schemaVersion": 2,
  "pageId": "page_docs_2",
  "pageTitle": "公开文档",
  "projectSlug": "echo-garden",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_docs",
      "type": "docs_list",
      "title": "公开文档",
      "visible": true,
      "dataSource": "docs",
      "props": {
        "limit": 8,
        "category": ""
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 回响花园 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-12 16:45:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_team_2', '{
  "schemaVersion": 2,
  "pageId": "page_team_2",
  "pageTitle": "制作团队",
  "projectSlug": "echo-garden",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_team",
      "type": "team_grid",
      "title": "制作团队",
      "visible": true,
      "dataSource": "team",
      "props": {
        "columns": 3,
        "showRole": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 回响花园 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-12 16:45:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_report_2', '{
  "schemaVersion": 2,
  "pageId": "page_report_2",
  "pageTitle": "问题反馈",
  "projectSlug": "echo-garden",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_issue_form",
      "type": "issue_form",
      "title": "问题汇报",
      "visible": true,
      "dataSource": null,
      "props": {
        "intro": "",
        "categories": "Bug 反馈,体验建议,其他",
        "successMessage": "感谢反馈！我们会尽快处理。"
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 回响花园 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-12 16:45:00');
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at) VALUES ('page_links_2', '{
  "schemaVersion": 2,
  "pageId": "page_links_2",
  "pageTitle": "友情链接",
  "projectSlug": "echo-garden",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_links",
      "type": "links_grid",
      "title": "友情链接",
      "visible": true,
      "dataSource": "links",
      "props": {
        "columns": 3,
        "showDescription": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 回响花园 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', 1, '2025-05-12 16:45:00');

-- 7. 页面发布快照（仅已发布站点 stellar-drift；与草稿同版本）
INSERT INTO site_page_publication (id, page_id, project_id, config_version, config_json, published_at) VALUES (1, 'page_home_1', 1, 1, '{
  "schemaVersion": 2,
  "pageId": "page_home_1",
  "pageTitle": "星汐边境 官方社区",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_hero",
      "type": "hero",
      "title": "游戏介绍横幅",
      "visible": true,
      "dataSource": "game_info",
      "props": {
        "headline": "",
        "subheadline": "欢迎来到 星汐边境 玩家社区",
        "backgroundImage": "",
        "showGameName": true,
        "align": "center"
      }
    },
    {
      "id": "cmp_default_about",
      "type": "about",
      "title": "游戏简介",
      "visible": true,
      "dataSource": "game_info",
      "props": {
        "body": "",
        "showTagline": true
      }
    },
    {
      "id": "cmp_default_notices",
      "type": "notice_list",
      "title": "社区公告",
      "visible": true,
      "dataSource": "announcements",
      "props": {
        "limit": 3,
        "showPinned": true
      }
    },
    {
      "id": "cmp_default_issue_progress",
      "type": "issue_progress",
      "title": "问题处理进展",
      "visible": true,
      "dataSource": "issues_progress",
      "props": {
        "limit": 5,
        "emptyText": "暂无已公开的问题处理进展"
      }
    },
    {
      "id": "cmp_default_changelogs",
      "type": "changelog_list",
      "title": "更新日志",
      "visible": true,
      "dataSource": "changelogs",
      "props": {
        "limit": 3,
        "showVersion": true
      }
    },
    {
      "id": "cmp_default_docs",
      "type": "docs_list",
      "title": "公开文档",
      "visible": true,
      "dataSource": "docs",
      "props": {
        "limit": 3,
        "category": ""
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', '2025-05-06 09:30:00');
INSERT INTO site_page_publication (id, page_id, project_id, config_version, config_json, published_at) VALUES (2, 'page_changelog_1', 1, 1, '{
  "schemaVersion": 2,
  "pageId": "page_changelog_1",
  "pageTitle": "更新日志",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_changelog",
      "type": "changelog_list",
      "title": "更新日志",
      "visible": true,
      "dataSource": "changelogs",
      "props": {
        "limit": 6,
        "showVersion": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', '2025-05-06 09:30:00');
INSERT INTO site_page_publication (id, page_id, project_id, config_version, config_json, published_at) VALUES (3, 'page_docs_1', 1, 1, '{
  "schemaVersion": 2,
  "pageId": "page_docs_1",
  "pageTitle": "公开文档",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_docs",
      "type": "docs_list",
      "title": "公开文档",
      "visible": true,
      "dataSource": "docs",
      "props": {
        "limit": 8,
        "category": ""
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', '2025-05-06 09:30:00');
INSERT INTO site_page_publication (id, page_id, project_id, config_version, config_json, published_at) VALUES (4, 'page_team_1', 1, 1, '{
  "schemaVersion": 2,
  "pageId": "page_team_1",
  "pageTitle": "制作团队",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_team",
      "type": "team_grid",
      "title": "制作团队",
      "visible": true,
      "dataSource": "team",
      "props": {
        "columns": 3,
        "showRole": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', '2025-05-06 09:30:00');
INSERT INTO site_page_publication (id, page_id, project_id, config_version, config_json, published_at) VALUES (5, 'page_report_1', 1, 1, '{
  "schemaVersion": 2,
  "pageId": "page_report_1",
  "pageTitle": "问题反馈",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_issue_form",
      "type": "issue_form",
      "title": "问题汇报",
      "visible": true,
      "dataSource": null,
      "props": {
        "intro": "",
        "categories": "Bug 反馈,体验建议,其他",
        "successMessage": "感谢反馈！我们会尽快处理。"
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', '2025-05-06 09:30:00');
INSERT INTO site_page_publication (id, page_id, project_id, config_version, config_json, published_at) VALUES (6, 'page_links_1', 1, 1, '{
  "schemaVersion": 2,
  "pageId": "page_links_1",
  "pageTitle": "友情链接",
  "projectSlug": "stellar-drift",
  "theme": {
    "primaryColor": "#5B8CFF",
    "accentColor": "#9B5CFF",
    "backgroundColor": "#0B1020",
    "surfaceColor": "#151B33",
    "textColor": "#E8ECFF",
    "mutedTextColor": "#9AA6CC",
    "fontFamily": "system",
    "borderRadius": 16
  },
  "components": [
    {
      "id": "cmp_default_links",
      "type": "links_grid",
      "title": "友情链接",
      "visible": true,
      "dataSource": "links",
      "props": {
        "columns": 3,
        "showDescription": true
      }
    },
    {
      "id": "cmp_default_footer",
      "type": "footer",
      "title": "页脚",
      "visible": true,
      "dataSource": null,
      "props": {
        "copyright": "© 2025 星汐边境 社区",
        "note": "由 GameCommunityCreator 可视化搭建"
      }
    }
  ]
}', '2025-05-06 09:30:00');

-- 8. 站点设置（深浅色模式）
INSERT INTO site_settings (project_id, theme_mode, updated_at) VALUES (1, 'DARK', '2025-05-06 09:30:00');
INSERT INTO site_settings (project_id, theme_mode, updated_at) VALUES (2, 'LIGHT', '2025-05-12 16:45:00');

-- 9. 站点导航（header 6 条 + footer 2 条；targetType=page 指向真实页面 ID）
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'header', '首页', 'page', 'page_home_1', '', 0, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'header', '更新日志', 'page', 'page_changelog_1', '', 1, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'header', '公开文档', 'page', 'page_docs_1', '', 2, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'header', '制作团队', 'page', 'page_team_1', '', 3, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'header', '问题反馈', 'page', 'page_report_1', '', 4, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'header', '友情链接', 'page', 'page_links_1', '', 5, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'footer', '首页', 'page', 'page_home_1', '', 0, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (1, 'footer', '问题反馈', 'page', 'page_report_1', '', 1, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'header', '首页', 'page', 'page_home_2', '', 0, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'header', '更新日志', 'page', 'page_changelog_2', '', 1, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'header', '公开文档', 'page', 'page_docs_2', '', 2, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'header', '制作团队', 'page', 'page_team_2', '', 3, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'header', '问题反馈', 'page', 'page_report_2', '', 4, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'header', '友情链接', 'page', 'page_links_2', '', 5, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'footer', '首页', 'page', 'page_home_2', '', 0, 1);
INSERT INTO site_navigation (project_id, location, title, target_type, target_page_id, url, sort_order, visible) VALUES (2, 'footer', '问题反馈', 'page', 'page_report_2', '', 1, 1);

-- 10. 更新日志（4 条，均 PUBLISHED；v3 正文为 Markdown，content 与 body_markdown 同值）
INSERT INTO changelog (id, project_id, version, title, content, body_markdown, release_date, status, created_at, updated_at) VALUES (1, 1, 'v0.9.2', '潮汐回响版本更新', '## 新增内容

- 主线章节「潮汐回响」
- 三座可建造星港设施：潮汐观测站、自动补给港、信标灯塔

## 修复

- 边境站电力网络在极端天气下异常断开

## 调整

- 重新平衡星尘采集效率

> 更新后首次进入游戏会进行资源索引重建，请预留约 2 分钟。', '## 新增内容

- 主线章节「潮汐回响」
- 三座可建造星港设施：潮汐观测站、自动补给港、信标灯塔

## 修复

- 边境站电力网络在极端天气下异常断开

## 调整

- 重新平衡星尘采集效率

> 更新后首次进入游戏会进行资源索引重建，请预留约 2 分钟。', '2025-05-06', 'PUBLISHED', '2025-05-06 09:00:00', '2025-05-06 09:00:00');
INSERT INTO changelog (id, project_id, version, title, content, body_markdown, release_date, status, created_at, updated_at) VALUES (2, 1, 'v0.9.1', '星港设施与电力网络修复', '## 新增

- 星港设施「潮汐观测站」「自动补给港」

## 优化

- 修复电力网络在风暴季的离线问题
- 优化建造模式镜头手感
- 撤销操作支持长按连续撤销', '## 新增

- 星港设施「潮汐观测站」「自动补给港」

## 优化

- 修复电力网络在风暴季的离线问题
- 优化建造模式镜头手感
- 撤销操作支持长按连续撤销', '2025-04-18', 'PUBLISHED', '2025-04-18 10:00:00', '2025-04-18 10:00:00');
INSERT INTO changelog (id, project_id, version, title, content, body_markdown, release_date, status, created_at, updated_at) VALUES (3, 1, 'v0.9.0', '边境站开放建造', '## 边境站建造系统正式开放

采集、供电、居住三大模块全部可自由拼接：

| 模块 | 功能 | 解锁条件 |
| --- | --- | --- |
| 采集 | 星尘采集与仓储 | 完成教学 |
| 供电 | 电力网络与储能 | 修复电力核心 |
| 居住 | 生命维持与休息 | 建成第一座补给港 |

- 新增黄昏 / 夜间光照
- 星尘灯会在风暴来临时一盏盏亮起

### 版本定位

这是「星汐边境」EA 阶段最重要的一个版本。', '## 边境站建造系统正式开放

采集、供电、居住三大模块全部可自由拼接：

| 模块 | 功能 | 解锁条件 |
| --- | --- | --- |
| 采集 | 星尘采集与仓储 | 完成教学 |
| 供电 | 电力网络与储能 | 修复电力核心 |
| 居住 | 生命维持与休息 | 建成第一座补给港 |

- 新增黄昏 / 夜间光照
- 星尘灯会在风暴来临时一盏盏亮起

### 版本定位

这是「星汐边境」EA 阶段最重要的一个版本。', '2025-03-30', 'PUBLISHED', '2025-03-30 12:00:00', '2025-03-30 12:00:00');
INSERT INTO changelog (id, project_id, version, title, content, body_markdown, release_date, status, created_at, updated_at) VALUES (4, 1, 'v0.8.5', '星尘采集平衡性调整', '## 平衡性调整

- 下调蓝色星尘的刷新间隔
- 提高遗迹区稀有星尘的掉落概率
- 采集工具耐久消耗降低 **20%**，让探索节奏更连贯', '## 平衡性调整

- 下调蓝色星尘的刷新间隔
- 提高遗迹区稀有星尘的掉落概率
- 采集工具耐久消耗降低 **20%**，让探索节奏更连贯', '2025-03-08', 'PUBLISHED', '2025-03-08 18:00:00', '2025-03-08 18:00:00');

-- 11. 公开文档（4 条，均 PUBLISHED，分类 3 种；v3 正文为 Markdown）
INSERT INTO doc_article (id, project_id, title, category, content, body_markdown, slug, status, created_at, updated_at) VALUES (1, 1, '入门指南', '上手', '# 入门指南

第一次登上「漂流号」时，建议先完成船上的三分钟教学：

1. 移动与视角
2. 采集星尘
3. 搭建第一座采集模块

## 抵达星港之后

- 优先修复电力核心
- 再搭建采集模块
- 蓝色星尘是前期唯一的通用货币，遇到就顺手收下

> 想快速了解边境站的运转逻辑，可以继续阅读《边境站建造基础》。', '# 入门指南

第一次登上「漂流号」时，建议先完成船上的三分钟教学：

1. 移动与视角
2. 采集星尘
3. 搭建第一座采集模块

## 抵达星港之后

- 优先修复电力核心
- 再搭建采集模块
- 蓝色星尘是前期唯一的通用货币，遇到就顺手收下

> 想快速了解边境站的运转逻辑，可以继续阅读《边境站建造基础》。', 'getting-started', 'PUBLISHED', '2025-05-02 09:00:00', '2025-05-02 09:00:00');
INSERT INTO doc_article (id, project_id, title, category, content, body_markdown, slug, status, created_at, updated_at) VALUES (2, 1, '边境站建造基础', '建造', '# 边境站建造基础

边境站由三类模块组成：

| 模块 | 作用 | 耗电 |
| --- | --- | --- |
| 采集 | 采集星尘与资源 | 低 |
| 供电 | 提供并储存电力 | — |
| 居住 | 生命维持、休息 | 中 |

## 推荐布局

把采集区与居住区拆成两条独立电力线：

```text
发电机 → 储能罐 → 采集模块
              └→ 居住模块（极端天气优先保供电）
```

## 注意

- 极端天气只保留居住区供电，能显著延长生存时间
- 星尘灯与生命维持装置都会消耗电力', '# 边境站建造基础

边境站由三类模块组成：

| 模块 | 作用 | 耗电 |
| --- | --- | --- |
| 采集 | 采集星尘与资源 | 低 |
| 供电 | 提供并储存电力 | — |
| 居住 | 生命维持、休息 | 中 |

## 推荐布局

把采集区与居住区拆成两条独立电力线：

```text
发电机 → 储能罐 → 采集模块
              └→ 居住模块（极端天气优先保供电）
```

## 注意

- 极端天气只保留居住区供电，能显著延长生存时间
- 星尘灯与生命维持装置都会消耗电力', 'station-building', 'PUBLISHED', '2025-04-20 14:00:00', '2025-04-20 14:00:00');
INSERT INTO doc_article (id, project_id, title, category, content, body_markdown, slug, status, created_at, updated_at) VALUES (3, 1, '星尘与资源循环', '玩法', '# 星尘与资源循环

星尘分三档：

1. **蓝色**：基础建造
2. **紫色**：设施升级
3. **金色**：剧情推进

## 资源循环

- 采集 → 精炼 → 建材
- 或直接注入信标，换取补给航线

> 遗迹区会刷新稀有星尘，但离开时记得核对返航燃料。', '# 星尘与资源循环

星尘分三档：

1. **蓝色**：基础建造
2. **紫色**：设施升级
3. **金色**：剧情推进

## 资源循环

- 采集 → 精炼 → 建材
- 或直接注入信标，换取补给航线

> 遗迹区会刷新稀有星尘，但离开时记得核对返航燃料。', 'resource-loop', 'PUBLISHED', '2025-04-05 11:30:00', '2025-04-05 11:30:00');
INSERT INTO doc_article (id, project_id, title, category, content, body_markdown, slug, status, created_at, updated_at) VALUES (4, 1, '常见问题速查', '上手', '# 常见问题速查

## 更新后卡在「资源索引重建」？

属于正常流程，预留约 2 分钟即可，不要强制关闭。

## 电力网络在极端天气断开？

检查是否有单点供电，按《边境站建造基础》拆成双电力线。

## 社区发帖被下架？

请先阅读社区版规；剧透内容务必在标题标注「剧透」。', '# 常见问题速查

## 更新后卡在「资源索引重建」？

属于正常流程，预留约 2 分钟即可，不要强制关闭。

## 电力网络在极端天气断开？

检查是否有单点供电，按《边境站建造基础》拆成双电力线。

## 社区发帖被下架？

请先阅读社区版规；剧透内容务必在标题标注「剧透」。', 'faq', 'PUBLISHED', '2025-03-15 16:00:00', '2025-03-15 16:00:00');

-- 12. 制作组成员（4 人）
INSERT INTO team_member (id, project_id, name, avatar_url, role, bio, sort_order) VALUES (1, 1, '林澈', '', '制作人', '负责整体玩法与世界观设定，偶尔客串社区客服。', 0);
INSERT INTO team_member (id, project_id, name, avatar_url, role, bio, sort_order) VALUES (2, 1, '沈星', '', '主美术', '边境站与星港设施的视觉设计，喜欢在夜里画星尘灯。', 1);
INSERT INTO team_member (id, project_id, name, avatar_url, role, bio, sort_order) VALUES (3, 1, '顾临', '', '程序', '建造系统与电力网络实现，正在优化存档性能。', 2);
INSERT INTO team_member (id, project_id, name, avatar_url, role, bio, sort_order) VALUES (4, 1, '白露', '', '音频设计', '「潮汐回响」主题曲与全部环境音效的设计者。', 3);

-- 13. 友情链接（4 条，均 enabled）
INSERT INTO friend_link (id, project_id, name, url, description, sort_order, enabled) VALUES (1, 1, 'IndieDB', 'https://www.indiedb.com', '独立游戏资料站，《星汐边境》英文介绍页。', 0, 1);
INSERT INTO friend_link (id, project_id, name, url, description, sort_order, enabled) VALUES (2, 1, 'Steam 商店页', 'https://store.steampowered.com', '《星汐边境》Steam 商店页，含可试玩 Demo。', 1, 1);
INSERT INTO friend_link (id, project_id, name, url, description, sort_order, enabled) VALUES (3, 1, '开发日志', 'https://example.com/stellar-drift/blog', '开发组的版本设计日志，记录每个版本背后的取舍。', 2, 1);
INSERT INTO friend_link (id, project_id, name, url, description, sort_order, enabled) VALUES (4, 1, 'itch.io', 'https://itch.io', '独立游戏发行与试玩平台。', 3, 1);

-- 14. 问题反馈（4 条演示：2 条公开已解决 / 1 条已解决不公开 / 1 条待处理；JSON/Markdown 均为演示数据）
INSERT INTO issue_report (id, project_id, title, category, description, game_version, steps, status, resolution, is_public, created_at, updated_at) VALUES (1, 1, '登录界面偶发闪退', 'Bug 反馈', '在部分安卓设备上，登录界面偶发闪退，重启后恢复正常。', 'v0.9.2', '1. 打开游戏进入登录界面 2. 停留数秒 3. 偶发闪退', 'RESOLVED', '已定位为会话恢复逻辑缺陷，v0.9.3 修复并回归验证通过。', 1, '2025-05-07 10:20:00', '2025-05-08 09:10:00');
INSERT INTO issue_report (id, project_id, title, category, description, game_version, steps, status, resolution, is_public, created_at, updated_at) VALUES (2, 1, '帧率优化建议', '体验建议', '希望优化低配设备的帧率表现，尤其是星港场景。', 'v0.9.2', '', 'RESOLVED', '已加入性能优化计划，v0.9.4 提升低配设备帧率约 15%。', 1, '2025-05-06 20:05:00', '2025-05-08 15:20:00');
INSERT INTO issue_report (id, project_id, title, category, description, game_version, steps, status, resolution, is_public, created_at, updated_at) VALUES (3, 1, '文本错别字', '其他', '主线第二章某句对话存在错别字。', 'v0.9.2', '', 'RESOLVED', '已修正文案。', 0, '2025-05-05 14:00:00', '2025-05-05 18:30:00');
INSERT INTO issue_report (id, project_id, title, category, description, game_version, steps, status, resolution, is_public, created_at, updated_at) VALUES (4, 1, '手柄震动异常', 'Bug 反馈', '使用手柄时震动反馈偶尔持续不停，需要切出界面恢复。', 'v0.9.2', '1. 连接手柄 2. 进入战斗 3. 震动持续', 'PENDING', '', 0, '2025-05-09 11:00:00', '2025-05-09 11:00:00');

-- 15. 公告（stellar-drift 4 条 / echo-garden 3 条，均含置顶；v3 正文为 Markdown）
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (1, 1, '《星汐边境》0.9.2「潮汐回响」版本更新公告', '## 更新内容

- 新增「潮汐回响」主线章节
- 新增三座可建造星港设施
- 修复电力网络在极端天气下异常断开

## 注意事项

更新后首次进入游戏会进行 **资源索引重建**，请预留约 2 分钟。', '## 更新内容

- 新增「潮汐回响」主线章节
- 新增三座可建造星港设施
- 修复电力网络在极端天气下异常断开

## 注意事项

更新后首次进入游戏会进行 **资源索引重建**，请预留约 2 分钟。', 1, '2025-05-06 09:00:00');
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (2, 1, '星汐边境玩家社区正式开张', '## 欢迎来到星汐边境官方社区

这里会同步：

- 版本公告
- 开发日志
- 活动信息

也欢迎大家分享自己的边境站截图与探索故事。发帖前请先阅读社区版规，友善交流。', '## 欢迎来到星汐边境官方社区

这里会同步：

- 版本公告
- 开发日志
- 活动信息

也欢迎大家分享自己的边境站截图与探索故事。发帖前请先阅读社区版规，友善交流。', 0, '2025-05-04 18:00:00');
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (3, 1, '社区版规与发帖指南', '## 社区版规

1. 请勿发布与游戏无关的广告与引流内容
2. 剧透内容请在标题标注「剧透」
3. 搬运他人作品需注明出处
4. 遇到问题优先查阅置顶公告，或在「反馈与建议」板块发帖

> 违规内容会被下架并视情况禁言。', '## 社区版规

1. 请勿发布与游戏无关的广告与引流内容
2. 剧透内容请在标题标注「剧透」
3. 搬运他人作品需注明出处
4. 遇到问题优先查阅置顶公告，或在「反馈与建议」板块发帖

> 违规内容会被下架并视情况禁言。', 0, '2025-05-02 12:00:00');
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (4, 1, '创作者招募：边境电台与同人企划', '## 边境电台 × 同人企划

社区正在筹备「边境电台」播客与同人创作企划，招募：

- 主播
- 美术
- 文案志愿者

有兴趣的玩家可以在本帖下留言，或通过社区邮箱联系我们。', '## 边境电台 × 同人企划

社区正在筹备「边境电台」播客与同人创作企划，招募：

- 主播
- 美术
- 文案志愿者

有兴趣的玩家可以在本帖下留言，或通过社区邮箱联系我们。', 0, '2025-04-28 20:00:00');
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (5, 2, '《回响花园》首次封闭测试招募', '## 首次封闭测试招募

测试内容包括：

- 前三章花园谜题
- 「回响收集」系统

名额有限，请在本帖下按格式留言报名，我们会通过社区私信发送测试资格。', '## 首次封闭测试招募

测试内容包括：

- 前三章花园谜题
- 「回响收集」系统

名额有限，请在本帖下按格式留言报名，我们会通过社区私信发送测试资格。', 1, '2025-05-12 16:40:00');
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (6, 2, '开发日志 01：为什么花园会「回应」你', '## 为什么花园会「回应」你

我们想让声音成为玩家的第二双手：每段回响都记录着一段记忆。

- 玩家的选择会改变花园的形态
- 解谜的节奏更像在照料一座花园

这篇日志聊聊「回响收集」系统的设计取舍。', '## 为什么花园会「回应」你

我们想让声音成为玩家的第二双手：每段回响都记录着一段记忆。

- 玩家的选择会改变花园的形态
- 解谜的节奏更像在照料一座花园

这篇日志聊聊「回响收集」系统的设计取舍。', 0, '2025-05-11 11:00:00');
INSERT INTO announcement (id, project_id, title, content, body_markdown, pinned, published_at) VALUES (7, 2, '社区开张：欢迎来到回响花园', '## 欢迎来到回响花园

这里会同步测试招募、开发日志与玩家作品。

花园刚刚开门，欢迎各位园丁在「解谜讨论」板块分享你的第一段回响。', '## 欢迎来到回响花园

这里会同步测试招募、开发日志与玩家作品。

花园刚刚开门，欢迎各位园丁在「解谜讨论」板块分享你的第一段回响。', 0, '2025-05-10 14:30:00');

-- 16. 自检（可选执行）：应返回 2 / 12 / 6 / 12 / 4 / 4 / 4 / 4 / 3
-- SELECT (SELECT COUNT(*) FROM community_project) AS projects,
--        (SELECT COUNT(*) FROM site_page) AS pages,
--        (SELECT COUNT(*) FROM site_page_publication) AS pubs,
--        (SELECT COUNT(*) FROM site_page_draft) AS drafts,
--        (SELECT COUNT(*) FROM changelog) AS changelogs,
--        (SELECT COUNT(*) FROM doc_article) AS docs,
--        (SELECT COUNT(*) FROM team_member) AS team,
--        (SELECT COUNT(*) FROM friend_link) AS links,
--        (SELECT COUNT(*) FROM issue_report) AS issues;
