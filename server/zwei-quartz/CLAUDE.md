[根目录](../../CLAUDE.md) > [server](../) > **zwei-quartz**

# zwei-quartz — 定时任务模块

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-quartz**

## 模块职责

封装 Quartz 定时任务框架, 提供:

- 定时任务 CRUD (`SysJob`)
- 任务执行历史 (`SysJobLog`)
- 任务并发控制 / 立即执行 / 暂停 / 恢复
- 任务调度日志

## 关键依赖

- `zwei-common` (基础)
- Quartz (核心, 由 `zwei-admin` 引入 starter 或本模块按需)

## 主要子包

| 子包             | 职责                                                          |
|----------------|-------------------------------------------------------------|
| `controller`   | `SysJobController` (任务 CRUD) / `SysJobLogController` (执行历史) |
| `service`      | `ISysJobService` / `ISysJobLogService`                      |
| `service.impl` | 业务实现, 集成 Quartz `Scheduler`                                 |
| `mapper`       | `SysJobMapper` / `SysJobLogMapper`                          |
| `domain`       | `SysJob` / `SysJobLog`                                      |
| `util`         | Quartz 工具类 (`JobUtils`)                                     |
| `task`         | 业务侧自定义 Job (按需扩展)                                           |

## 对外接口 (Controller)

| 路径                                 | 方法     | 职责     |
|------------------------------------|--------|--------|
| `/api/v1/monitor/job/list`         | GET    | 分页查询任务 |
| `/api/v1/monitor/job/{jobId}`      | GET    | 详情     |
| `/api/v1/monitor/job`              | POST   | 新增     |
| `/api/v1/monitor/job`              | PUT    | 修改     |
| `/api/v1/monitor/job/{ids}`        | DELETE | 删除     |
| `/api/v1/monitor/job/changeStatus` | PUT    | 启停     |
| `/api/v1/monitor/job/run`          | PUT    | 立即执行   |
| `/api/v1/monitor/jobLog/list`      | GET    | 执行历史分页 |

## 数据模型

- `sys_job` — 任务 (jobName / jobGroup / invokeTarget / cronExpression / status / concurrent / misfirePolicy)
- `sys_job_log` — 执行历史 (jobName / invokeTarget / startTime / endTime / status / exceptionInfo)

## 测试与质量

- 集成测试: 启动后通过 API 触发任务
- 单元测试: Cron 表达式解析、并发控制

## 常见问题 (FAQ)

**Q: 业务侧怎么新增一个定时任务?**
A: 1) 在 `task/` 下实现一个 `BaseJob` 子类; 2) 在 `sys_job` 表插入记录, `invoke_target` 填写 `bean.method(params)`; 3)
Quartz 启动时自动加载。

**Q: 任务执行失败怎么排查?**
A: 查 `sys_job_log` 表的 `exception_info` 字段 (含堆栈)。

**Q: 集群环境怎么办?**
A: Quartz 本身支持 JDBC JobStore, 配置 `spring.quartz.job-store-type=jdbc`, 配合 `quartz.properties` 启用集群模式即可。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/quartz/controller/SysJobController.java`
- `src/main/java/com/zwei/quartz/controller/SysJobLogController.java`
- `src/main/java/com/zwei/quartz/service/ISysJobService.java`
- `src/main/java/com/zwei/quartz/service/ISysJobLogService.java`

## 变更记录 (Changelog)

| 时间               | 变更                          |
|------------------|-----------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描) |
