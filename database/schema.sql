-- =====================================================================
-- GameCommunityCreator-MVP 数据库建表脚本（v3.2；14 张表）
-- 契约来源：docs/API.md §10、docs/PAGE_SCHEMA.md
-- 特点：
--   1. 可重复执行（CREATE TABLE IF NOT EXISTS），不包含 DROP / TRUNCATE；
--   2. 不使用物理外键：逻辑外键 + 索引 + 应用层事务级联删除；
--   3. 统一 utf8mb4 / InnoDB；时间统一 DATETIME；布尔 TINYINT(1)；
--   4. 页面配置 JSON 使用 MEDIUMTEXT（单条上限 16MB，业务上限 200KB）。
-- 表清单（14 张）：
--   认证：app_user
--   v1 保留：community_project / game_info / announcement
--   多页面：site_page / site_page_draft / site_page_publication / site_settings / site_navigation
--   内容模块：changelog / doc_article / team_member / issue_report / friend_link
-- 历史说明：v1 遗留表 page_config / page_publication 与 v1 保留表 post 已按数据库结构审计清理
--          （见 docs/DATABASE_AUDIT.md）；旧库升级使用 database/migrations/002_multi_page.sql，
--          该脚本读取旧库自带的 v1 遗留表，全新部署不需要也不应创建它们。
-- 执行：MYSQL_PWD=123456 mysql -u root --default-character-set=utf8mb4 gcc_mvp < database/schema.sql
--       （脚本不含 USE，库名由命令行 / 部署环境决定，便于 Docker 部署时换库）
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、v1 保留表（结构不变；v3 增加 owner_user_id / body_markdown）
-- =====================================================================

CREATE TABLE IF NOT EXISTS app_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '开发者账号 ID',
  username      VARCHAR(50)  NOT NULL COMMENT '登录名（3–30，[A-Za-z0-9_-]，全局唯一）',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希（禁止明文）',
  display_name  VARCHAR(60)  NOT NULL COMMENT '显示名称（1–60）',
  created_at    DATETIME     NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '开发者账号（v3：站点所有者）';

