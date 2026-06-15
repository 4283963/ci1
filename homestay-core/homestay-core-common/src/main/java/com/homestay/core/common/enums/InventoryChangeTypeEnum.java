package com.homestay.core.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InventoryChangeTypeEnum {
    ADD_STOCK(1, "增加库存"),
    REDUCE_STOCK(2, "减少库存"),
    LOCK(3, "锁定"),
    UNLOCK(4, "释放锁定"),
    BOOK(5, "预订"),
    CANCEL_BOOK(6, "取消预订");

    private final Integer code;
    private final String desc;
}
