# Python 算法集成综合告警引擎 — 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 让 Groovy 综合告警脚本能通过 `algo.execute()` 子进程调用 Python 算法包，同时规范算法上传/解压/版本管理工作流。

**架构：** Java `PythonAlgoExecutor` 通过 ProcessBuilder 调用解压后的 Python `algo_entry.py`，以 `--method`/`--params` 命令行参数传 JSON，stdout 收 JSON 结果。`ScriptAlgoOps` 作为第四个工具 Bean 注入 Groovy 引擎。

**技术栈：** Java 17 (record/ProcessBuilder), Python 3.10+ (numpy/scipy), MyBatis, Spring Boot

**规格文档：** `docs/superpowers/specs/2026-06-28-python-algo-integration-design.md`

**涉及仓库：**
- 主仓库 `zwei`: `E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei`
- 算法仓库 `TanAngleWarn`: `E:\work\project\clzy-aic\TanAngleWarn`

---

## 文件结构

### 主仓库 (zwei) — 新增/修改

| 操作 | 文件 | 职责 |
|------|------|------|
| 新增 | `db/upgrade/v2.11__algo-version-work-path.sql` | DDL: algo_version 加 work_path 列 |
| 新增 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlgoResult.java` | 算法调用结果 record |
| 新增 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/PythonAlgoExecutor.java` | Python 子进程执行器 |
| 新增 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptAlgoOps.java` | Groovy 工具 Bean |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/config/AlarmProperties.java` | 新增 Algo 内部类 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoVersion.java` | 新增 workPath 字段 |
| 修改 | `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlgoVersionMapper.xml` | work_path 列映射 + 新查询 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoVersionMapper.java` | 新增方法签名 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoInfoMapper.java` | 新增 selectByCode |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/IAlgoVersionService.java` | 新增方法签名 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImpl.java` | upload 解压 + delete 清理 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImpl.java` | code 不可变 + delete 清理 |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionService.java` | tools 新增 "algo" |
| 修改 | `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoVersionController.java` | 新增 describe 端点 |

### 算法仓库 (TanAngleWarn) — 新增/修改

| 操作 | 文件 | 职责 |
|------|------|------|
| 新增 | `algo_entry.py` | 标准入口文件 |
| 新增 | `tests/test_algo_entry.py` | 入口测试 |
| 修改 | `src/calc.py` | 提取纯计算方法 |

---

## 任务 1：数据库 DDL + Domain + Mapper 变更

**文件：**
- 创建: `db/upgrade/v2.11__algo-version-work-path.sql`
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoVersion.java`
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoVersionMapper.java`
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoInfoMapper.java`
- 修改: `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlgoVersionMapper.xml`

- [ ] **步骤 1：创建 DDL 脚本**

创建 `db/upgrade/v2.11__algo-version-work-path.sql`:

```sql
-- =====================================================================
-- 算法版本工作路径 V20260628
-- algo_version 新增 work_path 列，存储解压后的工作目录相对路径
-- =====================================================================

ALTER TABLE algo_version ADD COLUMN work_path VARCHAR(500) DEFAULT NULL
    COMMENT '解压后的工作目录相对路径 (相对于 RuoYiConfig.profile)';
```

- [ ] **步骤 2：AlgoVersion domain 新增 workPath 字段**

在 `AlgoVersion.java` 的 `private String sha256;` 之后、`private Integer delFlag;` 之前添加:

```java
    /** 解压后的工作目录相对路径 (相对于 RuoYiConfig.profile) */
    private String workPath;
```

- [ ] **步骤 3：AlgoVersionMapper.java 新增方法签名**

在 `softDeleteById(Long id);` 之后添加:

```java
    /**
     * 按算法 ID + 版本号查询未删除版本
     */
    AlgoVersion selectByAlgoIdAndVersionNo(@Param("algoId") Long algoId,
                                            @Param("versionNo") String versionNo);

    /**
     * 按算法 ID 查询最新启用版本（按 create_time DESC 第一条）
     */
    AlgoVersion selectLatestByAlgoId(Long algoId);
```

- [ ] **步骤 4：AlgoInfoMapper.java 新增 selectByCode 方法**

在 `AlgoInfo checkCodeUnique(...)` 之后添加:

```java
    /**
     * 按 code 查询算法（用于 PythonAlgoExecutor 定位算法）
     */
    AlgoInfo selectByCode(@Param("code") String code);
```

- [ ] **步骤 5：AlgoVersionMapper.xml 更新**

在 `<resultMap>` 中 `delFlag` 的 `<result>` 之后添加:

```xml
        <result property="workPath" column="work_path"/>
```

在 `<sql id="columns">` 的 `sha256,` 之后、`del_flag,` 之前添加 `work_path,`。完整修改后的 columns:

```xml
    <sql id="columns">
        id, algo_id, version_no, file_name, original_name, file_size, sha256, work_path,
        del_flag, create_by, create_time, update_by, update_time, remark
    </sql>
```

在 `<insert>` 的列名和 VALUES 中添加 `work_path`:

