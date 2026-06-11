[根目录](../../CLAUDE.md) > [server](../) > **zwei-system**

# zwei-system — RBAC 业务实现 + 通知公告子包

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-system**

## 模块职责

实现 RBAC 4 级权限模型:

1. **用户/角色/菜单/部门/岗位/字典** — 业务 CRUD
2. **通知公告** (`notice/` 子包, 包级隔离) — 含 SSE 实时推送 + 多通道架构预留

## 关键依赖

- `zwei-common` (基础类)
- `spring-webmvc` (Controller/视图)

## 主要子包

| 子包                    | 路径                                                                        | 职责                       |
|-----------------------|---------------------------------------------------------------------------|--------------------------|
| `service`             | `ISysUserService` 等 11 个                                                  | RBAC 业务接口                |
| `service.impl`        | 各 `*ServiceImpl`                                                          | MyBatis Mapper 调用 + 业务逻辑 |
| `mapper`              | `*Mapper.java` + `*Mapper.xml`                                            | MyBatis 数据访问             |
| `domain`              | `SysUser`/`SysRole`/`SysMenu` 等                                           | 业务实体                     |
| `notice.domain`       | `SysNotice`/`SysNoticeRead`                                               | 通知公告实体                   |
| `notice.service`      | `ISysNoticeService`/`ISysNoticeReadService`/`NoticeStreamPublisher` (SSE) | 通知公告                     |
| `notice.service.impl` | `SysNoticeServiceImpl`/`SysNoticeReadServiceImpl`                         | 通知实现                     |
| `notice.notify`       | `INotifyChannel`/`NotifyChannelDispatcher`/`NotifySendRequest`            | 多通道架构预留                  |
| `notice.mapper`       | `SysNoticeMapper`/`SysNoticeReadMapper`                                   | 通知 Mapper                |

## 对外接口 (Service)

| 接口                                            | 职责                  |
|-----------------------------------------------|---------------------|
| `ISysUserService`                             | 用户 CRUD + 密码 + 角色分配 |
| `ISysRoleService`                             | 角色 + 权限分配           |
| `ISysMenuService`                             | 菜单 + 权限字符串          |
| `ISysDeptService`                             | 部门树 (5 级)           |
| `ISysPostService`                             | 岗位                  |
| `ISysDictTypeService` / `ISysDictDataService` | 字典                  |
| `ISysConfigService`                           | 系统参数 (key-value)    |
| `ISysUserOnlineService`                       | 在线用户 (Redis)        |
| `ISysNoticeService` / `ISysNoticeReadService` | 通知公告                |

## 通知公告子包 (notice/)

**包级隔离**: 16 个文件从 `zwei-system` 顶层下沉到 `com.zwei.system.notice.*`, 业务代码零改动。

| 组件                        | 路径                                                                    | 职责                       |
|---------------------------|-----------------------------------------------------------------------|--------------------------|
| `INotifyChannel`          | `notify/INotifyChannel.java`                                          | 通知通道接口 (邮件/短信/钉钉/企微 扩展点) |
| `NotifyChannelDispatcher` | `notify/NotifyChannelDispatcher.java`                                 | 多通道分发 (按请求选择通道)          |
| `NotifySendRequest`       | `notify/NotifySendRequest.java`                                       | 发送请求 DTO                 |
| `NoticeStreamPublisher`   | `service/NoticeStreamPublisher.java`                                  | 通过 `SseEmitter` 推送到前端    |
| `SysNoticeController`     | `zwei-admin/web/controller/system/notice/SysNoticeController.java`    | CRUD + 已读                |
| `NoticeStreamController`  | `zwei-admin/web/controller/system/notice/NoticeStreamController.java` | SSE 端点                   |

事件流: `SysNoticeServiceImpl` → `applicationEventPublisher.publishEvent(NoticeCreatedEvent)` → `NoticeStreamPublisher`
监听 → SSE 推送给该用户

### 通知公告核心实现类索引 (P3)

| 类                          | 文件                                                  | 关键方法 / 责任                                                                                 |
|----------------------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------|
| `SysNoticeServiceImpl`     | `notice/service/impl/SysNoticeServiceImpl.java`     | CRUD + 唯一性校验 + 创建时 (status=0) 发布 `NoticeCreatedEvent`                                     |
| `SysNoticeReadServiceImpl` | `notice/service/impl/SysNoticeReadServiceImpl.java` | 已读记录管理 (UNIQUE `user_id,notice_id`)                                                       |
| `NoticeStreamPublisher`    | `notice/service/NoticeStreamPublisher.java`         | SSE 推送到前端 (CopyOnWriteArrayList<SseEmitter>)，超时 300_000ms，自动剔除完成/超时/错误的 emitter，HTML 标签过滤 |

## 数据模型 (核心表)

| 表                                                                   | 说明                   |
|---------------------------------------------------------------------|----------------------|
| `sys_user`                                                          | 用户                   |
| `sys_role`                                                          | 角色                   |
| `sys_user_role`                                                     | 用户-角色关联              |
| `sys_menu`                                                          | 菜单 (含 `perms` 权限字符串) |
| `sys_role_menu`                                                     | 角色-菜单                |
| `sys_dept`                                                          | 部门 (5 级)             |
| `sys_post`                                                          | 岗位                   |
| `sys_dict_type` / `sys_dict_data`                                   | 字典                   |
| `sys_config`                                                        | 参数                   |
| `sys_notice` / `sys_notice_read`                                    | 通知公告 + 已读记录          |
| `sys_organization`                                                  | 组织架构 (独立于 sys_dept)  |
| `sys_notify_instance` / `sys_notify_target` / `sys_notify_template` | 多通道通知实例/目标/模板        |

## 测试与质量

- 集成测试在 `zwei-admin` 启动时跑
- Service 单元测试建议覆盖: 权限字符串解析、菜单树构建、字典缓存

## 常见问题 (FAQ)

**Q: 怎样扩展通知通道?**
A: 实现 `INotifyChannel` 接口 (`send(NotifySendRequest)`), 通过 Spring 自动注入到 `NotifyChannelDispatcher`, 在
`getChannels()` 自动生效。

**Q: 数据权限的 5 级部门如何配置?**
A: 用户的 `dept_id` 是其所属部门; 查询时 `@DataScope` 切面根据 `ancestors` 字段向上找 5 级, 拼接到 SQL。

**Q: 怎样触发 SSE 推送?**
A: 在 Service 中 `applicationEventPublisher.publishEvent(new NoticeCreatedEvent(...))`, 由
`NoticeStreamPublisher.@EventListener` 接收并 `SseEmitter.send()`。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/system/service/ISys*Service.java` (11 个)
- `src/main/java/com/zwei/system/notice/service/impl/SysNoticeServiceImpl.java` (P3)
- `src/main/java/com/zwei/system/notice/service/NoticeStreamPublisher.java` (P3)

## 变更记录 (Changelog)

| 时间               | 变更                                                           |
|------------------|--------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                  |
| 2026-06-10 19:08 | 增量补扫: 新增通知公告核心实现类索引、sys_organization 与 sys_notify_* 表补充到数据模型 |
