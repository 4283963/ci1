package com.homestay.core.web.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Proxy;

@Configuration
public class MapperProxyConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public <T> BaseMapper<T> baseMapper() {
        return (BaseMapper<T>) Proxy.newProxyInstance(
                BaseMapper.class.getClassLoader(),
                new Class<?>[]{BaseMapper.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(
                            "请为实体 [" + method.getDeclaringClass() + "] 创建具体的 Mapper 接口继承 BaseMapper，" +
                            "并通过 @Mapper 注解注册。当前通用代理仅用于框架演示编译通过。");
                }
        );
    }
}
