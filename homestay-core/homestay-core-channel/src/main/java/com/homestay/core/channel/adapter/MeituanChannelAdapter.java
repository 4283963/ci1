package com.homestay.core.channel.adapter;

import com.homestay.core.channel.model.*;
import com.homestay.core.model.entity.ChannelAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MeituanChannelAdapter extends AbstractChannelAdapter {

    @Override
    public String getChannelCode() {
        return "MEITUAN";
    }

    @Override
    public String getChannelName() {
        return "美团酒店";
    }

    @Override
    protected Map<String, String> buildAuthHeaders(ChannelAccount account) {
        Map<String, String> headers = new HashMap<>();
        headers.put("appkey", account.getAppKey());
        headers.put("sign", signRequest(account, ""));
        return headers;
    }

    @Override
    public ChannelResult<String> refreshToken(ChannelAccount account) {
        log.info("[MEITUAN] 美团使用签名机制，无需刷新Token");
        return ChannelResult.ok("N/A");
    }

    @Override
    public ChannelResult<List<ChannelRoomDTO>> pullRoomList(ChannelAccount account) {
        log.info("[MEITUAN] 拉取房型列表");
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<List<ChannelInventoryDTO>> pullInventory(ChannelAccount account,
                                                                   LocalDate startDate, LocalDate endDate) {
        log.info("[MEITUAN] 拉取库存: {} ~ {}", startDate, endDate);
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<List<ChannelOrderDTO>> pullOrders(ChannelAccount account,
                                                            LocalDate startDate, LocalDate endDate, Integer status) {
        log.info("[MEITUAN] 拉取订单: {} ~ {}, status={}", startDate, endDate, status);
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<Boolean> pushInventory(ChannelAccount account, List<ChannelInventoryDTO> inventoryList) {
        log.info("[MEITUAN] 推送库存: 记录数={}", inventoryList.size());
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> pushPrice(ChannelAccount account, List<ChannelInventoryDTO> priceList) {
        log.info("[MEITUAN] 推送价格: 记录数={}", priceList.size());
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> confirmOrder(ChannelAccount account, String channelOrderNo, Boolean confirmed) {
        log.info("[MEITUAN] 确认订单: orderNo={}", channelOrderNo);
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> rejectOrder(ChannelAccount account, String channelOrderNo, String reason) {
        log.info("[MEITUAN] 拒单: orderNo={}", channelOrderNo);
        return ChannelResult.ok(true);
    }

    @Override
    public String signRequest(ChannelAccount account, String body) {
        String raw = account.getAppKey() + body + account.getAppSecret();
        return cn.hutool.crypto.SecureUtil.md5(raw);
    }

    @Override
    protected ChannelOrderDTO doParseWebhook(ChannelWebhookEvent event) throws Exception {
        Map<String, Object> payload = parseJsonToMap(event.getRawBody());
        ChannelOrderDTO dto = new ChannelOrderDTO();
        Object data = payload.get("data");
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            dto.setChannelOrderNo(String.valueOf(dataMap.getOrDefault("orderId", "")));
            dto.setChannelRoomCode(String.valueOf(dataMap.getOrDefault("goodsId", "")));
            dto.setGuestName(String.valueOf(dataMap.getOrDefault("contactName", "")));
            dto.setGuestPhone(String.valueOf(dataMap.getOrDefault("contactPhone", "")));
        }
        dto.setRawData(event.getRawBody());
        return dto;
    }
}
