# 多渠道房态管理系统 - 模块联动说明

## 项目结构

```
ci1/
├── homestay-sql/               # PostgreSQL 数据库脚本
│   └── V1__init_schema.sql     # 建表 + 初始化渠道 + 统计视图
│
├── homestay-core/              # 后端中台（Spring Boot 3 + Maven 多模块）
│   ├── pom.xml
│   ├── homestay-core-common/   # 公共：枚举、异常、工具、统一返回
│   ├── homestay-core-model/    # 实体、DTO、VO
│   ├── homestay-core-lock/     # ★ 库存锁模块（分布式锁+乐观锁）
│   ├── homestay-core-channel/  # ★ 渠道适配层（携程/美团/飞猪/Airbnb/直订）
│   ├── homestay-core-service/  # 订单服务、大屏统计服务
│   └── homestay-core-web/      # 启动类、配置、Controller、WebSocket
│
├── homestay-sync/              # 同步微服务（独立 Spring Boot）
│   ├── pom.xml
│   └── src/main/java/com/homestay/sync/
│       ├── job/                # 定时同步任务（库存/订单/价格/房态）
│       ├── webhook/            # Webhook 事件解析器
│       ├── controller/         # Webhook 接入端点
│       ├── client/             # Feign 调用 homestay-core
│       └── service/            # 订单同步服务
│
└── homestay-ui/                # 前端大屏（Vue3 + Vite + ECharts）
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── views/Dashboard.vue # ★ 大屏主页
        ├── components/
        │   ├── StatCard.vue          # 统计卡片
        │   ├── RealtimeRoomPanel.vue # 实时房态表
        │   ├── ChannelChart.vue      # 渠道分布图（柱+饼）
        │   └── HeatmapChart.vue      # ★ 房型热度热力图
        ├── api/                # 接口封装
        ├── store/              # Pinia 状态管理
        └── utils/websocket.js  # STOMP 实时推送
```

---

## 三大模块联动架构图

```
┌──────────────────────────────────────────────────────────────┐
│                         前端 homestay-ui                       │
│  Vue3 + ECharts + Pinia + WebSocket(STOMP over SockJS)        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ 统计卡片 │ │实时房态表│ │渠道分布  │ │热力图    │         │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘         │
└───────┼────────────┼────────────┼────────────┼───────────────┘
        │ HTTP       │ HTTP       │ HTTP       │ WebSocket(订阅)
        ▼            ▼            ▼            ▼
┌──────────────────────────────────────────────────────────────┐
│                    后端中台 homestay-core :8080               │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │  Dashboard   │  │   Order      │  │  Inventory + Lock  │  │
│  │  Controller  │  │  Controller  │  │  Controller        │  │
│  └──────┬───────┘  └──────┬───────┘  └─────────┬──────────┘  │
│         │                 │                     │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌─────────▼──────────┐  │
│  │ Dashboard    │  │ OrderService │  │ InventoryLockSvc   │  │
│  │ Service      │  │              │  │ InventoryService   │  │
│  └──────┬───────┘  └──────┬───────┘  └─────────┬──────────┘  │
│         │                 │                     │              │
│         │          ┌──────▼──────────────┐      │              │
│         │          │ 库存锁定 → 扣减库存  │◄─────┘              │
│         │          └──────┬──────────────┘                     │
│         │                 │ 调用                               │
│         │          ┌──────▼──────────────┐                     │
│         │          │ ChannelAdapterFactor │                    │
│         │          │ → 各渠道适配器       │                    │
│         │          └─────────────────────┘                     │
│         │                                                       │
│         │  /topic/realtime-rooms (WebSocket 30秒推一次)         │
│         └──────────────────────────────────────────► STOMP Broker
│                                                               │
└───────────────┬───────────────────────────────────────────────┘
                │ Feign /api/* 调用
                ▼
┌──────────────────────────────────────────────────────────────┐
│                 同步微服务 homestay-sync :8081                 │
│                                                               │
│  ┌──────────────────────────────┐  ┌────────────────────────┐ │
│  │   定时任务(Quartz 风格 cron)  │  │  Webhook 接入控制器    │ │
│  │                              │  │  /webhook/{channelCode}│ │
│  │ */2h 库存全量推送到渠道       │  │  GET  签名校验         │ │
│  │ */5m 订单增量拉取            │  │  POST 接收渠道事件     │ │
│  │ */1h 价格推送               │  └───────────┬────────────┘ │
│  │ 3:00 房态同步               │              │                │
│  └───────────────┬──────────────┘              │                │
│                  │                             │                │
│                  ▼                             ▼                │
│         ┌───────────────────────────────────────────────┐      │
│         │            WebhookEventParser                 │      │
│         │  (自动解析各渠道JSON → 统一订单事件对象)        │      │
│         └───────────────────────┬───────────────────────┘      │
│                                 │                               │
│                                 ▼                               │
│                        ┌──────────────────┐                    │
│                        │ OrderSyncService │                    │
│                        └────────┬─────────┘                    │
│                                 │                               │
└─────────────────────────────────┼───────────────────────────────┘
                                  │ Feign
                                  ▼
                      写回 homestay-core 的订单/库存
```

