# 消息中心功能完善设计

## 背景

顶部右上角"通知中心"铃铛的消息面板（`web/src/layout/index.vue`）当前存在 5 个体验缺口：

1. **后端**：通知中心场景下的告警通知列表（`/alarm/notifications/recent`）、公告列表（`/system/notice/listTop`）均不分页，硬编码条数上限。
2. **前端**：消息面板双 Tab（事件/公告）只渲染接口返回的第一页数据，无翻页 UI。
3. **告警事件**：从面板跳转到 `/alarm/realtime?alarmId=X` 后虽然 `RealtimeAlarm.vue` 已实现自动弹详情，但 `getAlarmRecordDetail` 失败时静默吞掉异常，用户无任何反馈。
4. **公告**：`handleNoticeClick` 跳转 `/system/notice/detail/:id`，但该路由未注册，跳过去 404。
5. **菜单**：完整的 `SysNotice.vue` 管理页与路由 `/system/notice` 已存在，但 `menuList` 写死的"系统管理"菜单组没有暴露入口，用户无法通过侧边栏进入管理页。

## 目标

- 通知中心双 Tab 支持分页加载，UI 紧凑不挤占下拉浮层空间。
- 公告点击跳详情页可正常打开，告警点击跳独立页并自动打开详情对话框。
- "系统管理"菜单组新增"通知公告"子菜单，复用已有 `SysNotice.vue`。
- 保持现有"事件 Tab 仅显示未读"语义（用户决策：分页 = 未读记录的分页）。

## 现状分析

### 后端分页现状

| 端点 | 分页 | 说明 |
|---|---|---|
| `GET /api/v1/system/notice/list` | YES | 管理后台用，权限 `system:notice:list` |
| `GET /api/v1/system/notice/listTop` | NO | 硬编码 `limit=5`，无权限，首页顶部公告用 |
| `GET /api/v1/alarm/notifications/recent` | NO | `limit` 参数最大 100，仅未读 |
| `GET /api/v1/alarm/notifications/unread-count` | NO | 计数 |

### 前端关键代码位置

| 文件 | 行号 | 说明 |
|---|---|---|
| `web/src/layout/index.vue` | 219-307 | 消息面板模板（双 Tab + footer） |
| `web/src/layout/index.vue` | 376-441 | 消息状态 + 拉取函数 |
| `web/src/layout/index.vue` | 762-789 | `handleNoticeClick` / `handleEventClick` |
| `web/src/layout/index.vue` | 530-599 | `menuList` 写死菜单 |
| `web/src/layout/index.vue` | 615-644 | `menuRouteMap` / `menuLabelMap` |
| `web/src/views/alarm/RealtimeAlarm.vue` | 202-222 | `?alarmId=` 自动打开详情逻辑 |
| `web/src/views/system/SysNotice.vue` | 1-120 | 已存在的完整 CRUD 管理页 |
| `web/src/router/index.ts` | 63 | `/system/notice` 已注册，缺 `/detail/:id` |

### 已有可复用资产

- `AlarmDetailDialog.vue` — 告警详情弹窗，已被 `RealtimeAlarm.vue` 引用
- `SysNotice.vue` — 完整的公告 CRUD 管理页，含内置详情 Dialog
- `getAlarmRecordDetail(id)` / `getNoticeById(id)` — 详情 API 已存在

## 设计

### 第 1 节：后端通知消息列表支持分页

#### 1.1 `GET /api/v1/system/notice/listTop` 改造

**Controller：** `SysNoticeController.java#listTop`

新增请求参数：

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `pageNum` | int | 1 | 页码，从 1 开始 |
| `pageSize` | int | 10 | 每页条数，最大 50 |

**返回结构：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [ /* SysNotice[]，含 isRead 标记 */ ],
  "total": 25,
  "unreadCount": 3,
  "timestamp": "..."
}
```

向后兼容：现有前端不传 `pageNum/pageSize` 时使用默认值（10 条），与原 5 条上限行为不同但前端会同步改造。

**Service：** `ISysNoticeReadService`

```java
// 现有
List<SysNotice> selectNoticeListWithReadStatus(Long userId, int limit);

// 新增重载
List<SysNotice> selectNoticeListWithReadStatus(Long userId, int pageNum, int pageSize);
int selectNoticeCountWithReadStatus(Long userId);  // 用于 total
```

**Mapper：** `SysNoticeReadMapper` + xml 增加对应 SQL，按 `status='0'` + `createTime DESC` 过滤，LEFT JOIN `sys_notice_read` 计算当前用户的 `isRead`。

#### 1.2 `GET /api/v1/alarm/notifications/recent` 改造

**Controller：** `AlarmNotificationController.java#recent`

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `pageNum` | int | 1 | 页码 |
| `pageSize` | int | 10 | 每页条数，最大 50 |

