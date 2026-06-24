# 消息中心功能完善 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 完善右上角通知中心：后端通知/公告列表加分页、前端面板分页 UI、告警跳转失败提示、公告详情页 404 修复、系统管理菜单组新增通知公告入口。

**架构：** 后端在 `listTop` / `recent` 接口顶层增加 `total` 字段并接收 `pageNum` / `pageSize`，返回结构向后兼容；前端 `layout/index.vue` 新增分页状态与极简上下页 UI，新建独立路由 `/system/notice/detail/:id` 承载公告详情页，`menuList` 追加 SysNotice 项暴露已有管理页。

**技术栈：** Java 17 + Spring Boot + MyBatis（后端）；Vue 3 + TS + Element Plus（前端）。

**项目测试惯例声明：** 本项目当前后端 `src/test` 目录为空、前端有 vitest 配置但仅覆盖 `src/lib` / `src/composables` / `src/components` 且现有 `views/` 与 `layout/` 下无任何测试。为避免本次功能扩展演变为"先补测试基础设施"的范围爆炸，本计划遵循项目现状：**验证步骤以"编译检查 + 手动 curl/浏览器操作"为主，不强行 TDD**。后续若团队决定建立测试基线，应作为独立任务推进。

**关联规格：** `docs/superpowers/specs/2026-06-24-message-center-enhancement-design.md`

---

## 文件结构

### 后端 Java/XML（修改 10 个文件）

| 文件 | 改动职责 |
|---|---|
| `server/zwei-system/src/main/java/com/zwei/system/notice/mapper/SysNoticeReadMapper.java` | 新增分页 + count 方法签名 |
| `server/zwei-system/src/main/resources/mapper/system/SysNoticeReadMapper.xml` | 改 `selectNoticeListWithReadStatus` 支持 `offset/limit` + 新增 `selectNoticeCountWithReadStatus` |
| `server/zwei-system/src/main/java/com/zwei/system/notice/service/ISysNoticeReadService.java` | 新增分页重载方法 |
| `server/zwei-system/src/main/java/com/zwei/system/notice/service/impl/SysNoticeReadServiceImpl.java` | 实现分页重载 |
| `server/zwei-admin/src/main/java/com/zwei/web/controller/system/notice/SysNoticeController.java` | `listTop` 接收 `pageNum/pageSize`，返回顶层加 `total` |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapper.java` | 新增 `selectUserUnreadPage` + `selectUserUnreadTotal` |
| `server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmNotificationMapper.xml` | 新增对应 SQL |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmNotificationService.java` | 新增分页方法 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java` | 实现分页 |
| `server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmNotificationController.java` | `recent` 接收 `pageNum/pageSize`，返回顶层加 `total` |

### 前端（修改 5 个 + 新建 1 个）

| 文件 | 改动职责 |
|---|---|
| `web/src/api/notice.ts` | `getTopNotices` 加 `pageNum/pageSize` 参数 + `total` 返回类型 |
| `web/src/api/alarmNotification.ts` | 新增 `getAlarmNotificationPage` 函数 |
| `web/src/layout/index.vue` | 分页状态 + footer UI + handleNoticeClick 简化 + menuList 加 SysNotice |
| `web/src/router/index.ts` | 加 `/system/notice/detail/:id` 路由 |
| `web/src/views/alarm/RealtimeAlarm.vue` | catch 加 `ElMessage.warning` |
| `web/src/views/system/NoticeDetail.vue` | **新建** 公告详情页 |

---

## 任务依赖关系

```
任务1 (后端公告分页) ─┐
                       ├─→ 任务3 (前端API) ─→ 任务4 (面板分页UI)
任务2 (后端告警分页) ─┘