---

## 核心流程 1：渠道订单 Webhook → 中台入库

```
美团/携程等 OTA 平台
        │  HTTP POST 订单事件
        ▼
  homestay-sync :8081/webhook/CTRIP
        │  (WebhookController)
        │  1. 存 t_webhook_log（状态0=待处理）
        │  2. 验签
        ▼
  WebhookEventParser.parseAndProcess()
        │  统一解析 JSON → ChannelOrderEvent
        ▼
  OrderSyncService.processChannelOrder()
        │
        │  事件类型路由:
        │  ├─ ORDER_CREATE → 调 core /orders (Feign)
        │  ├─ ORDER_CANCEL → 调 core /orders/{id}/cancel
        │  └─ ORDER_MODIFY → 预留业务位
        │
        ▼
  写 t_sync_log 记录同步结果
        │
        ▼
  homestay-core :8080/api/orders  POST
        │  (OrderController → OrderServiceImpl)
        │
        │  ① 渠道去重检查（channel_order_no）
        │  ② 生成锁 DTO → InventoryLockService.tryLock()
        │     ├─ Redisson 分布式锁 KEY=房型+日期
        │     ├─ 逐日检查 available = total - booked - locked
        │     ├─ UPDATE t_inventory SET locked_rooms += n
        │     │        WHERE version = ?  ← 乐观锁
        │     └─ INSERT t_inventory_lock
        │  ③ 扣减库存：booked_rooms += 1, locked_rooms -=1
        │  ④ INSERT t_order + t_order_daily（按天拆分）
        │  ⑤ confirmLock() → 将预占锁升级为确认锁
        │  ⑥ INSERT t_inventory_log 操作流水
        │
        ▼
    STOMP Broker /topic/realtime-rooms
        │  (每30秒 DashboardController 定时推送)
        ▼
  homestay-ui Dashboard.vue 实时刷新房态表格
```

**关键代码位置：**

