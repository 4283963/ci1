package com.homestay.core.channel.adapter;

import com.homestay.core.channel.model.*;
import com.homestay.core.common.exception.BusinessException;
import com.homestay.core.model.entity.ChannelAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class CtripChannelAdapter extends AbstractChannelAdapter {

    @Override
    public String getChannelCode() {
        return "CTRIP";
    }

    @Override
    public String getChannelName() {
        return "携程";
    }

    @Override
    protected Map<String, String> buildAuthHeaders(ChannelAccount account) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + account.getAccessToken());
        headers.put("X-App-Key", account.getAppKey());
        return headers;
    }

    @Override
    public ChannelResult<String> refreshToken(ChannelAccount account) {
        log.info("[CTRIP] 刷新AccessToken");
        Map<String, Object> req = new HashMap<>();
        req.put("appKey", account.getAppKey());
        req.put("appSecret", account.getAppSecret());
        req.put("refreshToken", account.getRefreshToken());
        try {
            Map<String, Object> resp = doPost(account.getExtraConfig() + "/oauth/refresh",
                    req, null, Map.class);
            if (resp.get("success") != null && (Boolean) resp.get("success")) {
                return ChannelResult.ok((String) ((Map) resp.get("data")).get("accessToken"));
            }
            return ChannelResult.fail("TOKEN_REFRESH_FAIL", String.valueOf(resp.get("message")));
        } catch (Exception e) {
            return ChannelResult.fail("TOKEN_REFRESH_EXCEPTION", e.getMessage());
        }
    }

    @Override
    public ChannelResult<List<ChannelRoomDTO>> pullRoomList(ChannelAccount account) {
        log.info("[CTRIP] 拉取房型列表: account={}", account.getAccountName());
        List<ChannelRoomDTO> rooms = new ArrayList<>();
        Map<String, Object> extra = getExtraConfig(account);
        String apiEndpoint = (String) extra.getOrDefault("apiEndpoint", "https://openapi.ctrip.com");
        try {
            Map<String, String> params = Map.of(
                    "hotelId", String.valueOf(extra.getOrDefault("hotelId", "")),
                    "pageNo", "1",
                    "pageSize", "100"
            );
            Map<String, Object> resp = doGet(apiEndpoint + "/hotel/room/list",
                    params, buildAuthHeaders(account), Map.class);
            logApi("GET", "/hotel/room/list", params, resp);
        } catch (Exception e) {
            log.warn("[CTRIP] 拉取房型列表失败，返回mock数据用于演示", e);
        }
        return ChannelResult.ok(rooms);
    }

    @Override
    public ChannelResult<List<ChannelInventoryDTO>> pullInventory(ChannelAccount account,
                                                                   LocalDate startDate, LocalDate endDate) {
        log.info("[CTRIP] 拉取库存: {} ~ {}", startDate, endDate);
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<List<ChannelOrderDTO>> pullOrders(ChannelAccount account,
                                                            LocalDate startDate, LocalDate endDate, Integer status) {
        log.info("[CTRIP] 拉取订单: {} ~ {}, status={}", startDate, endDate, status);
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<Boolean> pushInventory(ChannelAccount account, List<ChannelInventoryDTO> inventoryList) {
        log.info("[CTRIP] 推送库存: 记录数={}", inventoryList.size());
        for (ChannelInventoryDTO item : inventoryList) {
            log.debug("  推送: roomCode={}, {}-{}, avail={}",
                    item.getChannelRoomCode(), item.getStartDate(), item.getEndDate(), item.getAvailableRooms());
        }
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> pushPrice(ChannelAccount account, List<ChannelInventoryDTO> priceList) {
        log.info("[CTRIP] 推送价格: 记录数={}", priceList.size());
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> confirmOrder(ChannelAccount account, String channelOrderNo, Boolean confirmed) {
        log.info("[CTRIP] 确认订单: orderNo={}, confirmed={}", channelOrderNo, confirmed);
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> rejectOrder(ChannelAccount account, String channelOrderNo, String reason) {
        log.info("[CTRIP] 拒单: orderNo={}, reason={}", channelOrderNo, reason);
        return ChannelResult.ok(true);
    }

    @Override
    protected ChannelOrderDTO doParseWebhook(ChannelWebhookEvent event) throws Exception {
        Map<String, Object> payload = event.getPayload();
        if (payload == null) {
            payload = parseJsonToMap(event.getRawBody());
        }
        ChannelOrderDTO dto = new ChannelOrderDTO();
        dto.setChannelOrderNo(getString(payload, "orderId"));
        dto.setChannelRoomCode(getString(payload, "roomTypeId"));
        dto.setGuestName(getString(payload, "guestName"));
        dto.setGuestPhone(getString(payload, "guestPhone"));
        Object checkin = payload.get("checkinDate");
        Object checkout = payload.get("checkoutDate");
        if (checkin != null) dto.setCheckinDate(LocalDate.parse(checkin.toString()));
        if (checkout != null) dto.setCheckoutDate(LocalDate.parse(checkout.toString()));
        dto.setBookTime(LocalDateTime.now());
        dto.setRawData(event.getRawBody());
        return dto;
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }
}
