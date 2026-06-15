package com.homestay.core.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RevenueAnalysisVO {
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalPlatformFee;
    private BigDecimal totalNetProfit;
    private BigDecimal avgNetRate;
    private List<RevenueForecastVO> dailyForecast;
    private List<RevenueForecastVO.ChannelBreakdown> channelBreakdown;
}