```xml
    <insert id="insert" parameterType="com.zwei.iot.alarm.algolib.domain.AlgoVersion"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO algo_version (algo_id, version_no, file_name, original_name,
                                  file_size, sha256, work_path, del_flag,
                                  create_by, create_time, update_by, update_time, remark)
        VALUES (#{algoId}, #{versionNo}, #{fileName}, #{originalName},
                #{fileSize}, #{sha256}, #{workPath}, 0,
                #{createBy}, #{createTime}, #{updateBy}, #{updateTime}, #{remark})
    </insert>
```

在 `</mapper>` 之前添加两个新查询:

```xml
    <select id="selectByAlgoIdAndVersionNo" resultMap="AlgoVersionResult">
        SELECT <include refid="columns"/>
        FROM algo_version
        WHERE algo_id = #{algoId}
        AND version_no = #{versionNo}
        AND del_flag = 0
        LIMIT 1
    </select>

    <select id="selectLatestByAlgoId" parameterType="Long" resultMap="AlgoVersionResult">
        SELECT <include refid="columns"/>
        FROM algo_version
        WHERE algo_id = #{algoId} AND del_flag = 0
        ORDER BY create_time DESC
        LIMIT 1
    </select>
```

- [ ] **步骤 6：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 7：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add db/upgrade/v2.11__algo-version-work-path.sql \
  server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/domain/AlgoVersion.java \
  server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoVersionMapper.java \
  server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/mapper/AlgoInfoMapper.java \
  server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlgoVersionMapper.xml
git commit -m "feat(alarm): algo_version 新增 work_path 列 + Mapper 查询方法"
```

---

## 任务 2：AlarmProperties 配置扩展

**文件：**
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/config/AlarmProperties.java`

- [ ] **步骤 1：新增 Algo 内部类**

在 `AlarmProperties.java` 的 `private int groovyTimeoutSeconds = 30;` 之后添加字段和内部类:

```java
    /**
     * Python 算法执行配置
     */
    private Algo algo = new Algo();

    public Algo getAlgo() { return algo; }
    public void setAlgo(Algo algo) { this.algo = algo; }

    /**
     * Python 算法执行器配置
     */
    public static class Algo {
        /** 工作目录 (相对于 RuoYiConfig.profile) */
        private String workspaceDir = "algo-workspace";
        /** Python 可执行命令 */
        private String pythonCmd = "python";
        /** 子进程超时秒数 */
        private int timeoutSeconds = 60;
        /** 线程池大小 */
        private int poolSize = 4;

        public String getWorkspaceDir() { return workspaceDir; }
        public void setWorkspaceDir(String workspaceDir) { this.workspaceDir = workspaceDir; }
        public String getPythonCmd() { return pythonCmd; }
        public void setPythonCmd(String pythonCmd) { this.pythonCmd = pythonCmd; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }
```

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/config/AlarmProperties.java
git commit -m "feat(alarm): AlarmProperties 新增 Algo 内部类配置"
```

---

## 任务 3：AlgoResult 记录类

**文件：**
- 创建: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlgoResult.java`

- [ ] **步骤 1：创建 AlgoResult**

```java
package com.zwei.iot.alarm.service.engine;

import java.util.Map;

/**
 * Python 算法调用结果。
 *
 * @param success 执行是否成功
 * @param data    返回数据 (成功时)
 * @param error   错误信息 (失败时)
 * @author zwei
 */
public record AlgoResult(boolean success, Map<String, Object> data, String error) {

    public static AlgoResult ok(Map<String, Object> data) {
        return new AlgoResult(true, data, null);
    }

    public static AlgoResult fail(String error) {
        return new AlgoResult(false, null, error);
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/AlgoResult.java
git commit -m "feat(alarm): 新增 AlgoResult 记录类"
```

---

## 任务 4：PythonAlgoExecutor

**文件：**
- 创建: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/PythonAlgoExecutor.java`

- [ ] **步骤 1：创建 PythonAlgoExecutor**

```java
package com.zwei.iot.alarm.service.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.config.RuoYiConfig;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import com.zwei.iot.alarm.config.AlarmProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Python 算法子进程执行器。
 * <p>
 * 通过 ProcessBuilder 调用解压后的 algo_entry.py，
 * 以 --method/--params 命令行参数传递 JSON，stdout 收 JSON 结果。
 *
 * @author zwei
 */
@Component
public class PythonAlgoExecutor {

    private static final Logger log = LoggerFactory.getLogger(PythonAlgoExecutor.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long MAX_PARAMS_SIZE = 10 * 1024 * 1024L; // 10MB

    private final AlarmProperties properties;
    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;
    private final ExecutorService executor;

    public PythonAlgoExecutor(AlarmProperties properties,
                               AlgoInfoMapper algoInfoMapper,
                               AlgoVersionMapper algoVersionMapper) {
        this.properties = properties;
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
        int poolSize = properties.getAlgo().getPoolSize();
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "python-algo");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    /**
     * 精确版本调用
     */
    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params) {
        // 1. 校验算法存在 + 启用
        AlgoInfo algo = algoInfoMapper.selectByCode(algoCode);
        if (algo == null) {
            return AlgoResult.fail("算法不存在: " + algoCode);
        }
        if (algo.getStatus() == null || algo.getStatus() != 1) {
            return AlgoResult.fail("算法已停用: " + algoCode);
        }

        // 2. 查版本
        AlgoVersion version = algoVersionMapper.selectByAlgoIdAndVersionNo(algo.getId(), versionNo);
        if (version == null) {
            return AlgoResult.fail("版本不存在: " + algoCode + "/" + versionNo);
        }

        return doExecute(algoCode, version, method, params);
    }

