package com.lelcc.common;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模板资源自检：templates/** 全是纯数据，写错一个属性名只在「应用模板」那一刻才炸，
 * 而应用模板是会覆盖线上页面的重操作——把要连数据库才能发现的错误拖到那时候才暴露代价太大。
 * 这里把应用模板时会做的那几类校验提前到单元测试，不连数据库：
 *
 * <ol>
 *   <li>组件库声明的每个属性 key 都在 {@link ComponentPropertyRegistry} 里登记过
 *       （对应 TemplateServiceImpl#checkSchema）；</li>
 *   <li>组件默认样式/默认内容、页面节点上的每个值都能过属性校验
 *       （对应 TemplateServiceImpl#toComponentType 与 UiPageServiceImpl#saveNodes 里的 UiValueValidator）；</li>
 *   <li>manifest 声明的页面文件都在，页面文件也都被 manifest 声明了，路由地址不重复。</li>
 * </ol>
 */
class TemplateResourceTest {

    private static final String ROOT = "templates/";
    private static final List<String> CATEGORIES = List.of("base", "layout", "interactive", "business");

    @Test
    void everyTemplateInIndexHasReadableManifestAndPages() {
        JSONObject index = JSONUtil.parseObj(read(ROOT + "index.json"));
        JSONArray codes = index.getJSONArray("templates");
        assertThat(codes).as("templates/index.json 必须声明至少一个模板").isNotEmpty();

        for (Object item : codes) {
            String code = String.valueOf(item);
            JSONObject manifest = JSONUtil.parseObj(read(ROOT + code + "/manifest.json"));
            assertThat(manifest.getInt("version")).as(code + " 缺少 version").isNotNull();
            assertThat(manifest.getStr("nameZh")).as(code + " 缺少 nameZh").isNotBlank();

            JSONArray pageKeys = manifest.getJSONArray("pageKeys");
            assertThat(pageKeys).as(code + " 没有声明任何页面").isNotEmpty();
            for (Object key : pageKeys) {
                assertThat(read(ROOT + code + "/pages/" + key + ".json"))
                        .as(code + " 声明的页面文件不存在：" + key).isNotBlank();
            }
        }
    }

    @Test
    void componentSchemasOnlyUseRegisteredProperties() {
        for (String code : templateCodes()) {
            JSONObject file = JSONUtil.parseObj(read(ROOT + code + "/components.json"));
            JSONArray components = file.getJSONArray("components");
            assertThat(components).as(code + " 没有组件库定义").isNotEmpty();
            for (Object item : components) {
                JSONObject component = (JSONObject) item;
                String typeCode = component.getStr("typeCode");
                assertThat(CATEGORIES).as(typeCode + " 的分类不合法").contains(component.getStr("category", "base"));
                assertThat(component.getStr("renderKind")).as(typeCode + " 缺少渲染内核").isNotBlank();
                for (String field : List.of("styleSchema", "contentSchema", "behaviorSchema")) {
                    for (String key : strList(component.getJSONArray(field))) {
                        assertThat(ComponentPropertyRegistry.isKnown(key))
                                .as("模板组件 " + typeCode + " 的 " + field + " 属性没在属性库里登记：" + key)
                                .isTrue();
                    }
                }
            }
        }
    }

    @Test
    void componentDefaultsPassPropertyValidation() {
        for (String code : templateCodes()) {
            for (Object item : JSONUtil.parseObj(read(ROOT + code + "/components.json")).getJSONArray("components")) {
                JSONObject component = (JSONObject) item;
                String typeCode = component.getStr("typeCode");
                String styleError = UiValueValidator.validate(strList(component.getJSONArray("styleSchema")),
                        map(component.getJSONObject("defaultStyle")), typeCode + " / 默认样式");
                assertThat(styleError).isNull();
                String contentError = UiValueValidator.validate(strList(component.getJSONArray("contentSchema")),
                        map(component.getJSONObject("defaultContent")), typeCode + " / 默认内容");
                assertThat(contentError).isNull();
            }
        }
    }