任务5 (公告详情页)  ── 独立，但建议在任务3之后（handleNoticeClick 在 layout 内）
任务6 (告警catch提示) ── 独立
任务7 (菜单加SysNotice) ── 独立
```

推荐顺序：1 → 2 → 3 → 4 → 5 → 6 → 7（顺序执行，每步 commit）

---

## 任务 1：后端 — 公告 listTop 接口支持分页

**文件：**
- 修改：`server/zwei-system/src/main/java/com/zwei/system/notice/mapper/SysNoticeReadMapper.java`
- 修改：`server/zwei-system/src/main/resources/mapper/system/SysNoticeReadMapper.xml`
- 修改：`server/zwei-system/src/main/java/com/zwei/system/notice/service/ISysNoticeReadService.java`
- 修改：`server/zwei-system/src/main/java/com/zwei/system/notice/service/impl/SysNoticeReadServiceImpl.java`
- 修改：`server/zwei-admin/src/main/java/com/zwei/web/controller/system/notice/SysNoticeController.java`

- [ ] **步骤 1.1：阅读现有 Mapper 与 Service 接口**

运行：
```bash
cat server/zwei-system/src/main/java/com/zwei/system/notice/mapper/SysNoticeReadMapper.java
cat server/zwei-system/src/main/java/com/zwei/system/notice/service/ISysNoticeReadService.java
cat server/zwei-system/src/main/java/com/zwei/system/notice/service/impl/SysNoticeReadServiceImpl.java
cat server/zwei-admin/src/main/java/com/zwei/web/controller/system/notice/SysNoticeController.java
```

预期：看到现有 `selectNoticeListWithReadStatus(Long userId, int limit)` 等方法签名、`listTop` Controller 中的实现。

- [ ] **步骤 1.2：在 `SysNoticeReadMapper.java` 新增两个方法签名**

在文件末尾 `}` 之前追加：

```java
    /**
     * 分页查询当前用户的公告列表（带已读状态）。
     * 公告状态为"正常"（status='0'），按 notice_id DESC 排序。
     */
    List<SysNotice> selectNoticePageWithReadStatus(@Param("userId") Long userId,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    /**
     * 当前用户可见的正常状态公告总数（用于分页 total）。
     */
    int selectNoticeCountWithReadStatus(@Param("userId") Long userId);
```

注意 import：若文件顶部未 import `org.apache.ibatis.annotations.Param`，添加：
```java
import org.apache.ibatis.annotations.Param;
```

- [ ] **步骤 1.3：在 `SysNoticeReadMapper.xml` 新增两条 SQL**

在 `</mapper>` 之前（即文件末尾）追加：

```xml
    <!-- 分页查询带已读状态的公告列表 -->
    <select id="selectNoticePageWithReadStatus" resultType="SysNotice">
        select
            n.notice_id    as noticeId,
            n.notice_title as noticeTitle,
            n.notice_type  as noticeType,
            n.notice_content as noticeContent,
            n.status,
            n.create_by    as createBy,
            n.create_time  as createTime,
            case when r.notice_id is not null then true else false end as isRead
        from sys_notice n
        left join sys_notice_read r
            on r.notice_id = n.notice_id and r.user_id = #{userId}
        where n.status = '0'
        order by n.notice_id desc
        limit #{offset}, #{limit}
    </select>

    <!-- 当前用户可见公告总数 -->
    <select id="selectNoticeCountWithReadStatus" resultType="int">
        select count(*) from sys_notice n
        where n.status = '0'
    </select>
```

注意：`selectNoticeCountWithReadStatus` 不区分用户（"正常状态公告"对所有用户可见，因此 total 与用户无关）。这与 `selectUnreadCount` 的"用户特定"语义不同——后者计算"该用户未读数"。

- [ ] **步骤 1.4：在 `ISysNoticeReadService.java` 新增分页方法签名**

接口中追加：

```java
    /**
     * 分页查询当前用户可见的公告列表（带 isRead 标记）。
     *
     * @param userId   当前用户 ID
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @return 公告列表
     */
    List<SysNotice> selectNoticePage(Long userId, int pageNum, int pageSize);

    /**
     * 当前用户可见公告总数（用于分页 total）。
     */
    int selectNoticeCount();
```

- [ ] **步骤 1.5：在 `SysNoticeReadServiceImpl.java` 实现两个新方法**

在类末尾 `}` 之前追加：

```java
    @Override
    public List<SysNotice> selectNoticePage(Long userId, int pageNum, int pageSize) {
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, Math.min(pageSize, 50));
        int offset = (safePage - 1) * safeSize;
        return noticeReadMapper.selectNoticePageWithReadStatus(userId, offset, safeSize);
    }

    @Override
    public int selectNoticeCount() {
        return noticeReadMapper.selectNoticeCountWithReadStatus(null);
    }
```

注意：`selectNoticeCountWithReadStatus` 的 `userId` 参数实际未参与 SQL 过滤（公告对所有用户可见），传 `null` 即可。Mapper 接口保留 `@Param("userId")` 是为将来扩展（如按角色过滤公告）预留。

**确认 Impl 类字段名**：阅读现有 Impl 文件，确认 Mapper 字段名（很可能是 `noticeReadMapper` 或 `sysNoticeReadMapper`）。若字段名不同，使用实际名称替换。

- [ ] **步骤 1.6：在 `SysNoticeController.java` 改造 `listTop` 方法**

定位 `listTop` 方法（`@GetMapping("/listTop")`），改为：

```java
    /**
     * 首页顶部公告（通知中心面板用），支持分页。
     * 返回结构：{ code, msg, data: SysNotice[], total, unreadCount, timestamp }
     */
    @GetMapping("/listTop")
    public AjaxResult listTop(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtils.getUserId();
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, Math.min(pageSize, 50));

        List<SysNotice> list = noticeReadService.selectNoticePage(userId, safePage, safeSize);
        int total = noticeReadService.selectNoticeCount();
        int unreadCount = noticeReadService.selectUnreadCount(userId);

        AjaxResult ajax = AjaxResult.success(list);
        ajax.put("total", total);
        ajax.put("unreadCount", unreadCount);
        ajax.put("timestamp", System.currentTimeMillis());
        return ajax;
    }
```

**确认 import**：
- `org.springframework.web.bind.annotation.RequestParam` 已 import
- `com.zwei.common.utils.SecurityUtils` 已 import
- 若没有，添加对应 import 语句

**确认 Controller 字段名**：阅读文件，确认是 `noticeReadService` 还是 `sysNoticeReadService`，使用实际名称。

- [ ] **步骤 1.7：编译验证**

运行：
```bash
cd server
mvn clean compile -pl zwei-system,zwei-admin -am -q
```

预期：`BUILD SUCCESS`。若失败，根据错误信息修正 import 或字段名。

- [ ] **步骤 1.8：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add server/zwei-system/src/main/java/com/zwei/system/notice/mapper/SysNoticeReadMapper.java \
        server/zwei-system/src/main/resources/mapper/system/SysNoticeReadMapper.xml \
        server/zwei-system/src/main/java/com/zwei/system/notice/service/ISysNoticeReadService.java \
        server/zwei-system/src/main/java/com/zwei/system/notice/service/impl/SysNoticeReadServiceImpl.java \
        server/zwei-admin/src/main/java/com/zwei/web/controller/system/notice/SysNoticeController.java
git commit -m "feat(notice): listTop 接口支持分页

- SysNoticeReadMapper 新增 selectNoticePageWithReadStatus + selectNoticeCountWithReadStatus
- ISysNoticeReadService 新增 selectNoticePage / selectNoticeCount
- SysNoticeController.listTop 接收 pageNum/pageSize，返回顶层增加 total/timestamp"
```