    /**
     * 最新启用版本调用
     */
    public AlgoResult executeLatest(String algoCode,
                                     String method, Map<String, Object> params) {
        AlgoInfo algo = algoInfoMapper.selectByCode(algoCode);
        if (algo == null) {
            return AlgoResult.fail("算法不存在: " + algoCode);
        }
        if (algo.getStatus() == null || algo.getStatus() != 1) {
            return AlgoResult.fail("算法已停用: " + algoCode);
        }

        AlgoVersion version = algoVersionMapper.selectLatestByAlgoId(algo.getId());
        if (version == null) {
            return AlgoResult.fail("算法无可用版本: " + algoCode);
        }

        return doExecute(algoCode, version, method, params);
    }

    /**
     * 查询算法方法文档
     */
    public AlgoResult describe(String algoCode, String versionNo) {
        AlgoInfo algo = algoInfoMapper.selectByCode(algoCode);
        if (algo == null) {
            return AlgoResult.fail("算法不存在: " + algoCode);
        }
        AlgoVersion version = algoVersionMapper.selectByAlgoIdAndVersionNo(algo.getId(), versionNo);
        if (version == null) {
            return AlgoResult.fail("版本不存在: " + algoCode + "/" + versionNo);
        }

        File workDir = resolveWorkDir(algoCode, versionNo);
        File entryFile = new File(workDir, "algo_entry.py");
        if (!entryFile.exists()) {
            return AlgoResult.fail("algo_entry.py 不存在于工作目录");
        }

        try {
            String output = runProcess(workDir, properties.getAlgo().getPythonCmd(),
                    "algo_entry.py", "--describe");
            Map<String, Object> data = JSON.readValue(output, new TypeReference<>() {});
            return AlgoResult.ok(data);
        } catch (Exception e) {
            log.error("算法文档查询失败: {}/{}", algoCode, versionNo, e);
            return AlgoResult.fail("文档查询失败: " + e.getMessage());
        }
    }

