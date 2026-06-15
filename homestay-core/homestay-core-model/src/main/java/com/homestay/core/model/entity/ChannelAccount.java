package com.homestay.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_channel_account")
public class ChannelAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long channelId;
    private Long propertyId;
    private String accountName;
    private String appKey;
    private String appSecret;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpireAt;
    private String extraConfig;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