    /**
     * 页面节点上的值必须能过属性校验；节点类型在组件库里声明过时，用到的 key 还必须在该类型开放的属性里。
     * 后者正是"页面里用了组件没开放的属性"这类错误——真到应用模板时才会被 saveNodes 拦下。
     */
    @Test
    void pageNodeValuesPassPropertyValidationAndRespectComponentSchema() {
        for (String code : templateCodes()) {
            Map<String, Set<String>> schemas = declaredSchemas(code);
            JSONObject manifest = JSONUtil.parseObj(read(ROOT + code + "/manifest.json"));
            for (Object key : manifest.getJSONArray("pageKeys")) {
                String pageKey = String.valueOf(key);
                JSONObject page = JSONUtil.parseObj(read(ROOT + code + "/pages/" + pageKey + ".json"));
                JSONArray nodes = page.getJSONArray("nodes");
                assertThat(nodes).as(code + "/" + pageKey + " 没有节点").isNotEmpty();

                Set<Long> ids = new LinkedHashSet<>();
                for (Object nodeItem : nodes) {
                    JSONObject node = (JSONObject) nodeItem;
                    String where = code + "/" + pageKey + " 节点 " + node.getStr("nodeName");
                    assertThat(node.getStr("typeCode")).as(where + " 缺少 typeCode").isNotBlank();
                    assertThat(ids.add(node.getLong("id"))).as(where + " 的 id 在同一页里重复").isTrue();

                    Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
                    groups.put("styleJson", map(node.getJSONObject("styleJson")));
                    groups.put("contentJson", map(node.getJSONObject("contentJson")));
                    groups.put("behaviorJson", map(node.getJSONObject("behaviorJson")));
                    for (Map.Entry<String, Map<String, Object>> group : groups.entrySet()) {
                        for (Map.Entry<String, Object> entry : group.getValue().entrySet()) {
                            String property = entry.getKey();
                            assertThat(ComponentPropertyRegistry.isKnown(property))
                                    .as(where + " 的 " + group.getKey() + " 用了没登记的属性：" + property)
                                    .isTrue();
                            assertThat(ComponentPropertyRegistry.validate(property, entry.getValue()))
                                    .as(where + " 的 " + property + " 取值不合法：" + entry.getValue())
                                    .isNull();
                            Set<String> open = schemas.get(node.getStr("typeCode"));
                            if (open != null) {
                                assertThat(open).as(where + " 用了组件未开放的属性：" + property).contains(property);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void pageRoutesAndKeysAreUniqueAcrossTemplates() {
        Map<String, String> routeOwner = new LinkedHashMap<>();
        Set<String> pageKeyOwner = new LinkedHashSet<>();
        for (String code : templateCodes()) {
            JSONObject manifest = JSONUtil.parseObj(read(ROOT + code + "/manifest.json"));
            JSONArray pageKeys = manifest.getJSONArray("pageKeys");
            List<String> files = pageFiles(code);
            List<String> declared = strList(pageKeys);
            if (files != null) {
                // 目录能列出来时顺带查"页面文件写了却没进 manifest"——漏登记就是模板里多一个孤儿文件
                assertThat(declared).as(code + " 的 pages/ 里有没被 manifest 声明的文件").containsAll(files);
            }
            for (Object key : pageKeys) {
                String pageKey = String.valueOf(key);
                assertThat(pageKeyOwner.add(code + "/" + pageKey)).isTrue();
                JSONObject page = JSONUtil.parseObj(read(ROOT + code + "/pages/" + pageKey + ".json"));
                String route = page.getStr("routePath");
                assertThat(route).as(code + "/" + pageKey + " 缺少 routePath").isNotBlank();
                assertThat(routeOwner.put(route, pageKey)).as("路由地址被两个页面占用：" + route).isNull();
            }
        }
    }

    /* ---------------- 工具 ---------------- */

    private List<String> templateCodes() {
        List<String> codes = new ArrayList<>();
        for (Object item : JSONUtil.parseObj(read(ROOT + "index.json")).getJSONArray("templates")) {
            codes.add(String.valueOf(item));
        }
        return codes;
    }

    /** 组件库里声明过的类型 -> 它开放的内容属性，用于校验页面节点没有越权用属性 */
    private Map<String, Set<String>> declaredSchemas(String code) {
        Map<String, Set<String>> schemas = new LinkedHashMap<>();
        for (Object item : JSONUtil.parseObj(read(ROOT + code + "/components.json")).getJSONArray("components")) {
            JSONObject component = (JSONObject) item;
            Set<String> open = new LinkedHashSet<>(strList(component.getJSONArray("styleSchema")));
            open.addAll(strList(component.getJSONArray("contentSchema")));
            open.addAll(strList(component.getJSONArray("behaviorSchema")));
            schemas.put(component.getStr("typeCode"), open);
        }
        return schemas;
    }

    /** pages/ 目录下的 *.json 文件名；从 jar 里跑时列不出来，返回 null 表示跳过该项检查 */
    private List<String> pageFiles(String code) {
        URL url = TemplateResourceTest.class.getClassLoader().getResource(ROOT + code + "/pages");
        if (url == null || !"file".equals(url.getProtocol())) {
            return null;
        }
        try (var stream = Files.list(Path.of(url.toURI()))) {
            return stream.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> strList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            array.forEach(item -> list.add(String.valueOf(item)));
        }
        return list;
    }

    private static Map<String, Object> map(JSONObject object) {
        if (object == null || object.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(object);
    }

    private static String read(String path) {
        try (InputStream in = TemplateResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("模板资源不存在：" + path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("读取模板资源失败：" + path, e);
        }
    }
}
