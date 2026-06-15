package com.homestay.core.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelCodeEnum {
    CTRIP("CTRIP", "携程"),
    MEITUAN("MEITUAN", "美团酒店"),
    FLIGGY("FLIGGY", "飞猪"),
    AIRBNB("AIRBNB", "Airbnb爱彼迎"),
    DIRECT("DIRECT", "官网直订");

    private final String code;
    private final String name;
}