---

## 任务 2：后端 — 告警通知 recent 接口支持分页

**文件：**
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapper.java`
- 修改：`server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmNotificationMapper.xml`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmNotificationService.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java`
- 修改：`server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmNotificationController.java`

- [ ] **步骤 2.1：阅读现有 Mapper / Service 接口**

运行：
```bash
cat server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapper.java
cat server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmNotificationService.java
cat server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java
```

确认现有 `selectUserRecent(Long userId, int limit)` 与 `selectUnreadCount(Long userId, String channel)` 的方法签名及 Impl 字段名（mapper 字段名）。

- [ ] **步骤 2.2：在 `AlarmNotificationMapper.java` 新增方法签名**

文件末尾 `}` 之前追加：

```java
    /**
     * 分页查询当前用户未读事件通知（SYSTEM 渠道，source_type IN alarm/offline）。
     */
    List<AlarmNotification> selectUserUnreadPage(@Param("userId") Long userId,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    /**
     * 当前用户未读事件通知总数（用于分页 total）。
     * 与 selectUnreadCount(userId, "SYSTEM") 等价，保留独立方法以明确语义。
     */
    int selectUserUnreadTotal(@Param("userId") Long userId);
```

确认 import：`org.apache.ibatis.annotations.Param` 已存在（项目已使用），无需新增。

- [ ] **步骤 2.3：在 `AlarmNotificationMapper.xml` 新增两条 SQL**

在 `</mapper>` 之前追加：

```xml
    <select id="selectUserUnreadPage" resultMap="AlarmNotificationResult">
        SELECT id, source_type, source_id, title, content,
               recipient_name, read_time, create_time, channel,
               dispatch_rule_id, status
        FROM alarm_notification
        WHERE recipient_id = #{userId}
          AND channel = 'SYSTEM'
          AND source_type IN ('alarm', 'offline')
          AND read_time IS NULL
        ORDER BY create_time DESC
        LIMIT #{offset}, #{limit}
    </select>

    <select id="selectUserUnreadTotal" resultType="int">
        SELECT COUNT(1)
        FROM alarm_notification
        WHERE recipient_id = #{userId}
          AND channel = 'SYSTEM'
          AND source_type IN ('alarm', 'offline')
          AND read_time IS NULL
    </select>
```

注意：`selectUserUnreadTotal` 比现有 `selectUnreadCount` 多了 `source_type IN ('alarm','offline')` 过滤——保持与 `selectUserUnreadPage` 的过滤条件完全一致，确保 total 与列表条数匹配。

- [ ] **步骤 2.4：在 `IAlarmNotificationService.java` 新增方法签名**

接口末尾 `}` 之前追加：

```java
    /**
     * 分页查询当前用户未读事件通知（仅 SYSTEM 渠道，alarm/offline 类型）。
     *
     * @param userId   用户 ID
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     */
    List<AlarmNotification> selectUserUnreadPage(Long userId, int pageNum, int pageSize);

    /**
     * 当前用户未读事件通知总数（仅 SYSTEM 渠道，alarm/offline 类型）。
     */
    int selectUserUnreadTotal(Long userId);
```

- [ ] **步骤 2.5：在 `AlarmNotificationServiceImpl.java` 实现两个新方法**

类末尾 `}` 之前追加：

```java
    @Override
    public List<AlarmNotification> selectUserUnreadPage(Long userId, int pageNum, int pageSize) {
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, Math.min(pageSize, 50));
        int offset = (safePage - 1) * safeSize;
        return notificationMapper.selectUserUnreadPage(userId, offset, safeSize);
    }

    @Override
    public int selectUserUnreadTotal(Long userId) {
        return notificationMapper.selectUserUnreadTotal(userId);
    }
```

**确认字段名**：阅读 Impl 文件确认 Mapper 字段名（很可能为 `notificationMapper` 或 `alarmNotificationMapper`），使用实际名称。

- [ ] **步骤 2.6：改造 `AlarmNotificationController.java#recent`**

将现有 `recent` 方法替换为：

```java
    /**
     * 当前用户未读事件通知列表（分页）。
     * <p>已读事件不再返回。返回顶层包含 total 字段，便于前端分页控件计算总页数。</p>
     * <p>向后兼容：若调用方传入 limit（旧契约），则映射为 pageSize=limit, pageNum=1。</p>
     */
    @GetMapping("/recent")
    @PreAuthorize("@ss.hasPermi('alarm:notification:list')")
    public AjaxResult recent(@RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) Integer limit) {
        Long userId = SecurityUtils.getUserId();
        // 向后兼容：旧调用方传 limit 时，退化为第 1 页取 limit 条
        int safePage, safeSize;
        if (limit != null) {
            safePage = 1;
            safeSize = Math.max(1, Math.min(limit, 100));
        } else {
            safePage = Math.max(1, pageNum);
            safeSize = Math.max(1, Math.min(pageSize, 50));
        }
        List<AlarmNotification> list = notificationService.selectUserUnreadPage(userId, safePage, safeSize);
        int total = notificationService.selectUserUnreadTotal(userId);
        List<AlarmNotificationItemVO> vos = list.stream().map(this::toItemVO).toList();
        AjaxResult ajax = AjaxResult.success(vos);
        ajax.put("total", total);
        return ajax;
    }
```

