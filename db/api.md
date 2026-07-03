# 地质灾害监测预警系统 - 后端接口设计文档

**日期：** 2026-07-03\
**版本：** v2.0\
**作者：** 系统开发组

---

## 变更记录

| 版本 | 日期 | 变更内容 |
| --- | --- | --- |
| v2.0 | 2026-07-03 | 全面升级：新增 Terra AI 助手、算法库、告警通知中心、数据共享策略、数据解析策略、MQTT 异常报文、传感器维度查询、计算属性测试 共 8 大模块接口文档；更新告警分发规则 v2（阈值/综合分离）；补充 MQTT 消息日志/异常日志独立端点 |
| v1.6 | 2026-06-04 | 告警统计统一使用触发次数，移除默认7天时间过滤；告警模块数据完整性修复 |

---

## 1. 接口基础规范

### 1.1 请求方式

| 操作 | HTTP方法 | 示例                           |
| -- | ------ | ---------------------------- |
| 查询 | GET    | `/api/v1/hazard-points/page` |
| 新增 | POST   | `/api/v1/hazard-points`      |
| 修改 | PUT    | `/api/v1/hazard-points/{id}` |
| 删除 | DELETE | `/api/v1/hazard-points/{id}` |

### 1.2 路径规范

- 小写 + 中横线
- 按模块划分：`/api/v1/{module}/{resource}`

### 1.3 公共请求头

```http
Authorization: Bearer {token}
Content-Type: application/json
```

### 1.4 统一返回格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {},
  "timestamp": 1735689213000
}
```

### 1.5 状态码定义

| 状态码 | 含义    | 说明          |
| --- | ----- | ----------- |
| 200 | 成功    | 操作成功        |
| 400 | 参数错误  | 参数缺失、校验失败、类型不匹配、JSON解析失败 |
| 401 | 未登录   | Token过期或未登录 |
| 403 | 无权限   | 没有操作权限      |
| 404 | 不存在   | 资源不存在       |
| 405 | 方法不允许 | 请求方法不支持    |
| 409 | 冲突    | 幂等请求冲突、唯一键冲突、状态不一致 |
| 415 | 媒体类型错误 | Content-Type 不受支持 |
| 500 | 服务器异常 | 系统内部错误      |
| 601 | 预警触发  | 告警触发        |
| 602 | 设备离线  | 设备离线通知      |

### 1.5.1 常见异常响应

| 场景 | 返回码 | 典型消息 |
| --- | --- | --- |
| 缺少必填查询参数 | 400 | `缺少必填参数[id]` |
| 路径/查询参数类型不匹配 | 400 | `请求参数类型不匹配，参数[id]要求类型为：'java.lang.Long'...` |
| `@RequestBody` 校验失败 | 400 | `用户名不能为空` |
| 方法级参数校验失败 | 400 | `请求参数校验失败` 或具体校验消息 |
| JSON 格式错误或请求体为空 | 400 | `请求体格式错误或JSON解析失败` |
| 请求方法不支持 | 405 | `Request method 'POST' is not supported` |
| 请求内容类型不支持 | 415 | `不支持的请求内容类型` |
| 接口或静态资源不存在 | 404 | `请求资源不存在` |
| 未登录或 Token 无效 | 401 | `请求访问：{uri}，认证失败，无法访问系统资源` |
| 无接口权限 | 403 | `没有权限，请联系管理员授权` |

### 1.6 分页规范

**请求参数：**

- `pageNum`: 页码，默认1
- `pageSize`: 每页数量，默认10

**返回格式：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "total": 128,
    "rows": [],
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1735689213000
}
```

***

## 2. 系统管理模块接口

### 2.1 用户认证

**说明：**

- 登录、验证码、用户信息、路由接口统一使用 `/api/v1/auth` 前缀。
- 登出接口由 Spring Security 接管，实际路径为 `/api/v1/system/auth/logout`。
- 注册接口为独立匿名接口 `/register`。

#### 2.1.1 获取验证码

**路径：** `GET /api/v1/auth/captcha`\
**描述：** 获取登录/注册验证码。当前系统默认验证码类型为 `math`，但接口返回的是 Base64 图片，不直接返回算式文本。

**请求参数：** 无

**成功响应（验证码开启）：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "captchaEnabled": true,
    "captchaKey": "6f4a94b3d7b24f0e8f15f28f2a6b32fd",
    "captchaImage": "/9j/4AAQSkZJRgABAQAAAQABAAD..."
  },
  "timestamp": 1779624887000
}
```

**成功响应（验证码关闭）：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "captchaEnabled": false
  },
  "timestamp": 1779624887000
}
```

| 字段           | 类型      | 必返 | 说明 |
| ------------ | ------- | -- | -- |
| captchaEnabled | boolean | 是  | 是否启用验证码 |
| captchaKey   | string  | 否  | 验证码唯一标识，验证码开启时返回 |
| captchaImage | string  | 否  | Base64 编码图片内容，前端可拼接 `data:image/png;base64,` 使用 |

#### 2.1.2 登录

**路径：** `POST /api/v1/auth/login`\
**描述：** 用户名密码登录，成功后返回 JWT Token。

**请求体：**

```json
{
  "username": "admin",
  "password": "admin123",
  "code": "1234",
  "uuid": "6f4a94b3d7b24f0e8f15f28f2a6b32fd",
  "rememberMe": false
}
```

| 字段       | 类型      | 必填 | 说明 |
| -------- | ------- | -- | -- |
| username | string  | 是  | 登录用户名 |
| password | string  | 是  | 登录密码 |
| code     | string  | 否  | 验证码内容；当 `captchaEnabled=true` 时必填 |
| uuid     | string  | 否  | 验证码标识；当 `captchaEnabled=true` 时必填，对应验证码接口返回的 `captchaKey` |
| rememberMe | boolean | 否  | 是否记住我，默认 `false`；`true` 时 Token 有效期为 7 天，否则为 20 分钟 |

**成功响应：**

```json
{
  "code": 200,
  "msg": "登陆成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjYyNTYyZjcxLTAxN2ItNDM5MS1hZmEyLTlkZjRkNzYxM2M3OSIsImp3dF91c2VybmFtZSI6ImFkbWluIn0.zJx...",
    "expiresIn": 1200
  },
  "timestamp": 1779624887000
}
```

| 字段      | 类型     | 必返 | 说明 |
| ------- | ------ | -- | -- |
| token   | string | 是  | JWT Token，后续请求需在请求头中携带 `Authorization: Bearer {token}` |
| expiresIn | int    | 是  | Token 过期时间，单位秒；普通登录为 `1200`，记住我登录为 `604800` |

#### 2.1.3 获取当前登录用户信息

**路径：** `GET /api/v1/auth/getInfo`\
**描述：** 获取当前登录用户详情、角色、权限以及密码策略相关信息。

**请求头：**

| 参数            | 类型     | 必填 | 说明 |
| ------------- | ------ | -- | -- |
| Authorization | string | 是  | Bearer Token |

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "user": {
      "userId": 1,
      "deptId": 103,
      "userName": "admin",
      "nickName": "系统管理员",
      "email": "admin@example.com",
      "phonenumber": "13800138000",
      "sex": "0",
      "avatar": "",
      "status": "0",
      "loginIp": "127.0.0.1",
      "loginDate": "2026-05-24 10:20:30",
      "dept": {
        "deptId": 103,
        "parentId": 100,
        "deptName": "监测中心"
      }
    },
    "roles": [
      "admin"
    ],
    "permissions": [
      "*:*:*"
    ],
    "pwdChrtype": "0",
    "isDefaultModifyPwd": false,
    "isPasswordExpired": false
  },
  "timestamp": 1779624887000
}
```

| 字段                 | 类型           | 必返 | 说明 |
| ------------------ | ------------ | -- | -- |
| user               | object       | 是  | 当前登录用户对象，实际结构遵循后端 `SysUser` 序列化结果 |
| roles              | string[]     | 是  | 当前用户角色标识集合 |
| permissions        | string[]     | 是  | 当前用户权限标识集合 |
| pwdChrtype         | string       | 是  | 密码字符规则配置值，对应系统配置 `sys.account.chrtype` |
| isDefaultModifyPwd | boolean      | 是  | 是否仍在使用初始密码且需提醒修改 |
| isPasswordExpired  | boolean      | 是  | 当前密码是否已过期 |

#### 2.1.4 获取当前用户路由

**路径：** `GET /api/v1/auth/getRouters`\
**描述：** 获取当前登录用户可访问的前端动态路由树。

**请求头：**

| 参数            | 类型     | 必填 | 说明 |
| ------------- | ------ | -- | -- |
| Authorization | string | 是  | Bearer Token |

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "name": "System",
      "path": "/system",
      "hidden": false,
      "redirect": "noRedirect",
      "component": "Layout",
      "alwaysShow": true,
      "meta": {
        "title": "系统管理",
        "icon": "system",
        "noCache": true
      },
      "children": [
        {
          "name": "User",
          "path": "user",
          "hidden": false,
          "component": "system/user/index",
          "query": "",
          "meta": {
            "title": "用户管理",
            "icon": "user",
            "noCache": true
          }
        }
      ]
    }
  ],
  "timestamp": 1779624887000
}
```

**返回字段说明：**

| 字段         | 类型        | 必返 | 说明 |
| ---------- | --------- | -- | -- |
| name       | string    | 否  | 路由名称 |
| path       | string    | 是  | 路由路径 |
| hidden     | boolean   | 否  | 是否在侧边栏隐藏 |
| redirect   | string    | 否  | 重定向地址 |
| component  | string    | 否  | 前端组件路径 |
| query      | string    | 否  | 路由参数字符串 |
| alwaysShow | boolean   | 否  | 是否始终显示根路由 |
| meta       | object    | 否  | 路由元信息，包含 `title`、`icon`、`noCache`、`link` |
| children   | object[]  | 否  | 子路由列表 |

#### 2.1.5 登出

**路径：** `POST /api/v1/system/auth/logout`\
**描述：** 当前用户退出登录并删除 Redis 中的登录态缓存。该接口由 Spring Security 统一处理，不经过业务 Controller。

**请求头：**

| 参数            | 类型     | 必填 | 说明 |
| ------------- | ------ | -- | -- |
| Authorization | string | 是  | Bearer Token |

**请求体：** 无

**成功响应：**

```json
{
  "code": 200,
  "msg": "退出成功",
  "timestamp": 1779624887000
}
```

#### 2.1.6 用户注册

**路径：** `POST /register`\
**描述：** 匿名注册新用户。仅当系统配置 `sys.account.registerUser=true` 时可用；注册对象继承登录对象，因此入参结构与登录一致。

**请求体：**

```json
{
  "username": "testuser",
  "password": "123456",
  "code": "4321",
  "uuid": "6f4a94b3d7b24f0e8f15f28f2a6b32fd"
}
```

