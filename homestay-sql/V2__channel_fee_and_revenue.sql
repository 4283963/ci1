-- =====================================================
-- 渠道扣费规则表 + 收益预测视图
-- =====================================================

CREATE TABLE IF NOT EXISTS t_channel_fee_rule (
    id              BIGSERIAL       PRIMARY KEY,
    channel_id      BIGINT          NOT NULL REFERENCES t_channel(id),
    room_type_id    BIGINT          REFERENCES t_room_type(id),
    fee_type        SMALLINT        NOT NULL DEFAULT 1,
    commission_rate NUMERIC(5,4)    NOT NULL DEFAULT 0,
    fixed_fee       NUMERIC(12,2)   NOT NULL DEFAULT 0,
    per_night_fee   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    settlement_days INTEGER         NOT NULL DEFAULT 1,
    min_fee         NUMERIC(12,2)   NOT NULL DEFAULT 0,
    max_fee         NUMERIC(12,2),
    priority        INTEGER         NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    effective_from  DATE            NOT NULL DEFAULT CURRENT_DATE,
    effective_to    DATE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_channel_fee_rule IS '渠道扣费规则表';
COMMENT ON COLUMN t_channel_fee_rule.fee_type IS '1-按比例佣金 2-固定费用 3-比例+固定混合 4-每夜固定费';
COMMENT ON COLUMN t_channel_fee_rule.commission_rate IS '佣金比例(0~1), 如0.15表示15%';
COMMENT ON COLUMN t_channel_fee_rule.fixed_fee IS '每单固定费用';
COMMENT ON COLUMN t_channel_fee_rule.per_night_fee IS '每晚固定费用';
COMMENT ON COLUMN t_channel_fee_rule.settlement_days IS '结算周期(天), 1=T+1';
COMMENT ON COLUMN t_channel_fee_rule.priority IS '优先级, 数字越大优先级越高, 同渠道同房型取最高优先级';
COMMENT ON COLUMN t_channel_fee_rule.effective_from IS '生效起始日期';
COMMENT ON COLUMN t_channel_fee_rule.effective_to IS '生效截止日期, NULL表示永久有效';

CREATE INDEX idx_fee_rule_channel ON t_channel_fee_rule(channel_id);
CREATE INDEX idx_fee_rule_room ON t_channel_fee_rule(channel_id, room_type_id);

INSERT INTO t_channel_fee_rule (channel_id, room_type_id, fee_type, commission_rate, fixed_fee, per_night_fee, settlement_days, priority, status) VALUES
(1, NULL, 3, 0.1500, 5.00, 0, 7, 10, 1),
(2, NULL, 1, 0.1000, 0, 0, 3, 10, 1),
(3, NULL, 3, 0.0800, 3.00, 0, 5, 10, 1),
(4, NULL, 1, 0.0300, 0, 0, 1, 10, 1),
(5, NULL, 4, 0, 0, 2.00, 1, 10, 1);

CREATE OR REPLACE VIEW v_channel_net_profit AS
SELECT
    o.id AS order_id,
    o.order_no,
    o.channel_id,
    c.channel_name,
    o.room_type_id,
    rt.type_name,
    o.checkin_date,
    o.checkout_date,
    o.night_count,
    o.total_amount AS gross_amount,
    COALESCE(f.fee_type, 1) AS fee_type,
    COALESCE(f.commission_rate, 0) AS commission_rate,
    COALESCE(f.fixed_fee, 0) AS fixed_fee,
    COALESCE(f.per_night_fee, 0) AS per_night_fee,
    CASE
        WHEN COALESCE(f.fee_type, 1) = 1 THEN o.total_amount * COALESCE(f.commission_rate, 0)
        WHEN COALESCE(f.fee_type, 1) = 2 THEN COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 3 THEN o.total_amount * COALESCE(f.commission_rate, 0) + COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 4 THEN COALESCE(f.per_night_fee, 0) * o.night_count
        ELSE 0
    END AS platform_fee,
    o.total_amount -
    CASE
        WHEN COALESCE(f.fee_type, 1) = 1 THEN o.total_amount * COALESCE(f.commission_rate, 0)
        WHEN COALESCE(f.fee_type, 1) = 2 THEN COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 3 THEN o.total_amount * COALESCE(f.commission_rate, 0) + COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 4 THEN COALESCE(f.per_night_fee, 0) * o.night_count
        ELSE 0
    END AS net_profit,
    CASE
        WHEN o.total_amount > 0 THEN
            ROUND(((o.total_amount -
            CASE
                WHEN COALESCE(f.fee_type, 1) = 1 THEN o.total_amount * COALESCE(f.commission_rate, 0)
                WHEN COALESCE(f.fee_type, 1) = 2 THEN COALESCE(f.fixed_fee, 0)
                WHEN COALESCE(f.fee_type, 1) = 3 THEN o.total_amount * COALESCE(f.commission_rate, 0) + COALESCE(f.fixed_fee, 0)
                WHEN COALESCE(f.fee_type, 1) = 4 THEN COALESCE(f.per_night_fee, 0) * o.night_count
                ELSE 0
            END) / o.total_amount * 100)::NUMERIC, 2)
        ELSE 0
    END AS net_rate
FROM t_order o
JOIN t_channel c ON c.id = o.channel_id
JOIN t_room_type rt ON rt.id = o.room_type_id
LEFT JOIN t_channel_fee_rule f ON f.channel_id = o.channel_id
    AND (f.room_type_id = o.room_type_id OR f.room_type_id IS NULL)
    AND f.status = 1
    AND CURRENT_DATE BETWEEN f.effective_from AND COALESCE(f.effective_to, '9999-12-31')
    AND f.priority = (
        SELECT MAX(f2.priority) FROM t_channel_fee_rule f2
        WHERE f2.channel_id = o.channel_id
        AND (f2.room_type_id = o.room_type_id OR f2.room_type_id IS NULL)
        AND f2.status = 1
        AND CURRENT_DATE BETWEEN f2.effective_from AND COALESCE(f2.effective_to, '9999-12-31')
    )
WHERE o.order_status NOT IN (5, 6);

CREATE OR REPLACE VIEW v_daily_revenue_forecast AS
SELECT
    od.stay_date,
    o.channel_id,
    c.channel_name,
    o.room_type_id,
    rt.type_name,
    SUM(od.room_price) AS daily_gross,
    CASE
        WHEN COALESCE(f.fee_type, 1) = 1 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0)
        WHEN COALESCE(f.fee_type, 1) = 2 THEN COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 3 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0) + COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 4 THEN COUNT(DISTINCT o.id) * COALESCE(f.per_night_fee, 0)
        ELSE 0
    END AS daily_platform_fee,
    SUM(od.room_price) -
    CASE
        WHEN COALESCE(f.fee_type, 1) = 1 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0)
        WHEN COALESCE(f.fee_type, 1) = 2 THEN COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 3 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0) + COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0)
        WHEN COALESCE(f.fee_type, 1) = 4 THEN COUNT(DISTINCT o.id) * COALESCE(f.per_night_fee, 0)
        ELSE 0
    END AS daily_net_profit
FROM t_order_daily od
JOIN t_order o ON o.id = od.order_id AND o.order_status NOT IN (5, 6)
JOIN t_channel c ON c.id = o.channel_id
JOIN t_room_type rt ON rt.id = od.room_type_id
LEFT JOIN t_channel_fee_rule f ON f.channel_id = o.channel_id
    AND (f.room_type_id = od.room_type_id OR f.room_type_id IS NULL)
    AND f.status = 1
    AND CURRENT_DATE BETWEEN f.effective_from AND COALESCE(f.effective_to, '9999-12-31')
    AND f.priority = (
        SELECT MAX(f2.priority) FROM t_channel_fee_rule f2
        WHERE f2.channel_id = o.channel_id
        AND (f2.room_type_id = od.room_type_id OR f2.room_type_id IS NULL)
        AND f2.status = 1
        AND CURRENT_DATE BETWEEN f2.effective_from AND COALESCE(f2.effective_to, '9999-12-31')
    )
GROUP BY od.stay_date, o.channel_id, c.channel_name, o.room_type_id, rt.type_name,
         f.fee_type, f.commission_rate, f.fixed_fee, f.per_night_fee;
