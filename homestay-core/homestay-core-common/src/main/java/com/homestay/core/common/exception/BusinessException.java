package com.homestay.core.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public static BusinessException of(String msg) {
        return new BusinessException(msg);
    }

    public static BusinessException of(Integer code, String msg) {
        return new BusinessException(code, msg);
    }
}
