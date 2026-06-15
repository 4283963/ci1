package com.homestay.sync.client;

import com.homestay.sync.dto.ApiR;
import com.homestay.sync.dto.OrderCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "homestay-core", url = "${homestay.core.base-url}")
public interface HomestayCoreClient {

    @PostMapping("/orders")
    ApiR<Map<String, Object>> createOrder(@RequestBody OrderCreateRequest request);

    @GetMapping("/orders/channel")
    ApiR<Map<String, Object>> getOrderByChannelOrderNo(
            @RequestParam("channelId") Long channelId,
            @RequestParam("channelOrderNo") String channelOrderNo);

    @PostMapping("/orders/{id}/cancel")
    ApiR<Map<String, Object>> cancelOrder(@PathVariable("id") Long id,
                                           @RequestParam(value = "reason", required = false) String reason);

    @GetMapping("/inventory/query")
    ApiR<Object> queryInventory(@RequestParam("roomTypeId") Long roomTypeId,
                                @RequestParam("startDate") String startDate,
                                @RequestParam("endDate") String endDate);

    @PostMapping("/channel/{channelCode}/push-inventory")
    ApiR<Boolean> pushInventory(@PathVariable("channelCode") String channelCode,
                                 @RequestParam("channelAccountId") Long channelAccountId,
                                 @RequestBody Object inventoryList);
}