- Webhook 入口： [WebhookController.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-sync/src/main/java/com/homestay/sync/controller/WebhookController.java#L1)
- 订单创建+锁库存核心： [OrderServiceImpl.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-service/src/main/java/com/homestay/core/service/impl/OrderServiceImpl.java#L1)
- 分布式锁实现： [InventoryLockServiceImpl.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-lock/src/main/java/com/homestay/core/lock/impl/InventoryLockServiceImpl.java#L1)
- AOP 分布式锁切面： [DistributedLockAspect.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-lock/src/main/java/com/homestay/core/lock/impl/DistributedLockAspect.java#L1)

---

## 核心流程 2：中台库存变更 → 推送到各渠道

```
运营在后台调整了某房型某日的库存/价格
        │
        ▼
  homestay-core InventoryController.adjust()
        │  UPDATE t_inventory
        ▼
  homestay-sync SyncScheduledJobs 每2小时触发
        │
        ▼
  syncInventoryJob()  （带 Redisson 分布式锁防重入）
        │
        │  1. 查询各渠道账号配置
        │  2. 查询渠道→房型映射 t_channel_room_mapping
        │  3. 组装 ChannelInventoryDTO 列表
        │
        ▼
  ChannelAdapterFactory.getAdapter(channelCode)
        │
        ├─► CtripChannelAdapter.pushInventory()  → 携程 open API
        ├─► MeituanChannelAdapter.pushInventory() → 美团 API
        ├─► FliggyChannelAdapter.pushInventory()  → 飞猪 TOP
        ├─► AirbnbChannelAdapter.pushInventory()  → Airbnb API
        └─► DirectChannelAdapter.pushInventory()  → 直订(跳过)
        │
        ▼
  写 t_sync_log（成功/失败条数、耗时、错误详情）
```

**关键代码位置：**

- 渠道适配器工厂： [ChannelAdapterFactory.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-channel/src/main/java/com/homestay/core/channel/service/ChannelAdapterFactory.java#L1)
- 渠道统一接口： [ChannelAdapter.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-channel/src/main/java/com/homestay/core/channel/adapter/ChannelAdapter.java#L1)
- 各适配器抽象基类： [AbstractChannelAdapter.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-channel/src/main/java/com/homestay/core/channel/adapter/AbstractChannelAdapter.java#L1)
- 定时同步任务： [SyncScheduledJobs.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-sync/src/main/java/com/homestay/sync/job/SyncScheduledJobs.java#L1)

---

## 核心流程 3：前端大屏热力图 + 实时房态推送

```
用户打开 Dashboard.vue
        │
        ├── HTTP GET /api/dashboard/realtime          → 实时房态表
        ├── HTTP GET /api/dashboard/channel-distribution → 渠道柱/饼图
        └── HTTP GET /api/dashboard/heatmap           → 房型热度热力图
        │
        ▼  STOMP 连接
  SockJS → /api/ws → WebSocketConfig
        │
        │ 订阅 /topic/realtime-rooms
        │
        ▼  每 30 秒 DashboardController 推送
  DashboardService.getRealtimeRoomStatus()
        │  SQL: 联表 t_property + t_room_type + t_inventory
        │       计算 booking_rate = booked/total*100
        ▼
  messagingTemplate.convertAndSend("/topic/realtime-rooms", data)
        │
        ▼
  store.setRealtimeData() → 表格响应式更新
        │
  ┌────────────────────────────────────────────┐
  │  HeatmapChart.vue 三视图切换                │
  │  ├─ 按日期  X=日期 Y=房型 value=订单数      │
  │  ├─ 按时段  X=00:00~23:00 Y=房型           │
  │  └─ 按星期  X=周一周二... Y=房型            │
  │  visualMap 6色渐变：深蓝→蓝→青→橙→红        │
  │  颜色越深 = 该时段/日期越热门              │
  └────────────────────────────────────────────┘
```

**关键代码位置：**

- 大屏主页面： [Dashboard.vue](file:///Users/kl/Documents/trae_projects2/ci1/homestay-ui/src/views/Dashboard.vue#L1)
- 实时房态面板： [RealtimeRoomPanel.vue](file:///Users/kl/Documents/trae_projects2/ci1/homestay-ui/src/components/RealtimeRoomPanel.vue#L1)
- 热力图组件（三模式切换）： [HeatmapChart.vue](file:///Users/kl/Documents/trae_projects2/ci1/homestay-ui/src/components/HeatmapChart.vue#L1)
- WebSocket 配置： [WebSocketConfig.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-web/src/main/java/com/homestay/core/web/config/WebSocketConfig.java#L1)
- WS 定时推送+API： [DashboardController.java](file:///Users/kl/Documents/trae_projects2/ci1/homestay-core/homestay-core-web/src/main/java/com/homestay/core/web/controller/DashboardController.java#L1)

---

## 库存锁的三层防护机制（防超卖）

| 层级 | 技术 | 作用 | 场景 |
|---|---|---|---|
| 第1层 | Redisson 分布式锁（可重入RLock） | 防止多线程同时扣同一房型同日库存 | 5分钟内渠道并发下单 |
| 第2层 | 数据库乐观锁（version 字段） | 防止分布式锁过期后并发更新冲突 | 极端高并发、网络延迟 |
| 第3层 | SQL `WHERE available >= n` 条件 | 数据库层兜底校验 | 绕过服务层直接改DB的情况 |

```
UPDATE t_inventory
SET locked_rooms = locked_rooms + #{n},
    available_rooms = available_rooms - #{n},
    version = version + 1
WHERE room_type_id = #{rt}
  AND stay_date   = #{date}
  AND version     = #{oldVersion}   -- 第2层
  AND available_rooms >= #{n}       -- 第3层
```

三层任一失败 → 抛异常 → 事务回滚 → 库存不会扣。

---

## 启动步骤

### 1. 数据库初始化

```bash
createdb homestay
psql -U postgres -d homestay -f homestay-sql/V1__init_schema.sql
```

### 2. 启动 Redis（Redisson 需要）

```bash
redis-server --daemonize yes
```

### 3. 启动 homestay-core 中台

```bash
cd homestay-core
# 先建具体 Mapper 接口（见下面注意事项），然后：
mvn -pl homestay-core-web -am spring-boot:run -DskipTests
# 端口 :8080
```

### 4. 启动 homestay-sync 同步服务

```bash
cd homestay-sync
mvn spring-boot:run -DskipTests
# 端口 :8081
# Webhook 地址: http://<服务器IP>:8081/webhook/{CTRIP|MEITUAN|FLIGGY|AIRBNB|DIRECT}
```

### 5. 启动前端大屏

```bash
cd homestay-ui
npm install   # 或 pnpm i / yarn
npm run dev
# 访问 http://localhost:3000/#/dashboard
```

---

## 重要：编译前需要做的事

`homestay-core` 和 `homestay-sync` 里使用了通用代理 `MapperProxyConfig` 让框架先跑起来。
**生产环境**你需要为每个实体创建具体的 Mapper 接口（10 秒一个）：

```java
// homestay-core-service/src/main/java/com/homestay/core/service/mapper/InventoryMapper.java
package com.homestay.core.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.model.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {}
```

然后删除 `MapperProxyConfig.java`，把 Service 里注入的 `BaseMapper<Inventory>` 改成具体的 `InventoryMapper` 即可。

同理需要建的 Mapper：
- `InventoryLockMapper`
- `OrderMapper`
- `OrderDailyMapper`
- `ChannelMapper`
- `ChannelAccountMapper`
- `ChannelRoomMappingMapper`
- `RoomTypeMapper`
- `PropertyMapper`
- `SyncLogMapper`（sync 项目）
- `WebhookLogMapper`（sync 项目）

---

## 数据库表快速索引

| 表 | 用途 | 关键字段 |
|---|---|---|
| t_inventory | **核心库存表** | room_type_id + stay_date（唯一索引） |
| t_inventory_lock | **库存锁表** | lock_key（唯一）、status、expire_at |
| t_order | 订单主表 | order_no（唯一）、channel_id + channel_order_no |
| t_order_daily | 订单按天拆 | 用于热力图统计 join |
| t_channel | 渠道字典 | adapter_class 反射加载适配器 |
| t_channel_account | 渠道账号 | app_key/secret/token，按 channel+property |
| t_channel_room_mapping | 房型映射 | 中台房型 ↔ 渠道房型编码 |
| t_sync_task | 定时任务配置 | cron 表达式、下次执行时间 |
| t_sync_log | 同步流水 | 每一条任务的成功/失败数 |
| t_webhook_log | Webhook 日志 | 原始请求、处理状态、重试用 |
| v_realtime_room_status | 大屏视图 | 今日房态+出租率 |
| v_channel_order_distribution | 大屏视图 | 各渠道每日订单/收入 |
| v_room_type_heatmap | 大屏视图 | 房型热度（按天/星期/小时） |
