package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_inventory_lock")
public class InventoryLock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String lockKey;
    private Long roomTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer lockCount;
    private Integer lockType;
    private String sourceType;
    private String sourceId;
    private Long channelId;
    private LocalDateTime expireAt;
    private LocalDateTime lockedAt;
    private LocalDateTime unlockAt;
    private Integer status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
