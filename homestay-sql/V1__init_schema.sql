-- =====================================================
-- 多渠道房态管理系统 - 数据库初始化脚本
-- Database: PostgreSQL 14+
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- 1. 基础数据表
-- =====================================================

-- 物业/房源表
CREATE TABLE IF NOT EXISTS t_property (
    id              BIGSERIAL       PRIMARY KEY,
    property_code   VARCHAR(64)     NOT NULL UNIQUE,
    property_name   VARCHAR(128)    NOT NULL,
    address         VARCHAR(512),
    city            VARCHAR(64),
    province        VARCHAR(64),
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_property IS '物业/房源表';
COMMENT ON COLUMN t_property.status IS '状态: 1-启用 0-停用';

-- 房型表
CREATE TABLE IF NOT EXISTS t_room_type (
    id              BIGSERIAL       PRIMARY KEY,
    property_id     BIGINT          NOT NULL REFERENCES t_property(id),
    type_code       VARCHAR(64)     NOT NULL,
    type_name       VARCHAR(128)    NOT NULL,
    bed_count       INTEGER         NOT NULL DEFAULT 1,
    max_guests      INTEGER         NOT NULL DEFAULT 2,
    room_count      INTEGER         NOT NULL DEFAULT 1,
    description     TEXT,
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(property_id, type_code)
);

COMMENT ON TABLE t_room_type IS '房型表';
CREATE INDEX idx_room_type_property ON t_room_type(property_id);

-- 具体物理房间表
CREATE TABLE IF NOT EXISTS t_room (
    id              BIGSERIAL       PRIMARY KEY,
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    room_no         VARCHAR(64)     NOT NULL,
    floor           VARCHAR(32),
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(room_type_id, room_no)
);

COMMENT ON TABLE t_room IS '具体物理房间表';
COMMENT ON COLUMN t_room.status IS '状态: 1-正常 2-维修中 0-停用';
CREATE INDEX idx_room_type ON t_room(room_type_id);

-- 日历库存表（核心：按日期+房型维度的库存）
CREATE TABLE IF NOT EXISTS t_inventory (
    id              BIGSERIAL       PRIMARY KEY,
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    stay_date       DATE            NOT NULL,
    total_rooms     INTEGER         NOT NULL DEFAULT 0,
    booked_rooms    INTEGER         NOT NULL DEFAULT 0,
    locked_rooms    INTEGER         NOT NULL DEFAULT 0,
    available_rooms INTEGER         NOT NULL DEFAULT 0,
    base_price      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(room_type_id, stay_date)
);

COMMENT ON TABLE t_inventory IS '日历库存表';
COMMENT ON COLUMN t_inventory.version IS '乐观锁版本号';
CREATE INDEX idx_inventory_date ON t_inventory(stay_date);
CREATE INDEX idx_inventory_type_date ON t_inventory(room_type_id, stay_date);

-- 价格计划表
CREATE TABLE IF NOT EXISTS t_rate_plan (
    id              BIGSERIAL       PRIMARY KEY,
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    plan_code       VARCHAR(64)     NOT NULL,
    plan_name       VARCHAR(128)    NOT NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    weekday_price   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    weekend_price   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    holiday_price   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_rate_plan IS '价格计划表';

-- =====================================================
-- 2. 渠道平台相关表
-- =====================================================

-- 渠道平台表
CREATE TABLE IF NOT EXISTS t_channel (
    id              BIGSERIAL       PRIMARY KEY,
    channel_code    VARCHAR(32)     NOT NULL UNIQUE,
    channel_name    VARCHAR(64)     NOT NULL,
    adapter_class   VARCHAR(256)    NOT NULL,
    api_endpoint    VARCHAR(512),
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_channel IS '渠道平台表';
COMMENT ON COLUMN t_channel.adapter_class IS '适配层实现类全限定名';

-- 渠道账号配置
CREATE TABLE IF NOT EXISTS t_channel_account (
    id              BIGSERIAL       PRIMARY KEY,
    channel_id      BIGINT          NOT NULL REFERENCES t_channel(id),
    property_id     BIGINT          NOT NULL REFERENCES t_property(id),
    account_name    VARCHAR(128)    NOT NULL,
    app_key         VARCHAR(256),
    app_secret      VARCHAR(512),
    access_token    VARCHAR(512),
    refresh_token   VARCHAR(512),
    token_expire_at TIMESTAMPTZ,
    extra_config    JSONB,
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_channel_account IS '渠道账号配置';
CREATE INDEX idx_channel_account ON t_channel_account(channel_id, property_id);

-- 渠道房型映射表
CREATE TABLE IF NOT EXISTS t_channel_room_mapping (
    id                  BIGSERIAL       PRIMARY KEY,
    channel_account_id  BIGINT          NOT NULL REFERENCES t_channel_account(id),
    room_type_id        BIGINT          NOT NULL REFERENCES t_room_type(id),
    channel_room_code   VARCHAR(128)    NOT NULL,
    channel_room_name   VARCHAR(256),
    sync_status         SMALLINT        NOT NULL DEFAULT 0,
    last_sync_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(channel_account_id, room_type_id, channel_room_code)
);

COMMENT ON TABLE t_channel_room_mapping IS '渠道房型映射表';
COMMENT ON COLUMN t_channel_room_mapping.sync_status IS '0-未同步 1-同步中 2-已同步 -1-同步失败';
CREATE INDEX idx_mapping_room_type ON t_channel_room_mapping(room_type_id);

-- =====================================================
-- 3. 库存锁相关表（核心）
-- =====================================================

-- 库存锁记录表
CREATE TABLE IF NOT EXISTS t_inventory_lock (
    id              BIGSERIAL       PRIMARY KEY,
    lock_key        VARCHAR(128)    NOT NULL UNIQUE,
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    lock_count      INTEGER         NOT NULL,
    lock_type       SMALLINT        NOT NULL,
    source_type     VARCHAR(32)     NOT NULL,
    source_id       VARCHAR(128)    NOT NULL,
    channel_id      BIGINT          REFERENCES t_channel(id),
    expire_at       TIMESTAMPTZ     NOT NULL,
    locked_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    unlock_at       TIMESTAMPTZ,
    status          SMALLINT        NOT NULL DEFAULT 1,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_inventory_lock IS '库存锁记录表';
COMMENT ON COLUMN t_inventory_lock.lock_key IS '锁唯一键: 房型ID+日期范围+来源';
COMMENT ON COLUMN t_inventory_lock.lock_type IS '1-预占锁 2-确认锁 3-屏蔽锁';
COMMENT ON COLUMN t_inventory_lock.source_type IS '订单/活动/手动屏蔽等';
COMMENT ON COLUMN t_inventory_lock.source_id IS '来源业务ID';
COMMENT ON COLUMN t_inventory_lock.status IS '1-已锁定 2-已释放 0-已过期';
CREATE INDEX idx_lock_room_dates ON t_inventory_lock(room_type_id, start_date, end_date);
CREATE INDEX idx_lock_source ON t_inventory_lock(source_type, source_id);
CREATE INDEX idx_lock_expire ON t_inventory_lock(expire_at, status);

-- 库存操作流水表（审计用）
CREATE TABLE IF NOT EXISTS t_inventory_log (
    id              BIGSERIAL       PRIMARY KEY,
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    stay_date       DATE            NOT NULL,
    before_total    INTEGER         NOT NULL,
    before_booked   INTEGER         NOT NULL,
    before_locked   INTEGER         NOT NULL,
    after_total     INTEGER         NOT NULL,
    after_booked    INTEGER         NOT NULL,
    after_locked    INTEGER         NOT NULL,
    change_type     SMALLINT        NOT NULL,
    change_amount   INTEGER         NOT NULL,
    operator        VARCHAR(64),
    operator_id     BIGINT,
    source_type     VARCHAR(32),
    source_id       VARCHAR(128),
    remark          VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_inventory_log IS '库存操作流水表';
COMMENT ON COLUMN t_inventory_log.change_type IS '1-增加库存 2-减少库存 3-锁定 4-释放锁定 5-预订 6-取消预订';
CREATE INDEX idx_inv_log_room_date ON t_inventory_log(room_type_id, stay_date);
CREATE INDEX idx_inv_log_created ON t_inventory_log(created_at);

-- =====================================================
-- 4. 订单相关表
-- =====================================================

-- 订单主表
CREATE TABLE IF NOT EXISTS t_order (
    id              BIGSERIAL       PRIMARY KEY,
    order_no        VARCHAR(64)     NOT NULL UNIQUE,
    channel_id      BIGINT          REFERENCES t_channel(id),
    channel_order_no VARCHAR(128),
    property_id     BIGINT          NOT NULL REFERENCES t_property(id),
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    room_id         BIGINT          REFERENCES t_room(id),
    guest_name      VARCHAR(128)    NOT NULL,
    guest_phone     VARCHAR(32),
    checkin_date    DATE            NOT NULL,
    checkout_date   DATE            NOT NULL,
    night_count     INTEGER         NOT NULL,
    guest_count     INTEGER         NOT NULL DEFAULT 1,
    total_amount    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    paid_amount     NUMERIC(12,2)   NOT NULL DEFAULT 0,
    order_status    SMALLINT        NOT NULL DEFAULT 1,
    pay_status      SMALLINT        NOT NULL DEFAULT 0,
    lock_id         BIGINT          REFERENCES t_inventory_lock(id),
    remark          TEXT,
    extra_data      JSONB,
    book_time       TIMESTAMPTZ,
    cancel_time     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_order IS '订单主表';
COMMENT ON COLUMN t_order.order_status IS '1-待确认 2-已确认 3-已入住 4-已退房 5-已取消 6-未到';
COMMENT ON COLUMN t_order.pay_status IS '0-未支付 1-部分支付 2-已支付';
CREATE INDEX idx_order_dates ON t_order(checkin_date, checkout_date);
CREATE INDEX idx_order_channel ON t_order(channel_id, channel_order_no);
CREATE INDEX idx_order_status ON t_order(order_status);
CREATE INDEX idx_order_created ON t_order(created_at);

-- 订单每日明细（便于统计）
CREATE TABLE IF NOT EXISTS t_order_daily (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL REFERENCES t_order(id) ON DELETE CASCADE,
    room_type_id    BIGINT          NOT NULL REFERENCES t_room_type(id),
    stay_date       DATE            NOT NULL,
    room_price      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(order_id, stay_date)
);

COMMENT ON TABLE t_order_daily IS '订单每日明细';
CREATE INDEX idx_order_daily_date ON t_order_daily(stay_date);
CREATE INDEX idx_order_daily_type_date ON t_order_daily(room_type_id, stay_date);

-- =====================================================
-- 5. 同步相关表
-- =====================================================

-- 同步任务表
CREATE TABLE IF NOT EXISTS t_sync_task (
    id              BIGSERIAL       PRIMARY KEY,
    task_code       VARCHAR(64)     NOT NULL,
    task_name       VARCHAR(128)    NOT NULL,
    task_type       SMALLINT        NOT NULL,
    channel_id      BIGINT          REFERENCES t_channel(id),
    channel_account_id BIGINT       REFERENCES t_channel_account(id),
    cron_expr       VARCHAR(128),
    sync_status     SMALLINT        NOT NULL DEFAULT 0,
    last_run_at     TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    next_run_at     TIMESTAMPTZ,
    params          JSONB,
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_sync_task IS '同步任务表';
COMMENT ON COLUMN t_sync_task.task_type IS '1-库存同步 2-价格同步 3-订单同步 4-房态同步';
COMMENT ON COLUMN t_sync_task.sync_status IS '0-待执行 1-执行中 2-成功 -1-失败';
CREATE INDEX idx_sync_task_channel ON t_sync_task(channel_id);

-- 同步执行日志
CREATE TABLE IF NOT EXISTS t_sync_log (
    id              BIGSERIAL       PRIMARY KEY,
    task_id         BIGINT          REFERENCES t_sync_task(id),
    channel_id      BIGINT          REFERENCES t_channel(id),
    sync_type       SMALLINT        NOT NULL,
    start_time      TIMESTAMPTZ     NOT NULL,
    end_time        TIMESTAMPTZ,
    success_count   INTEGER         NOT NULL DEFAULT 0,
    fail_count      INTEGER         NOT NULL DEFAULT 0,
    total_count     INTEGER         NOT NULL DEFAULT 0,
    sync_status     SMALLINT        NOT NULL,
    error_msg       TEXT,
    request_data    TEXT,
    response_data   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_sync_log IS '同步执行日志';
CREATE INDEX idx_sync_log_task ON t_sync_log(task_id);
CREATE INDEX idx_sync_log_time ON t_sync_log(start_time);

-- Webhook 接收日志
CREATE TABLE IF NOT EXISTS t_webhook_log (
    id              BIGSERIAL       PRIMARY KEY,
    channel_id      BIGINT          REFERENCES t_channel(id),
    event_type      VARCHAR(64)     NOT NULL,
    request_id      VARCHAR(128),
    request_body    TEXT            NOT NULL,
    headers         JSONB,
    process_status  SMALLINT        NOT NULL DEFAULT 0,
    process_msg     VARCHAR(512),
    received_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

COMMENT ON TABLE t_webhook_log IS 'Webhook 接收日志';
COMMENT ON COLUMN t_webhook_log.process_status IS '0-待处理 1-处理成功 -1-处理失败';
CREATE INDEX idx_webhook_event ON t_webhook_log(event_type);
CREATE INDEX idx_webhook_received ON t_webhook_log(received_at);

-- =====================================================
-- 6. 初始化基础渠道数据
-- =====================================================

INSERT INTO t_channel (channel_code, channel_name, adapter_class, status) VALUES
('CTRIP', '携程', 'com.homestay.core.channel.adapter.CtripChannelAdapter', 1),
('MEITUAN', '美团酒店', 'com.homestay.core.channel.adapter.MeituanChannelAdapter', 1),
('FLIGGY', '飞猪', 'com.homestay.core.channel.adapter.FliggyChannelAdapter', 1),
('AIRBNB', 'Airbnb爱彼迎', 'com.homestay.core.channel.adapter.AirbnbChannelAdapter', 1),
('DIRECT', '官网直订', 'com.homestay.core.channel.adapter.DirectChannelAdapter', 1)
ON CONFLICT (channel_code) DO NOTHING;

-- =====================================================
-- 7. 创建用于大屏统计的视图
-- =====================================================

-- 实时房态统计视图
CREATE OR REPLACE VIEW v_realtime_room_status AS
SELECT
    p.id AS property_id,
    p.property_name,
    rt.id AS room_type_id,
    rt.type_name,
    rt.room_count AS physical_rooms,
    CURRENT_DATE AS stat_date,
    inv.total_rooms,
    inv.booked_rooms,
    inv.locked_rooms,
    inv.available_rooms,
    inv.base_price,
    (inv.booked_rooms::DECIMAL / NULLIF(inv.total_rooms, 0) * 100)::DECIMAL(5,2) AS booking_rate
FROM t_property p
JOIN t_room_type rt ON rt.property_id = p.id
LEFT JOIN t_inventory inv ON inv.room_type_id = rt.id AND inv.stay_date = CURRENT_DATE
WHERE p.status = 1 AND rt.status = 1;

-- 渠道订单分布视图
CREATE OR REPLACE VIEW v_channel_order_distribution AS
SELECT
    c.id AS channel_id,
    c.channel_name,
    DATE_TRUNC('day', o.created_at)::DATE AS order_date,
    COUNT(o.id) AS order_count,
    SUM(o.total_amount) AS total_revenue,
    SUM(o.night_count) AS total_nights
FROM t_channel c
LEFT JOIN t_order o ON o.channel_id = c.id AND o.order_status NOT IN (5, 6)
GROUP BY c.id, c.channel_name, DATE_TRUNC('day', o.created_at);

-- 房型热度统计视图（按日期范围聚合）
CREATE OR REPLACE VIEW v_room_type_heatmap AS
SELECT
    rt.id AS room_type_id,
    rt.type_name,
    p.property_name,
    od.stay_date,
    EXTRACT(DOW FROM od.stay_date) AS day_of_week,
    EXTRACT(HOUR FROM o.book_time) AS book_hour,
    COUNT(DISTINCT o.id) AS order_count,
    SUM(od.room_price) AS revenue
FROM t_room_type rt
JOIN t_property p ON p.id = rt.property_id
JOIN t_order_daily od ON od.room_type_id = rt.id
JOIN t_order o ON o.id = od.order_id AND o.order_status NOT IN (5, 6)
GROUP BY rt.id, rt.type_name, p.property_name, od.stay_date,
         EXTRACT(DOW FROM od.stay_date), EXTRACT(HOUR FROM o.book_time);
