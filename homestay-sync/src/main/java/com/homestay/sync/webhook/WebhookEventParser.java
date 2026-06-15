package com.homestay.sync.webhook;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.sync.service.OrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventParser {

    private final ObjectMapper objectMapper;
    private final BaseMapper<com.homestay.sync.entity.WebhookLog> webhookLogMapper;
    private final OrderSyncService orderSyncService;

    public com.homestay.sync.entity.WebhookLog parseAndProcess(String channelCode, Long channelId,
                                                                String eventType, String rawBody,
                                                                Map<String, String> headers) {
        log.info("收到Webhook: channel={}, eventType={}", channelCode, eventType);

        com.homestay.sync.entity.WebhookLog logEntity = new com.homestay.sync.entity.WebhookLog();
        logEntity.setChannelId(channelId);
        logEntity.setEventType(eventType);
        logEntity.setRequestBody(rawBody);
        logEntity.setHeaders(headers != null ? toJson(headers) : null);
        logEntity.setProcessStatus(0);
        logEntity.setReceivedAt(LocalDateTime.now());
        webhookLogMapper.insert(logEntity);

        try {
            ChannelOrderEvent event = parseEvent(channelCode, eventType, rawBody);
            if (event == null) {
                log.warn("无法解析Webhook事件，跳过处理");
                logEntity.setProcessStatus(-1);
                logEntity.setProcessMsg("无法解析事件");
                logEntity.setProcessedAt(LocalDateTime.now());
                webhookLogMapper.updateById(logEntity);
                return logEntity;
            }
            event.setChannelId(channelId);
            orderSyncService.processChannelOrder(event);

            logEntity.setProcessStatus(1);
            logEntity.setProcessMsg("处理成功");
            logEntity.setProcessedAt(LocalDateTime.now());
            webhookLogMapper.updateById(logEntity);
        } catch (Exception e) {
            log.error("Webhook处理失败", e);
            logEntity.setProcessStatus(-1);
            logEntity.setProcessMsg(e.getMessage());
            logEntity.setProcessedAt(LocalDateTime.now());
            webhookLogMapper.updateById(logEntity);
        }
        return logEntity;
    }

    private ChannelOrderEvent parseEvent(String channelCode, String eventType, String rawBody) {
        try {
            Map<String, Object> payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
            ChannelOrderEvent event = new ChannelOrderEvent();
            event.setChannelCode(channelCode);
            event.setEventType(eventType);
            event.setRawBody(rawBody);

            Map<String, Object> data = payload.containsKey("data") && payload.get("data") instanceof Map
                    ? (Map<String, Object>) payload.get("data") : payload;

            event.setChannelOrderNo(firstNonNull(data, "orderId", "orderNo", "order_id", "order_no", "bookingId"));
            event.setGuestName(firstNonNull(data, "guestName", "contactName", "customerName", "name"));
            event.setGuestPhone(firstNonNull(data, "guestPhone", "contactPhone", "customerPhone", "phone"));
            Object rt = firstNonNull(data, "roomTypeId", "roomId", "goodsId", "productId");
            if (rt != null) {
                try { event.setRoomTypeId(Long.parseLong(rt.toString())); } catch (Exception ignored) {}
            }
            Object pi = firstNonNull(data, "propertyId", "hotelId");
            if (pi != null) {
                try { event.setPropertyId(Long.parseLong(pi.toString())); } catch (Exception ignored) {}
            }
            String ci = firstNonNull(data, "checkinDate", "checkin", "arrivalDate", "check_in_date");
            String co = firstNonNull(data, "checkoutDate", "checkout", "departureDate", "check_out_date");
            if (ci != null) event.setCheckinDate(LocalDate.parse(ci.length() >= 10 ? ci.substring(0, 10) : ci));
            if (co != null) event.setCheckoutDate(LocalDate.parse(co.length() >= 10 ? co.substring(0, 10) : co));
            Object gc = firstNonNull(data, "guestCount", "personCount", "guestNum");
            if (gc != null) event.setGuestCount(Integer.parseInt(gc.toString()));
            Object amt = firstNonNull(data, "totalAmount", "amount", "price", "totalPrice");
            if (amt != null) event.setTotalAmount(new BigDecimal(amt.toString()));
            event.setRemark(firstNonNull(data, "remark", "note", "comments"));
            event.setCancelReason(firstNonNull(data, "cancelReason", "reason"));

            return event;
        } catch (Exception e) {
            log.warn("解析Webhook事件失败: {}", e.getMessage());
            return null;
        }
    }

    private String firstNonNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && !v.toString().isEmpty()) {
                return v.toString();
            }
        }
        return null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
