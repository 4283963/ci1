package com.homestay.core.channel.model;

import lombok.Data;

import java.util.Map;

@Data
public class ChannelWebhookEvent {
    private String channelCode;
    private String eventType;
    private String requestId;
    private String rawBody;
    private Map<String, String> headers;
    private Map<String, Object> payload;
}
