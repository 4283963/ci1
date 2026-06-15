package com.homestay.core.channel.model;

import lombok.Data;

@Data
public class ChannelResult<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ChannelResult<T> ok(T data) {
        ChannelResult<T> r = new ChannelResult<>();
        r.setSuccess(true);
        r.setCode("0");
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> ChannelResult<T> fail(String code, String message) {
        ChannelResult<T> r = new ChannelResult<>();
        r.setSuccess(false);
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
