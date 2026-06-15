package com.homestay.core.common.exception;

public class InventoryInsufficientException extends BusinessException {

    public InventoryInsufficientException(String msg) {
        super(4001, msg);
    }

    public static InventoryInsufficientException of(Long roomTypeId, String date, int required, int available) {
        return new InventoryInsufficientException(
                String.format("房型[%d]在[%s]库存不足，需要%d间，可用%d间", roomTypeId, date, required, available)
        );
    }
}
