package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_channel_fee_rule")
public class ChannelFeeRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long channelId;
    private Long roomTypeId;
    private Integer feeType;
    private BigDecimal commissionRate;
    private BigDecimal fixedFee;
    private BigDecimal perNightFee;
    private Integer settlementDays;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    private Integer priority;
    private Integer status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