- [ ] **步骤 2.7：编译验证**

运行：
```bash
cd server
mvn clean compile -pl zwei-iot-alarm -am -q
```

预期：`BUILD SUCCESS`。

- [ ] **步骤 2.8：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/mapper/AlarmNotificationMapper.java \
        server/zwei-iot-alarm/src/main/resources/mapper/alarm/AlarmNotificationMapper.xml \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/IAlarmNotificationService.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/service/impl/AlarmNotificationServiceImpl.java \
        server/zwei-iot-alarm/src/main/java/com/zwei/iot/alarm/controller/AlarmNotificationController.java
git commit -m "feat(alarm): notifications/recent 接口支持分页

- AlarmNotificationMapper 新增 selectUserUnreadPage + selectUserUnreadTotal
- IAlarmNotificationService 新增对应分页方法
- Controller#recent 接收 pageNum/pageSize，返回顶层增加 total
- 向后兼容：旧调用方传 limit 时映射为 pageSize=limit pageNum=1"
```

---

## 任务 3：前端 API — 封装分页参数

**文件：**
- 修改：`web/src/api/notice.ts`
- 修改：`web/src/api/alarmNotification.ts`

- [ ] **步骤 3.1：阅读现有 API 封装**

运行：
```bash
cat web/src/api/notice.ts
cat web/src/api/alarmNotification.ts
```

确认 `getTopNotices` / `getRecentAlarmNotifications` 的现有签名与返回类型。

- [ ] **步骤 3.2：改造 `web/src/api/notice.ts`**

定位 `getTopNotices` 函数，改为：

```typescript
/** 首页顶部公告列表（分页）。返回 { data, total, unreadCount, timestamp } */
export function getTopNotices(pageNum = 1, pageSize = 10): Promise<TopNoticeResponse> {
  return request({
    url: '/system/notice/listTop',
    method: 'get',
    params: { pageNum, pageSize }
  })
}
```

确认 `TopNoticeResponse` 类型定义（同文件内），需包含 `total` 字段：

```typescript
export interface TopNoticeResponse {
  code: number
  msg: string
  data: SysNotice[]
  total: number
  unreadCount: number
  timestamp?: number
}
```

若现有定义缺 `total`，加上。

- [ ] **步骤 3.3：在 `web/src/api/alarmNotification.ts` 新增分页 API**

在文件末尾追加：

```typescript
/** 分页查询当前用户未读事件通知（替代 getRecentAlarmNotifications）。 */
export function getAlarmNotificationPage(
  pageNum = 1,
  pageSize = 10
): Promise<AjaxResult<AlarmNotificationItem[]>> {
  return request({
    url: '/alarm/notifications/recent',
    method: 'get',
    params: { pageNum, pageSize }
  })
}
```

返回结构：`AjaxResult<T>` 中通过顶层扩展 `total` 字段。需要确认 `AjaxResult` 类型允许任意额外字段——查看 `web/src/api/notice.ts` 或类型定义文件。若 `AjaxResult` 是 strict 类型，改为：

```typescript
export interface AlarmNotificationPageResponse {
  code: number
  msg: string
  data: AlarmNotificationItem[]
  total: number
}

export function getAlarmNotificationPage(
  pageNum = 1,
  pageSize = 10
): Promise<AlarmNotificationPageResponse> {
  return request({
    url: '/alarm/notifications/recent',
    method: 'get',
    params: { pageNum, pageSize }
  })
}
```

`getRecentAlarmNotifications(limit)` 保留不删，避免破坏其他调用点（但 layout 中将改用新函数）。

- [ ] **步骤 3.4：类型检查**

运行：
```bash
cd web
npm run build
```

预期：编译通过（vite build 内含 vue-tsc 类型检查）。若有类型错误，根据提示修正。

- [ ] **步骤 3.5：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add web/src/api/notice.ts web/src/api/alarmNotification.ts
git commit -m "feat(web-api): 通知中心分页接口封装

- getTopNotices 增加 pageNum/pageSize 参数
- TopNoticeResponse 类型增加 total 字段
- 新增 getAlarmNotificationPage 函数（旧 getRecentAlarmNotifications 保留）"
```

---

## 任务 4：前端 — 消息面板分页 UI

**文件：**
- 修改：`web/src/layout/index.vue`

- [ ] **步骤 4.1：定位现有代码位置**

运行（在项目根目录）：
```bash
grep -n "eventUnreadCount\|noticeUnreadCount\|fetchEventMessages\|fetchNoticeMessages\|message-panel-footer\|markAllAsRead" web/src/layout/index.vue
```

记录以下关键行号：
- 分页状态声明区（约 376-392 行）
- `fetchNoticeMessages` / `fetchEventMessages`（约 422-441 行）
- footer 模板（约 302-304 行）
- `markAllAsRead`（约 791+ 行）

- [ ] **步骤 4.2：新增分页状态（紧邻现有消息状态声明区）**

在 `noticeUnreadCount` / `eventUnreadCount` 声明之后追加：

