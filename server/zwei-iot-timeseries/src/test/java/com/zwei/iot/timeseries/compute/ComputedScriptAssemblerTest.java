package com.zwei.iot.timeseries.compute;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ComputedScriptAssembler")
class ComputedScriptAssemblerTest {

    private final ComputedScriptAssembler assembler = new ComputedScriptAssembler();

    private ComputedAttribute attr(String code, String script, int sortOrder) {
        return new ComputedAttribute(1L, 100L, code, code, "", script, sortOrder);
    }

    @Test
    @DisplayName("空列表产出空脚本")
    void emptyList() {
        assertThat(assembler.assemble(List.of())).isEmpty();
    }

    @Test
    @DisplayName("单个属性: 拼出 calc_xxx 函数 + compute 主入口")
    void singleAttr() {
        String script = assembler.assemble(List.of(
                attr("velocity", "return curData.properties.displacement / 10", 1)));

        assertThat(script).contains("def calc_velocity(curData, prevData)")
                          .contains("return curData.properties.displacement / 10")
                          .contains("def compute(curData, prevData)")
                          .contains("out.velocity = calc_velocity(curData, prevData)");
    }

    @Test
    @DisplayName("多属性按 sort_order: 后算的属性对应 try 块在后")
    void multiAttrOrder() {
        String script = assembler.assemble(List.of(
                attr("delta", "return 1", 2),
                attr("velocity", "return 2", 1)));

        int posVelocity = script.indexOf("out.velocity =");
        int posDelta = script.indexOf("out.delta =");
        assertThat(posVelocity).isGreaterThan(0);
        assertThat(posDelta).isGreaterThan(posVelocity);  // velocity 先于 delta
    }

    @Test
    @DisplayName("求值顺序回填 curData.properties.putAll(out)")
    void populateCurDataForChaining() {
        String script = assembler.assemble(List.of(
                attr("a", "return 1", 1), attr("b", "return 2", 2)));

        // 至少出现一次 putAll 调用(让 b 能引用 a 的结果)
        assertThat(script).contains("curData.properties.putAll(out)");
    }

    @Test
    @DisplayName("缓存命中: 同列表二次调用返回相同实例")
    void cacheHit() {
        List<ComputedAttribute> attrs = List.of(attr("x", "return 1", 1));
        String s1 = assembler.assemble(attrs);
        String s2 = assembler.assemble(attrs);
        assertThat(s2).isSameAs(s1);
    }

    @Test
    @DisplayName("缓存失效: 内容变化重新拼装")
    void cacheInvalidate() {
        String s1 = assembler.assemble(List.of(attr("x", "return 1", 1)));
        String s2 = assembler.assemble(List.of(attr("x", "return 2", 1)));
        assertThat(s2).isNotSameAs(s1).isNotEqualTo(s1);
    }
}
