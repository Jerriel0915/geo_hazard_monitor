package com.zwei.iot.timeseries.compute;

import com.zwei.common.utils.CacheUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@DisplayName("ScriptCacheOps (CacheUtil 实例外壳)")
class ScriptCacheOpsTest {

    @Test
    @DisplayName("getInt(key) 委托 CacheUtil.getInt")
    void getIntDelegates() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k")).thenReturn(42);
            assertThat(new ScriptCacheOps().getInt("k")).isEqualTo(42);
            mocked.verify(() -> CacheUtil.getInt("k"));
        }
    }

    @Test
    @DisplayName("getInt(key, default) 委托默认值重载")
    void getIntDefaultDelegates() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k", 7)).thenReturn(7);
            assertThat(new ScriptCacheOps().getInt("k", 7)).isEqualTo(7);
            mocked.verify(() -> CacheUtil.getInt("k", 7));
        }
    }
}
