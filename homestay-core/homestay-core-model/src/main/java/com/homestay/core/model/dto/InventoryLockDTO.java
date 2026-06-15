package com.homestay.core.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryLockDTO {
    private Long roomTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer lockCount;
    private Integer lockType;
    private String sourceType;
    private String sourceId;
    private Long channelId;
    private Integer expireMinutes;
}
