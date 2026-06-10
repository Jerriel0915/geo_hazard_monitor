[根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-video**

# zwei-iot-video — 视频设备管理 + 隐患点关联

> 面包屑: [根目录](../../CLAUDE.md) > [server](../) > **zwei-iot-video**

## 模块职责

视频监控设备域:

- 视频设备 CRUD (`video_device`) — 含协议编码/流地址 (RTMP/HLS)/在线状态
- 隐患点关联 (`video_device_hazard_point` 物理表) — UNIQUE 约束 + FK 引用
- 视频流播放 (HLS/m3u8, 前端通过 hls.js / mpegts.js 播放)
- **实现跨模块接口** `IVideoDeviceStatService` (由 `zwei-iot-device` 定义)

## 关键依赖

- `zwei-common`
- `zwei-iot-device` (跨模块接口 `IVideoDeviceStatService` + `IDeviceHazardRelationService`)
- lombok

## 主要子包

| 子包             | 职责                                                      |
|----------------|---------------------------------------------------------|
| `controller`   | `VideoDeviceController` (CRUD + 隐患点绑定 + 播放地址)           |
| `service`      | `IVideoDeviceService` / `IVideoDeviceStatService`       |
| `service.impl` | `VideoDeviceServiceImpl` / `VideoDeviceStatServiceImpl` |
| `domain`       | `VideoDevice` / `VideoDeviceHazardPoint`                |
| `domain.vo`    | `BoundVideoDeviceVO`                                    |
| `mapper`       | `VideoDeviceMapper` / `VideoDeviceHazardPointMapper`    |

## 对外接口 (Controller)

| 路径                                           | 职责           |
|----------------------------------------------|--------------|
| `/api/v1/iot/video-device/*`                 | 视频设备 CRUD    |
| `/api/v1/iot/video-device/{id}/hazard-point` | 绑定/解绑隐患点     |
| `/api/v1/iot/video-device/play/{id}`         | 获取播放地址 (HLS) |

## 跨模块接口实现 (IVideoDeviceStatService)

| 方法                | 用途                     |
|-------------------|------------------------|
| `countAll()`      | 视频设备总数 (仪表盘)           |
| `countByStatus()` | 按状态分组 (0=离线 1=在线 2=故障) |

> **解耦**: `zwei-monitor` 通过 `IVideoDeviceStatService` 拿数据, 不知道具体实现, 也不依赖 video 的 Mapper。

## 核心实现类索引 (P1)

| 类                            | 文件                                             | 关键方法 / 责任                                                                                            |
|------------------------------|------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `VideoDeviceServiceImpl`     | `service/impl/VideoDeviceServiceImpl.java`     | CRUD + 唯一性校验 + 删除时先解绑 (clean `video_device_hazard_point` 再 `deleteVideoDeviceById`)，`@Transactional` |
| `VideoDeviceStatServiceImpl` | `service/impl/VideoDeviceStatServiceImpl.java` | 实现 `IVideoDeviceStatService`：`countAll()` / `countByStatus()` 薄封装委托                                  |

## 数据模型

- `video_device` — 视频设备 (id / code UNIQUE / name / icon / iconPath / protocolCode / protocolName / streamUrl /
  status: 0=离线 1=在线 2=故障 / lastOnlineTime / installTime / longitude / latitude / delFlag)
- `video_device_hazard_point` — 视频-隐患点绑定 (id / videoDeviceId / hazardPointId / installLongitude /
  installLatitude / bindTime / createBy / updateBy)
    - UNIQUE `uk_video_device_hazard_point` (videoDeviceId, hazardPointId)
    - FK `fk_vdhp_video` → video_device(id) / `fk_vdhp_hp` → hazard_point(id) (本表位于 video 模块, 但 FK 引用 hazard
      模块)
    - CHECK `chk_vdhp_lat` / `chk_vdhp_lng` (经纬度范围)

## 流媒体协议支持

- RTSP → HLS (通过 FFmpeg / MediaMTX 等转码服务, 由 docker-compose 提供)
- m3u8 播放: 前端 `hls.js` (`web/src/views/basic/VideoDevice.vue`)
- 实时流: `mpegts.js` 支持 FLV/TS over HTTP
- 数据库初始化时插入 1 条样例: `'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'`

## 测试与质量

- 单元测试: `IVideoDeviceStatService` 各方法
- 集成测试: RTSP 模拟 + HLS 切片生成

## 常见问题 (FAQ)

**Q: 视频设备不在线怎么排查?**
A: 1) `lastOnlineTime` 是否在 5 分钟内; 2) RTSP URL 是否可达 (telnet / ffprobe); 3) 转码服务是否正常。

**Q: HLS 切片延迟大?**
A: 调小切片时长 (例: 1s), 但增加 CDN 压力。也可改用 WebRTC / LLHLS。

**Q: 跨模块接口与 `zwei-iot-hazard` 的 `hazard_point_video` 有什么区别?**
A: 数据库中表名实际是 `video_device_hazard_point`（位于 video 模块，**不是** `hazard_point_video`）。
`IVideoDeviceStatService` 是**逻辑接口** (在本模块)。两者配合: 统计走接口, 详细绑定走 hazard 模块 API。

## 相关文件清单

- `pom.xml`
- `src/main/java/com/zwei/iot/video/controller/VideoDeviceController.java`
- `src/main/java/com/zwei/iot/video/service/IVideoDeviceService.java`
- `src/main/java/com/zwei/iot/video/service/impl/VideoDeviceServiceImpl.java` (P1)
- `src/main/java/com/zwei/iot/video/service/impl/VideoDeviceStatServiceImpl.java` (P1)

## 变更记录 (Changelog)

| 时间               | 变更                                                                        |
|------------------|---------------------------------------------------------------------------|
| 2026-06-10 18:52 | 首次生成模块级 CLAUDE.md (架构师自动扫描)                                               |
| 2026-06-10 19:08 | 增量补扫: 新增核心实现类索引、表名澄清 `video_device_hazard_point`、删除级联顺序、protocol_code 实际值 |
