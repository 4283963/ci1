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
public class AirbnbChannelAdapter extends AbstractChannelAdapter {

    @Override
    public String getChannelCode() {
        return "AIRBNB";
    }

    @Override
    public String getChannelName() {
        return "Airbnb爱彼迎";
    }

    @Override
    protected Map<String, String> buildAuthHeaders(ChannelAccount account) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Airbnb-Oauth-Token", account.getAccessToken());
        headers.put("X-Airbnb-API-Key", account.getAppKey());
        return headers;
    }

    @Override
    public ChannelResult<String> refreshToken(ChannelAccount account) {
        log.info("[AIRBNB] 刷新OAuth2 Token");
        return ChannelResult.ok(account.getAccessToken());
    }

    @Override
    public ChannelResult<List<ChannelRoomDTO>> pullRoomList(ChannelAccount account) {
        log.info("[AIRBNB] 拉取房源列表(Listing)");
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<List<ChannelInventoryDTO>> pullInventory(ChannelAccount account,
                                                                   LocalDate startDate, LocalDate endDate) {
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<List<ChannelOrderDTO>> pullOrders(ChannelAccount account,
                                                            LocalDate startDate, LocalDate endDate, Integer status) {
        return ChannelResult.ok(new ArrayList<>());
    }

    @Override
    public ChannelResult<Boolean> pushInventory(ChannelAccount account, List<ChannelInventoryDTO> inventoryList) {
        log.info("[AIRBNB] 推送日历(设置可用/不可用/价格)");
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> pushPrice(ChannelAccount account, List<ChannelInventoryDTO> priceList) {
        return pushInventory(account, priceList);
    }

    @Override
    public ChannelResult<Boolean> confirmOrder(ChannelAccount account, String channelOrderNo, Boolean confirmed) {
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> rejectOrder(ChannelAccount account, String channelOrderNo, String reason) {
        return ChannelResult.ok(true);
    }

    @Override
    protected ChannelOrderDTO doParseWebhook(ChannelWebhookEvent event) throws Exception {
        ChannelOrderDTO dto = new ChannelOrderDTO();
        dto.setRawData(event.getRawBody());
        return dto;
    }
}
