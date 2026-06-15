package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_property")
public class Property {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String propertyCode;
    private String propertyName;
    private String address;
    private String city;
    private String province;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
