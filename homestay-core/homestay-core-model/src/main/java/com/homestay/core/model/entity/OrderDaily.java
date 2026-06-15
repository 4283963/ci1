package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("t_order_daily")
public class OrderDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long roomTypeId;
    private LocalDate stayDate;
    private BigDecimal roomPrice;
    private java.time.LocalDateTime createdAt;
}
