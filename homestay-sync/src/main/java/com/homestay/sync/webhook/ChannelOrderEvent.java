package com.homestay.sync.webhook;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChannelOrderEvent {
    private String channelCode;
    private Long channelId;
    private String eventType;
    private String channelOrderNo;
    private Long propertyId;
    private Long roomTypeId;
    private String guestName;
    private String guestPhone;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private String remark;
    private String cancelReason;
    private String rawBody;
}
