package com.homestay.core.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomTypeHeatmapVO {
    private Long roomTypeId;
    private String typeName;
    private String propertyName;
    private String stayDate;
    private Integer dayOfWeek;
    private Integer bookHour;
    private Integer orderCount;
    private BigDecimal revenue;
}
