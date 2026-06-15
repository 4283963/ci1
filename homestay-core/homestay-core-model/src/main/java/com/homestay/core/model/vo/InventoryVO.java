package com.homestay.core.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryVO {
    private Long roomTypeId;
    private String typeName;
    private LocalDate stayDate;
    private Integer totalRooms;
    private Integer bookedRooms;
    private Integer lockedRooms;
    private Integer availableRooms;
    private BigDecimal basePrice;
}
