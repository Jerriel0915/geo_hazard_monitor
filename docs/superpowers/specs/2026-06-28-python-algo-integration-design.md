# Python 算法集成综合告警引擎 — 设计规格

> 日期: 2026-06-28
> 状态: 已确认 (Awaiting Implementation Plan)
> 范围: zwei-iot-alarm + TanAngleWarn 算法仓库

## 1. 背景与目标

### 1.1 现状

知微 (Zwei) 后端已有:
- **算法库管理** (`algo_info` + `algo_version` 表): 支持 zip 上传、版本管理、CRUD
- **Groovy 脚本引擎** (`GroovyScriptExecutor`): 沙箱执行综合告警策略脚本，注入 `cache`/`sensor`/`log` 工具
- **综合告警执行服务** (`ComprehensiveAlarmExecutionService`): 统一三种触发源 (CRON/DataIngest/AlarmTrigger) 的执行链路

切线角算法仓库 (`TanAngleWarn`):
- Flask HTTP 服务 + MCP 服务双入口
- 核心算法在 `src/calc.py`: `calculate_uniform_speed()` 和 `calculate_improved_tangent_angle()`
- 需要历史数据 (7天/90天)，有起算点识别、停算条件等复杂逻辑
- 当前是独立服务，不是可嵌入的库

### 1.2 目标

1. 规范算法工作路径: 上传 zip 后自动解压按版本存入目录
2. 新增 Python 算法执行器工具类，注入到 Groovy 引擎
3. 制定 Python 算法包规范 (入口文件、方法路由、文档声明)
4. 改造切线角算法符合规范，封装两个方法

### 1.3 关键决策 (已与用户确认)

| 决策点 | 选择 |
|-------|------|
| Python 执行方式 | **子进程调用 (ProcessBuilder)** |
| 工作路径结构 | `algo-workspace/{algo_code}/{versionNo}/` |
| `algo_code` 不可变性 | 创建后禁止修改 |
| Python 入口规范 | 固定 `algo_entry.py` + `--method`/`--params` CLI + JSON stdout |
| Groovy 调用 API | `algo.execute(code, version, method, params)` + `algo.executeLatest(code, method, params)` |
| 数据获取模式 | Groovy 侧查好数据传入，Python 为纯计算单元 |
| 算法文档查询 | `algo_entry.py` 实现 `describe()` + REST 端点暴露 |

## 2. 算法工作路径与版本管理

### 2.1 DDL 变更

`algo_version` 表新增字段:

```sql
ALTER TABLE algo_version ADD COLUMN work_path VARCHAR(500) DEFAULT NULL
    COMMENT '解压后的工作目录相对路径 (相对于 RuoYiConfig.profile)';
```

`algo_info` 表: `code` 字段 Service 层约束为不可修改 (update 时忽略 code 字段)。

### 2.2 上传流程改造

`AlgoVersionServiceImpl.upload()` 在现有步骤 4 (落盘) 之后新增解压逻辑:

```
现有流程不变:
  1. 校验文件 (zip, ≤100MB)
  2. 校验算法存在
  3. 校验版本号唯一
  4. 落盘 zip → algo-lib/yyyy/MM/dd/uuid.zip

新增:
  5. 解压 zip → algo-workspace/{algo_code}/{versionNo}/
  6. 校验 algo_entry.py 存在 (不存在则回滚并报错)
  7. work_path = "algo-workspace/{algo_code}/{versionNo}" 写入 algo_version
```

### 2.3 删除流程改造

删除算法版本时:
- 逻辑删除数据库记录 (现有)
- 物理删除工作目录 `algo-workspace/{algo_code}/{versionNo}/` (新增)

删除整个算法时:
- 逻辑删除 algo_info + 所有 algo_version (现有)
- 物理删除 `algo-workspace/{algo_code}/` 整个目录 (新增)

### 2.4 配置项

`AlarmProperties` 新增内部类 `Algo`:

```yaml
iot:
  alarm:
    algo:
      workspace-dir: algo-workspace    # 相对于 RuoYiConfig.profile
      python-cmd: python               # Python 可执行命令
      timeout-seconds: 60              # 子进程超时
```

## 3. Python 算法执行器 (PythonAlgoExecutor)

### 3.1 类定义

