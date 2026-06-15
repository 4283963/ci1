package com.homestay.core.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.model.entity.*;
import com.homestay.core.model.vo.ChannelOrderDistributionVO;
import com.homestay.core.model.vo.RealtimeRoomStatusVO;
import com.homestay.core.model.vo.RoomTypeHeatmapVO;
import com.homestay.core.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<RealtimeRoomStatusVO> getRealtimeRoomStatus() {
        try {
            String sql = "SELECT p.id AS property_id, p.property_name, rt.id AS room_type_id, " +
                    "rt.type_name, rt.room_count AS physical_rooms, CURRENT_DATE AS stat_date, " +
                    "inv.total_rooms, inv.booked_rooms, inv.locked_rooms, inv.available_rooms, " +
                    "inv.base_price, " +
                    "ROUND((inv.booked_rooms::DECIMAL / NULLIF(inv.total_rooms, 0) * 100), 2) AS booking_rate " +
                    "FROM t_property p " +
                    "JOIN t_room_type rt ON rt.property_id = p.id " +
                    "LEFT JOIN t_inventory inv ON inv.room_type_id = rt.id AND inv.stay_date = CURRENT_DATE " +
                    "WHERE p.status = 1 AND rt.status = 1 " +
                    "ORDER BY p.id, rt.id";

            List<RealtimeRoomStatusVO> list = jdbcTemplate.query(sql, (rs, row) -> {
                RealtimeRoomStatusVO vo = new RealtimeRoomStatusVO();
                vo.setPropertyId(rs.getLong("property_id"));
                vo.setPropertyName(rs.getString("property_name"));
                vo.setRoomTypeId(rs.getLong("room_type_id"));
                vo.setTypeName(rs.getString("type_name"));
                vo.setPhysicalRooms(rs.getInt("physical_rooms"));
                vo.setStatDate(rs.getDate("stat_date").toLocalDate());
                vo.setTotalRooms(rs.getObject("total_rooms", Integer.class));
                vo.setBookedRooms(rs.getObject("booked_rooms", Integer.class));
                vo.setLockedRooms(rs.getObject("locked_rooms", Integer.class));
                vo.setAvailableRooms(rs.getObject("available_rooms", Integer.class));
                vo.setBasePrice(rs.getBigDecimal("base_price"));
                vo.setBookingRate(rs.getBigDecimal("booking_rate"));
                return vo;
            });
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.error("查询实时房态异常", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ChannelOrderDistributionVO> getChannelOrderDistribution(LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT c.id AS channel_id, c.channel_name, " +
                    "DATE_TRUNC('day', o.created_at)::DATE AS order_date, " +
                    "COUNT(o.id) AS order_count, " +
                    "COALESCE(SUM(o.total_amount), 0) AS total_revenue, " +
                    "COALESCE(SUM(o.night_count), 0) AS total_nights " +
                    "FROM t_channel c " +
                    "LEFT JOIN t_order o ON o.channel_id = c.id " +
                    "  AND o.order_status NOT IN (5, 6) " +
                    "  AND o.created_at::DATE BETWEEN ? AND ? " +
                    "GROUP BY c.id, c.channel_name, DATE_TRUNC('day', o.created_at) " +
                    "ORDER BY c.id, order_date";

            List<ChannelOrderDistributionVO> list = jdbcTemplate.query(sql, (rs, row) -> {
                ChannelOrderDistributionVO vo = new ChannelOrderDistributionVO();
                vo.setChannelId(rs.getLong("channel_id"));
                vo.setChannelName(rs.getString("channel_name"));
                java.sql.Date d = rs.getDate("order_date");
                vo.setOrderDate(d != null ? d.toLocalDate().toString() : null);
                vo.setOrderCount(rs.getInt("order_count"));
                vo.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                vo.setTotalNights(rs.getInt("total_nights"));
                return vo;
            }, startDate, endDate);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.error("查询渠道订单分布异常", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<RoomTypeHeatmapVO> getRoomTypeHeatmap(LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT rt.id AS room_type_id, rt.type_name, p.property_name, " +
                    "od.stay_date, " +
                    "EXTRACT(DOW FROM od.stay_date) AS day_of_week, " +
                    "EXTRACT(HOUR FROM o.book_time) AS book_hour, " +
                    "COUNT(DISTINCT o.id) AS order_count, " +
                    "COALESCE(SUM(od.room_price), 0) AS revenue " +
                    "FROM t_room_type rt " +
                    "JOIN t_property p ON p.id = rt.property_id " +
                    "LEFT JOIN t_order_daily od ON od.room_type_id = rt.id " +
                    "LEFT JOIN t_order o ON o.id = od.order_id AND o.order_status NOT IN (5, 6) " +
                    "WHERE od.stay_date BETWEEN ? AND ? " +
                    "GROUP BY rt.id, rt.type_name, p.property_name, od.stay_date, " +
                    "EXTRACT(DOW FROM od.stay_date), EXTRACT(HOUR FROM o.book_time) " +
                    "ORDER BY od.stay_date, order_count DESC";

            List<RoomTypeHeatmapVO> list = jdbcTemplate.query(sql, (rs, row) -> {
                RoomTypeHeatmapVO vo = new RoomTypeHeatmapVO();
                vo.setRoomTypeId(rs.getLong("room_type_id"));
                vo.setTypeName(rs.getString("type_name"));
                vo.setPropertyName(rs.getString("property_name"));
                vo.setStayDate(rs.getDate("stay_date").toLocalDate().toString());
                vo.setDayOfWeek(rs.getInt("day_of_week"));
                Object bh = rs.getObject("book_hour");
                vo.setBookHour(bh != null ? ((Number) bh).intValue() : 12);
                vo.setOrderCount(rs.getInt("order_count"));
                vo.setRevenue(rs.getBigDecimal("revenue"));
                return vo;
            }, startDate, endDate);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.error("查询房型热力图异常", e);
            return new ArrayList<>();
        }
    }
}
