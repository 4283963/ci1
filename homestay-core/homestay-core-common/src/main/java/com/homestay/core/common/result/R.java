package com.homestay.core.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    public static <T> R<T> ok() {
        return R.<T>builder()
                .code(200)
                .msg("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> R<T> ok(T data) {
        return R.<T>builder()
                .code(200)
                .msg("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> R<T> fail(String msg) {
        return R.<T>builder()
                .code(500)
                .msg(msg)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> R<T> fail(Integer code, String msg) {
        return R.<T>builder()
                .code(code)
                .msg(msg)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}
