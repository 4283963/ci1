package com.homestay.sync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.sync.client.HomestayCoreClient;
import com.homestay.sync.dto.ApiR;
import com.homestay.sync.dto.OrderCreateRequest;
import com.homestay.sync.webhook.ChannelOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncService {

    private final HomestayCoreClient coreClient;
    private final BaseMapper<com.homestay.sync.entity.SyncLog> syncLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public com.homestay.sync.entity.SyncLog processChannelOrder(ChannelOrderEvent event) {
        log.info("处理渠道订单事件: channel={}, eventType={}, orderNo={}",
                event.getChannelCode(), event.getEventType(), event.getChannelOrderNo());

        com.homestay.sync.entity.SyncLog syncLog = new com.homestay.sync.entity.SyncLog();
        syncLog.setTaskCode("WEBHOOK_ORDER_" + event.getEventType());
        syncLog.setTaskName("Webhook订单同步-" + event.getEventType());
        syncLog.setTaskType(3);
        syncLog.setChannelId(event.getChannelId());
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setSyncStatus(1);
        syncLog.setRequestData(event.getRawBody());
        syncLog.setCreatedAt(LocalDateTime.now());
        syncLogMapper.insert(syncLog);

        try {
            ApiR<Map<String, Object>> existResp = coreClient.getOrderByChannelOrderNo(
                    event.getChannelId(), event.getChannelOrderNo());

            switch (event.getEventType().toUpperCase()) {
                case "ORDER_CREATE", "ORDER_NEW", "NEW_ORDER" -> {
                    if (existResp.isSuccess() && existResp.getData() != null) {
                        log.info("订单已存在，跳过创建: {}", event.getChannelOrderNo());
                    } else {
                        OrderCreateRequest req = new OrderCreateRequest();
                        req.setChannelId(event.getChannelId());
                        req.setChannelOrderNo(event.getChannelOrderNo());
                        req.setPropertyId(event.getPropertyId());
                        req.setRoomTypeId(event.getRoomTypeId());
                        req.setGuestName(event.getGuestName());
                        req.setGuestPhone(event.getGuestPhone());
                        req.setCheckinDate(event.getCheckinDate());
                        req.setCheckoutDate(event.getCheckoutDate());
                        req.setGuestCount(event.getGuestCount());
                        req.setTotalAmount(event.getTotalAmount());
                        req.setRemark(event.getRemark());
                        req.setExtraData(event.getRawBody());
                        ApiR<Map<String, Object>> resp = coreClient.createOrder(req);
                        if (!resp.isSuccess()) {
                            throw new RuntimeException("创建订单失败: " + resp.getMsg());
                        }
                        syncLog.setSuccessCount(1);
                    }
                }
                case "ORDER_CANCEL", "CANCEL_ORDER" -> {
                    if (existResp.isSuccess() && existResp.getData() != null) {
                        Number orderId = (Number) existResp.getData().get("id");
                        if (orderId != null) {
                            coreClient.cancelOrder(orderId.longValue(), event.getCancelReason());
                            syncLog.setSuccessCount(1);
                        }
                    }
                }
                case "ORDER_MODIFY", "ORDER_UPDATE" -> {
                    log.info("订单变更事件，需要业务补充处理逻辑: {}", event.getChannelOrderNo());
                }
                default -> log.warn("未处理的事件类型: {}", event.getEventType());
            }

            syncLog.setSyncStatus(2);
            syncLog.setEndTime(LocalDateTime.now());
            syncLog.setTotalCount(1);
            syncLogMapper.updateById(syncLog);
        } catch (Exception e) {
            log.error("处理渠道订单事件失败", e);
            syncLog.setSyncStatus(-1);
            syncLog.setEndTime(LocalDateTime.now());
            syncLog.setErrorMsg(e.getMessage());
            syncLog.setFailCount(1);
            syncLog.setTotalCount(1);
            syncLogMapper.updateById(syncLog);
            throw e;
        }
        return syncLog;
    }
}