```typescript
const eventPage = reactive({ current: 1, size: 10, total: 0 })
const noticePage = reactive({ current: 1, size: 10, total: 0 })

/** 当前 Tab 对应的分页对象（便于模板与翻页函数统一引用） */
const currentPageRef = computed(() => notifyTab.value === 'event' ? eventPage : noticePage)

/** 当前 Tab 总页数（至少 1，避免空列表显示 1/0） */
const currentTabTotalPages = computed(() => {
  const p = currentPageRef.value
  return Math.max(1, Math.ceil(p.total / p.size))
})
```

- [ ] **步骤 4.3：改造 `fetchNoticeMessages` 与 `fetchEventMessages`**

替换原函数为：

```typescript
async function fetchNoticeMessages() {
  try {
    const res = await getTopNotices(noticePage.current, noticePage.size)
    noticeMessages.value = (res.data ?? []).map(toNoticeMessage)
    noticeUnreadCount.value = res.unreadCount ?? 0
    noticePage.total = res.total ?? 0
  } catch { /* keep previous data */ }
}

async function fetchEventMessages() {
  try {
    const [pageRes, unreadRes] = await Promise.all([
      getAlarmNotificationPage(eventPage.current, eventPage.size),
      getAlarmNotificationUnreadCount()
    ])
    eventMessages.value = (pageRes.data ?? []).map(toEventMessage)
    eventPage.total = pageRes.total ?? 0
    eventUnreadCount.value = unreadRes.data?.unreadCount ?? 0
  } catch { /* keep previous data */ }
}
```

- [ ] **步骤 4.4：更新 import**

修改 `<script setup>` 顶部的 import：

```typescript
import {getTopNotices, markRead as markNoticeRead, markReadAll as markAllNoticeRead, type SysNotice} from '@/api/notice'
import {
  getAlarmNotificationPage,         // ← 替换 getRecentAlarmNotifications
  getAlarmNotificationUnreadCount,
  markAlarmNotificationRead,
  markAllAlarmNotificationsRead,
  type AlarmNotificationItem
} from '@/api/alarmNotification'
```

- [ ] **步骤 4.5：新增翻页函数**

在 `fetchEventMessages` 之后追加：

```typescript
function reloadCurrentTab() {
  if (notifyTab.value === 'event') fetchEventMessages()
  else fetchNoticeMessages()
}

function goPrevPage() {
  if (currentPageRef.value.current <= 1) return
  currentPageRef.value.current--
  reloadCurrentTab()
}

function goNextPage() {
  if (currentPageRef.value.current >= currentTabTotalPages.value) return
  currentPageRef.value.current++
  reloadCurrentTab()
}
```

- [ ] **步骤 4.6：改造 SSE 推送回调，重置到第 1 页**

定位 `startNoticeSSE` 中 `noticeEventSource.addEventListener('notice', ...)` 回调，将其中的：
```typescript
noticeMessages.value.unshift(msg)
if (noticeMessages.value.length > 20) noticeMessages.value.pop()
noticeUnreadCount.value++
```

替换为：
```typescript
noticePage.current = 1
noticeUnreadCount.value++
fetchNoticeMessages()
```

（SSE 推送时重置到第 1 页 + 重新拉取，让用户立即看到新公告。）

类似地，定位 `startAlarmSSE` 中 `alarm-notify` 与 `alarm` 事件回调，在 `fetchEventMessages()` 调用前加：
```typescript
eventPage.current = 1
```

- [ ] **步骤 4.7：改造 `markAllAsRead` 重置页码**

定位 `markAllAsRead`，在两个分支内增加页码重置：

```typescript
const markAllAsRead = async () => {
  if (notifyTab.value === 'event') {
    try {
      await markAllAlarmNotificationsRead()
      eventMessages.value = []
      eventUnreadCount.value = 0
      eventPage.current = 1
      eventPage.total = 0
    } catch { /* ignore */ }
  } else {
    // 此分支现有逻辑保持不变，仅在末尾追加：
    try {
      await markAllNoticeRead()
      // ... 现有的 list 重置逻辑 ...
      noticePage.current = 1
      noticePage.total = 0
    } catch { /* ignore */ }
  }
}
```

**注意**：阅读现有 `markAllAsRead` 公告分支的实际代码（约 791-810 行），不要破坏现有 `noticeMessages.value = ...` / `noticeUnreadCount.value = 0` 语句，仅在末尾追加 page 重置。

- [ ] **步骤 4.8：改造 footer 模板**

定位现有 `<div class="message-panel-footer" v-if="currentTabHasMessages">`，替换其内部内容为：

```vue
<div class="message-panel-footer" v-if="currentTabHasMessages">
  <div class="pager" v-if="currentTabTotalPages > 1">
    <span class="pager-btn"
          :class="{ disabled: currentPageRef.current <= 1 }"
          @click="goPrevPage">‹</span>
    <span class="pager-info">{{ currentPageRef.current }}/{{ currentTabTotalPages }}</span>
    <span class="pager-btn"
          :class="{ disabled: currentPageRef.current >= currentTabTotalPages }"
          @click="goNextPage">›</span>
  </div>
  <el-button size="small" @click="markAllAsRead">全部标为已读</el-button>
</div>
```

- [ ] **步骤 4.9：添加分页样式**

在 `<style scoped>` 末尾追加（具体位置：找到现有 `.message-panel-footer` 样式附近，追加在其后）：