    private AlgoResult doExecute(String algoCode, AlgoVersion version,
                                  String method, Map<String, Object> params) {
        // 3. 定位工作目录
        File workDir = resolveWorkDir(algoCode, version.getVersionNo());
        File entryFile = new File(workDir, "algo_entry.py");
        if (!entryFile.exists()) {
            // 尝试从 work_path 字段回退
            if (version.getWorkPath() != null) {
                workDir = new File(RuoYiConfig.getProfile(), version.getWorkPath());
                entryFile = new File(workDir, "algo_entry.py");
            }
            if (!entryFile.exists()) {
                return AlgoResult.fail("algo_entry.py 不存在，请重新上传算法包");
            }
        }

        // 4. 序列化参数
        String paramsJson;
        try {
            paramsJson = JSON.writeValueAsString(params);
        } catch (Exception e) {
            return AlgoResult.fail("参数序列化失败: " + e.getMessage());
        }
        if (paramsJson.length() > MAX_PARAMS_SIZE) {
            return AlgoResult.fail("参数超过最大限制 10MB");
        }

        // 5. 子进程执行 (有线程池超时控制)
        int timeout = properties.getAlgo().getTimeoutSeconds();
        Future<AlgoResult> future = executor.submit(() -> {
            try {
                String output = runProcess(workDir, properties.getAlgo().getPythonCmd(),
                        "algo_entry.py", "--method", method, "--params", paramsJson);
                Map<String, Object> result = JSON.readValue(output, new TypeReference<>() {});
                boolean success = Boolean.TRUE.equals(result.get("success"));
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                String error = (String) result.get("error");
                return new AlgoResult(success, data, error);
            } catch (Exception e) {
                log.error("Python算法执行异常: {}/{}/{}", algoCode, version.getVersionNo(), method, e);
                return AlgoResult.fail("执行异常: " + e.getMessage());
            }
        });

        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Python算法执行超时({}s): {}/{}/{}", timeout, algoCode, version.getVersionNo(), method);
            return AlgoResult.fail("执行超时 (" + timeout + "s)");
        } catch (Exception e) {
            return AlgoResult.fail("执行中断: " + e.getMessage());
        }
    }

    /**
     * 运行子进程并返回 stdout
     */
    private String runProcess(File workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line);
            }
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderr.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0 && stdout.isEmpty()) {
            throw new RuntimeException("Python进程退出码 " + exitCode + ": " + stderr);
        }
        return stdout.toString();
    }

    /**
     * 解析工作目录，校验路径穿越
     */
    private File resolveWorkDir(String algoCode, String versionNo) {
        // 校验路径穿越
        if (algoCode.contains("..") || versionNo.contains("..")) {
            throw new IllegalArgumentException("非法路径字符");
        }
        Path basePath = Paths.get(RuoYiConfig.getProfile(), properties.getAlgo().getWorkspaceDir());
        return basePath.resolve(algoCode).resolve(versionNo).toFile();
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/PythonAlgoExecutor.java
git commit -m "feat(alarm): 新增 PythonAlgoExecutor 子进程执行器"
```

---

## 任务 5：ScriptAlgoOps 工具 Bean

**文件：**
- 创建: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptAlgoOps.java`

- [ ] **步骤 1：创建 ScriptAlgoOps**

```java
package com.zwei.iot.alarm.service.engine;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Groovy 脚本可调用的算法操作工具 Bean。
 * <p>
 * 注入到 GroovyScriptExecutor 的 tools 中，脚本通过 {@code algo.execute(...)} 调用。
 *
 * @author zwei
 */
@Component
public class ScriptAlgoOps {

    private final PythonAlgoExecutor executor;

    public ScriptAlgoOps(PythonAlgoExecutor executor) {
        this.executor = executor;
    }

    /**
     * 精确版本调用
     *
     * @param algoCode  算法编码
     * @param versionNo 版本号
     * @param method    方法名
     * @param params    参数 Map
     * @return 算法结果
     */
    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params) {
        return executor.execute(algoCode, versionNo, method, params);
    }

    /**
     * 最新启用版本调用
     */
    public AlgoResult executeLatest(String algoCode,
                                     String method, Map<String, Object> params) {
        return executor.executeLatest(algoCode, method, params);
    }
}
```

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptAlgoOps.java
git commit -m "feat(alarm): 新增 ScriptAlgoOps Groovy 工具 Bean"
```

---

## 任务 6：ComprehensiveAlarmExecutionService 注入 algo 工具

**文件：**
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionService.java`

- [ ] **步骤 1：添加 ScriptAlgoOps 依赖注入**

在类的字段区域（`private final StrategyScopeResolver scopeResolver;` 之后）添加:

```java
    private final ScriptAlgoOps scriptAlgoOps;
```

由于类使用 `@RequiredArgsConstructor`，Lombok 会自动加入构造参数。

- [ ] **步骤 2：在 tools Map 中注入 algo**

在 `execute()` 方法中（约第 87-90 行），现有代码:

```java
        Map<String, Object> tools = new HashMap<>();
        tools.put("cache", cacheOps);
        tools.put("sensor", scriptSensorQuery);
        tools.put("log", scriptLogger);
```

在 `tools.put("log", scriptLogger);` 之后添加:

```java
        tools.put("algo", scriptAlgoOps);
```

- [ ] **步骤 3：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 4：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ComprehensiveAlarmExecutionService.java
git commit -m "feat(alarm): ComprehensiveAlarmExecutionService 注入 algo 工具 Bean"
```

---

## 任务 7：AlgoVersionServiceImpl — 上传解压 + 删除清理

**文件：**
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/IAlgoVersionService.java`
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImpl.java`

- [ ] **步骤 1：IAlgoVersionService 新增 describe 方法签名**

在 `boolean checkVersionUnique(Long algoId, String versionNo);` 之后添加:

```java
    /**
     * 查询算法方法文档 (调用 Python --describe)
     */
    com.zwei.iot.alarm.service.engine.AlgoResult describe(String algoCode, String versionNo);

    /**
     * 查询最新启用版本的方法文档
     */
    com.zwei.iot.alarm.service.engine.AlgoResult describeLatest(String algoCode);
```

- [ ] **步骤 2：AlgoVersionServiceImpl — 新增依赖和解压逻辑**

在 `AlgoVersionServiceImpl` 的构造函数中增加 `PythonAlgoExecutor` 依赖:

```java
    private final com.zwei.iot.alarm.service.engine.PythonAlgoExecutor pythonAlgoExecutor;

    public AlgoVersionServiceImpl(AlgoInfoMapper algoInfoMapper,
                                  AlgoVersionMapper algoVersionMapper,
                                  com.zwei.iot.alarm.service.engine.PythonAlgoExecutor pythonAlgoExecutor) {
        this.algoInfoMapper = algoInfoMapper;
        this.algoVersionMapper = algoVersionMapper;
        this.pythonAlgoExecutor = pythonAlgoExecutor;
    }
```

在 `upload()` 方法中，在步骤 4 (落盘) 之后、步骤 5 (SHA256) 之前，新增解压逻辑。即在这段代码之后:

```java
        file.transferTo(dest);
```

添加解压代码:

```java
        // 4.1 解压到工作目录
        String workspaceDir = "algo-workspace";
        String workPathRelative = workspaceDir + "/" + algo.getCode() + "/" + versionNo;
        File workDir = new File(getProfilePath() + File.separator + workPathRelative);
        if (workDir.exists()) {
            deleteDirectory(workDir);
        }
        if (!workDir.mkdirs()) {
            throw new ServiceException("创建工作目录失败: " + workDir.getAbsolutePath());
        }
        try {
            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                    new java.io.FileInputStream(dest));
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(workDir, entry.getName());
                // 校验路径穿越
                if (!entryFile.getCanonicalPath().startsWith(workDir.getCanonicalPath())) {
                    zis.close();
                    throw new ServiceException("zip 包含非法路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    entryFile.getParentFile().mkdirs();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(entryFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            zis.close();
        } catch (java.io.IOException e) {
            throw new ServiceException("解压失败: " + e.getMessage());
        }

        // 4.2 校验 algo_entry.py 存在
        if (!new File(workDir, "algo_entry.py").exists()) {
            deleteDirectory(workDir);
            throw new ServiceException("算法包缺少 algo_entry.py 入口文件");
        }
```

在步骤 6 (入库) 的 `AlgoVersion.builder()` 链中添加 `.workPath(workPathRelative)`:

```java
        AlgoVersion version = AlgoVersion.builder()
                .algoId(algoId)
                .versionNo(versionNo)
                .fileName(relativePath)
                .originalName(original)
                .fileSize(file.getSize())
                .sha256(sha256)
                .workPath(workPathRelative)
                .remark(remark)
                .createBy(createBy)
                .createTime(new Date())
                .build();
```

- [ ] **步骤 3：AlgoVersionServiceImpl — delete 方法增加工作目录清理**

将现有 `delete` 方法替换为:

```java
    @Override
    public int delete(Long id) {
        AlgoVersion version = algoVersionMapper.selectById(id);
        if (version == null) {
            return 0;
        }
        // 物理删除工作目录
        if (version.getWorkPath() != null) {
            File workDir = new File(getProfilePath() + File.separator + version.getWorkPath());
            if (workDir.exists()) {
                deleteDirectory(workDir);
            }
        }
        return algoVersionMapper.softDeleteById(id);
    }
```

- [ ] **步骤 4：AlgoVersionServiceImpl — 新增 describe 方法实现和辅助方法**

在文件末尾（`sha256Hex` 方法之后）添加:

```java
    @Override
    public com.zwei.iot.alarm.service.engine.AlgoResult describe(String algoCode, String versionNo) {
        return pythonAlgoExecutor.describe(algoCode, versionNo);
    }

    @Override
    public com.zwei.iot.alarm.service.engine.AlgoResult describeLatest(String algoCode) {
        // 查最新版本
        com.zwei.iot.alarm.algolib.domain.AlgoInfo algo = algoInfoMapper.selectByCode(algoCode);
        if (algo == null) {
            return com.zwei.iot.alarm.service.engine.AlgoResult.fail("算法不存在: " + algoCode);
        }
        com.zwei.iot.alarm.algolib.domain.AlgoVersion version = algoVersionMapper.selectLatestByAlgoId(algo.getId());
        if (version == null) {
            return com.zwei.iot.alarm.service.engine.AlgoResult.fail("算法无可用版本");
        }
        return pythonAlgoExecutor.describe(algoCode, version.getVersionNo());
    }

    /**
     * 递归删除目录
     */
    private static void deleteDirectory(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectory(child);
                } else {
                    child.delete();
                }
            }
        }
        dir.delete();
    }
