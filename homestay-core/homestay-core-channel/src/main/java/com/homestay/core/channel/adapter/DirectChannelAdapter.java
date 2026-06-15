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
public class DirectChannelAdapter extends AbstractChannelAdapter {

    @Override
    public String getChannelCode() {
        return "DIRECT";
    }

    @Override
    public String getChannelName() {
        return "官网直订";
    }

    @Override
    protected Map<String, String> buildAuthHeaders(ChannelAccount account) {
        return new HashMap<>();
    }

    @Override
    public ChannelResult<String> refreshToken(ChannelAccount account) {
        return ChannelResult.ok("N/A");
    }

    @Override
    public ChannelResult<List<ChannelRoomDTO>> pullRoomList(ChannelAccount account) {
        log.info("[DIRECT] 官网直订无需拉取房型");
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
        log.info("[DIRECT] 官网直订无需推送库存");
        return ChannelResult.ok(true);
    }

    @Override
    public ChannelResult<Boolean> pushPrice(ChannelAccount account, List<ChannelInventoryDTO> priceList) {
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
        return dto;
    }
}