```css
.pager {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}
.pager-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.pager-btn:hover:not(.disabled) {
  background: rgba(0, 0, 0, 0.06);
}
.pager-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.pager-info {
  min-width: 36px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
```

- [ ] **步骤 4.10：类型检查 + 构建验证**

运行：
```bash
cd web
npm run build
```

预期：构建通过。

- [ ] **步骤 4.11：浏览器手动验证**

启动后端（`mvn package -DskipTests -pl zwei-admin -am` + `java -jar -Dspring.profiles.active=local zwei-admin/target/zwei-admin.jar`）与前端（`npm run dev`）。

验证清单：
- [ ] 通知中心面板打开，双 Tab 显示数据
- [ ] 当 total > size 时，footer 显示 `1/N` 与上下页按钮
- [ ] 点击 › 翻到下一页，数据更新
- [ ] 点击 ‹ 翻回上一页
- [ ] 第 1 页时 ‹ 灰禁用；最后一页时 › 灰禁用
- [ ] SSE 推送新公告时，自动跳回第 1 页
- [ ] "全部标为已读"后页码重置为 1/1

- [ ] **步骤 4.12：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add web/src/layout/index.vue
git commit -m "feat(layout): 通知中心面板分页 UI

- 新增 eventPage/noticePage 分页状态与 currentTabTotalPages 计算属性
- footer 增加极简上下页按钮（‹ 1/3 ›）
- SSE 推送新消息时重置到第 1 页
- 全部标为已读后页码归 1
- 改用 getAlarmNotificationPage 替代 getRecentAlarmNotifications"
```

---

## 任务 5：前端 — 公告详情页 + 路由 + handleNoticeClick 简化

**文件：**
- 创建：`web/src/views/system/NoticeDetail.vue`
- 修改：`web/src/router/index.ts`
- 修改：`web/src/layout/index.vue`

- [ ] **步骤 5.1：新建 `web/src/views/system/NoticeDetail.vue`**

```vue
<template>
  <div class="notice-detail-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h2 class="page-title">公告详情</h2>
    </div>
    <el-card v-loading="loading" class="notice-card">
      <template v-if="detail.noticeId">
        <h1 class="notice-title">{{ detail.noticeTitle }}</h1>
        <div class="notice-meta">
          <el-tag :type="detail.noticeType === '1' ? 'warning' : 'success'" size="small">
            {{ detail.noticeType === '1' ? '通知' : '公告' }}
          </el-tag>
          <span class="meta-item">发布人：{{ detail.createBy || '-' }}</span>
          <span class="meta-item">发布时间：{{ detail.createTime || '-' }}</span>
        </div>
        <el-divider />
        <div class="notice-content" v-html="sanitizedContent" />
      </template>
      <el-empty v-else-if="!loading" description="公告不存在或已被删除" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoticeById, markRead, type SysNotice } from '@/api/notice'

const route = useRoute()
const router = useRouter()
const detail = ref<Partial<SysNotice>>({})
const loading = ref(false)

/** 简单 XSS 缓解：移除 <script>/<iframe>/<object>/<embed> 标签。
 *  公告仅 system:notice:add 权限的管理员可发布，信任端输入；
 *  此处做一层兜底过滤，避免意外粘贴恶意脚本。 */
const sanitizedContent = computed(() => {
  const html = detail.value.noticeContent ?? ''
  return html
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<(iframe|object|embed)\b[^>]*>.*?<\/\1>/gis, '')
})

async function loadDetail() {
  const id = Number(route.params.id)
  if (Number.isNaN(id) || id <= 0) {
    ElMessage.error('公告 ID 无效')
    return
  }
  loading.value = true
  try {
    const res = await getNoticeById(id)
    detail.value = res.data ?? {}
    // 异步标记已读，不阻塞渲染（失败静默，下次进入会再次尝试）
    if (res.data?.noticeId) {
      markRead(id).catch(() => { /* ignore */ })
    }
  } catch {
    ElMessage.error('加载公告详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/system/notice')
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.notice-detail-page {
  padding: 16px 24px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}
.notice-card {
  max-width: 900px;
}
.notice-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 12px;
}
.notice-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}
.meta-item {
  font-size: 13px;
}
.notice-content {
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}
.notice-content :deep(img) {
  max-width: 100%;
}
</style>
```

- [ ] **步骤 5.2：在 `web/src/router/index.ts` 注册路由**

在 Layout children 中 `/system/notice` 行之后追加：

```typescript
{ path: '/system/notice', name: 'SysNotice', component: () => import('@/views/system/SysNotice.vue') },
{ path: '/system/notice/detail/:id', name: 'NoticeDetail', component: () => import('@/views/system/NoticeDetail.vue') },
```

- [ ] **步骤 5.3：简化 `web/src/layout/index.vue#handleNoticeClick`**

定位现有 `handleNoticeClick`（约 762-772 行），替换为：

```typescript
const handleNoticeClick = (msg: NotifyMessage) => {
  router.push(`/system/notice/detail/${msg.id}`)
  messagePanelVisible.value = false
}
```

移除原 `markNoticeRead` 调用与 try/catch——标记已读由 `NoticeDetail.vue` 负责（用户真正打开详情页才标记）。

若 `markNoticeRead` 在文件其他位置已无引用，从 import 中移除：
```typescript
// 修改前
import {getTopNotices, markRead as markNoticeRead, markReadAll as markAllNoticeRead, type SysNotice} from '@/api/notice'
// 修改后
import {getTopNotices, markReadAll as markAllNoticeRead, type SysNotice} from '@/api/notice'
```

