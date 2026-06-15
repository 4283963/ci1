package com.homestay.core.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LockTypeEnum {
    PRE_LOCK(1, "预占锁"),
    CONFIRM_LOCK(2, "确认锁"),
    BLOCK_LOCK(3, "屏蔽锁");

    private final Integer code;
    private final String desc;
}