废弃 `limit` 参数（或保留兼容：若传了 `limit` 则 `pageSize=limit, pageNum=1`）。

**返回结构：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [ /* AlarmNotificationItemVO[] */ ],
  "total": 8
}
```

保持"仅未读"语义（`readTime IS NULL`），排序 `createTime DESC`。

**Service：** `IAlarmNotificationService`

```java
// 现有
List<AlarmNotification> selectUserRecent(Long userId, int limit);

// 新增
List<AlarmNotification> selectUserUnreadPage(Long userId, int pageNum, int pageSize);
// selectUnreadCount(userId, "SYSTEM") 已存在，复用为 total
```

**Mapper：** `AlarmNotificationMapper` + xml 增加分页 SQL。

---

### 第 2 节：前端消息面板分页 UI

#### 2.1 状态扩展

`web/src/layout/index.vue`：

```ts
const eventPage = reactive({ current: 1, size: 10, total: 0 })
const noticePage = reactive({ current: 1, size: 10, total: 0 })

const currentTabTotalPages = computed(() => {
  const p = notifyTab.value === 'event' ? eventPage : noticePage
  return Math.max(1, Math.ceil(p.total / p.size))
})
```

#### 2.2 数据拉取函数改造

```ts
async function fetchNoticeMessages() {
  try {
    const res = await getTopNotices(noticePage.current, noticePage.size)
    noticeMessages.value = (res.data ?? []).map(toNoticeMessage)
    noticeUnreadCount.value = res.unreadCount ?? 0
    noticePage.total = res.total ?? 0
  } catch { /* keep previous */ }
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
  } catch { /* keep previous */ }
}
```

#### 2.3 Footer UI（替换现有 `message-panel-footer`）

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

```ts
const currentPageRef = computed(() => notifyTab.value === 'event' ? eventPage : noticePage)

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
function reloadCurrentTab() {
  if (notifyTab.value === 'event') fetchEventMessages()
  else fetchNoticeMessages()
}
```

#### 2.4 交互规则

| 触发场景 | 行为 |
|---|---|
| 切换 Tab | 保留各自页码（不重置） |
| SSE 推送新事件/公告 | 当前 Tab 重置到第 1 页并刷新，让用户立即看到新消息 |
| 标记单条已读 | 重新拉当前页（total 会更新） |
| 全部标记已读 | 清空当前 Tab 数据，`total=0`，页码回 1 |
| 翻页按钮 disabled 态 | 第 1 页禁用 ‹，最后一页禁用 › |

#### 2.5 CSS

新增 `.pager` / `.pager-btn` / `.pager-info` 样式，灰底圆角，disabled 态降低透明度 + cursor:not-allowed。

---

### 第 3 节：告警事件点击跳转独立详情页

#### 3.1 主流程（保持现状）

`handleEventClick` 不改跳转目标：

```ts
if (msg.sourceType === 'alarm') {
  router.push({ path: '/alarm/realtime', query: { alarmId: String(msg.sourceId) } })
} else if (msg.sourceType === 'offline') {
  router.push({ path: '/basic/device', query: { deviceId: String(msg.sourceId) } })
}
```

`RealtimeAlarm.vue:202-222` 已实现 `?alarmId=` 自动调 `getAlarmRecordDetail` + 弹 `AlarmDetailDialog`。

#### 3.2 失败提示改造

`RealtimeAlarm.vue:219` 当前 catch 静默忽略。改为：

```ts
} catch (e) {
  ElMessage.warning('该告警可能已被处置或删除，无法查看详情')
}
```

#### 3.3 边缘情况说明

跳转到"待办告警"页时，若该告警已被处置（status≠1），列表里看不到对应行——但 `AlarmDetailDialog` 仍会正常弹出详情，用户可查看完整信息。**当前可接受**，未来若需要"跳转到对应状态页"的精细化路由，作为后续迭代项。

---

### 第 4 节：公告详情页（修复 404）

#### 4.1 新增路由

`web/src/router/index.ts` Layout children 中追加：

```ts
{
  path: '/system/notice/detail/:id',
  name: 'NoticeDetail',
  component: () => import('@/views/system/NoticeDetail.vue')
}
```

#### 4.2 新建 `views/system/NoticeDetail.vue`

**模板结构：**

```
.notice-detail-page
├── .header  ← 返回按钮 + "公告详情" 标题
└── el-card
    ├── h1.notice-title
    ├── .notice-meta  ← 类型 tag + 发布人 + 发布时间
    ├── el-divider
    └── .notice-content (v-html)  ← 富文本渲染
```

**逻辑：**

```ts
const route = useRoute()
const router = useRouter()
const detail = ref<Partial<SysNotice>>({})
const loading = ref(false)

