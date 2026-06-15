package com.homestay.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sync_log")
public class SyncLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String taskCode;
    private String taskName;
    private Long channelId;
    private Integer taskType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer successCount;
    private Integer failCount;
    private Integer totalCount;
    private Integer syncStatus;
    private String errorMsg;
    private String requestData;
    private String responseData;
    private LocalDateTime createdAt;
}
