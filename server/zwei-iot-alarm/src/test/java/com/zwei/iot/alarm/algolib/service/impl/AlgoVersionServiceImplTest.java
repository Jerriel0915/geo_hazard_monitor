package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.service.engine.PythonAlgoExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlgoVersionServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlgoVersionServiceImpl")
class AlgoVersionServiceImplTest {

    @Mock private AlgoInfoMapper algoInfoMapper;
    @Mock private AlgoVersionMapper algoVersionMapper;
    @Mock private PythonAlgoExecutor pythonAlgoExecutor;

    @TempDir
    Path tempDir;

    private AlgoVersionServiceImpl service;

    @BeforeEach
    void setUp() {
        String tempPath = tempDir.toString();
        service = new AlgoVersionServiceImpl(algoInfoMapper, algoVersionMapper, pythonAlgoExecutor) {
            @Override
            protected String getProfilePath() {
                return tempPath;
            }
        };
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("非 zip 文件抛异常")
        void nonZipThrows() {
            MultipartFile file = new MockMultipartFile(
                    "file", "algo.tar", "application/octet-stream", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.upload(1L, "v1", null, file, "admin"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("zip");

            verify(algoVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("算法不存在抛异常")
        void algoNotFoundThrows() {
            when(algoInfoMapper.selectById(99L)).thenReturn(null);
            MultipartFile file = new MockMultipartFile(
                    "file", "algo.zip", "application/zip", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.upload(99L, "v1", null, file, "admin"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("不存在");

            verify(algoVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("版本号冲突抛异常")
        void versionDuplicateThrows() {
            when(algoInfoMapper.selectById(1L))
                    .thenReturn(AlgoInfo.builder().id(1L).code("ALGO_X").name("X").build());
            when(algoVersionMapper.checkVersionUnique(1L, "v1"))
                    .thenReturn(AlgoVersion.builder().id(50L).versionNo("v1").build());

            MultipartFile file = new MockMultipartFile(
                    "file", "algo.zip", "application/zip", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.upload(1L, "v1", null, file, "admin"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("版本号已存在");

            verify(algoVersionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("合法 zip 上传 → 解压 + 落盘 + 入库 + 返回版本 ID")
        void success() {
            when(algoInfoMapper.selectById(1L))
                    .thenReturn(AlgoInfo.builder().id(1L).code("ALGO_X").name("X").build());
            when(algoVersionMapper.checkVersionUnique(1L, "v1.0.0")).thenReturn(null);
            when(algoVersionMapper.insert(any(AlgoVersion.class))).thenAnswer(inv -> {
                inv.<AlgoVersion>getArgument(0).setId(777L);
                return 1;
            });

            byte[] content = createValidAlgoZip();
            MultipartFile file = new MockMultipartFile(
                    "file", "algo.zip", "application/zip", content);

            Long versionId = service.upload(1L, "v1.0.0", "首次上传", file, "admin");

            assertThat(versionId).isEqualTo(777L);
            verify(algoVersionMapper).insert(argThat(v ->
                    v.getAlgoId().equals(1L)
                    && v.getVersionNo().equals("v1.0.0")
                    && v.getOriginalName().equals("algo.zip")
                    && v.getFileSize().equals((long) content.length)
                    && v.getFileName().startsWith("algo-lib/")
                    && v.getSha256() != null && !v.getSha256().isEmpty()
                    && v.getWorkPath() != null && v.getWorkPath().contains("ALGO_X")
                    && v.getCreateBy().equals("admin")));
        }
    }

    @Test
    @DisplayName("delete — 版本不存在返回 0")
    void deleteNotFound() {
        when(algoVersionMapper.selectById(99L)).thenReturn(null);
        assertThat(service.delete(99L)).isEqualTo(0);
        verify(algoVersionMapper, never()).softDeleteById(any());
    }

    @Test
    @DisplayName("delete — 清理工作目录后委托 mapper.softDeleteById")
    void delete() {
        AlgoVersion version = AlgoVersion.builder()
                .id(5L).versionNo("v1").workPath(null).build();
        when(algoVersionMapper.selectById(5L)).thenReturn(version);
        when(algoVersionMapper.softDeleteById(5L)).thenReturn(1);
        assertThat(service.delete(5L)).isEqualTo(1);
        verify(algoVersionMapper).softDeleteById(5L);
    }

    /** 构造包含 algo_entry.py 的最小 zip 字节流 */
    private static byte[] createValidAlgoZip() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("algo_entry.py"));
            zos.write("# entry\n".getBytes());
            zos.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
