# 算法库功能设计规格

> 创建日期: 2026-06-17
> 状态: 已批准 (待实施)
> 关联需求: 在告警中心下新增【算法库】菜单及页面，支持算法版本化管理与 Python 算法包上传

## 1. 背景与目标

### 1.1 业务背景

当前告警中心仅提供判据、综合策略（Groovy 脚本）等告警配置能力，缺少对**外部算法包**（特别是 Python 算法）的统一管理。本次需求新增"算法库"模块，作为未来算法引擎接入的基础管理设施：

- 集中托管算法元数据（名称、描述、启停状态）
- 支持算法的多版本归档（用户手动输入版本号 + 上传 zip 算法包）
- 卡片视图直观展示，便于运维人员快速浏览

### 1.2 范围与非目标

**包含：**
- 算法信息与版本两张表的 CRUD
- 算法包 zip 文件上传 / 下载（本地磁盘存储）
- 前端卡片列表 + 详情抽屉 + 版本管理
- 启停开关（仅算法整体层级）

**不包含（YAGNI）：**
- Python 执行器接入（当前项目无 Python 运行时）
- 算法包内容解析、解压、内部结构校验
- 版本对比、"当前活跃版本"机制
- 物理文件清理（保留全部历史以便审计）
- 与 Groovy 综合策略脚本的集成

## 2. 关键设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 启停粒度 | 算法整体启停（version 表无 status） | 用户确认；版本仅作历史归档 |
| 版本号 | 用户手动输入 + 按 create_time 倒序 | 用户确认；不强制语义化版本规范 |
| 上传机制 | 算法库专用上传接口（不复用 `/common/upload`） | 需记录 size/sha256/原文件名，仅接受 zip |
| 后端模块 | 放入 `zwei-iot-alarm`，子包 `algolib` | 与告警逻辑集中，便于后续引擎对接 |
| 菜单注册 | **前端硬编码 `layout/index.vue`** | 当前架构约定，不走 sys_menu |
| 删除策略 | 算法逻辑删 + 级联逻辑删版本，**保留物理文件** | 审计追溯需要 |

## 3. 数据库设计

### 3.1 表结构

#### `algo_info`（算法信息表）

```sql
CREATE TABLE `algo_info` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(64)  NOT NULL COMMENT '算法编码（全局唯一，用于程序引用）',
    `name`        varchar(128) NOT NULL COMMENT '算法名称',
    `description` varchar(500) DEFAULT NULL COMMENT '算法描述',
    `status`      tinyint      DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
    `del_flag`    tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `create_by`   varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `del_flag`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法信息表';
```

#### `algo_version`（算法版本表）

