package com.homestay.core.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    PENDING(1, "待确认"),
    CONFIRMED(2, "已确认"),
    CHECKED_IN(3, "已入住"),
    CHECKED_OUT(4, "已退房"),
    CANCELLED(5, "已取消"),
    NO_SHOW(6, "未到");

    private final Integer code;
    private final String desc;
}
