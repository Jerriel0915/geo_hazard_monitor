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

    @Test
    @DisplayName("getLong / getDouble / getFloat / getBigDecimal 委托")
    void numericGettersDelegate() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getLong("k")).thenReturn(100L);
            mocked.when(() -> CacheUtil.getLong("k", 1L)).thenReturn(100L);
            mocked.when(() -> CacheUtil.getDouble("k")).thenReturn(3.14);
            mocked.when(() -> CacheUtil.getDouble("k", 0.0)).thenReturn(3.14);
            mocked.when(() -> CacheUtil.getFloat("k")).thenReturn(1.5f);
            mocked.when(() -> CacheUtil.getFloat("k", 0.0f)).thenReturn(1.5f);
            BigDecimal bd = new BigDecimal("99.9");
            mocked.when(() -> CacheUtil.getBigDecimal("k")).thenReturn(bd);
            mocked.when(() -> CacheUtil.getBigDecimal("k", BigDecimal.ZERO)).thenReturn(bd);

            ScriptCacheOps ops = new ScriptCacheOps();
            assertThat(ops.getLong("k")).isEqualTo(100L);
            assertThat(ops.getLong("k", 1L)).isEqualTo(100L);
            assertThat(ops.getDouble("k")).isEqualTo(3.14);
            assertThat(ops.getDouble("k", 0.0)).isEqualTo(3.14);
            assertThat(ops.getFloat("k")).isEqualTo(1.5f);
            assertThat(ops.getFloat("k", 0.0f)).isEqualTo(1.5f);
            assertThat(ops.getBigDecimal("k")).isEqualByComparingTo(bd);
            assertThat(ops.getBigDecimal("k", BigDecimal.ZERO)).isEqualByComparingTo(bd);

            mocked.verify(() -> CacheUtil.getLong("k"));
            mocked.verify(() -> CacheUtil.getLong("k", 1L));
            mocked.verify(() -> CacheUtil.getDouble("k"));
            mocked.verify(() -> CacheUtil.getDouble("k", 0.0));
            mocked.verify(() -> CacheUtil.getFloat("k"));
            mocked.verify(() -> CacheUtil.getFloat("k", 0.0f));
            mocked.verify(() -> CacheUtil.getBigDecimal("k"));
            mocked.verify(() -> CacheUtil.getBigDecimal("k", BigDecimal.ZERO));
        }
    }

    @Test
    @DisplayName("getString / getBoolean 委托")
    void stringAndBooleanDelegate() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getString("k")).thenReturn("v");
            mocked.when(() -> CacheUtil.getString("k", "d")).thenReturn("v");
            mocked.when(() -> CacheUtil.getBoolean("k")).thenReturn(true);
            mocked.when(() -> CacheUtil.getBoolean("k", false)).thenReturn(true);

            ScriptCacheOps ops = new ScriptCacheOps();
            assertThat(ops.getString("k")).isEqualTo("v");
            assertThat(ops.getString("k", "d")).isEqualTo("v");
            assertThat(ops.getBoolean("k")).isTrue();
            assertThat(ops.getBoolean("k", false)).isTrue();

            mocked.verify(() -> CacheUtil.getString("k"));
            mocked.verify(() -> CacheUtil.getString("k", "d"));
            mocked.verify(() -> CacheUtil.getBoolean("k"));
            mocked.verify(() -> CacheUtil.getBoolean("k", false));
        }
    }

    @Test
    @DisplayName("通用操作 set/delete/hasKey/expire/getExpire 委托")
    void universalOpsDelegate() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            ScriptCacheOps ops = new ScriptCacheOps();

            ops.set("k", "v");
            mocked.verify(() -> CacheUtil.set("k", "v"));

            ops.set("k", "v", 30L, TimeUnit.MINUTES);
            mocked.verify(() -> CacheUtil.set("k", "v", 30L, TimeUnit.MINUTES));

            mocked.when(() -> CacheUtil.delete("k")).thenReturn(true);
            assertThat(ops.delete("k")).isTrue();

            mocked.when(() -> CacheUtil.hasKey("k")).thenReturn(true);
            assertThat(ops.hasKey("k")).isTrue();

            mocked.when(() -> CacheUtil.expire("k", 60L)).thenReturn(true);
            assertThat(ops.expire("k", 60L)).isTrue();

            mocked.when(() -> CacheUtil.expire("k", 5L, TimeUnit.SECONDS)).thenReturn(true);
            assertThat(ops.expire("k", 5L, TimeUnit.SECONDS)).isTrue();

            mocked.when(() -> CacheUtil.getExpire("k")).thenReturn(42L);
            assertThat(ops.getExpire("k")).isEqualTo(42L);
        }
    }

    @Test
    @DisplayName("CacheUtil 抛异常时 wrapper 透传 (不吞噬)")
    void exceptionPropagated() {
        try (MockedStatic<CacheUtil> mocked = mockStatic(CacheUtil.class)) {
            mocked.when(() -> CacheUtil.getInt("k"))
                  .thenThrow(new RuntimeException("redis down"));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new ScriptCacheOps().getInt("k"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("redis down");
        }
    }
}
