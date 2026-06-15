package com.homestay.core.channel.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ChannelOrderDTO {
    private String channelOrderNo;
    private String channelRoomCode;
    private String guestName;
    private String guestPhone;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer nightCount;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private Integer orderStatus;
    private Integer payStatus;
    private String remark;
    private LocalDateTime bookTime;
    private String rawData;
}
