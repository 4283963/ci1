package com.homestay.core.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SourceTypeEnum {
    CHANNEL_ORDER("CHANNEL_ORDER", "渠道订单"),
    DIRECT_ORDER("DIRECT_ORDER", "直订订单"),
    MANUAL_BLOCK("MANUAL_BLOCK", "手动屏蔽"),
    PROMOTION("PROMOTION", "活动促销"),
    OFFLINE_ORDER("OFFLINE_ORDER", "线下订单");

    private final String code;
    private final String desc;
}