```

- [ ] **步骤 5：AlgoLibraryServiceImpl — code 不可变 + delete 清理**

在 `AlgoLibraryServiceImpl.update()` 方法中，在方法开头添加 code 保护:

```java
    @Override
    public int update(AlgoInfo algoInfo) {
        // code 字段不可修改
        algoInfo.setCode(null);
        if (algoInfo.getCode() != null && !checkCodeUnique(algoInfo.getCode(), algoInfo.getId())) {
            throw new ServiceException("修改失败，算法编码已存在: " + algoInfo.getCode());
        }
        algoInfo.setUpdateTime(new Date());
        return algoInfoMapper.update(algoInfo);
    }
```

在 `deleteWithVersions()` 方法中添加工作目录清理:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWithVersions(Long id) {
        AlgoInfo info = algoInfoMapper.selectById(id);
        if (info == null) return 0;

        int rows = algoInfoMapper.softDelete(id);
        if (rows > 0) {
            algoVersionMapper.softDeleteByAlgoId(id);
            // 物理删除整个算法的工作目录
            if (info.getCode() != null) {
                try {
                    String workDirPath = com.zwei.common.config.RuoYiConfig.getProfile()
                            + File.separator + "algo-workspace" + File.separator + info.getCode();
                    File workDir = new File(workDirPath);
                    if (workDir.exists()) {
                        deleteDirectoryRecursive(workDir);
                    }
                } catch (Exception e) {
                    // 工作目录删除失败不影响逻辑删除
                }
            }
        }
        return rows;
    }

    private static void deleteDirectoryRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirectoryRecursive(child);
                } else {
                    child.delete();
                }
            }
        }
        dir.delete();
    }
```

需要在 `AlgoLibraryServiceImpl` 中添加 `import java.io.File;`。

- [ ] **步骤 6：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 7：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/IAlgoVersionService.java \
  server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoVersionServiceImpl.java \
  server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/service/impl/AlgoLibraryServiceImpl.java
git commit -m "feat(alarm): 算法上传自动解压 + 删除清理 + code 不可变"
```

---

## 任务 8：AlgoVersionController — describe REST 端点

**文件：**
- 修改: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoVersionController.java`

- [ ] **步骤 1：新增 describe 端点**

在 `AlgoVersionController` 的 `download` 方法之后添加:

