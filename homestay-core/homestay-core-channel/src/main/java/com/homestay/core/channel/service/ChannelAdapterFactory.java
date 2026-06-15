package com.homestay.core.channel.service;

import com.homestay.core.channel.adapter.ChannelAdapter;
import com.homestay.core.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelAdapterFactory {

    private final List<ChannelAdapter> adapters;
    private final Map<String, ChannelAdapter> adapterMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ChannelAdapter adapter : adapters) {
            adapterMap.put(adapter.getChannelCode().toUpperCase(), adapter);
            log.info("渠道适配器已注册: {} -> {}", adapter.getChannelCode(), adapter.getClass().getSimpleName());
        }
    }

    public ChannelAdapter getAdapter(String channelCode) {
        ChannelAdapter adapter = adapterMap.get(channelCode.toUpperCase());
        if (adapter == null) {
            throw BusinessException.of("不支持的渠道: " + channelCode);
        }
        return adapter;
    }

    public boolean isSupported(String channelCode) {
        return adapterMap.containsKey(channelCode.toUpperCase());
    }

    public List<ChannelAdapter> getAllAdapters() {
        return adapters;
    }
}
