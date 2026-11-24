package com.gcc.community.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcc.community.common.ApiException;
import com.gcc.community.domain.PageAction;
import com.gcc.community.domain.PageComponentConfig;
import com.gcc.community.domain.PageConfig;
import com.gcc.community.domain.ThemeConfig;

// 页面配置校验器，把前端传过来的页面 JSON 检查一遍。
@Component
public class PageConfigValidator {

    // 配置最大 200KB
    public static final int MAX_CONFIG_BYTES = 204800;

    public static final int SCHEMA_VERSION_V1 = 1;
    public static final int SCHEMA_VERSION_V2 = 2;

    public static final int MIN_COMPONENTS = 1;
    public static final int MAX_COMPONENTS = 20;

    public static final String UPGRADE_MESSAGE = "请将页面配置升级到 schemaVersion=2";

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,62}$");
    private static final Pattern PAGE_ID_PATTERN = Pattern.compile("^page_[a-z0-9_]{2,32}$");
    private static final Pattern COMPONENT_ID_PATTERN = Pattern.compile("^[a-z0-9_]{3,40}$");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    // 现在支持的 11 种组件
    public static final Set<String> COMPONENT_TYPES = Set.of(
            "hero", "about", "notice_list", "footer", "button",
            "changelog_list", "docs_list", "team_grid", "issue_form", "links_grid",
            "issue_progress");

    // 旧版配置只支持这 4 种组件
    public static final Set<String> V1_COMPONENT_TYPES = Set.of(
            "hero", "about", "notice_list", "footer");

    // 每种组件对应的固定数据源
    public static final Map<String, String> DATA_SOURCE_BY_TYPE = Map.ofEntries(
            Map.entry("hero", "game_info"),
            Map.entry("about", "game_info"),
            Map.entry("notice_list", "announcements"),
            Map.entry("footer", ""),
            Map.entry("button", ""),
            Map.entry("changelog_list", "changelogs"),
            Map.entry("docs_list", "docs"),
            Map.entry("team_grid", "team"),
            Map.entry("issue_form", ""),
            Map.entry("links_grid", "links"),
            Map.entry("issue_progress", "issues_progress"));

    public static final Set<String> FONT_FAMILIES = Set.of("system", "serif", "mono");
    public static final Set<String> ACTION_TYPES =
            Set.of(PageAction.TYPE_NAVIGATE, PageAction.TYPE_EXTERNAL_LINK, PageAction.TYPE_NONE);

    private static final Map<String, List<PropSpec>> COMPONENT_PROPS = buildPropSpecs();

    @Autowired
    private ObjectMapper objectMapper;

    // 对外的方法

    // 兼容校验：1 和 2 两个版本都接受
    public PageConfig validate(JsonNode configNode, PageValidationContext ctx) {
        return doValidate(configNode, ctx, false);
    }

    // 保存时只接受新版配置，旧版直接报错
    public PageConfig validateForSave(JsonNode configNode, PageValidationContext ctx) {
        if (configNode == null || configNode.isNull() || !configNode.isObject()) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("config", "页面配置必须是 JSON 对象");
            throw fail(errors);
        }
        JsonNode versionNode = configNode.get("schemaVersion");
        if (versionNode == null || !versionNode.isIntegralNumber() || versionNode.asInt() != SCHEMA_VERSION_V2) {
            throw ApiException.badRequest(UPGRADE_MESSAGE, Map.of("schemaVersion", UPGRADE_MESSAGE));
        }
        return doValidate(configNode, ctx, true);
    }

    // 主流程

    private PageConfig doValidate(JsonNode configNode, PageValidationContext ctx, boolean requireV2) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (configNode == null || configNode.isNull() || !configNode.isObject()) {
            errors.put("config", "页面配置必须是 JSON 对象");
            throw fail(errors);
        }

        // 先看原始 JSON 是不是太大了
        checkRawBytes(configNode);

        int schemaVersion = validateSchemaVersion(configNode, errors);
        boolean isV2 = schemaVersion == SCHEMA_VERSION_V2;

        // 校验页面标题、slug 和 pageId
        String pageTitle = requireText(configNode, "pageTitle", "pageTitle", 1, 60, errors);
        String configSlug = requireText(configNode, "projectSlug", "projectSlug", 1, 63, errors);
        if (configSlug != null) {
            if (!SLUG_PATTERN.matcher(configSlug).matches()) {
                errors.put("projectSlug", "项目标识格式不合法（小写字母、数字、中划线，2–63 位且以字母或数字开头）");
            } else if (ctx != null && ctx.projectSlug() != null && !configSlug.equals(ctx.projectSlug())) {
                errors.put("projectSlug", "页面配置的 projectSlug 必须与所属社区项目的标识一致");
            }
        }
        String pageId = validatePageId(configNode, isV2, ctx, errors);

        // 校验主题配置
        ThemeConfig theme = validateTheme(configNode.get("theme"), errors);

        // 校验组件和按钮动作
        List<PageComponentConfig> components =
                validateComponents(configNode.get("components"), schemaVersion, ctx, errors);

        if (!errors.isEmpty()) {
            throw fail(errors);
        }

        PageConfig config = new PageConfig(schemaVersion, isV2 ? pageId : null, pageTitle, configSlug, theme,
                components);

        // 填完默认值再检查一次大小
        try {
            if (objectMapper.writeValueAsBytes(config).length > MAX_CONFIG_BYTES) {
                errors.put("config", "页面配置序列化后不能超过 200 KB");
                throw fail(errors);
            }
        } catch (JsonProcessingException ex) {
            throw ApiException.badRequest("页面配置无法序列化");
        }
        return config;
    }

    private void checkRawBytes(JsonNode configNode) {
        Map<String, String> errors = new LinkedHashMap<>();
        try {
            if (objectMapper.writeValueAsBytes(configNode).length > MAX_CONFIG_BYTES) {
                errors.put("config", "页面配置序列化后不能超过 200 KB");
                throw fail(errors);
            }
        } catch (JsonProcessingException ex) {
            throw ApiException.badRequest("页面配置不是合法的 JSON");
        }
    }

    // schemaVersion

    private int validateSchemaVersion(JsonNode configNode, Map<String, String> errors) {
        JsonNode node = configNode.get("schemaVersion");
        if (node == null || node.isNull()) {
            errors.put("schemaVersion", "不能为空");
            return 0;
        }
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            errors.put("schemaVersion", "必须是整数");
            return 0;
        }
        int value = node.asInt();
        if (value != SCHEMA_VERSION_V1 && value != SCHEMA_VERSION_V2) {
            errors.put("schemaVersion", "仅支持 schemaVersion = 1 或 2");
            return 0;
        }
        return value;
    }

    // pageId

    private String validatePageId(JsonNode configNode, boolean isV2, PageValidationContext ctx,
            Map<String, String> errors) {
        JsonNode node = configNode.get("pageId");
        String expected = ctx == null ? null : ctx.pageId();
        if (node == null || node.isNull()) {
            if (isV2) {
                errors.put("pageId", "v2 页面配置必须包含 pageId");
            }
            return null;
        }
        if (!node.isTextual()) {
            errors.put("pageId", "必须是字符串");
            return null;
        }
        String pageId = node.asText();
        if (!PAGE_ID_PATTERN.matcher(pageId).matches()) {
            errors.put("pageId", "页面稳定 ID 格式不合法（^page_[a-z0-9_]{2,32}$）");
            return pageId;
        }
        if (expected != null && !pageId.equals(expected)) {
            errors.put("pageId", "页面配置的 pageId 必须与当前页面一致");
        }
        return pageId;
    }

    // theme 的 8 个字段

    private ThemeConfig validateTheme(JsonNode themeNode, Map<String, String> errors) {
        if (themeNode == null || themeNode.isNull() || !themeNode.isObject()) {
            errors.put("theme", "主题配置必须是对象");
            return null;
        }
        String primary = requireColor(themeNode, "primaryColor", errors);
        String accent = requireColor(themeNode, "accentColor", errors);
        String background = requireColor(themeNode, "backgroundColor", errors);
        String surface = requireColor(themeNode, "surfaceColor", errors);
        String text = requireColor(themeNode, "textColor", errors);
        String muted = requireColor(themeNode, "mutedTextColor", errors);

        String fontFamily = requireText(themeNode, "fontFamily", "theme.fontFamily", 1, 16, errors);
        if (fontFamily != null && !FONT_FAMILIES.contains(fontFamily)) {
            errors.put("theme.fontFamily", "只能是 system、serif 或 mono");
        }

        Integer borderRadius = requireInteger(themeNode, "borderRadius", "theme.borderRadius", 0, 24, errors);

        return new ThemeConfig(primary, accent, background, surface, text, muted, fontFamily,
                borderRadius == null ? 0 : borderRadius);
    }

    private String requireColor(JsonNode parent, String field, Map<String, String> errors) {
        String path = "theme." + field;
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            errors.put(path, "不能为空");
            return null;
        }
        if (!node.isTextual()) {
            errors.put(path, "必须是字符串");
            return null;
        }
        String value = node.asText();
        if (!COLOR_PATTERN.matcher(value).matches()) {
            errors.put(path, "必须是 #RRGGBB 格式的颜色");
            return null;
        }
        return value;
    }

    // components

    private List<PageComponentConfig> validateComponents(JsonNode componentsNode, int schemaVersion,
            PageValidationContext ctx, Map<String, String> errors) {
        if (componentsNode == null || componentsNode.isNull() || !componentsNode.isArray()) {
            errors.put("components", "组件列表必须是数组");
            return List.of();
        }
        int size = componentsNode.size();
        if (size < MIN_COMPONENTS) {
            errors.put("components", "至少需要 1 个组件");
        } else if (size > MAX_COMPONENTS) {
            errors.put("components", "最多允许 20 个组件");
        }

        List<PageComponentConfig> components = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            JsonNode node = componentsNode.get(i);
            String path = "components[" + i + "]";
            if (node == null || node.isNull() || !node.isObject()) {
                errors.put(path, "组件必须是 JSON 对象");
                continue;
            }
            String id = validateComponentId(node, path, ids, errors);
            String type = validateComponentType(node, path, schemaVersion, errors);
            String title = requireText(node, "title", path + ".title", 1, 60, errors);
            Boolean visible = requireBoolean(node, "visible", path + ".visible", errors);

            if (type == null) {
                // 类型不认识，后面的就不用查了
                continue;
            }
            String dataSource = validateDataSource(node, type, path, errors);
            Map<String, Object> props = validateProps(node, type, path, errors);
            PageAction action = validateAction(node, type, path, ctx, errors);
            components.add(new PageComponentConfig(id, type, title, Boolean.TRUE.equals(visible),
                    dataSource, props, action));
        }
        return components;
    }

    private String validateComponentId(JsonNode component, String path, Set<String> ids,
            Map<String, String> errors) {
        JsonNode node = component.get("id");
        if (node == null || node.isNull()) {
            errors.put(path + ".id", "不能为空");
            return null;
        }
        if (!node.isTextual()) {
            errors.put(path + ".id", "必须是字符串");
            return null;
        }
        String id = node.asText();
        if (!COMPONENT_ID_PATTERN.matcher(id).matches()) {
            errors.put(path + ".id", "只能由小写字母、数字、下划线组成，长度 3–40");
            return null;
        }
        if (!ids.add(id)) {
            errors.put(path + ".id", "组件 id 在同一页面内不能重复");
        }
        return id;
    }

    private String validateComponentType(JsonNode component, String path, int schemaVersion,
            Map<String, String> errors) {
        JsonNode node = component.get("type");
        if (node == null || node.isNull()) {
            errors.put(path + ".type", "不能为空");
            return null;
        }
        if (!node.isTextual()) {
            errors.put(path + ".type", "必须是字符串");
            return null;
        }
        String type = node.asText();
        if (!COMPONENT_TYPES.contains(type)) {
            errors.put(path + ".type", "未注册的组件类型：" + type);
            return null;
        }
        if (schemaVersion == SCHEMA_VERSION_V1 && !V1_COMPONENT_TYPES.contains(type)) {
            errors.put(path + ".type", "v1 配置不支持该组件类型，请升级到 schemaVersion=2");
            return null;
        }
        return type;
    }

    private String validateDataSource(JsonNode component, String type, String path, Map<String, String> errors) {
        String expected = DATA_SOURCE_BY_TYPE.get(type);
        JsonNode node = component.get("dataSource");
        if (expected == null || expected.isEmpty()) {
            // 这几种组件的 dataSource 固定是 null
            if (node == null || !node.isNull()) {
                errors.put(path + ".dataSource", "该组件类型的 dataSource 必须为 null");
            }
            return null;
        }
        if (node == null || node.isNull()) {
            errors.put(path + ".dataSource", "不能为空，必须等于 " + expected);
            return expected;
        }
        if (!node.isTextual()) {
            errors.put(path + ".dataSource", "必须是字符串");
            return expected;
        }
        if (!expected.equals(node.asText())) {
            errors.put(path + ".dataSource", "必须等于该组件类型的固定数据源 " + expected);
            return expected;
        }
        return expected;
    }

    private Map<String, Object> validateProps(JsonNode component, String type, String path,
            Map<String, String> errors) {
        List<PropSpec> specs = COMPONENT_PROPS.getOrDefault(type, List.of());
        Map<String, Object> props = new LinkedHashMap<>();
        String propsPath = path + ".props";

        JsonNode propsNode = component.get("props");
        if (propsNode == null || propsNode.isNull() || !propsNode.isObject()) {
            errors.put(propsPath, "组件属性必须是对象");
            for (PropSpec spec : specs) {
                props.put(spec.key(), spec.defaultValue());
            }
            return props;
        }

        Set<String> allowedKeys = new LinkedHashSet<>();
        for (PropSpec spec : specs) {
            allowedKeys.add(spec.key());
        }
        propsNode.fieldNames().forEachRemaining(key -> {
            if (!allowedKeys.contains(key)) {
                errors.put(propsPath + "." + key, "不支持的属性");
            }
        });

        for (PropSpec spec : specs) {
            String propPath = propsPath + "." + spec.key();
            JsonNode valueNode = propsNode.get(spec.key());
            if (valueNode == null) {
                props.put(spec.key(), spec.defaultValue());
                continue;
            }
            if (valueNode.isNull()) {
                errors.put(propPath, "值不能为 null");
                props.put(spec.key(), spec.defaultValue());
                continue;
            }
            if (valueNode.isContainerNode()) {
                errors.put(propPath, "属性值必须是标量，禁止嵌套对象或数组");
                props.put(spec.key(), spec.defaultValue());
                continue;
            }
            validatePropValue(spec, valueNode, propPath, props, errors);
        }
        return props;
    }

    private void validatePropValue(PropSpec spec, JsonNode valueNode, String propPath,
            Map<String, Object> props, Map<String, String> errors) {
        switch (spec.type()) {
            case STRING -> {
                if (!valueNode.isTextual()) {
                    errors.put(propPath, "必须是字符串");
                    return;
                }
                String value = valueNode.asText();
                if (value.strip().length() < spec.minLength() || value.length() > spec.maxValue()) {
                    errors.put(propPath, "长度必须在 " + spec.minLength() + " 到 " + spec.maxValue() + " 个字符之间");
                    return;
                }
                if (spec.httpUrlOrBlank() && !value.isBlank() && !UrlRules.isValidHttpUrl(value)) {
                    errors.put(propPath, "需要为空或 http(s) 开头的绝对 URL");
                    return;
                }
                props.put(spec.key(), value);
            }
            case CSV_LIST -> {
                if (!valueNode.isTextual()) {
                    errors.put(propPath, "必须是字符串");
                    return;
                }
                String value = valueNode.asText();
                if (value.length() > spec.maxValue()) {
                    errors.put(propPath, "长度不能超过 " + spec.maxValue() + " 个字符");
                    return;
                }
                if (!value.isBlank()) {
                    String[] parts = value.split(",", -1);
                    if (parts.length > 5) {
                        errors.put(propPath, "最多 5 项，英文逗号分隔");
                        return;
                    }
                    for (String part : parts) {
                        String item = part.strip();
                        if (item.length() < 1 || item.length() > 20) {
                            errors.put(propPath, "每一项长度必须在 1 到 20 个字符之间");
                            return;
                        }
                    }
                }
                props.put(spec.key(), value);
            }
            case INTEGER -> {
                if (!valueNode.isIntegralNumber() || !valueNode.canConvertToInt()) {
                    errors.put(propPath, "必须是整数");
                    return;
                }
                int value = valueNode.asInt();
                if (value < spec.minValue() || value > spec.maxValue()) {
                    errors.put(propPath, "必须在 " + spec.minValue() + " 到 " + spec.maxValue() + " 之间");
                    return;
                }
                props.put(spec.key(), value);
            }
            case BOOLEAN -> {
                if (!valueNode.isBoolean()) {
                    errors.put(propPath, "必须是布尔值");
                    return;
                }
                props.put(spec.key(), valueNode.asBoolean());
            }
            case ENUM -> {
                if (!valueNode.isTextual()) {
                    errors.put(propPath, "必须是字符串");
                    return;
                }
                String value = valueNode.asText();
                if (!spec.allowedValues().contains(value)) {
                    errors.put(propPath, "只能是 " + String.join("、", spec.allowedValues()));
                    return;
                }
                props.put(spec.key(), value);
            }
            default -> errors.put(propPath, "不支持的属性类型");
        }
    }

    // action

    private PageAction validateAction(JsonNode component, String type, String path,
            PageValidationContext ctx, Map<String, String> errors) {
        String actionPath = path + ".action";
        JsonNode actionNode = component.get("action");
        if (!"button".equals(type)) {
            if (actionNode != null && !actionNode.isNull()) {
                errors.put(actionPath, "只有 button 组件支持 action");
            }
            return null;
        }
        if (actionNode == null || actionNode.isNull() || !actionNode.isObject()) {
            errors.put(actionPath, "按钮组件必须配置 action");
            return null;
        }
        JsonNode typeNode = actionNode.get("type");
        if (typeNode == null || typeNode.isNull()) {
            errors.put(actionPath + ".type", "不能为空");
            return null;
        }
        if (!typeNode.isTextual()) {
            errors.put(actionPath + ".type", "必须是字符串");
            return null;
        }
        String actionType = typeNode.asText();
        if (!ACTION_TYPES.contains(actionType)) {
            errors.put(actionPath + ".type", "不支持的动作类型：" + actionType + "（仅 navigate / externalLink / none）");
            return null;
        }
        return switch (actionType) {
            case PageAction.TYPE_NAVIGATE -> validateNavigateAction(actionNode, actionPath, ctx, errors);
            case PageAction.TYPE_EXTERNAL_LINK -> validateExternalLinkAction(actionNode, actionPath, errors);
            default -> new PageAction(PageAction.TYPE_NONE, null, null, null);
        };
    }

    private PageAction validateNavigateAction(JsonNode actionNode, String actionPath,
            PageValidationContext ctx, Map<String, String> errors) {
        JsonNode targetNode = actionNode.get("targetPageId");
        String targetPath = actionPath + ".targetPageId";
        if (targetNode == null || targetNode.isNull()) {
            errors.put(targetPath, "navigate 动作必须指定 targetPageId");
            return null;
        }
        if (!targetNode.isTextual()) {
            errors.put(targetPath, "必须是字符串");
            return null;
        }
        String target = targetNode.asText();
        if (!PAGE_ID_PATTERN.matcher(target).matches()) {
            errors.put(targetPath, "页面稳定 ID 格式不合法（^page_[a-z0-9_]{2,32}$）");
            return null;
        }
        String status = ctx == null ? null : ctx.targetPageStatuses().get(target);
        if (status == null) {
            errors.put(targetPath, "目标页面不存在");
            return null;
        }
        if ("DISABLED".equals(status)) {
            errors.put(targetPath, "目标页面已停用");
            return null;
        }
        return new PageAction(PageAction.TYPE_NAVIGATE, target, null, null);
    }

    private PageAction validateExternalLinkAction(JsonNode actionNode, String actionPath,
            Map<String, String> errors) {
        JsonNode urlNode = actionNode.get("url");
        String urlPath = actionPath + ".url";
        if (urlNode == null || urlNode.isNull()) {
            errors.put(urlPath, "externalLink 动作必须指定 url");
            return null;
        }
        if (!urlNode.isTextual()) {
            errors.put(urlPath, "必须是字符串");
            return null;
        }
        String url = urlNode.asText();
        if (url.length() < 1 || url.length() > UrlRules.MAX_URL_LENGTH) {
            errors.put(urlPath, "长度必须在 1 到 300 个字符之间");
            return null;
        }
        if (!UrlRules.isValidHttpUrl(url)) {
            errors.put(urlPath, "必须是 http/https 绝对 URL（禁止 javascript:、data:、相对地址）");
            return null;
        }
        JsonNode targetNode = actionNode.get("targetPageId");
        if (targetNode != null && !targetNode.isNull()
                && !(targetNode.isTextual() && targetNode.asText().isBlank())) {
            errors.put(actionPath + ".targetPageId", "externalLink 动作不能设置 targetPageId");
            return null;
        }
        boolean openInNewTab = true;
        JsonNode openNode = actionNode.get("openInNewTab");
        if (openNode != null && !openNode.isNull()) {
            if (!openNode.isBoolean()) {
                errors.put(actionPath + ".openInNewTab", "必须是布尔值");
                return null;
            }
            openInNewTab = openNode.asBoolean();
        }
        return new PageAction(PageAction.TYPE_EXTERNAL_LINK, null, url, openInNewTab);
    }

    // 工具方法

    private String requireText(JsonNode parent, String field, String path, int minLength, int maxLength,
            Map<String, String> errors) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            errors.put(path, "不能为空");
            return null;
        }
        if (!node.isTextual()) {
            errors.put(path, "必须是字符串");
            return null;
        }
        String value = node.asText();
        int length = value.strip().length();
        if (length < minLength || length > maxLength) {
            errors.put(path, "长度必须在 " + minLength + " 到 " + maxLength + " 个字符之间");
            return value;
        }
        return value;
    }

    private Integer requireInteger(JsonNode parent, String field, String path, int min, int max,
            Map<String, String> errors) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            errors.put(path, "不能为空");
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            errors.put(path, "必须是整数");
            return null;
        }
        int value = node.asInt();
        if (value < min || value > max) {
            errors.put(path, "必须在 " + min + " 到 " + max + " 之间");
            return null;
        }
        return value;
    }

    private Boolean requireBoolean(JsonNode parent, String field, String path, Map<String, String> errors) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            errors.put(path, "不能为空");
            return null;
        }
        if (!node.isBoolean()) {
            errors.put(path, "必须是布尔值");
            return null;
        }
        return node.asBoolean();
    }

    private ApiException fail(Map<String, String> errors) {
        Map.Entry<String, String> first = errors.entrySet().iterator().next();
        String message = "页面配置校验失败：" + first.getKey() + " " + first.getValue();
        return ApiException.badRequest(message, new LinkedHashMap<>(errors));
    }

    // 每种组件允许的属性

    private enum PropType {
        STRING, INTEGER, BOOLEAN, ENUM, CSV_LIST
    }

    // 一个属性的类型和取值范围
    private record PropSpec(String key, PropType type, int minValue, int maxValue,
            List<String> allowedValues, Object defaultValue, boolean httpUrlOrBlank, int minLength) {
    }

    private static PropSpec text(String key, int maxLength, String defaultValue) {
        return new PropSpec(key, PropType.STRING, 0, maxLength, List.of(), defaultValue, false, 0);
    }

    private static PropSpec requiredText(String key, int maxLength, String defaultValue) {
        return new PropSpec(key, PropType.STRING, 0, maxLength, List.of(), defaultValue, false, 1);
    }

    private static PropSpec urlText(String key, int maxLength) {
        return new PropSpec(key, PropType.STRING, 0, maxLength, List.of(), "", true, 0);
    }

    private static PropSpec csvList(String key, int maxLength, String defaultValue) {
        return new PropSpec(key, PropType.CSV_LIST, 0, maxLength, List.of(), defaultValue, false, 0);
    }

    private static PropSpec integer(String key, int min, int max, int defaultValue) {
        return new PropSpec(key, PropType.INTEGER, min, max, List.of(), defaultValue, false, 0);
    }

    private static PropSpec bool(String key, boolean defaultValue) {
        return new PropSpec(key, PropType.BOOLEAN, 0, 0, List.of(), defaultValue, false, 0);
    }

    private static PropSpec enumeration(String key, List<String> allowed, String defaultValue) {
        return new PropSpec(key, PropType.ENUM, 0, 0, allowed, defaultValue, false, 0);
    }

    private static Map<String, List<PropSpec>> buildPropSpecs() {
        Map<String, List<PropSpec>> specs = new LinkedHashMap<>();
        // 旧版就有的组件
        specs.put("hero", List.of(
                text("headline", 60, ""),
                text("subheadline", 120, ""),
                urlText("backgroundImage", 300),
                bool("showGameName", true),
                enumeration("align", List.of("left", "center"), "center")));
        specs.put("about", List.of(
                text("body", 1000, ""),
                bool("showTagline", true)));
        specs.put("notice_list", List.of(
                integer("limit", 1, 10, 3),
                bool("showPinned", true)));
        specs.put("footer", List.of(
                text("copyright", 120, ""),
                text("note", 80, "")));
        // 后来加的组件
        specs.put("button", List.of(
                requiredText("label", 30, "按钮"),
                enumeration("variant", List.of("primary", "secondary", "ghost"), "primary"),
                text("icon", 8, ""),
                enumeration("align", List.of("left", "center", "right"), "left")));
        specs.put("changelog_list", List.of(
                integer("limit", 1, 20, 6),
                bool("showVersion", true)));
        specs.put("docs_list", List.of(
                integer("limit", 1, 20, 8),
                text("category", 40, "")));
        specs.put("team_grid", List.of(
                integer("columns", 1, 4, 3),
                bool("showRole", true)));
        specs.put("issue_form", List.of(
                text("intro", 200, ""),
                csvList("categories", 120, "Bug 反馈,体验建议,其他"),
                text("successMessage", 100, "感谢反馈！我们会尽快处理。")));
        specs.put("links_grid", List.of(
                integer("columns", 1, 4, 3),
                bool("showDescription", true)));
        // 首页的问题处理进展
        specs.put("issue_progress", List.of(
                integer("limit", 1, 20, 5),
                text("emptyText", 60, "")));
        return specs;
    }
}
