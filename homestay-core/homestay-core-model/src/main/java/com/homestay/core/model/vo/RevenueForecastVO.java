package com.homestay.core.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevenueForecastVO {
    private String date;
    private BigDecimal grossRevenue;
    private BigDecimal platformFee;
    private BigDecimal netProfit;
    private BigDecimal netRate;

    @Data
    public static class ChannelBreakdown {
        private Long channelId;
        private String channelName;
        private BigDecimal grossRevenue;
        private BigDecimal platformFee;
        private BigDecimal netProfit;
        private BigDecimal netRate;
        private BigDecimal commissionRate;
        private Integer orderCount;
    }
}
