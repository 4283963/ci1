package com.homestay.core.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class RealtimeRoomStatusVO {
    private Long propertyId;
    private String propertyName;
    private Long roomTypeId;
    private String typeName;
    private Integer physicalRooms;
    private LocalDate statDate;
    private Integer totalRooms;
    private Integer bookedRooms;
    private Integer lockedRooms;
    private Integer availableRooms;
    private BigDecimal basePrice;
    private BigDecimal bookingRate;
}
