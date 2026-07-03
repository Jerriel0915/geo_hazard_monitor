package com.zwei.iot.timeseries.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisReplyParser — Redis 原生命令响应解析")
class RedisReplyParserTest {

    @Nested
    @DisplayName("parseList — 安全解析为 List")
    class ParseList {

        @Test
        @DisplayName("嵌套 List<byte[]> 递归转换为 List<String>")
        void nestedByteArrays() {
            List<Object> inner = new ArrayList<>();
            inner.add("hello".getBytes(StandardCharsets.UTF_8));
            inner.add("world".getBytes(StandardCharsets.UTF_8));
            List<Object> outer = new ArrayList<>();
            outer.add(inner);

            List<Object> result = RedisReplyParser.parseList(outer);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<Object> parsed = (List<Object>) result.get(0);
            assertThat(parsed).hasSize(2);
            assertThat(new String((byte[]) parsed.get(0), StandardCharsets.UTF_8)).isEqualTo("hello");
        }

        @Test
        @DisplayName("三层嵌套 — XAUTOCLAIM 响应结构")
        void tripleNestedLikeXautoclaim() {
            // [nextStartId, [ [id, [k1,v1,...]], [id, [k1,v1,...]] ]]
            byte[] nextStart = "12345-0".getBytes(StandardCharsets.UTF_8);
            byte[] msgId = "12345-1".getBytes(StandardCharsets.UTF_8);
            List<Object> fields = new ArrayList<>();
            fields.add("payload".getBytes(StandardCharsets.UTF_8));
            fields.add("{\"value\":42}".getBytes(StandardCharsets.UTF_8));
            List<Object> entry = new ArrayList<>();
            entry.add(msgId);
            entry.add(fields);
            List<Object> entries = new ArrayList<>();
            entries.add(entry);
            List<Object> xautoclaimReply = new ArrayList<>();
            xautoclaimReply.add(nextStart);
            xautoclaimReply.add(entries);

            List<Object> result = RedisReplyParser.parseList(xautoclaimReply);

            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(byte[].class);
            assertThat(new String((byte[]) result.get(0), StandardCharsets.UTF_8)).isEqualTo("12345-0");
        }

        @Test
        @DisplayName("null 输入返回空列表")
        void nullReturnsEmpty() {
            assertThat(RedisReplyParser.parseList(null)).isEmpty();
        }

        @Test
        @DisplayName("非 List 输入返回空列表")
        void nonListReturnsEmpty() {
            assertThat(RedisReplyParser.parseList("not a list")).isEmpty();
        }

        @Test
        @DisplayName("空列表原样返回")
        void emptyListReturnsEmpty() {
            assertThat(RedisReplyParser.parseList(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("parse — 递归转换")
    class Parse {

        @Test
        @DisplayName("byte[] 原样保留")
        void byteArrayPreserved() {
            byte[] input = "test".getBytes(StandardCharsets.UTF_8);
            assertThat(RedisReplyParser.parse(input)).isSameAs(input);
        }

        @Test
        @DisplayName("null 原样返回")
        void nullPreserved() {
            assertThat(RedisReplyParser.parse(null)).isNull();
        }

        @Test
        @DisplayName("深层嵌套被完全递归")
        void deeplyNested() {
            List<Object> l3 = List.of("val".getBytes(StandardCharsets.UTF_8));
            List<Object> l2 = List.of(l3);
            List<Object> l1 = List.of(l2);

            @SuppressWarnings("unchecked")
            List<Object> result = (List<Object>) RedisReplyParser.parse(l1);
            assertThat(result).hasSize(1);
            @SuppressWarnings("unchecked")
            List<Object> r2 = (List<Object>) result.get(0);
            @SuppressWarnings("unchecked")
            List<Object> r3 = (List<Object>) r2.get(0);
            assertThat(r3).hasSize(1);
        }
    }
}
