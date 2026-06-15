package com.homestay.core.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChannelOrderDistributionVO {
    private Long channelId;
    private String channelName;
    private String orderDate;
    private Integer orderCount;
    private BigDecimal totalRevenue;
    private Integer totalNights;
}