```java
    @GetMapping("/{algoCode}/versions/{versionNo}/describe")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult describe(@PathVariable String algoCode,
                               @PathVariable String versionNo) {
        com.zwei.iot.alarm.service.engine.AlgoResult result = versionService.describe(algoCode, versionNo);
        if (result.success()) {
            return success(result.data());
        }
        return error(result.error());
    }

    @GetMapping("/{algoCode}/describe-latest")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult describeLatest(@PathVariable String algoCode) {
        com.zwei.iot.alarm.service.engine.AlgoResult result = versionService.describeLatest(algoCode);
        if (result.success()) {
            return success(result.data());
        }
        return error(result.error());
    }
```

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/controller/AlgoVersionController.java
git commit -m "feat(alarm): 新增算法 describe REST 端点"
```

---

## 任务 9：AlgoInfoMapper.xml 新增 selectByCode

**文件：**
- 修改: `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlgoInfoMapper.xml`

- [ ] **步骤 1：添加 selectByCode SQL**

找到 `AlgoInfoMapper.xml` 文件（路径可能为 `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlgoInfoMapper.xml`）。在 `checkCodeUnique` 查询之后添加:

```xml
    <select id="selectByCode" resultMap="AlgoInfoResult">
        SELECT id, code, name, description, status, del_flag,
               create_by, create_time, update_by, update_time, remark
        FROM algo_info
        WHERE code = #{code} AND del_flag = 0
        LIMIT 1
    </select>
```

注意：确认 resultMap 名称为 `AlgoInfoResult`，如不同则按实际修改。

- [ ] **步骤 2：编译验证**

运行：`cd server && mvn compile -pl zwei-iot-alarm -am -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlgoInfoMapper.xml
git commit -m "feat(alarm): AlgoInfoMapper.xml 新增 selectByCode 查询"
```

---

## 任务 10：TanAngleWarn — algo_entry.py + calc.py 改造

**仓库：** `E:\work\project\clzy-aic\TanAngleWarn`

- [ ] **步骤 1：创建新分支**

```bash
cd "E:\work\project\clzy-aic\TanAngleWarn"
git checkout -b feature/algo-standalone
```

- [ ] **步骤 2：创建 algo_entry.py**

在仓库根目录创建 `algo_entry.py`:

```python
"""标准算法入口文件 — 由知微综合告警引擎通过子进程调用。

调用协议:
  python algo_entry.py --method <method> --params '<json>'
  python algo_entry.py --describe
"""
import json
import argparse
import sys
import os

# 将 src 目录加入 Python 路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from src.calc_standalone import calc_speed, calc_angle


def dispatch(method: str, params: dict) -> dict:
    """方法路由表"""
    routes = {
        "calc_speed": calc_speed,
        "calc_angle": calc_angle,
    }
    if method not in routes:
        return {"success": False, "data": None,
                "error": f"Unknown method: {method}"}
    return routes[method](params)


