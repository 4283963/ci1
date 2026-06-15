package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_room_type")
public class RoomType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long propertyId;
    private String typeCode;
    private String typeName;
    private Integer bedCount;
    private Integer maxGuests;
    private Integer roomCount;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
