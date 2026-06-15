package com.homestay.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.core.model.entity.ChannelFeeRule;
import com.homestay.core.model.entity.Inventory;
import com.homestay.core.model.vo.RevenueAnalysisVO;
import com.homestay.core.model.vo.RevenueForecastVO;
import com.homestay.core.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueServiceImpl implements RevenueService {

    private final JdbcTemplate jdbcTemplate;
    private final BaseMapper<ChannelFeeRule> feeRuleMapper;
    private final BaseMapper<Inventory> inventoryMapper;

    @Override
    public RevenueAnalysisVO getRevenueAnalysis(LocalDate startDate, LocalDate endDate) {
        try {
            List<RevenueForecastVO> confirmedForecast = getConfirmedRevenue(startDate, endDate);
            List<RevenueForecastVO> predictedForecast = getPredictedRevenue(startDate, endDate, confirmedForecast);
            List<RevenueForecastVO> mergedForecast = mergeForecast(confirmedForecast, predictedForecast, startDate, endDate);
            List<RevenueForecastVO.ChannelBreakdown> channelBreakdown = getChannelBreakdown(startDate, endDate);

            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalFee = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            for (RevenueForecastVO day : mergedForecast) {
                totalGross = totalGross.add(day.getGrossRevenue() != null ? day.getGrossRevenue() : BigDecimal.ZERO);
                totalFee = totalFee.add(day.getPlatformFee() != null ? day.getPlatformFee() : BigDecimal.ZERO);
                totalNet = totalNet.add(day.getNetProfit() != null ? day.getNetProfit() : BigDecimal.ZERO);
            }
            BigDecimal avgNetRate = totalGross.compareTo(BigDecimal.ZERO) > 0
                    ? totalNet.divide(totalGross, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            RevenueAnalysisVO vo = new RevenueAnalysisVO();
            vo.setTotalGrossRevenue(totalGross);
            vo.setTotalPlatformFee(totalFee);
            vo.setTotalNetProfit(totalNet);
            vo.setAvgNetRate(avgNetRate);
            vo.setDailyForecast(mergedForecast);
            vo.setChannelBreakdown(channelBreakdown);
            return vo;
        } catch (Exception e) {
            log.error("收益分析异常", e);
            RevenueAnalysisVO empty = new RevenueAnalysisVO();
            empty.setTotalGrossRevenue(BigDecimal.ZERO);
            empty.setTotalPlatformFee(BigDecimal.ZERO);
            empty.setTotalNetProfit(BigDecimal.ZERO);
            empty.setAvgNetRate(BigDecimal.ZERO);
            empty.setDailyForecast(new ArrayList<>());
            empty.setChannelBreakdown(new ArrayList<>());
            return empty;
        }
    }

    private List<RevenueForecastVO> getConfirmedRevenue(LocalDate startDate, LocalDate endDate) {
        String sql =
                "SELECT od.stay_date, " +
                "SUM(od.room_price) AS daily_gross, " +
                "CASE " +
                "  WHEN COALESCE(f.fee_type, 1) = 1 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 2 THEN COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 3 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0) + COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 4 THEN COUNT(DISTINCT o.id) * COALESCE(f.per_night_fee, 0) " +
                "  ELSE 0 " +
                "END AS daily_fee, " +
                "SUM(od.room_price) - " +
                "CASE " +
                "  WHEN COALESCE(f.fee_type, 1) = 1 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 2 THEN COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 3 THEN SUM(od.room_price) * COALESCE(f.commission_rate, 0) + COUNT(DISTINCT o.id) * COALESCE(f.fixed_fee, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 4 THEN COUNT(DISTINCT o.id) * COALESCE(f.per_night_fee, 0) " +
                "  ELSE 0 " +
                "END AS daily_net " +
                "FROM t_order_daily od " +
                "JOIN t_order o ON o.id = od.order_id AND o.order_status NOT IN (5, 6) " +
                "LEFT JOIN t_channel_fee_rule f ON f.channel_id = o.channel_id " +
                "  AND (f.room_type_id = od.room_type_id OR f.room_type_id IS NULL) " +
                "  AND f.status = 1 " +
                "  AND CURRENT_DATE BETWEEN f.effective_from AND COALESCE(f.effective_to, '9999-12-31') " +
                "  AND f.priority = (SELECT MAX(f2.priority) FROM t_channel_fee_rule f2 WHERE f2.channel_id = o.channel_id AND (f2.room_type_id = od.room_type_id OR f2.room_type_id IS NULL) AND f2.status = 1 AND CURRENT_DATE BETWEEN f2.effective_from AND COALESCE(f2.effective_to, '9999-12-31')) " +
                "WHERE od.stay_date BETWEEN ? AND ? " +
                "GROUP BY od.stay_date, f.fee_type, f.commission_rate, f.fixed_fee, f.per_night_fee " +
                "ORDER BY od.stay_date";

        Map<LocalDate, RevenueForecastVO> map = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            LocalDate date = rs.getDate("stay_date").toLocalDate();
            RevenueForecastVO vo = new RevenueForecastVO();
            vo.setDate(date.toString());
            vo.setGrossRevenue(rs.getBigDecimal("daily_gross"));
            vo.setPlatformFee(rs.getBigDecimal("daily_fee"));
            vo.setNetProfit(rs.getBigDecimal("daily_net"));
            BigDecimal gross = vo.getGrossRevenue();
            BigDecimal net = vo.getNetProfit();
            vo.setNetRate(gross != null && gross.compareTo(BigDecimal.ZERO) > 0
                    ? net.multiply(BigDecimal.valueOf(100)).divide(gross, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            map.merge(date, vo, (existing, incoming) -> {
                existing.setGrossRevenue(existing.getGrossRevenue().add(incoming.getGrossRevenue()));
                existing.setPlatformFee(existing.getPlatformFee().add(incoming.getPlatformFee()));
                existing.setNetProfit(existing.getNetProfit().add(incoming.getNetProfit()));
                BigDecimal g = existing.getGrossRevenue();
                existing.setNetRate(g.compareTo(BigDecimal.ZERO) > 0
                        ? existing.getNetProfit().multiply(BigDecimal.valueOf(100)).divide(g, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
                return existing;
            });
        }, startDate, endDate);

        return new ArrayList<>(map.values());
    }

    private List<RevenueForecastVO> getPredictedRevenue(LocalDate startDate, LocalDate endDate,
                                                         List<RevenueForecastVO> confirmed) {
        LocalDate today = LocalDate.now();
        LocalDate forecastStart = today.plusDays(1);
        LocalDate forecastEnd = today.plusDays(30);
        if (forecastStart.isAfter(endDate)) {
            return new ArrayList<>();
        }

        LocalDate actualStart = forecastStart.isBefore(startDate) ? startDate : forecastStart;
        LocalDate actualEnd = forecastEnd.isAfter(endDate) ? endDate : forecastEnd;

        Map<LocalDate, RevenueForecastVO> confirmedMap = new LinkedHashMap<>();
        for (RevenueForecastVO v : confirmed) {
            confirmedMap.put(LocalDate.parse(v.getDate()), v);
        }

        BigDecimal avgBookingRate = calculateRecentBookingRate();
        Map<Long, ChannelFeeRule> feeRuleMap = loadFeeRules();

        Map<LocalDate, RevenueForecastVO> predictedMap = new LinkedHashMap<>();
        LocalDate d = actualStart;
        while (!d.isAfter(actualEnd)) {
            if (confirmedMap.containsKey(d)) {
                d = d.plusDays(1);
                continue;
            }

            final LocalDate date = d;
            List<Inventory> inventories = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getStayDate, date)
            );

            BigDecimal dayGross = BigDecimal.ZERO;
            BigDecimal dayFee = BigDecimal.ZERO;

            for (Inventory inv : inventories) {
                BigDecimal basePrice = inv.getBasePrice() != null ? inv.getBasePrice() : BigDecimal.ZERO;
                int totalRooms = inv.getTotalRooms() != null ? inv.getTotalRooms() : 0;
                int availableRooms = inv.getAvailableRooms() != null ? inv.getAvailableRooms() : 0;
                if (availableRooms <= 0 || basePrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                int predictedBookings = Math.max(0, (int) Math.round(availableRooms * avgBookingRate.doubleValue()));
                if (predictedBookings == 0) continue;

                BigDecimal roomGross = basePrice.multiply(BigDecimal.valueOf(predictedBookings));
                dayGross = dayGross.add(roomGross);

                ChannelFeeRule bestRule = findBestFeeRule(feeRuleMap, inv.getRoomTypeId());
                BigDecimal roomFee = calculateFee(bestRule, roomGross, predictedBookings);
                dayFee = dayFee.add(roomFee);
            }

            if (dayGross.compareTo(BigDecimal.ZERO) > 0) {
                RevenueForecastVO vo = new RevenueForecastVO();
                vo.setDate(date.toString());
                vo.setGrossRevenue(dayGross);
                vo.setPlatformFee(dayFee);
                vo.setNetProfit(dayGross.subtract(dayFee));
                vo.setNetRate(dayGross.compareTo(BigDecimal.ZERO) > 0
                        ? dayGross.subtract(dayFee).multiply(BigDecimal.valueOf(100)).divide(dayGross, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
                predictedMap.put(date, vo);
            }

            d = d.plusDays(1);
        }

        return new ArrayList<>(predictedMap.values());
    }

    private List<RevenueForecastVO> mergeForecast(List<RevenueForecastVO> confirmed,
                                                   List<RevenueForecastVO> predicted,
                                                   LocalDate startDate, LocalDate endDate) {
        Map<String, RevenueForecastVO> merged = new LinkedHashMap<>();
        for (RevenueForecastVO v : confirmed) {
            merged.put(v.getDate(), v);
        }
        for (RevenueForecastVO v : predicted) {
            merged.putIfAbsent(v.getDate(), v);
        }

        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            String key = d.toString();
            if (!merged.containsKey(key)) {
                RevenueForecastVO empty = new RevenueForecastVO();
                empty.setDate(key);
                empty.setGrossRevenue(BigDecimal.ZERO);
                empty.setPlatformFee(BigDecimal.ZERO);
                empty.setNetProfit(BigDecimal.ZERO);
                empty.setNetRate(BigDecimal.ZERO);
                merged.put(key, empty);
            }
            d = d.plusDays(1);
        }

        List<RevenueForecastVO> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(RevenueForecastVO::getDate));
        return result;
    }

    private List<RevenueForecastVO.ChannelBreakdown> getChannelBreakdown(LocalDate startDate, LocalDate endDate) {
        String sql =
                "SELECT o.channel_id, c.channel_name, " +
                "SUM(o.total_amount) AS gross, " +
                "CASE " +
                "  WHEN COALESCE(f.fee_type, 1) = 1 THEN SUM(o.total_amount) * COALESCE(f.commission_rate, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 2 THEN COUNT(o.id) * COALESCE(f.fixed_fee, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 3 THEN SUM(o.total_amount) * COALESCE(f.commission_rate, 0) + COUNT(o.id) * COALESCE(f.fixed_fee, 0) " +
                "  WHEN COALESCE(f.fee_type, 1) = 4 THEN COUNT(o.id) * COALESCE(f.per_night_fee, 0) " +
                "  ELSE 0 " +
                "END AS fee, " +
                "COUNT(o.id) AS order_count, " +
                "COALESCE(f.commission_rate, 0) AS commission_rate " +
                "FROM t_order o " +
                "JOIN t_channel c ON c.id = o.channel_id " +
                "LEFT JOIN t_channel_fee_rule f ON f.channel_id = o.channel_id " +
                "  AND (f.room_type_id = o.room_type_id OR f.room_type_id IS NULL) " +
                "  AND f.status = 1 " +
                "  AND CURRENT_DATE BETWEEN f.effective_from AND COALESCE(f.effective_to, '9999-12-31') " +
                "  AND f.priority = (SELECT MAX(f2.priority) FROM t_channel_fee_rule f2 WHERE f2.channel_id = o.channel_id AND (f2.room_type_id = o.room_type_id OR f2.room_type_id IS NULL) AND f2.status = 1 AND CURRENT_DATE BETWEEN f2.effective_from AND COALESCE(f2.effective_to, '9999-12-31')) " +
                "WHERE o.order_status NOT IN (5, 6) " +
                "  AND o.checkin_date BETWEEN ? AND ? " +
                "GROUP BY o.channel_id, c.channel_name, f.fee_type, f.commission_rate, f.fixed_fee, f.per_night_fee " +
                "ORDER BY gross DESC";

        Map<Long, RevenueForecastVO.ChannelBreakdown> map = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long channelId = rs.getLong("channel_id");
            RevenueForecastVO.ChannelBreakdown cb = new RevenueForecastVO.ChannelBreakdown();
            cb.setChannelId(channelId);
            cb.setChannelName(rs.getString("channel_name"));
            cb.setGrossRevenue(rs.getBigDecimal("gross"));
            cb.setPlatformFee(rs.getBigDecimal("fee"));
            BigDecimal gross = cb.getGrossRevenue();
            BigDecimal fee = cb.getPlatformFee();
            cb.setNetProfit(gross.subtract(fee));
            cb.setNetRate(gross.compareTo(BigDecimal.ZERO) > 0
                    ? cb.getNetProfit().multiply(BigDecimal.valueOf(100)).divide(gross, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            cb.setOrderCount(rs.getInt("order_count"));
            cb.setCommissionRate(rs.getBigDecimal("commission_rate"));
            map.merge(channelId, cb, (existing, incoming) -> {
                existing.setGrossRevenue(existing.getGrossRevenue().add(incoming.getGrossRevenue()));
                existing.setPlatformFee(existing.getPlatformFee().add(incoming.getPlatformFee()));
                existing.setNetProfit(existing.getGrossRevenue().subtract(existing.getPlatformFee()));
                existing.setOrderCount(existing.getOrderCount() + incoming.getOrderCount());
                BigDecimal g = existing.getGrossRevenue();
                existing.setNetRate(g.compareTo(BigDecimal.ZERO) > 0
                        ? existing.getNetProfit().multiply(BigDecimal.valueOf(100)).divide(g, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
                return existing;
            });
        }, startDate, endDate);

        return new ArrayList<>(map.values());
    }

    private BigDecimal calculateRecentBookingRate() {
        try {
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            String sql = "SELECT COALESCE(AVG(CASE WHEN inv.total_rooms > 0 THEN inv.booked_rooms::DECIMAL / inv.total_rooms ELSE 0 END), 0.5) " +
                    "FROM t_inventory inv WHERE inv.stay_date BETWEEN ? AND CURRENT_DATE";
            BigDecimal rate = jdbcTemplate.queryForObject(sql, BigDecimal.class, sevenDaysAgo);
            return rate != null ? rate : BigDecimal.valueOf(0.5);
        } catch (Exception e) {
            log.warn("计算历史入住率失败，使用默认值0.5", e);
            return BigDecimal.valueOf(0.5);
        }
    }

    private Map<Long, ChannelFeeRule> loadFeeRules() {
        List<ChannelFeeRule> rules = feeRuleMapper.selectList(
                new LambdaQueryWrapper<ChannelFeeRule>()
                        .eq(ChannelFeeRule::getStatus, 1)
                        .le(ChannelFeeRule::getEffectiveFrom, LocalDate.now())
                        .and(w -> w.isNull(ChannelFeeRule::getEffectiveTo)
                                .or().ge(ChannelFeeRule::getEffectiveTo, LocalDate.now()))
        );
        Map<Long, ChannelFeeRule> map = new HashMap<>();
        for (ChannelFeeRule rule : rules) {
            Long key = rule.getRoomTypeId() != null ? rule.getRoomTypeId() : 0L;
            ChannelFeeRule existing = map.get(key);
            if (existing == null || rule.getPriority() > existing.getPriority()) {
                map.put(rule.getChannelId() * 100000L + key, rule);
            }
        }
        return map;
    }

    private ChannelFeeRule findBestFeeRule(Map<Long, ChannelFeeRule> feeRuleMap, Long roomTypeId) {
        Long specificKey = feeRuleMap.keySet().stream()
                .filter(k -> k % 100000L == (roomTypeId != null ? roomTypeId : 0L))
                .findFirst().orElse(null);
        if (specificKey != null) {
            return feeRuleMap.get(specificKey);
        }
        Long genericKey = feeRuleMap.keySet().stream()
                .filter(k -> k % 100000L == 0L)
                .findFirst().orElse(null);
        return genericKey != null ? feeRuleMap.get(genericKey) : null;
    }

    private BigDecimal calculateFee(ChannelFeeRule rule, BigDecimal grossAmount, int bookingCount) {
        if (rule == null) return BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        int feeType = rule.getFeeType() != null ? rule.getFeeType() : 1;
        switch (feeType) {
            case 1:
                fee = grossAmount.multiply(rule.getCommissionRate() != null ? rule.getCommissionRate() : BigDecimal.ZERO);
                break;
            case 2:
                fee = rule.getFixedFee() != null ? rule.getFixedFee().multiply(BigDecimal.valueOf(bookingCount)) : BigDecimal.ZERO;
                break;
            case 3:
                fee = grossAmount.multiply(rule.getCommissionRate() != null ? rule.getCommissionRate() : BigDecimal.ZERO)
                        .add(rule.getFixedFee() != null ? rule.getFixedFee().multiply(BigDecimal.valueOf(bookingCount)) : BigDecimal.ZERO);
                break;
            case 4:
                fee = rule.getPerNightFee() != null ? rule.getPerNightFee().multiply(BigDecimal.valueOf(bookingCount)) : BigDecimal.ZERO;
                break;
            default:
                break;
        }
        if (rule.getMinFee() != null && fee.compareTo(rule.getMinFee()) < 0) {
            fee = rule.getMinFee();
        }
        if (rule.getMaxFee() != null && fee.compareTo(rule.getMaxFee()) > 0) {
            fee = rule.getMaxFee();
        }
        return fee;
    }
}
