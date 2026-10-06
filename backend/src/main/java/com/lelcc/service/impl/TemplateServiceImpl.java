package com.lelcc.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.ComponentPropertyRegistry;
import com.lelcc.common.SiteConfigRegistry;
import com.lelcc.common.UiValueValidator;
import com.lelcc.dto.NodeDTO;
import com.lelcc.dto.PageDTO;
import com.lelcc.dto.TemplateApplyDTO;
import com.lelcc.dto.TemplateApplyVO;
import com.lelcc.dto.TemplateVO;
import com.lelcc.entity.Board;
import com.lelcc.entity.BoardVersion;
import com.lelcc.entity.Notice;
import com.lelcc.entity.ProgressStage;
import com.lelcc.entity.Tag;
import com.lelcc.entity.UiComponentType;
import com.lelcc.entity.UiNode;
import com.lelcc.entity.UiPage;
import com.lelcc.mapper.BoardMapper;
import com.lelcc.mapper.BoardVersionMapper;
import com.lelcc.mapper.NoticeMapper;
import com.lelcc.mapper.ProgressStageMapper;
import com.lelcc.mapper.TagMapper;
import com.lelcc.mapper.UiComponentTypeMapper;
import com.lelcc.mapper.UiNodeMapper;
import com.lelcc.mapper.UiPageMapper;
import com.lelcc.service.SiteService;
import com.lelcc.service.TemplateService;
import com.lelcc.service.UiPageService;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 社区模板服务：把 classpath:templates/&lt;code&gt; 下的模板数据变成可用的社区。
 *
 * <p>写库全部走既有服务（UiPageService / SiteService），不另起一套：页面节点树的父子关系解析、
 * 属性白名单、值类型校验都复用保存接口那一套，模板数据因此不可能绕过校验写脏数据。</p>
 *
 * <p>幂等策略：站点配置按 key upsert；组件库按 type_code upsert；模板页面按 page_key 新建，
 * 已有页面只在「按模板重建」打开（或该页本来就是空页）时才写节点；初始内容一律「该表为空才写」。</p>
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    private static final String INDEX_RESOURCE = "templates/index.json";
    private static final String TEMPLATE_DIR = "templates/";

    private static final int ENABLED = 1;
    /** 占位符前缀/后缀，如 {{siteName}}。用双花括号与前端模板语法保持一致 */
    private static final String PH_OPEN = "{{";
    private static final String PH_CLOSE = "}}";

    private static final Set<String> CATEGORIES =
            Set.of("base", "layout", "display", "interactive", "business");
    /** 已注册的基础渲染内核（与 UiComponentTypeServiceImpl 的清单一致） */
    private static final Set<String> BASE_KERNELS = Set.of(
            "text", "heading", "image", "button", "divider", "spacer",
            "container", "carousel", "list", "tabs", "modal", "icon",
            "input", "form");

    @Resource
    private SiteService siteService;

    @Resource
    private UiPageService pageService;

    @Resource
    private UiPageMapper pageMapper;

    @Resource
    private UiNodeMapper nodeMapper;

    @Resource
    private UiComponentTypeMapper componentTypeMapper;

    @Resource
    private BoardMapper boardMapper;

    @Resource
    private BoardVersionMapper boardVersionMapper;

    @Resource
    private ProgressStageMapper progressStageMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public List<TemplateVO> list() {
        // 模板清单本身是个数据文件：新增模板 = 加一个目录 + 在 index.json 里加一行，不用改 Java
        JSONObject index = JSONUtil.parseObj(read(INDEX_RESOURCE));
        List<TemplateVO> templates = new ArrayList<>();
        JSONArray codes = index.getJSONArray("templates");
        if (codes == null) {
            return templates;
        }
        for (Object code : codes) {
            templates.add(toVO(manifest(String.valueOf(code))));
        }
        return templates;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateApplyVO apply(TemplateApplyDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.code())) {
            throw new BusinessException(400, "请选择要应用的社区模板");
        }
        String code = dto.code();
        JSONObject manifest = manifest(code);
        Map<String, Object> values = dto.values() == null ? Map.of() : dto.values();

        Map<String, String> ctx = buildContext(manifest, values);
        boolean overwritePages = readBool(values, "overwritePages", true);
        boolean seedInitial = readBool(values, "seedInitial", true);

        List<String> createdPages = new ArrayList<>();
        List<String> rebuiltPages = new ArrayList<>();
        List<String> keptPages = new ArrayList<>();
        List<String> createdComponents = new ArrayList<>();
        List<String> updatedComponents = new ArrayList<>();
        List<String> seeded = new ArrayList<>();
        List<String> skippedSeed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        applySiteConfig(code, ctx);
        applyComponents(code, ctx, createdComponents, updatedComponents);
        applyPages(code, manifest, ctx, overwritePages,
                createdPages, rebuiltPages, keptPages, warnings);
        applyInitialContent(code, ctx, seedInitial, seeded, skippedSeed, warnings);
        collectWarnings(manifest, warnings);

        return new TemplateApplyVO(code, manifest.getStr("name"), siteService.getConfig(),
                createdPages, rebuiltPages, keptPages,
                createdComponents, updatedComponents, seeded, skippedSeed, warnings);
    }

    /* ==================== 站点配置 ==================== */

    /**
     * 写站点配置。
     *
     * <p>规则：模板里写成占位符（{{xxx}}）的项，替换后为空就**跳过不写**——
     * 「创建社区」表单留空表示"这一项我不填"，而不是"把已有值清掉"。
     * 想清空某一项请用「站点信息」表单；否则管理员为了重建页面而重跑一次模板，
     * 就会把之前填好的简介/Logo 一起抹掉。写成字面值的项（如 nav_items）照常写。</p>
     */
    private void applySiteConfig(String code, Map<String, String> ctx) {
        JSONObject config = JSONUtil.parseObj(read(resource(code, "site-config.json")));
        Map<String, String> incoming = new LinkedHashMap<>();
        for (String key : config.keySet()) {
            if (key.startsWith("_")) {
                continue;
            }
            Object raw = config.get(key);
            if (raw == null) {
                continue;
            }
            String template = String.valueOf(raw);
            String value = substituteText(template, ctx);
            if (value.isBlank() && template.contains(PH_OPEN)) {
                continue;
            }
            incoming.put(key, value);
        }
        // 校验与写库交给 SiteService：模板不重复实现配置项的规则
        siteService.putValues(incoming);
    }

    /* ==================== 组件库 ==================== */

    private void applyComponents(String code, Map<String, String> ctx,
                                 List<String> created, List<String> updated) {
        JSONObject file = JSONUtil.parseObj(read(resource(code, "components.json")));
        JSONArray components = file.getJSONArray("components");
        if (components == null) {
            return;
        }
        for (Object item : components) {
            if (!(item instanceof JSONObject component)) {
                continue;
            }
            String typeCode = component.getStr("typeCode");
            if (StrUtil.isBlank(typeCode)) {
                throw new BusinessException(500, "模板 " + code + " 的组件库定义缺少 typeCode");
            }
            UiComponentType entity = toComponentType(component);
            UiComponentType exists = componentTypeMapper.selectOne(
                    Wrappers.<UiComponentType>lambdaQuery().eq(UiComponentType::getTypeCode, typeCode));
            if (exists == null) {
                componentTypeMapper.insert(entity);
                created.add(typeCode);
            } else {
                entity.setId(exists.getId());
                componentTypeMapper.updateById(entity);
                updated.add(typeCode);
            }
        }
    }

    private UiComponentType toComponentType(JSONObject source) {
        String typeCode = source.getStr("typeCode");
        String category = source.getStr("category", "base");
        String renderKind = source.getStr("renderKind");
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException(500, "模板组件 " + typeCode + " 的分类不合法：" + category);
        }
        if (StrUtil.isBlank(renderKind)
                || (!renderKind.startsWith("builtin:") && !BASE_KERNELS.contains(renderKind))) {
            throw new BusinessException(500, "模板组件 " + typeCode + " 的渲染内核未注册：" + renderKind);
        }
        List<String> styleSchema = strList(source.getJSONArray("styleSchema"));
        List<String> contentSchema = strList(source.getJSONArray("contentSchema"));
        List<String> behaviorSchema = strList(source.getJSONArray("behaviorSchema"));
        checkSchema(typeCode, "样式", styleSchema);
        checkSchema(typeCode, "内容", contentSchema);
        checkSchema(typeCode, "行为", behaviorSchema);
        Map<String, Object> defaultStyle = toMap(source.getJSONObject("defaultStyle"));
        Map<String, Object> defaultContent = toMap(source.getJSONObject("defaultContent"));
        String styleError = UiValueValidator.validate(styleSchema, defaultStyle, typeCode + " / 默认样式");
        if (styleError != null) {
            throw new BusinessException(500, "模板组件定义有误：" + styleError);
        }
        String contentError = UiValueValidator.validate(contentSchema, defaultContent, typeCode + " / 默认内容");
        if (contentError != null) {
            throw new BusinessException(500, "模板组件定义有误：" + contentError);
        }

        UiComponentType entity = new UiComponentType();
        entity.setTypeCode(typeCode);
        entity.setTypeName(source.getStr("typeName", typeCode));
        entity.setCategory(category);
        entity.setRenderKind(renderKind);
        entity.setIsContainer(source.getInt("isContainer", 0));
        entity.setIsBuiltin(source.getInt("isBuiltin", 0));
        entity.setStyleSchema(JSONUtil.toJsonStr(styleSchema));
        entity.setContentSchema(JSONUtil.toJsonStr(contentSchema));
        entity.setBehaviorSchema(JSONUtil.toJsonStr(behaviorSchema));
        entity.setDefaultStyle(JSONUtil.toJsonStr(defaultStyle));
        entity.setDefaultContent(JSONUtil.toJsonStr(defaultContent));
        entity.setPreviewIcon(source.getStr("previewIcon"));
        entity.setSort(source.getInt("sort", 0));
        entity.setStatus(ENABLED);
        entity.setRemark(source.getStr("remark"));
        return entity;
    }

    private void checkSchema(String typeCode, String label, List<String> schema) {
        for (String key : schema) {
            if (!ComponentPropertyRegistry.isKnown(key)) {
                throw new BusinessException(500,
                        "模板组件 " + typeCode + " 的" + label + "属性不在属性库中：" + key);
            }
        }
    }

    /* ==================== 页面 ==================== */

    private void applyPages(String code, JSONObject manifest, Map<String, String> ctx,
                            boolean overwritePages,
                            List<String> created, List<String> rebuilt, List<String> kept,
                            List<String> warnings) {
        JSONArray pageKeys = manifest.getJSONArray("pageKeys");
        if (pageKeys == null || pageKeys.isEmpty()) {
            throw new BusinessException(500, "模板 " + code + " 没有声明任何页面");
        }

        for (Object key : pageKeys) {
            String pageKey = String.valueOf(key);
            JSONObject page = JSONUtil.parseObj(read(resource(code, "pages/" + pageKey + ".json")));
            replacePlaceholders(page, ctx);

            String pageName = page.getStr("pageName", pageKey);
            String routePath = page.getStr("routePath");
            int isHome = page.getInt("isHome", 0);
            String remark = page.getStr("remark");

            UiPage existing = pageMapper.selectOne(
                    Wrappers.<UiPage>lambdaQuery().eq(UiPage::getPageKey, pageKey));
            boolean isNew = existing == null;
            long nodeCount = isNew ? 0L : nodeMapper.selectCount(
                    Wrappers.<UiNode>lambdaQuery().eq(UiNode::getPageId, existing.getId()));

            if (routePath != null && !routePath.isBlank()) {
                UiPage conflict = pageMapper.selectOne(Wrappers.<UiPage>lambdaQuery()
                        .eq(UiPage::getRoutePath, routePath)
                        .ne(UiPage::getPageKey, pageKey)
                        .last("LIMIT 1"));
                if (conflict != null) {
                    warnings.add("路由 " + routePath + " 已被页面「" + conflict.getPageName()
                            + "」占用，模板页「" + pageName + "」可能不会生效");
                }
            }

            Long pageId;
            if (isNew) {
                pageService.create(new PageDTO(pageKey, pageName, routePath, isHome, ENABLED, remark));
                pageId = requirePageId(pageKey);
                created.add(pageKey);
            } else {
                pageId = existing.getId();
                if (overwritePages) {
                    pageService.update(pageId, new PageDTO(pageKey, pageName, routePath, isHome, ENABLED, remark));
                }
            }

            // 新页、选择重建、或这页本来就是空的：都写节点。已有内容且未选择重建时原样保留
            boolean writeNodes = isNew || overwritePages || nodeCount == 0;
            if (!writeNodes) {
                kept.add(pageKey);
                continue;
            }
            pageService.saveNodes(pageId, toNodeDTOs(page.getJSONArray("nodes"), pageKey));
            if (!isNew && nodeCount > 0) {
                rebuilt.add(pageKey);
            }
        }
    }

    private Long requirePageId(String pageKey) {
        UiPage page = pageMapper.selectOne(Wrappers.<UiPage>lambdaQuery().eq(UiPage::getPageKey, pageKey));
        if (page == null) {
            throw new BusinessException(500, "页面创建后读不回来：" + pageKey);
        }
        return page.getId();
    }

    private List<NodeDTO> toNodeDTOs(JSONArray nodes, String pageKey) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(500, "模板页面 " + pageKey + " 没有节点");
        }
        List<NodeDTO> result = new ArrayList<>(nodes.size());
        for (Object item : nodes) {
            if (!(item instanceof JSONObject node)) {
                continue;
            }
            String typeCode = node.getStr("typeCode");
            if (StrUtil.isBlank(typeCode)) {
                throw new BusinessException(500, "模板页面 " + pageKey + " 有节点缺少 typeCode");
            }
            result.add(new NodeDTO(
                    node.getLong("id"),
                    node.getLong("parentId"),
                    typeCode,
                    node.getStr("nodeName"),
                    Math.max(node.getInt("sort", 0), 0),
                    node.getInt("visible", 1),
                    toMap(node.getJSONObject("styleJson")),
                    toMap(node.getJSONObject("contentJson")),
                    toMap(node.getJSONObject("behaviorJson")),
                    node.getStr("remark")));
        }
        return result;
    }

    /* ==================== 初始内容 ==================== */

    /**
     * 初始内容一律「该表为空才写」：重复应用模板不会插重复数据，
     * 已经在用的社区也不会被塞进一份"模板专属"的假内容。
     */
    private void applyInitialContent(String code, Map<String, String> ctx, boolean seedInitial,
                                     List<String> seeded, List<String> skipped, List<String> warnings) {
        if (!seedInitial) {
            skipped.add("初始内容（未勾选）");
            if (boardMapper.selectCount(null) == 0) {
                warnings.add("当前没有任何板块，用户端首页与攻略页会显示空状态；可在「内容管理 → 板块管理」新增");
            }
            return;
        }

        JSONObject seed = JSONUtil.parseObj(read(resource(code, "seed/initial.json")));
        replacePlaceholders(seed, ctx);

        Long boardId = ensureBoard(seed.getJSONObject("board"), seeded, skipped);
        if (boardId != null) {
            ensureFirstVersion(seed.getJSONObject("boardVersion"), boardId, seeded, skipped);
            ensureStages(seed.getJSONArray("progressStages"), boardId, seeded, skipped);
        }
        ensureTags(seed.getJSONArray("tags"), boardId, seeded, skipped);
        ensureNotices(seed.getJSONArray("notices"), seeded, skipped);
    }

    private Long ensureBoard(JSONObject spec, List<String> seeded, List<String> skipped) {
        if (spec == null) {
            return firstBoardId();
        }
        if (boardMapper.selectCount(null) > 0) {
            skipped.add("板块（已有板块，未新增）");
            return firstBoardId();
        }
        Board board = new Board();
        board.setName(spec.getStr("name", "游戏"));
        board.setIntro(spec.getStr("intro", ""));
        board.setSort(spec.getInt("sort", 1));
        board.setStatus(spec.getInt("status", ENABLED));
        boardMapper.insert(board);
        seeded.add("板块：" + board.getName());
        return board.getId();
    }

    private Long firstBoardId() {
        List<Board> boards = boardMapper.selectList(Wrappers.<Board>lambdaQuery()
                .orderByAsc(Board::getSort)
                .orderByAsc(Board::getId));
        return boards.isEmpty() ? null : boards.get(0).getId();
    }

    private void ensureFirstVersion(JSONObject spec, Long boardId, List<String> seeded, List<String> skipped) {
        if (spec == null) {
            return;
        }
        if (boardVersionMapper.selectCount(null) > 0) {
            skipped.add("游戏版本（已有版本，未新增）");
            return;
        }
        BoardVersion version = new BoardVersion();
        version.setBoardId(boardId);
        version.setVersionNo(spec.getStr("versionNo", "1.0.0"));
        version.setVersionName(spec.getStr("versionName"));
        version.setUpdateNote(spec.getStr("updateNote"));
        version.setReleaseTime(LocalDateTime.now());
        version.setStatus(ENABLED);
        boardVersionMapper.insert(version);

        if (spec.getBool("setCurrent", true)) {
            // 「当前版本」的唯一事实源是 board.current_version_id，版本表里没有这个字段
            Board update = new Board();
            update.setId(boardId);
            update.setCurrentVersionId(version.getId());
            boardMapper.updateById(update);
        }
        seeded.add("初始版本：" + version.getVersionNo());
    }

    private void ensureStages(JSONArray specs, Long boardId, List<String> seeded, List<String> skipped) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        if (progressStageMapper.selectCount(null) > 0) {
            skipped.add("进度阶段（已有阶段，未新增）");
            return;
        }
        int count = 0;
        for (Object item : specs) {
            if (!(item instanceof JSONObject spec)) {
                continue;
            }
            ProgressStage stage = new ProgressStage();
            stage.setBoardId(boardId);
            stage.setStageName(spec.getStr("stageName"));
            stage.setDescription(spec.getStr("description"));
            stage.setSort(spec.getInt("sort", 0));
            stage.setStatus(ENABLED);
            progressStageMapper.insert(stage);
            count++;
        }
        seeded.add("进度阶段 ×" + count);
    }

    private void ensureTags(JSONArray specs, Long boardId, List<String> seeded, List<String> skipped) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        if (tagMapper.selectCount(null) > 0) {
            skipped.add("标签（已有标签，未新增）");
            return;
        }
        int count = 0;
        for (Object item : specs) {
            if (!(item instanceof JSONObject spec)) {
                continue;
            }
            Tag tag = new Tag();
            // boardScoped 的标签挂到主板块下；其余是全局标签，任何板块都能用
            tag.setBoardId(spec.getBool("boardScoped", false) ? boardId : null);
            tag.setTagName(spec.getStr("tagName"));
            tag.setTagType(spec.getInt("tagType", 1));
            tag.setSort(spec.getInt("sort", 0));
            tag.setStatus(ENABLED);
            tagMapper.insert(tag);
            count++;
        }
        seeded.add("内容分类标签 ×" + count);
    }

    private void ensureNotices(JSONArray specs, List<String> seeded, List<String> skipped) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        if (noticeMapper.selectCount(null) > 0) {
            skipped.add("公告（已有公告，未新增）");
            return;
        }
        int count = 0;
        for (Object item : specs) {
            if (!(item instanceof JSONObject spec)) {
                continue;
            }
            Notice notice = new Notice();
            notice.setTitle(spec.getStr("title"));
            notice.setContent(spec.getStr("content"));
            notice.setType(spec.getInt("type", 1));
            notice.setSort(spec.getInt("sort", 0));
            notice.setStatus(spec.getInt("status", ENABLED));
            noticeMapper.insert(notice);
            count++;
        }
        seeded.add("公告 ×" + count);
    }

    /* ==================== 提醒 ==================== */

    /** 模板依赖的既有页面是否都在；缺了要在报告里说清楚，别让管理员自己撞 404 才发现 */
    private void collectWarnings(JSONObject manifest, List<String> warnings) {
        JSONObject related = manifest.getJSONObject("relatedPages");
        if (related != null) {
            for (String pageKey : related.keySet()) {
                boolean exists = pageMapper.exists(Wrappers.<UiPage>lambdaQuery().eq(UiPage::getPageKey, pageKey));
                if (!exists) {
                    warnings.add("缺少「" + related.getStr(pageKey) + "」，请先执行 sql/ui_login.sql，"
                            + "否则页头的登录/注册入口会打开 404 页");
                }
            }
        }
        JSONObject custom = manifest.getJSONObject("customPages");
        if (custom != null) {
            for (String pageKey : custom.keySet()) {
                boolean exists = pageMapper.exists(Wrappers.<UiPage>lambdaQuery().eq(UiPage::getPageKey, pageKey));
                if (exists) {
                    warnings.add(custom.getStr(pageKey));
                }
            }
        }
        Set<String> unique = new LinkedHashSet<>(warnings);
        warnings.clear();
        warnings.addAll(unique);
    }

    /* ==================== 输入与占位符 ==================== */

    /** 按 manifest 的 inputs 校验输入并组装占位符上下文；布尔项单独读，不进上下文 */
    private Map<String, String> buildContext(JSONObject manifest, Map<String, Object> values) {
        JSONArray inputs = manifest.getJSONArray("inputs");
        Map<String, String> ctx = new LinkedHashMap<>();
        if (inputs == null) {
            return ctx;
        }
        for (Object item : inputs) {
            if (!(item instanceof JSONObject input)) {
                continue;
            }
            String key = input.getStr("key");
            String label = input.getStr("label", key);
            String type = input.getStr("type", "text");
            if ("boolean".equals(type)) {
                continue;
            }
            Object raw = values.get(key);
            String value = raw == null ? "" : String.valueOf(raw).trim();
            if (value.isEmpty()) {
                value = StrUtil.nullToEmpty(input.getStr("default"));
            }
            if (Boolean.TRUE.equals(input.getBool("required", false)) && value.isEmpty()) {
                throw new BusinessException(400, label + "不能为空");
            }
            Integer maxLength = input.getInt("maxLength");
            if (maxLength != null && value.length() > maxLength) {
                throw new BusinessException(400, label + "不能超过 " + maxLength + " 个字");
            }
            ctx.put(key, value);
        }
        ctx.put("year", String.valueOf(LocalDateTime.now().getYear()));
        return ctx;
    }

    private boolean readBool(Map<String, Object> values, String key, boolean fallback) {
        Object raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    /** 递归替换 JSON 里的 {{key}} 占位符（模板里不写死社区名/游戏名靠的就是这一步） */
    private void replacePlaceholders(Object node, Map<String, String> ctx) {
        if (node instanceof JSONObject obj) {
            for (String key : new ArrayList<>(obj.keySet())) {
                Object value = obj.get(key);
                if (value instanceof CharSequence) {
                    obj.set(key, substituteText(value.toString(), ctx));
                } else if (value != null) {
                    replacePlaceholders(value, ctx);
                }
            }
            return;
        }
        if (node instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                Object value = array.get(i);
                if (value instanceof CharSequence) {
                    array.set(i, substituteText(value.toString(), ctx));
                } else if (value != null) {
                    replacePlaceholders(value, ctx);
                }
            }
        }
    }

    private String substituteText(String text, Map<String, String> ctx) {
        String result = text;
        for (Map.Entry<String, String> entry : ctx.entrySet()) {
            result = result.replace(PH_OPEN + entry.getKey() + PH_CLOSE, entry.getValue());
        }
        return result;
    }

    /* ==================== 资源读取与类型转换 ==================== */

    private JSONObject manifest(String code) {
        String path = TEMPLATE_DIR + code + "/manifest.json";
        if (!new ClassPathResource(path).exists()) {
            throw new BusinessException(404, "社区模板不存在：" + code);
        }
        return JSONUtil.parseObj(read(path));
    }

    private String resource(String code, String relative) {
        return TEMPLATE_DIR + code + "/" + relative;
    }

    private String read(String path) {
        ClassPathResource classPathResource = new ClassPathResource(path);
        if (!classPathResource.exists()) {
            throw new BusinessException(500, "模板文件缺失：" + path);
        }
        try (InputStream in = classPathResource.getInputStream()) {
            // 显式按 UTF-8 读：模板里有中文文案，跟随平台默认编码会乱码
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(500, "模板文件读取失败：" + path);
        }
    }

    private TemplateVO toVO(JSONObject manifest) {
        List<TemplateVO.InputVO> inputs = new ArrayList<>();
        JSONArray rawInputs = manifest.getJSONArray("inputs");
        if (rawInputs != null) {
            for (Object item : rawInputs) {
                if (!(item instanceof JSONObject input)) {
                    continue;
                }
                inputs.add(new TemplateVO.InputVO(
                        input.getStr("key"),
                        input.getStr("label"),
                        input.getStr("type", "text"),
                        input.getBool("required", false),
                        input.getInt("maxLength"),
                        input.getStr("placeholder"),
                        input.getStr("help"),
                        // 默认值统一转成字符串下发：布尔项因此也是 "true"/"false"，
                        // 前端不必为"默认值是 JSON 布尔还是字符串"分两种情况处理
                        input.get("default") == null ? null : String.valueOf(input.get("default"))));
            }
        }
        return new TemplateVO(
                manifest.getStr("code"),
                manifest.getStr("name"),
                manifest.getStr("nameZh"),
                manifest.getInt("version", 1),
                manifest.getStr("description"),
                inputs,
                strList(manifest.getJSONArray("pageKeys")),
                strMap(manifest.getJSONObject("relatedPages")),
                strMap(manifest.getJSONObject("customPages")));
    }

    private List<String> strList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (Object item : array) {
                list.add(String.valueOf(item));
            }
        }
        return list;
    }

    private Map<String, String> strMap(JSONObject object) {
        Map<String, String> map = new LinkedHashMap<>();
        if (object != null) {
            for (String key : object.keySet()) {
                map.put(key, object.getStr(key));
            }
        }
        return map;
    }

    private Map<String, Object> toMap(JSONObject object) {
        return object == null ? new LinkedHashMap<>() : new LinkedHashMap<>(object);
    }
}