**注意**：`markAllNoticeRead` 仍用于 `markAllAsRead`，保留。

- [ ] **步骤 5.4：类型检查 + 构建**

运行：
```bash
cd web
npm run build
```

预期：构建通过。

- [ ] **步骤 5.5：浏览器手动验证**

- [ ] 通知中心 → 公告 Tab → 点击任一公告 → 跳转 `/system/notice/detail/{id}`
- [ ] 详情页正确展示标题、类型 tag、发布人、发布时间、富文本内容
- [ ] 浏览器返回按钮回到上一页
- [ ] 再次打开通知中心，该公告显示为已读（无未读小圆点）
- [ ] 直接输入不存在的 ID（如 `/system/notice/detail/99999999`）→ 显示 `el-empty` 提示
- [ ] 直接输入非法 ID（如 `/system/notice/detail/abc`）→ 弹出"公告 ID 无效"

- [ ] **步骤 5.6：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add web/src/views/system/NoticeDetail.vue web/src/router/index.ts web/src/layout/index.vue
git commit -m "feat(notice): 新增公告详情页 + 路由

- 新建 views/system/NoticeDetail.vue（富文本渲染 + XSS 兜底过滤）
- 注册路由 /system/notice/detail/:id（修复原 handleNoticeClick 跳 404）
- handleNoticeClick 移除 markNoticeRead 调用，标记已读职责转移到详情页"
```

---

## 任务 6：前端 — 告警事件跳转失败提示

**文件：**
- 修改：`web/src/views/alarm/RealtimeAlarm.vue`

- [ ] **步骤 6.1：定位 catch 分支**

打开文件，定位 202-222 行（`onMounted` 内 `?alarmId=` 处理逻辑）。

- [ ] **步骤 6.2：确认 ElMessage 已 import**

运行：
```bash
grep -n "ElMessage" web/src/views/alarm/RealtimeAlarm.vue | head -5
```

若未 import，在 `<script setup>` 顶部添加：
```typescript
import { ElMessage } from 'element-plus'
```

- [ ] **步骤 6.3：修改 catch 分支增加用户提示**

将：
```typescript
      } catch { /* 告警可能已不存在或无权查看，静默忽略 */ }
```

替换为：
```typescript
      } catch {
        // 告警可能已被处置/删除，或当前用户无权查看
        ElMessage.warning('该告警可能已被处置或删除，无法查看详情')
      }
```

同时，在 `if (detail && detail.id)` 的 else 分支也加提示：
```typescript
        if (detail && detail.id) {
          currentRow.value = detail
          detailDialogVisible.value = true
        } else {
          ElMessage.warning('该告警可能已被处置或删除，无法查看详情')
        }
```

- [ ] **步骤 6.4：类型检查 + 构建**

运行：
```bash
cd web
npm run build
```

预期：构建通过。

- [ ] **步骤 6.5：浏览器手动验证**

- [ ] 在数据库手动删除一条 alarm_record（或修改其 id 不存在）
- [ ] 在 alarm_notification 表保留指向该 id 的通知记录
- [ ] 通知中心 → 事件 Tab → 点击该通知 → 跳转到 /alarm/realtime?alarmId=X
- [ ] 应弹出"该告警可能已被处置或删除，无法查看详情"提示

- [ ] **步骤 6.6：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add web/src/views/alarm/RealtimeAlarm.vue
git commit -m "fix(alarm): 通知中心跳转的告警不存在时给用户提示

原 catch 静默吞掉异常，用户无感知。现补充 ElMessage.warning，
覆盖 detail 不存在或接口异常两种场景。"
```

---

## 任务 7：前端 — 系统管理菜单组新增通知公告入口

**文件：**
- 修改：`web/src/layout/index.vue`

- [ ] **步骤 7.1：在 menuList System 项 children 追加 SysNotice**

定位 `menuList` 中 `name: 'System'` 的项（约 587-598 行），在 children 末尾追加：

```typescript
{
  name: 'System',
  label: '系统管理',
  icon: '<svg ...>...</svg>',
  children: [
    { name: 'Organization', label: '组织管理' },
    { name: 'Identity', label: '身份管理' },
    { name: 'Permission', label: '权限管理' },
    { name: 'Log', label: '日志管理' },
    { name: 'Settings', label: '系统设置' },
    { name: 'SysNotice', label: '通知公告' }   // ← 新增
  ]
}
```

- [ ] **步骤 7.2：在 menuRouteMap 追加映射**

定位 `menuRouteMap`（约 615-644 行），在 `Settings: '/system/settings',` 之后追加：

```typescript
  Settings: '/system/settings',
  SysNotice: '/system/notice',
```

- [ ] **步骤 7.3：在 menuLabelMap 追加映射**

定位 `menuLabelMap`（约 646+ 行），在 `Settings: '系统设置',` 之后追加：

```typescript
  Settings: '系统设置',
  SysNotice: '通知公告',
```

- [ ] **步骤 7.4：类型检查 + 构建**

运行：
```bash
cd web
npm run build
```

预期：构建通过。

- [ ] **步骤 7.5：浏览器手动验证**