**文件**: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/PythonAlgoExecutor.java`

**职责**: 接收算法编码+版本+方法名+参数 → 定位工作目录 → 执行 Python 子进程 → 解析 JSON 结果

```java
@Component
public class PythonAlgoExecutor {

    private final AlarmProperties properties;
    private final AlgoInfoMapper algoInfoMapper;
    private final AlgoVersionMapper algoVersionMapper;
    private final ExecutorService executor;  // 独立线程池 "python-algo"

    /**
     * 精确版本调用
     */
    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params);

    /**
     * 最新启用版本调用
     */
    public AlgoResult executeLatest(String algoCode,
                                     String method, Map<String, Object> params);

    /**
     * 查询算法方法文档
     */
    public AlgoResult describe(String algoCode, String versionNo);
}
```

### 3.2 执行流程

```
execute(code, version, method, params):
  1. 查 algo_info (by code) → 校验存在 + status=1
  2. 查 algo_version (by algoId + versionNo) → 校验存在
  3. 拼接工作目录: {profile}/{workspace-dir}/{code}/{version}/
  4. 校验 algo_entry.py 存在
  5. 序列化 params 为 JSON
  6. 构建命令: {pythonCmd} algo_entry.py --method {method} --params '{json}'
  7. ProcessBuilder.directory(workDir).start()
  8. 读取 stdout → 解析 JSON → AlgoResult
  9. 超时/异常 → AlgoResult.fail(error)
```

### 3.3 AlgoResult 结构

```java
public record AlgoResult(boolean success, Map<String, Object> data, String error) {
    public static AlgoResult ok(Map<String, Object> data) { ... }
    public static AlgoResult fail(String error) { ... }
}
```

### 3.4 安全措施

- 子进程超时: 配置 `iot.alarm.algo.timeout-seconds` (默认 60s)
- 工作目录锁定在 `algo-workspace/` 下，校验路径无 `..` 穿越
- 独立线程池 (`python-algo`, daemon, poolSize=4)，与 Groovy 的 `groovy-eval` 线程池分离
- `params` JSON 序列化时限制大小 (默认 10MB，防止超大参数注入)

## 4. Groovy 引擎集成

### 4.1 新增工具 Bean: ScriptAlgoOps

**文件**: `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/engine/ScriptAlgoOps.java`

```java
@Component
public class ScriptAlgoOps {

    private final PythonAlgoExecutor executor;

    /** Groovy 脚本调用: 精确版本 */
    public AlgoResult execute(String algoCode, String versionNo,
                               String method, Map<String, Object> params) {
        return executor.execute(algoCode, versionNo, method, params);
    }

    /** Groovy 脚本调用: 最新启用版本 */
    public AlgoResult executeLatest(String algoCode,
                                     String method, Map<String, Object> params) {
        return executor.executeLatest(algoCode, method, params);
    }
}
```

### 4.2 ComprehensiveAlarmExecutionService 改造

在 `execute()` 方法的步骤 3 (构建工具) 中新增:

```java
// 现有 (ComprehensiveAlarmExecutionService.java:87-90):
Map<String, Object> tools = new HashMap<>();
tools.put("cache", cacheOps);
tools.put("sensor", scriptSensorQuery);
tools.put("log", scriptLogger);

// 新增:
tools.put("algo", scriptAlgoOps);
```

### 4.3 Groovy 脚本使用示例

```groovy
// 查历史数据
def history = sensor.queryRange(
    hazardPointIds[0], "displacement_x",
    now - 90, now
)

// 调用 Python 算法计算匀速 v
def speedResult = algo.executeLatest("tan-angle-warn", "calc_speed",
    [history_data: history, window_days: 7])

if (!speedResult.success) {
    log.error("算法执行失败: " + speedResult.error)
    return 0
}

// 调用 Python 算法计算切线角
def angleResult = algo.executeLatest("tan-angle-warn", "calc_angle",
    [history_data: history, speed: speedResult.data.speed])

if (angleResult.success) {
    return angleResult.data.level as int  // 返回告警等级
}
return 0
```

## 5. Python 算法包规范

### 5.1 标准包结构

```
algorithm.zip (解压后)
├── algo_entry.py        # 【必须】固定入口文件
├── requirements.txt     # 【可选】pip 依赖声明
└── src/                 # 算法源码 (自由组织)
    ├── __init__.py
    ├── calc.py
    └── ...