```sql
CREATE TABLE `algo_version` (
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `algo_id`        bigint       NOT NULL COMMENT '算法ID（关联 algo_info.id）',
    `version_no`     varchar(64)  NOT NULL COMMENT '版本号（用户输入，同一算法下唯一）',
    `file_name`      varchar(255) NOT NULL COMMENT '存储文件名（相对路径，如 algo-lib/2026/06/17/uuid.zip）',
    `original_name`  varchar(255) NOT NULL COMMENT '原始文件名',
    `file_size`      bigint       DEFAULT '0' COMMENT '文件大小（字节）',
    `sha256`         varchar(64)  DEFAULT NULL COMMENT 'SHA256 摘要（完整性校验）',
    `del_flag`       tinyint      DEFAULT '0' COMMENT '删除标记: 0-正常, 1-删除',
    `create_by`      varchar(64)  DEFAULT NULL COMMENT '创建者',
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（版本列表按此倒序）',
    `update_by`      varchar(64)  DEFAULT NULL COMMENT '更新者',
    `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         varchar(500) DEFAULT NULL COMMENT '版本说明（变更日志）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_algo_version` (`algo_id`, `version_no`, `del_flag`),
    KEY `idx_algo_id` (`algo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法版本表';
```

### 3.2 设计要点

- **唯一索引含 `del_flag`**：允许逻辑删除后重建同名记录（绕开 MySQL 唯一索引对 NULL 重复值的不友好行为）
- **`code` 字段**：程序内引用用（后续告警引擎对接时使用），与展示用 `name` 分离，编辑时不可修改
- **`file_name` 存相对路径**：便于迁移存储后端；前端通过 `/algo-lib/versions/{id}/download` 间接下载
- **无外键约束**：遵循项目约定（仅 `device_hazard_point`、`video_device_hazard_point` 保留物理外键），关联由 Service 层维护

### 3.3 升级脚本位置

- 路径：`db/upgrade/V20260617__algo_library.sql`（**新建 `db/upgrade/` 目录**）
- 内容：仅上述两条 CREATE TABLE 语句（**不包含** sys_menu 菜单初始化）

## 4. 后端设计

### 4.1 模块结构

包路径：`com.zwei.iot.alarm.algolib.*`（在 `zwei-iot-alarm` 模块下新建子包）

```
server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/algolib/
├── controller/
│   ├── AlgoLibraryController.java       # 算法 CRUD + 启停
│   └── AlgoVersionController.java       # 版本列表 + 上传 + 下载 + 删除
├── service/
│   ├── IAlgoLibraryService.java
│   ├── IAlgoVersionService.java
│   └── impl/
│       ├── AlgoLibraryServiceImpl.java
│       └── AlgoVersionServiceImpl.java
├── domain/
│   ├── AlgoInfo.java
│   ├── AlgoVersion.java
│   └── dto/
│       ├── AlgoCreateRequest.java       # 新增（code/name/description/remark）
│       ├── AlgoUpdateRequest.java       # 修改（name/description/remark，无 code）
│       ├── AlgoStatusRequest.java       # 启停（status: 0|1）
│       └── AlgoVersionCreateRequest.java# 上传时元数据（version_no/remark）
├── mapper/
│   ├── AlgoInfoMapper.java
│   └── AlgoVersionMapper.java
└── resources/mapper/
    ├── AlgoInfoMapper.xml
    └── AlgoVersionMapper.xml
```

### 4.2 API 接口清单

统一前缀 `/api/v1/algo-lib/*`，遵循项目 `BaseController + AjaxResult + PageHelper` 模式。

#### 算法管理

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/algo-lib/page` | `iot:algo-library:list` | 分页列表（支持 name 模糊、status 筛选） |
| GET | `/algo-lib/{id}` | `iot:algo-library:query` | 详情（含版本列表，按 create_time DESC） |
| POST | `/algo-lib` | `iot:algo-library:add` | 新增算法 |
| PUT | `/algo-lib/{id}` | `iot:algo-library:edit` | 修改算法（不可改 code） |
| PUT | `/algo-lib/{id}/status` | `iot:algo-library:edit` | 启停（复用 edit 权限） |
| DELETE | `/algo-lib/{id}` | `iot:algo-library:remove` | 删除算法（级联逻辑删版本） |

#### 版本管理

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/algo-lib/{algoId}/versions` | `iot:algo-library:query` | 某算法的版本列表（倒序） |
| POST | `/algo-lib/{algoId}/versions/upload` | `iot:algo-library:upload` | 上传 zip 包 + 元数据 |
| DELETE | `/algo-lib/versions/{id}` | `iot:algo-library:remove` | 删除某版本（逻辑删，不删物理文件） |
| GET | `/algo-lib/versions/{id}/download` | `iot:algo-library:query` | 下载算法包 |

### 4.3 上传接口约束

- **HTTP**：`POST multipart/form-data`
- **入参**：`MultipartFile file` + `String versionNo` + `String remark`
- **类型校验**：仅接受 `.zip` 扩展名（Controller 层显式校验，不依赖浏览器 Content-Type）
- **大小限制**：单文件 100 MB；同步调整 `application.yml` 的 `spring.servlet.multipart.max-file-size` 与 `max-request-size`
- **存储路径**：`{zwei.profile}/algo-lib/yyyy/MM/dd/{uuid}.zip`（与 CommonController 的 `/upload` 目录隔离）
- **入库字段**：
  - `file_name` = 相对路径（如 `algo-lib/2026/06/17/uuid.zip`）
  - `original_name` = 原始文件名
  - `file_size` = 字节数
  - `sha256` = 上传时计算并存储
- **唯一性校验**：Service 层先 `SELECT COUNT(*) WHERE algo_id=? AND version_no=? AND del_flag=0`，存在则抛 `ServiceException("版本号已存在")`

### 4.4 算法删除事务流程

```
deleteAlgo(id):
  BEGIN TRANSACTION
    UPDATE algo_info  SET del_flag=1 WHERE id=?
    UPDATE algo_version SET del_flag=1 WHERE algo_id=?
  COMMIT
  // 物理文件保留，不删除
```

### 4.5 列表联表字段

`AlgoInfo.java` Domain 中额外增加三个非持久化字段，由分页查询 SQL 联表 `algo_version` 填充：

```java
@TableField(exist = false)  // 或 MyBatis <result> 标签
private Integer versionCount;       // 该算法下未删除版本数

@TableField(exist = false)
private String  latestVersionNo;    // 最近一次上传的 version_no

@TableField(exist = false)
private LocalDateTime latestUploadTime;  // 最近一次上传 create_time
```

`AlgoInfoMapper.xml` 的分页查询使用 LEFT JOIN 子查询：

```sql
SELECT a.*,
       COALESCE(v.cnt, 0)                    AS version_count,
       v.latest_version_no                   AS latest_version_no,
       v.latest_upload_time                  AS latest_upload_time
FROM algo_info a
LEFT JOIN (
    SELECT algo_id,
           COUNT(*)                          AS cnt,
           MAX(create_time)                  AS latest_upload_time,
           SUBSTRING_INDEX(GROUP_CONCAT(version_no ORDER BY create_time DESC), ',', 1) AS latest_version_no
    FROM algo_version
    WHERE del_flag = 0
    GROUP BY algo_id
) v ON v.algo_id = a.id
WHERE a.del_flag = 0
  <if test="name != null and name != ''">AND a.name LIKE CONCAT('%', #{name}, '%')</if>
  <if test="status != null">AND a.status = #{status}</if>
ORDER BY a.create_time DESC
```

### 4.6 关键基础设施扩展

- `MimeTypeUtils`（`zwei-common`）：新增 `ZIP = "zip"` 常量到 `DEFAULT_ALLOWED_EXTENSION`
- `FileUploadUtils`：新增方法 `String extractAlgoLibPath(String filename)` 生成 `algo-lib/yyyy/MM/dd/uuid.ext` 相对路径
- `application.yml`：调整 `spring.servlet.multipart.max-file-size=100MB`、`max-request-size=100MB`

## 5. 前端设计

### 5.1 文件结构

```
web/src/
├── api/
│   └── algoLibrary.ts                  # 所有算法库 API
├── views/alarm/
│   ├── AlgoLibrary.vue                 # 卡片列表页（主组件）
│   └── components/
│       ├── AlgoFormDialog.vue          # 新增/编辑算法弹窗
│       └── AlgoDetailDrawer.vue        # 详情抽屉（含版本列表 + 上传）
└── layout/
    └── index.vue                       # 修改：三处新增算法库菜单
```

### 5.2 路由

在 `web/src/router/index.ts` 新增：

```typescript
{
  path: '/alarm/algo-library',
  name: 'AlgoLibrary',
  component: () => import('@/views/alarm/AlgoLibrary.vue')
}
```

### 5.3 菜单硬编码（`web/src/layout/index.vue`）

**修改 3 处：**

1. `menuList` 中 `Alarm.children`（综合告警后、通知设置前插入）：

```javascript
{
  name: 'Alarm',
  label: '告警中心',
  // ...icon...
  children: [
    {name: 'RealtimeAlarm', label: '待办告警'},
    {name: 'AlarmNotification', label: '历史告警'},
    {divider: true},
    { name: 'AlarmCriteria', label: '告警判据' },
    {name: 'AlarmDisposal', label: '综合告警'},
    {name: 'AlgoLibrary', label: '算法库'},          // 新增
    {divider: true},
    {name: 'NotificationSetting', label: '通知设置'}
  ]
}
```

2. `menuRouteMap` 加：`AlgoLibrary: '/alarm/algo-library'`
3. `menuLabelMap` 加：`AlgoLibrary: '算法库'`

**菜单可见性**：跟随 `filteredMenuList` 默认逻辑（不做角色过滤，全部可见）。按钮级权限通过 `hasPerm('iot:algo-library:*')` 控制。

### 5.4 卡片列表页（`AlgoLibrary.vue`）

**参考**：`views/alarm/CompositeAlarm.vue` 的 CSS Grid + el-card 风格。

**顶部工具栏：**
- 搜索框（算法名称模糊）
- 状态筛选下拉（全部/启用/停用）
- 「新增算法」按钮（权限 `iot:algo-library:add`）

**卡片网格**（`grid-template-columns: repeat(auto-fill, minmax(380px, 1fr))`）：

```
┌────────────────────────────────────────┐
│ 算法名称              [启用 ●○] ←switch│  header
│ 算法描述文字（最多 2 行，超出省略）    │
├────────────────────────────────────────┤
│ 编码:  ALGO_RAIN_01                   │  meta
│ 版本数: 3                             │
│ 最近上传: 2026-06-15 14:32 (v1.2.0)   │
├────────────────────────────────────────┤
│ [详情] [编辑] [删除]                   │  footer
└────────────────────────────────────────┘
```

**交互：**
- 启停开关 `el-switch`：直接调 `PUT /algo-lib/{id}/status`，无需二次确认
- 「详情」打开抽屉（含版本管理）
- 「编辑」打开 `AlgoFormDialog`（不可改 code）
- 「删除」二次确认 `el-message-box`，提示"将同时删除该算法下所有版本记录"
- 停用状态卡片半透明（复用 `card--disabled` 样式）

**分页**：复用综合告警分页（`page-sizes: [12, 24, 48]`）。

### 5.5 新增/编辑弹窗（`AlgoFormDialog.vue`）

**字段：**
- `code`（新增时必填、正则 `^[A-Z][A-Z0-9_]{2,63}$`，编辑时禁用）
- `name`（必填，1-128 字符）
- `description`（可选，textarea，500 字内）
- `remark`（可选，textarea）

**校验**：前端校验 + 后端 `@Validated` 兜底。

### 5.6 详情抽屉（`AlgoDetailDrawer.vue`）

**布局**：`el-drawer` size=720px，右侧滑出。

**上半部分（算法基本信息）：** 算法名称、编码、描述、状态、创建/更新时间。

**下半部分（版本列表）：**
- 表格 `el-table`，列：版本号、原始文件名、文件大小（KB/MB 格式化）、上传人、上传时间、备注、操作（下载、删除）
- 按 `create_time` 倒序（后端已排）
- 顶部「上传新版本」按钮（权限 `iot:algo-library:upload`）

**上传新版本交互：**
1. 点击按钮 → 弹出 `el-dialog`
2. 表单：版本号输入框（必填） + 备注（可选） + 文件选择器（`el-upload` drag 模式，仅 `.zip`，100 MB 内）
3. 校验：版本号在该算法下不能重复（前端调 `GET /versions` 本地校验 + 后端兜底）
4. 提交：`POST /algo-lib/{algoId}/versions/upload`（multipart + 字段）
5. 上传中显示进度条（`el-progress`），成功后关闭弹窗、刷新版本表

**版本行操作：**
- 下载：`GET /algo-lib/versions/{id}/download`（权限 `iot:algo-library:query`）
- 删除：二次确认后 `DELETE /algo-lib/versions/{id}`（逻辑删）

### 5.7 API 模块（`algoLibrary.ts`）

```typescript
import request from '@/utils/request'

// ===== 类型 =====
export interface AlgoInfoPageParams {
  pageNum?: number
  pageSize?: number
  name?: string
  status?: 0 | 1
}
export interface AlgoInfoPayload {
  code?: string
  name: string
  description?: string
  remark?: string
}
export interface AlgoInfo {
  id: number
  code: string
  name: string
  description?: string
  status: 0 | 1
  versionCount?: number
  latestVersion?: string
  latestUploadTime?: string
  createTime?: string
  updateTime?: string
}
export interface AlgoVersion {
  id: number
  algoId: number
  versionNo: string
  fileName: string
  originalName: string
  fileSize: number
  sha256?: string
  createBy?: string
  createTime?: string
  remark?: string
}

// ===== 算法 =====
export function getAlgoLibraryPage(params: AlgoInfoPageParams) {
  return request.get('/algo-lib/page', { params })
}
export function getAlgoLibraryDetail(id: number | string) {
  return request.get(`/algo-lib/${id}`)
}
export function createAlgoLibrary(data: AlgoInfoPayload) {
  return request.post('/algo-lib', data)
}
export function updateAlgoLibrary(id: number | string, data: AlgoInfoPayload) {
  return request.put(`/algo-lib/${id}`, data)
}
export function updateAlgoLibraryStatus(id: number | string, status: 0 | 1) {
  return request.put(`/algo-lib/${id}/status`, { status })
}
export function deleteAlgoLibrary(id: number | string) {
  return request.delete(`/algo-lib/${id}`)
}

// ===== 版本 =====
export function getAlgoVersionList(algoId: number | string) {
  return request.get(`/algo-lib/${algoId}/versions`)
}
export function uploadAlgoVersion(
  algoId: number | string,
  payload: { file: File; versionNo: string; remark?: string },
  onProgress?: (percent: number) => void
) {
  const formData = new FormData()
  formData.append('file', payload.file)
  formData.append('versionNo', payload.versionNo)
  if (payload.remark) formData.append('remark', payload.remark)
  return request.post(`/algo-lib/${algoId}/versions/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
    }
  })
}
export function deleteAlgoVersion(id: number | string) {
  return request.delete(`/algo-lib/versions/${id}`)
}
export function downloadAlgoVersion(id: number | string) {
  return request.raw.get(`/algo-lib/versions/${id}/download`, { responseType: 'blob' })
}
```

## 6. 边界情况处理

| 场景 | 处理 |
|---|---|
| 删除算法 | Service 层事务：`algo_info.del_flag=1` + `algo_version.del_flag=1`；**物理文件保留** |
| 上传时版本号冲突 | Service 层先查未删除记录，存在则抛 `ServiceException("版本号已存在")` |
| 上传非 zip 文件 | Controller 层校验扩展名，返回 `AjaxResult.error("仅支持 zip 格式")` |
| 上传超过 100 MB | Spring multipart 全局配置 + Controller 层二次校验 `file.getSize()` |
| code 重复 | DB 唯一索引兜底；Service 层先查提前返回友好错误 |
| 编辑时修改 code | `AlgoUpdateRequest` DTO 不含 code 字段 |
| 下载文件不存在 | Service 层校验 `FileUtils.exist()`，返回 404 |
| 唯一索引含 del_flag | 允许删除后重建同名 |

## 7. 实现顺序

1. **数据库升级脚本**：`db/upgrade/V20260617__algo_library.sql`（建表）
2. **后端基础设施**：扩展 `MimeTypeUtils`、`FileUploadUtils`、`application.yml`
3. **后端 Domain + Mapper + XML**：`AlgoInfo`、`AlgoVersion`
4. **后端 Service + Impl**：业务逻辑、唯一性校验、级联逻辑删、文件处理
5. **后端 Controller**：两个 Controller，对接权限注解
6. **前端 API 模块**：`algoLibrary.ts`
7. **前端路由 + 菜单硬编码**：`router/index.ts` + `layout/index.vue`（三处）
8. **前端卡片列表页**：`AlgoLibrary.vue`
9. **前端弹窗 + 抽屉**：`AlgoFormDialog.vue`、`AlgoDetailDrawer.vue`
10. **本地验证**：启动后端 + 前端，跑通"新增 → 上传 → 启停 → 删除"全流程

## 8. 测试覆盖

### 8.1 后端单元测试（`zwei-iot-alarm/src/test/`）

- `AlgoLibraryServiceImplTest`：CRUD、code 唯一性、级联逻辑删
- `AlgoVersionServiceImplTest`：版本号冲突、文件元数据正确写入

### 8.2 前端手测清单

- [ ] 新增算法 → 卡片出现 → 上传版本 → 详情抽屉显示 → 启停 → 删除算法 → 验证版本同时逻辑删
- [ ] 上传非 zip 文件被拒绝
- [ ] 上传超过 100 MB 文件被拒绝
- [ ] 同算法下重复版本号被拒绝
- [ ] 编辑时 code 字段禁用
- [ ] 非 admin 用户按钮权限正确隐藏

## 9. worktree 开发流程

1. 从 `web260429` 创建 worktree `algo-library`（使用 `using-git-worktrees` skill）
2. 按上述实现顺序 10 步推进
3. 完成后 PR 合并回 `web260429`（或按用户要求合到 develop）

## 10. 风险与未决事项

| 项 | 说明 |
|---|---|
| 物理文件累积 | 算法包不主动清理，长期可能占用磁盘；后续可加定时清理 job |
| 并发上传冲突 | 极低概率；DB 唯一索引兜底，前端捕获 `DuplicateKeyException` 转友好提示 |
| 后续 Python 执行器接入 | 本设计仅做管理，未来需新增 `algo_executor` 模块调度 Python 子进程，并引用 `algo_info.code` + 指定 `version_no` |
