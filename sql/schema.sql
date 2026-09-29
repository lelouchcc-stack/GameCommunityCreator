-- 可定制游戏社区平台 · 建表脚本
-- 依据《16-毕设数据库设计.md》生成，与文档 DDL 的差异见下（共 7 处）：
--   1. board_version 删除 is_current            —— 当前版本以 board.current_version_id 为唯一事实源
--   2. comment 增加 root_id                     —— 回复统一挂在所属一级评论下，便于按一级评论分页
--   3. post 索引 idx_board_status 改为 (board_id, status, create_time) —— 板块列表按时间倒序不再 filesort
--   4. post_like / post_favorite 增加 (member_id, create_time) —— 支撑"我的点赞""我的收藏"
--   5. report 增加 uk_post_member(post_id, member_id)  —— 防同一用户重复举报同一帖子
--   6. post 增加 audit_remark                   —— 记录审核驳回原因
--   7. member_game_progress 增加 version_id / is_public，stage_id 改为可空 —— 游玩版本为可选上传
--
-- 执行方式：整份复制到 Navicat 查询窗口执行即可。库已存在，用 IF NOT EXISTS 不会重建。
-- 未包含 DROP TABLE，重复执行会因表已存在而报错，这是有意的（避免误删数据）。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ssy_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE ssy_db;

-- ==================== 一、系统权限域 ====================

CREATE TABLE sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  nickname    VARCHAR(50)  COMMENT '昵称',
  avatar      VARCHAR(255) COMMENT '头像',
  phone       VARCHAR(20)  COMMENT '手机号',
  email       VARCHAR(50)  COMMENT '邮箱',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
  remark      VARCHAR(200) COMMENT '备注',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='后台用户表';

