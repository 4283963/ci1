package com.homestay.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiR<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    public boolean isSuccess() {
        return code != null && code == 200;
    }
}
