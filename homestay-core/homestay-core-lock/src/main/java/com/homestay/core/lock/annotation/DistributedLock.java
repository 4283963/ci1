package com.homestay.core.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    String key() default "";

    String prefix() default "HOMESTAY:LOCK";

    long waitTime() default 3;

    long leaseTime() default 30;

    TimeUnit unit() default TimeUnit.SECONDS;
}
