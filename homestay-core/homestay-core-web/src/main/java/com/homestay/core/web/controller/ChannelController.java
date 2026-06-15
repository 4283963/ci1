package com.homestay.core.web.controller;

import com.homestay.core.channel.adapter.ChannelAdapter;
import com.homestay.core.channel.model.ChannelRoomDTO;
import com.homestay.core.channel.model.ChannelInventoryDTO;
import com.homestay.core.channel.model.ChannelOrderDTO;
import com.homestay.core.channel.model.ChannelResult;
import com.homestay.core.channel.service.ChannelAdapterFactory;
import com.homestay.core.common.result.R;
import com.homestay.core.model.entity.ChannelAccount;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelAdapterFactory channelAdapterFactory;
    private final BaseMapper<ChannelAccount> channelAccountMapper;

    @GetMapping("/adapters")
    public R<List<String>> listSupportedChannels() {
        List<String> channels = channelAdapterFactory.getAllAdapters().stream()
                .map(a -> a.getChannelCode() + " - " + a.getChannelName())
                .toList();
        return R.ok(channels);
    }

    @GetMapping("/{channelCode}/rooms")
    public R<List<ChannelRoomDTO>> pullRoomList(@PathVariable String channelCode,
                                                 @RequestParam Long channelAccountId) {
        ChannelAccount account = channelAccountMapper.selectById(channelAccountId);
        if (account == null) {
            return R.fail("渠道账号不存在");
        }
        ChannelAdapter adapter = channelAdapterFactory.getAdapter(channelCode);
        ChannelResult<List<ChannelRoomDTO>> result = adapter.pullRoomList(account);
        if (result.isSuccess()) {
            return R.ok(result.getData());
        }
        return R.fail(result.getCode(), result.getMessage());
    }

    @GetMapping("/{channelCode}/orders")
    public R<List<ChannelOrderDTO>> pullOrders(@PathVariable String channelCode,
                                                @RequestParam Long channelAccountId,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                @RequestParam(required = false) Integer status) {
        ChannelAccount account = channelAccountMapper.selectById(channelAccountId);
        ChannelAdapter adapter = channelAdapterFactory.getAdapter(channelCode);
        ChannelResult<List<ChannelOrderDTO>> result = adapter.pullOrders(account, startDate, endDate, status);
        if (result.isSuccess()) {
            return R.ok(result.getData());
        }
        return R.fail(result.getCode(), result.getMessage());
    }

    @PostMapping("/{channelCode}/push-inventory")
    public R<Boolean> pushInventory(@PathVariable String channelCode,
                                     @RequestParam Long channelAccountId,
                                     @RequestBody List<ChannelInventoryDTO> inventoryList) {
        ChannelAccount account = channelAccountMapper.selectById(channelAccountId);
        ChannelAdapter adapter = channelAdapterFactory.getAdapter(channelCode);
        ChannelResult<Boolean> result = adapter.pushInventory(account, inventoryList);
        if (result.isSuccess()) {
            return R.ok(result.getData());
        }
        return R.fail(result.getCode(), result.getMessage());
    }
}
