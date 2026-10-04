package com.lelcc.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图片值类型（对象存储 objectKey 与完整地址并存）与 url 类型的校验边界。
 * 组件属性库与站点配置注册表用的是同一条 IMAGE 规则，这里覆盖前者。
 */
class ComponentPropertyRegistryTest {

    private static final String KEY = "upload/2026/10/04/4be43da786c041bdafcccc39fc979aa6.png";

    @Test
    void imageAcceptsObjectKeyAndAbsoluteUrl() {
        assertThat(ComponentPropertyRegistry.validate("imageUrl", KEY)).isNull();
        assertThat(ComponentPropertyRegistry.validate("imageUrl", "/static/logo.png")).isNull();
        assertThat(ComponentPropertyRegistry.validate("imageUrl", "https://cdn.example.com/a.png")).isNull();
        assertThat(ComponentPropertyRegistry.validate("backgroundImage", KEY)).isNull();
    }

    @Test
    void imageRejectsPathTraversalAndUnknownKeyShape() {
        assertThat(ComponentPropertyRegistry.validate("imageUrl", "upload/../../etc/passwd")).isNotNull();
        assertThat(ComponentPropertyRegistry.validate("imageUrl", "upload/latest/a.png")).isNotNull();
        assertThat(ComponentPropertyRegistry.validate("imageUrl", "ftp://example.com/a.png")).isNotNull();
    }

    @Test
    void imageTreatsBlankAsUnset() {
        assertThat(ComponentPropertyRegistry.validate("imageUrl", "")).isNull();
        assertThat(ComponentPropertyRegistry.validate("imageUrl", null)).isNull();
    }

    @Test
    void urlTypeStillRejectsObjectKey() {
        assertThat(ComponentPropertyRegistry.validate("linkUrl", KEY)).isNotNull();
        assertThat(ComponentPropertyRegistry.validate("linkUrl", "/post/1")).isNull();
    }
}