- [ ] 左侧菜单"系统管理"展开 → 末尾出现"通知公告"
- [ ] 点击"通知公告" → 跳转 `/system/notice`，显示已有的 SysNotice.vue CRUD 页面
- [ ] CRUD 操作（新增/修改/删除/查看）正常工作
- [ ] 已读人员查询功能正常

- [ ] **步骤 7.6：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add web/src/layout/index.vue
git commit -m "feat(menu): 系统管理菜单组新增通知公告入口

复用已有 SysNotice.vue 与 /system/notice 路由，无需改数据库。
menuRouteMap 与 menuLabelMap 同步追加映射。"
```

---

## 任务 8：全链路冒烟测试

- [ ] **步骤 8.1：重建后端 jar**

```bash
cd server
mvn clean package -DskipTests -pl zwei-admin -am -q
```

预期：`BUILD SUCCESS`。

- [ ] **步骤 8.2：重启后端**

```bash
# 在 server/ 目录
# 先 kill 旧进程（按 PID），再启动
java -jar -Dspring.profiles.active=local zwei-admin/target/zwei-admin.jar
```

预期：约 22 秒后看到 `Started RuoYiApplication`。

- [ ] **步骤 8.3：前端构建验证**

```bash
cd web
npm run build
```

预期：dist 目录生成，无类型错误。

- [ ] **步骤 8.4：完整业务流程冒烟**

1. 登录系统 → 右上角铃铛显示未读角标
2. 打开通知中心：
   - 事件 Tab：显示前 10 条未读，若有 >10 条显示分页按钮
   - 公告 Tab：显示前 10 条，若有 >10 条显示分页按钮
3. 翻页：上下页按钮工作正常
4. 点击告警事件 → 跳 `/alarm/realtime?alarmId=X` → 详情对话框弹出
5. 点击公告 → 跳 `/system/notice/detail/X` → 详情页正常显示
6. 左侧"系统管理"→"通知公告" → CRUD 管理页正常
7. 触发一条新告警（或在 DB 插一条 alarm_notification）→ SSE 推送到事件 Tab，自动跳第 1 页

- [ ] **步骤 8.5：更新 web/CLAUDE.md 与根 CLAUDE.md 变更记录**

在 `web/CLAUDE.md` 末尾"变更记录"表格追加一行：

```markdown
| 2026-06-24 18:00 | 通知中心 v2: 双 Tab 分页 + 公告详情页 + 菜单暴露 SysNotice | listTop/recent 接口分页 + NoticeDetail.vue + menuList 加 SysNotice |
```

在根 `CLAUDE.md` 末尾"变更记录"表格追加一行（若团队惯例需要）。

- [ ] **步骤 8.6：Commit**

```bash
cd "E:\work\PMO\4.其他项目\sys-交通边坡监测预警\zwei"
git add web/CLAUDE.md CLAUDE.md
git commit -m "docs: 更新 CLAUDE.md 变更记录 - 通知中心 v2 上线"
```

---

## 自检清单

### 规格覆盖度

| 规格章节 | 实现任务 |
|---|---|
| 第 1 节：后端 listTop 分页 | 任务 1 |
| 第 1 节：后端 recent 分页 | 任务 2 |
| 第 2 节：前端面板分页 UI | 任务 3 (API) + 任务 4 (UI) |
| 第 3 节：告警跳转 + 失败提示 | 任务 6 |
| 第 4 节：公告详情页 | 任务 5 |
| 第 5 节：系统管理菜单 | 任务 7 |
| 全链路验证 | 任务 8 |

无遗漏。

### 占位符扫描

- ✗ 无 TODO / 待定 / "后续实现"
- ✗ 无"添加适当的错误处理"等模糊描述
- ✗ 无"类似任务 N"——每个任务的代码块都完整给出
- ✓ 后端字段名（mapper / service）在步骤中明确说明"若不同请替换"，这是合理的工程提示，非占位符

### 类型一致性

- `getTopNotices(pageNum, pageSize)` — 任务 3 定义，任务 4 使用 ✓
- `getAlarmNotificationPage(pageNum, pageSize)` — 任务 3 定义，任务 4 使用 ✓
- `selectNoticePage(userId, pageNum, pageSize)` — 任务 1 Service 接口定义，Controller 调用 ✓
- `selectUserUnreadPage(userId, pageNum, pageSize)` — 任务 2 Service 接口定义，Controller 调用 ✓
- `eventPage` / `noticePage` — 任务 4 步骤 4.2 定义，步骤 4.3 / 4.5 / 4.6 / 4.7 使用 ✓
- `currentPageRef` / `currentTabTotalPages` — 任务 4 步骤 4.2 定义，步骤 4.8 模板使用 ✓
- `NoticeDetail` 路由 name — 任务 5 步骤 5.2 定义，无其他任务引用 ✓

类型一致。

### 潜在风险

| 风险 | 控制措施 |
|---|---|
| Impl 类字段名可能与计划不同 | 每个步骤 1.1 / 2.1 明确要求先 cat 文件确认 |
| `markAllAsRead` 公告分支可能更复杂 | 步骤 4.7 明确"不破坏现有逻辑，仅末尾追加" |
| `alarm_notification.source_type` 可能为 NULL | 任务 2 SQL 与现有 `selectUserRecent` 保持一致，行为不变 |
| `sys_notice.status='0'` 语义 | 任务 1 SQL 与现有 `selectUnreadCount` 保持一致 |

---

## 执行交接

计划已完成并保存到 `docs/superpowers/plans/2026-06-24-message-center-enhancement.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

选哪种方式？
