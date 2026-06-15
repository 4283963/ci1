package com.homestay.core.channel.adapter;

import com.homestay.core.channel.model.*;
import com.homestay.core.model.entity.ChannelAccount;

import java.time.LocalDate;
import java.util.List;

public interface ChannelAdapter {

    String getChannelCode();

    String getChannelName();

    default boolean support(String channelCode) {
        return getChannelCode().equalsIgnoreCase(channelCode);
    }

    ChannelResult<String> refreshToken(ChannelAccount account);

    ChannelResult<List<ChannelRoomDTO>> pullRoomList(ChannelAccount account);

    ChannelResult<List<ChannelInventoryDTO>> pullInventory(ChannelAccount account,
                                                           LocalDate startDate,
                                                           LocalDate endDate);

    ChannelResult<List<ChannelOrderDTO>> pullOrders(ChannelAccount account,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     Integer status);

    ChannelResult<Boolean> pushInventory(ChannelAccount account,
                                          List<ChannelInventoryDTO> inventoryList);

    ChannelResult<Boolean> pushPrice(ChannelAccount account,
                                     List<ChannelInventoryDTO> priceList);

    ChannelResult<Boolean> confirmOrder(ChannelAccount account,
                                        String channelOrderNo,
                                        Boolean confirmed);

    ChannelResult<Boolean> rejectOrder(ChannelAccount account,
                                       String channelOrderNo,
                                       String reason);

    ChannelResult<ChannelOrderDTO> parseWebhookEvent(ChannelWebhookEvent event,
                                                     ChannelAccount account);

    default String signRequest(ChannelAccount account, String body) {
        return body;
    }

    default boolean verifyWebhookSignature(ChannelWebhookEvent event, ChannelAccount account) {
        return true;
    }
}
