package com.homestay.core.channel.adapter;

import com.homestay.core.channel.model.*;
import com.homestay.core.model.entity.ChannelAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FliggyChannelAdapter extends AbstractChannelAdapter {

    @Override
    public String getChannelCode() {
        return "FLIGGY";
    }

    @Override
    public String getChannelName() {
        return "飞猪";
    }

    @Override
    protected Map<String, String> buildAuthHeaders(ChannelAccount account) {
        Map<String, String> headers = new HashMap<>();
        headers.put("session", account.getAccessToken());
        return headers;
    }

    @Override
    public ChannelResult<String> refreshToken(ChannelAccount account) {
        log.info("[FLIGGY] 刷新TOP Session");
        return ChannelResult.ok(account.getAccessToken());
    }

    @Override
    public ChannelResult<List<ChannelRoomDTO>> pullRoomList(ChannelAccount account) {
        log.info("[FLIGGY] 拉取房型列表(飞猪商品)");
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
        log.info("[FLIGGY] 推送库存(taobao.hotel.room.update)");
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> pushPrice(ChannelAccount account, List<ChannelInventoryDTO> priceList) {
        log.info("[FLIGGY] 推送价格");
        return ChannelResult.ok(true);
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
        dto.setBookTime(LocalDateTime.now());
        return dto;
    }
}
