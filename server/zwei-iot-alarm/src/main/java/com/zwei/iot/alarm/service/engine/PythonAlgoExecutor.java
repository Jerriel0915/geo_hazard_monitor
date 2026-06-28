package com.zwei.iot.alarm.service.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.config.RuoYiConfig;
import com.zwei.common.domain.AlgoResult;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class PythonAlgoExecutor {

    private static final Logger log = LoggerFactory.getLogger(PythonAlgoExecutor.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long MAX_PARAMS_SIZE = 10 * 1024 * 1024L;

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

    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params) {
        AlgoInfo algo = algoInfoMapper.selectByCode(algoCode);
        if (algo == null) {
            return AlgoResult.fail("算法不存在: " + algoCode);
        }
        if (algo.getStatus() == null || algo.getStatus() != 1) {
            return AlgoResult.fail("算法已停用: " + algoCode);
        }
        AlgoVersion version = algoVersionMapper.selectByAlgoIdAndVersionNo(algo.getId(), versionNo);
        if (version == null) {
            return AlgoResult.fail("版本不存在: " + algoCode + "/" + versionNo);
        }
        return doExecute(algoCode, version, method, params);
    }

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
        File workDir = resolveWorkDir(algoCode, version.getVersionNo());
        File entryFile = new File(workDir, "algo_entry.py");
        if (!entryFile.exists()) {
            if (version.getWorkPath() != null) {
                workDir = new File(RuoYiConfig.getProfile(), version.getWorkPath());
                entryFile = new File(workDir, "algo_entry.py");
            }
            if (!entryFile.exists()) {
                return AlgoResult.fail("algo_entry.py 不存在，请重新上传算法包");
            }
        }

        String paramsJson;
        try {
            paramsJson = JSON.writeValueAsString(params);
        } catch (Exception e) {
            return AlgoResult.fail("参数序列化失败: " + e.getMessage());
        }
        if (paramsJson.length() > MAX_PARAMS_SIZE) {
            return AlgoResult.fail("参数超过最大限制 10MB");
        }

        final File finalWorkDir = workDir;
        final String finalParamsJson = paramsJson;
        int timeout = properties.getAlgo().getTimeoutSeconds();
        Future<AlgoResult> future = executor.submit(() -> {
            File tempParams = null;
            try {
                // Write params to temp file to avoid Windows command-line space/quoting issues
                tempParams = File.createTempFile("algo_params_", ".json");
                Files.writeString(tempParams.toPath(), finalParamsJson);
                String output = runProcess(finalWorkDir, properties.getAlgo().getPythonCmd(),
                        "algo_entry.py", "--method", method, "--params-file", tempParams.getAbsolutePath());
                Map<String, Object> result = JSON.readValue(output, new TypeReference<>() {});
                boolean success = Boolean.TRUE.equals(result.get("success"));
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                String error = (String) result.get("error");
                return new AlgoResult(success, data, error);
            } catch (Exception e) {
                log.error("Python算法执行异常: {}/{}/{}", algoCode, version.getVersionNo(), method, e);
                return AlgoResult.fail("执行异常: " + e.getMessage());
            } finally {
                if (tempParams != null) {
                    tempParams.delete();
                }
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

    private File resolveWorkDir(String algoCode, String versionNo) {
        if (algoCode.contains("..") || versionNo.contains("..")) {
            throw new IllegalArgumentException("非法路径字符");
        }
        Path basePath = Paths.get(RuoYiConfig.getProfile(), properties.getAlgo().getWorkspaceDir());
        return basePath.resolve(algoCode).resolve(versionNo).toFile();
    }
}
