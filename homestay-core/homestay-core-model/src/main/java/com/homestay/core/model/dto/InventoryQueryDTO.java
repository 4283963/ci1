package com.homestay.core.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryQueryDTO {
    private Long roomTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
}
