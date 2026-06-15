package com.homestay.core.channel.model;

import lombok.Data;

import java.util.List;

@Data
public class ChannelRoomDTO {
    private String channelRoomCode;
    private String channelRoomName;
    private Integer roomCount;
    private List<String> channelRatePlanCodes;
    private String rawData;
}