CREATE TABLE IF NOT EXISTS community_project (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '社区项目 ID',
  name        VARCHAR(80)  NOT NULL COMMENT '社区名称（1–80）',
  slug        VARCHAR(63)  NOT NULL COMMENT '项目标识（^[a-z0-9][a-z0-9-]{1,62}$，全局唯一）',
  game_name   VARCHAR(80)  NOT NULL COMMENT '游戏名称（1–80）',
  description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '社区简介（0–500）',
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT | PUBLISHED（至少发布过一页）',
  owner_user_id BIGINT       NULL COMMENT '所属开发者账号（v3；逻辑外键 app_user.id，旧数据为 NULL 由 bootstrap 认领）',
  created_at    DATETIME     NOT NULL COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_community_project_slug (slug),
  KEY idx_community_project_updated_at (updated_at),
  KEY idx_community_project_status (status),
  KEY idx_community_project_owner (owner_user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '社区项目（一款游戏 = 一个社区项目）';

CREATE TABLE IF NOT EXISTS game_info (
  project_id     BIGINT        NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  name           VARCHAR(80)   NOT NULL COMMENT '游戏名称（1–80）',
  tagline        VARCHAR(160)  NOT NULL DEFAULT '' COMMENT '一句话简介（≤160）',
  description    VARCHAR(3000) NOT NULL DEFAULT '' COMMENT '游戏简介（≤3000）',
  cover_url      VARCHAR(300)  NOT NULL DEFAULT '' COMMENT '封面图地址（≤300，可为空）',
  platforms      VARCHAR(120)  NOT NULL DEFAULT '' COMMENT '平台（≤120）',
  release_status VARCHAR(40)   NOT NULL DEFAULT '' COMMENT '发行状态（≤40，如 EA / 已发售）',
  PRIMARY KEY (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '游戏信息（每个社区项目一条）';

CREATE TABLE IF NOT EXISTS announcement (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告 ID',
  project_id   BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  title        VARCHAR(120) NOT NULL COMMENT '公告标题（1–120）',
  content       TEXT        NOT NULL COMMENT '公告正文（兼容旧客户端的同值别名；新代码以 body_markdown 为主）',
  body_markdown MEDIUMTEXT  NULL COMMENT '公告正文 Markdown（v3；读取优先，空回退 content）',
  pinned        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否置顶：1 置顶，0 普通',
  published_at DATETIME     NOT NULL COMMENT '发布时间',
  PRIMARY KEY (id),
  KEY idx_announcement_project_pinned_published (project_id, pinned, published_at),
  KEY idx_announcement_project_published (project_id, published_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '社区公告';

-- =====================================================================
-- 二、v2 新增表：多页面模型
-- =====================================================================

CREATE TABLE IF NOT EXISTS site_page (
  id                VARCHAR(40)  NOT NULL COMMENT '页面稳定 ID（^page_[a-z0-9_]{2,32}$，按钮 targetPageId 引用它）',
  project_id        BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  name              VARCHAR(40)  NOT NULL COMMENT '页面名称（1–40）',
  slug              VARCHAR(63)  NOT NULL COMMENT '页面 slug（home/changelog/docs/team/report/links 固定；custom 站内唯一）',
  page_type         VARCHAR(20)  NOT NULL COMMENT 'home | custom | changelog | docs | team | issue-report | links',
  template_key      VARCHAR(40)  NOT NULL DEFAULT 'blank' COMMENT '默认模板 key（PAGE_SCHEMA §6）',
  status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT | PUBLISHED | DISABLED',
  sort_order        INT          NOT NULL DEFAULT 0 COMMENT '页面排序（升序）',
  draft_version     INT          NOT NULL DEFAULT 1 COMMENT '草稿版本号（乐观锁，新建为 1）',
  published_version INT          NULL COMMENT '已发布快照版本（从未发布为 NULL）',
  created_at        DATETIME     NOT NULL COMMENT '创建时间',
  updated_at        DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_site_page_project_slug (project_id, slug),
  KEY idx_site_page_project_sort (project_id, sort_order),
  KEY idx_site_page_project_type (project_id, page_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '站点页面（一个站点多个页面）';

CREATE TABLE IF NOT EXISTS site_page_draft (
  page_id        VARCHAR(40) NOT NULL COMMENT '页面稳定 ID（逻辑外键 site_page.id）',
  config_json    MEDIUMTEXT  NOT NULL COMMENT '草稿页面配置 JSON（PAGE_SCHEMA v2）',
  config_version INT         NOT NULL COMMENT '草稿版本号（与 site_page.draft_version 一致）',
  updated_at     DATETIME    NOT NULL COMMENT '最后保存时间',
  PRIMARY KEY (page_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '页面草稿配置';

CREATE TABLE IF NOT EXISTS site_page_publication (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '发布快照 ID',
  page_id        VARCHAR(40) NOT NULL COMMENT '页面稳定 ID（逻辑外键 site_page.id）',
  project_id     BIGINT      NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  config_version INT         NOT NULL COMMENT '被发布的草稿版本号（= 发布时的 site_page.draft_version）',
  config_json    MEDIUMTEXT  NOT NULL COMMENT '发布时的页面配置快照 JSON（只读，公开端唯一数据源）',
  published_at   DATETIME    NOT NULL COMMENT '发布时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_site_page_publication_page_version (page_id, config_version),
  KEY idx_site_page_publication_project_published (project_id, published_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '页面发布快照（公开端只读此处，绝不读取草稿）';

-- =====================================================================
-- 三、v2 新增表：站点设置 / 导航
-- =====================================================================

CREATE TABLE IF NOT EXISTS site_settings (
  project_id BIGINT      NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  theme_mode VARCHAR(10) NOT NULL DEFAULT 'DARK' COMMENT 'DARK | LIGHT | SYSTEM',
  updated_at DATETIME    NOT NULL COMMENT '更新时间',
  PRIMARY KEY (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '站点设置（深浅色主题模式）';

CREATE TABLE IF NOT EXISTS site_navigation (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '导航条目 ID',
  project_id     BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  location       VARCHAR(10)  NOT NULL COMMENT 'header | footer',
  title          VARCHAR(40)  NOT NULL COMMENT '导航标题（1–40）',
  target_type    VARCHAR(10)  NOT NULL COMMENT 'page | external',
  target_page_id VARCHAR(40)  NULL COMMENT 'targetType=page 时的页面稳定 ID',
  url            VARCHAR(300) NOT NULL DEFAULT '' COMMENT 'targetType=external 时的 http(s) 绝对 URL',
  sort_order     INT          NOT NULL DEFAULT 0 COMMENT '排序（0–999，升序）',
  visible        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否显示',
  PRIMARY KEY (id),
  KEY idx_site_navigation_project_location_sort (project_id, location, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '站点导航（header/footer，整体替换保存）';

-- =====================================================================
-- 四、v2 新增表：P1 内容模块
-- =====================================================================

CREATE TABLE IF NOT EXISTS changelog (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '更新日志 ID',
  project_id   BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  version      VARCHAR(40)  NOT NULL COMMENT '版本号（1–40，如 v0.9.2）',
  title        VARCHAR(120) NOT NULL COMMENT '标题（1–120）',
  content       MEDIUMTEXT  NOT NULL COMMENT '正文（兼容旧客户端的同值别名）',
  body_markdown MEDIUMTEXT  NULL COMMENT '正文 Markdown（v3；读取优先，空回退 content）',
  release_date  DATE        NOT NULL COMMENT '发布日期',
  status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT | PUBLISHED',
  created_at   DATETIME     NOT NULL COMMENT '创建时间',
  updated_at   DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_changelog_project_release (project_id, release_date),
  KEY idx_changelog_project_status (project_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '更新日志（内容模块）';

CREATE TABLE IF NOT EXISTS doc_article (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档 ID',
  project_id BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  title      VARCHAR(120) NOT NULL COMMENT '标题（1–120）',
  category   VARCHAR(40)  NOT NULL DEFAULT '' COMMENT '分类（0–40）',
  content       MEDIUMTEXT  NOT NULL COMMENT '正文（兼容旧客户端的同值别名）',
  body_markdown MEDIUMTEXT  NULL COMMENT '正文 Markdown（v3；读取优先，空回退 content）',
  slug          VARCHAR(63) NOT NULL COMMENT '文档 slug（站内唯一）',
  status     VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT | PUBLISHED',
  created_at DATETIME     NOT NULL COMMENT '创建时间',
  updated_at DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc_article_project_slug (project_id, slug),
  KEY idx_doc_article_project_status (project_id, status),
  KEY idx_doc_article_project_category (project_id, category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '公开文档（内容模块）';

CREATE TABLE IF NOT EXISTS team_member (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成员 ID',
  project_id BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  name       VARCHAR(60)  NOT NULL COMMENT '姓名/昵称（1–60）',
  avatar_url VARCHAR(300) NOT NULL DEFAULT '' COMMENT '头像地址（0–300，http(s) 或空）',
  role       VARCHAR(60)  NOT NULL DEFAULT '' COMMENT '角色（0–60）',
  bio        VARCHAR(500) NOT NULL DEFAULT '' COMMENT '简介（0–500）',
  sort_order INT          NOT NULL DEFAULT 0 COMMENT '排序（升序）',
  PRIMARY KEY (id),
  KEY idx_team_member_project_sort (project_id, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '制作组成员（内容模块）';

CREATE TABLE IF NOT EXISTS issue_report (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '问题反馈 ID',
  project_id   BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  title        VARCHAR(120) NOT NULL COMMENT '标题（1–120）',
  category     VARCHAR(40)  NOT NULL COMMENT '分类（1–40）',
  description  MEDIUMTEXT   NOT NULL COMMENT '问题描述（1–3000）',
  game_version VARCHAR(40)  NOT NULL DEFAULT '' COMMENT '游戏版本（0–40）',
  steps        MEDIUMTEXT   NOT NULL COMMENT '复现步骤（0–2000，空串表示未填写）',
  status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PROCESSING | RESOLVED',
  resolution   VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '处理结果（管理员填写，仅 RESOLVED 有意义）',
  is_public    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否允许公开展示（仅 RESOLVED 且 =1 才公开）',
  created_at   DATETIME     NOT NULL COMMENT '创建时间',
  updated_at   DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_issue_report_project_status (project_id, status),
  KEY idx_issue_report_project_created (project_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '问题反馈（公开提交 + 管理端状态流转）';

CREATE TABLE IF NOT EXISTS friend_link (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '友情链接 ID',
  project_id  BIGINT       NOT NULL COMMENT '所属社区项目 ID（逻辑外键 community_project.id）',
  name        VARCHAR(60)  NOT NULL COMMENT '名称（1–60）',
  url         VARCHAR(300) NOT NULL COMMENT 'URL（1–300，http(s) 绝对地址）',
  description VARCHAR(200) NOT NULL DEFAULT '' COMMENT '简介（0–200）',
  sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序（升序）',
  enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用（公开端只显示启用项）',
  PRIMARY KEY (id),
  KEY idx_friend_link_project_sort (project_id, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '友情链接（内容模块）';
