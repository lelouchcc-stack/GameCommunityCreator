package com.gcc.community.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gcc.community.common.ApiException;
import com.gcc.community.domain.PageConfig;
import com.gcc.community.validation.PageConfigValidator;
import com.gcc.community.validation.PageValidationContext;

/** 默认页面模板的读取和渲染。 */
@Service
public class DefaultPageTemplateService {

    public static final String TEMPLATE_DIR = "templates/pages/";
    public static final String PLACEHOLDER_PAGE_ID = "{{pageId}}";
    public static final String PLACEHOLDER_GAME_NAME = "{{gameName}}";
    public static final String PLACEHOLDER_SLUG = "{{slug}}";
    public static final String PLACEHOLDER_PAGE_TITLE = "{{pageTitle}}";

    /** 渲染模板时用到的参数。 */
    public record Placeholders(String pageId, String gameName, String slug, String pageTitle) {
    }

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PageConfigValidator validator;

    // 读模板 JSON
    public JsonNode loadTemplate(String templateKey) {
        if (templateKey == null || !PageTypes.TEMPLATE_KEYS.contains(templateKey)) {
            throw ApiException.badRequest("模板不存在：" + templateKey);
        }
        ClassPathResource resource = new ClassPathResource(TEMPLATE_DIR + templateKey + ".json");
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载页面模板：" + templateKey, ex);
        }
    }

    // 递归替换占位符，不改模板本身
    public JsonNode renderNode(String templateKey, Placeholders placeholders) {
        return replacePlaceholders(loadTemplate(templateKey), placeholders);
    }

    // 渲染并校验
    public PageConfig render(String templateKey, Placeholders placeholders, PageValidationContext ctx) {
        return validator.validateForSave(renderNode(templateKey, placeholders), ctx);
    }

    // 渲染成 JSON 字符串
    public String renderJson(String templateKey, Placeholders placeholders, PageValidationContext ctx) {
        try {
            return objectMapper.writeValueAsString(render(templateKey, placeholders, ctx));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("页面模板无法序列化", ex);
        }
    }

    // 页面类型用哪个模板
    public static String templateKeyForPageType(String pageType) {
        String templateKey = PageTypes.DEFAULT_TEMPLATE_BY_TYPE.get(pageType);
        if (templateKey == null) {
            throw ApiException.badRequest("不支持的页面类型：" + pageType);
        }
        return templateKey;
    }

    private JsonNode replacePlaceholders(JsonNode node, Placeholders placeholders) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode copy = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                copy.set(field.getKey(), replacePlaceholders(field.getValue(), placeholders));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                copy.add(replacePlaceholders(item, placeholders));
            }
            return copy;
        }
        if (node.isTextual()) {
            String replaced = node.asText()
                    .replace(PLACEHOLDER_PAGE_ID, nullSafe(placeholders.pageId()))
                    .replace(PLACEHOLDER_GAME_NAME, nullSafe(placeholders.gameName()))
                    .replace(PLACEHOLDER_SLUG, nullSafe(placeholders.slug()))
                    .replace(PLACEHOLDER_PAGE_TITLE, nullSafe(placeholders.pageTitle()));
            return TextNode.valueOf(replaced);
        }
        return node;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
