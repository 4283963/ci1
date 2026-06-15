package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_channel_room_mapping")
public class ChannelRoomMapping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long channelAccountId;
    private Long roomTypeId;
    private String channelRoomCode;
    private String channelRoomName;
    private Integer syncStatus;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
