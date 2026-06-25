package com.zwei.common.utils;

import com.zwei.common.core.redis.RedisCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CacheUtil 单元测试。
 *
 * <p>核心策略: 通过包私有方法注入 mock RedisCache, 避免依赖 SpringUtils.getBean()。
 * 仅验证委托关系 (是否调用了 RedisCache 的正确方法 + 参数透传), 不验证 Redis 实际行为。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CacheUtil (Redis 二次封装)")
class CacheUtilTest {

    @Mock
    private RedisCache cache;

    // ==================== 通用操作 (委托) ====================

    @Test
    @DisplayName("set(key,value) 委托 setCacheObject(key,value)")
    void setDelegates() {
        CacheUtil.doSet(cache, "k1", "v1");

        verify(cache).setCacheObject("k1", "v1");
    }

    @Test
    @DisplayName("set(key,value,timeout,unit) 委托 setCacheObject(Integer timeout)")
    void setWithTtlDelegates() {
        CacheUtil.doSet(cache, "k1", "v1", 30L, TimeUnit.MINUTES);

        // RedisCache 签名为 Integer timeout — 验证 int 装箱透传
        verify(cache).setCacheObject(eq("k1"), eq("v1"), eq(30), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("get 委托 getCacheObject 并透传返回值")
    void getDelegates() {
        when(cache.getCacheObject("k1")).thenReturn("hello");

        Object result = CacheUtil.doGet(cache, "k1");

        assertThat(result).isEqualTo("hello");
        verify(cache).getCacheObject("k1");
    }

    @Test
    @DisplayName("get key 不存在时返回 null")
    void getMissingReturnsNull() {
        when(cache.getCacheObject("missing")).thenReturn(null);

        assertThat(CacheUtil.doGet(cache, "missing")).isNull();
    }

    @Test
    @DisplayName("delete 委托 deleteObject")
    void deleteDelegates() {
        when(cache.deleteObject("k1")).thenReturn(true);

        boolean result = CacheUtil.doDelete(cache, "k1");

        assertThat(result).isTrue();
        verify(cache).deleteObject("k1");
    }

    @Test
    @DisplayName("hasKey 委托并支持 Boolean 返回")
    void hasKeyDelegates() {
        when(cache.hasKey("k1")).thenReturn(Boolean.TRUE);

        assertThat(CacheUtil.doHasKey(cache, "k1")).isTrue();
        verify(cache).hasKey("k1");
    }

    @Test
    @DisplayName("expire(key,timeout) 委托 expire(seconds)")
    void expireSecondsDelegates() {
        when(cache.expire(eq("k1"), anyLong())).thenReturn(true);

        assertThat(CacheUtil.doExpire(cache, "k1", 60L)).isTrue();
        verify(cache).expire("k1", 60L);
    }

    @Test
    @DisplayName("expire(key,timeout,unit) 委托带 unit 重载")
    void expireWithUnitDelegates() {
        when(cache.expire(eq("k1"), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(CacheUtil.doExpire(cache, "k1", 5L, TimeUnit.MINUTES)).isTrue();
        verify(cache).expire("k1", 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("getExpire 委托并透传 long 返回值")
    void getExpireDelegates() {
        when(cache.getExpire("k1")).thenReturn(42L);

        assertThat(CacheUtil.doGetExpire(cache, "k1")).isEqualTo(42L);
        verify(cache).getExpire("k1");
    }

    // ==================== 基础类型 getters ====================

    @Nested
    @DisplayName("getInt")
    class GetInt {
        @Test
        @DisplayName("Number 来源 → intValue")
        void fromNumber() {
            when(cache.getCacheObject("k")).thenReturn(42L);
            assertThat(CacheUtil.doGetInt(cache, "k")).isEqualTo(42);
        }

        @Test
        @DisplayName("String 来源 → Integer.parseInt")
        void fromString() {
            when(cache.getCacheObject("k")).thenReturn("123");
            assertThat(CacheUtil.doGetInt(cache, "k")).isEqualTo(123);
        }

        @Test
        @DisplayName("String 非数字 → null")
        void fromNonNumericString() {
            when(cache.getCacheObject("k")).thenReturn("abc");
            assertThat(CacheUtil.doGetInt(cache, "k")).isNull();
        }

        @Test
        @DisplayName("类型不匹配 (List) → null")
        void fromMismatch() {
            when(cache.getCacheObject("k")).thenReturn(List.of(1));
            assertThat(CacheUtil.doGetInt(cache, "k")).isNull();
        }

        @Test
        @DisplayName("key 不存在 → null (包装) / defaultValue (基本)")
        void missingKey() {
            when(cache.getCacheObject("missing")).thenReturn(null);
            assertThat(CacheUtil.doGetInt(cache, "missing")).isNull();
            assertThat(CacheUtil.doGetInt(cache, "missing", 7)).isEqualTo(7);
        }

        @Test
        @DisplayName("默认值重载: 有值返回值, 不覆盖")
        void defaultValueNotUsedWhenPresent() {
            when(cache.getCacheObject("k")).thenReturn(5);
            assertThat(CacheUtil.doGetInt(cache, "k", 99)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getLong / getDouble / getFloat / getBigDecimal")
    class NumericGetters {
        @Test
        @DisplayName("getLong: Number 与 String 来源")
        void getLongBothSources() {
            when(cache.getCacheObject("a")).thenReturn(100L);
            when(cache.getCacheObject("b")).thenReturn("999");
            assertThat(CacheUtil.doGetLong(cache, "a")).isEqualTo(100L);
            assertThat(CacheUtil.doGetLong(cache, "b")).isEqualTo(999L);
        }

        @Test
        @DisplayName("getLong: 默认值重载")
        void getLongDefault() {
            when(cache.getCacheObject("k")).thenReturn(null);
            assertThat(CacheUtil.doGetLong(cache, "k", -1L)).isEqualTo(-1L);
        }

        @Test
        @DisplayName("getDouble: Number 来源 (BigDecimal 也要能转)")
        void getDoubleFromNumber() {
            when(cache.getCacheObject("k")).thenReturn(new BigDecimal("3.14"));
            assertThat(CacheUtil.doGetDouble(cache, "k")).isEqualTo(3.14);
        }

        @Test
        @DisplayName("getDouble: String 来源 → Double.parseDouble")
        void getDoubleFromString() {
            when(cache.getCacheObject("k")).thenReturn("2.5");
            assertThat(CacheUtil.doGetDouble(cache, "k")).isEqualTo(2.5);
        }

        @Test
        @DisplayName("getDouble: 默认值重载")
        void getDoubleDefault() {
            when(cache.getCacheObject("k")).thenReturn("notnum");
            assertThat(CacheUtil.doGetDouble(cache, "k", 0.0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("getFloat: Number 与 String 来源")
        void getFloatBoth() {
            when(cache.getCacheObject("a")).thenReturn(1.5f);
            when(cache.getCacheObject("b")).thenReturn("0.25");
            assertThat(CacheUtil.doGetFloat(cache, "a")).isEqualTo(1.5f);
            assertThat(CacheUtil.doGetFloat(cache, "b")).isEqualTo(0.25f);
        }

        @Test
        @DisplayName("getBigDecimal: Long 来源 → BigDecimal.valueOf")
        void getBigDecimalFromLong() {
            when(cache.getCacheObject("k")).thenReturn(12345L);
            assertThat(CacheUtil.doGetBigDecimal(cache, "k"))
                .isEqualByComparingTo(new BigDecimal("12345"));
        }

        @Test
        @DisplayName("getBigDecimal: String 来源 (保留精度)")
        void getBigDecimalFromString() {
            when(cache.getCacheObject("k")).thenReturn("3.141592653589793238");
            assertThat(CacheUtil.doGetBigDecimal(cache, "k"))
                .isEqualByComparingTo(new BigDecimal("3.141592653589793238"));
        }

        @Test
        @DisplayName("getBigDecimal: 默认值重载")
        void getBigDecimalDefault() {
            when(cache.getCacheObject("k")).thenReturn(null);
            assertThat(CacheUtil.doGetBigDecimal(cache, "k", BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getString (严格: 仅返回 String 类型值)")
    class GetString {
        @Test
        @DisplayName("String 原值透传")
        void stringValue() {
            when(cache.getCacheObject("k")).thenReturn("hello");
            assertThat(CacheUtil.doGetString(cache, "k")).isEqualTo("hello");
        }

        @Test
        @DisplayName("Number 不 toString, 返回 null (防误导)")
        void numberReturnsNull() {
            when(cache.getCacheObject("k")).thenReturn(42);
            assertThat(CacheUtil.doGetString(cache, "k")).isNull();
        }

        @Test
        @DisplayName("默认值重载")
        void defaultValue() {
            when(cache.getCacheObject("k")).thenReturn(null);
            assertThat(CacheUtil.doGetString(cache, "k", "fallback")).isEqualTo("fallback");
        }
    }

    @Nested
    @DisplayName("getBoolean")
    class GetBoolean {
        @Test
        @DisplayName("Boolean 直接返回")
        void booleanDirect() {
            when(cache.getCacheObject("k")).thenReturn(Boolean.TRUE);
            assertThat(CacheUtil.doGetBoolean(cache, "k")).isTrue();
        }

        @Test
        @DisplayName("String 来源 → Boolean.parseBoolean")
        void stringParsed() {
            when(cache.getCacheObject("a")).thenReturn("true");
            when(cache.getCacheObject("b")).thenReturn("false");
            when(cache.getCacheObject("c")).thenReturn("anything");
            assertThat(CacheUtil.doGetBoolean(cache, "a")).isTrue();
            assertThat(CacheUtil.doGetBoolean(cache, "b")).isFalse();
            // Boolean.parseBoolean 对非 "true" (忽略大小写) 一律返回 false
            assertThat(CacheUtil.doGetBoolean(cache, "c")).isFalse();
        }

        @Test
        @DisplayName("Number 不被视为 boolean (返回 null)")
        void numberReturnsNull() {
            when(cache.getCacheObject("k")).thenReturn(1);
            assertThat(CacheUtil.doGetBoolean(cache, "k")).isNull();
        }

        @Test
        @DisplayName("默认值重载")
        void defaultValue() {
            when(cache.getCacheObject("k")).thenReturn(null);
            assertThat(CacheUtil.doGetBoolean(cache, "k", false)).isFalse();
        }
    }
}
