package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_channel")
public class Channel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelCode;
    private String channelName;
    private String adapterClass;
    private String apiEndpoint;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
