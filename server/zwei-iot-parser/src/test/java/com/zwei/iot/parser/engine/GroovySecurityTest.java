package com.zwei.iot.parser.engine;

import com.zwei.iot.parser.support.GroovyScriptValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Groovy sandbox security")
class GroovySecurityTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "import java.lang.System\nMap parse(String t, byte[] m) { return [:]; }",
        "import java.lang.Runtime\nMap parse(String t, byte[] m) { return [:]; }",
        "import java.io.File\nMap parse(String t, byte[] m) { return [:]; }",
        "import java.lang.Thread\nMap parse(String t, byte[] m) { return [:]; }",
        "import java.lang.ClassLoader\nMap parse(String t, byte[] m) { return [:]; }",
    })
    @DisplayName("should reject scripts importing dangerous packages")
    void rejectDangerousImports(String script) {
        String error = GroovyScriptValidator.validate(script);
        assertThat(error).isNotNull().contains("脚本编译失败");
    }

    @Test
    @DisplayName("should reject script exceeding 100KB limit")
    void rejectOversizedScript() {
        String hugeScript = "Map parse(String t, byte[] m) { return [:]; }" + " ".repeat(100_001);
        String error = GroovyScriptValidator.validate(hugeScript);
        assertThat(error).contains("过长");
    }

    @Test
    @DisplayName("should reject null script")
    void rejectNullScript() {
        String error = GroovyScriptValidator.validate(null);
        assertThat(error).contains("不能为空");
    }

    @Test
    @DisplayName("should reject empty script")
    void rejectEmptyScript() {
        String error = GroovyScriptValidator.validate("");
        assertThat(error).contains("不能为空");
    }

    @Test
    @DisplayName("should accept valid parse script")
    void acceptValidScript() {
        String valid = "Map<String, Object> parse(String topic, byte[] msg) {\n" +
            "  String s = new String(msg, 'UTF-8')\n" +
            "  return [sensorCode:'1', dataTime:0L, properties:[[identifier:'v',value:1.0,quality:0]]]\n" +
            "}";
        String error = GroovyScriptValidator.validate(valid);
        assertThat(error).isNull();
    }
}