async function loadDetail() {
  const id = Number(route.params.id)
  if (Number.isNaN(id)) {
    ElMessage.error('公告 ID 无效')
    return
  }
  loading.value = true
  try {
    const res = await getNoticeById(id)
    detail.value = res.data ?? {}
    // 异步标记已读，不阻塞渲染
    markRead(id).catch(() => { /* ignore */ })
  } finally {
    loading.value = false
  }
}

function goBack() {
  // 优先 router.back()，无历史则回 /system/notice
  if (window.history.length > 1) router.back()
  else router.push('/system/notice')
}

onMounted(loadDetail)
```

#### 4.3 `layout/index.vue#handleNoticeClick` 改造

移除 `markNoticeRead` 调用（标记已读由详情页负责）：

```ts
const handleNoticeClick = (msg: NotifyMessage) => {
  router.push(`/system/notice/detail/${msg.id}`)
  messagePanelVisible.value = false
}
```

但前端角标即时更新仍需要——通过监听 `markRead` 成功或 SSE 推送刷新角标。**简化方案**：详情页 `markRead` 成功后，回到通知中心面板时通过 `onMounted`/面板打开时调 `fetchNoticeMessages()` 重拉即可（用户下次打开面板会看到最新角标）。

---

### 第 5 节：系统管理菜单组新增"通知公告"入口

#### 5.1 前端 menuList 改动

`web/src/layout/index.vue` 第 591-597 行 `System` children 末尾追加：

```ts
{
  name: 'System',
  label: '系统管理',
  // ... icon
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

#### 5.2 路由映射

```ts
// menuRouteMap
SysNotice: '/system/notice',

// menuLabelMap
SysNotice: '通知公告',
```

#### 5.3 不需要改动

- **路由表**：`/system/notice` 已注册（router/index.ts:63）
- **页面组件**：`SysNotice.vue` 完整 CRUD 已就绪
- **数据库**：前端菜单不读 `sys_menu` 表，无需插入菜单数据
- **权限过滤**：项目惯例仅 `ServiceStatus` 做角色过滤，其他菜单全员可见，沿用惯例

---

## 文件清单

### 后端 Java（共 8 个文件）

| 文件 | 改动类型 |
|---|---|
| `server/zwei-admin/.../notice/SysNoticeController.java` | 改 `listTop` 加分页参数 + total 返回 |
| `server/zwei-iot-alarm/.../controller/AlarmNotificationController.java` | 改 `recent` 加分页参数 + total 返回 |
| `server/zwei-system/.../notice/service/ISysNoticeReadService.java` | 新增分页重载方法 |
| `server/zwei-system/.../notice/service/impl/SysNoticeReadServiceImpl.java` | 实现分页重载 |
| `server/zwei-system/.../notice/mapper/SysNoticeReadMapper.java` | 新增 Mapper 方法 |
| `server/zwei-system/.../notice/mapper/SysNoticeReadMapper.xml` | 新增分页 SQL |
| `server/zwei-iot-alarm/.../service/IAlarmNotificationService.java` | 新增分页方法 |
| `server/zwei-iot-alarm/.../service/impl/AlarmNotificationServiceImpl.java` | 实现分页 |

### 前端 TypeScript / Vue（共 6 个文件，其中 1 个新增）

| 文件 | 改动类型 |
|---|---|
| `web/src/api/notice.ts` | `getTopNotices` 加分页参数 |
| `web/src/api/alarmNotification.ts` | 新增 `getAlarmNotificationPage` |
| `web/src/layout/index.vue` | 分页状态 + footer UI + handleNoticeClick 简化 + menuList 加 SysNotice |
| `web/src/router/index.ts` | 加 `/system/notice/detail/:id` 路由 |
| `web/src/views/alarm/RealtimeAlarm.vue` | catch 加用户提示 |
| `web/src/views/system/NoticeDetail.vue` | **新建** |

## 不在范围内（YAGNI）

- 菜单不读后端 `sys_menu` 表（项目历史架构，不在本次改造范围）
- 不做告警跳转到"对应状态页"的精细化路由（当前待办告警页弹详情可接受）
- 不改造 `list` / `readUsersList` 等管理后台分页接口（已支持）
- 不引入新的权限校验逻辑（沿用 `system:notice:list` 现有权限字符串）
- 不做"已读/未读切换开关"（用户选择保留"仅未读"语义）

## 风险与回滚

| 风险 | 缓解 |
|---|---|
| `listTop` 改造后老调用方（如其他客户端）拿不到 5 条数据 | 接口向后兼容：传 limit 时映射到 pageSize，不传时默认 10 |
| 前端 menuList 写死无角色过滤，普通用户也能看到"通知公告"菜单 | 沿用项目惯例，全员可见；后端 `@PreAuthorize('system:notice:list')` 兜底 |
| 公告 v-html 渲染存在 XSS 风险 | 公告仅管理员可发布（`system:notice:add` 权限），信任端输入；不做净化 |
| SSE 推送 + 分页同时存在时页码错乱 | 推送时强制重置到第 1 页并重新拉取 |