def describe() -> dict:
    """声明可用方法及参数说明"""
    return {
        "methods": {
            "calc_speed": {
                "summary": "计算匀速变形速率 v",
                "params": {
                    "history_data": {
                        "type": "array", "required": True,
                        "desc": "历史监测数据 [{time, value}]"
                    },
                    "window_days": {
                        "type": "int", "required": False,
                        "default": 7, "desc": "回溯窗口天数"
                    }
                },
                "returns": {
                    "speed": "float", "start_point": "object",
                    "r_squared": "float", "data_count": "int"
                }
            },
            "calc_angle": {
                "summary": "计算切线角",
                "params": {
                    "history_data": {
                        "type": "array", "required": True,
                        "desc": "历史监测数据 [{time, value}]"
                    },
                    "speed": {
                        "type": "float", "required": True,
                        "desc": "匀速变形速率 (由 calc_speed 得到)"
                    },
                    "current_displacement": {
                        "type": "float", "required": True,
                        "desc": "当前位移量"
                    },
                    "prev_displacement": {
                        "type": "float", "required": True,
                        "desc": "24小时前位移量"
                    },
                    "prev_time": {
                        "type": "string", "required": True,
                        "desc": "24小时前数据时间 (yyyy-MM-dd HH:mm:ss)"
                    },
                    "current_time": {
                        "type": "string", "required": True,
                        "desc": "当前数据时间 (yyyy-MM-dd HH:mm:ss)"
                    }
                },
                "returns": {
                    "angle": "float", "level": "int",
                    "tangent_speed": "float"
                }
            }
        }
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--method", required=False, default="__none__")
    parser.add_argument("--params", required=False, default="{}")
    parser.add_argument("--describe", action="store_true")
    args = parser.parse_args()

    if args.describe:
        print(json.dumps(describe(), ensure_ascii=False))
        sys.exit(0)

    params = json.loads(args.params)
    try:
        result = dispatch(args.method, params)
        if "success" not in result:
            result = {"success": True, "data": result, "error": None}
        print(json.dumps(result, ensure_ascii=False, default=str))
    except Exception as e:
        print(json.dumps({
            "success": False, "data": None, "error": str(e)
        }, ensure_ascii=False))
        sys.exit(1)
```

- [ ] **步骤 3：创建 src/calc_standalone.py (纯计算模块)**

在 `src/` 目录创建 `calc_standalone.py`:

```python
"""纯计算模块 — 不依赖数据源，历史数据从 params 传入。

由 algo_entry.py 的 dispatch 路由调用。
"""
from datetime import datetime, timedelta
from typing import List, Optional

import numpy as np
from scipy.stats import linregress


def calc_speed(params: dict) -> dict:
    """计算匀速变形速率 v (mm/day)。

    从 history_data 中取最近 window_days 天的数据，
    用首末两点计算匀速速度。

    Args:
        params: {
            "history_data": [{"time": "2026-01-01 00:00:00", "value": 1.2}, ...],
            "window_days": 7  # 可选，默认 7
        }

    Returns:
        {"success": True, "data": {"speed": 0.05, ...}, "error": None}
    """
    history_data = params.get("history_data", [])
    window_days = params.get("window_days", 7)

    if not history_data:
        return {"success": False, "data": None, "error": "history_data 为空"}

    # 解析时间序列
    points = _parse_history(history_data)
    if len(points) < 2:
        return {"success": False, "data": None, "error": "数据点不足 (< 2)"}

    # 取最近 window_days 天的数据
    latest_time = points[-1][0]
    cutoff = latest_time - timedelta(days=window_days)
    window_points = [(t, v) for t, v in points if t >= cutoff]

    if len(window_points) < 2:
        window_points = points  # 数据不足时用全部

    # 计算匀速速度: 位移差 / 时间差
    start_time, start_value = window_points[0]
    end_time, end_value = window_points[-1]
    time_diff_days = (end_time - start_time).total_seconds() / (60 * 60 * 24)

    if time_diff_days == 0:
        return {"success": False, "data": None, "error": "时间差为 0"}

    speed = (end_value - start_value) / time_diff_days

    # 线性拟合 R²
    x = np.arange(len(window_points))
    y = np.array([v for _, v in window_points])
    if len(window_points) >= 2:
        slope, intercept, r_value, p_value, std_err = linregress(x, y)
        r_squared = r_value ** 2
    else:
        r_squared = 0.0

    return {
        "success": True,
        "data": {
            "speed": float(speed),
            "start_point": {
                "time": start_time.strftime("%Y-%m-%d %H:%M:%S"),
                "value": float(start_value),
                "index": 0
            },
            "r_squared": float(r_squared),
            "data_count": len(window_points)
        },
        "error": None
    }


def calc_angle(params: dict) -> dict:
    """计算改进的切线角 (度)。

    基于匀速变形速率 v，计算当前时刻和前一时刻的切线角。

    改进切线角公式:
        T(i) = delta_S(i) / v
        angle = arctan((T(i) - T(i-1)) / delta_time_days)

    Args:
        params: {
            "current_displacement": float,
            "prev_displacement": float,
            "prev_time": "2026-01-01 00:00:00",
            "current_time": "2026-01-02 00:00:00",
            "speed": float  # 由 calc_speed 得到的匀速速率
        }

    Returns:
        {"success": True, "data": {"angle": 12.5, "level": 3, ...}, "error": None}
    """
    current_displacement = params.get("current_displacement")
    prev_displacement = params.get("prev_displacement")
    prev_time_str = params.get("prev_time")
    current_time_str = params.get("current_time")
    speed = params.get("speed")

    if speed is None or speed == 0:
        return {"success": False, "data": None, "error": "匀速速度 v 为空或零"}

    if any(v is None for v in [current_displacement, prev_displacement, prev_time_str, current_time_str]):
        return {"success": False, "data": None, "error": "缺少必要参数"}

    # 解析时间
    prev_time = _parse_time(prev_time_str)
    current_time = _parse_time(current_time_str)
    time_diff_days = (current_time - prev_time).total_seconds() / (60 * 60 * 24)

    if time_diff_days == 0:
        return {"success": False, "data": None, "error": "时间差为 0"}

    # 计算 T(i) 和 T(i-1)
    T_i = current_displacement / speed
    T_i_minus_1 = prev_displacement / speed

    # 切线角
    delta_T = T_i - T_i_minus_1
    angle_rad = np.arctan(delta_T / time_diff_days)
    angle_deg = float(np.degrees(angle_rad))

    # 根据切线角确定告警等级
    level = _angle_to_level(angle_deg)

    # 切线方向速率
    tangent_speed = float(delta_T / time_diff_days)

    return {
        "success": True,
        "data": {
            "angle": angle_deg,
            "level": level,
            "tangent_speed": tangent_speed,
            "details": {
                "normal_speed": float(speed),
                "T_i": float(T_i),
                "T_i_minus_1": float(T_i_minus_1),
                "time_diff_days": float(time_diff_days)
            }
        },
        "error": None
    }


def _parse_history(history_data: list) -> List[tuple]:
    """将历史数据 [{time, value}] 解析为 [(datetime, float), ...] 升序列表"""
    points = []
    for item in history_data:
        t = _parse_time(item["time"])
        v = float(item["value"])
        points.append((t, v))
    points.sort(key=lambda x: x[0])
    return points


def _parse_time(time_str: str) -> datetime:
    """解析时间字符串，支持多种格式"""
    for fmt in ["%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M:%S.%f",
                "%Y-%m-%dT%H:%M:%S.%f", "%Y-%m-%d"]:
        try:
            return datetime.strptime(time_str[:len(fmt) + 5 if '%' not in fmt else len(time_str)], fmt)
        except ValueError:
            continue
    # 尝试 ISO 格式
    try:
        return datetime.fromisoformat(time_str)
    except Exception:
        raise ValueError(f"无法解析时间: {time_str}")


def _angle_to_level(angle: float) -> int:
    """根据切线角映射告警等级

    切线角 (度) → 等级:
        < 45°  → 0 (无告警)
        45-60° → 1 (蓝色预警)
        60-70° → 2 (黄色预警)
        70-80° → 3 (橙色预警)
        >= 80° → 4 (红色预警)
    """
    if angle < 45:
        return 0
    elif angle < 60:
        return 1
    elif angle < 70:
        return 2
    elif angle < 80:
        return 3
    else:
        return 4
```

- [ ] **步骤 4：创建 src/__init__.py (如果不存在)**

```bash
cd "E:\work\project\clzy-aic\TanAngleWarn"
```

检查并创建: 如果 `src/__init__.py` 不存在则创建空文件。

- [ ] **步骤 5：验证 algo_entry.py 可执行**

```bash
cd "E:\work\project\clzy-aic\TanAngleWarn"
python algo_entry.py --describe
```

预期输出: 包含 `calc_speed` 和 `calc_angle` 方法定义的 JSON。

测试 calc_speed:

```bash
python algo_entry.py --method calc_speed --params "{\"history_data\": [{\"time\": \"2026-01-01 00:00:00\", \"value\": 1.0}, {\"time\": \"2026-01-02 00:00:00\", \"value\": 1.5}, {\"time\": \"2026-01-03 00:00:00\", \"value\": 2.0}], \"window_days\": 7}"
```

预期: `{"success": true, "data": {"speed": 0.5, ...}, "error": null}`

测试 calc_angle:

```bash
python algo_entry.py --method calc_angle --params "{\"current_displacement\": 2.0, \"prev_displacement\": 1.5, \"prev_time\": \"2026-01-02 00:00:00\", \"current_time\": \"2026-01-03 00:00:00\", \"speed\": 0.5}"
```

预期: `{"success": true, "data": {"angle": ..., "level": ...}, "error": null}`

- [ ] **步骤 6：Commit + 推送**

```bash
cd "E:\work\project\clzy-aic\TanAngleWarn"
git add algo_entry.py src/calc_standalone.py src/__init__.py
git commit -m "feat: 新增标准算法入口 algo_entry.py + 纯计算模块 calc_standalone.py"
git push origin feature/algo-standalone
```

---

## 任务 11：后端整体编译 + 测试验证

- [ ] **步骤 1：全量编译**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei\server"
mvn clean compile -pl zwei-iot-alarm -am -q
```

预期: BUILD SUCCESS

- [ ] **步骤 2：运行已有测试确保无回归**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei\server"
mvn test -pl zwei-iot-alarm -q
```

预期: 已有的 60 case 全部 PASS

- [ ] **步骤 3：执行 DDL**

在本地 MySQL 执行:

```sql
ALTER TABLE algo_version ADD COLUMN work_path VARCHAR(500) DEFAULT NULL
    COMMENT '解压后的工作目录相对路径 (相对于 RuoYiConfig.profile)';
```

- [ ] **步骤 4：最终 Commit (如有遗漏的变更)**

```bash
git add -A
git status  # 检查是否有未提交的文件
git commit -m "feat(alarm): Python 算法集成完整实现" || echo "nothing to commit"
```

---

## 自检

**1. 规格覆盖度:**
- ✅ 规格 §2 (工作路径) → 任务 1 (DDL + Domain) + 任务 7 (上传解压/删除清理)
- ✅ 规格 §3 (PythonAlgoExecutor) → 任务 4
- ✅ 规格 §4 (Groovy 集成) → 任务 5 (ScriptAlgoOps) + 任务 6 (注入)
- ✅ 规格 §5 (Python 规范) → 任务 10 (algo_entry.py)
- ✅ 规格 §6 (文档查询) → 任务 8 (Controller) + 任务 10 (describe 函数)
- ✅ 规格 §7 (TanAngleWarn) → 任务 10
- ✅ 规格 §2.4 (配置) → 任务 2 (AlarmProperties)

**2. 占位符扫描:** 无 TODO/待定。所有代码块完整。

**3. 类型一致性:**
- `AlgoResult` 在任务 3 定义，在任务 4/5/7/8 引用，签名一致
- `PythonAlgoExecutor.execute/describe` 在任务 4 定义，在任务 5 (ScriptAlgoOps) 和任务 7 (ServiceImpl) 引用，签名一致
- `AlgoVersion.workPath` 在任务 1 定义，在任务 4 (回退查找) 和任务 7 (入库/删除) 引用，名称一致
- `AlgoInfoMapper.selectByCode` 在任务 1 定义，在任务 4 (PythonAlgoExecutor) 引用
- `AlgoVersionMapper.selectByAlgoIdAndVersionNo/selectLatestByAlgoId` 在任务 1 定义，在任务 4 引用
- Python 侧 `calc_speed`/`calc_angle` 在 `algo_entry.py` (任务 10) 与 `calc_standalone.py` (任务 10) 名称一致