CREATE TABLE sys_role (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
  role_name   VARCHAR(50) NOT NULL COMMENT '角色名',
  role_key    VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识:admin/ops',
  sort        INT         NOT NULL DEFAULT 0,
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
  remark      VARCHAR(200),
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_menu (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
  parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父菜单ID,0=根',
  menu_name   VARCHAR(50) NOT NULL COMMENT '菜单名称',
  menu_type   CHAR(1)     NOT NULL COMMENT 'M目录 C菜单 F按钮',
  path        VARCHAR(200) COMMENT '路由地址',
  component   VARCHAR(255) COMMENT '组件路径',
  perms       VARCHAR(100) COMMENT '权限标识,如post:audit',
  icon        VARCHAR(100) COMMENT '图标',
  sort        INT         NOT NULL DEFAULT 0,
  visible     TINYINT     NOT NULL DEFAULT 1,
  status      TINYINT     NOT NULL DEFAULT 1,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='菜单表';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB COMMENT='角色菜单关联表';

CREATE TABLE sys_oper_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT COMMENT '操作人ID',
  username    VARCHAR(50),
  module      VARCHAR(50)  COMMENT '模块',
  operation   VARCHAR(100) COMMENT '操作描述',
  method      VARCHAR(200) COMMENT '请求方法',
  params      TEXT         COMMENT '请求参数(需在应用层脱敏 password 等敏感字段)',
  ip          VARCHAR(50),
  status      TINYINT      COMMENT '1成功 0失败',
  error_msg   TEXT,
  cost_time   BIGINT       COMMENT '耗时(毫秒)',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ==================== 二、站点配置域 ====================

CREATE TABLE site_config (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key   VARCHAR(50)  NOT NULL UNIQUE COMMENT 'site_name/logo/primary_color/dark_mode/copyright',
  config_value VARCHAR(500) COMMENT '配置值',
  remark       VARCHAR(200) COMMENT '说明',
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='站点配置表';

CREATE TABLE home_module (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  module_type VARCHAR(30) NOT NULL COMMENT 'banner/notice/hot_post/board_nav',
  module_name VARCHAR(50) COMMENT '模块名称',
  sort        INT     NOT NULL DEFAULT 0 COMMENT '排序',
  enabled     TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  config_json VARCHAR(500) COMMENT '模块私有参数,如热门帖取几条',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='首页模块配置表';

-- ==================== 三、板块与内容维度域 ====================

CREATE TABLE board (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  name               VARCHAR(50)  NOT NULL COMMENT '板块/游戏名称',
  intro              VARCHAR(500) COMMENT '简介',
  cover              VARCHAR(255) COMMENT '封面',
  sort               INT     NOT NULL DEFAULT 0,
  status             TINYINT NOT NULL DEFAULT 1 COMMENT '1开放 0关闭',
  current_version_id BIGINT  COMMENT '当前版本ID(过期判定与默认筛选的回落值)',
  create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='板块表';

CREATE TABLE board_version (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  board_id     BIGINT      NOT NULL COMMENT '板块ID',
  version_no   VARCHAR(50) NOT NULL COMMENT '版本号,如1.3.0',
  version_name VARCHAR(50) COMMENT '版本名',
  release_time DATETIME    COMMENT '发布时间',
  update_note  TEXT        COMMENT '更新说明',
  status       TINYINT NOT NULL DEFAULT 1,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_board (board_id)
) ENGINE=InnoDB COMMENT='游戏版本表';

CREATE TABLE progress_stage (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  board_id    BIGINT      NOT NULL COMMENT '板块ID',
  stage_name  VARCHAR(50) NOT NULL COMMENT '阶段名,如新手期/开荒期/中期/满级',
  description VARCHAR(200) COMMENT '阶段说明',
  sort        INT     NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_board (board_id)
) ENGINE=InnoDB COMMENT='进度阶段表';

CREATE TABLE member_game_progress (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id   BIGINT NOT NULL,
  board_id    BIGINT NOT NULL,
  version_id  BIGINT COMMENT '我的游玩版本,空=未填写',
  stage_id    BIGINT COMMENT '我的进度阶段,空=未填写',
  is_public   TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开:1公开 0仅自己可见',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_member_board (member_id, board_id)
) ENGINE=InnoDB COMMENT='玩家游戏进度表';

-- ==================== 四、玩家与内容域 ====================

CREATE TABLE member (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  nickname    VARCHAR(50)  COMMENT '昵称',
  avatar      VARCHAR(255) COMMENT '头像',
  intro       VARCHAR(200) COMMENT '个人简介',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0封禁',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='玩家表';

CREATE TABLE post (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  board_id       BIGINT       NOT NULL COMMENT '板块ID',
  member_id      BIGINT       NOT NULL COMMENT '作者',
  version_id     BIGINT       COMMENT '关联游戏版本,空=通用',
  stage_id       BIGINT       COMMENT '关联进度阶段,空=通用',
  title          VARCHAR(100) NOT NULL COMMENT '标题',
  content        LONGTEXT     COMMENT '正文',
  cover          VARCHAR(255) COMMENT '封面',
  image_urls     VARCHAR(1000) COMMENT '图片地址(JSON数组)',
  view_count     INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  like_count     INT NOT NULL DEFAULT 0 COMMENT '点赞数(冗余)',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数(冗余)',
  comment_count  INT NOT NULL DEFAULT 0 COMMENT '评论数(冗余)',
  status         TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1已发布 2已下架',
  audit_remark   VARCHAR(200) COMMENT '审核意见/驳回原因',
  is_top         TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_board_status_time (board_id, status, create_time),
  KEY idx_member (member_id),
  KEY idx_version (version_id),
  KEY idx_stage (stage_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='帖子表';

CREATE TABLE tag (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  board_id    BIGINT      COMMENT '所属板块,空=全局标签',
  tag_name    VARCHAR(30) NOT NULL COMMENT '标签名',
  tag_type    TINYINT NOT NULL DEFAULT 1 COMMENT '1内容类型(攻略/讨论/同人) 2主题',
  sort        INT     NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_board (board_id)
) ENGINE=InnoDB COMMENT='标签表';

CREATE TABLE post_tag (
  post_id BIGINT NOT NULL,
  tag_id  BIGINT NOT NULL,
  PRIMARY KEY (post_id, tag_id)
) ENGINE=InnoDB COMMENT='帖子标签关联表';

CREATE TABLE comment (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id     BIGINT   NOT NULL,
  member_id   BIGINT   NOT NULL,
  parent_id   BIGINT   NOT NULL DEFAULT 0 COMMENT '0=一级评论,非0=被回复的评论ID',
  root_id     BIGINT   NOT NULL DEFAULT 0 COMMENT '所属一级评论ID,一级评论自身为0',
  content     VARCHAR(1000) NOT NULL,
  like_count  INT NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0删除',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_post (post_id),
  KEY idx_parent (parent_id),
  KEY idx_root (root_id)
) ENGINE=InnoDB COMMENT='评论表';

CREATE TABLE post_like (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id     BIGINT NOT NULL,
  member_id   BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_post_member (post_id, member_id),
  KEY idx_member_time (member_id, create_time)
) ENGINE=InnoDB COMMENT='点赞表';

CREATE TABLE post_favorite (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id     BIGINT NOT NULL,
  member_id   BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_post_member (post_id, member_id),
  KEY idx_member_time (member_id, create_time)
) ENGINE=InnoDB COMMENT='收藏表';

CREATE TABLE report (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id        BIGINT NOT NULL,
  member_id      BIGINT NOT NULL COMMENT '举报人',
  reason         VARCHAR(50)  COMMENT '举报原因',
  description    VARCHAR(500) COMMENT '补充说明',
  status         TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2已驳回',
  handle_user_id BIGINT  COMMENT '处理人(后台账号)',
  handle_time    DATETIME COMMENT '处理时间',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status (status),
  UNIQUE KEY uk_post_member (post_id, member_id)
) ENGINE=InnoDB COMMENT='举报表';

-- ==================== 五、运营域 ====================

CREATE TABLE notice (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(100) NOT NULL COMMENT '标题',
  content     TEXT COMMENT '内容',
  type        TINYINT NOT NULL DEFAULT 1 COMMENT '1公告 2版本更新说明',
  sort        INT     NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1发布 0草稿',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='公告表';
