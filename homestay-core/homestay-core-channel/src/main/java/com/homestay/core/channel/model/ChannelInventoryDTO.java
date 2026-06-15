package com.homestay.core.channel.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class ChannelInventoryDTO {
    private String channelRoomCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalRooms;
    private Integer availableRooms;
    private BigDecimal price;
    private Map<String, Object> extra;
}
