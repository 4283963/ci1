package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long channelId;
    private String channelOrderNo;
    private Long propertyId;
    private Long roomTypeId;
    private Long roomId;
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
    private Long lockId;
    private String remark;
    private String extraData;
    private LocalDateTime bookTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
