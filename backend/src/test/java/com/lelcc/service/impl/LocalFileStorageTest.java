package com.lelcc.service.impl;

import com.lelcc.common.BusinessException;
import com.lelcc.config.UploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 本地存储：落盘、URL 现拼、删除、路径穿越防护 */
class LocalFileStorageTest {

    @TempDir
    Path dir;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties();
        properties.getLocal().setDir(dir.toString());
        properties.getLocal().setPublicBaseUrl("/static");
        storage = new LocalFileStorage(properties);
    }

    @Test
    void putWritesFileUnderRoot() throws Exception {
        storage.put("upload/2026/10/04/a.png", new byte[]{1, 2, 3}, "image/png");

        Path saved = dir.resolve("upload/2026/10/04/a.png");
        assertThat(saved).exists();
        assertThat(Files.readAllBytes(saved)).containsExactly(1, 2, 3);
    }

    @Test
    void urlOfComposesPublicBaseAndKey() {
        assertThat(storage.urlOf("upload/2026/10/04/a.png"))
                .isEqualTo("/static/upload/2026/10/04/a.png");
    }

    @Test
    void deleteRemovesSavedObject() {
        storage.put("upload/a.png", new byte[]{1}, "image/png");

        storage.delete("upload/a.png");

        assertThat(dir.resolve("upload/a.png")).doesNotExist();
    }

    @Test
    void deleteOnMissingKeyIsSilent() {
        storage.delete("upload/not-there.png");
    }

    @Test
    void rejectsKeyThatEscapesRoot() {
        assertThatThrownBy(() -> storage.put("../../evil.png", new byte[]{1}, "image/png"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法的存储路径");
    }

    @Test
    void providerNameIsLocal() {
        assertThat(storage.provider()).isEqualTo("local");
    }
}
