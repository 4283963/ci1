package com.homestay.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_webhook_log")
public class WebhookLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long channelId;
    private String eventType;
    private String requestId;
    private String requestBody;
    private String headers;
    private Integer processStatus;
    private String processMsg;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
}
