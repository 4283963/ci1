package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomTypeId;
    private LocalDate stayDate;
    private Integer totalRooms;
    private Integer bookedRooms;
    private Integer lockedRooms;
    private Integer availableRooms;
    private BigDecimal basePrice;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