```

### 5.2 algo_entry.py 规范

```python
import json
import argparse
import sys

def dispatch(method: str, params: dict) -> dict:
    """方法路由表 — 每个算法包自行维护"""
    routes = {
        # "method_name": target_function,
    }
    if method not in routes:
        return {"success": False, "data": None,
                "error": f"Unknown method: {method}"}
    return routes[method](params)

def describe() -> dict:
    """声明可用方法及参数说明，供前端 API 文档展示"""
    return {
        "methods": {
            # "method_name": {
            #     "summary": "方法说明",
            #     "params": { ... },
            #     "returns": { ... }
            # }
        }
    }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--method", required=False, default="__none__")
    parser.add_argument("--params", required=False, default="{}")
    parser.add_argument("--describe", action="store_true")
    args = parser.parse_args()

    # 文档查询模式
    if args.describe:
        print(json.dumps(describe(), ensure_ascii=False))
        sys.exit(0)

    # 方法调用模式
    params = json.loads(args.params)
    try:
        result = dispatch(args.method, params)
        if "success" not in result:
            result = {"success": True, "data": result, "error": None}
        print(json.dumps(result, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({
            "success": False, "data": None, "error": str(e)
        }, ensure_ascii=False))
        sys.exit(1)
```

### 5.3 调用协议

**Java → Python (方法调用)**:
```bash
python algo_entry.py --method calc_speed --params '{"history_data": [...]}'
```

**Java → Python (文档查询)**:
```bash
python algo_entry.py --describe
```

**Python → Java (返回格式)**:
```json
{"success": true, "data": {...}, "error": null}
```

## 6. 算法 API 文档查询

### 6.1 REST 端点

新增到 `AlgoVersionController`:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/algo-lib/{algoCode}/versions/{versionNo}/describe` | 查询指定版本的方法说明 |
| GET | `/api/v1/algo-lib/{algoCode}/describe-latest` | 查询最新启用版本的方法说明 |

**返回格式**:
```json
{
  "code": 200,
  "data": {
    "methods": {
      "calc_speed": {
        "summary": "计算匀速变形速率 v",
        "params": {
          "history_data": {"type": "array", "required": true, "desc": "历史监测数据"},
          "window_days": {"type": "int", "required": false, "default": 7, "desc": "回溯窗口天数"}
        },
        "returns": {"speed": "float", "start_point": "object"}
      }
    }
  }
}
```

### 6.2 前端集成

综合告警脚本编辑器的 API 文档面板可调用上述端点，展示已注册算法的方法列表和参数说明。

## 7. TanAngleWarn 算法改造

### 7.1 改造目标

将独立 Flask 服务改造为符合上述规范的算法包，同时保留原有 Flask 入口兼容旧部署。

### 7.2 改造内容

1. **新增 `algo_entry.py`** — 标准入口文件，路由到两个方法
2. **提取纯计算方法** — 从 `src/calc.py` 中提取:
   - `calc_speed(params)` — 计算匀速 v (原 `calculate_uniform_speed`)
   - `calc_angle(params)` — 计算切线角 (原 `calculate_improved_tangent_angle`)
3. **移除数据库依赖** — 历史数据改为从 `params` 传入
4. **保留 `src/main.py`** — Flask 服务兼容旧部署
5. **打包结构** — 符合标准 zip 规范

### 7.3 方法入参/出参契约

#### calc_speed (计算匀速 v)

```python
# 入参
{
    "history_data": [
        {"time": "2026-01-01 00:00:00", "value": 1.2},
        {"time": "2026-01-02 00:00:00", "value": 1.3},
        ...
    ],
    "window_days": 7   # 可选，默认 7
}

# 出参
{
    "success": true,
    "data": {
        "speed": 0.05,              # 匀速变形速率 mm/day
        "start_point": {            # 起算点信息
            "time": "2026-01-01",
            "value": 1.2,
            "index": 0
        },
        "r_squared": 0.98,         # 线性拟合 R²
        "data_count": 7             # 参与计算的数据点数
    },
    "error": null
}
```

#### calc_angle (计算切线角)

```python
# 入参
{
    "history_data": [...],          # 同上
    "speed": 0.05,                 # 匀速变形速率 (由 calc_speed 得到)
    "window_days": 90              # 可选，默认 90
}

# 出参
{
    "success": true,
    "data": {
        "angle": 12.5,             # 切线角 (度)
        "level": 3,                # 告警等级建议 (0-4)
        "tangent_speed": 0.06,     # 切线方向速率
        "details": {               # 详细计算过程
            "normal_speed": 0.05,
            "deviation": 0.01,
            "stop_condition": false
        }
    },
    "error": null
}
```

### 7.4 describe() 声明

```python
def describe():
    return {
        "methods": {
            "calc_speed": {
                "summary": "计算匀速变形速率 v",
                "params": {
                    "history_data": {"type": "array", "required": True,
                                     "desc": "历史监测数据 [{time, value}]"},
                    "window_days": {"type": "int", "required": False,
                                   "default": 7, "desc": "回溯窗口天数"}
                },
                "returns": {"speed": "float", "start_point": "object",
                           "r_squared": "float", "data_count": "int"}
            },
            "calc_angle": {
                "summary": "计算切线角",
                "params": {
                    "history_data": {"type": "array", "required": True,
                                    "desc": "历史监测数据 [{time, value}]"},
                    "speed": {"type": "float", "required": True,
                             "desc": "匀速变形速率 (由 calc_speed 得到)"},
                    "window_days": {"type": "int", "required": False,
                                   "default": 90, "desc": "回溯窗口天数"}
                },
                "returns": {"angle": "float", "level": "int",
                           "tangent_speed": "float", "details": "object"}
            }
        }
    }
```

### 7.5 分支策略

新建分支 `feature/algo-standalone` 推送。

## 8. 文件变更清单

### 8.1 后端 Java (zwei-iot-alarm)

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `service/engine/PythonAlgoExecutor.java` | Python 子进程执行器 |
| 新增 | `service/engine/ScriptAlgoOps.java` | Groovy 脚本工具 Bean |
| 新增 | `service/engine/AlgoResult.java` | 算法调用结果 record |
| 修改 | `service/engine/ComprehensiveAlarmExecutionService.java` | tools Map 新增 "algo" |
| 修改 | `config/AlarmProperties.java` | 新增 Algo 内部类配置 |
| 修改 | `algolib/service/impl/AlgoVersionServiceImpl.java` | upload() 新增解压逻辑 |
| 修改 | `algolib/service/impl/AlgoLibraryServiceImpl.java` | update() 禁止修改 code |
| 修改 | `algolib/controller/AlgoVersionController.java` | 新增 describe 端点 |
| 修改 | `algolib/domain/AlgoVersion.java` | 新增 workPath 字段 |
| 修改 | `algolib/mapper/AlgoVersionMapper.java` + XML | 新增 work_path 列映射 |

### 8.2 数据库

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `db/upgrade/v2.11__algo-version-work-path.sql` | algo_version 新增 work_path 列 |

### 8.3 Python (TanAngleWarn 仓库)

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `algo_entry.py` | 标准入口文件 |
| 修改 | `src/calc.py` | 提取纯计算方法 `calc_speed()` / `calc_angle()` |
| 保留 | `src/main.py` | Flask 服务兼容旧部署 |
| 保留 | `src/main_mcp.py` | MCP 服务兼容 |
| 修改 | `requirements.txt` | 确认 numpy/pandas 依赖 |

### 8.4 前端 (web)

| 操作 | 说明 |
|------|------|
| 可选 | 综合告警脚本编辑器集成算法 API 文档查询 |

## 9. 测试策略

### 9.1 后端单元测试

- `PythonAlgoExecutorTest`: mock ProcessBuilder，验证命令拼接、JSON 解析、超时处理、路径校验
- `ScriptAlgoOpsTest`: 验证 Groovy 工具 Bean 正确代理到 executor
- `AlgoVersionServiceImplTest` 扩展: 验证上传后解压 + work_path 写入

### 9.2 Python 算法测试

- `test_algo_entry.py`: 验证 `dispatch()`、`describe()`、命令行入口
- `test_calc.py` 扩展: 验证 `calc_speed()`、`calc_angle()` 的纯计算逻辑

### 9.3 集成验证

- 上传算法 zip → 检查工作目录解压
- Groovy 脚本中调用 `algo.execute()` → 验证子进程调用和结果返回
- 调用 describe 端点 → 验证方法文档返回
