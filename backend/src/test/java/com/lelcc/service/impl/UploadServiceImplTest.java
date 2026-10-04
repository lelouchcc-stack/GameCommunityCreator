package com.lelcc.service.impl;

import com.lelcc.common.BusinessException;
import com.lelcc.dto.StorageConfigVO;
import com.lelcc.dto.UploadVO;
import com.lelcc.service.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 上传校验与 objectKey 生成：与具体存储后端无关，用一个内存假实现验证 */
class UploadServiceImplTest {

    private final FakeStorage storage = new FakeStorage();
    private final UploadServiceImpl service = new UploadServiceImpl();

    @BeforeEach
    void injectStorage() {
        ReflectionTestUtils.setField(service, "fileStorage", storage);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请选择要上传的图片");
    }

    @Test
    void rejectsDisallowedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "a.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    void rejectsNonImageContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不是图片");
    }

    @Test
    void rejectsOversizedFile() {
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", tooBig);

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过 5MB");
    }

    @Test
    void storesUnderDatedObjectKeyAndReturnsComposedUrl() {
        byte[] content = "png-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "cover.PNG", "image/png", content);

        UploadVO result = service.uploadImage(file);

        assertThat(result.key()).matches("^upload/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.png$");
        assertThat(result.url()).isEqualTo("/static/" + result.key());
        assertThat(result.name()).isEqualTo("cover.PNG");
        assertThat(result.size()).isEqualTo(content.length);
        assertThat(storage.objects).containsEntry(result.key(), content);
    }

    @Test
    void storageConfigExposesProviderAndBaseUrl() {
        StorageConfigVO config = service.storageConfig();

        assertThat(config.provider()).isEqualTo("local");
        assertThat(config.publicBaseUrl()).isEqualTo("/static");
    }

    /** 只记录写入结果的内存实现，模拟任一 provider 的落盘行为 */
    private static class FakeStorage implements FileStorage {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        @Override
        public void put(String key, byte[] content, String contentType) {
            objects.put(key, content);
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }

        @Override
        public String urlOf(String key) {
            return FileStorage.joinUrl(publicBaseUrl(), key);
        }

        @Override
        public String publicBaseUrl() {
            return "/static";
        }

        @Override
        public String provider() {
            return "local";
        }
    }
}