| 字段       | 类型     | 必填 | 说明 |
| -------- | ------ | -- | -- |
| username | string | 是  | 注册用户名，长度 2-20 |
| password | string | 是  | 登录密码，长度 5-20 |
| code     | string | 否  | 验证码内容；当系统开启验证码时必填 |
| uuid     | string | 否  | 验证码标识；当系统开启验证码时必填 |

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "timestamp": 1779624887000
}
```

**失败响应示例（系统未开启注册）：**

```json
{
  "code": 500,
  "msg": "当前系统没有开启注册功能！",
  "timestamp": 1779624887000
}
```

#### 2.1.7 个人中心

说明：

- 个人中心接口统一使用 `/api/v1/profile/*`
- 该组接口用于当前登录用户查看和维护自己的资料，不用于管理员代他人操作

##### 2.1.7.1 获取个人中心信息

**路径：** `GET /api/v1/profile`\
**描述：** 获取当前登录用户的个人中心资料。

**请求头：**

| 参数            | 类型     | 必填 | 说明 |
| ------------- | ------ | -- | -- |
| Authorization | string | 是  | Bearer Token |

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "avatar": "/profile/avatar/2026/05/24/admin.png",
    "phone": "13800138000",
    "email": "admin@example.com",
    "orgId": 103,
    "orgName": "监测中心",
    "status": 0,
    "sex": "0",
    "roleGroup": "超级管理员",
    "postGroup": "总负责人"
  },
  "timestamp": 1779624887000
}
```

##### 2.1.7.2 修改个人中心资料

**路径：** `PUT /api/v1/profile`\
**描述：** 修改当前登录用户的个人资料。

**请求体：**

```json
{
  "realName": "系统管理员",
  "phone": "13800138001",
  "email": "admin_new@example.com",
  "sex": "0"
}
```

| 字段       | 类型     | 必填 | 说明 |
| -------- | ------ | -- | -- |
| realName | string | 是  | 真实姓名 |
| phone    | string | 否  | 手机号 |
| email    | string | 否  | 邮箱 |
| sex      | string | 否  | 性别，沿用系统字典值 |

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1779624887000
}
```

##### 2.1.7.3 修改个人中心密码

**路径：** `PUT /api/v1/profile/password`\
**描述：** 修改当前登录用户密码。

**请求体：**

```json
{
  "oldPassword": "old_password",
  "newPassword": "new_password"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1779624887000
}
```

##### 2.1.7.4 上传头像

**路径：** `POST /api/v1/profile/avatar`\
**描述：** 上传当前登录用户头像。

**请求类型：** `multipart/form-data`

**表单字段：**

| 字段名 | 类型   | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | 头像文件，兼容旧字段名 `avatarfile` |

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "imgUrl": "/profile/avatar/2026/05/24/avatar.png",
  "timestamp": 1779624887000
}
```

##### 2.1.7.5 常见失败响应

**未登录或Token无效：**

```json
{
  "code": 401,
  "msg": "请求访问：/api/v1/profile，认证失败，无法访问系统资源",
  "timestamp": 1779624887000
}
```

**业务失败示例：**

```json
{
  "code": 500,
  "msg": "修改用户'admin'失败，手机号码已存在",
  "timestamp": 1779624887000
}
```

### 2.2 用户管理

说明：

- 用户状态：`0` 正常，`1` 停用
- 统一分页返回结构：`rows / total / pageNum / pageSize`
- 修改用户时以路径参数 `id` 为准，不依赖请求体中的 `id`

#### 2.2.0 权限要求

| 接口 | 权限标识 |
| --- | --- |
| `GET /api/v1/users/page` | `system:user:list` |
| `GET /api/v1/users/{id}` | `system:user:query` |
| `POST /api/v1/users` | `system:user:add` |
| `PUT /api/v1/users/{id}` | `system:user:edit` |
| `DELETE /api/v1/users/{id}` | `system:user:remove` |
| `DELETE /api/v1/users/batch` | `system:user:remove` |
| `PUT /api/v1/users/{id}/password` | `system:user:resetPwd` |

#### 2.2.1 分页查询用户

**路径：** `GET /api/v1/users/page`\
**描述：** 分页查询用户列表

**请求参数：**

| 参数       | 类型     | 必填 | 说明        |
| -------- | ------ | -- | --------- |
| pageNum  | int    | 否  | 页码，默认1    |
| pageSize | int    | 否  | 每页数量，默认10 |
| username | string | 否  | 用户名模糊查询   |
| realName | string | 否  | 真实姓名模糊查询  |
| orgId    | long   | 否  | 所属组织ID    |
| status   | int    | 否  | 状态        |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "total": 100,
    "rows": [
      {
        "id": 1,
        "username": "admin",
        "realName": "系统管理员",
        "avatar": "/profile/avatar/2026/05/24/admin.png",
        "phone": "13800138000",
        "email": "admin@example.com",
        "orgId": 1,
        "orgName": "监测中心",
        "status": 0,
        "lastLoginTime": "2024-01-20 14:30:00",
        "createTime": "2024-01-01 00:00:00",
        "createBy": "system",
        "updateTime": "2024-01-10 10:00:00",
        "updateBy": "admin"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1735689213000
}
```

#### 2.2.2 获取用户详情

**路径：** `GET /api/v1/users/{id}`\
**描述：** 根据ID获取用户详情

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "avatar": "/profile/avatar/2026/05/24/admin.png",
    "phone": "13800138000",
    "email": "admin@example.com",
    "orgId": 1,
    "orgName": "监测中心",
    "status": 0,
    "createTime": "2024-01-01 00:00:00",
    "createBy": "system",
    "updateTime": "2024-01-10 10:00:00",
    "updateBy": "admin",
    "roleIds": [1, 2],
    "postIds": [1]
  },
  "timestamp": 1735689213000
}
```

#### 2.2.3 新增用户

**路径：** `POST /api/v1/users`\
**描述：** 新增用户

**请求体：**

```json
{
  "username": "testuser",
  "password": "123456",
  "realName": "测试用户",
  "phone": "13900139000",
  "email": "test@example.com",
  "orgId": 1,
  "status": 0,
  "roleIds": [2, 3],
  "remark": "测试账号"
}
```

| 字段       | 类型        | 必填 | 说明           |
| -------- | --------- | -- | ------------ |
| username | string    | 是  | 用户名，唯一       |
| password | string    | 是  | 登录密码         |
| realName | string    | 是  | 真实姓名         |
| phone    | string    | 否  | 手机号          |
| email    | string    | 否  | 邮箱           |
| orgId    | long      | 否  | 所属组织ID       |
| status   | int       | 否  | 状态，默认建议传 `0` |
| roleIds  | long[]    | 否  | 角色ID列表       |
| remark   | string    | 否  | 备注           |

**成功响应：**

```json
{
  "code": 200,
  "msg": "新增成功",
  "data": {
    "id": 2
  },
  "timestamp": 1735689213000
}
```

#### 2.2.4 修改用户

**路径：** `PUT /api/v1/users/{id}`\
**描述：** 修改用户信息

**请求体：**

```json
{
  "realName": "测试用户修改",
  "phone": "13900139001",
  "email": "test_update@example.com",
  "orgId": 2,
  "status": 0,
  "roleIds": [2],
  "remark": "修改后的备注"
}
```

说明：

- 路径参数 `id` 为唯一主键来源
- `username` 不作为修改入口字段
- 建议只传需要更新的字段

**成功响应：**

```json
{
  "code": 200,
  "msg": "修改成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.2.5 删除用户

**路径：** `DELETE /api/v1/users/{id}`\
**描述：** 删除单个用户，当前登录用户不可删除自己

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.2.6 批量删除用户

**路径：** `DELETE /api/v1/users/batch`\
**描述：** 批量删除用户，当前登录用户不可在删除列表中

**请求体：**

```json
{
  "ids": [1, 2, 3]
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.2.7 修改密码

**路径：** `PUT /api/v1/users/{id}/password`\
**描述：** 修改指定用户密码

**请求体：**

```json
{
  "oldPassword": "old_password",
  "newPassword": "new_password"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.2.8 扩展接口一览

| 方法 | 路径 | 描述 | 说明 |
| --- | --- | --- | --- |
| `PUT` | `/api/v1/users/changeStatus` | 修改用户状态 | 请求体使用系统实体字段，至少传 `userId`、`status` |
| `GET` | `/api/v1/users/authRole/{userId}` | 获取用户授权信息 | 返回 `user` 与 `roles` |
| `PUT` | `/api/v1/users/authRole` | 保存用户授权角色 | 参数为 `userId`、`roleIds` |
| `GET` | `/api/v1/users/deptTree` | 获取用户可选组织树 | 返回组织树 |
| `POST` | `/api/v1/users/export` | 导出用户数据 | 文件流接口 |
| `POST` | `/api/v1/users/importData` | 导入用户数据 | `multipart/form-data` |
| `POST` | `/api/v1/users/importTemplate` | 下载导入模板 | 文件流接口 |

#### 2.2.9 常见失败响应

**未登录或Token无效：**

```json
{
  "code": 401,
  "msg": "请求访问：/api/v1/users/page，认证失败，无法访问系统资源",
  "timestamp": 1735689213000
}
```

**无接口权限：**

```json
{
  "code": 403,
  "msg": "没有权限，请联系管理员授权",
  "timestamp": 1735689213000
}
```

**参数校验失败：**

```json
{
  "code": 400,
  "msg": "用户名不能为空",
  "timestamp": 1735689213000
}
```

**业务失败示例：**

```json
{
  "code": 500,
  "msg": "用户不存在",
  "timestamp": 1735689213000
}
```

### 2.3 角色管理

说明：

- 角色状态：`0` 正常，`1` 停用
- 数据权限：`1` 全部数据，`2` 自定义数据，`3` 本组织数据，`4` 本组织及下级，`5` 仅本人数据
- 修改角色时以路径参数 `id` 为准，不依赖请求体中的 `id`

#### 2.3.0 权限要求

| 接口 | 权限标识 |
| --- | --- |
| `GET /api/v1/roles/page` | `system:role:list` |
| `GET /api/v1/roles/{id}` | `system:role:query` |
| `POST /api/v1/roles` | `system:role:add` |
| `PUT /api/v1/roles/{id}` | `system:role:edit` |
| `DELETE /api/v1/roles/{id}` | `system:role:remove` |
| `DELETE /api/v1/roles/batch` | `system:role:remove` |
| `PUT /api/v1/roles/{id}/dataScope` | `system:role:edit` |
| `PUT /api/v1/roles/{id}/status` | `system:role:edit` |

#### 2.3.1 分页查询角色

**路径：** `GET /api/v1/roles/page`\
**描述：** 分页查询角色列表

**请求参数：**

| 参数       | 类型     | 必填 | 说明   |
| -------- | ------ | -- | ---- |
| pageNum  | int    | 否  | 页码   |
| pageSize | int    | 否  | 每页数量 |
| code     | string | 否  | 角色编码 |
| name     | string | 否  | 角色名称 |
| status   | int    | 否  | 状态   |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "total": 10,
    "rows": [
      {
        "id": 1,
        "code": "ADMIN",
        "name": "超级管理员",
        "description": "系统超级管理员",
        "dataScope": 1,
        "sortOrder": 0,
        "status": 0,
        "createTime": "2024-01-01 00:00:00",
        "createBy": "system",
        "updateTime": "2024-01-10 10:00:00",
        "updateBy": "admin"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1735689213000
}
```

#### 2.3.2 获取角色详情

**路径：** `GET /api/v1/roles/{id}`\
**描述：** 获取角色详情及菜单权限

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 1,
    "code": "ADMIN",
    "name": "超级管理员",
    "description": "系统超级管理员",
    "dataScope": 1,
    "sortOrder": 0,
    "status": 0,
    "menuIds": [1, 2, 3],
    "createTime": "2024-01-01 00:00:00",
    "createBy": "system",
    "updateTime": "2024-01-10 10:00:00",
    "updateBy": "admin"
  },
  "timestamp": 1735689213000
}
```

#### 2.3.3 新增角色

**路径：** `POST /api/v1/roles`\
**描述：** 新增角色

**请求体：**

```json
{
  "code": "TEST_ROLE",
  "name": "测试角色",
  "description": "测试角色描述",
  "dataScope": 1,
  "sortOrder": 10,
  "status": 0,
  "menuIds": [1, 2],
  "deptIds": [1, 2]
}
```

说明：

- `code`、`name`、`sortOrder` 为新增时必填
- `deptIds` 在 `dataScope = 2` 时通常需要传入

**成功响应：**

```json
{
  "code": 200,
  "msg": "新增成功",
  "data": {
    "id": 4
  },
  "timestamp": 1735689213000
}
```

#### 2.3.4 修改角色

**路径：** `PUT /api/v1/roles/{id}`\
**描述：** 修改角色

**请求体：**

```json
{
  "name": "测试角色修改",
  "description": "修改后的描述",
  "dataScope": 2,
  "sortOrder": 20,
  "status": 0,
  "menuIds": [1, 2, 3],
  "deptIds": [1, 2]
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "修改成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.3.5 删除角色

**路径：** `DELETE /api/v1/roles/{id}`\
**描述：** 删除单个角色，角色下存在用户时不可删除

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.3.6 批量删除角色

**路径：** `DELETE /api/v1/roles/batch`\
**描述：** 批量删除角色

**请求体：**

```json
{
  "ids": [2, 3, 4]
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.3.7 保存数据权限

**路径：** `PUT /api/v1/roles/{id}/dataScope`\
**描述：** 保存角色数据权限

**请求体：**

```json
{
  "dataScope": "2",
  "deptIds": [1, 2]
}
```

说明：

- 接口底层仍使用系统实体字段，`dataScope` 实际按字符串值处理
- `deptIds` 仅在自定义数据权限时使用

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.3.8 状态修改

**路径：** `PUT /api/v1/roles/{id}/status`\
**描述：** 修改角色状态

**请求体：**

```json
{
  "status": "1"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null,
  "timestamp": 1735689213000
}
```

#### 2.3.9 扩展接口一览

| 方法 | 路径 | 描述 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/roles/optionselect` | 获取角色下拉列表 | 返回原系统角色实体结构，如 `roleId / roleName / roleKey / status` |
| `GET` | `/api/v1/roles/{id}/deptTree` | 获取角色组织树及已选节点 | 返回 `checkedKeys` 与 `depts` |
| `POST` | `/api/v1/roles/export` | 导出角色数据 | 文件流接口 |
| `GET` | `/api/v1/roles/authUser/allocatedList` | 查询已分配用户列表 | 使用系统实体字段过滤 |
| `GET` | `/api/v1/roles/authUser/unallocatedList` | 查询未分配用户列表 | 使用系统实体字段过滤 |
| `PUT` | `/api/v1/roles/authUser/cancel` | 取消单个用户授权 | 请求体为 `userId / roleId` |
| `PUT` | `/api/v1/roles/authUser/cancelAll` | 批量取消用户授权 | 参数为 `roleId / userIds` |
| `PUT` | `/api/v1/roles/authUser/selectAll` | 批量选择用户授权 | 参数为 `roleId / userIds` |

### 2.4 菜单管理

说明：

- `code` 字段对应后端 `routeName`，表示菜单编码/路由名称
- `perms` 表示权限标识
- 菜单类型：`0` 目录，`1` 菜单，`2` 按钮
- 显示状态：`0` 显示，`1` 隐藏
- 菜单状态：`0` 正常，`1` 停用

#### 2.4.0 权限要求

| 接口 | 权限标识 |
| --- | --- |
| `GET /api/v1/menus/tree` | `system:menu:list` |
| `GET /api/v1/menus/current` | 当前登录用户菜单，无额外菜单管理权限要求 |
| `GET /api/v1/menus/{id}` | `system:menu:query` |
| `POST /api/v1/menus` | `system:menu:add` |
| `PUT /api/v1/menus/{id}` | `system:menu:edit` |
| `DELETE /api/v1/menus/{id}` | `system:menu:remove` |

#### 2.4.1 获取菜单列表（树形）

**路径：** `GET /api/v1/menus/tree`\
**描述：** 获取树形菜单结构

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "系统管理",
      "code": "system",
      "path": "/system",
      "component": null,
      "icon": "setting",
      "type": 0,
      "visible": 0,
      "isCache": 1,
      "sortOrder": 0,
      "status": 0,
      "perms": null,
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "name": "用户管理",
          "code": "user",
          "path": "/system/users",
          "component": "system/Users",
          "icon": "user",
          "type": 1,
          "visible": 0,
          "isCache": 0,
          "sortOrder": 0,
          "status": 0,
          "perms": "system:user:list",
          "children": []
        }
      ]
    }
  ],
  "timestamp": 1735689213000
}
```

#### 2.4.2 获取当前用户菜单

**路径：** `GET /api/v1/menus/current`\
**描述：** 获取当前登录用户有权限的菜单

**成功响应：** 同 `GET /api/v1/menus/tree`

#### 2.4.3 获取菜单详情

**路径：** `GET /api/v1/menus/{id}`\
**描述：** 获取菜单详情

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 1,
    "parentId": 0,
    "name": "系统管理",
    "code": "system",
    "path": "/system",
    "component": null,
    "icon": "setting",
    "type": 0,
    "visible": 0,
    "perms": null,
    "isCache": 1,
    "sortOrder": 0,
    "status": 0,
    "createTime": "2024-01-01 00:00:00"
  },
  "timestamp": 1735689213000
}
```

#### 2.4.4 新增菜单

**路径：** `POST /api/v1/menus`\
**描述：** 新增菜单

**请求体：**

```json
{
  "parentId": 1,
  "name": "测试菜单",
  "code": "testMenu",
  "path": "/system/test",
  "component": "system/Test",
  "icon": "test",
  "type": 1,
  "visible": 0,
  "perms": "system:test:view",
  "isCache": 0,
  "sortOrder": 10,
  "status": 0
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| parentId | long | 是 | 父级菜单ID |
| name | string | 是 | 菜单名称 |
| code | string | 否 | 菜单编码，对应 `routeName` |
| path | string | 否 | 路由地址，目录/菜单场景建议传 |
| component | string | 否 | 组件路径，菜单场景建议传 |
| icon | string | 否 | 图标 |
| type | int | 是 | `0目录 / 1菜单 / 2按钮` |
| visible | int | 否 | `0显示 / 1隐藏` |
| perms | string | 否 | 权限标识，按钮场景通常必传 |
| isCache | int | 否 | `0缓存 / 1不缓存` |
| sortOrder | int | 是 | 排序 |
| status | int | 否 | `0正常 / 1停用` |

**成功响应：**

```json
{
  "code": 200,
  "msg": "新增成功",
  "data": {
    "id": 100
  },
  "timestamp": 1735689213000
}
```

#### 2.4.5 修改菜单

**路径：** `PUT /api/v1/menus/{id}`\
**描述：** 修改菜单

**请求体：**

```json
{
  "parentId": 1,
  "name": "测试菜单修改",
  "code": "testMenu",
  "path": "/system/test/update",
  "component": "system/TestUpdate",
  "icon": "test-update",
  "type": 1,
  "visible": 0,
  "perms": "system:test:view",
  "isCache": 0,
  "sortOrder": 20,
  "status": 0
}
```

#### 2.4.6 删除菜单

**路径：** `DELETE /api/v1/menus/{id}`\
**描述：** 删除菜单，存在子菜单或已分配角色时不可删除

#### 2.4.7 扩展接口一览

| 方法 | 路径 | 描述 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/menus/treeselect` | 获取菜单下拉树 | 返回 `TreeSelect` 结构：`id / label / children` |
| `GET` | `/api/v1/menus/roleMenuTreeselect/{roleId}` | 获取角色菜单树 | 返回 `checkedKeys` 与 `menus` |
| `PUT` | `/api/v1/menus/updateSort` | 批量保存菜单排序 | 请求体包含 `menuIds`、`orderNums`，逗号分隔 |
| `DELETE` | `/api/v1/menus/batch` | 批量删除菜单 | 请求体为 `Long[]` |

### 2.5 组织管理

说明：

- 组织状态：`0` 正常，`1` 停用
- 当前组织管理基于 `sys_dept` 扩展字段实现
- 组织详情中 `parentIds` 采用 `/0/1/` 格式

#### 2.5.0 权限要求

说明：组织接口路径为 `/api/v1/organizations/*`，但权限标识沿用部门模块的 `system:dept:*`。

| 接口 | 权限标识 |
| --- | --- |
| `GET /api/v1/organizations/tree` | `system:dept:list` |
| `GET /api/v1/organizations/page` | `system:dept:list` |
| `GET /api/v1/organizations/{id}` | `system:dept:query` |
| `POST /api/v1/organizations` | `system:dept:add` |
| `PUT /api/v1/organizations/{id}` | `system:dept:edit` |
| `DELETE /api/v1/organizations/{id}` | `system:dept:remove` |

#### 2.5.1 获取组织列表（树形）

**路径：** `GET /api/v1/organizations/tree`\
**描述：** 获取树形组织结构

**请求参数：**

| 参数   | 类型     | 必填 | 说明   |
| ---- | ------ | -- | ---- |
| code | string | 否  | 组织编码 |
| name | string | 否  | 组织名称 |
| status | int | 否 | 状态 |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 1,
      "code": "ROOT",
      "name": "系统管理员",
      "parentId": 0,
      "parentIds": "/0/",
      "level": 1,
      "leader": "admin",
      "phone": "13800138000",
      "email": "admin@example.com",
      "region": "成都",
      "address": "成华区龙潭寺",
      "status": 0,
      "sortOrder": 0,
      "children": [
        {
          "id": 2,
          "code": "DEPT001",
          "name": "监测中心",
          "parentId": 1,
          "parentIds": "/0/1/",
          "level": 2,
          "status": 0,
          "sortOrder": 0,
          "children": []
        }
      ]
    }
  ],
  "timestamp": 1735689213000
}
```

#### 2.5.2 分页查询组织

**路径：** `GET /api/v1/organizations/page`\
**描述：** 分页查询组织

**请求参数：**

| 参数       | 类型     | 必填 | 说明   |
| -------- | ------ | -- | ---- |
| pageNum  | int    | 否  | 页码   |
| pageSize | int    | 否  | 每页数量 |
| code     | string | 否  | 组织编码 |
| name     | string | 否  | 组织名称 |
| status   | int    | 否  | 状态   |

#### 2.5.3 获取组织详情

**路径：** `GET /api/v1/organizations/{id}`\
**描述：** 获取组织详情

#### 2.5.4 新增组织

**路径：** `POST /api/v1/organizations`\
**描述：** 新增组织

**请求体：**

```json
{
  "code": "DEPT003",
  "name": "测试部门",
  "parentId": 1,
  "leader": "张三",
  "phone": "13800138001",
  "email": "zhangsan@example.com",
  "region": "成都",
  "address": "测试地址",
  "sortOrder": 10,
  "status": 0
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| code | string | 是 | 组织编码 |
| name | string | 是 | 组织名称 |
| parentId | long | 是 | 父组织ID，根节点传 `0` |
| leader | string | 否 | 负责人 |
| phone | string | 否 | 联系电话 |
| email | string | 否 | 邮箱 |
| region | string | 否 | 区域 |
| address | string | 否 | 地址 |
| sortOrder | int | 是 | 排序 |
| status | int | 否 | 状态，建议传 `0` |

#### 2.5.5 修改组织

**路径：** `PUT /api/v1/organizations/{id}`\
**描述：** 修改组织

#### 2.5.6 删除组织

**路径：** `DELETE /api/v1/organizations/{id}`\
**描述：** 删除组织，存在下级组织或组织下存在用户时不可删除

### 2.6 系统日志模块

#### 2.6.1 分页查询操作日志

**路径：** `GET /api/v1/logs/operations/page`\
**描述：** 按条件分页查询接口调用操作日志

**请求头：**

| 参数            | 类型     | 必填 | 说明           |
| ------------- | ------ | -- | ------------ |
| Authorization | string | 是  | Bearer Token |

**请求参数：**

| 参数         | 类型     | 必填 | 说明                            |
| ---------- | ------ | -- | ----------------------------- |
| pageNum    | int    | 否  | 页码，默认 1                       |
| pageSize   | int    | 否  | 每页数量，默认 10                    |
| username   | string | 否  | 操作用户名                         |
| title      | string | 否  | 业务标题关键字                       |
| apiPath    | string | 否  | 接口路径关键字                       |
| execStatus | string | 否  | 执行状态，`SUCCESS` / `FAIL`       |
| startTime  | string | 否  | 开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| endTime    | string | 否  | 结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "rows": [
      {
        "id": 11,
        "eventId": 921779534004781100,
        "logType": "OPERATION",
        "title": "用户管理",
        "businessType": "INSERT",
        "apiPath": "/api/v1/system/users",
        "requestMethod": "POST",
        "controllerMethod": "com.zwei.web.controller.system.SysUserController.add",
        "username": "admin",
        "userId": 1,
        "clientIp": "127.0.0.1",
        "httpStatus": 200,
        "execStatus": "SUCCESS",
        "costTimeMs": 32,
        "occurredAt": "2026-05-23 18:30:15.123"
      }
    ]
  },
  "timestamp": 1779534175000
}
```

#### 2.6.2 分页查询认证日志

**路径：** `GET /api/v1/logs/auth/page`\
**描述：** 分页查询登录、登出、未认证、认证失败等身份认证日志

**请求头：**

| 参数            | 类型     | 必填 | 说明           |
| ------------- | ------ | -- | ------------ |
| Authorization | string | 是  | Bearer Token |

**请求参数：**

| 参数            | 类型     | 必填 | 说明                                                            |
| ------------- | ------ | -- | ------------------------------------------------------------- |
| pageNum       | int    | 否  | 页码，默认 1                                                       |
| pageSize      | int    | 否  | 每页数量，默认 10                                                    |
| username      | string | 否  | 用户名                                                           |
| authEventType | string | 否  | 认证事件类型，如 `LOGIN_SUCCESS`、`LOGIN_FAIL`、`LOGOUT`、`UNAUTHORIZED` |
| resultStatus  | string | 否  | 结果状态，`SUCCESS` / `FAIL`                                       |
| startTime     | string | 否  | 开始时间，格式 `yyyy-MM-dd HH:mm:ss`                                 |
| endTime       | string | 否  | 结束时间，格式 `yyyy-MM-dd HH:mm:ss`                                 |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 2,
    "rows": [
      {
        "id": 74,
        "eventId": 921779534004781002,
        "logType": "AUTH",
        "username": "admin",
        "authEventType": "LOGIN_SUCCESS",
        "authChannel": "PASSWORD",
        "requestUri": "/api/v1/auth/login",
        "requestMethod": "POST",
        "clientIp": "127.0.0.1",
        "resultStatus": "SUCCESS",
        "failureMessage": "登录成功",
        "occurredAt": "2026-05-23 18:47:03.083"
      }
    ]
  },
  "timestamp": 1779533272847
}
```

#### 2.6.3 分页查询运行日志

**路径：** `GET /api/v1/logs/runtime/page`\
**描述：** 分页查询系统运行期控制台日志

**请求头：**

| 参数            | 类型     | 必填 | 说明           |
| ------------- | ------ | -- | ------------ |
| Authorization | string | 是  | Bearer Token |

**请求参数：**

| 参数         | 类型     | 必填 | 说明                            |
| ---------- | ------ | -- | ----------------------------- |
| pageNum    | int    | 否  | 页码，默认 1                       |
| pageSize   | int    | 否  | 每页数量，默认 10                    |
| level      | string | 否  | 日志级别，如 `INFO`、`WARN`、`ERROR`  |
| loggerName | string | 否  | Logger 名称关键字                  |
| keyword    | string | 否  | 日志内容关键字                       |
| startTime  | string | 否  | 开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| endTime    | string | 否  | 结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "pageNum": 1,
    "pageSize": 5,
    "total": 25,
    "rows": [
      {
        "id": 25,
        "eventId": 921779534004781012,
        "logType": "RUNTIME",
        "level": "WARN",
        "loggerName": "net.dreamlu.mica.net.server.task.ServerHeartbeatTask",
        "threadName": "DefaultTimerTaskService",
        "message": "Mica-Mqtt-Server, 检查心跳, 共0个连接...",
        "messageDigest": "Mica-Mqtt-Server, 检查心跳, 共0个连接...",
        "sourceApp": "zwei-admin",
        "occurredAt": "2026-05-23 19:02:11.146"
      }
    ]
  },
  "timestamp": 1779533268655
}
```

#### 2.6.4 SSE 实时订阅日志流

**路径：** `GET /api/v1/logs/stream`\
**描述：** 通过 SSE 订阅实时日志流，支持按日志类型筛选，并支持 `Last-Event-ID` 与 checkpoint 断线恢复

**请求头：**

| 参数            | 类型     | 必填 | 说明                       |
| ------------- | ------ | -- | ------------------------ |
| Authorization | string | 是  | Bearer Token             |
| Accept        | string | 是  | 固定为 `text/event-stream`  |
| Last-Event-ID | string | 否  | 上次收到的事件 ID，断线重连时用于补发历史日志 |

**请求参数：**

| 参数            | 类型     | 必填 | 说明                                           |
| ------------- | ------ | -- | -------------------------------------------- |
| types         | string | 否  | 订阅日志类型，多个用逗号分隔，支持 `operation,auth,runtime`   |
| subscriberKey | string | 否  | 订阅者唯一标识，用于 checkpoint 持久化恢复；不传则服务端按用户和类型自动生成 |

**SSE 事件说明：**

| 事件名       | 说明                                   |
| --------- | ------------------------------------ |
| ready     | 连接建立成功，`data.resumeEventId` 表示本次恢复起点 |
| operation | 实时操作日志事件                             |
| auth      | 实时认证日志事件                             |
| runtime   | 实时运行日志事件                             |
| replay    | 断线恢复补发的历史日志事件                        |

#### 2.6.5 获取当前 SSE 活跃连接数

**路径：** `GET /api/v1/logs/stream/connections`\
**描述：** 查询当前日志流 SSE 活跃连接数

### 2.7 系统监控模块接口

> **模块说明：** 以下接口由 `zwei-monitor` 模块统一对外暴露，整合系统级监控能力并新增 MQTT 服务器实时状态查询。

#### 2.7.1 获取系统监控总览

**路径：** `GET /api/v1/monitor/overview`\
**描述：** 聚合服务器健康、Redis 状态、在线用户数、MQTT 核心指标和系统运行时长

**权限标识：** `monitor:overview:list`

#### 2.7.2 MQTT 服务器统计

**路径：** `GET /api/v1/monitor/mqtt/stats`\
**描述：** 获取 MQTT 服务器全量运行统计

**权限标识：** `monitor:mqtt:list`

#### 2.7.3 MQTT 监听器列表

**路径：** `GET /api/v1/monitor/mqtt/listeners`\
**描述：** 获取所有 MQTT 监听器配置（TCP/WS/SSL/HTTP）

**权限标识：** `monitor:mqtt:list`

#### 2.7.4 MQTT 运行配置

**路径：** `GET /api/v1/monitor/mqtt/config`\
**描述：** 获取 MQTT 服务器运行配置参数摘要

**权限标识：** `monitor:mqtt:list`

#### 2.7.5 分页查询连接客户端

**路径：** `GET /api/v1/monitor/mqtt/clients/page`\
**描述：** 分页查询当前连接的 MQTT 客户端列表

**权限标识：** `monitor:mqtt:list`

#### 2.7.6 获取客户端详情（含订阅）

**路径：** `GET /api/v1/monitor/mqtt/clients/{clientId}`\
**描述：** 获取指定客户端的连接详情与主题订阅列表

**权限标识：** `monitor:mqtt:list`

#### 2.7.7 踢出/批量踢出客户端

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `DELETE` | `/api/v1/monitor/mqtt/clients/{clientId}` | 踢出单个客户端 | `monitor:mqtt:kick` |
| `DELETE` | `/api/v1/monitor/mqtt/clients/batch` | 批量踢出 | `monitor:mqtt:kick` |

#### 2.7.8 MQTT 数据日志查询

**路径：** `GET /api/v1/monitor/mqtt/messages/page`\
**描述：** 分页查询最近通过平台转发的设备监测消息日志（按接收时间倒序）\
**权限标识：** `monitor:mqtt:list`

**请求参数：**

| 参数       | 类型     | 必填 | 说明                     |
|----------|--------|----|------------------------|
| page     | int    | 否  | 页码，默认 1                |
| pageSize | int    | 否  | 每页大小，默认 20             |
| clientId | string | 否  | 按 clientId 模糊过滤，不传则不过滤 |
| topic    | string | 否  | 按 topic 模糊过滤，不传则不过滤    |

#### 2.7.9 MQTT 异常报文日志查询

**路径：** `GET /api/v1/monitor/mqtt/exceptions/page`\
**描述：** 分页查询已认证但解析/报送失败的异常报文（按接收时间倒序）\
**权限标识：** `monitor:mqtt:list`

**请求参数：**

| 参数           | 类型     | 必填 | 说明                     |
|--------------|--------|----|------------------------|
| page         | int    | 否  | 页码，默认 1                |
| pageSize     | int    | 否  | 每页大小，默认 20             |
| clientId     | string | 否  | 按 clientId 模糊过滤，不传则不过滤 |
| topic        | string | 否  | 按 topic 模糊过滤            |
| rejectReason | string | 否  | 按拒绝原因过滤                 |
| startTime    | string | 否  | 开始时间 `yyyy-MM-dd HH:mm:ss` |
| endTime      | string | 否  | 结束时间 `yyyy-MM-dd HH:mm:ss` |

#### 2.7.10 MQTT 异常报文导出

**路径：** `POST /api/v1/monitor/mqtt/exceptions/export`\
**描述：** 导出异常报文为 Excel 文件（上限 10000 条）\
**权限标识：** `monitor:mqtt:list`

#### 2.7.11 MQTT 异常报文保留期配置

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/monitor/mqtt/exceptions/retention-config` | 查询保留期配置（enabled/retentionDays） | `monitor:mqtt:list` |
| `PUT` | `/api/v1/monitor/mqtt/exceptions/retention-config` | 更新保留期配置 | `monitor:mqtt:list` |

### 2.8 大屏仪表盘统计

> **模块说明：** 以下接口由 `zwei-monitor` 模块提供，为前端全息看板聚合多维度运维指标数据。

#### 2.8.1 大屏一体化聚合

**路径：** `GET /api/v1/monitor/dashboard/full`\
**描述：** 一次请求返回 overview + onlineRate + activeRate + trend + distribution + healthScore\
**权限标识：** `monitor:overview:list`

**请求参数：**

| 参数            | 类型  | 必填 | 说明               |
|---------------|-----|----|------------------|
| windowMinutes | int | 否  | 活跃率时间窗口（分钟），默认60 |

#### 2.8.2 系统健康度评分

**路径：** `GET /api/v1/monitor/dashboard/health-score`\
**描述：** 综合资料完善率(20%) + 设备在线率(15%) + 设备正常率(15%) + 告警响应率(20%占位) + 边坡稳定率(30%占位)\
**权限标识：** `monitor:overview:list`

#### 2.8.3 单项指标接口

| 路径                                                                  | 描述                               |
|---------------------------------------------------------------------|----------------------------------|
| `GET /api/v1/monitor/dashboard/overview`                            | 资源总览（设备/传感器/隐患点/监测类型/视频设备 总数与分布） |
| `GET /api/v1/monitor/dashboard/device-online-rate`                  | 设备在线率（按监测类型分组）                   |
| `GET /api/v1/monitor/dashboard/device-active-rate?windowMinutes=60` | 设备活跃率（窗口内有数据上报的设备占比）             |
| `GET /api/v1/monitor/dashboard/sensor-online-rate`                  | 传感器在线率（所属设备在线=传感器在线）             |
| `GET /api/v1/monitor/dashboard/sensor-active-rate?windowMinutes=60` | 传感器活跃率（窗口内有数据上报的传感器占比）           |
| `GET /api/v1/monitor/dashboard/hazard-point-trend?months=12`        | 隐患点月度增长趋势                        |
| `GET /api/v1/monitor/dashboard/sensor-distribution`                 | 传感器按监测类型分布                       |

### 2.9 通知公告模块

> **模块说明：** 通知公告模块已从 `zwei-system` 隔离至 `notice/` 子包，支持 SSE 实时推送和多通道扩展架构。

#### 2.9.1 通知公告 CRUD

| 方法     | 路径                            | 描述       | 权限                     |
|--------|-------------------------------|----------|------------------------|
| GET    | `/api/v1/system/notice/list`  | 分页查询通知列表 | `system:notice:list`   |
| GET    | `/api/v1/system/notice/{id}`  | 获取通知详情   | —                      |
| POST   | `/api/v1/system/notice`       | 新增通知     | `system:notice:add`    |
| PUT    | `/api/v1/system/notice`       | 修改通知     | `system:notice:edit`   |
| DELETE | `/api/v1/system/notice/{ids}` | 删除通知     | `system:notice:remove` |

#### 2.9.2 用户已读标记

| 方法   | 路径                                                   | 描述                                        |
|------|------------------------------------------------------|-------------------------------------------|
| GET  | `/api/v1/system/notice/listTop`                      | 首页顶部公告（最多5条，含 `isRead` 标记与 `unreadCount`） |
| POST | `/api/v1/system/notice/markRead?noticeId={id}`       | 标记单条已读                                    |
| POST | `/api/v1/system/notice/markReadAll?ids={ids}`        | 批量标记已读（ids 逗号分隔）                          |
| GET  | `/api/v1/system/notice/readUsers/list?noticeId={id}` | 已读人员列表                                    |

#### 2.9.3 SSE 实时推送

**路径：** `GET /api/v1/system/notice/stream`\
**描述：** SSE 端点，前端建立连接后实时接收新通知推送。

#### 2.9.4 多通道扩展架构（Phase 3 设计预留）

| 通道     | 状态  | 说明             |
|--------|-----|----------------|
| in_app | 已实现 | 应用内 SSE 实时推送   |
| email  | 预留  | Spring Mail 对接 |
| sms    | 预留  | 第三方 SMS SDK 对接 |

***

## 3. 基础管理模块接口

### 3.1 隐患点管理

#### 3.1.1 分页查询隐患点

**路径：** `GET /api/v1/hazard-points/page`\
**描述：** 分页查询隐患点列表

**请求参数：**

| 参数       | 类型     | 必填 | 说明    |
| -------- | ------ | -- | ----- |
| pageNum  | int    | 否  | 页码    |
| pageSize | int    | 否  | 每页数量  |
| code     | string | 否  | 隐患点编号 |
| name     | string | 否  | 隐患点名称 |
| groupId  | long   | 否  | 分组ID  |
| status   | int    | 否  | 状态    |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "total": 100,
    "rows": [
      {
        "id": 1,
        "code": "HP001",
        "name": "隐患点A",
        "groupId": 1,
        "groupName": "第一分组",
        "longitude": 104.156789,
        "latitude": 30.678901,
        "strike": 45.00,
        "description": "隐患描述",
        "status": 1,
        "statusName": "监测中",
        "deviceCount": 2,
        "createTime": "2024-01-10 10:00:00",
        "createBy": "admin"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1735689213000
}
```

#### 3.1.2 获取隐患点详情

**路径：** `GET /api/v1/hazard-points/{id}`\
**描述：** 获取隐患点详情

#### 3.1.3 新增隐患点

**路径：** `POST /api/v1/hazard-points`\
**描述：** 新增隐患点

**请求体：**

```json
{
  "code": "HP001",
  "name": "隐患点A",
  "groupId": 1,
  "longitude": 104.156789,
  "latitude": 30.678901,
  "strike": 45.00,
  "description": "隐患描述"
}
```

#### 3.1.4 修改隐患点

**路径：** `PUT /api/v1/hazard-points/{id}`\
**描述：** 修改隐患点

#### 3.1.5 删除/批量删除隐患点

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `DELETE` | `/api/v1/hazard-points/{id}` | 删除单个隐患点 |
| `DELETE` | `/api/v1/hazard-points/batch` | 批量删除，请求体 `{ ids: [...] }` |

#### 3.1.6 停测/恢复隐患点

**路径：** `PUT /api/v1/hazard-points/{id}/pause`\
**请求体：** `{ "pause": true }`

#### 3.1.7 完结隐患点

**路径：** `PUT /api/v1/hazard-points/{id}/complete`

#### 3.1.8 批量操作

**路径：** `PUT /api/v1/hazard-points/batch/operate`\
**描述：** 批量停测/恢复/完结。请求体：`{ ids: [...], operation: "pause|resume|complete" }`

### 3.2 隐患点分组管理

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/hazard-point-groups` | 获取分组列表 |
| `GET` | `/api/v1/hazard-point-groups/{id}` | 获取分组详情 |
| `POST` | `/api/v1/hazard-point-groups` | 新增分组 |
| `PUT` | `/api/v1/hazard-point-groups/{id}` | 修改分组 |
| `DELETE` | `/api/v1/hazard-point-groups/{id}` | 删除分组（有隐患点时不允许） |

### 3.3 监测大类管理

#### 3.3.1 获取监测大类列表

**路径：** `GET /api/v1/monitor-categories`\
**描述：** 获取所有监测大类列表（不分页）

### 3.4 监测类型管理

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/monitor-types/page` | 分页查询监测类型 |
| `GET` | `/api/v1/monitor-types` | 获取所有监测类型列表 |
| `GET` | `/api/v1/monitor-types/{id}` | 获取监测类型详情及监测内容 |
| `POST` | `/api/v1/monitor-types` | 新增监测类型 |
| `PUT` | `/api/v1/monitor-types/{id}` | 修改监测类型 |
| `DELETE` | `/api/v1/monitor-types/{id}` | 删除监测类型 |

### 3.5 监测内容管理

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/monitor-contents` | 获取监测内容列表（可选 `monitorTypeId`） |
| `POST` | `/api/v1/monitor-contents` | 新增监测内容 |
| `PUT` | `/api/v1/monitor-contents/{id}` | 修改监测内容 |
| `DELETE` | `/api/v1/monitor-contents/{id}` | 删除监测内容 |

### 3.6 设备管理

说明：

- 设备接口统一使用 `/api/v1/devices/*`
- 设备账号状态：`1-有效`，`2-禁用`
- 设备注册来源：`MANUAL / API / IMPORT`
- 设备接入协议：`MQTT / HTTP / COAP`
- 设备类型：`0-单参数`，`1-多参数`，`2-本地组网`
- 网络类型：`0-蜂窝`，`1-NB-Iot`

#### 3.6.0 权限要求

| 接口 | 权限标识 |
| --- | --- |
| `GET /api/v1/devices/page` | `basic:device:list` |
| `GET /api/v1/devices` | `basic:device:list` |
| `GET /api/v1/devices/{id}` | `basic:device:query` |
| `POST /api/v1/devices` | `basic:device:add` |
| `PUT /api/v1/devices/{id}` | `basic:device:edit` |
| `DELETE /api/v1/devices/{id}` | `basic:device:remove` |
| `POST /api/v1/devices/{id}/copy` | `basic:device:add` |
| `GET /api/v1/devices/{id}/sensors` | `basic:device:query` |
| `GET /api/v1/devices/{id}/auth-account` | `basic:device:auth:view` |
| `POST /api/v1/devices/{id}/auth-password/reset` | `basic:device:auth:reset` |
| `PUT /api/v1/devices/{id}/auth-status` | `basic:device:auth:status` |

#### 3.6.1-3.6.9 设备 CRUD + 账号管理

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/devices/page` | 分页查询设备（code/name/sn/status/runStatus 筛选） |
| `GET` | `/api/v1/devices/{id}` | 获取设备详情（含传感器列表） |
| `POST` | `/api/v1/devices` | 新增设备，自动生成6位用户名和8位密码 |
| `PUT` | `/api/v1/devices/{id}` | 修改设备基础信息 |
| `DELETE` | `/api/v1/devices/{id}` | 逻辑删除设备 |
| `POST` | `/api/v1/devices/{id}/copy` | 复制设备及传感器配置 |
| `GET` | `/api/v1/devices/{id}/auth-account` | 查询设备接入账号信息 |
| `POST` | `/api/v1/devices/{id}/auth-password/reset` | 重置设备接入密码 |
| `PUT` | `/api/v1/devices/{id}/auth-status` | 启用/禁用设备账号 |

#### 3.6.10 设备注册中心

**路径：** `POST /api/v1/device-registry/register`\
**描述：** 匿名设备注册接口，按 `requestId` 幂等处理，按 `sn` 去重

**鉴权：** 匿名接口

**请求体：**

```json
{
  "requestId": "REQ-20260528-0001",
  "registerCode": "ABCDEF123456",
  "vendorName": "中地厂商",
  "sn": "SW-202605-0001",
  "deviceName": "GNSS位移监测仪",
  "deviceType": "0",
  "network": "0",
  "protocol": "0",
  "monitorTypes": [
    { "type": "L1_LF", "sid": "1" }
  ]
}
```

### 3.7 传感器管理

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/devices/{deviceId}/sensors` | 获取设备下的传感器列表 |
| `GET` | `/api/v1/sensors/{id}` | 获取单个传感器详情 |
| `POST` | `/api/v1/devices/{deviceId}/sensors` | 为设备新增传感器 |
| `PUT` | `/api/v1/sensors/{id}` | 修改传感器 |
| `DELETE` | `/api/v1/sensors/{id}` | 删除传感器 |
| `DELETE` | `/api/v1/sensors/{sensorId}/attributes/{attrId}` | 删除传感器单个属性 |

### 3.8 视频设备管理

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/video-devices/page` | 分页查询视频设备 |
| `GET` | `/api/v1/video-devices/{id}` | 获取视频设备详情 |
| `POST` | `/api/v1/video-devices` | 新增视频设备 |
| `PUT` | `/api/v1/video-devices/{id}` | 修改视频设备 |
| `DELETE` | `/api/v1/video-devices/{id}` | 删除视频设备 |

***

## 4. 设备绑定接口

### 4.1 设备隐患点绑定

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/hazard-points/{hpId}/bound-devices` | 获取隐患点已绑定的设备列表 |
| `GET` | `/api/v1/hazard-points/{hpId}/unbound-devices` | 获取未绑定设备列表（keyword 可选模糊查询） |
| `POST` | `/api/v1/hazard-points/{hpId}/bind-devices` | 绑定设备到隐患点 |
| `DELETE` | `/api/v1/hazard-points/{hpId}/unbind-devices` | 解绑设备 |

### 4.2 视频设备隐患点绑定

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/hazard-points/{hpId}/bound-video-devices` | 获取已绑定的视频设备 |
| `POST` | `/api/v1/hazard-points/{hpId}/bind-video-devices` | 绑定视频设备到隐患点 |
| `DELETE` | `/api/v1/hazard-points/{hpId}/unbind-video-devices` | 解绑视频设备 |

***

## 5. 告警中心模块接口

> **实现状态（2026-07）：** zwei-iot-alarm 模块已实现。接口前缀统一为 `/api/v1/alarm/*`。
> 旧 `/api/v1/hazard-points/{hpId}/alarm-criteria` 和 `/api/v1/alarm-criteria/` 路径已废弃。

### 5.0 权限要求

| 接口                                         | 权限标识                             |
|--------------------------------------------|----------------------------------|
| `GET /api/v1/alarm/criteria/list`          | `iot:alarm-criteria:list`        |
| `GET /api/v1/alarm/criteria/{id}`          | `iot:alarm-criteria:list`        |
| `POST /api/v1/alarm/criteria`              | `iot:alarm-criteria:create`      |
| `PUT /api/v1/alarm/criteria/{id}`          | `iot:alarm-criteria:update`      |
| `DELETE /api/v1/alarm/criteria/{id}`       | `iot:alarm-criteria:delete`      |
| `PUT /api/v1/alarm/criteria/{id}/toggle`   | `iot:alarm-criteria:toggle`      |
| `GET /api/v1/alarm/records/pending`        | `iot:alarm-record:list`          |
| `GET /api/v1/alarm/records/history`        | `iot:alarm-record:list`          |
| `GET /api/v1/alarm/records/{id}`           | `iot:alarm-record:list`          |
| `PUT /api/v1/alarm/records/{id}/dispose`   | `iot:alarm-record:dispose`       |
| `POST /api/v1/alarm/records/batch`         | `iot:alarm-record:batch`         |
| `GET /api/v1/alarm/strategies/list`        | `iot:alarm-strategy:list`        |
| `GET /api/v1/alarm/strategies/{id}`        | `iot:alarm-strategy:list`        |
| `POST /api/v1/alarm/strategies`            | `iot:alarm-strategy:create`      |
| `PUT /api/v1/alarm/strategies/{id}`        | `iot:alarm-strategy:update`      |
| `DELETE /api/v1/alarm/strategies/{id}`     | `iot:alarm-strategy:delete`      |
| `PUT /api/v1/alarm/strategies/{id}/toggle` | `iot:alarm-strategy:toggle`      |
| `GET /api/v1/alarm/dispatch/list`          | `alarm:dispatch:list`            |
| `GET /api/v1/alarm/dispatch/{id}`          | `alarm:dispatch:list`            |
| `POST /api/v1/alarm/dispatch`              | `alarm:dispatch:add`             |
| `PUT /api/v1/alarm/dispatch/{id}`          | `alarm:dispatch:edit`            |
| `DELETE /api/v1/alarm/dispatch/{id}`       | `alarm:dispatch:remove`          |
| `PUT /api/v1/alarm/dispatch/{id}/enabled`  | `alarm:dispatch:edit`            |
| `GET /api/v1/alarm/dispatch/recipient-options` | `alarm:dispatch:list`        |
| `GET /api/v1/alarm/notifications/recent`   | `alarm:notification:list`        |
| `GET /api/v1/alarm/notifications/unread-count` | `alarm:notification:list`    |
| `POST /api/v1/alarm/notifications/{id}/read` | `alarm:notification:read`      |
| `POST /api/v1/alarm/notifications/read-all`  | `alarm:notification:read`      |
| `GET /api/v1/alarm/stream`                 | `iot:alarm-record:list` (SSE 订阅) |

### 5.1 告警判据管理

#### 5.1.1 分页查询判据列表

**路径：** `GET /api/v1/alarm/criteria/list`

**请求参数：**

| 参数               | 类型     | 必填 | 说明        |
|------------------|--------|----|-----------|
| pageNum          | int    | 否  | 页码，默认1    |
| pageSize         | int    | 否  | 每页数量，默认10 |
| name             | string | 否  | 判据名称模糊查询  |
| monitorTypeId    | long   | 否  | 监测类型ID    |
| monitorContentId | long   | 否  | 监测内容ID    |
| hazardPointId    | long   | 否  | 隐患点ID     |
| isEnabled        | int    | 否  | 启用状态      |

#### 5.1.2-5.1.7 判据 CRUD

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/alarm/criteria/{id}` | 获取判据详情 |
| `POST` | `/api/v1/alarm/criteria` | 新增判据 |
| `PUT` | `/api/v1/alarm/criteria/{id}` | 修改判据（version 自动 +1） |
| `DELETE` | `/api/v1/alarm/criteria/{id}` | 删除判据（软删除） |
| `PUT` | `/api/v1/alarm/criteria/{id}/toggle?isEnabled=0` | 启用/停用判据 |
| `GET` | `/api/v1/alarm/criteria/{id}/logs` | 查询判据变更版本历史 |

### 5.2 告警记录管理

#### 5.2.1 待办告警列表

**路径：** `GET /api/v1/alarm/records/pending`\
**描述：** 查询所有非终态告警（status IN (1,2)），按告警等级降序排列。

#### 5.2.2 历史告警列表

**路径：** `GET /api/v1/alarm/records/history`\
**描述：** 查询终态告警（status IN (3,4)）。

#### 5.2.3-5.2.6 告警处置

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/alarm/records/{id}` | 告警详情（含处置记录） |
| `PUT` | `/api/v1/alarm/records/{id}/dispose` | 处置告警：`status` 2=处理中/3=已销警/4=误报 |
| `POST` | `/api/v1/alarm/records/batch` | 批量处置 `{ids, status, note}` |
| `GET` | `/api/v1/alarm/records/{id}/logs` | 告警状态变更历史 |

### 5.3 综合告警策略

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/alarm/strategies/list` | 策略列表（name/triggerMode/isEnabled 筛选） |
| `POST` | `/api/v1/alarm/strategies` | 新增策略（Groovy 脚本 + REALTIME/CRON） |
| `PUT` | `/api/v1/alarm/strategies/{id}/toggle?isEnabled=0` | 启用/停用策略 |
| `GET` | `/api/v1/alarm/strategies/{id}/scope` | 策略绑定的隐患点ID列表 |

### 5.4 告警分发规则（v2）

> **v2 更新（2026-07）：** 分发规则已重构为独立模块 `dispatch/`，支持阈值告警与综合告警分离匹配。

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/alarm/dispatch/list` | 分页查询规则列表 | `alarm:dispatch:list` |
| `GET` | `/api/v1/alarm/dispatch/{id}` | 规则详情 | `alarm:dispatch:list` |
| `POST` | `/api/v1/alarm/dispatch` | 新增规则 | `alarm:dispatch:add` |
| `PUT` | `/api/v1/alarm/dispatch/{id}` | 编辑规则 | `alarm:dispatch:edit` |
| `DELETE` | `/api/v1/alarm/dispatch/{id}` | 删除规则 | `alarm:dispatch:remove` |
| `PUT` | `/api/v1/alarm/dispatch/{id}/enabled` | 启用/禁用 | `alarm:dispatch:edit` |
| `GET` | `/api/v1/alarm/dispatch/recipient-options` | 接收人选项（前端勾选） | `alarm:dispatch:list` |

### 5.5 告警通知中心（事件 Tab）

> **新增（2026-07）：** 用户视角的事件通知接口，当前用户仅可查看本人 SYSTEM 渠道通知并标记已读。

#### 5.5.1 当前用户未读事件列表

**路径：** `GET /api/v1/alarm/notifications/recent`\
**描述：** 分页获取当前用户 SYSTEM 渠道未读通知。返回顶层含 `total` 便于分页。\
**权限标识：** `alarm:notification:list`

**请求参数：**

| 参数       | 类型  | 必填 | 说明                                            |
|----------|-----|----|-----------------------------------------------|
| pageNum  | int | 否  | 页码，默认 1                                       |
| pageSize | int | 否  | 每页数量，默认 10，上限 50                               |
| limit    | int | 否  | 向后兼容旧调用方：传 limit 时退化为第 1 页取 limit 条（上限 100） |

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": 1,
      "sourceType": "ALARM",
      "sourceId": 42,
      "title": "橙色告警: 雨量超限",
      "content": "龙泉寺滑坡隐患点一小时雨量 55.2mm 超橙色阈值",
      "recipientName": "管理员",
      "readTime": null,
      "createTime": "2026-07-03 09:15:00"
    }
  ],
  "total": 5,
  "timestamp": 1735689213000
}
```

#### 5.5.2 当前用户未读事件数

**路径：** `GET /api/v1/alarm/notifications/unread-count`\
**权限标识：** `alarm:notification:list`

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "unreadCount": 5,
    "timestamp": 1735689213000
  }
}
```

#### 5.5.3 标记单条已读

**路径：** `POST /api/v1/alarm/notifications/{id}/read`\
**描述：** 仅当当前用户为接收人时生效。\
**权限标识：** `alarm:notification:read`

#### 5.5.4 全部标记已读

**路径：** `POST /api/v1/alarm/notifications/read-all`\
**描述：** 标记当前用户所有 SYSTEM 渠道通知为已读。\
**权限标识：** `alarm:notification:read`

### 5.6 SSE 实时告警推送

**路径：** `GET /api/v1/alarm/stream`\
**描述：** SSE 实时推送告警触发事件，按 userId 路由定向推送。订阅时绑定当前登录用户。\
**权限标识：** `iot:alarm-record:list`

**请求头：**

| 参数            | 类型     | 必填 | 说明                |
|---------------|--------|----|-------------------|
| Authorization | string | 是  | Bearer Token      |
| Accept        | string | 是  | text/event-stream |

**SSE 事件：**

| 事件名   | 说明       |
|-------|----------|
| ready | 连接建立成功   |
| alarm | 实时告警触发事件 |

**alarm 事件 data 示例：**

```json
{
  "alarmId": 42,
  "hazardPointId": 15,
  "alarmLevel": 3,
  "alarmType": "THRESHOLD",
  "alarmMessage": "雨量蓝色预警: hour_rainfall=55.2, 告警等级=橙色"
}
```

> **注意：** 浏览器原生 `EventSource` 不支持自定义 Header，生产环境建议通过 Nginx 反向代理注入 `Authorization`，或使用 `@microsoft/fetch-event-source` 等支持 Header 的 SSE 客户端库。

***

## 6. 监测数据模块接口

### 6.1 监测数据查询（hazardPointId 入口）

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/monitor-data/chart` | 查询监测数据用于图表展示（`hazardPointId` 必填，可选 `deviceId/sensorId/attrCode/valueType/startTime/endTime`） |
| `GET` | `/api/v1/monitor-data/page` | 分页查询监测数据（`hazardPointId` 必填，可选 `pageNum/pageSize/deviceId/sensorId/attrCode/valueType/startTime/endTime`） |
| `GET` | `/api/v1/monitor-data/latest` | 获取隐患点最新监测数据（`hazardPointId` 必填） |

**valueType 说明：** `current` 原始值 / `hour` 小时聚合 / `24h` 天聚合 / `72h` 三天聚合（走 IoTDB GROUP BY 降采样）

### 6.2 传感器维度查询（sensorCode 入口）

> **新增（2026-07）：** 基于 (deviceId, sensorCode) 入口的传感器级数据查询，提供批量最新值、区间、多表达式聚合、完整度、趋势能力。

#### 6.2.1 传感器最新值

**路径：** `GET /api/v1/monitor-data/sensor/latest`\
**描述：** 获取传感器下所有指标最新值（可指定 attrCode 过滤）\
**权限标识：** `basic:device:query`

**请求参数：**

| 参数         | 类型     | 必填 | 说明         |
|------------|--------|----|------------|
| deviceId   | long   | 是  | 设备ID       |
| sensorCode | string | 是  | 传感器编码      |
| attrCode   | string | 否  | 属性编码，不传返回所有 |

#### 6.2.2 区间数据查询

**路径：** `GET /api/v1/monitor-data/sensor/range`\
**描述：** 传感器区间数据查询，支持数值范围过滤\
**权限标识：** `basic:device:query`

**请求参数：**

| 参数         | 类型     | 必填 | 说明                       |
|------------|--------|----|--------------------------|
| deviceId   | long   | 是  | 设备ID                     |
| sensorCode | string | 是  | 传感器编码                    |
| attrCode   | string | 否  | 属性编码，不传则查询传感器所有属性       |
| startTime  | string | 是  | 开始时间（ISO-8601 或 yyyy-MM-dd HH:mm:ss） |
| endTime    | string | 是  | 结束时间                     |
| minValue   | double | 否  | 数值下限过滤                   |
| maxValue   | double | 否  | 数值上限过滤                   |
| limit      | int    | 否  | 返回条数上限，默认 5000            |
| offset     | int    | 否  | 偏移量，默认 0                 |

#### 6.2.3 多表达式聚合

**路径：** `POST /api/v1/monitor-data/sensor/aggregate`\
**描述：** 基于 ExpressionSpec DSL 的多表达式聚合查询\
**权限标识：** `basic:device:query`

**请求参数：**

| 参数          | 类型     | 必填 | 说明       |
|-------------|--------|----|----------|
| deviceId    | long   | 是  | 设备ID     |
| sensorCode  | string | 是  | 传感器编码    |
| startTime   | string | 是  | 开始时间     |
| endTime     | string | 是  | 结束时间     |
| granularity | string | 否  | raw/hour/24h/72h，默认 raw |
| minValue    | double | 否  | 数值下限过滤   |
| maxValue    | double | 否  | 数值上限过滤   |

**请求体：** `ExpressionSpec[]` 数组，支持 `AVG/MAX/MIN/SUM/COUNT/FIRST_VALUE/LAST_VALUE/EXTREME/STDDEV/P50/P95/P99`

#### 6.2.4 完整度查询

**路径：** `GET /api/v1/monitor-data/sensor/completeness`\
**描述：** 查询传感器在时间窗口内的数据完整度\
**权限标识：** `basic:device:query`

**请求参数：**

| 参数                | 类型     | 必填 | 说明           |
|-------------------|--------|----|--------------|
| deviceId          | long   | 是  | 设备ID         |
| sensorCode        | string | 是  | 传感器编码        |
| attrCode          | string | 是  | 属性编码         |
| startTime         | string | 是  | 开始时间         |
| endTime           | string | 是  | 结束时间         |
| expectedIntervalMs | long   | 否  | 期望采样间隔（毫秒）   |

#### 6.2.5 趋势分析

**路径：** `GET /api/v1/monitor-data/sensor/trend`\
**描述：** 查询传感器在时间窗口内的趋势（端点斜率近似）\
**权限标识：** `basic:device:query`

**请求参数：**

| 参数         | 类型     | 必填 | 说明    |
|------------|--------|----|-------|
| deviceId   | long   | 是  | 设备ID  |
| sensorCode | string | 是  | 传感器编码 |
| attrCode   | string | 是  | 属性编码  |
| startTime  | string | 是  | 开始时间  |
| endTime    | string | 是  | 结束时间  |

### 6.3 数据导入导出

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/monitor-data/export` | 导出监测数据（`hazardPointId` 必填） |
| `POST` | `/api/v1/monitor-data/import` | 导入监测数据（`file` MultipartFile） |

### 6.4 计算属性脚本测试

> **新增（2026-07）：** 在线测试计算属性 Groovy 脚本。

**路径：** `POST /api/v1/computed-attributes/test-script`\
**描述：** 在线测试计算属性脚本。在指定监测类型下合并已有的计算属性上下文，执行目标 attrCode 的 calcScript，返回执行结果。\
**权限标识：** `basic:monitorContent:test`

**请求体：**

```json
{
  "monitorTypeId": 1,
  "attrCode": "computed_daily_avg",
  "calcScript": "def result = curData.getOrDefault('rainfall_hour', 0) * 24; return ['computed_daily_avg': result]",
  "curData": { "rainfall_hour": 5.2 },
  "prevData": {}
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "success": true,
    "result": { "computed_daily_avg": 124.8 },
    "elapsedMs": 15
  }
}
```

***

## 7. 报告报表模块接口

### 7.1 报告模板

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/report-templates` | 获取报告模板列表 |
| `POST` | `/api/v1/report-templates` | 新增报告模板 |
| `PUT` | `/api/v1/report-templates/{id}` | 修改报告模板 |
| `DELETE` | `/api/v1/report-templates/{id}` | 删除报告模板 |

### 7.2 报告记录

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/report-records/page` | 分页查询报告记录（templateId/hazardPointId/reportDate/status 筛选） |
| `POST` | `/api/v1/report-records/generate` | 生成报告 |
| `GET` | `/api/v1/report-records/{id}/download` | 下载报告文件 |
| `DELETE` | `/api/v1/report-records/{id}` | 删除报告记录 |

***

## 8. 数据字典接口

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/dict-types` | 获取所有字典类型列表 |
| `GET` | `/api/v1/dict-data/{typeCode}` | 根据字典类型编码获取字典数据列表 |

***

## 9. 系统配置接口

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `GET` | `/api/v1/config/{code}` | 根据配置编码获取配置值 |
| `PUT` | `/api/v1/config/{code}` | 更新系统配置值 |

***

## 10. 文件上传接口

**路径：** `POST /api/v1/upload`\
**描述：** 上传文件（`multipart/form-data`，字段 `file` + 可选 `type`）

***

## 11. WebSocket / SSE 实时推送

### 11.1 告警实时推送（SSE）

**路径：** `GET /api/v1/alarm/stream`\
**描述：** 参见 5.6 节

### 11.2 告警通知推送（SSE）

**路径：** `GET /api/v1/alarm/notifications`\
**描述：** 通知中心 SSE 端点（通过 `AlarmStreamPublisher.publishToUser(userId, ...)` 定向推送）

### 11.3 设备状态推送（WebSocket）

**连接地址：** `ws://{host}/api/v1/ws/device-status`\
**描述：** 实时接收设备状态变化

***

## 12. Terra AI 助手模块接口

> **新增（2026-07）：** Terra AI 助手模块，提供 SSE 流式对话、会话管理、AI 人格配置、模型配置、技能管理、工具管理能力。

### 12.0 权限要求

| 接口域 | 权限标识 |
| --- | --- |
| 对话/会话 | `terra:chat` |
| 人格/模型/技能/工具配置 | `terra:settings` |

### 12.1 SSE 流式对话

#### 12.1.1 发起对话

**路径：** `POST /api/v1/terra/chat`\
**描述：** SSE 流式对话，基于 ReAct 循环支持工具调用。首次对话不传 `conversationId` 则自动创建新会话。\
**权限标识：** `terra:chat`

**请求体：**

```json
{
  "conversationId": 123,
  "message": "查询龙泉寺隐患点最近24小时的雨量数据"
}
```

| 字段             | 类型     | 必填 | 说明           |
|----------------|--------|----|--------------|
| conversationId | long   | 否  | 会话ID，首次不传则自动创建 |
| message        | string | 是  | 用户输入消息       |

**SSE 事件（Content-Type: text/event-stream）：**

| 事件名         | 说明                                |
|-------------|-----------------------------------|
| token       | AI 文本增量（前端逐字渲染）                   |
| tool_call   | 工具调用请求（含 execSide: frontend/backend） |
| tool_result | 工具执行结果                            |
| done        | 对话结束（含 messageId 和 token 消耗）       |
| error       | 错误通知                              |

#### 12.1.2 前端工具结果回调

**路径：** `POST /api/v1/terra/chat/tool-result`\
**描述：** 当 SSE 收到 `tool_call(execSide=frontend)` 事件后，前端执行完毕调用此端点提交结果，使 ReAct 循环继续。\
**权限标识：** `terra:chat`

**请求体：**

```json
{
  "callId": "toolu_xxx",
  "success": true,
  "result": { "status": "completed" }
}
```

| 字段      | 类型      | 必填 | 说明                        |
|---------|---------|----|---------------------------|
| callId  | string  | 是  | SSE tool_call 事件中的 callId |
| success | boolean | 是  | 执行是否成功                    |
| result  | object  | 否  | 执行结果（成功）或错误信息（失败）         |

### 12.2 会话管理

#### 12.2.1 会话列表

**路径：** `GET /api/v1/terra/conversations`\
**描述：** 查询当前用户的会话列表（按最后消息时间倒序）\
**权限标识：** `terra:chat`

#### 12.2.2 会话消息历史

**路径：** `GET /api/v1/terra/conversations/{id}/messages`\
**描述：** 查询会话的消息历史（最多 100 条，按时间正序）。需验证会话归属权。\
**权限标识：** `terra:chat`

#### 12.2.3 新建会话

**路径：** `POST /api/v1/terra/conversations`\
**描述：** 新建会话，请求体 `{ "title": "会话标题" }`，默认标题为"新对话"。\
**权限标识：** `terra:chat`

#### 12.2.4 删除会话

**路径：** `DELETE /api/v1/terra/conversations/{id}`\
**描述：** 逻辑删除会话。需验证会话归属权。\
**权限标识：** `terra:chat`

### 12.3 AI 人格配置

#### 12.3.1 获取人格配置列表

**路径：** `GET /api/v1/terra/personality`\
**权限标识：** `terra:settings`

#### 12.3.2 更新人格配置

**路径：** `PUT /api/v1/terra/personality`\
**权限标识：** `terra:settings`

请求体为 `TerraPersonality` 实体（含 `core` 核心层和 `role` 角色层配置）。

#### 12.3.3 切换启用/停用

**路径：** `PUT /api/v1/terra/personality/{id}/toggle`\
**权限标识：** `terra:settings`

### 12.4 模型配置管理

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/terra/model-configs` | 获取模型配置列表 | `terra:settings` |
| `GET` | `/api/v1/terra/model-configs/{id}` | 获取模型配置详情 | `terra:settings` |
| `POST` | `/api/v1/terra/model-configs` | 新增模型配置（baseUrl/apiKey/modelName） | `terra:settings` |
| `PUT` | `/api/v1/terra/model-configs` | 修改模型配置 | `terra:settings` |
| `DELETE` | `/api/v1/terra/model-configs/{id}` | 删除模型配置 | `terra:settings` |
| `PUT` | `/api/v1/terra/model-configs/{id}/activate` | 激活指定模型配置 | `terra:settings` |

### 12.5 技能管理

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/terra/skills` | 获取技能列表（preset/custom 区分 sourceType） | `terra:settings` |
| `GET` | `/api/v1/terra/skills/{id}` | 获取技能详情 | `terra:settings` |
| `DELETE` | `/api/v1/terra/skills/{id}` | 卸载自定义技能 | `terra:settings` |
| `PUT` | `/api/v1/terra/skills/{id}/toggle` | 启用/停用技能 | `terra:settings` |

### 12.6 工具管理

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/terra/tools` | 获取工具列表（含 execSide: frontend/backend） | `terra:settings` |
| `POST` | `/api/v1/terra/tools` | 新增工具 | `terra:settings` |
| `PUT` | `/api/v1/terra/tools` | 修改工具 | `terra:settings` |
| `DELETE` | `/api/v1/terra/tools/{id}` | 删除工具 | `terra:settings` |
| `PUT` | `/api/v1/terra/tools/{id}/toggle` | 启用/停用工具 | `terra:settings` |

***

## 13. 算法库模块接口

> **新增（2026-07）：** 算法库管理，支持算法信息 CRUD、版本上传/下载/查询、算法描述查询。

### 13.0 权限要求

| 接口域 | 权限标识 |
| --- | --- |
| 列表 | `iot:algo-library:list` |
| 详情/版本查询 | `iot:algo-library:query` |
| 新增 | `iot:algo-library:add` |
| 编辑/启停 | `iot:algo-library:edit` |
| 删除 | `iot:algo-library:remove` |
| 上传 | `iot:algo-library:upload` |

### 13.1 算法信息管理

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/algo-lib/page` | 分页查询算法列表（支持 code/name/status 查询条件） | `iot:algo-library:list` |
| `GET` | `/api/v1/algo-lib/{id}` | 获取算法详情 | `iot:algo-library:query` |
| `POST` | `/api/v1/algo-lib` | 新增算法（`code/name/description/remark`） | `iot:algo-library:add` |
| `PUT` | `/api/v1/algo-lib/{id}` | 修改算法（`name/description/remark`） | `iot:algo-library:edit` |
| `PUT` | `/api/v1/algo-lib/{id}/status?status=1` | 启用/停用算法（`0-停用 1-启用`） | `iot:algo-library:edit` |
| `DELETE` | `/api/v1/algo-lib/{id}` | 删除算法及所有版本 | `iot:algo-library:remove` |

### 13.2 算法版本管理

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/algo-lib/{algoId}/versions` | 获取算法的所有版本列表 | `iot:algo-library:query` |
| `POST` | `/api/v1/algo-lib/{algoId}/versions/upload` | 上传新版本（`multipart/form-data`：file/versionNo/remark） | `iot:algo-library:upload` |
| `DELETE` | `/api/v1/algo-lib/versions/{id}` | 删除版本 | `iot:algo-library:remove` |
| `GET` | `/api/v1/algo-lib/versions/{id}/download` | 下载版本文件（返回 Zip 文件流） | `iot:algo-library:query` |
| `GET` | `/api/v1/algo-lib/{algoCode}/versions/{versionNo}/describe` | 查询特定版本的算法描述 | `iot:algo-library:query` |
| `GET` | `/api/v1/algo-lib/{algoCode}/describe-latest` | 查询最新版本的算法描述 | `iot:algo-library:query` |

***

## 14. 数据共享策略模块接口

> **新增（2026-07）：** 数据共享策略管理，支持策略 CRUD、状态切换、执行、运行日志和脚本管理。

### 14.0 权限要求

| 接口域 | 权限标识 |
| --- | --- |
| 列表 | `datashare:strategy:list` / `basic:device:list`（page 接口） |
| 详情/日志/脚本查询 | `datashare:strategy:query` |
| 新增 | `datashare:strategy:add` |
| 编辑/启停/保存脚本 | `datashare:strategy:edit` |
| 删除 | `datashare:strategy:remove` |
| 执行 | `datashare:strategy:execute` |

### 14.1 策略 CRUD

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/datashare/strategy/page` | 分页查询共享策略列表 | `basic:device:list` |
| `GET` | `/api/v1/datashare/strategy` | 查询策略列表（name/status/method 筛选） | `datashare:strategy:list` |
| `GET` | `/api/v1/datashare/strategy/{id}` | 获取策略详情 | `datashare:strategy:query` |
| `POST` | `/api/v1/datashare/strategy` | 创建共享策略 | `datashare:strategy:add` |
| `PUT` | `/api/v1/datashare/strategy/{id}` | 更新共享策略 | `datashare:strategy:edit` |
| `DELETE` | `/api/v1/datashare/strategy/{id}` | 删除共享策略 | `datashare:strategy:remove` |

### 14.2 策略操作

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `PATCH` | `/api/v1/datashare/strategy/{id}/status` | 切换策略状态（请求体 `{"status": "ENABLED"}`） | `datashare:strategy:edit` |
| `POST` | `/api/v1/datashare/strategy/{id}/execute` | 执行策略 | `datashare:strategy:execute` |

### 14.3 日志与脚本

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/datashare/strategy/{id}/logs` | 获取运行日志列表 | `datashare:strategy:query` |
| `GET` | `/api/v1/datashare/strategy/{id}/script` | 获取当前脚本 | `datashare:strategy:query` |
| `POST` | `/api/v1/datashare/strategy/{id}/script` | 保存脚本（`script` + 可选 `variables`） | `datashare:strategy:edit` |

***

## 15. 数据解析策略模块接口

> **新增（2026-07）：** 数据解析策略管理，基于 Groovy 沙箱引擎（`GroovyScriptEngine` + `BuiltInFunctions`），提供解析策略 CRUD、启停、复制、在线测试和运行日志。

### 15.0 权限要求

| 接口域 | 权限标识 |
| --- | --- |
| 列表/日志查询 | `monitor:parser:list` |
| 编辑/启停/复制 | `monitor:parser:edit` |
| 在线测试 | `monitor:parser:test` |

### 15.1 策略 CRUD

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/iot/parser/strategy/page` | 分页查询解析策略列表 | `monitor:parser:list` |
| `GET` | `/api/v1/iot/parser/strategy/{id}` | 获取策略详情（含关联厂商/设备） | `monitor:parser:list` |
| `POST` | `/api/v1/iot/parser/strategy` | 新增策略（含预编译校验） | `monitor:parser:edit` |
| `PUT` | `/api/v1/iot/parser/strategy` | 更新策略 | `monitor:parser:edit` |
| `DELETE` | `/api/v1/iot/parser/strategy/{id}` | 逻辑删除 | `monitor:parser:edit` |
| `PUT` | `/api/v1/iot/parser/strategy/{id}/status?status=1` | 启用/停用策略 | `monitor:parser:edit` |
| `POST` | `/api/v1/iot/parser/strategy/{id}/copy` | 复制策略 | `monitor:parser:edit` |

### 15.2 在线测试与日志

| 方法 | 路径 | 描述 | 权限 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/iot/parser/strategy/test` | 在线测试脚本（`scriptCode/topic/testData`） | `monitor:parser:test` |
| `GET` | `/api/v1/iot/parser/strategy/{id}/logs` | 分页查询运行日志（`logLevel/startTime/endTime`） | `monitor:parser:list` |
| `DELETE` | `/api/v1/iot/parser/strategy/{id}/logs` | 清空运行日志 | `monitor:parser:edit` |

***

## 附录：状态码对照表

| 状态码 | 含义          | HTTP状态码 |
| --- | ----------- | ------- |
| 200 | 成功          | 200     |
| 400 | 参数错误        | 400     |
| 401 | 未登录/Token过期 | 401     |
| 403 | 无权限         | 403     |
| 404 | 资源不存在       | 404     |
| 405 | 请求方法不支持     | 405     |
| 409 | 资源冲突        | 409     |
| 415 | 请求内容类型不支持   | 415     |
| 500 | 服务器异常       | 500     |
| 601 | 预警触发        | 200     |
| 602 | 设备离线        | 200     |

***

**文档版本：** v2.0\
**生成日期：** 2026-07-03\
**最后更新：** 2026-07-03
