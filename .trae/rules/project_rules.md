# 日志记录
- 所有服务类和控制器类都应添加 `@Slf4j` 注解，用于日志记录。
- 日志记录应遵循 SLF4J 规范，使用 `log` 变量进行日志输出。
- 禁止在业务逻辑中直接使用 `System.out.println` 或 `System.err.println` 进行日志输出。

# 数据库规范
- 表名都加前缀 `zw_`，例如 `zw_product_change_log`。
- 表名都采用下划线命名法，例如 `zw_product_change_log`。


# Java规范

- 接口命名都需要大写字母I开头且不以Service，例如 `IDeviceAuthentication`。

# 项目构建
- 项目采用pnpm作为包管理工具。
- 项目采用Maven作为项目管理工具。
- 前端启动开发服务的命令：`npm run dev`, 该命令会启动一个本地开发服务器，监听默认端口 `80`。
